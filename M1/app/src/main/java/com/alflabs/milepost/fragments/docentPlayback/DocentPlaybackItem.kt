package com.alflabs.milepost.fragments.docentPlayback

import com.alflabs.milepost.fragments.docentTriggers.DocentTriggerItem
import com.alflabs.milepost.trigger.PlaybackTriggerMetadata

data class DocentPlaybackItem(
    val isPlaying: Boolean,
    val isPending: Boolean,
    // Fields from the PlayTriggerEntity
    val triggerId: Long,
    val audioFileId: Long,
    val playbackTrigger: PlaybackTriggerMetadata?,
    // Fields from the AudioFileEntity
    val fileName: String,
    val persistableUri: String,
) {
    constructor(editorItem: DocentTriggerItem) : this(
        isPlaying = false,
        isPending = false,
        triggerId = editorItem.id,
        audioFileId = editorItem.audioFileId,
        playbackTrigger = editorItem.playbackTrigger,
        fileName = editorItem.fileName,
        persistableUri = editorItem.persistableUri,
    )
}
