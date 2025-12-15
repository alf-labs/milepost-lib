package com.alflabs.mpgpstrack.fragments.review

import android.content.Context
import com.alflabs.mpgpstrack.location.latLongData
import com.alflabs.mpgpstrack.location.locationData
import com.alflabs.mpgpstrack.location.locationDataList
import com.alflabs.mpgpstrack.location.speedBearingData
import com.alflabs.mpgpstrack.tracks.TrackEntity
import com.alflabs.mpgpstrack.tracks.trackSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File
import java.time.Instant

class ExporterTest {
    private lateinit var context: Context
    private lateinit var trackEntity: TrackEntity

    @Before
    fun setup() {
        context = mock<Context> {
            on { cacheDir } doReturn File("/tmp")
        }

        val location1 = locationData {
            latLong = latLongData {
                latitude = 37.5805
                longitude = 121.9652
                timestampMillis = Instant.parse("2020-01-02T03:04:05Z").toEpochMilli()
            }
            speedBearing = speedBearingData {
                speedMps = 4.4f
            }
            mp = 10.0f
        }

        val location2 = locationData {
            latLong = latLongData {
                latitude = 37.58725
                longitude = 121.92685
                timestampMillis = Instant.parse("2020-01-02T03:14:05Z").toEpochMilli()
            }
        }

        val location3 = locationData {
            latLong = latLongData {
                latitude = 37.5940
                longitude = 121.8885
                timestampMillis = Instant.parse("2020-01-02T03:24:05Z").toEpochMilli()
            }
            speedBearing = speedBearingData {
                speedMps = 8.9f
            }
            mp = 42.0f
        }

        val locations = locationDataList {
            locations.add(location1)
            locations.add(location2)
            locations.add(location3)
        }

        trackEntity = TrackEntity(
            uuid = "test-uuid-1234",
            label = "Test Track",
            locationList = locations,
            // This summary should not be exported
            summary = trackSummary {
                mpMin = 10f
                mpMax = 42f
                lengthM = 2400f
                durationS = 1800f
            },
            createdDateISO = "2021-02-03T04:05:06Z",
            updatedDateISO = "2022-03-04T05:06:07Z"
        )
    }

    @Test
    fun createCsvExportFile_createsCorrectCsvFile() {
        val file = Exporter.createCsvExportFile(context, trackEntity)
        assertThat(file).isNotNull()
        assertThat(file!!.name).isEqualTo("track_test-uuid-1234.csv")

        val fileContent = file.readText()
        val expectedContent = """
            iso_date_time,latitude,longitude,speed_mps,mp
            2020-01-02T03:04:05Z,37.5805,121.9652,4.4,10.0
            2020-01-02T03:14:05Z,37.58725,121.92685,,
            2020-01-02T03:24:05Z,37.594,121.8885,8.9,42.0
        """.trimIndent()

        assertThat(fileContent).isEqualTo(expectedContent)
    }

    @Test
    fun createJsonExportFile_createsCorrectJsonFile() {
        val file = Exporter.createJsonExportFile(context, trackEntity)
        assertThat(file).isNotNull()
        assertThat(file!!.name).isEqualTo("track_test-uuid-1234.json")

        // We can't do a simple string comparison for JSON due to object mapper formatting variations
        // and unordered fields. A more robust test would parse the JSON and verify fields.
        // For this test, we just check for presence of key fields.
        val fileContent = file.readText()
        assertThat(fileContent).contains(""uuid":"test-uuid-1234"")
        assertThat(fileContent).contains(""label":"Test Track"")
        assertThat(fileContent).contains(""latitude":37.5805")
        assertThat(fileContent).contains(""longitude":121.9652")
        assertThat(fileContent).contains(""speedMps":4.4")
    }
}
