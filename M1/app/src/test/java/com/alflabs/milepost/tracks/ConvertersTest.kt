package com.alflabs.milepost.tracks

import com.alflabs.milepost.location.latLongData
import com.alflabs.milepost.location.locationData
import com.alflabs.milepost.location.locationDataList
import com.alflabs.milepost.location.speedData
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
                    speed = speedData { speedMps = 1.0f } }
            ))
        }

        val serialized = converters.fromLocationDataList(originalList)
        assertThat(serialized).isEqualTo("ChQKEhEAAAAAAADwPxkAAAAAAADwPwoUChIRAAAAAAAAAEAZAAAAAAAAAEAKGwoSEQAAAAAAAAhAGQAAAAAAAAhAEgUNAACAPw==")

        val deserialized = converters.toLocationDataList(serialized)
        assertThat(deserialized).isEqualTo(originalList)
    }

    @Test
    fun testLocationDataListConverter_reloadOldFormatWithBearing() {
        val converters = Converters()

        val originalList = locationDataList {
            locations.addAll(listOf(
                locationData { latLong = latLongData { latitude = 1.0; longitude = 1.0 } },
                locationData { latLong = latLongData { latitude = 2.0; longitude = 2.0 } },
                locationData { latLong = latLongData { latitude = 3.0; longitude = 3.0 };
                    speed = speedData { speedMps = 1.0f } }
            ))
        }

        // Captured serialized proto3 with speedBearing bearing fields.
        val serialized = "ChQKEhEAAAAAAADwPxkAAAAAAADwPwoUChIRAAAAAAAAAEAZAAAAAAAAAEAKIAoSEQAAAAAAAAhAGQAAAAAAAAhAEgoNAACAPxUAAIA/"

        // Note: Protobuf Lite does not have DiscardUnknownFieldsParser. Unknown fields (e.g.
        // deleted/reserved fields) are still accumulated as "unknown" fields when
        // deserializing.
        val deserialized = converters.toLocationDataList(serialized)
        assertThat(deserialized).isNotNull()
        deserialized!!

        // This fails due to the unknown field in speedData.
        assertThat(deserialized).isNotEqualTo(originalList)
        // Instead check fields directly
        assertThat(deserialized.locationsList.size).isEqualTo(3)
        assertThat(deserialized.locationsList[0]).isEqualTo(originalList.locationsList[0])
        assertThat(deserialized.locationsList[1]).isEqualTo(originalList.locationsList[1])
        assertThat(deserialized.locationsList[2].latLong).isEqualTo(originalList.locationsList[2].latLong)
        assertThat(deserialized.locationsList[2].speed.speedMps).isEqualTo(1.0f)
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
