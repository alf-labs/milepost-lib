package com.alflabs.milepost.tracks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "play_triggers",
    foreignKeys = [
        ForeignKey(
            entity = AudioFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["audio_file_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DocentTrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["docent_track_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlayTriggerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "audio_file_id", index = true)
    val audioFileId: Long,

    @ColumnInfo(name = "docent_track_id", index = true)
    val docentTrackId: Long,

    @ColumnInfo(name = "playback_trigger")
    val playbackTrigger: String
)
