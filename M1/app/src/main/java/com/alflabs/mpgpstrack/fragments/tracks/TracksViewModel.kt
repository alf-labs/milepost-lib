package com.alflabs.mpgpstrack.fragments.tracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alflabs.mpgpstrack.tracks.TrackDisplayInfo
import com.alflabs.mpgpstrack.tracks.TrackEntity
import com.alflabs.mpgpstrack.tracks.TrackEntityDao
import com.alflabs.mpgpstrack.tracks.trackSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@HiltViewModel
class TracksViewModel @Inject constructor(
    private val trackEntityDao: TrackEntityDao
) : ViewModel() {

    val tracks = trackEntityDao.getAllDisplayInfo().asLiveData()

    fun createRandomTrack() {
        val a = Random.nextInt( 1, 1000) / 10f
        val b = Random.nextInt(50, 1000) / 10f
        viewModelScope.launch {
            var track = TrackEntity(
                label = "Track ${Random.nextInt(1000)}",
                createdDateISO = Date().toString(),
                summary = trackSummary {
                    mpMin = min(a, b)
                    mpMax = max(a, b)
                },
            )
            val id = trackEntityDao.insert(track)
            track = track.copy(id = id)
            println("@@ DB INSERT track = $track")
        }
    }

    suspend fun getIdByUuid(uuid: String): Long? {
        return trackEntityDao.getIdByUuid(uuid)
    }

    suspend fun getByUuid(uuid: String): TrackEntity? {
        return trackEntityDao.getByUuid(uuid)
    }

    /** Insert a TrackEntity. Set id to 0 to have it auto-generated. The new id is returned. */
    fun insert(track: TrackEntity) {
        viewModelScope.launch {
            trackEntityDao.insert(track)
        }
    }

    /** Update a TrackEntity with a matching id. */
    fun update(track: TrackEntity) {
        viewModelScope.launch {
            trackEntityDao.update(track)
        }
    }

    fun delete(trackInfo: TrackDisplayInfo) {
        viewModelScope.launch {
            trackEntityDao.deleteById(trackInfo.id)
            println("@@ DB DELETE track = $trackInfo")
        }
    }
}
