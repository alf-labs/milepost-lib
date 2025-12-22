package com.alflabs.mpgpstrack.tracks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alflabs.mpgpstrack.location.LocationDataList
import com.alflabs.mpgpstrack.location.LocationDataListPojo
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
    val summary: TrackSummary? = null,

    // Actual location data (empty before first gps fix).
    @ColumnInfo
    val locationList: LocationDataList? = null,
) {
    fun toPojo() = TrackEntityPojo(
        uuid = uuid,
        createdDateISO = createdDateISO,
        updatedDateISO = updatedDateISO,
        label = label,
        locationList = locationList?.let { LocationDataListPojo(it) },
    )
}

/// A mirror of [TrackEntity] for JSON import/export.
@JsonIgnoreProperties(value = ["id"])
data class TrackEntityPojo @JsonCreator constructor(
    @param:JsonProperty("uuid")             val uuid: String,
    @param:JsonProperty("createdDateISO")   val createdDateISO: String,
    @param:JsonProperty("updatedDateISO")   val updatedDateISO: String? = null,
    @param:JsonProperty("label")            val label: String? = null,
    @param:JsonProperty("locationList")     val locationList: LocationDataListPojo? = null,
) {
    fun toProto() = TrackEntity(
        id = 0,
        uuid = uuid,
        createdDateISO = createdDateISO,
        updatedDateISO = updatedDateISO,
        label = label,
        summary = null,
        locationList = locationList?.toProto(),
    )
}

// A minimal version of TrackEntity for displaying in the TracksFragment.
data class TrackDisplayInfo(
    val id: Long = 0,
    val createdDateISO: String,
    val label: String? = null,
    val summary: TrackSummary? = null,
)
