package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_media WHERE isVideo = :isVideo AND isTrashed = 0 ORDER BY dateAdded DESC")
    fun getMediaByType(isVideo: Boolean): Flow<List<VaultMediaItem>>

    @Query("SELECT * FROM vault_media WHERE isTrashed = 0 ORDER BY dateAdded DESC")
    fun getAllActiveMedia(): Flow<List<VaultMediaItem>>

    @Query("SELECT * FROM vault_media WHERE isTrashed = 1 ORDER BY dateAdded DESC")
    fun getTrashedMedia(): Flow<List<VaultMediaItem>>

    @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): VaultMediaItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: VaultMediaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(items: List<VaultMediaItem>)

    @Update
    suspend fun updateMedia(item: VaultMediaItem)

    @Delete
    suspend fun deleteMedia(item: VaultMediaItem)

    @Query("DELETE FROM vault_media WHERE id = :id")
    suspend fun deleteMediaById(id: Long)

    @Query("UPDATE vault_media SET isTrashed = :trashed WHERE id = :id")
    suspend fun setTrashedStatus(id: Long, trashed: Boolean)

    @Query("DELETE FROM vault_media WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("SELECT DISTINCT album FROM vault_media WHERE isTrashed = 0")
    fun getAlbums(): Flow<List<String>>

    // Notes
    @Query("SELECT * FROM vault_notes ORDER BY dateModified DESC")
    fun getAllNotes(): Flow<List<VaultNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VaultNote): Long

    @Update
    suspend fun updateNote(note: VaultNote)

    @Delete
    suspend fun deleteNote(note: VaultNote)

    // Intruder attempts
    @Query("SELECT * FROM intruder_attempts ORDER BY timestamp DESC")
    fun getIntruderAttempts(): Flow<List<IntruderAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntruderAttempt(attempt: IntruderAttempt)

    @Query("DELETE FROM intruder_attempts")
    suspend fun clearIntruderAttempts()
}
