package com.alflabs.milepostlib.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.milepostlib.dagger.FakeFileOpsModule
import com.alflabs.milepostlib.dagger.FakeLoggerModule
import com.alflabs.milepostlib.db.DocentTrackEntity
import com.alflabs.milepostlib.db.GpsTrackEntity
import com.alflabs.milepostlib.db.gpsTrackSummary
import com.alflabs.milepostlib.fragments.docentTriggers.DocentTriggerItem
import com.alflabs.milepostlib.location.LocationDataList
import com.alflabs.milepostlib.trigger.PlaybackTriggerDirection
import com.alflabs.milepostlib.trigger.playbackTrigger
import com.alflabs.utils.FakeFileOps
import com.alflabs.utils.StringLogger
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
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
            1L to "audio 1 segment[0:42]",
            2L to "audio 1 segment[2:24]",
            3L to "audio 3 segment[0:42]",
        )

        docentItems = listOf(
            DocentTriggerItem(
                id = 1,
                audioSegmentId = 1,
                persistableUri = "Uri-1",
                docentTrackId = 42L,
                audioDescriptor = audioFiles[1]!!,
                playbackTrigger = playbackTrigger {
                    includeInColdStart = true
                    mpMin = 10f
                    mpMax = 11f
                    mpDirection = PlaybackTriggerDirection.DIRECTION_TO_HIGHER_MP
                },
            ),
            DocentTriggerItem(
                id = 2,
                audioSegmentId = 2,
                persistableUri = "Uri-2",
                docentTrackId = 42L,
                audioDescriptor = audioFiles[2]!!,
                playbackTrigger = playbackTrigger {
                    includeInColdStart = true
                    mpMin = 20f
                    mpMax = 22f
                    mpDirection = PlaybackTriggerDirection.DIRECTION_UNSPECIFIED
                },
            ),
            DocentTriggerItem(
                id = 3,
                audioSegmentId = 3,
                persistableUri = "Uri-3",
                docentTrackId = 42L,
                audioDescriptor = audioFiles[3]!!,
                playbackTrigger = playbackTrigger {
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
              "audioDescriptors" : [ "audio 1 segment[0:42]", "audio 1 segment[2:24]", "audio 3 segment[0:42]" ],
              "docentTrack" : {
                "creationTime" : "2021-02-03T04:05:06Z",
                "lastUsedTime" : "2022-03-04T05:06:07Z",
                "userTitle" : "  A fairly long Test+!@#$%^&*()!!=\\/<>.,;':\"[|]?-Track name 42 that is more than 64 characters  ",
                "uuid" : "test-uuid-456"
              },
              "gpsTrackName" : "  A fairly long Test+!@#$%^&*()!!=\\/<>.,;':\"[|]?-Track name 42 that is more than 64 characters  ",
              "gpsTrackUuid" : "test-uuid-1234",
              "triggers" : [ {
                "audioDescriptor" : "audio 1 segment[0:42]",
                "playbackTrigger" : {
                  "includeInColdStart" : true,
                  "mpDirection" : "DIRECTION_TO_HIGHER_MP",
                  "mpMax" : 11.0,
                  "mpMin" : 10.0
                }
              }, {
                "audioDescriptor" : "audio 1 segment[2:24]",
                "playbackTrigger" : {
                  "includeInColdStart" : true,
                  "mpDirection" : "DIRECTION_UNSPECIFIED",
                  "mpMax" : 22.0,
                  "mpMin" : 20.0
                }
              }, {
                "audioDescriptor" : "audio 3 segment[0:42]",
                "playbackTrigger" : {
                  "includeInColdStart" : false,
                  "mpDirection" : "DIRECTION_TO_LOWER_MP",
                  "mpMax" : 33.0,
                  "mpMin" : 30.0
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
        assertThat(fakeLogger.string.trim()).doesNotContain("error")
        assertThat(file).isNotNull()
        file!!
        assertThat(file.path.replace('\\', '/')).endsWith("/docent test-uuid-456 A fairly long Test+ -Track name 42 that is more than 64 characte.json")
        val actualJson = fakeFileOps.toString(file, Charsets.UTF_8)
        assertThat(actualJson.toTestString()).diffIsEqualTo(
            expectedJson.trimIndent().toTestString())
    }

    @Test
    fun loadDocentTrackEntityFromUri_loadsCorrect() = runTest {
        val mockUri = mock<Uri>()
        val mockContentResolver = mock<ContentResolver> {
            on { openInputStream(mockUri) } doReturn ByteArrayInputStream(expectedJson.toByteArray())
        }
        val mockContext = mock<Context> {
            on { contentResolver } doReturn mockContentResolver
        }

        val validator = { validation: DocentImportExportData ->
            assertThat(validation.docentTrack.uuid).isEqualTo("test-uuid-456")
            assertThat(validation.gpsTrackUuid).isEqualTo("test-uuid-1234")
            assertThat(validation.gpsTrackName).isEqualTo("""  A fairly long Test+!@#$%^&*()!!=\/<>.,;':"[|]?-Track name 42 that is more than 64 characters  """)
            assertThat(validation.audioDescriptors).containsExactly(
                "audio 1 segment[0:42]",
                "audio 1 segment[2:24]",
                "audio 3 segment[0:42]",
            )

            DocentImportValidationResult(
                canContinue = true,
                message = "TBD",
                gpsTrackId = 123L,
                descriptorsToId = mapOf(
                    "audio 1 segment[0:42]" to 1L,
                    "audio 1 segment[2:24]" to 2L,
                    "audio 3 segment[0:42]" to 3L,
                    "audio 4 segment[0:42]" to 4L,
                    "audio 5 segment[0:42]" to 5L,
                ),
                descriptorsToUri = mapOf(
                    "audio 1 segment[0:42]" to "Uri-1",
                    "audio 1 segment[2:24]" to "Uri-2",
                    "audio 3 segment[0:42]" to "Uri-3",
                    "audio 4 segment[0:42]" to "Uri-4",
                    "audio 5 segment[0:42]" to "Uri-5",
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
