package com.alflabs.milepost.fragments.docentEditor

import androidx.room.Ignore
import com.alflabs.milepost.db.PlayTriggerEntity
import com.alflabs.milepost.db.PlaybackTriggerMetadata

/**
 * A derivative of [com.alflabs.milepost.db.PlayTriggerEntity]
 * suitable for the [DocentEditorFragment] recycler view.
 */
data class DocentEditorItem(
    val id: Long,
    val audioFileId: Long,
    val docentTrackId: Long,
    val playbackTrigger: PlaybackTriggerMetadata?,
) {
    // These fields are internal and not populated by the DAO in getItems().
    @Ignore var audioFileName: String? = null

    fun toEntity() : PlayTriggerEntity {
        return PlayTriggerEntity(
            id = id,
            audioFileId = audioFileId,
            docentTrackId = docentTrackId,
            playbackTrigger = playbackTrigger
        )
    }

    override fun toString(): String {
        return "DocentEditorItem(id=$id, audioFileId=$audioFileId [$audioFileName], docentTrackId=$docentTrackId, playbackTrigger=$playbackTrigger)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocentEditorItem

        if (id != other.id) return false
        if (audioFileId != other.audioFileId) return false
        if (docentTrackId != other.docentTrackId) return false
        if (playbackTrigger != other.playbackTrigger) return false
        if (audioFileName != other.audioFileName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + audioFileId.hashCode()
        result = 31 * result + docentTrackId.hashCode()
        result = 31 * result + (playbackTrigger?.hashCode() ?: 0)
        result = 31 * result + (audioFileName?.hashCode() ?: 0)
        return result
    }


}
