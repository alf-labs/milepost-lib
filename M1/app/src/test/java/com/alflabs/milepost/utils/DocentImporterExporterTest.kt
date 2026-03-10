package com.alflabs.milepost.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.milepost.dagger.FakeFileOpsModule
import com.alflabs.milepost.dagger.FakeLoggerModule
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.milepost.db.gpsTrackSummary
import com.alflabs.milepost.fragments.docentTriggers.DocentTriggerItem
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
import org.robolectric.Shadows
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class DocentImporterExporterTest {
    private val fakeLogger = FakeLoggerModule.providesLogger() as StringLogger
    private val fakeFileOps = FakeFileOpsModule.providesFileOps() as FakeFileOps
    private val docentImporterExporter = DocentImporterExporter(fakeLogger, fakeFileOps)
    private lateinit var docentTrackEntity: DocentTrackEntity
    private lateinit var gpsTrackEntity: GpsTrackEntity
    private lateinit var docentItems: List<DocentTriggerItem>

    @Before
    fun setup() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()

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
            DocentTriggerItem(
                id = 1,
                audioFileId = 1,
                persistableUri = "Uri-1",
                docentTrackId = 42L,
                fileName = audioFiles[1]!!,
                playbackTrigger = playbackTriggerMetadata {
                    includeInColdStart = true
                    mpMin = 10f
                    mpMax = 11f
                    mpDirection = PlaybackTriggerDirection.DIRECTION_TO_HIGHER_MP
                },
            ),
            DocentTriggerItem(
                id = 2,
                audioFileId = 2,
                persistableUri = "Uri-2",
                docentTrackId = 42L,
                fileName = audioFiles[2]!!,
                playbackTrigger = playbackTriggerMetadata {
                    includeInColdStart = true
                    mpMin = 20f
                    mpMax = 22f
                    mpDirection = PlaybackTriggerDirection.DIRECTION_UNSPECIFIED
                },
            ),
            DocentTriggerItem(
                id = 3,
                audioFileId = 3,
                persistableUri = "Uri-3",
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

    private val expectedJson = """
            {
              "docentTrackUuid" : "test-uuid-456",
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
            """

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
            expectedJson.trimIndent().replace("\r\n", "\n"))
    }

    @Test
    fun loadDocentTrackEntityFromUri_loadsCorrect() {
        val mockUri = mock<Uri>()
        val mockContentResolver = mock<ContentResolver> {
            on { openInputStream(mockUri) } doReturn ByteArrayInputStream(expectedJson.toByteArray())
        }
        val mockContext = mock<Context> {
            on { contentResolver } doReturn mockContentResolver
        }

        val validator = { validation: DocentImportValidationData ->
            assertThat(validation.docentTrackUuid).isEqualTo("test-uuid-456")
            assertThat(validation.gpsTrackUuid).isEqualTo("test-uuid-1234")
            assertThat(validation.gpsTrackName).isEqualTo("""  A fairly long Test+!@#$%^&*()!!=\/<>.,;':"[|]?-Track name 42 that is more than 64 characters  """)
            assertThat(validation.audioFileNames).containsExactly(
                "audio 1 segment",
                "audio 2 segment",
                "audio 3 segment",
            )

            DocentImportValidationResult(
                canContinue = true,
                message = "TBD",
                gpsTrackId = 123L,
                audioFileNamesToId = mapOf(
                    "audio 1 segment" to 1L,
                    "audio 2 segment" to 2L,
                    "audio 3 segment" to 3L,
                    "audio 4 segment" to 4L,
                    "audio 5 segment" to 5L,
                ),
                audioFileNamesToUri = mapOf(
                    "audio 1 segment" to "Uri-1",
                    "audio 2 segment" to "Uri-2",
                    "audio 3 segment" to "Uri-3",
                    "audio 4 segment" to "Uri-4",
                    "audio 5 segment" to "Uri-5",
                ),
            )
        }

        val result = docentImporterExporter.loadDocentTrackEntityFromUri(
            mockContext,
            mockUri,
            validator)

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(fakeLogger.string.trim()).isEqualTo("DocentImporterExporter: loadDocentTrackEntityFromUri")
        assertThat(result).isNotNull()
        result!!
        assertThat(result.docentTrackEntity).isEqualTo(docentTrackEntity.copy(id = 0))
        assertThat(result.triggerItems).containsExactlyElementsIn(
            docentItems.map {
                it.copy(id = 0, docentTrackId = 0)
            }
        )
    }
}
