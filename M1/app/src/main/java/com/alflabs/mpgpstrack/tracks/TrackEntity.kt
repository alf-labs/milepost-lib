package com.alflabs.mpgpstrack.tracks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alflabs.mpgpstrack.location.LocationDataList
import java.util.UUID


// Doc: https://developer.android.com/training/data-storage/room/defining-data

@Entity(tableName = "tracks")
data class TrackEntity(
    // Note: according to the autoGenerate doc, setting the value to 0 forces it to be generated.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Mandatory UUID for this track.
    @ColumnInfo
    val uuid: String = UUID.randomUUID().toString(),

    // Mandatory creation date as an ISO date string.
    @ColumnInfo
    val createdDateISO: String,

    // Optional last updated date as an ISO date string.
    @ColumnInfo
    val updateDateISO: String? = null,

    // User label. Can be empty in which case we'll display "Track #id" as a label.
    @ColumnInfo
    val label: String? = null,

    // The min MilePost computed or set for this track.
    @ColumnInfo
    val minMP: Float? = null,

    // The max MilePost computed or set for this track.
    @ColumnInfo
    val maxMP: Float? = null,

    @ColumnInfo
    val locationList: LocationDataList? = null,
)
