package com.alflabs.milepost.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "audio_segments")
data class AudioSegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo
    val uuid: String = UUID.randomUUID().toString(),

    @ColumnInfo
    val fileName: String,

    @ColumnInfo
    val lastAccessTime: String,

    @ColumnInfo
    val persistableUri: String,

    @ColumnInfo
    val metadata: AudioSegmentMetadata? = null,
)
