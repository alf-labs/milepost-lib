package com.alflabs.milepost.location

import android.location.Location

class LocationDistance {
    private val distResults = FloatArray(1)

    fun between(ll1: LatLongData, ll2: LatLongData): Float {
        Location.distanceBetween(
            ll1.latitude, ll1.longitude,
            ll2.latitude, ll2.longitude,
            distResults
        )
        return distResults[0]
    }
}
