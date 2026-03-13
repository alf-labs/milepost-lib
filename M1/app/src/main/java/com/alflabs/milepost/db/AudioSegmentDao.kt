package com.alflabs.milepost.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alflabs.milepost.fragments.audioSegments.AudioSegmentItem
import kotlinx.coroutines.flow.Flow

/** All queries for the audio_segments table */
@Dao
interface AudioSegmentDao {
    @Insert
    suspend fun insert(audioFile: AudioSegmentEntity): Long

    @Update
    suspend fun update(audioFile: AudioSegmentEntity)

    @Query("UPDATE audio_segments SET fileName = :fileName, persistableUri = :persistableUri WHERE id = :id")
    suspend fun updatePersistableUri(id: Long, fileName: String, persistableUri: String)

    @Query("SELECT * FROM audio_segments WHERE id = :id")
    fun getById(id: Long): Flow<AudioSegmentEntity?>

    @Query("SELECT id, fileName, lastAccessTime, persistableUri, metadata FROM audio_segments ORDER BY fileName ASC")
    fun getItems(): Flow<List<AudioSegmentItem>>

    @Query("DELETE FROM audio_segments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
