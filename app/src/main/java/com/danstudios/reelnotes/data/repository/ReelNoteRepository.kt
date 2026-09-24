package com.danstudios.reelnotes.data.repository

import com.danstudios.reelnotes.data.local.ReelNoteDao
import com.danstudios.reelnotes.data.local.toDomainModel
import com.danstudios.reelnotes.data.local.toEntity
import com.danstudios.reelnotes.domain.model.NoteCategory
import com.danstudios.reelnotes.domain.model.ReelNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReelNoteRepository(private val reelNoteDao: ReelNoteDao) {

    fun getAllNotes(): Flow<List<ReelNote>> {
        return reelNoteDao.getAllNotes().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getNoteById(id: Long): Flow<ReelNote?> {
        return reelNoteDao.getNoteById(id).map { it?.toDomainModel() }
    }

    fun getFavoriteNotes(): Flow<List<ReelNote>> {
        return reelNoteDao.getFavoriteNotes().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getNotesByCategory(category: NoteCategory): Flow<List<ReelNote>> {
        return reelNoteDao.getNotesByCategory(category.name).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun searchNotes(query: String): Flow<List<ReelNote>> {
        return reelNoteDao.searchNotes(query.trim()).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun saveNote(note: ReelNote): Long {
        return reelNoteDao.insertNote(note.toEntity())
    }

    suspend fun updateNote(note: ReelNote) {
        reelNoteDao.updateNote(note.toEntity())
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        reelNoteDao.updateFavorite(id = id, isFavorite = isFavorite)
    }

    suspend fun deleteNote(id: Long) {
        reelNoteDao.deleteById(id)
    }

    suspend fun insertAll(notes: List<ReelNote>) {
        for (note in notes) {
            reelNoteDao.insertNote(note.toEntity())
        }
    }
}
