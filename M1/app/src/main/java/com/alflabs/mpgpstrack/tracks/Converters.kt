package com.alflabs.mpgpstrack.tracks

import androidx.room.TypeConverter
import com.alflabs.mpgpstrack.location.LocationDataList

class Converters {
    @TypeConverter
    fun fromLocationDataList(value: LocationDataList?): String? {
        return value?.toByteArray()?.toString(Charsets.UTF_8)
    }

    @TypeConverter
    fun toLocationDataList(value: String?): LocationDataList? {
        return value?.let {
            LocationDataList.parseFrom(it.toByteArray(Charsets.UTF_8))
        }
    }
}
