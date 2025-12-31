package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData

interface IOpActor {

    val currentTrackId: Long?

    fun stop()
}
