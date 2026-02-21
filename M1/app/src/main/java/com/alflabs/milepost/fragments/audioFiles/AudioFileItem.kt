package com.alflabs.milepost.fragments.audioFiles

/**
 * A derivative of [com.alflabs.milepost.tracks.AudioFileEntity] suitable for the AudioFileFragment recycler view.
 */
data class AudioFileItem(
    val id: Long,
    val fileName: String,
    val lastAccessTime: String,
    val persistableUri: String,
    var uriValid: Boolean? = null,
)

