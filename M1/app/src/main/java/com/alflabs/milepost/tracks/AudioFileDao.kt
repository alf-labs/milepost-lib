package com.alflabs.milepost.tracks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alflabs.milepost.fragments.audioFiles.AudioFileItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioFileDao {
    @Insert
    suspend fun insert(audioFile: AudioFileEntity): Long

    @Update
    suspend fun update(audioFile: AudioFileEntity)

    @Query("UPDATE audio_files SET fileName = :fileName, persistableUri = :persistableUri WHERE id = :id")
    suspend fun updatePersistableUri(id: Long, fileName: String, persistableUri: String)

    @Query("SELECT * FROM audio_files WHERE id = :id")
    fun getById(id: Long): Flow<AudioFileEntity?>

    @Query("SELECT id, fileName, lastAccessTime, persistableUri FROM audio_files ORDER BY fileName ASC")
    fun getItems(): Flow<List<AudioFileItem>>

    @Query("DELETE FROM audio_files WHERE id = :id")
    suspend fun deleteById(id: Long)
}
