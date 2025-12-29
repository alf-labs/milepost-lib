package com.alflabs.milepost.location

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
import com.alflabs.milepost.R
import com.alflabs.milepost.fragments.PermissionsFragment
import com.alflabs.milepost.main.AppSettings
import com.alflabs.milepost.main.GlobalState
import com.alflabs.milepost.main.MainActivity
import com.alflabs.milepost.main.StatusData
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.milepost.tracks.TrackEntityDao
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
    @Inject lateinit var appSettings: AppSettings

    private lateinit var locationCallback: LocationCallback
    private var thresholdDistanceMeters = AppSettings.THRESHOLD_DISTANCE_METERS_DEFAULT
    private var thresholdSpeedMps = AppSettings.THRESHOLD_SPEED_MPS_DEFAULT
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

        setNotification(notification, notificationManager)

        thresholdDistanceMeters = appSettings.thresholdDistanceMeters(scope)
        thresholdSpeedMps = appSettings.thresholdSpeedMps(scope)

        scope.launch {
            val track: TrackEntity? = when(runningMode) {
                StatusData.RECORD -> TrackEntity(
                    trackLabel = "", // this triggers the title dialog in the review screen
                    createdDateISO = Date().toString(),
                )
                else -> null
            }

            val recordTempData: RecordTempData? = if (runningMode == StatusData.RECORD) {
                val recordId = trackEntityDao.insert(track!!)
                RecordTempData(
                    recordId = recordId,
                    locationDataListBuilder = LocationDataList.newBuilder(),
                    locationUpdater = { builder -> scope.launch {
                        trackEntityDao.updateLocationList(recordId, builder.build())
                    } },
                )
            } else {
                null
            }

            globalState.locationStream.publishWith(locationPublisher)
            globalState.updateStatus(
                runningMode,
                recordTempData?.recordId,
                recordTempData?.locationDataListBuilder)

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        if (location != null) {
                            val ld = onSingleLocationResult(
                                location,
                                notification,
                                notificationManager)
                            recordTempData?.let {
                                onRecordLocation(
                                    it,
                                    ld,
                                    location)
                            }
                        }
                    }
                }
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

    private fun setNotification(
        notification: NotificationCompat.Builder,
        notificationManager: NotificationManager,
    ) {
        val statusText = "Location Service: Getting first fix"
        val updatedNotification = notification.setContentText(statusText)
        notificationManager.notify(NOTIFICATION_ID, updatedNotification.build())
    }

    private fun onSingleLocationResult(
        location: Location,
        notification: NotificationCompat.Builder,
        notificationManager: NotificationManager,
    ): LocationData {
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

        return ld
    }

    private fun onRecordLocation(
        data: RecordTempData,
        locationData: LocationData,
        location: Location,
    ) {
        val n = data.locationDataListBuilder.locationsCount
        // Always add the first point.
        var shouldAdd = n == 0
        var replaceIndex = -1

        if (!shouldAdd && data.lastLocation != null) {
            val distance = location.distanceTo(data.lastLocation!!)
            // New point added if we're above the distance threshold
            shouldAdd = (distance > thresholdDistanceMeters)

            data.lastLocation?.let { lastLoc ->
                if (distance <= lastLoc.accuracy
                        && location.accuracy < lastLoc.accuracy) {
                    // If the distance delta is below the last point accuracy,
                    // and the accuracy got better,
                    // then consider it a better replacement for the same point.
                    replaceIndex = n - 1
                } else if (!shouldAdd) {
                    // If the point is closer than the threshold distance but the speed
                    // is valid and has fallen under a 1 m/s threshold, consider adding it.
                    var newSpeed = if (location.hasSpeed()) location.speed else -1f
                    var lastSpeed = if (lastLoc.hasSpeed()) lastLoc.speed else -1f
                    if (location.hasSpeedAccuracy() && newSpeed < location.speedAccuracyMetersPerSecond) {
                        newSpeed = -1f
                    }
                    if (lastLoc.hasSpeedAccuracy() && lastSpeed < lastLoc.speedAccuracyMetersPerSecond) {
                        lastSpeed = -1f
                    }
                    shouldAdd = lastSpeed > thresholdSpeedMps
                        && newSpeed >= 0
                        && newSpeed < thresholdSpeedMps
                }
            }
        }

        if (replaceIndex >= 0) {
            data.locationDataListBuilder.setLocations(
                replaceIndex,
                locationData
            )
            data.lastLocation = location
            data.locationUpdater(data.locationDataListBuilder)

        } else if (shouldAdd) {
            data.locationDataListBuilder.addLocations(locationData)
            data.lastLocation = location
            data.locationUpdater(data.locationDataListBuilder)
        }
    }
}

private data class RecordTempData(
    val recordId: Long,
    val locationDataListBuilder: LocationDataList.Builder,
    val locationUpdater: (LocationDataList.Builder) -> Unit,
    var lastLocation: Location? = null,
)
