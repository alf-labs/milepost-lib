package com.alflabs.milepost.tracks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlayTriggerDao {
    @Insert
    suspend fun insert(playTrigger: PlayTriggerEntity): Long

    @Query("SELECT * FROM play_triggers WHERE audioFileId = :audioFileId")
    suspend fun getTriggersForAudioFile(audioFileId: Long): List<PlayTriggerEntity>

    @Query("SELECT * FROM play_triggers WHERE docentTrackId = :docentTrackId")
    suspend fun getTriggersForDocentTrack(docentTrackId: String): List<PlayTriggerEntity>

    @Query("DELETE FROM play_triggers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
