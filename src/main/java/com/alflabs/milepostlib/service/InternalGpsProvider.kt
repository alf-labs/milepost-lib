package com.alflabs.milepostlib.service

import android.Manifest
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.alflabs.utils.ILogger
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class InternalGpsProvider @Inject constructor(
    private val logger: ILogger,
    private val locationRequest: LocationRequest,
    private val fusedLocationProviderClient: FusedLocationProviderClient,
) {

    companion object {
        private val TAG = InternalGpsProvider::class.java.simpleName
    }

    private var locationCallback: LocationCallback? = null

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    suspend fun start(locationCallback: LocationCallback): Boolean {
        // Note: this call would fail if the proper permissions have not been granted
        // beforehand. The fragment handles the permissions requests before
        // starting the GPS provider, to avoid receiving a permission exception here.
        try {
            stop()
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            this.locationCallback = locationCallback
            logger.d(TAG, "@@ InternalGpsProvider started")
            return true
        } catch (e: Exception) {
            logger.d(TAG, "@@ InternalGpsProvider failed", e)
            return false
        }
    }

    fun onDestroy() {
        stop()
    }

    private fun stop() {
        locationCallback?.let { callback ->
            fusedLocationProviderClient.removeLocationUpdates(callback)
            locationCallback = null
        }

    }

}
