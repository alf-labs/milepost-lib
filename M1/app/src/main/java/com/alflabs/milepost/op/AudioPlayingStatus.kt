package com.alflabs.milepost.op

/**
 * Command that indicates which audio file is currently playing or scheduled to play.
 *
 * The ids only used as unique identifiers to differentiate files.
 */
data class AudioPlayingStatus(
    val type: Type,
    val isPlaying: Boolean,
    val triggerId: Long,
    val audioFileId: Long,
) {
    enum class Type {
        Current,
        Pending,
    }
}
