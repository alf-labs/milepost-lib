package com.alflabs.milepost.fragments.audioSegments

import androidx.room.Ignore
import com.alflabs.milepost.db.AudioSegmentMetadata
import com.alflabs.milepost.utils.Time

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
        // Regex match:  name     .abcd9   ?    [ 0          ; 12345    ] ?$
        // Regex group:  1                        2            3
        val descRE = """^(.*?)(?:\.\w{1,4})?(?:\[([0-9]+)?(?:;([0-9]+))?])?$""".toRegex()

        fun toDisplayTitle(desc: String): String {
            return descRE.matchEntire(desc)?.let { match ->
                var name = match.groupValues[1]
                var start = match.groupValues[2]    // empty string if missing
                var end = match.groupValues[3]      // empty string if missing

                if (start.isNotBlank() && start != "0") {
                    start.toLongOrNull()?.let { ms ->
                        start = Time.formatMillis(ms)
                    } ?: run {
                        start = ""
                    }
                } else {
                    start = ""
                }

                end.toLongOrNull()?.let { ms ->
                    end = Time.formatMillis(ms)
                } ?: run {
                    end = ""
                }

                if (start.isNotEmpty() && end.isNotEmpty()) {
                    name += " ($start - $end)"
                } else if (start.isNotEmpty()) {
                    name += " ($start)"
                } else if (end.isNotEmpty()) {
                    name += " ($end)"
                }

                name.ifBlank { desc }
            } ?: run {
                desc
            }
        }
    }

    /** Returns the base filename of the descriptor, without the extension nor timestamp. */
    fun displayTitle(): String = toDisplayTitle(descriptor)
}
