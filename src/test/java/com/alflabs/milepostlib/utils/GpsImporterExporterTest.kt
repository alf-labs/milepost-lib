package com.alflabs.milepostlib.utils

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.milepostlib.dagger.FakeFileOpsModule
import com.alflabs.milepostlib.dagger.FakeLoggerModule
import com.alflabs.milepostlib.db.GpsTrackEntity
import com.alflabs.milepostlib.db.gpsTrackSummary
import com.alflabs.milepostlib.location.latLongData
import com.alflabs.milepostlib.location.locationData
import com.alflabs.milepostlib.location.locationDataList
import com.alflabs.milepostlib.location.speedData
import com.alflabs.utils.FakeFileOps
import com.alflabs.utils.StringLogger
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.Shadows
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class GpsImporterExporterTest {
    private val fakeLogger = FakeLoggerModule.providesLogger() as StringLogger
    private val fakeFileOps = FakeFileOpsModule.providesFileOps() as FakeFileOps
    private val gpsImporterExporter = GpsImporterExporter(fakeLogger, fakeFileOps)
    private lateinit var gpsTrackEntity: GpsTrackEntity

    @Before
    fun setup() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val location1 = locationData {
            latLong = latLongData {
                latitude = 37.592879
                longitude = -121.88799
                timestampMillis = Instant.parse("2020-01-02T03:04:05Z").toEpochMilli()
            }
            speed = speedData {
                speedMps = 4.4704f
            }
            mp = 35.63f
            poiLabel = "Sunol"
        }

        val location2 = locationData {
            latLong = latLongData {
                latitude = 37.598261
                longitude = -121.921506
                timestampMillis = Instant.parse("2020-01-02T03:14:05Z").toEpochMilli()
            }
        }

        val location3 = locationData {
            latLong = latLongData {
                latitude = 37.578987
                longitude = -121.981725
                accuracy = 2.44f
                timestampMillis = Instant.parse("2020-01-02T03:24:05Z").toEpochMilli()
            }
            speed = speedData {
                speedMps = 8.9408f
            }
            mp = 29.0f
            flags = 1
            poiLabel = "Niles"
        }

        val locations = locationDataList {
            locations.add(location1)
            locations.add(location2)
            locations.add(location3)
        }

        gpsTrackEntity = GpsTrackEntity(
            uuid = "test-uuid-1234",
            trackLabel = """  A fairly long Test+!@#$%^&*()!!=\/<>.,;':"[|]?-Track name 42 that is more than 64 characters  """,
            startLabel = "Start Here",
            endLabel = "End There",
            locationList = locations,
            // This summary should not be exported
            summary = gpsTrackSummary {
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
        val file = gpsImporterExporter.createCsvExportFile(mockContext, gpsTrackEntity)
        assertThat(fakeLogger.string.trim()).isEqualTo("GpsImporterExporter: createCsvExportFile")
        assertThat(file).isNotNull()
        file!!
        assertThat(file.path.replace('\\', '/')).endsWith("/gps test-uuid-1234 A fairly long Test+ -Track name 42 that is more than 64 characte.csv")
        assertThat(fakeFileOps.toString(file, Charsets.UTF_8)).isEqualTo(
            """
            isoDateTime,latitude,longitude,speed_mph,mp
            2020-01-02T03:04:05Z,37.592879,-121.88799,10.0,35.63
            2020-01-02T03:14:05Z,37.598261,-121.921506,,
            2020-01-02T03:24:05Z,37.578987,-121.981725,20.0,29.0

        """.trimIndent())
    }

    @Test
    fun createJsonExportFile_createsCorrectJsonFile() {
        val mockContext = mock<Context> {
            on { cacheDir } doReturn File("/tmp")
        }
        val file = gpsImporterExporter.createJsonExportFile(mockContext, gpsTrackEntity)
        assertThat(fakeLogger.string.trim()).doesNotContain("error")
        assertThat(file).isNotNull()
        file!!
        assertThat(file.path.replace('\\', '/')).endsWith("/gps test-uuid-1234 A fairly long Test+ -Track name 42 that is more than 64 characte.json")
        assertThat(fakeFileOps.toString(file, Charsets.UTF_8).toTestString()).diffIsEqualTo(
            """
            {
              "uuid" : "test-uuid-1234",
              "createdDateISO" : "2021-02-03T04:05:06Z",
              "updatedDateISO" : "2022-03-04T05:06:07Z",
              "trackLabel" : "  A fairly long Test+!@#$%^&*()!!=\\/<>.,;':\"[|]?-Track name 42 that is more than 64 characters  ",
              "startLabel" : "Start Here",
              "endLabel" : "End There",
              "locationList" : {
                "locations" : [ {
                  "latLong" : {
                    "timestampMillis" : 1577934245000,
                    "latitude" : 37.592879,
                    "longitude" : -121.88799
                  },
                  "speed" : {
                    "speedMps" : 4.4704
                  },
                  "mp" : 35.63,
                  "flags" : 0,
                  "poiLabel" : "Sunol"
                }, {
                  "latLong" : {
                    "timestampMillis" : 1577934845000,
                    "latitude" : 37.598261,
                    "longitude" : -121.921506
                  },
                  "flags" : 0
                }, {
                  "latLong" : {
                    "timestampMillis" : 1577935445000,
                    "latitude" : 37.578987,
                    "longitude" : -121.981725,
                    "accuracy" : 2.44
                  },
                  "speed" : {
                    "speedMps" : 8.9408
                  },
                  "mp" : 29.0,
                  "flags" : 1,
                  "poiLabel" : "Niles"
                } ]
              }
            }
        """.trimIndent().toTestString())
    }

    @Test
    fun loadGpsTrackEntityFromUri_loadsOldFormatWithBearing() {
        val jsonString = """
            {
              "id" : 0,
              "uuid" : "test-uuid-1234",
              "createdDateISO" : "2021-02-03T04:05:06Z",
              "updatedDateISO" : "2022-03-04T05:06:07Z",
              "trackLabel" : "  A fairly long Test+!@#$%^&*()!!=\\/<>.,;':\"[|]?-Track name 42 that is more than 64 characters  ",
              "startLabel" : "Start Here",
              "endLabel" : "End There",
              "locationList" : {
                "locations" : [ {
                  "latLong" : {
                    "timestampMillis" : 1577934245000,
                    "latitude" : 37.592879,
                    "longitude" : -121.88799
                  },
                  "speedBearing" : {
                    "speedMps" : 4.4704,
                    "bearingDeg" : 0.0
                  },
                  "mp" : 35.63,
                  "flags" : 0,
                  "poiLabel" : "Sunol"
                }, {
                  "latLong" : {
                    "timestampMillis" : 1577934845000,
                    "latitude" : 37.598261,
                    "longitude" : -121.921506
                  },
                  "flags" : 0
                }, {
                  "latLong" : {
                    "timestampMillis" : 1577935445000,
                    "latitude" : 37.578987,
                    "longitude" : -121.981725,
                    "accuracy" : 2.44
                  },
                  "speedBearing" : {
                    "speedMps" : 8.9408,
                    "bearingDeg" : 0.0
                  },
                  "mp" : 29.0,
                  "flags" : 1,
                  "poiLabel" : "Niles"
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

        val loadedTrackEntity = gpsImporterExporter.loadGpsTrackEntityFromUri(mockContext, mockUri)

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(fakeLogger.string.trim()).isEqualTo("GpsImporterExporter: loadGpsTrackEntityFromUri")
        assertThat(loadedTrackEntity).isNotNull()
        loadedTrackEntity!!
        // The summary is not part of the JSON, so copy it from the original for comparison.
        val expectedTrackEntity = gpsTrackEntity.copy(summary = loadedTrackEntity.summary)
        assertThat(loadedTrackEntity).diffIsEqualTo(expectedTrackEntity)
    }

    @Test
    fun loadTrackEntityFromUri_loadsCorrectGpsTrackEntity() {
        val jsonString = """
            {
              "id" : 0,
              "uuid" : "test-uuid-1234",
              "createdDateISO" : "2021-02-03T04:05:06Z",
              "updatedDateISO" : "2022-03-04T05:06:07Z",
              "trackLabel" : "  A fairly long Test+!@#$%^&*()!!=\\/<>.,;':\"[|]?-Track name 42 that is more than 64 characters  ",
              "startLabel" : "Start Here",
              "endLabel" : "End There",
              "locationList" : {
                "locations" : [ {
                  "latLong" : {
                    "timestampMillis" : 1577934245000,
                    "latitude" : 37.592879,
                    "longitude" : -121.88799
                  },
                  "speed" : {
                    "speedMps" : 4.4704
                  },
                  "mp" : 35.63,
                  "flags" : 0,
                  "poiLabel" : "Sunol"
                }, {
                  "latLong" : {
                    "timestampMillis" : 1577934845000,
                    "latitude" : 37.598261,
                    "longitude" : -121.921506
                  },
                  "flags" : 0
                }, {
                  "latLong" : {
                    "timestampMillis" : 1577935445000,
                    "latitude" : 37.578987,
                    "longitude" : -121.981725,
                    "accuracy" : 2.44
                  },
                  "speed" : {
                    "speedMps" : 8.9408
                  },
                  "mp" : 29.0,
                  "flags" : 1,
                  "poiLabel" : "Niles"
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

        val loadedTrackEntity = gpsImporterExporter.loadGpsTrackEntityFromUri(mockContext, mockUri)

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(fakeLogger.string.trim()).isEqualTo("GpsImporterExporter: loadGpsTrackEntityFromUri")
        assertThat(loadedTrackEntity).isNotNull()
        loadedTrackEntity!!
        // The summary is not part of the JSON, so copy it from the original for comparison.
        val expectedTrackEntity = gpsTrackEntity.copy(summary = loadedTrackEntity.summary)
        assertThat(loadedTrackEntity).diffIsEqualTo(expectedTrackEntity)
    }
}
