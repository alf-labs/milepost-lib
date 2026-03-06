package com.alflabs.milepost.trigger

import com.alflabs.milepost.db.PlayTriggerEntity
import com.alflabs.milepost.fragments.docentEditor.DocentEditorItem
import com.fasterxml.jackson.annotation.JsonCreator

/** A representation of [PlayTriggerEntity]
 * combined with [PlaybackTriggerMetadata] for JSON import/export. */
data class PlaybackTriggerPojo @JsonCreator constructor(
    val audioFileName: String,
    val includeInColdStart: Boolean,
    val mpMin: Float,
    val mpMax: Float,
    val mpDirection: PlaybackTriggerDirectionPojo,
) {
    constructor(item: DocentEditorItem) : this(
        audioFileName = item.fileName,
        includeInColdStart = item.playbackTrigger!!.includeInColdStart,
        mpMin = item.playbackTrigger.mpMin,
        mpMax = item.playbackTrigger.mpMax,
        mpDirection = PlaybackTriggerDirectionPojo(item.playbackTrigger.mpDirection)
    )

    fun toItem(docentTrackId: Long, audioFileNameToId: Map<String, Long>) : DocentEditorItem {
        return DocentEditorItem(
            id = 0,
            fileName = this@PlaybackTriggerPojo.audioFileName,
            audioFileId = audioFileNameToId[this@PlaybackTriggerPojo.audioFileName]!!,
            docentTrackId = docentTrackId,
            playbackTrigger = playbackTriggerMetadata {
                includeInColdStart = this@PlaybackTriggerPojo.includeInColdStart
                mpMin = this@PlaybackTriggerPojo.mpMin
                mpMax = this@PlaybackTriggerPojo.mpMax
                mpDirection = this@PlaybackTriggerPojo.mpDirection.toProto()
            }
        )
    }
}

