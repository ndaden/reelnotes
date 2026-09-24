package com.danstudios.reelnotes.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.danstudios.reelnotes.domain.model.NoteCategory
import com.danstudios.reelnotes.domain.model.ReelNote
import com.danstudios.reelnotes.domain.model.StructuredNoteData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "reel_notes")
data class ReelNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reelUrl: String,
    val shortcode: String,
    val author: String? = null,
    val title: String,
    val category: String,
    val summary: String,
    val rawCaption: String = "",
    val markdownContent: String = "",
    val structuredJson: String = "{}",
    val thumbnailUrl: String? = null,
    val tagsJson: String = "[]",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

private val localJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

fun ReelNote.toEntity(): ReelNoteEntity {
    return ReelNoteEntity(
        id = id,
        reelUrl = reelUrl,
        shortcode = shortcode,
        author = author,
        title = title,
        category = category.name,
        summary = summary,
        rawCaption = rawCaption,
        markdownContent = markdownContent,
        structuredJson = localJson.encodeToString(structuredData),
        thumbnailUrl = thumbnailUrl,
        tagsJson = localJson.encodeToString(tags),
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ReelNoteEntity.toDomainModel(): ReelNote {
    val structuredData = try {
        localJson.decodeFromString<StructuredNoteData>(structuredJson)
    } catch (_: Exception) {
        StructuredNoteData(summary = summary)
    }

    val tags = try {
        localJson.decodeFromString<List<String>>(tagsJson)
    } catch (_: Exception) {
        emptyList()
    }

    return ReelNote(
        id = id,
        reelUrl = reelUrl,
        shortcode = shortcode,
        author = author,
        title = title,
        category = NoteCategory.fromString(category),
        summary = summary,
        rawCaption = rawCaption,
        markdownContent = markdownContent,
        structuredData = structuredData,
        thumbnailUrl = thumbnailUrl,
        tags = tags,
        isFavorite = isFavorite,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
