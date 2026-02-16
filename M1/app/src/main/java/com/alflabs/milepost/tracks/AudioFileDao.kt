package com.alflabs.milepost.tracks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioFileDao {
    @Insert
    suspend fun insert(audioFile: AudioFileEntity): Long

    @Update
    suspend fun update(audioFile: AudioFileEntity)

    @Query("SELECT * FROM audio_files WHERE id = :id")
    fun getById(id: Long): Flow<AudioFileEntity?>

    @Query("SELECT * FROM audio_files ORDER BY lastAccessDate DESC")
    suspend fun getAll(): List<AudioFileEntity>

    @Query("DELETE FROM audio_files WHERE id = :id")
    suspend fun deleteById(id: Long)
}
