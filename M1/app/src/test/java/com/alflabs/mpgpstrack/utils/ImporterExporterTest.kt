package com.alflabs.mpgpstrack.utils

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.mpgpstrack.dagger.FakeFileOpsModule
import com.alflabs.mpgpstrack.dagger.FakeLoggerModule
import com.alflabs.mpgpstrack.location.latLongData
import com.alflabs.mpgpstrack.location.locationData
import com.alflabs.mpgpstrack.location.locationDataList
import com.alflabs.mpgpstrack.location.speedBearingData
import com.alflabs.mpgpstrack.tracks.TrackEntity
import com.alflabs.mpgpstrack.tracks.trackSummary
import com.alflabs.utils.FakeFileOps
import com.alflabs.utils.StringLogger
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ImporterExporterTest {
    private val fakeLogger = FakeLoggerModule.providesLogger() as StringLogger
    private val fakeFileOps = FakeFileOpsModule.providesFileOps() as FakeFileOps
    private val importerExporter = ImporterExporter(fakeLogger, fakeFileOps)
    private lateinit var trackEntity: TrackEntity

    @Before
    fun setup() {
        val location1 = locationData {
            latLong = latLongData {
                latitude = 37.5805
                longitude = 121.9652
                timestampMillis = Instant.parse("2020-01-02T03:04:05Z").toEpochMilli()
            }
            speedBearing = speedBearingData {
                speedMps = 4.4704f
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
                speedMps = 8.9408f
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
        val mockContext = mock<Context> {
            on { cacheDir } doReturn File("/tmp")
        }
        val file = importerExporter.createCsvExportFile(mockContext, trackEntity)
        assertThat(fakeLogger.string.trim()).isEqualTo("ImporterExporter: createCsvExportFile")
        assertThat(file).isNotNull()
        file!!
        assertThat(file.path.replace('\\', '/')).endsWith("/track_test-uuid-1234.csv")
        assertThat(fakeFileOps.toString(file, Charsets.UTF_8)).isEqualTo(
            """
            isoDateTime,latitude,longitude,speed_mph,mp
            2020-01-02T03:04:05Z,37.5805,121.9652,10.0,10.0
            2020-01-02T03:14:05Z,37.58725,121.92685,,12.155647
            2020-01-02T03:24:05Z,37.594,121.8885,20.0,42.0

        """.trimIndent())
    }

    @Test
    fun createJsonExportFile_createsCorrectJsonFile() {
        val mockContext = mock<Context> {
            on { cacheDir } doReturn File("/tmp")
        }
        val file = importerExporter.createJsonExportFile(mockContext, trackEntity)
        assertThat(fakeLogger.string.trim()).isEqualTo("ImporterExporter: createJsonExportFile")
        assertThat(file).isNotNull()
        file!!
        assertThat(file.path.replace('\\', '/')).endsWith("/track_test-uuid-1234.json")
        assertThat(fakeFileOps.toString(file, Charsets.UTF_8).replace("\r\n", "\n")).isEqualTo(
            """
            {
              "id" : 0,
              "uuid" : "test-uuid-1234",
              "createdDateISO" : "2021-02-03T04:05:06Z",
              "updatedDateISO" : "2022-03-04T05:06:07Z",
              "label" : "Test Track",
              "locationList" : {
                "locations" : [ {
                  "latLong" : {
                    "timestampMillis" : 1577934245000,
                    "latitude" : 37.5805,
                    "longitude" : 121.9652
                  },
                  "speedBearing" : {
                    "speedMps" : 4.4704,
                    "bearingDeg" : 0.0
                  },
                  "mp" : 10.0,
                  "flags" : 0
                }, {
                  "latLong" : {
                    "timestampMillis" : 1577934845000,
                    "latitude" : 37.58725,
                    "longitude" : 121.92685
                  },
                  "flags" : 0
                }, {
                  "latLong" : {
                    "timestampMillis" : 1577935445000,
                    "latitude" : 37.594,
                    "longitude" : 121.8885
                  },
                  "speedBearing" : {
                    "speedMps" : 8.9408,
                    "bearingDeg" : 0.0
                  },
                  "mp" : 42.0,
                  "flags" : 0
                } ]
              }
            }
        """.trimIndent().replace("\r\n", "\n"))
    }

    @Test
    fun loadTrackEntityFromUri_loadsCorrectTrackEntity() {
        val jsonString = """
            {
              "id" : 0,
              "uuid" : "test-uuid-1234",
              "createdDateISO" : "2021-02-03T04:05:06Z",
              "updatedDateISO" : "2022-03-04T05:06:07Z",
              "label" : "Test Track",
              "locationList" : {
                "locations" : [ {
                  "latLong" : {
                    "timestampMillis" : 1577934245000,
                    "latitude" : 37.5805,
                    "longitude" : 121.9652
                  },
                  "speedBearing" : {
                    "speedMps" : 4.4704,
                    "bearingDeg" : 0.0
                  },
                  "mp" : 10.0,
                  "flags" : 0
                }, {
                  "latLong" : {
                    "timestampMillis" : 1577934845000,
                    "latitude" : 37.58725,
                    "longitude" : 121.92685
                  },
                  "flags" : 0
                }, {
                  "latLong" : {
                    "timestampMillis" : 1577935445000,
                    "latitude" : 37.594,
                    "longitude" : 121.8885
                  },
                  "speedBearing" : {
                    "speedMps" : 8.9408,
                    "bearingDeg" : 0.0
                  },
                  "mp" : 42.0,
                  "flags" : 0
                } ]
              }
            }
        """.trimIndent()
        val mockUri = mock<Uri>()
        val mockContentResolver = mock<android.content.ContentResolver> {
            on { openInputStream(mockUri) } doReturn ByteArrayInputStream(jsonString.toByteArray())
        }
        val mockContext = mock<Context> {
            on { contentResolver } doReturn mockContentResolver
        }

        val loadedTrackEntity = importerExporter.loadTrackEntityFromUri(mockContext, mockUri)

        assertThat(fakeLogger.string.trim()).isEqualTo("ImporterExporter: loadTrackEntityFromUri")
        assertThat(loadedTrackEntity).isNotNull()
        loadedTrackEntity!!
        // The summary is not part of the JSON, so copy it from the original for comparison.
        val expectedTrackEntity = trackEntity.copy(summary = loadedTrackEntity.summary)
        assertThat(loadedTrackEntity).isEqualTo(expectedTrackEntity)
    }
}
