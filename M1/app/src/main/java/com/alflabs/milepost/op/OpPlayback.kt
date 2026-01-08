package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.app.GlobalState
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.rx.IPublisher
import com.alflabs.rx.ISubscriber
import com.alflabs.rx.Publishers
import com.alflabs.rx.Schedulers
import com.alflabs.utils.ILogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class OpPlayback @AssistedInject constructor(
    @Assisted val track: TrackEntity,
    private val logger: ILogger,
    private val globalState: GlobalState,
) : IOpActor {

    @AssistedFactory
    interface Factory {
        fun create(track: TrackEntity): OpPlayback
    }

    companion object {
        private val TAG = OpPlayback::class.java.simpleName
    }

    val playbackTrack: TrackEntity
        get() = track

    override val currentTrackId: Long
        get() = track.id

    override val currentLocationsList: List<LocationData>?
        get() = track.locationList?.locationsList

    private val locationSubscriber: ISubscriber<in LocationData?> =
        ISubscriber<LocationData?> { _, event -> onLocationUpdate(event) }
    private val mpEstimatePublisher: IPublisher<MpEstimate?> = Publishers.latest()
    private val mpEstimator = MpEstimator()
    private var lastIndex = -1
    private var lastDirection = MpDirection.UNKNOWN

    init {
        logger.d(TAG, "init: ${track.id}")
        globalState.locationStream.subscribe(Schedulers.sync(), locationSubscriber)
        globalState.mpEstimateStream.publishWith(mpEstimatePublisher)
    }

    override fun stop() {
        logger.d(TAG, "stop")
        globalState.mpEstimateStream.remove(mpEstimatePublisher)
        globalState.locationStream.remove(locationSubscriber)
    }

    private var tempNewIndex = -1
    fun onLocationUpdate(newLocation: LocationData?) {
        // Note: this call is synchronous and happens on the MpActivity Looper thread
        // as invoked by the FusedLocationProviderClient.
        if (newLocation == null) return

        val newLatLong = newLocation.latLong
        val locations: List<LocationData> = track.locationList?.locationsList ?: return
        if (locations.isEmpty()) return

// DEBUG... is examineNearby working properly?
//        var newIndex = if (lastIndex == -1) {
//            mpEstimator.findFirstNearest(locations, newLatLong)
//        } else {
//            mpEstimator.examineNearby(locations, newLatLong, lastIndex)
//        }
        var newIndex = mpEstimator.findFirstNearest(locations, newLatLong)
        logger.d(TAG, "onLocationUpdate: $lastIndex --> $newIndex")

        // Reject an invalid index
        if (newIndex < 0 || newIndex >= locations.size) return

// DEBUG to force movement for testing
        if (lastIndex >= 0) {
            tempNewIndex = (tempNewIndex + 1) % 20
            val offset = if (tempNewIndex <= 10) tempNewIndex else 20 - tempNewIndex

            newIndex = (newIndex + offset) % locations.size
            logger.d(TAG, "DEBUG : $lastIndex --> $newIndex ($tempNewIndex)")
        }

        val direction = when {
            lastIndex < 0 -> MpDirection.UNKNOWN
            lastIndex < newIndex -> MpDirection.TO_END
            lastIndex > newIndex -> MpDirection.TO_START
            else -> MpDirection.UNKNOWN
        }

        val mp = mpEstimator.interpolateMp(locations, newIndex, newLatLong)

        lastIndex = newIndex
        if (direction != MpDirection.UNKNOWN) lastDirection = direction

        mpEstimatePublisher.publish(MpEstimate(
            mp = mp,
            locIndex = newIndex,
            latLong = locations[newIndex].latLong,
            direction = lastDirection
        ))
    }

}

