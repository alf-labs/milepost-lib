package com.alflabs.milepost.fragments.docentTriggers

import com.alflabs.milepost.db.PlayTriggerEntity
import com.alflabs.milepost.trigger.PlaybackTriggerMetadata

/**
 * A derivative of [com.alflabs.milepost.db.PlayTriggerEntity]
 * suitable for the [DocentTriggerFragment] recycler view.
 */
data class DocentTriggerItem(
    // Fields from the PlayTriggerEntity
    val id: Long,
    val audioFileId: Long,
    val docentTrackId: Long,
    val playbackTrigger: PlaybackTriggerMetadata?,
    // Fields from the AudioFileEntity
    val fileName: String,
    val persistableUri: String,
) {

    fun toEntity() : PlayTriggerEntity {
        return PlayTriggerEntity(
            id = id,
            audioFileId = audioFileId,
            docentTrackId = docentTrackId,
            playbackTrigger = playbackTrigger
        )
    }

    override fun toString(): String {
        return "DocentTriggerItem(id=$id, audioFileId=$audioFileId [$fileName], docentTrackId=$docentTrackId, playbackTrigger=$playbackTrigger)"
    }
}
