package com.alflabs.mpgpstrack.performance

import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.mpgpstrack.location.LatLongDataPojo
import com.alflabs.mpgpstrack.location.LocationDataListPojo
import com.alflabs.mpgpstrack.location.LocationDataPojo
import com.alflabs.mpgpstrack.location.SpeedBearingDataPojo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.truth.Expect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PojoPerformanceTest {

    @get:Rule
    val expect: Expect = Expect.create()

    companion object {
        val TAG = PojoPerformanceTest::class.java.simpleName
        const val NUM_LOC = 10_000
        const val NUM_ENT =    100
    }

    @Test
    fun measureMemory() {
        val entities = mutableListOf<LocationDataListPojo>()
        val memInfo1 : Debug.MemoryInfo = Debug.MemoryInfo()
        val memInfo2 : Debug.MemoryInfo = Debug.MemoryInfo()

        System.gc()
        System.gc()
        System.gc()
        val freeMem1 = Runtime.getRuntime().freeMemory()
        Debug.getMemoryInfo(memInfo1)
        val totalPss1 = memInfo1.totalPss * 1024

        repeat(NUM_ENT) {
            entities.add(createLocList())
        }

        System.gc()
        System.gc()
        System.gc()
        val freeMem2 = Runtime.getRuntime().freeMemory()
        Debug.getMemoryInfo(memInfo2)
        val totalPss2 = memInfo2.totalPss * 1024
        Log.d(TAG, "@@ Num entities: ${entities.size}, hash ${entities.hashCode()}")

        val memPerEntLoc = (freeMem2 - freeMem1).toDouble() / NUM_ENT / NUM_LOC
        val pssPerEntLoc = (totalPss2 - totalPss1).toDouble() / NUM_ENT / NUM_LOC
        Log.w(TAG, "@@ FreeMem : $freeMem1 -> $freeMem2, delta ${freeMem2 - freeMem1} ==> $memPerEntLoc")
        Log.w(TAG, "@@ TotalPSS: $totalPss1 -> $totalPss2, delta ${totalPss2 - totalPss1} ==> $pssPerEntLoc")
        Log.w(TAG, "@@ Mem Info Stats 1: ${memInfo1.memoryStats}")
        Log.w(TAG, "@@ Mem Info Stats 2: ${memInfo2.memoryStats}")

        expect.that(freeMem1).isLessThan(freeMem2)
        expect.that(memPerEntLoc).isIn( 10.. 19)
        expect.that(pssPerEntLoc).isIn(220..230)
    }

    @Test
    fun serializationSize() {
        val entity = createLocList()
        val objectMapper = ObjectMapper()
        val serialized = objectMapper.writeValueAsString(entity)
        val size = serialized?.length ?: -1
        val szPerLoc = size.toDouble() / NUM_LOC
        Log.w(TAG, "@@ Serialized size: $size ==> $szPerLoc")
        expect.that(size).isEqualTo(2_126_689)
        expect.that(szPerLoc).isWithin(1.0).of(212.6)
    }

    @Test
    fun serializationTime() {
        val objectMapper = ObjectMapper().registerModule(
            KotlinModule.Builder().build()
        )

        val entity = createLocList()
        var dummy = 0

        System.gc()
        System.gc()
        System.gc()
        val now1 = SystemClock.currentThreadTimeMillis()

        repeat(NUM_ENT) {
            val serialized = objectMapper.writeValueAsString(entity)
            val deserialized = objectMapper.readValue<LocationDataListPojo>(serialized)

            dummy += deserialized.locations.size // to create a need for the result
        }

        val now2 = SystemClock.currentThreadTimeMillis()
        val msPerEnt    = (now2 - now1).toDouble() / NUM_ENT
        val msPerEntLoc = msPerEnt / NUM_LOC
        Log.w(TAG, "@@ Serialization Time : $msPerEnt ms/ent ==> $msPerEntLoc ms/ent/loc")
        expect.that(msPerEnt).isWithin(10.0).of(769.8)
        expect.that(msPerEntLoc).isWithin(0.001).of(0.0769)
    }

    fun createLocList(): LocationDataListPojo {
        // Note: the code here is made to vaguely look like the one in LocationService.
        val locationList = mutableListOf<LocationDataPojo>()

        var longVal = 1L
        var fltVal = 0.5f
        var dblVal = 0.5
        repeat(NUM_LOC) {
            val location = LocationDataPojo(
                latLong = LatLongDataPojo(
                    latitude = dblVal++,
                    longitude = dblVal ++,
                    timestampMillis = longVal++,
                    accuracy = fltVal++,
                ),
                speedBearing = SpeedBearingDataPojo(
                    speedMps = fltVal++,
                    bearingDeg = fltVal++,
                    speedAcc = fltVal++,
                    bearingAcc = fltVal++,
                ),
                mp = fltVal++,
                flags = 4
            )
            locationList.add(location)
        }

        return LocationDataListPojo(locationList)
    }

}
