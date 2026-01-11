package com.alflabs.milepost.tracks

import androidx.room.TypeConverter
import com.alflabs.milepost.location.LocationDataList
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class Converters {
    @TypeConverter
    fun fromLocationDataList(value: LocationDataList?): String? {
        return value?.let {
            Base64.encode(it.toByteArray())
        }
    }

    @TypeConverter
    fun toLocationDataList(value: String?): LocationDataList? {
        return value?.let {
            val decoded = Base64.decode(it)
            LocationDataList.parseFrom(decoded)
        }
    }

    @TypeConverter
    fun fromTrackSummary(value: GpsTrackSummary?): String? {
        return value?.let {
            Base64.encode(it.toByteArray())
        }
    }

    @TypeConverter
    fun toTrackSummary(value: String?): GpsTrackSummary? {
        return value?.let {
            val decoded = Base64.decode(it)
            GpsTrackSummary.parseFrom(decoded)
        }
    }
}
