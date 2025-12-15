package com.alflabs.mpgpstrack.tracks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.alflabs.mpgpstrack.location.LocationDataList
import com.alflabs.mpgpstrack.location.LocationDataListPojo
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
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
    val updatedDateISO: String? = null,

    // User label. Can be empty in which case we'll display "Track #id" as a label.
    @ColumnInfo
    val label: String? = null,

    // Optional summary info for list tracks fragment.
    @ColumnInfo
    @JsonIgnore
    val summary: TrackSummary? = null,

    // Actual location data (empty before first gps fix).
    @ColumnInfo
    @JsonIgnore
    val locationList: LocationDataList? = null,

    @Ignore
    @field:JsonProperty("locationList")
    var locationListPojo: LocationDataListPojo? = null,
) {
    // Room DAO needs a constructor without the ignored fields.
    constructor(
        id: Long,
        uuid: String,
        createdDateISO: String,
        updatedDateISO: String?,
        label: String?,
        summary: TrackSummary?,
        locationList: LocationDataList?,
    ) : this(
        id,
        uuid,
        createdDateISO,
        updatedDateISO,
        label,
        summary,
        locationList,
        null)

    fun fillPojo() {
        locationListPojo = locationList?.let { LocationDataListPojo(it) }
    }
}

// A minimal version of TrackEntity for displaying in the TracksFragment.
data class TrackDisplayInfo(
    val id: Long = 0,
    val createdDateISO: String,
    val label: String? = null,
    val summary: TrackSummary? = null,
)
