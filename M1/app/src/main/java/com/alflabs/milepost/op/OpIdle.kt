package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData

class OpIdle : IOpActor {
    override val currentGpsTrackId: Long?
        get() = null

    override val currentLocationsList: List<LocationData>?
        get() = null

    override fun stop() {
        // no-op
    }
}
