package com.alflabs.milepost.fragments.audioSegments

import androidx.room.Ignore
import com.alflabs.milepost.db.AudioSegmentMetadata

/**
 * A derivative of [com.alflabs.milepost.db.AudioSegmentEntity]
 * suitable for the [AudioSegmentsFragment] recycler view.
 */
data class AudioSegmentItem(
    val id: Long,
    val descriptor: String,
    val lastAccessTime: String,
    val persistableUri: String,
    val metadata: AudioSegmentMetadata?,
) {
    // These fields are internal and not populated by the DAO in getItems().
    @Ignore var uriValid: Boolean? = null
    @Ignore var isPlaying: Boolean = false

    companion object {
        // Regex match:                  .abcd9   ?    [0;12345] ?$
        val descriptorExtension = """(?:\.\w{0,4})?(?:\[[0-9;]+])?$""".toRegex()
    }

    /** Returns the base filename of the descriptor, without the extension nor timestamp. */
    fun displayTitle(): String {
        return descriptor.replace(descriptorExtension, "")
    }
}
