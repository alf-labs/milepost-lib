package com.alflabs.milepost.fragments.review

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.milepost.tracks.TrackEntityDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val trackEntityDao: TrackEntityDao
) : ViewModel() {

    companion object {
        private val TAG = ReviewViewModel::class.java.simpleName
    }

    private val _trackId = MutableLiveData<Long>()

    val track: LiveData<TrackEntity?> = _trackId.switchMap { id ->
        trackEntityDao.getById(id).asLiveData()
    }

    fun loadTrack(trackId: Long) {
        Log.d(TAG, "Review load track by id #$trackId")
        _trackId.value = trackId
    }
}

