package com.alflabs.milepostlib.service

import android.location.Location
import com.alflabs.milepostlib.app.AppSettings
import com.alflabs.milepostlib.db.GpsTrackEntity
import com.alflabs.milepostlib.location.LocationData
import com.alflabs.milepostlib.location.LocationDataList
import com.alflabs.utils.ILogger
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@Singleton
class SimulationGpsProvider @Inject constructor(
    private val logger: ILogger,
    private val appSettings: AppSettings,
) {
    companion object {
        private val TAG = SimulationGpsProvider::class.java.simpleName
    }

    private var gpsTrack: GpsTrackEntity? = null
    private var simulateGps = -1
    private var simulateGpsFactor = 1.0f

    suspend fun start(locationCallback: LocationCallback): Boolean {
        simulateGpsFactor = appSettings.simulateGpsFactor()
        simulateGps = when(appSettings.simulateGps()) {
            "simulate_from_start" -> 1
            "simulate_from_end" -> -1
            else -> 0
        }
        process(locationCallback)

        return simulateGps != 0
    }

    private suspend fun process(
        locationCallback: LocationCallback
    ) = withContext(Dispatchers.IO) {
        var simulateDirection = simulateGps
        while (simulateGps != 0) {
            gpsTrack?.let { track ->
                val locList = track.locationList
                if (locList == null || locList.locationsList.isEmpty()) {
                    logger.d(TAG, "@@ Simulate GPS warning: Empty GPS track")
                    return@withContext
                }
                runOnGpsTrack(locList, simulateDirection, simulateGpsFactor, locationCallback)
                simulateDirection = -simulateDirection
            }
            delay(2.seconds)
        }
    }

    suspend fun runOnGpsTrack(
        locList: LocationDataList,
        direction: Int,
        speedFactor: Float,
        locationCallback: LocationCallback
    ) {
        // simulateDirection: 1 = from start; -1 = from end.
        logger.d(TAG, "@@ Simulate GPS: direction $direction")

        replayGpsTrack(locList.locationsList, direction, speedFactor).collect { location ->
            logger.d(TAG, "@@ Simulate GPS: ${location.latitude} x ${location.longitude} @ Time: ${location.time}")
            locationCallback.onLocationResult(LocationResult.create(listOf(location)))
        }
    }

    fun onDestroy() {
        logger.d(TAG, "@@ Simulate GPS closing...")
        simulateGps = -1
        gpsTrack = null
    }

    fun setGpsTrack(gpsTrack: GpsTrackEntity?) {
        logger.d(TAG, "@@ Simulate GPS: GPS Track set to ${gpsTrack?.trackLabel}")
        this.gpsTrack = gpsTrack
    }

    /**
     * Replays a list of [LocationData] points in real-time,
     * emitting an [android.location.Location]
     * at least every [maxIntervalMs] (default 5 seconds).
     *
     * [direction] is the loop offset and MUST be either 1 (start to end) or -1 (end to start).
     *
     * [speedFactor] is a multiplier to speed up the delay (e.g. make them shorter).
     * E.g. 1.0 is normal speed, 2.0 is twice faster (shorter delays),
     */
    fun replayGpsTrack(
        locations: List<LocationData>,
        direction: Int,
        speedFactor: Float,
        maxIntervalMs: Long = 5000L
    ): Flow<Location> = flow {
        if (locations.isEmpty()) return@flow

        val delayMultiplier = 1.0f / max(1.0f, speedFactor)

        // Emit the initial recorded point
        val firstLocation = locations.first()
        emit(firstLocation.toAndroidLocation())

        // Iterate through consecutive pairs of points in either direction
        val locSize1 = locations.size - 1
        val range = if (direction == 1) {
             0 until locSize1           // Note: [0..locSize1[ as until excludes top boundary
        } else {
            locSize1 downTo 1           // Note: [locSize1..1] included.
        }
        for (i in range) {
            val pointA = locations[i]
            val pointB = locations[i + direction]

            // Note: Time A/B are reversed when starting from the end point.
            val timeA = min(pointA.latLong.timestampMillis, pointB.latLong.timestampMillis)
            val timeB = max(pointA.latLong.timestampMillis, pointB.latLong.timestampMillis)
            val duration = max(timeB - timeA, 1)    // protect against unexpected time A==B

            var previousTime = timeA
            var currentTime = timeA + maxIntervalMs

            // Emit interpolated points if time gap > maxIntervalMs
            while (currentTime < timeB) {
                val delayMs = currentTime - previousTime
                if (delayMs > 0) {
                    delay((delayMs * delayMultiplier).toLong().milliseconds)
                }

                val fraction = (currentTime - timeA).toDouble() / duration
                val interpolatedLocation = interpolateLocation(pointA, pointB, fraction, currentTime)
                emit(interpolatedLocation)

                previousTime = currentTime
                currentTime += maxIntervalMs
            }

            // Delay remaining duration and emit the actual recorded point B
            val finalDelay = maxOf(0L, timeB - previousTime)
            if (finalDelay > 0) {
                delay((finalDelay * delayMultiplier).toLong().milliseconds)
            }

            emit(pointB.toAndroidLocation())
        }
    }

    /**
     * Linearly interpolates coordinates, timestamp, and speed between two protobuf LocationData points.
     */
    private fun interpolateLocation(
        pointA: LocationData,
        pointB: LocationData,
        fraction: Double,
        timestampMillis: Long
    ): Location {
        val latA = pointA.latLong.latitude
        val latB = pointB.latLong.latitude
        val lngA = pointA.latLong.longitude
        val lngB = pointB.latLong.longitude

        val interpolatedLat = latA + fraction * (latB - latA)
        val interpolatedLng = lngA + fraction * (lngB - lngA)

        val interpolatedSpeed = if (pointA.hasSpeed() && pointB.hasSpeed()) {
            val speedA = pointA.speed.speedMps
            val speedB = pointB.speed.speedMps
            speedA + fraction.toFloat() * (speedB - speedA)
        } else if (pointA.hasSpeed()) {
            pointA.speed.speedMps
        } else null

        return createLocation(
            latitude = interpolatedLat,
            longitude = interpolatedLng,
            timestampMillis = timestampMillis,
            speedMps = interpolatedSpeed,
        )
    }

    /**
     * Converts a Protobuf [LocationData] to an [android.location.Location].
     */
    private fun LocationData.toAndroidLocation(): Location {
        val speed = if (hasSpeed()) speed.speedMps else null
        return createLocation(
            latitude = latLong.latitude,
            longitude = latLong.longitude,
            timestampMillis = latLong.timestampMillis,
            speedMps = speed,
        )
    }

    /**
     * Factory helper for constructing an [android.location.Location] object.
     */
    private fun createLocation(
        latitude: Double,
        longitude: Double,
        timestampMillis: Long,
        speedMps: Float?,
    ): Location = Location("simulation").apply {
        this.latitude = latitude
        this.longitude = longitude
        this.time = timestampMillis
        speedMps?.let { this.speed = it }
    }
}

