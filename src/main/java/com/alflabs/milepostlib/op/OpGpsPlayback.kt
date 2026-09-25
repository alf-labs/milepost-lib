package com.alflabs.milepostlib.op

import com.alflabs.milepostlib.app.AppSettings
import com.alflabs.milepostlib.location.LocationData
import com.alflabs.milepostlib.app.GlobalState
import com.alflabs.milepostlib.location.LocationFlags
import com.alflabs.milepostlib.location.hasFlag
import com.alflabs.milepostlib.db.GpsTrackEntity
import com.alflabs.rx.IPublisher
import com.alflabs.rx.ISubscriber
import com.alflabs.rx.Publishers
import com.alflabs.rx.Schedulers
import com.alflabs.utils.ILogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.math.max
import kotlin.math.min

/**
 * The GPS Playback OpActor subscribes to the global location event stream and
 * computes the MP value most closely matching the current location updates.
 * These are then sent to a global MP event stream.
 */
class OpGpsPlayback @AssistedInject constructor(
    @Assisted val gpsTrack: GpsTrackEntity,
    private val logger: ILogger,
    private val globalState: GlobalState,
    private val appSettings: AppSettings,
) : IOpGpsActor {

    @AssistedFactory
    interface Factory {
        fun create(gpsTrack: GpsTrackEntity): OpGpsPlayback
    }

    companion object {
        private val TAG = OpGpsPlayback::class.java.simpleName
    }

    val gpsPlaybackTrack: GpsTrackEntity
        get() = gpsTrack

    override val currentGpsTrackId: Long
        get() = gpsTrack.id

    override val currentLocationsList: List<LocationData>?
        get() = filteredLocations

    private val locationSubscriber: ISubscriber<in LocationData?> =
        ISubscriber<LocationData?> { _, event -> onLocationUpdate(event) }
    private val mpEstimatePublisher: IPublisher<MpEstimate?> = Publishers.latest()
    private val mpEstimator = MpEstimator()
    private val filteredLocations = mutableListOf<LocationData>()
    private var lastIndex = -1
    private var lastDirection = MpDirection.UNKNOWN
    private var simulateGps = -1
    private var simulateGpsFactor = 1.0f

    init {
        logger.d(TAG, "init: ${gpsTrack.id}")
        // Only keep location data points that are not marked "ignore"
        gpsTrack.locationList?.locationsList?.let { list ->
            filteredLocations.clear()
            filteredLocations.addAll(list.filter { loc ->
                !loc.flags.hasFlag(LocationFlags.IGNORE)
            })
        }
        globalState.locationStream.subscribe(Schedulers.sync(), locationSubscriber)
        globalState.mpEstimateStream.publishWith(mpEstimatePublisher)
    }

    suspend fun initAsync() {
        simulateGpsFactor = appSettings.simulateGpsFactor()
        simulateGps = when(appSettings.simulateGps()) {
            "from_start" -> 1
            "from_end" -> 2
            else -> -1
        }
    }

    override fun stop() {
        logger.d(TAG, "stop")
        globalState.mpEstimateStream.remove(mpEstimatePublisher)
        globalState.locationStream.remove(locationSubscriber)
    }

    private var simulIndex = -1
    private var simulInc = 0
    fun onLocationUpdate(newLocation: LocationData?) {
        // Note: this call is synchronous and happens on the MpActivity Looper thread
        // as invoked by the FusedLocationProviderClient.
        if (newLocation == null) return

        if (filteredLocations.isEmpty()) return

        val newLatLong = when {
            // Simulate from start or end
            simulateGps > 0 -> {
                val sz1 = filteredLocations.lastIndex
                if (simulInc == 0) {
                    if (simulateGps == 1) {
                        simulIndex = 0
                        simulInc = 1
                    } else {
                        simulIndex = sz1
                        simulInc = -1
                    }
                } else {
                    simulIndex = min(max(0, simulIndex + simulInc), sz1)
                    if (simulIndex == 0 || simulIndex >= sz1) {
                        simulInc = -simulInc
                    }
                }
                filteredLocations[simulIndex].latLong
            }
            // Use real coordinate
            else -> {
                newLocation.latLong
            }
        }


// DEBUG... is examineNearby working properly?
//        var newIndex = if (lastIndex == -1) {
//            mpEstimator.findFirstNearest(locations, newLatLong)
//        } else {
//            mpEstimator.examineNearby(locations, newLatLong, lastIndex)
//        }
        val newIndex = mpEstimator.findFirstNearest(filteredLocations, newLatLong)
        logger.d(TAG, "onLocationUpdate: $lastIndex --> $newIndex")

        // Reject an invalid index
        if (newIndex < 0 || newIndex >= filteredLocations.size) return

        val direction = when {
            lastIndex < 0 -> MpDirection.UNKNOWN
            lastIndex < newIndex -> MpDirection.TO_END
            lastIndex > newIndex -> MpDirection.TO_START
            else -> MpDirection.UNKNOWN
        }

        val mp = mpEstimator.interpolateMp(filteredLocations, newIndex, newLatLong)

        lastIndex = newIndex
        if (direction != MpDirection.UNKNOWN) lastDirection = direction

        mpEstimatePublisher.publish(MpEstimate(
            mp = mp,
            locIndex = newIndex,
            latLong = filteredLocations[newIndex].latLong,
            direction = lastDirection
        ))
    }

}

