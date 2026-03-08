package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData

/** An operation actor (implementation) that can handle GPS entities and locations. */
interface IOpGpsActor : IOpActor {
    val currentGpsTrackId: Long?
    val currentLocationsList: List<LocationData>?
}
