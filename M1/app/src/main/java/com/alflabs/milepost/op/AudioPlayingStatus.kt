package com.alflabs.milepost.op

/**
 * Command that indicates which audio file is currently playing or scheduled to play.
 *
 * The trigger id is only useful so that we can propagate it back to the UI
 * and to identify which trigger is current/pending.
 */
data class AudioPlayingStatus(
    val type: Type,
    val isPlaying: Boolean,
    val triggerId: Long,
) {
    enum class Type {
        Current,
        Pending,
    }
}
