package com.alflabs.milepost.fragments.review

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.alflabs.milepost.tracks.GpsTrackEntity
import com.alflabs.milepost.tracks.GpsTrackDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GpsReviewViewModel @Inject constructor(
    private val gpsTrackDao: GpsTrackDao
) : ViewModel() {

    companion object {
        private val TAG = GpsReviewViewModel::class.java.simpleName
    }

    private val _trackId = MutableLiveData<Long>()

    val track: LiveData<GpsTrackEntity?> = _trackId.switchMap { id ->
        gpsTrackDao.getById(id).asLiveData()
    }

    fun loadTrack(trackId: Long) {
        Log.d(TAG, "Review load track by id #$trackId")
        _trackId.value = trackId
    }
}

