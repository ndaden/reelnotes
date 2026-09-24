package com.danstudios.reelnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danstudios.reelnotes.data.local.PreferencesManager
import com.danstudios.reelnotes.data.repository.ReelNoteRepository
import com.danstudios.reelnotes.data.util.SampleDataProvider
import com.danstudios.reelnotes.domain.extractor.ReelExtractionPipeline
import com.danstudios.reelnotes.domain.model.NoteCategory
import com.danstudios.reelnotes.domain.model.ReelNote
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReelNotesViewModel(
    private val repository: ReelNoteRepository,
    private val preferences: PreferencesManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<NoteCategory?>(null)
    val selectedCategory: StateFlow<NoteCategory?> = _selectedCategory.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingStatus = MutableStateFlow("")
    val processingStatus: StateFlow<String> = _processingStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(preferences.geminiApiKey)
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _preferredLanguage = MutableStateFlow(preferences.preferredLanguage)
    val preferredLanguage: StateFlow<String> = _preferredLanguage.asStateFlow()

    private val _newlyCreatedNoteId = MutableSharedFlow<Long>()
    val newlyCreatedNoteId: SharedFlow<Long> = _newlyCreatedNoteId.asSharedFlow()

    // Notes filtered by search, category, and favorites
    val filteredNotes: StateFlow<List<ReelNote>> = combine(
        repository.getAllNotes(),
        _searchQuery,
        _selectedCategory,
        _onlyFavorites
    ) { allNotes, query, category, favOnly ->
        allNotes.filter { note ->
            val matchesCategory = (category == null || note.category == category)
            val matchesFav = (!favOnly || note.isFavorite)
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                note.title.contains(query, ignoreCase = true) ||
                note.summary.contains(query, ignoreCase = true) ||
                note.author?.contains(query, ignoreCase = true) == true ||
                note.tags.any { it.contains(query, ignoreCase = true) } ||
                note.rawCaption.contains(query, ignoreCase = true)
            }
            matchesCategory && matchesFav && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getNoteById(id: Long): Flow<ReelNote?> {
        return repository.getNoteById(id)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: NoteCategory?) {
        _selectedCategory.value = category
    }

    fun toggleOnlyFavorites() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun updateApiKey(key: String) {
        preferences.geminiApiKey = key
        _geminiApiKey.value = key
    }

    fun updateLanguage(lang: String) {
        preferences.preferredLanguage = lang
        _preferredLanguage.value = lang
    }

    fun toggleFavorite(note: ReelNote) {
        viewModelScope.launch {
            repository.toggleFavorite(note.id, !note.isFavorite)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    fun updateIngredientChecked(note: ReelNote, ingredientIndex: Int, isChecked: Boolean) {
        viewModelScope.launch {
            val currentIngredients = note.structuredData.ingredients.toMutableList()
            if (ingredientIndex in currentIngredients.indices) {
                currentIngredients[ingredientIndex] = currentIngredients[ingredientIndex].copy(isChecked = isChecked)
                val updatedData = note.structuredData.copy(ingredients = currentIngredients)
                repository.updateNote(note.copy(structuredData = updatedData))
            }
        }
    }

    fun updateStepDone(note: ReelNote, stepNumber: Int, isDone: Boolean) {
        viewModelScope.launch {
            val currentSteps = note.structuredData.steps.toMutableList()
            val stepIndex = currentSteps.indexOfFirst { it.stepNumber == stepNumber }
            if (stepIndex != -1) {
                currentSteps[stepIndex] = currentSteps[stepIndex].copy(isDone = isDone)
                val updatedData = note.structuredData.copy(steps = currentSteps)
                repository.updateNote(note.copy(structuredData = updatedData))
            }
        }
    }

    fun processSharedUrl(sharedText: String, onFinished: ((Long) -> Unit)? = null) {
        if (sharedText.isBlank()) return

        viewModelScope.launch {
            _isProcessing.value = true
            _processingStatus.value = "Récupération du Reel Instagram..."
            try {
                _processingStatus.value = "Analyse et structuration des notes..."
                val note = ReelExtractionPipeline.processReel(
                    sharedInput = sharedText,
                    apiKey = preferences.geminiApiKey.ifBlank { null },
                    preferredLanguage = preferences.preferredLanguage
                )

                _processingStatus.value = "Enregistrement dans vos notes..."
                val savedId = repository.saveNote(note)
                _newlyCreatedNoteId.emit(savedId)
                onFinished?.invoke(savedId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur lors du traitement du Reel"
            } finally {
                _isProcessing.value = false
                _processingStatus.value = ""
            }
        }
    }

    fun reloadSampleData() {
        viewModelScope.launch {
            repository.insertAll(SampleDataProvider.getSampleNotes())
        }
    }

    class Factory(
        private val repository: ReelNoteRepository,
        private val preferences: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReelNotesViewModel(repository, preferences) as T
        }
    }
}
