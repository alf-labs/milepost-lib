package com.alflabs.milepost.trigger

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class PlaybackTriggerDirectionPojo @JsonCreator constructor(
    @param:JsonProperty("direction") val direction: String
) {
    constructor(proto: PlaybackTriggerDirection) : this(
        when(proto) {
            PlaybackTriggerDirection.DIRECTION_UNSPECIFIED -> "Both"
            PlaybackTriggerDirection.DIRECTION_TO_LOWER_MP -> "ToLowMP"
            PlaybackTriggerDirection.DIRECTION_TO_HIGHER_MP -> "ToHighMP"
            PlaybackTriggerDirection.UNRECOGNIZED -> "Unknown"
        }
    )

    fun toProto(): PlaybackTriggerDirection = when(direction) {
        "Both" -> PlaybackTriggerDirection.DIRECTION_UNSPECIFIED
        "ToLowMP" -> PlaybackTriggerDirection.DIRECTION_TO_LOWER_MP
        "ToHighMP" -> PlaybackTriggerDirection.DIRECTION_TO_HIGHER_MP
        else -> PlaybackTriggerDirection.UNRECOGNIZED
    }
}
