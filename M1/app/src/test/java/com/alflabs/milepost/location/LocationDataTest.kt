package com.alflabs.milepost.location

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
    fun testSpeedBearingData() {
        val speedBearingData = speedBearingData {
            speedMps = 25.0f
            bearingDeg = 180.0f
            speedAcc = 1.0f
            bearingAcc = 5.0f
        }

        assertThat(speedBearingData.speedMps).isWithin(0.0f).of(25.0f)
        assertThat(speedBearingData.bearingDeg).isWithin(0.0f).of(180.0f)
        assertThat(speedBearingData.speedAcc).isWithin(0.0f).of(1.0f)
        assertThat(speedBearingData.bearingAcc).isWithin(0.0f).of(5.0f)
    }

    @Test
    fun testLocationData() {
        val latLongData = latLongData {
            timestampMillis = 1234567890L
            latitude = 34.0522
            longitude = -118.2437
            accuracy = 10.0f
        }

        val speedBearingData = speedBearingData {
            speedMps = 25.0f
            bearingDeg = 180.0f
            speedAcc = 1.0f
            bearingAcc = 5.0f
        }

        val locationData = locationData {
            this.latLong = latLongData
            this.speedBearing = speedBearingData
            mp = 100.0f
            flags = 1
        }

        assertThat(locationData.latLong).isEqualTo(latLongData)
        assertThat(locationData.speedBearing).isEqualTo(speedBearingData)
        assertThat(locationData.mp).isWithin(0.0f).of(100.0f)
        assertThat(locationData.flags).isEqualTo(1)
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
