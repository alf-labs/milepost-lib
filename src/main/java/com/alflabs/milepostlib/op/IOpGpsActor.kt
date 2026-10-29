package com.alflabs.milepostlib.op

import com.alflabs.milepostlib.location.LocationData

/** An operation actor (implementation) that can handle GPS entities and locations. */
interface IOpGpsActor : IOpActor {
    val currentGpsTrackId: Long?
    val currentLocationsList: List<LocationData>?
}
