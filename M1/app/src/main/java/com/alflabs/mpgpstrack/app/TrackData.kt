package com.alflabs.mpgpstrack.app

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
)
