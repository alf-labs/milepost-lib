package com.alflabs.milepost.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.alflabs.milepost.trigger.PlaybackTriggerMetadata
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

@Entity(
    tableName = "play_triggers",
    foreignKeys = [
        ForeignKey(
            entity = AudioFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["audioFileId"],
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
    val audioFileId: Long,

    @ColumnInfo(index = true)
    val docentTrackId: Long,

    @ColumnInfo
    val playbackTrigger: PlaybackTriggerMetadata? = null
) {
    // TBD toPojo()
}

/** A mirror of [PlayTriggerEntity] for JSON import/export. */
data class PlayTriggerEntityPojo @JsonCreator constructor(
    @param:JsonProperty("audioFileId") val audioFileId: Long,
    @param:JsonProperty("docentTrackId") val docentTrackId: Long,
    @param:JsonProperty("playbackTrigger") val playbackTrigger: PlaybackTriggerMetadata? = null
) {
    // TBD toProto
}
