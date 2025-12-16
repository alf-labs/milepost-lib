package com.alflabs.mpgpstrack.performance

import android.os.Debug
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.mpgpstrack.location.LocationDataList
import com.alflabs.mpgpstrack.location.latLongData
import com.alflabs.mpgpstrack.location.locationData
import com.alflabs.mpgpstrack.location.speedBearingData
import com.google.common.truth.Expect
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
            entities.add(createEntity())
        }

        System.gc()
        System.gc()
        System.gc()
        val freeMem2 = Runtime.getRuntime().freeMemory()
        Debug.getMemoryInfo(memInfo2)
        val totalPss2 = memInfo2.totalPss * 1024
        Log.d(TAG, "@@ Num entities: ${entities.size}, hash ${entities.hashCode()}")

        Log.w(TAG, "@@ FreeMem : $freeMem1 -> $freeMem2, delta ${freeMem2 - freeMem1} / ${(freeMem2 - freeMem1)/NUM_ENT}")
        Log.w(TAG, "@@ TotalPSS: $totalPss1 -> $totalPss2, delta ${totalPss2 - totalPss1} / ${(totalPss2 - totalPss1)/NUM_ENT}")
        Log.w(TAG, "@@ Mem Info Stats 1: ${memInfo1.memoryStats}")
        Log.w(TAG, "@@ Mem Info Stats 2: ${memInfo2.memoryStats}")
        expect.that(freeMem1).isLessThan(freeMem2)

        // Numbers:
        // loc   /  ent
        //  1,000/  100: FreeMem += 256,192/ent -- TotalPSS +=   103,761/ent
        //  1,000/1,000: FreeMem +=  75,497/ent -- TotalPSS +=   138,951/ent
        // 10,000/  100: FreeMem += 754,974/ent -- TotalPSS += 1,389,824/ent
        // 10,000/  200: OOM
    }

    fun createEntity(): LocationDataList {
        // Note: the code here is made to vaguely look like the one in LocationService.
        val locationDataListBuilder = LocationDataList.newBuilder()

        var longVal = 1L
        var fltVal = 0.5f
        var dblVal = 0.5
        repeat(NUM_LOC) {
            val location = locationData {
                latLong = latLongData {
                    latitude = dblVal++
                    longitude = dblVal++
                    timestampMillis = longVal++
                }
                speedBearing = speedBearingData {
                    speedMps = fltVal++
                }
                mp = fltVal++
            }
            locationDataListBuilder.addLocations(location)
        }

        return locationDataListBuilder.build()
    }

}
