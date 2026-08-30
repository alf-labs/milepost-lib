package com.alflabs.milepostlib.fragments.docentPlayback

import androidx.lifecycle.ViewModel
import com.alflabs.milepostlib.db.DocentTrackEntity
import com.alflabs.milepostlib.location.LocationData
import com.alflabs.milepostlib.op.MpDirection
import com.alflabs.milepostlib.op.MpEstimate
import com.alflabs.milepostlib.db.GpsTrackEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DocentPlaybackViewModel @Inject constructor() : ViewModel() {
    var docentPlaybackTrack: DocentTrackEntity? = null
    var gpsPlaybackTrack: GpsTrackEntity? = null
    val triggers = mutableListOf<DocentPlaybackItem>()
    val filteredLocations = mutableListOf<LocationData>()
    var startLabel: String = "Startbound"
    var endLabel: String = "Endbound"

    var lastMp: MpEstimate? = null
    var lastMpIndexRendered: Int = -1
    var lastMpDirectionRendered: MpDirection = MpDirection.UNKNOWN
}
