package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData

interface IOpActor {

    val currentTrackId: Long?

    val currentLocationsList: List<LocationData>?

    fun stop()
}
