package com.alflabs.mpgpstrack.location

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocationDataTest {

    @Test
    fun testLatLongData() {
        val latLongData = LatLongData.newBuilder()
            .setTimestampMillis(1234567890L)
            .setLatitude(34.0522)
            .setLongitude(-118.2437)
            .setAccuracy(10.0f)
            .build()

        assertThat(latLongData.timestampMillis).isEqualTo(1234567890L)
        assertThat(latLongData.latitude).isWithin(0.0).of(34.0522)
        assertThat(latLongData.longitude).isWithin(0.0).of(-118.2437)
        assertThat(latLongData.accuracy).isWithin(0.0f).of(10.0f)
    }

    @Test
    fun testSpeedBearingData() {
        val speedBearingData = SpeedBearingData.newBuilder()
            .setSpeedMps(25.0f)
            .setBearingDeg(180.0f)
            .setSpeedAcc(1.0f)
            .setBearingAcc(5.0f)
            .build()

        assertThat(speedBearingData.speedMps).isWithin(0.0f).of(25.0f)
        assertThat(speedBearingData.bearingDeg).isWithin(0.0f).of(180.0f)
        assertThat(speedBearingData.speedAcc).isWithin(0.0f).of(1.0f)
        assertThat(speedBearingData.bearingAcc).isWithin(0.0f).of(5.0f)
    }

    @Test
    fun testLocationData() {
        val latLongData = LatLongData.newBuilder()
            .setTimestampMillis(1234567890L)
            .setLatitude(34.0522)
            .setLongitude(-118.2437)
            .setAccuracy(10.0f)
            .build()

        val speedBearingData = SpeedBearingData.newBuilder()
            .setSpeedMps(25.0f)
            .setBearingDeg(180.0f)
            .setSpeedAcc(1.0f)
            .setBearingAcc(5.0f)
            .build()

        val locationData = LocationData.newBuilder()
            .setLatLong(latLongData)
            .setSpeedBearing(speedBearingData)
            .setMp(100.0f)
            .setFlags(1)
            .build()

        assertThat(locationData.latLong).isEqualTo(latLongData)
        assertThat(locationData.speedBearing).isEqualTo(speedBearingData)
        assertThat(locationData.mp).isWithin(0.0f).of(100.0f)
        assertThat(locationData.flags).isEqualTo(1)
    }

    @Test
    fun testLocationDataList() {
        val locationData1 = LocationData.newBuilder().setFlags(1).build()
        val locationData2 = LocationData.newBuilder().setFlags(2).build()

        val locationDataList = LocationDataList.newBuilder()
            .addLocations(locationData1)
            .addLocations(locationData2)
            .build()

        assertThat(locationDataList.locationsCount).isEqualTo(2)
        assertThat(locationDataList.locationsList).containsExactly(locationData1, locationData2).inOrder()
    }
}
