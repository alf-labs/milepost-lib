package com.alflabs.milepostlib.fragments.docentTriggers

import androidx.room.Ignore
import com.alflabs.milepostlib.db.AudioSegmentMetadata
import com.alflabs.milepostlib.db.PlayTriggerEntity
import com.alflabs.milepostlib.trigger.PlaybackTriggerMetadata
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A derivative of [com.alflabs.milepostlib.db.PlayTriggerEntity]
 * suitable for the [DocentTriggersFragment] recycler view.
 */
data class DocentTriggerItem(
    // Fields from the PlayTriggerEntity
    @field:JsonIgnore val id: Long = 0,
    @field:JsonIgnore val audioSegmentId: Long = 0,
    @field:JsonIgnore val docentTrackId: Long = 0,
    @param:JsonProperty("playbackTrigger") val playbackTrigger: PlaybackTriggerMetadata? = null,
    // Fields from the AudioSegmentEntity
    @param:JsonProperty("audioDescriptor") val audioDescriptor: String,
    @field:JsonIgnore val persistableUri: String = "",
    @param:JsonProperty("metadata") val metadata: AudioSegmentMetadata? = null,
) {
    // These fields are internal and not populated by the DAO in getItems().
    // They are also not part of the default adapter equality diff callback.
    @field:JsonIgnore
    @Ignore var durationDisplay: String = ""
    @field:JsonIgnore
    @Ignore var durationMp: Float = 0f

    fun toEntity() : PlayTriggerEntity {
        return PlayTriggerEntity(
            id = id,
            audioSegmentId = audioSegmentId,
            docentTrackId = docentTrackId,
            playbackTrigger = playbackTrigger
        )
    }

    fun toItem(
        docentTrackId: Long,
        descriptorsToId: Map<String, Long>,
        descriptorsToUri: Map<String, String>,
    ) : DocentTriggerItem {
        return this.copy(
            id = 0,
            audioSegmentId = descriptorsToId[audioDescriptor]!!,
            persistableUri = descriptorsToUri[audioDescriptor]!!,
            docentTrackId = docentTrackId,
        )
    }


    override fun toString(): String {
        return "DocentTriggerItem(id=$id, audioSegmentId=$audioSegmentId [$audioDescriptor], docentTrackId=$docentTrackId, playbackTrigger=$playbackTrigger)"
    }
}
