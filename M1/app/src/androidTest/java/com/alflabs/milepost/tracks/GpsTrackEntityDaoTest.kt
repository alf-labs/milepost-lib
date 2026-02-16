package com.alflabs.milepost.tracks

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.milepost.location.locationDataList
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class GpsTrackEntityDaoTest {
    private lateinit var dao: GpsTrackDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.trackEntityDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insert_newEntity() {
        runBlocking {
            val track = GpsTrackEntity(
                id = 0, // auto-generates a new unique id.
                uuid = "test-uuid-1",
                trackLabel = "Test Track 1",
                locationList = locationDataList { },
                createdDateISO = "2021-02-03T04:05:06Z",
                updatedDateISO = "2022-03-04T05:06:07Z"
            )
            val id = dao.insert(track)
            val flowId1 = dao.getById(id)
            assertThat(flowId1.first()).isEqualTo(track.copy(id=1))
        }
    }

    @Test
    @Throws(Exception::class)
    fun insert_dupEntity() {
        runBlocking {
            val track0 = GpsTrackEntity(
                id = 0, // auto-generates a new unique id.
                uuid = "test-uuid-1",
                trackLabel = "Test Track 1",
                locationList = locationDataList { },
                createdDateISO = "2021-02-03T04:05:06Z",
                updatedDateISO = "2022-03-04T05:06:07Z"
            )
            val track1 = track0.copy(id = 1)
            val id1 = dao.insert(track0)
            val flowId1 = dao.getById(id1)
            assertThat(flowId1.first()).isEqualTo(track1)

            // Try to insert the same track again with an existing id
            // This next call fails with exception message:
            // android.database.sqlite.SQLiteConstraintException: UNIQUE constraint failed: gps_tracks.id
            val thrownException = assertThrows(SQLiteConstraintException::class.java) {
                runBlocking {
                    dao.insert(track1)
                }
            }
            assertThat(thrownException).hasMessageThat().contains("UNIQUE constraint failed: gps_tracks.id")

        }
    }
}
