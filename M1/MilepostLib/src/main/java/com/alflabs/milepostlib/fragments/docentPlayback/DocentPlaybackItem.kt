package com.alflabs.milepostlib.fragments.docentPlayback

import androidx.room.Ignore
import com.alflabs.milepostlib.fragments.docentTriggers.DocentTriggerItem
import com.alflabs.milepostlib.trigger.PlaybackTriggerMetadata

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
    @Ignore var currentMs: Int = 0
    @Ignore var durationMs: Int = 0
    @Ignore var isPaused: Boolean = false

    val btnAction: BtnAction
        get() = when {
            isPlaying && !isPaused -> BtnAction.Pause
            isPlaying && isPaused -> BtnAction.Unpause
            else -> BtnAction.ForcePlay
        }

    constructor(editorItem: DocentTriggerItem) : this(
        isPlaying = false,
        isPending = false,
        triggerId = editorItem.id,
        audioSegmentId = editorItem.audioSegmentId,
        playbackTrigger = editorItem.playbackTrigger,
        descriptor = editorItem.descriptor,
        persistableUri = editorItem.persistableUri,
    )

    enum class BtnAction {
        /** If not isPlaying --> can force next. */
        ForcePlay,
        /** If isPlaying --> can pause. */
        Pause,
        /** If isPlaying and isPaused --> can unpause. */
        Unpause,
    }
}
