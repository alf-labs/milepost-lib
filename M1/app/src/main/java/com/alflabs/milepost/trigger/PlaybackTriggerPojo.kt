package com.alflabs.milepost.trigger

import com.alflabs.milepost.db.PlayTriggerEntity
import com.alflabs.milepost.fragments.docentTriggers.DocentTriggerItem
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/** A representation of [PlayTriggerEntity]
 * combined with [PlaybackTriggerMetadata] for JSON import/export. */
data class PlaybackTriggerPojo @JsonCreator constructor(
    @param:JsonProperty("audioDescriptor") val audioDescriptor: String,
    @param:JsonProperty("includeInColdStart") val includeInColdStart: Boolean,
    @param:JsonProperty("mpMin") val mpMin: Float,
    @param:JsonProperty("mpMax") val mpMax: Float,
    @param:JsonProperty("mpDirection") val mpDirection: PlaybackTriggerDirectionPojo,
) {
    constructor(item: DocentTriggerItem) : this(
        audioDescriptor = item.descriptor,
        includeInColdStart = item.playbackTrigger!!.includeInColdStart,
        mpMin = item.playbackTrigger.mpMin,
        mpMax = item.playbackTrigger.mpMax,
        mpDirection = PlaybackTriggerDirectionPojo(item.playbackTrigger.mpDirection)
    )

    fun toItem(
        docentTrackId: Long,
        descriptorsToId: Map<String, Long>,
        descriptorsToUri: Map<String, String>,
    ) : DocentTriggerItem {
        return DocentTriggerItem(
            id = 0,
            descriptor = this@PlaybackTriggerPojo.audioDescriptor,
            audioSegmentId = descriptorsToId[this@PlaybackTriggerPojo.audioDescriptor]!!,
            persistableUri = descriptorsToUri[this@PlaybackTriggerPojo.audioDescriptor]!!,
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
