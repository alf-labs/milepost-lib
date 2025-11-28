package com.alflabs.mpgpstrack.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.alflabs.mpgpstrack.R
import com.alflabs.mpgpstrack.dagger.CurrentLocation
import com.alflabs.rx.IPublisher
import com.alflabs.rx.Publishers
import com.alflabs.utils.ILogger
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {
    private val TAG = LocationService::class.java.simpleName

    @Inject lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    @Inject lateinit var locationRequest: LocationRequest
    @Inject lateinit var logger: ILogger
    @Inject lateinit var currentLocation: CurrentLocation

    private lateinit var locationCallback: LocationCallback

    private val locationPublisher: IPublisher<String?> = Publishers.latest()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d(TAG, "onStartCommand: ")
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                "location",
                "Location",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        startForeground(1, notification.build())

        currentLocation.start()
        currentLocation.location.publishWith(locationPublisher)
        locationPublisher.publish("Location Service Started")
        var idx = 0
        val indicator = "|/-\\"

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val indic = indicator[idx++ % 4]
                    var msg = "Location: ${location.latitude}, ${location.longitude}"
                    if (location.hasSpeed()) {
                        msg += "\nSpeed: ${location.speed} mps +/- ${location.speedAccuracyMetersPerSecond}"
                    }
                    if (location.hasBearing()) {
                        msg += "\nBearing: ${location.bearing} deg +/- ${location.bearingAccuracyDegrees}"
                    }
                    msg += "\n$indic"
                    logger.d(TAG, msg)

                    locationPublisher.publish(msg)

                    val updatedNotification = notification.setContentText(msg)
                    notificationManager.notify(1, updatedNotification.build())
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            logger.d(TAG, "onStartCommand: No permission")
            return START_NOT_STICKY
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        logger.d(TAG, "Started")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        logger.d(TAG, "onBind")
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.d(TAG, "onDestroy")
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        locationPublisher.publish("Location Service Stopped")
        currentLocation.location.remove(locationPublisher)
    }
}
