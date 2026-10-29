package com.alflabs.milepostlib.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.alflabs.milepostlib.trigger.PlaybackTriggerMetadata
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

@Entity(
    tableName = "play_triggers",
    foreignKeys = [
        ForeignKey(
            entity = AudioSegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["audioSegmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DocentTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["docentTrackId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlayTriggerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(index = true)
    val audioSegmentId: Long,

    @ColumnInfo(index = true)
    val docentTrackId: Long,

    @ColumnInfo
    val playbackTrigger: PlaybackTriggerMetadata? = null
) {
    // TBD toPojo()
}

/** A mirror of [PlayTriggerEntity] for JSON import/export. */
data class PlayTriggerEntityPojo @JsonCreator constructor(
    // TBD alias audioFileId
    @param:JsonProperty("audioSegmentId") val audioSegmentId: Long,
    @param:JsonProperty("docentTrackId") val docentTrackId: Long,
    @param:JsonProperty("playbackTrigger") val playbackTrigger: PlaybackTriggerMetadata? = null
) {
    // TBD toProto
}
