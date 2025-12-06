package com.alflabs.mpgpstrack.tracks

import com.alflabs.mpgpstrack.location.LatLongData
import com.alflabs.mpgpstrack.location.LocationData
import com.alflabs.mpgpstrack.location.LocationDataList
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {

    @Test
    fun testLocationDataListConverter() {
        val converters = Converters()

        val location1 = LocationData.newBuilder()
            .setLatLong(LatLongData.newBuilder().setLatitude(1.0).setLongitude(1.0))
            .build()
        val location2 = LocationData.newBuilder()
            .setLatLong(LatLongData.newBuilder().setLatitude(2.0).setLongitude(2.0))
            .build()
        val location3 = LocationData.newBuilder()
            .setLatLong(LatLongData.newBuilder().setLatitude(3.0).setLongitude(3.0))
            .build()

        val originalList = LocationDataList.newBuilder()
            .addLocations(location1)
            .addLocations(location2)
            .addLocations(location3)
            .build()

        val serialized = converters.fromLocationDataList(originalList)
        assertThat(serialized).isEqualTo("ChQKEhEAAAAAAADwPxkAAAAAAADwPwoUChIRAAAAAAAAAEAZAAAAAAAAAEAKFAoSEQAAAAAAAAhAGQAAAAAAAAhA")

        val deserialized = converters.toLocationDataList(serialized)
        assertThat(deserialized).isEqualTo(originalList)
    }
}
