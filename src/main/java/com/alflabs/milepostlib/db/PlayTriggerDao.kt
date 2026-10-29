package com.alflabs.milepostlib.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.alflabs.milepostlib.fragments.docentTriggers.DocentTriggerItem
import kotlinx.coroutines.flow.Flow

/** All queries for the play_triggers table */
@Dao
interface PlayTriggerDao {
    @Insert
    suspend fun insert(playTrigger: PlayTriggerEntity): Long

    @Update
    suspend fun update(playTrigger: PlayTriggerEntity)

    @Query("SELECT * FROM play_triggers WHERE audioSegmentId = :audioSegmentId")
    fun getTriggersForAudioSegment(audioSegmentId: Long): Flow<List<PlayTriggerEntity>>

    @Query("SELECT COUNT(*) FROM play_triggers WHERE audioSegmentId = :audioSegmentId")
    suspend fun countTriggersForAudioSegment(audioSegmentId: Long): Int

    @Query("""
        SELECT T.id, T.audioSegmentId, T.docentTrackId, T.playbackTrigger, A.descriptor, A.persistableUri, A.metadata
        FROM play_triggers AS T
        INNER JOIN audio_segments AS A ON T.audioSegmentId = A.id
        WHERE T.docentTrackId = :docentTrackId
        ORDER BY A.descriptor ASC
        """)
    fun getTriggersForDocentTrack(docentTrackId: Long): Flow<List<DocentTriggerItem>>

    @Query("DELETE FROM play_triggers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
