package com.alflabs.milepost.service

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.alflabs.milepost.R
import com.alflabs.milepost.fragments.PermissionsFragment
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.location.latLongData
import com.alflabs.milepost.location.locationData
import com.alflabs.milepost.location.speedData
import com.alflabs.milepost.app.AppSettings
import com.alflabs.milepost.app.GlobalState
import com.alflabs.milepost.activity.MpActivity
import com.alflabs.milepost.tracks.GpsTrackEntityDao
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

    companion object {
        private val TAG = LocationService::class.java.simpleName
        private const val NOTIFICATION_ID = 1000

        var serviceMode: ServiceMode = ServiceMode.STOPPED
            private set

        fun startService(activity: Activity) {
            Log.d(TAG, "startService, currently in state $serviceMode")
            if (serviceMode == ServiceMode.STOPPED) {
                val intent = Intent(activity, LocationService::class.java)
                activity.startForegroundService(intent)
            }
        }

        fun stopService(activity: Activity) {
            Log.d(TAG, "stopService, currently in state $serviceMode")
            val intent = Intent(activity, LocationService::class.java)
            activity.stopService(intent)
        }
    }

    @Inject lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    @Inject lateinit var locationRequest: LocationRequest
    @Inject lateinit var logger: ILogger
    @Inject lateinit var globalState: GlobalState
    @Inject lateinit var gpsTrackEntityDao: GpsTrackEntityDao
    @Inject lateinit var appSettings: AppSettings

    private lateinit var locationCallback: LocationCallback
    private val serviceModePublisher: IPublisher<ServiceMode?> = Publishers.latest()
    private val locationPublisher: IPublisher<LocationData?> = Publishers.latest()

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d(TAG, "onStartCommand: $intent")

        globalState.serviceModeStream.publishWith(serviceModePublisher)
        globalState.locationStream.publishWith(locationPublisher)

        serviceMode = ServiceMode.RUNNING
        serviceModePublisher.publish(serviceMode)

        if (!PermissionsFragment.allPermissionsGranted(this)) {
            logger.d(TAG, "onStartCommand: No permission")
            return START_NOT_STICKY
        }

        val mpActivityIntent = Intent(this, MpActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, mpActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: Waiting for first fix")
            .setSmallIcon(R.mipmap.ic_launcher_mp)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager == null) {
            logger.d(TAG, "onStartCommand: NotificationManager not found")
            return START_NOT_STICKY
        }
        notificationManager.createNotificationChannel(
            NotificationChannel(
                "location",
                "Location",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        startForeground(NOTIFICATION_ID, notification.build())

        setNotification(notification, notificationManager)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // Publish (synchronous)
                    val ld = toLocationData(location)
                    locationPublisher.publish(ld)

                    // Update notification text
                    val msg = "Location: ${location.latitude}, ${location.longitude}"
                    val updatedNotification = notification.setContentText(msg)
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())
                }
            }
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
        serviceMode = ServiceMode.STOPPED
        serviceModePublisher.publish(serviceMode)
        globalState.locationStream.remove(locationPublisher)
        globalState.serviceModeStream.remove(serviceModePublisher)
    }

    private fun setNotification(
        notification: NotificationCompat.Builder,
        notificationManager: NotificationManager,
    ) {
        val statusText = "Location Service: Getting first fix"
        val updatedNotification = notification.setContentText(statusText)
        notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())
    }
}

private fun toLocationData(location: Location): LocationData = locationData {
    latLong = latLongData {
        timestampMillis = location.time
        latitude = location.latitude
        longitude = location.longitude
        if (location.hasAccuracy()) {
            accuracy = location.accuracy
        }
    }

    if (location.hasSpeed()) {
        speed = speedData {
            speedMps = location.speed
            if (location.hasSpeedAccuracy()) {
                speedAcc = location.speedAccuracyMetersPerSecond
            }
        }
    }
}
