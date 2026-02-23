package com.alflabs.milepost.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_files")
data class AudioFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo
    val fileName: String,

    @ColumnInfo
    val lastAccessTime: String,

    @ColumnInfo
    val persistableUri: String,

    @ColumnInfo
    val metadata: AudioFileMetadata? = null,
)

