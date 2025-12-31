package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.main.GlobalState
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.rx.ISubscriber
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

    override val currentTrackId: Long
        get() = track.id

    private val locationSubscriber: ISubscriber<in LocationData?> =
        ISubscriber<LocationData?> { _, event -> onLocationUpdate(event) }


    init {
        logger.d(TAG, "init: ${track.id}")
    }

    override fun stop() {
        logger.d(TAG, "stop")
    }

    fun onLocationUpdate(value: LocationData?) {
        // Note: this call is synchronous and happens on the MainActivity Looper thread
        // as invoked by the FusedLocationProviderClient.
        logger.d(TAG, "onLocationChanged: ${value?.latLong?.timestampMillis}")
        if (value == null) return
    }
}
