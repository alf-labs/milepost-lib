package com.alflabs.milepostlib.location

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocationDataTest {

    @Test
    fun testLatLongData() {
        val latLongData = latLongData {
            timestampMillis = 1234567890L
            latitude = 34.0522
            longitude = -118.2437
            accuracy = 10.0f
        }

        assertThat(latLongData.timestampMillis).isEqualTo(1234567890L)
        assertThat(latLongData.latitude).isWithin(0.0).of(34.0522)
        assertThat(latLongData.longitude).isWithin(0.0).of(-118.2437)
        assertThat(latLongData.accuracy).isWithin(0.0f).of(10.0f)
    }

    @Test
    fun testSpeedData() {
        val speedData = speedData {
            speedMps = 25.0f
            speedAcc = 1.0f
        }

        assertThat(speedData.speedMps).isWithin(0.0f).of(25.0f)
        assertThat(speedData.speedAcc).isWithin(0.0f).of(1.0f)
    }

    @Test
    fun testLocationData() {
        val latLongData = latLongData {
            timestampMillis = 1234567890L
            latitude = 34.0522
            longitude = -118.2437
            accuracy = 10.0f
        }

        val speedData = speedData {
            speedMps = 25.0f
            speedAcc = 1.0f
        }

        val locationData = locationData {
            this.latLong = latLongData
            this.speed = speedData
            mp = 100.0f
            flags = 1
            poiLabel = "Test POI"
        }

        assertThat(locationData.latLong).isEqualTo(latLongData)
        assertThat(locationData.speed).isEqualTo(speedData)
        assertThat(locationData.mp).isWithin(0.0f).of(100.0f)
        assertThat(locationData.flags).isEqualTo(1)
        assertThat(locationData.poiLabel).isEqualTo("Test POI")
    }

    @Test
    fun testLocationDataList() {
        val locationData1 = locationData { flags = 1 }
        val locationData2 = locationData { flags = 2 }

        val locationDataList = locationDataList {
            locations.addAll(listOf(locationData1, locationData2))
        }

        assertThat(locationDataList.locationsCount).isEqualTo(2)
        assertThat(locationDataList.locationsList).containsExactly(locationData1, locationData2).inOrder()
    }
}
