package com.alflabs.milepostlib.op

/**
 * Command that indicates which audio file is currently playing or scheduled to play.
 *
 * The trigger id is only useful so that we can propagate it back to the UI
 * and to identify which trigger is current/pending.
 */
data class AudioPlayingStatus(
    val type: Type,
    val isPlaying: Boolean,
    val isPaused: Boolean,
    val triggerId: Long,
    val currentMs: Int = 0,
    val durationMs: Int = 0,
) {
    enum class Type {
        Current,
        Pending,
    }
}
