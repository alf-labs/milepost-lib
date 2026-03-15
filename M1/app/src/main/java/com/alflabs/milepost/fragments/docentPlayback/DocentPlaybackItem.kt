package com.alflabs.milepost.fragments.docentPlayback

import androidx.room.Ignore
import com.alflabs.milepost.fragments.docentTriggers.DocentTriggerItem
import com.alflabs.milepost.trigger.PlaybackTriggerMetadata

data class DocentPlaybackItem(
    val isPlaying: Boolean,
    val isPending: Boolean,
    // Fields from the PlayTriggerEntity
    val triggerId: Long,
    val audioSegmentId: Long,
    val playbackTrigger: PlaybackTriggerMetadata?,
    // Fields from the AudioSegmentEntity
    val descriptor: String,
    val persistableUri: String,
) {
    // These fields are internal and not populated by the DAO.
    // They are also not part of the default adapter equality diff callback.
    @Ignore var playDisplay: String = ""

    constructor(editorItem: DocentTriggerItem) : this(
        isPlaying = false,
        isPending = false,
        triggerId = editorItem.id,
        audioSegmentId = editorItem.audioSegmentId,
        playbackTrigger = editorItem.playbackTrigger,
        descriptor = editorItem.descriptor,
        persistableUri = editorItem.persistableUri,
    )
}
