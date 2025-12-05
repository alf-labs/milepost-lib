package com.alflabs.mpgpstrack.tracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@HiltViewModel
class TracksViewModel @Inject constructor(
    private val trackDataDao: TrackDataDao
) : ViewModel() {

    val tracks = trackDataDao.getAll().asLiveData()

    fun createRandomTrack() {
        val a = Random.nextInt( 1, 1000) / 10f
        val b = Random.nextInt(50, 1000) / 10f
        viewModelScope.launch {
            var track = TrackData(
                name = "Track ${Random.nextInt(1000)}",
                date = Date().toString(),
                minMP = min(a, b),
                maxMP = max(a, b),
            )
            val id = trackDataDao.insert(track)
            track = track.copy(id = id)
            println("@@ DB INSERT track = $track")
        }
    }

    fun updateTrack(track: TrackData) {
        viewModelScope.launch {
            trackDataDao.update(track)
            println("@@ DB UPDATE track = $track")
        }
    }

    fun delete(track: TrackData) {
        viewModelScope.launch {
            trackDataDao.delete(track)
            println("@@ DB DELETE track = $track")
        }
    }
}
