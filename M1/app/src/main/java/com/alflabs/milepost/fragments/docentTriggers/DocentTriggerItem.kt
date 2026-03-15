package com.alflabs.milepost.fragments.docentTriggers

import androidx.room.Ignore
import com.alflabs.milepost.db.AudioSegmentMetadata
import com.alflabs.milepost.db.PlayTriggerEntity
import com.alflabs.milepost.trigger.PlaybackTriggerMetadata

/**
 * A derivative of [com.alflabs.milepost.db.PlayTriggerEntity]
 * suitable for the [DocentTriggersFragment] recycler view.
 */
data class DocentTriggerItem(
    // Fields from the PlayTriggerEntity
    val id: Long,
    val audioSegmentId: Long,
    val docentTrackId: Long,
    val playbackTrigger: PlaybackTriggerMetadata?,
    // Fields from the AudioSegmentEntity
    val descriptor: String,
    val persistableUri: String,
    val metadata: AudioSegmentMetadata? = null,
) {
    // These fields are internal and not populated by the DAO in getItems().
    // They are also not part of the default adapter equality diff callback.
    @Ignore var durationDisplay: String = ""
    @Ignore var durationMp: Float = 0f

    fun toEntity() : PlayTriggerEntity {
        return PlayTriggerEntity(
            id = id,
            audioSegmentId = audioSegmentId,
            docentTrackId = docentTrackId,
            playbackTrigger = playbackTrigger
        )
    }

    override fun toString(): String {
        return "DocentTriggerItem(id=$id, audioSegmentId=$audioSegmentId [$descriptor], docentTrackId=$docentTrackId, playbackTrigger=$playbackTrigger)"
    }
}
