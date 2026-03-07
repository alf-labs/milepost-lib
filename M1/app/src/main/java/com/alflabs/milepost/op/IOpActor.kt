package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData

interface IOpActor {

    val currentDocentTrackId: Long?
    val currentGpsTrackId: Long?

    val currentLocationsList: List<LocationData>?

    fun stop()
}
