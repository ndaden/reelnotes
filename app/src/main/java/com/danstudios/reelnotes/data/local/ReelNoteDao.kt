package com.danstudios.reelnotes.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelNoteDao {

    @Query("SELECT * FROM reel_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<ReelNoteEntity>>

    @Query("SELECT * FROM reel_notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<ReelNoteEntity?>

    @Query("SELECT * FROM reel_notes WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteNotes(): Flow<List<ReelNoteEntity>>

    @Query("SELECT * FROM reel_notes WHERE category = :category ORDER BY createdAt DESC")
    fun getNotesByCategory(category: String): Flow<List<ReelNoteEntity>>

    @Query("""
        SELECT * FROM reel_notes 
        WHERE title LIKE '%' || :query || '%' 
           OR rawCaption LIKE '%' || :query || '%' 
           OR tagsJson LIKE '%' || :query || '%' 
           OR author LIKE '%' || :query || '%' 
        ORDER BY createdAt DESC
    """)
    fun searchNotes(query: String): Flow<List<ReelNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ReelNoteEntity): Long

    @Update
    suspend fun updateNote(note: ReelNoteEntity)

    @Delete
    suspend fun deleteNote(note: ReelNoteEntity)

    @Query("DELETE FROM reel_notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE reel_notes SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())
}
