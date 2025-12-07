package com.alflabs.mpgpstrack.fragments.tracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.alflabs.mpgpstrack.tracks.TrackEntity
import com.alflabs.mpgpstrack.tracks.TrackEntityDao
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

    val tracks = trackEntityDao.getAll().asLiveData()

    fun createRandomTrack() {
        val a = Random.nextInt( 1, 1000) / 10f
        val b = Random.nextInt(50, 1000) / 10f
        viewModelScope.launch {
            var track = TrackEntity(
                label = "Track ${Random.nextInt(1000)}",
                createdDateISO = Date().toString(),
                minMP = min(a, b),
                maxMP = max(a, b),
            )
            val id = trackEntityDao.insert(track)
            track = track.copy(id = id)
            println("@@ DB INSERT track = $track")
        }
    }

    fun updateTrack(track: TrackEntity) {
        viewModelScope.launch {
            trackEntityDao.update(track)
            println("@@ DB UPDATE track = $track")
        }
    }

    fun delete(track: TrackEntity) {
        viewModelScope.launch {
            trackEntityDao.delete(track)
            println("@@ DB DELETE track = $track")
        }
    }
}
