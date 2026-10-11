package com.alflabs.milepostlib.service

import com.google.android.gms.location.LocationCallback


interface IGpsProvider {
    suspend fun start(locationCallback: LocationCallback): Boolean
    fun stop()
    fun onDestroy()

}
