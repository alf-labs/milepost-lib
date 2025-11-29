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
import com.alflabs.mpgpstrack.dagger.CurrentLatLong
import com.alflabs.mpgpstrack.dagger.CurrentLocation
import com.alflabs.mpgpstrack.dagger.CurrentLocationData
import com.alflabs.mpgpstrack.dagger.CurrentSpeedBearing
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

    private val locationPublisher: IPublisher<CurrentLocationData?> = Publishers.latest()

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
        var currentLocationData = CurrentLocationData(
            running = true,
            status = "Location Service: Getting first fix",
            latLong = null,
            speedBearing = null
        )
        locationPublisher.publish(
            currentLocationData
        )
        val updatedNotification = notification.setContentText(currentLocationData.status)
        notificationManager.notify(1, updatedNotification.build())


        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {

                    val latLong = CurrentLatLong(
                        timestampMillis = location.time,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = if (location.hasAccuracy()) location.accuracy else 0f,
                    )

                    val speedBearing = if (location.hasSpeed() && location.hasBearing()) {
                        CurrentSpeedBearing(
                            speedMps = location.speed,
                            speedAcc = location.speedAccuracyMetersPerSecond,
                            bearingDeg = location.bearing,
                            bearingAcc = location.bearingAccuracyDegrees,
                        )
                    } else {
                        null
                    }

                    currentLocationData = currentLocationData.update(latLong, speedBearing)
                    locationPublisher.publish(currentLocationData)

                    val msg = "Location: ${location.latitude}, ${location.longitude}"
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
        locationPublisher.publish(
            CurrentLocationData(
                running = false,
                status = "Location Service Stopped",
                latLong = null,
                speedBearing = null
            ))
        currentLocation.location.remove(locationPublisher)
    }
}
