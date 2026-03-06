package com.alflabs.milepost.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.milepost.dagger.FakeFileOpsModule
import com.alflabs.milepost.dagger.FakeLoggerModule
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.milepost.db.gpsTrackSummary
import com.alflabs.milepost.fragments.docentEditor.DocentEditorItem
import com.alflabs.milepost.location.LocationDataList
import com.alflabs.milepost.trigger.PlaybackTriggerDirection
import com.alflabs.milepost.trigger.playbackTriggerMetadata
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

@RunWith(AndroidJUnit4::class)
class DocentImporterExporterTest {
    private val fakeLogger = FakeLoggerModule.providesLogger() as StringLogger
    private val fakeFileOps = FakeFileOpsModule.providesFileOps() as FakeFileOps
    private val docentImporterExporter = DocentImporterExporter(fakeLogger, fakeFileOps)
    private lateinit var docentTrackEntity: DocentTrackEntity
    private lateinit var gpsTrackEntity: GpsTrackEntity
    private lateinit var docentItems: List<DocentEditorItem>

    @Before
    fun setup() {
        gpsTrackEntity = GpsTrackEntity(
            id = 123,
            uuid = "test-uuid-1234",
            trackLabel = """  A fairly long Test+!@#$%^&*()!!=\/<>.,;':"[|]?-Track name 42 that is more than 64 characters  """,
            startLabel = "Start Here",
            endLabel = "End There",
            locationList = LocationDataList.getDefaultInstance(),
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

        val audioFiles = mapOf(
            1L to "audio 1 segment",
            2L to "audio 2 segment",
            3L to "audio 3 segment",
        )

        docentItems = listOf(
            DocentEditorItem(
                id = 1,
                audioFileId = 1,
                docentTrackId = 42L,
                fileName = audioFiles[1]!!,
                playbackTrigger = playbackTriggerMetadata {
                    includeInColdStart = true
                    mpMin = 10f
                    mpMax = 11f
                    mpDirection = PlaybackTriggerDirection.DIRECTION_TO_HIGHER_MP
                },
            ),
            DocentEditorItem(
                id = 2,
                audioFileId = 2,
                docentTrackId = 42L,
                fileName = audioFiles[2]!!,
                playbackTrigger = playbackTriggerMetadata {
                    includeInColdStart = true
                    mpMin = 20f
                    mpMax = 22f
                    mpDirection = PlaybackTriggerDirection.DIRECTION_UNSPECIFIED
                },
            ),
            DocentEditorItem(
                id = 3,
                audioFileId = 3,
                docentTrackId = 42L,
                fileName = audioFiles[3]!!,
                playbackTrigger = playbackTriggerMetadata {
                    includeInColdStart = false
                    mpMin = 30f
                    mpMax = 33f
                    mpDirection = PlaybackTriggerDirection.DIRECTION_TO_LOWER_MP
                },
            ),
        )

        docentTrackEntity = DocentTrackEntity(
            id = 42L,
            uuid = "test-uuid-456",
            userTitle = """  A fairly long Test+!@#$%^&*()!!=\/<>.,;':"[|]?-Track name 42 that is more than 64 characters  """,
            creationTime = "2021-02-03T04:05:06Z",
            lastUsedTime = "2022-03-04T05:06:07Z",
            gpsTrackId = 123L,
        )

    }

    @Test
    fun createJsonExportFile_createsCorrectJsonFile() {
        val mockContext = mock<Context> {
            on { cacheDir } doReturn File("/tmp")
        }
        val file = docentImporterExporter.createJsonExportFile(
            mockContext,
            docentTrackEntity,
            gpsTrackEntity,
            docentItems
        )
        assertThat(fakeLogger.string.trim()).isEqualTo("DocentImporterExporter: createJsonExportFile")
        assertThat(file).isNotNull()
        file!!
        assertThat(file.path.replace('\\', '/')).endsWith("/docent test-uuid-456 A fairly long Test+ -Track name 42 that is more than 64 characte.json")
        assertThat(fakeFileOps.toString(file, Charsets.UTF_8).replace("\r\n", "\n")).isEqualTo(
            """
            {
              "uuid" : "test-uuid-456",
              "userTitle" : "  A fairly long Test+!@#$%^&*()!!=\\/<>.,;':\"[|]?-Track name 42 that is more than 64 characters  ",
              "createdDateISO" : "2021-02-03T04:05:06Z",
              "lastUsedDateISO" : "2022-03-04T05:06:07Z",
              "gpsTrackUuid" : "test-uuid-1234",
              "gpsTrackName" : "  A fairly long Test+!@#$%^&*()!!=\\/<>.,;':\"[|]?-Track name 42 that is more than 64 characters  ",
              "audioFileNames" : [ "audio 1 segment", "audio 2 segment", "audio 3 segment" ],
              "triggers" : [ {
                "audioFileName" : "audio 1 segment",
                "includeInColdStart" : true,
                "mpMin" : 10.0,
                "mpMax" : 11.0,
                "mpDirection" : {
                  "direction" : "ToHighMP"
                }
              }, {
                "audioFileName" : "audio 2 segment",
                "includeInColdStart" : true,
                "mpMin" : 20.0,
                "mpMax" : 22.0,
                "mpDirection" : {
                  "direction" : "Both"
                }
              }, {
                "audioFileName" : "audio 3 segment",
                "includeInColdStart" : false,
                "mpMin" : 30.0,
                "mpMax" : 33.0,
                "mpDirection" : {
                  "direction" : "ToLowMP"
                }
              } ]
            }
            """.trimIndent().replace("\r\n", "\n"))
    }

//    @Test
//    fun loadTrackEntityFromUri_loadsCorrectGpsTrackEntity() {
//        val jsonString = """
//            {
//              "id" : 0,
//              "uuid" : "test-uuid-1234",
//              "createdDateISO" : "2021-02-03T04:05:06Z",
//              "updatedDateISO" : "2022-03-04T05:06:07Z",
//              "trackLabel" : "  A fairly long Test+!@#$%^&*()!!=\\/<>.,;':\"[|]?-Track name 42 that is more than 64 characters  ",
//              "startLabel" : "Start Here",
//              "endLabel" : "End There",
//              "locationList" : {
//                "locations" : [ {
//                  "latLong" : {
//                    "timestampMillis" : 1577934245000,
//                    "latitude" : 37.592879,
//                    "longitude" : -121.88799
//                  },
//                  "speed" : {
//                    "speedMps" : 4.4704
//                  },
//                  "mp" : 35.63,
//                  "flags" : 0,
//                  "poiLabel" : "Sunol"
//                }, {
//                  "latLong" : {
//                    "timestampMillis" : 1577934845000,
//                    "latitude" : 37.598261,
//                    "longitude" : -121.921506
//                  },
//                  "flags" : 0
//                }, {
//                  "latLong" : {
//                    "timestampMillis" : 1577935445000,
//                    "latitude" : 37.578987,
//                    "longitude" : -121.981725,
//                    "accuracy" : 2.44
//                  },
//                  "speed" : {
//                    "speedMps" : 8.9408
//                  },
//                  "mp" : 29.0,
//                  "flags" : 1,
//                  "poiLabel" : "Niles"
//                } ]
//              }
//            }
//        """.trimIndent()
//        val mockUri = mock<Uri>()
//        val mockContentResolver = mock<ContentResolver> {
//            on { openInputStream(mockUri) } doReturn ByteArrayInputStream(jsonString.toByteArray())
//        }
//        val mockContext = mock<Context> {
//            on { contentResolver } doReturn mockContentResolver
//        }
//
//        val loadedTrackEntity = docentImporterExporter.loadGpsTrackEntityFromUri(mockContext, mockUri)
//
//        assertThat(fakeLogger.string.trim()).isEqualTo("GpsImporterExporter: loadTrackEntityFromUri")
//        assertThat(loadedTrackEntity).isNotNull()
//        loadedTrackEntity!!
//        // The summary is not part of the JSON, so copy it from the original for comparison.
//        val expectedTrackEntity = gpsTrackEntity.copy(summary = loadedTrackEntity.summary)
//        assertThat(loadedTrackEntity).isEqualTo(expectedTrackEntity)
//    }
}
