package com.alflabs.milepostlib.service

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.alflabs.milepostlib.R
import com.alflabs.milepostlib.activity.MpActivity
import com.alflabs.milepostlib.app.AppSettings
import com.alflabs.milepostlib.app.GlobalState
import com.alflabs.milepostlib.fragments.PermissionsFragment
import com.alflabs.milepostlib.location.LocationData
import com.alflabs.milepostlib.location.latLongData
import com.alflabs.milepostlib.location.locationData
import com.alflabs.milepostlib.location.speedData
import com.alflabs.milepostlib.op.OperatorMode
import com.alflabs.rx.AndroidSchedulers
import com.alflabs.rx.IPublisher
import com.alflabs.rx.ISubscriber
import com.alflabs.rx.Publishers
import com.alflabs.rx.Schedulers
import com.alflabs.utils.ILogger
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Optional
import javax.inject.Inject


@AndroidEntryPoint
class LocationService : LifecycleService() {

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

    @Inject lateinit var logger: ILogger
    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var globalState: GlobalState
    @Inject lateinit var internalGpsProvider: InternalGpsProvider
    @Inject lateinit var externalGpsProvider: ExternalGpsProvider
    @Inject lateinit var simulationGpsProvider: SimulationGpsProvider
    @Inject lateinit var docentMediaPlayerFactory: DocentMediaPlayer.Factory
    @Inject lateinit var notificationManagerOpt: Optional<NotificationManager>

    private lateinit var locationCallback: LocationCallback
    private lateinit var docentMediaPlayer: DocentMediaPlayer
    private val serviceModePublisher: IPublisher<ServiceMode?> = Publishers.latest()
    private val locationPublisher: IPublisher<LocationData?> = Publishers.latest()
    private val operatorModeSubscriber: ISubscriber<in OperatorMode?> =
        ISubscriber<OperatorMode?> { _, event -> onGlobalModeChanged(event) }
    private var currentOpMode: OperatorMode? = null
    private var currentGpsProvider: IGpsProvider? = null

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        logger.d(TAG, "onStartCommand: $intent")

        docentMediaPlayer = docentMediaPlayerFactory.create(this, lifecycleScope)
        globalState.serviceModeStream.publishWith(serviceModePublisher)
        globalState.locationStream.publishWith(locationPublisher)

        serviceMode = ServiceMode.RUNNING
        serviceModePublisher.publish(serviceMode)

        val mpActivityIntent = Intent(this, MpActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, mpActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: Waiting for first fix")
            .setSmallIcon(R.mipmap.ic_launcher_lib)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        if (notificationManagerOpt.isEmpty) {
            logger.d(TAG, "onStartCommand: NotificationManager not found")
            return START_NOT_STICKY
        }

        notificationManagerOpt.ifPresent { notifManager ->
            notifManager.createNotificationChannel(
                NotificationChannel(
                    "location",
                    "Location",
                    NotificationManager.IMPORTANCE_LOW
                )
            )

            startForeground(NOTIFICATION_ID, notification.build())

            setNotification(notification, notifManager)

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        // Publish (synchronous)
                        val ld = toLocationData(location)
                        locationPublisher.publish(ld)

                        // Update notification text
                        val msg = "Location: ${location.latitude}, ${location.longitude}"
                        val updatedNotification = notification.setContentText(msg)
                        notifManager.notify(NOTIFICATION_ID, updatedNotification.build())
                    }
                }
            }
        }

        lifecycleScope.launch {
            globalState.operatorModeStream.subscribe(
                Schedulers.sync(),
                operatorModeSubscriber
            )
            startSimulation(globalState.operatorMode)
        }

        logger.d(TAG, "LocService Started Not Sticky")
        return START_NOT_STICKY
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    private fun onGlobalModeChanged(opMode: OperatorMode?) {
        logger.d(TAG, "@@ LocService: OpMode changed to $opMode")
        opMode?.let { newOpMode ->
            lifecycleScope.launch {
                val testingSensors = newOpMode == OperatorMode.SENSORS
                val simulation = appSettings.simulateGps().contains("simu" /* see arrays.xml */)

                // We would run the simulationGpsProvider if simu is requested and NOT testing.
                val wouldRunSimulation = simulation && !testingSensors
                // Would we keep the same provider?
                val sameProvider = (currentGpsProvider is SimulationGpsProvider) == wouldRunSimulation
                // If not the same, we want to change.
                if (!sameProvider) {
                    startSimulation(newOpMode)
                }
            }
        }
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION]
    )
    private suspend fun startSimulation(opMode: OperatorMode) {
        currentOpMode = opMode
        logger.d(TAG, "@@ LocService: Choosing GPS Provider")

        currentGpsProvider?.let {
            it.stop()
            currentGpsProvider = null
        }

        // We only run the simulationGpsProvider if simu is requrested and NOT testing.
        val testingSensors = currentOpMode == OperatorMode.SENSORS
        val simulation = appSettings.simulateGps().contains("simu" /* see arrays.xml */)
        if (simulation && !testingSensors) {
            // Simulation mode, no need to check permissions.
            currentGpsProvider = simulationGpsProvider
            currentGpsProvider?.start(locationCallback)
        } else if (!PermissionsFragment.allPermissionsGranted(
                this@LocationService, externalGpsProvider)) {
            logger.d(TAG, "onStartCommand: No permission")
        } else {
            // Real GPS mode
            var started = false
            if (appSettings.useExternalGps()) {
                currentGpsProvider = externalGpsProvider
                started = currentGpsProvider!!.start(locationCallback)
            }
            if (!started) {
                currentGpsProvider = internalGpsProvider
                started = currentGpsProvider!!.start(locationCallback)
            }
            logger.d(TAG, "@@ LocService: GPS Provider started: $started")
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        logger.d(TAG, "onBind")
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.d(TAG, "onDestroy")
        docentMediaPlayer.onDestroy()
        internalGpsProvider.onDestroy()
        externalGpsProvider.onDestroy()
        simulationGpsProvider.onDestroy()
        serviceMode = ServiceMode.STOPPED
        serviceModePublisher.publish(serviceMode)
        globalState.operatorModeStream.remove(operatorModeSubscriber)
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
