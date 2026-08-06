package com.alflabs.milepost.service

import android.content.Context
import com.alflabs.utils.ILogger
import com.google.android.gms.location.LocationCallback
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject


class ExternalGpsProvider @AssistedInject constructor(
    @Assisted private val context: Context,
    private val logger: ILogger,
) {
    @AssistedFactory
    interface Factory {
        fun create(context: Context): ExternalGpsProvider
    }

    companion object {
        private val TAG = ExternalGpsProvider::class.java.simpleName
    }

    suspend fun start(locationCallback: LocationCallback): Boolean {
        // TBD: The purpose of this method to connect to an external USB GPS,
        // periodically parse NMEA messages, and provide them to the location
        // callback till onDestroy is called.
        //
        // This is called from LocationService.
        // Only the LocationCallback.onLocationResult() method is used.
        // Ideally we'll reuse the GMS LocationResult class to pass data
        // (we only read the fields location.time, latitude, longitude, and accuracy)
        // but if that's not possible we can just make our own data class.
        //
        // The method should ideally return false if we enumerate usb devices
        // and find no usb serial gps receiver at all.
        // That way the caller can fallback on internal gps.
        // In the unlikely event there's more than one external gps, we'll just pick
        // up the first one.
        //
        // Suggested implementation is to run this in a looper so that we can
        // handle disconnections and keep reconnecting as needed, then request
        // messages at about 1 Hz.
        // However since the caller is a lifecycleScope.launch coroutine, we could
        // make this use any kotlin coroutine mechanism for the loop.
        //
        // Specs: our hardware is a "GlobalSat BU-353N5 USB GNSS Receiver"
        // connected via a USB hub to the android tablet. We'll need a
        // USB Serial Driver of some kind (library or implemented here).

        return false
    }

    fun onDestroy() {
        // TBD: Request looper or coroutine to stop as needed and free resources.
    }
}
