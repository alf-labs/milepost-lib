package com.alflabs.mpgpstrack.fragments.review

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alflabs.mpgpstrack.tracks.TrackEntity
import com.alflabs.mpgpstrack.tracks.TrackEntityDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val trackEntityDao: TrackEntityDao
) : ViewModel() {
    private val TAG = ReviewViewModel::class.java.simpleName

    private val _track = MutableLiveData<TrackEntity?>()
    val track: LiveData<TrackEntity?> = _track

    fun loadTrack(trackId: Long) {
        viewModelScope.launch {
            Log.d(TAG, "Review load track by id #$trackId")
            _track.value = trackEntityDao.getById(trackId)
        }
    }
}
