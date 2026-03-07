package com.alflabs.milepost.fragments.docentPlayback

import androidx.lifecycle.ViewModel
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.op.MpDirection
import com.alflabs.milepost.op.MpEstimate
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.milepost.fragments.docentEditor.DocentEditorItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DocentPlaybackViewModel @Inject constructor() : ViewModel() {
    var docentPlaybackTrack: DocentTrackEntity? = null
    var gpsPlaybackTrack: GpsTrackEntity? = null
    val triggers = mutableListOf<DocentEditorItem>()
    val filteredLocations = mutableListOf<LocationData>()
    var startLabel: String = "Startbound"
    var endLabel: String = "Endbound"

    var lastMp: MpEstimate? = null
    var lastMpIndexRendered: Int = -1
    var lastMpDirectionRendered: MpDirection = MpDirection.UNKNOWN
}
