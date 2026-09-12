package com.alflabs.milepostlib.location

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import kotlin.collections.forEach

class LocationDataListDeserializer : StdDeserializer<LocationDataList>(LocationDataList::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocationDataList {
        val node: JsonNode = p.codec.readTree(p)
        return locationDataList {
            node.get("locationsList")?.forEach { elem ->
                locations.add(ctxt.readTreeAsValue(elem, LocationData::class.java))
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
            if (node.hasNonNull("timestampMillis")) timestampMillis = node.get("timestampMillis").asLong()
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
