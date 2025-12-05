package com.alflabs.mpgpstrack.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.alflabs.mpgpstrack.app.TrackDataDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TracksViewModel @Inject constructor(
    trackDataDao: TrackDataDao
) : ViewModel() {

    val tracks = trackDataDao.getAll().asLiveData()
}
