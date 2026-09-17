package com.alflabs.milepostlib.db

import com.alflabs.milepostlib.location.LatLongData
import com.alflabs.milepostlib.location.LocationData
import com.alflabs.milepostlib.location.LocationDataList
import com.alflabs.milepostlib.location.SpeedData
import com.alflabs.milepostlib.location.latLongData
import com.alflabs.milepostlib.location.locationData
import com.alflabs.milepostlib.location.locationDataList
import com.alflabs.milepostlib.location.speedData
import com.alflabs.milepostlib.trigger.PlaybackTriggerDirection
import com.alflabs.milepostlib.trigger.PlaybackTriggerMetadata
import com.alflabs.milepostlib.trigger.playbackTriggerMetadata
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.util.UUID
import kotlin.collections.forEach

class LocationDataListDeserializer : StdDeserializer<LocationDataList>(LocationDataList::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocationDataList {
        val node: JsonNode = p.codec.readTree(p)
        return locationDataList {
            if (node.hasNonNull("locationsList")) {
                node.get("locationsList")?.forEach { elem ->
                    locations.add(ctxt.readTreeAsValue(elem, LocationData::class.java))
                }
            } else {
                // "locations" is the legacy name for "locationsList"
                node.get("locations")?.forEach { elem ->
                    locations.add(ctxt.readTreeAsValue(elem, LocationData::class.java))
                }
            }
        }
    }
}

class LocationDataDeserializer : StdDeserializer<LocationData>(LocationData::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocationData {
        val node: JsonNode = p.codec.readTree(p)
        return locationData {
            if (node.hasNonNull("latLong")) {
                latLong = ctxt.readTreeAsValue(node.get("latLong"), LatLongData::class.java)
            }
            if (node.hasNonNull("speed")) {
                speed = ctxt.readTreeAsValue(node.get("speed"), SpeedData::class.java)
            } else if (node.hasNonNull("speedBearing")) {
                // "speedBearing" is the legacy name for "speed"
                speed = ctxt.readTreeAsValue(node.get("speedBearing"), SpeedData::class.java)
            }
            if (node.hasNonNull("flags")) flags = node.get("flags").intValue()
            if (node.hasNonNull("mp")) {
                val value = node.get("mp").floatValue()
                if (value != 0f) mp = value
            }
            if (node.hasNonNull("poiLabel")) {
                val value = node.get("poiLabel").asText()
                if (value.isNotEmpty()) poiLabel = value
            }
        }
    }
}

class LatLongDataDeserializer : StdDeserializer<LatLongData>(LatLongData::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LatLongData {
        val node: JsonNode = p.codec.readTree(p)
        return latLongData {
            if (node.hasNonNull("latitude")) latitude = node.get("latitude").asDouble()
            if (node.hasNonNull("longitude")) longitude = node.get("longitude").asDouble()
            if (node.hasNonNull("timestampMillis")) timestampMillis =
                node.get("timestampMillis").asLong()
            if (node.hasNonNull("accuracy")) {
                val value = node.get("accuracy").floatValue()
                if (value != 0f) accuracy = value
            }
        }
    }
}

class SpeedDataDeserializer : StdDeserializer<SpeedData>(SpeedData::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SpeedData {
        val node: JsonNode = p.codec.readTree(p)
        return speedData {
            if (node.hasNonNull("speedMps")) speedMps = node.get("speedMps").floatValue()
            if (node.hasNonNull("speedAcc")) {
                val value = node.get("speedAcc").floatValue()
                if (value != 0f) speedAcc = value
            }
        }
    }
}

class PlayTriggerEntityDeserializer : StdDeserializer<PlayTriggerEntity>(PlayTriggerEntity::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): PlayTriggerEntity {
        val node: JsonNode = p.codec.readTree(p)
        return PlayTriggerEntity(
            audioSegmentId = node.get("audioSegmentId").asLong(),
            docentTrackId = node.get("docentTrackId").asLong(),
            playbackTrigger = if (node.hasNonNull("playbackTrigger")) {
                ctxt.readTreeAsValue(node.get("playbackTrigger"), PlaybackTriggerMetadata::class.java)
            } else null
        )
    }
}

class DocentTrackEntityDeserializer : StdDeserializer<DocentTrackEntity>(DocentTrackEntity::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DocentTrackEntity {
        val node: JsonNode = p.codec.readTree(p)
        return DocentTrackEntity(
            uuid = if (node.hasNonNull("uuid")) node.get("uuid").asText() else UUID.randomUUID().toString(),
            userTitle = if (node.hasNonNull("userTitle")) node.get("userTitle").asText() else null,
            creationTime = if (node.hasNonNull("creationTime")) node.get("creationTime").asText() else "",
            lastUsedTime = if (node.hasNonNull("lastUsedTime")) node.get("lastUsedTime").asText() else null,
            gpsTrackId = if (node.hasNonNull("gpsTrackId")) node.get("gpsTrackId").asLong() else 0L
        )
    }
}

class PlaybackTriggerMetadataDeserializer : StdDeserializer<PlaybackTriggerMetadata>(PlaybackTriggerMetadata::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): PlaybackTriggerMetadata {
        val node: JsonNode = p.codec.readTree(p)
        return playbackTriggerMetadata {
            if (node.hasNonNull("includeInColdStart")) includeInColdStart = node.get("includeInColdStart").asBoolean()
            if (node.hasNonNull("mpMin")) mpMin = node.get("mpMin").floatValue()
            if (node.hasNonNull("mpMax")) mpMax = node.get("mpMax").floatValue()
            if (node.hasNonNull("mpDirection")) {
                mpDirection = PlaybackTriggerDirection.valueOf(node.get("mpDirection").asText())
            }
        }
    }
}

class AudioSegmentMetadataDeserializer : StdDeserializer<AudioSegmentMetadata>(AudioSegmentMetadata::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): AudioSegmentMetadata {
        val node: JsonNode = p.codec.readTree(p)
        return audioSegmentMetadata {
            if (node.hasNonNull("durationMs")) durationMs = node.get("durationMs").asLong()
        }
    }
}
