package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.main.GlobalState
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.rx.ISubscriber
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
    private val mpEstimator = MpEstimator()
    private var lastIndex = -1
    private var lastDirection = 0

    init {
        logger.d(TAG, "init: ${track.id}")
        globalState.locationStream.subscribe(Schedulers.sync(), locationSubscriber)
    }

    override fun stop() {
        logger.d(TAG, "stop")
        globalState.locationStream.remove(locationSubscriber)
    }

    fun onLocationUpdate(newLocation: LocationData?) {
        // Note: this call is synchronous and happens on the MainActivity Looper thread
        // as invoked by the FusedLocationProviderClient.
        logger.d(TAG, "onLocationChanged: ${newLocation?.latLong?.timestampMillis}")
        if (newLocation == null) return

        val newLatLong = newLocation.latLong
        val locations: List<LocationData> = track.locationList?.locationsList ?: return
        if (locations.isEmpty()) return

        val newIndex = if (lastIndex == -1) {
            mpEstimator.findFirstNearest(locations, newLatLong)
        } else {
            mpEstimator.examineNearby(locations, newLatLong, lastIndex)
        }

        // Reject an invalid index
        if (newIndex < 0 || newIndex >= locations.size) return

        val direction = if (lastIndex < newIndex) -1 else if (lastIndex > newIndex) 1 else 0

        val mp = mpEstimator.interpolateMp(locations, newIndex, newLatLong)

        lastIndex = newIndex
        if (direction != 0) lastDirection = direction

        // TBD publish (mp, direction).
    }
}

