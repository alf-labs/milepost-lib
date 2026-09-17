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
