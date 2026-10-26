package com.alflabs.milepostlib.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alflabs.milepostlib.fragments.audioSegments.AudioSegmentItem
import kotlinx.coroutines.flow.Flow

/** All queries for the audio_segments table */
@Dao
interface AudioSegmentDao {
    @Insert
    suspend fun insert(audioFile: AudioSegmentEntity): Long

    @Update
    suspend fun update(audioFile: AudioSegmentEntity)

    @Query("UPDATE audio_segments SET descriptor = :descriptor, persistableUri = :persistableUri, metadata = :metadata WHERE id = :id")
    suspend fun updatePersistableUri(
        id: Long,
        descriptor: String,
        persistableUri: String,
        metadata: AudioSegmentMetadata
    )

    @Query("SELECT * FROM audio_segments WHERE id = :id")
    fun getById(id: Long): Flow<AudioSegmentEntity?>

    @Query("SELECT id, descriptor, lastAccessTime, persistableUri, metadata FROM audio_segments ORDER BY descriptor ASC")
    fun getItems(): Flow<List<AudioSegmentItem>>

    @Query("SELECT id, descriptor, lastAccessTime, persistableUri, metadata FROM audio_segments ORDER BY descriptor ASC")
    suspend fun getItemsAsync(): List<AudioSegmentItem>

    @Query("DELETE FROM audio_segments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
