package com.alflabs.milepost.tracks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alflabs.milepost.location.LocationDataList
import com.alflabs.milepost.location.LocationDataListPojo
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID


// Doc: https://developer.android.com/training/data-storage/room/defining-data

@Entity(tableName = "gps_tracks")
data class GpsTrackEntity(
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
    val trackLabel: String? = null,

    // Start and end labels.
    @ColumnInfo(defaultValue = "null")
    val startLabel: String? = null,
    @ColumnInfo(defaultValue = "null")
    val endLabel: String? = null,

    // Optional summary info for list tracks fragment.
    @ColumnInfo
    val summary: GpsTrackSummary? = null,

    // Actual location data (empty before first gps fix).
    @ColumnInfo
    val locationList: LocationDataList? = null,
) {
    fun toPojo() = TrackEntityPojo(
        uuid = uuid,
        createdDateISO = createdDateISO,
        updatedDateISO = updatedDateISO,
        trackLabel = trackLabel,
        startLabel = startLabel,
        endLabel = endLabel,
        locationList = locationList?.let { LocationDataListPojo(it) },
    )
}

/// A mirror of [GpsTrackEntity] for JSON import/export.
@JsonIgnoreProperties(value = ["id"])
data class TrackEntityPojo @JsonCreator constructor(
    @param:JsonProperty("uuid")             val uuid: String,
    @param:JsonProperty("createdDateISO")   val createdDateISO: String,
    @param:JsonProperty("updatedDateISO")   val updatedDateISO: String? = null,
    @param:JsonAlias("label")
    @param:JsonProperty("trackLabel")       val trackLabel: String? = null,
    @param:JsonProperty("startLabel")       val startLabel: String? = null,
    @param:JsonProperty("endLabel")         val endLabel: String? = null,
    @param:JsonProperty("locationList")     val locationList: LocationDataListPojo? = null,
) {
    fun toProto() = GpsTrackEntity(
        id = 0,
        uuid = uuid,
        createdDateISO = createdDateISO,
        updatedDateISO = updatedDateISO,
        trackLabel = trackLabel,
        startLabel = startLabel,
        endLabel = endLabel,
        summary = null,
        locationList = locationList?.toProto(),
    )
}

// A minimal version of GpsTrackEntity for displaying in the TracksFragment.
data class TrackDisplayInfo(
    val id: Long = 0,
    val createdDateISO: String,
    val trackLabel: String? = null,
    val summary: GpsTrackSummary? = null,
)
