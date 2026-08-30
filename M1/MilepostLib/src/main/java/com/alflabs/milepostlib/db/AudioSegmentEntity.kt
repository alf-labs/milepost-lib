package com.alflabs.milepostlib.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_segments")
data class AudioSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // The descriptor is a "reasonably unique" reference composed of the filename and the
    // start-end timestamps of the segment (which currently is always its entirety).
    // These are used to identify segments when importing JSON docent tracks.
    @ColumnInfo
    val descriptor: String,

    @ColumnInfo
    val lastAccessTime: String,

    @ColumnInfo
    val persistableUri: String,

    @ColumnInfo
    val metadata: AudioSegmentMetadata? = null,
)
