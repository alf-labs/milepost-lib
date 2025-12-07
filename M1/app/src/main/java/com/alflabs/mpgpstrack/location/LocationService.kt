package com.alflabs.mpgpstrack.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewModelScope
import com.alflabs.mpgpstrack.R
import com.alflabs.mpgpstrack.fragments.PermissionsFragment
import com.alflabs.mpgpstrack.main.MainActivity
import com.alflabs.mpgpstrack.main.GlobalState
import com.alflabs.mpgpstrack.main.StatusData
import com.alflabs.mpgpstrack.tracks.AppDatabase
import com.alflabs.mpgpstrack.tracks.TrackEntity
import com.alflabs.mpgpstrack.tracks.TrackEntityDao
import com.alflabs.mpgpstrack.tracks.trackSummary
import com.alflabs.rx.IPublisher
import com.alflabs.rx.Publishers
import com.alflabs.utils.ILogger
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


@AndroidEntryPoint
class LocationService : Service() {
    private val TAG = LocationService::class.java.simpleName

    companion object {
        const val EXTRA_RUNNING_MODE = "status"
        private const val NOTIFICATION_ID = 1000
    }

    @Inject lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    @Inject lateinit var locationRequest: LocationRequest
    @Inject lateinit var logger: ILogger
    @Inject lateinit var globalState: GlobalState
    @Inject lateinit var trackEntityDao: TrackEntityDao

    private lateinit var locationCallback: LocationCallback
    private val locationPublisher: IPublisher<LocationData?> = Publishers.latest()
    private val scope = CoroutineScope(Dispatchers.IO)

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d(TAG, "onStartCommand: $intent")

        val runningMode = intent
            ?.getSerializableExtra(EXTRA_RUNNING_MODE, StatusData::class.java)
            ?: StatusData.STOPPED

        // Sanity check on desired running mode and required permissions.
        if (runningMode == StatusData.STOPPED) {
            globalState.updateStatus(StatusData.STOPPED)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!PermissionsFragment.allPermissionsGranted(this)) {
            logger.d(TAG, "onStartCommand: No permission")
            return START_NOT_STICKY
        }

        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: Waiting for first fix")
            .setSmallIcon(R.mipmap.ic_launcher)
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

        setInitialState(runningMode, notification, notificationManager)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (location != null) {
                        onSingleLocationResult(location, notification, notificationManager)
                    }
                }
            }
        }

        scope.launch {
            if (runningMode == StatusData.RECORD) {
                val a = Random.nextInt(1, 1000) / 10f
                val b = Random.nextInt(50, 1000) / 10f
                var track = TrackEntity(
                    label = "Record ${Random.nextInt(1000)}",
                    createdDateISO = Date().toString(),
                    summary = trackSummary {
                        mpMin = min(a, b)
                        mpMax = max(a, b)
                    },
                )
                val id = trackEntityDao.insert(track)
                logger.d(TAG, "@@ RECORD to $id")
            }

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

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
        globalState.updateStatus(StatusData.STOPPED)
        globalState.locationStream.remove(locationPublisher)
    }

    private fun setInitialState(
        status: StatusData,
        notification: NotificationCompat.Builder,
        notificationManager: NotificationManager,
    ) {
        globalState.locationStream.publishWith(locationPublisher)
        val statusText = "Location Service: Getting first fix"
        globalState.updateStatus(status)
        val updatedNotification = notification.setContentText(statusText)
        notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())
    }

    private fun onSingleLocationResult(
        location: Location,
        notification: NotificationCompat.Builder,
        notificationManager: NotificationManager,
    ) {
        val ld = locationData {
            latLong = latLongData {
                timestampMillis = location.time
                latitude = location.latitude
                longitude = location.longitude
                if (location.hasAccuracy()) {
                    accuracy = location.accuracy
                }
            }

            if (location.hasSpeed() && location.hasBearing()) {
                speedBearing = speedBearingData {
                    speedMps = location.speed
                    bearingAcc = location.bearing
                    if (location.hasSpeedAccuracy()) {
                        speedAcc = location.speedAccuracyMetersPerSecond
                    }
                    if (location.hasBearingAccuracy()) {
                        bearingAcc = location.bearingAccuracyDegrees
                    }
                }
            }
        }

        locationPublisher.publish(ld)

        val msg = "Location: ${location.latitude}, ${location.longitude}"
        val updatedNotification = notification.setContentText(msg)
        notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())
    }
}
