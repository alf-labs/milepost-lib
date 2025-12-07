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

    fun updateTrack(trackInfo: TrackDisplayInfo) {
        viewModelScope.launch {
            val track = trackEntityDao.getById(trackInfo.id)
            track?.let {
                val newTrack = it.copy(
                    label = trackInfo.label,
                    summary = trackInfo.summary)
                trackEntityDao.update(newTrack)
                println("@@ DB UPDATE track = $newTrack")
            }
        }
    }

    fun delete(trackInfo: TrackDisplayInfo) {
        viewModelScope.launch {
            trackEntityDao.deleteById(trackInfo.id)
            println("@@ DB DELETE track = $trackInfo")
        }
    }
}
