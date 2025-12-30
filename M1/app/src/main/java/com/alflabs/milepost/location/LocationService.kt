package com.alflabs.milepost.location

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.alflabs.milepost.main.AppSettings
import com.alflabs.milepost.main.GlobalState
import com.alflabs.milepost.main.ServiceMode
import com.alflabs.milepost.tracks.TrackEntityDao
import com.alflabs.utils.ILogger
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


@AndroidEntryPoint
class LocationService : Service() {

    companion object {
        private val TAG = LocationService::class.java.simpleName
        private const val NOTIFICATION_ID = 1000
    }

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    @Inject
    lateinit var locationRequest: LocationRequest
    @Inject
    lateinit var logger: ILogger
    @Inject
    lateinit var globalState: GlobalState
    @Inject
    lateinit var trackEntityDao: TrackEntityDao
    @Inject
    lateinit var appSettings: AppSettings

    private val scope = CoroutineScope(Dispatchers.IO)

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d(TAG, "onStartCommand: $intent")
        globalState.serviceMode = ServiceMode.RUNNING


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
        globalState.serviceMode = ServiceMode.STOPPED
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
