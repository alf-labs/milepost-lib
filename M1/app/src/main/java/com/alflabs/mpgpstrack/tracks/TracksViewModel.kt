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
            val track = TrackData(
                name = "Track ${Random.nextInt(1000)}",
                date = Date().toString(),
                minMP = min(a, b),
                maxMP = max(a, b),
            )
            trackDataDao.insert(track)
        }
    }

    fun updateTrack(track: TrackData) {
        viewModelScope.launch {
            trackDataDao.update(track)
        }
    }

    fun delete(track: TrackData) {
        viewModelScope.launch {
            trackDataDao.delete(track)
        }
    }
}
