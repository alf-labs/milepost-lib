package com.alflabs.milepost.tracks

import com.alflabs.milepost.location.latLongData
import com.alflabs.milepost.location.locationData
import com.alflabs.milepost.location.locationDataList
import com.alflabs.milepost.location.speedBearingData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {

    @Test
    fun testLocationDataListConverter() {
        val converters = Converters()

        val originalList = locationDataList {
            locations.addAll(listOf(
                locationData { latLong = latLongData { latitude = 1.0; longitude = 1.0 } },
                locationData { latLong = latLongData { latitude = 2.0; longitude = 2.0 } },
                locationData { latLong = latLongData { latitude = 3.0; longitude = 3.0 };
                    speedBearing = speedBearingData { speedMps = 1.0f; bearingDeg = 1.0f } }
            ))
        }

        val serialized = converters.fromLocationDataList(originalList)
        assertThat(serialized).isEqualTo("ChQKEhEAAAAAAADwPxkAAAAAAADwPwoUChIRAAAAAAAAAEAZAAAAAAAAAEAKIAoSEQAAAAAAAAhAGQAAAAAAAAhAEgoNAACAPxUAAIA/")

        val deserialized = converters.toLocationDataList(serialized)
        assertThat(deserialized).isEqualTo(originalList)
    }

    @Test
    fun testTrackSummaryConverter1() {
        val converters = Converters()

        val originalSummary = gpsTrackSummary {
            mpMin = 1.0f
            mpMax = 2.0f
            lengthM = 3.0f
            durationS = 4.0f
        }

        val serialized = converters.fromTrackSummary(originalSummary)
        assertThat(serialized).isEqualTo("DQAAgD8VAAAAQB0AAEBAJQAAgEA=")

        val deserialized = converters.toTrackSummary(serialized)
        assertThat(deserialized).isEqualTo(originalSummary)
    }

    @Test
    fun testTrackSummaryConverter2() {
        val converters = Converters()

        val originalSummary = gpsTrackSummary {
            mpMin = 1.0f
            mpMax = 2.0f
        }

        val serialized = converters.fromTrackSummary(originalSummary)
        assertThat(serialized).isEqualTo("DQAAgD8VAAAAQA==")

        val deserialized = converters.toTrackSummary(serialized)
        assertThat(deserialized).isEqualTo(originalSummary)
    }
}
