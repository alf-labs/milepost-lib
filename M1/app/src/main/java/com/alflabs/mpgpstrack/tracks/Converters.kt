package com.alflabs.mpgpstrack.tracks

import androidx.room.TypeConverter
import com.alflabs.mpgpstrack.location.LocationData
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

class Converters {
    private val objectMapper = ObjectMapper()

    @TypeConverter
    fun fromLocationDataList(value: List<LocationData>?): String? {
        return value?.let { objectMapper.writeValueAsString(it) }
    }

    @TypeConverter
    fun toLocationDataList(value: String?): List<LocationData>? {
        return value?.let { objectMapper.readValue(it, object : TypeReference<List<LocationData>>() {}) }
    }
}
