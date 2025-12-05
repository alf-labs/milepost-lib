package com.alflabs.mpgpstrack.tracks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


// Doc: https://developer.android.com/training/data-storage/room/defining-data


@Entity(tableName = "tracks")
data class TrackData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo
    val name: String,

    @ColumnInfo(defaultValue = "")
    val date: String = "",

    @ColumnInfo(defaultValue = "0.0")
    val minMP: Float = 0.0f,

    @ColumnInfo(defaultValue = "0.0")
    val maxMP: Float = 0.0f,
)
