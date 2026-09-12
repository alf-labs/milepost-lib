package com.alflabs.milepostlib.db

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

class GpsTrackSummaryDeserializer : StdDeserializer<GpsTrackSummary>(GpsTrackSummary::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): GpsTrackSummary {
        val node: JsonNode = p.codec.readTree(p)
        return gpsTrackSummary {
            if (node.hasNonNull("mpMin")) {
                val value = node.get("mpMin").floatValue()
                if (value != 0f) mpMin = value
            }
            if (node.hasNonNull("mpMax")) {
                val value = node.get("mpMax").floatValue()
                if (value != 0f) mpMax = value
            }
            if (node.hasNonNull("lengthM")) {
                val value = node.get("lengthM").floatValue()
                if (value != 0f) lengthM = value
            }
            if (node.hasNonNull("durationS")) {
                val value = node.get("durationS").floatValue()
                if (value != 0f) durationS = value
            }
        }
    }
}
