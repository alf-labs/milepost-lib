package com.alflabs.milepost.fragments.tracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alflabs.milepost.tracks.TrackDisplayInfo
import com.alflabs.milepost.tracks.GpsTrackEntity
import com.alflabs.milepost.tracks.GpsTrackEntityDao
import com.alflabs.milepost.tracks.trackSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@HiltViewModel
class TracksViewModel @Inject constructor(
    private val gpsTrackEntityDao: GpsTrackEntityDao
) : ViewModel() {

    val tracks = gpsTrackEntityDao.getAllDisplayInfo().asLiveData()

    fun createRandomTrack() {
        val a = Random.nextInt( 1, 1000) / 10f
        val b = Random.nextInt(50, 1000) / 10f
        viewModelScope.launch {
            var track = GpsTrackEntity(
                trackLabel = "Track ${Random.nextInt(1000)}",
                createdDateISO = Date().toString(),
                summary = trackSummary {
                    mpMin = min(a, b)
                    mpMax = max(a, b)
                },
            )
            val id = gpsTrackEntityDao.insert(track)
            track = track.copy(id = id)
            println("@@ DB INSERT track = $track")
        }
    }

    suspend fun getIdByUuid(uuid: String): Long? {
        return gpsTrackEntityDao.getIdByUuid(uuid)
    }

    suspend fun getByUuid(uuid: String): GpsTrackEntity? {
        return gpsTrackEntityDao.getByUuid(uuid)
    }

    /** Insert a GpsTrackEntity. Set id to 0 to have it auto-generated. The new id is returned. */
    fun insert(track: GpsTrackEntity) {
        viewModelScope.launch {
            gpsTrackEntityDao.insert(track)
        }
    }

    /** Update a GpsTrackEntity with a matching id. */
    fun update(track: GpsTrackEntity) {
        viewModelScope.launch {
            gpsTrackEntityDao.update(track)
        }
    }

    fun delete(trackInfo: TrackDisplayInfo) {
        viewModelScope.launch {
            gpsTrackEntityDao.deleteById(trackInfo.id)
            println("@@ DB DELETE track = $trackInfo")
        }
    }
}
