package com.alflabs.milepostlib.fragments.gpsPlayback

import androidx.lifecycle.ViewModel
import com.alflabs.milepostlib.location.LocationData
import com.alflabs.milepostlib.op.MpDirection
import com.alflabs.milepostlib.op.MpEstimate
import com.alflabs.milepostlib.db.GpsTrackEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GpsPlaybackViewModel @Inject constructor() : ViewModel() {
    var gpsPlaybackTrack: GpsTrackEntity? = null
    val filteredLocations = mutableListOf<LocationData>()
    var startLabel: String = "Startbound"
    var endLabel: String = "Endbound"

    var lastMp: MpEstimate? = null
    var lastMpIndexRendered: Int = -1
    var lastMpDirectionRendered: MpDirection = MpDirection.UNKNOWN
}
