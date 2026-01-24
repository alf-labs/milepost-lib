package com.alflabs.milepost.fragments.playback

import androidx.lifecycle.ViewModel
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.op.MpDirection
import com.alflabs.milepost.op.MpEstimate
import com.alflabs.milepost.tracks.GpsTrackEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor() : ViewModel() {
    var playbackTrack: GpsTrackEntity? = null
    val filteredLocations = mutableListOf<LocationData>()
    var startLabel: String = "Startbound"
    var endLabel: String = "Endbound"

    var lastMp: MpEstimate? = null
    var lastMpIndexRendered: Int = -1
    var lastMpDirectionRendered: MpDirection = MpDirection.UNKNOWN
}
