package com.alflabs.milepost.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alflabs.milepost.fragments.docentEditor.DocentEditorItem
import kotlinx.coroutines.flow.Flow

/** All queries for the play_triggers table */
@Dao
interface PlayTriggerDao {
    @Insert
    suspend fun insert(playTrigger: PlayTriggerEntity): Long

    @Update
    suspend fun update(playTrigger: PlayTriggerEntity)

    @Query("SELECT * FROM play_triggers WHERE audioFileId = :audioFileId")
    fun getTriggersForAudioFile(audioFileId: Long): Flow<List<PlayTriggerEntity>>

    @Query("SELECT COUNT(*) FROM play_triggers WHERE audioFileId = :audioFileId")
    suspend fun countTriggersForAudioFile(audioFileId: Long): Int

    @Query("""
        SELECT T.id, T.audioFileId, T.docentTrackId, T.playbackTrigger, A.fileName
        FROM play_triggers AS T
        INNER JOIN audio_files AS A ON T.audioFileId = A.id
        WHERE T.docentTrackId = :docentTrackId
        ORDER BY A.fileName ASC
        """)
    fun getTriggersForDocentTrack(docentTrackId: Long): Flow<List<DocentEditorItem>>

    @Query("DELETE FROM play_triggers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
