package com.alflabs.milepostlib.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alflabs.milepostlib.location.LocationDataList
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.util.UUID


// Doc: https://developer.android.com/training/data-storage/room/defining-data

@Entity(tableName = "gps_tracks")
data class GpsTrackEntity(
    // Note: according to the autoGenerate doc, setting the value to 0 forces it to be generated.
    @PrimaryKey(autoGenerate = true)
    @JsonIgnore
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
    @param:JsonAlias("label")
    val trackLabel: String? = null,

    // Start and end labels.
    @ColumnInfo(defaultValue = "null")
    val startLabel: String? = null,
    @ColumnInfo(defaultValue = "null")
    val endLabel: String? = null,

    // Optional summary info for list tracks fragment.
    @ColumnInfo
    @JsonIgnore
    val summary: GpsTrackSummary? = null,

    // Actual location data (empty before first gps fix).
    @ColumnInfo
    val locationList: LocationDataList? = null,
) {
    /** A version of [trackLabel] for display that is never null or empty. */
    fun displayTrackLabel(): String {
        if (trackLabel.isNullOrBlank()) {
            return "GPS Track #$id"
        }
        return trackLabel
    }
}

class GpsTrackEntityDeserializer : StdDeserializer<GpsTrackEntity>(GpsTrackEntity::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): GpsTrackEntity {
        val node: JsonNode = p.codec.readTree(p)
        return GpsTrackEntity(
            id = 0L, // "id" property is ignored, it's internal and should not be loaded.
            uuid = if (node.hasNonNull("uuid")) node.get("uuid").asText() else "",
            createdDateISO = if (node.hasNonNull("createdDateISO")) node.get("createdDateISO").asText() else "",
            updatedDateISO = if (node.hasNonNull("updatedDateISO")) node.get("updatedDateISO").asText() else null,
            trackLabel = if (node.hasNonNull("trackLabel")) {
                node.get("trackLabel").asText()
            } else  if (node.hasNonNull("label")) {
                // legacy name for trackLabel
                node.get("label").asText()
            } else {
                null
            },
            startLabel = if (node.hasNonNull("startLabel")) node.get("startLabel").asText() else null,
            endLabel = if (node.hasNonNull("endLabel")) node.get("endLabel").asText() else null,
            summary = if (node.hasNonNull("summary")) ctxt.readTreeAsValue(node.get("summary"), GpsTrackSummary::class.java) else null,
            locationList = if (node.hasNonNull("locationList")) ctxt.readTreeAsValue(node.get("locationList"), LocationDataList::class.java) else null
        )
    }
}
