package com.alflabs.milepost.fragments.docentEditor

import com.alflabs.milepost.db.PlayTriggerEntity
import com.alflabs.milepost.trigger.PlaybackTriggerMetadata

/**
 * A derivative of [com.alflabs.milepost.db.PlayTriggerEntity]
 * suitable for the [DocentEditorFragment] recycler view.
 */
data class DocentEditorItem(
    // Fields from the PlayTriggerEntity
    val id: Long,
    val audioFileId: Long,
    val docentTrackId: Long,
    val playbackTrigger: PlaybackTriggerMetadata?,
    // Fields from the AudioFileEntity
    val fileName: String,
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
        return "DocentEditorItem(id=$id, audioFileId=$audioFileId [$fileName], docentTrackId=$docentTrackId, playbackTrigger=$playbackTrigger)"
    }
}
