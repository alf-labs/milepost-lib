package com.alflabs.mpgpstrack.tracks

import androidx.room.TypeConverter
import com.alflabs.mpgpstrack.location.LocationDataList
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class Converters {
    @TypeConverter
    fun fromLocationDataList(value: LocationDataList?): String? {
        // Base 64 version:
        return value?.let {
            Base64.encode(it.toByteArray())
        }
    }

    @TypeConverter
    fun toLocationDataList(value: String?): LocationDataList? {
        return value?.let {
            // Base 64 version:
            val decoded = Base64.decode(it)
            LocationDataList.parseFrom(decoded)
        }
    }
}
