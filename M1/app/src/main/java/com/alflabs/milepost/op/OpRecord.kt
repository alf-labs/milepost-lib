package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.location.LocationDataList
import com.alflabs.milepost.location.LocationDistance
import com.alflabs.milepost.app.AppSettings
import com.alflabs.milepost.app.GlobalState
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.milepost.tracks.TrackEntityDao
import com.alflabs.rx.ISubscriber
import com.alflabs.rx.Schedulers
import com.alflabs.utils.IClock
import com.alflabs.utils.ILogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OpRecord @AssistedInject constructor(
    @Assisted val track: TrackEntity,
    private val clock: IClock,
    private val logger: ILogger,
    private val globalState: GlobalState,
    private val appSettings: AppSettings,
    private val trackEntityDao: TrackEntityDao,
) : IOpActor {

    @AssistedFactory
    interface Factory {
        fun create(track: TrackEntity): OpRecord
    }

    companion object {
        private val TAG = OpRecord::class.java.simpleName
        private const val WRITE_INTERVAL_MS = 30_000
    }

    override val currentTrackId: Long
        get() = track.id

    override val currentLocationsList: List<LocationData>?
        get() = locationDataListBuilder.locationsList

    private val locationSubscriber: ISubscriber<in LocationData?> =
        ISubscriber<LocationData?> { _, event -> onLocationUpdate(event) }
    private val recordId: Long = track.id
    private var lastLocation: LocationData? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val locationDataListBuilder = LocationDataList.newBuilder()
    private val locationDistance = LocationDistance()
    private var thresholdDistanceMeters = AppSettings.THRESHOLD_DISTANCE_METERS_DEFAULT
    private var thresholdSpeedMps = AppSettings.THRESHOLD_SPEED_MPS_DEFAULT
    private var nextWriteTS = -1L
    private var needsWrite = true

    init {
        logger.d(TAG, "init: ${track.id}")
        globalState.locationStream.subscribe(Schedulers.sync(), locationSubscriber)
    }

    suspend fun initAsync() : OpRecord {
        thresholdDistanceMeters = appSettings.thresholdDistanceMeters()
        thresholdSpeedMps = appSettings.thresholdSpeedMps()
        return this
    }

    override fun stop() {
        logger.d(TAG, "stop")
        locationUpdater(writeNow = true)
        globalState.locationStream.remove(locationSubscriber)
    }

    fun onLocationUpdate(newLocation: LocationData?) {
        // Note: this call is synchronous and happens on the MpActivity Looper thread
        // as invoked by the FusedLocationProviderClient.
        logger.d(TAG, "onLocationChanged: ${newLocation?.latLong?.timestampMillis}")
        if (newLocation == null) return

        val newLatLong = newLocation.latLong
        val n = locationDataListBuilder.locationsCount
        // Always add the first point.
        var shouldAdd = n == 0
        var replaceIndex = -1

        if (!shouldAdd && lastLocation != null) {
            val lastLatLong = lastLocation!!.latLong
            val distance = locationDistance.between(lastLatLong, newLatLong)
            // New point added if we're above the distance threshold
            shouldAdd = (distance > thresholdDistanceMeters)

            lastLocation?.let { lastLoc ->
                if (lastLatLong.hasAccuracy()
                    && distance <= lastLatLong.accuracy
                    && newLatLong.accuracy < lastLatLong.accuracy) {
                    // If the distance delta is below the last point accuracy,
                    // and the accuracy got better,
                    // then consider it a better replacement for the same point.
                    replaceIndex = n - 1
                } else if (!shouldAdd) {
                    // If the point is closer than the threshold distance but the speed
                    // is valid and has fallen under a 1 m/s threshold, consider adding it.
                    val newSpeedData = if (newLocation.hasSpeedBearing()) newLocation.speedBearing else null
                    val lastSpeedData = if (lastLoc.hasSpeedBearing()) lastLoc.speedBearing else null

                    var newSpeed = newSpeedData?.speedMps ?: -1f
                    var lastSpeed = lastSpeedData?.speedMps ?: -1f
                    if (newSpeedData != null
                        && newSpeedData.hasSpeedAcc()
                        && newSpeed < newSpeedData.speedAcc) {
                        newSpeed = -1f
                    }
                    if (lastSpeedData != null
                        && lastSpeedData.hasSpeedAcc()
                        && lastSpeed < lastSpeedData.speedAcc) {
                        lastSpeed = -1f
                    }
                    shouldAdd = lastSpeed > thresholdSpeedMps
                            && newSpeed >= 0
                            && newSpeed < thresholdSpeedMps
                }
            }
        }

        if (replaceIndex >= 0) {
            locationDataListBuilder.setLocations(
                replaceIndex,
                newLocation
            )
            lastLocation = newLocation
            needsWrite = true
            locationUpdater()

        } else if (shouldAdd) {
            locationDataListBuilder.addLocations(newLocation)
            lastLocation = newLocation
            needsWrite = true
            logger.d(TAG, "Add from ${Thread.currentThread()}")
            locationUpdater()
        }
    }

    private fun locationUpdater(writeNow: Boolean = false) {
        if (!needsWrite) return
        if (!writeNow && clock.elapsedRealtime() < nextWriteTS) return

        scope.launch {
            needsWrite = false
            logger.d(TAG, "Writing ${locationDataListBuilder.locationsCount} points from ${Thread.currentThread()}")
            trackEntityDao.updateLocationList(recordId, locationDataListBuilder.build())
            nextWriteTS = clock.elapsedRealtime() + WRITE_INTERVAL_MS
        }
    }

}

