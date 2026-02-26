package com.alflabs.milepost.fragments.docentEdit

import androidx.room.Ignore
import com.alflabs.milepost.db.PlaybackTriggerMetadata

/**
 * A derivative of [com.alflabs.milepost.db.PlayTriggerEntity]
 * suitable for the [DocentEditFragment] recycler view.
 */
data class DocentEditItem(
    val id: Long,
    val audioFileId: Long,
    val docentTrackId: Long,
    val playbackTrigger: PlaybackTriggerMetadata?,
) {
    // These fields are internal and not populated by the DAO in getItems().
    @Ignore var internalStuff: String? = null
}
