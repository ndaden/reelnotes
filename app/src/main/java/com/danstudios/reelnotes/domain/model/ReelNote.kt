package com.danstudios.reelnotes.domain.model

data class ReelNote(
    val id: Long = 0,
    val reelUrl: String,
    val shortcode: String,
    val author: String? = null,
    val title: String,
    val category: NoteCategory = NoteCategory.GENERAL,
    val summary: String,
    val rawCaption: String = "",
    val markdownContent: String = "",
    val structuredData: StructuredNoteData = StructuredNoteData(),
    val thumbnailUrl: String? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
