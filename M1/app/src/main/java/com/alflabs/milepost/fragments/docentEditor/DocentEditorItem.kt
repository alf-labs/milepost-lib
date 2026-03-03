package com.alflabs.milepost.fragments.docentEditor

import androidx.room.Ignore
import com.alflabs.milepost.db.PlayTriggerEntity
import com.alflabs.milepost.db.PlaybackTriggerMetadata

/**
 * A derivative of [com.alflabs.milepost.db.PlayTriggerEntity]
 * suitable for the [DocentEditorFragment] recycler view.
 */
data class DocentEditorItem(
    val id: Long,
    val audioFileId: Long,
    val docentTrackId: Long,
    val playbackTrigger: PlaybackTriggerMetadata?,
) {
    // These fields are internal and not populated by the DAO in getItems().
    @Ignore var audioFileName: String? = null

    fun toEntity() : PlayTriggerEntity {
        return PlayTriggerEntity(
            id = id,
            audioFileId = audioFileId,
            docentTrackId = docentTrackId,
            playbackTrigger = playbackTrigger
        )
    }
}
