package com.alflabs.milepost.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alflabs.milepost.fragments.docentEditor.DocentEditorItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayTriggerDao {
    @Insert
    suspend fun insert(playTrigger: PlayTriggerEntity): Long

    @Update
    suspend fun update(playTrigger: PlayTriggerEntity)

    @Query("SELECT COUNT(*) FROM play_triggers WHERE audioFileId = :audioFileId")
    suspend fun countTriggersForAudioFile(audioFileId: Long): Int

    @Query("SELECT * FROM play_triggers WHERE audioFileId = :audioFileId")
    fun getTriggersForAudioFile(audioFileId: Long): Flow<List<PlayTriggerEntity>>

    @Query("SELECT id, audioFileId, docentTrackId, playbackTrigger FROM play_triggers WHERE docentTrackId = :docentTrackId")
    fun getTriggersForDocentTrack(docentTrackId: Long): Flow<List<DocentEditorItem>>

    @Query("DELETE FROM play_triggers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
