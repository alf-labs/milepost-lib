package com.alflabs.mpgpstrack.tracks

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alflabs.mpgpstrack.location.locationDataList
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TrackEntityDaoTest {
    private lateinit var dao: TrackEntityDao
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
    fun writeTrackAndReadInList() {
        runBlocking {
            val track = TrackEntity(
                uuid = "test-uuid-1",
                label = "Test Track 1",
                locationList = locationDataList { },
                createdDateISO = "2021-02-03T04:05:06Z",
                updatedDateISO = "2022-03-04T05:06:07Z"
            )
            val id = dao.insert(track)
            val stored = dao.getById(id)
            assertThat(stored).isEqualTo(track.copy(id=1))
        }
    }
}
