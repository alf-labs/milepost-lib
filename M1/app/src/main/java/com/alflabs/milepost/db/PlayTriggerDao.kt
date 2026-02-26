package com.alflabs.milepost.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alflabs.milepost.fragments.docentEdit.DocentEditItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayTriggerDao {
    @Insert
    suspend fun insert(playTrigger: PlayTriggerEntity): Long

    @Query("SELECT * FROM play_triggers WHERE audioFileId = :audioFileId")
    fun getTriggersForAudioFile(audioFileId: Long): Flow<List<PlayTriggerEntity>>

    @Query("SELECT id, audioFileId, docentTrackId, playbackTrigger FROM play_triggers WHERE docentTrackId = :docentTrackId")
    fun getTriggersForDocentTrack(docentTrackId: Long): Flow<List<DocentEditItem>>

    @Query("DELETE FROM play_triggers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
