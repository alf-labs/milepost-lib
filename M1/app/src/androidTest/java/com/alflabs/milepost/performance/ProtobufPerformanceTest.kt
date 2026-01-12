package com.alflabs.milepost.performance

import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.milepost.location.LocationDataList
import com.alflabs.milepost.location.latLongData
import com.alflabs.milepost.location.locationData
import com.alflabs.milepost.location.speedData
import com.alflabs.milepost.tracks.Converters
import com.google.common.truth.Expect
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProtobufPerformanceTest {

    @get:Rule
    val expect: Expect = Expect.create()

    companion object {
        val TAG = ProtobufPerformanceTest::class.java.simpleName
        const val NUM_LOC = 10_000
        const val NUM_ENT =    100
    }

    @Test
    fun measureMemory() {
        val entities = mutableListOf<LocationDataList>()
        val memInfo1 : Debug.MemoryInfo = Debug.MemoryInfo()
        val memInfo2 : Debug.MemoryInfo = Debug.MemoryInfo()

        System.gc()
        System.gc()
        System.gc()
        val freeMem1 = Runtime.getRuntime().freeMemory()
        Debug.getMemoryInfo(memInfo1)
        val totalPss1 = memInfo1.totalPss * 1024

        repeat(NUM_ENT) {
            entities.add(createLocList(NUM_LOC))
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

        // Numbers:
        // loc   /  ent
        //  1,000/  100: FreeMem += 256,192/ent -- TotalPSS +=   103,761/ent -- 256~103/ent/loc
        //  1,000/1,000: FreeMem +=  75,497/ent -- TotalPSS +=   138,951/ent --  75~138/ent/loc
        // 10,000/  100: FreeMem += 754,974/ent -- TotalPSS += 1,389,824/ent --  75~138/ent/loc
        // 10,000/  200: OOM

        expect.that(freeMem1).isLessThan(freeMem2)
        expect.that(memPerEntLoc).isIn( 75.. 76)
        expect.that(pssPerEntLoc).isIn(135..145)
    }

    @Test
    fun exhaustMemoryCount() {
        var numLoc = 10 * NUM_LOC

        while (true) {
            try {
                System.gc()
                val entity = createLocList(numLoc)
                assertThat(numLoc).isEqualTo(entity.locationsCount)
                Log.w(TAG, "@@ OOM Test: $numLoc --> ${entity.locationsCount}")
                numLoc += 10 * NUM_LOC
            } catch (e: OutOfMemoryError) {
                System.gc()
                Log.w(TAG, "@@ OOM at $numLoc: $e")
                break
            }
        }

        assertThat(numLoc).isEqualTo(1_000_000)
    }

    @Test
    fun serializationSize() {
        val entity = createLocList(NUM_LOC)
        val serialized = Converters().fromLocationDataList(entity)
        val size = serialized?.length ?: -1
        val szPerLoc = size.toDouble() / NUM_LOC
        Log.w(TAG, "@@ Serialized size: $size ==> $szPerLoc")
        expect.that(size).isEqualTo(786_500)
        expect.that(szPerLoc).isWithin(1.0).of(78.6)
    }

    @Test
    fun serializationTime() {
        val entity = createLocList(NUM_LOC)
        var locCounter = 0

        System.gc()
        System.gc()
        System.gc()
        val now1 = SystemClock.currentThreadTimeMillis()

        repeat(NUM_ENT) {
            val serialized = Converters().fromLocationDataList(entity)
            val deserialized = Converters().toLocationDataList(serialized)
            locCounter += deserialized?.locationsCount ?: 0 // to create a need for the result
        }

        val now2 = SystemClock.currentThreadTimeMillis()
        val msPerEnt    = (now2 - now1).toDouble() / NUM_ENT
        val msPerEntLoc = msPerEnt / NUM_LOC
        Log.w(TAG, "@@ Serialization Time : $msPerEnt ms/ent ==> $msPerEntLoc ms/ent/loc")
        expect.that(msPerEnt).isWithin(10.0).of(74.0)
        expect.that(msPerEntLoc).isWithin(0.0010).of(0.0074)
        assertThat(locCounter).isEqualTo(NUM_ENT * NUM_LOC)
    }

    fun createLocList(numLoc: Int): LocationDataList {
        // Note: the code here is made to vaguely look like the one in LocationService.
        val locationDataListBuilder = LocationDataList.newBuilder()

        var longVal = 1L
        var fltVal = 0.5f
        var dblVal = 0.5
        repeat(numLoc) {
            val location = locationData {
                latLong = latLongData {
                    latitude = dblVal++
                    longitude = dblVal++
                    timestampMillis = longVal++
                    accuracy = fltVal++
                }
                speed = speedData {
                    speedMps = fltVal++
                    speedAcc = fltVal++
                }
                mp = fltVal++
                flags = 4
            }
            locationDataListBuilder.addLocations(location)
        }

        return locationDataListBuilder.build()
    }

}
