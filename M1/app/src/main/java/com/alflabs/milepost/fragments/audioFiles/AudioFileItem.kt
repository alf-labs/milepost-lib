package com.alflabs.milepost.fragments.audioFiles

import androidx.room.Ignore

/**
 * A derivative of [com.alflabs.milepost.db.AudioFileEntity] suitable for the AudioFileFragment recycler view.
 */
data class AudioFileItem(
    val id: Long,
    val fileName: String,
    val lastAccessTime: String,
    val persistableUri: String) {
    // These fields are internal and not populated by the DAO in getItems().
    @Ignore var uriValid: Boolean? = null
    @Ignore var isPlaying: Boolean = false
}

