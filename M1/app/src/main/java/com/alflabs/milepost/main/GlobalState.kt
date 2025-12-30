package com.alflabs.milepost.main

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.location.LocationDataList
import com.alflabs.rx.IPublisher
import com.alflabs.rx.IStream
import com.alflabs.rx.Publishers
import com.alflabs.rx.Streams
import com.alflabs.utils.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalState @Inject constructor(
    val logger: ILogger
) {
    companion object {
        private val TAG = GlobalState::class.java.simpleName
    }

    val locationStream: IStream<LocationData?> = Streams.stream()

    var serviceMode: ServiceMode = ServiceMode.STOPPED

    // --- TBD deprecate --
    val modeStream: IStream<GlobalMode?> = Streams.stream()
    val mode: GlobalMode get() = _mode ?: GlobalMode.STOPPED

    var currentTrackId: Long? = null
        private set
    var currentLocationList: LocationDataList.Builder? = null
        private set

    private var _mode: GlobalMode? = null
    private val modePublisher: IPublisher<GlobalMode?> = Publishers.latest()
    // --- TBD deprecate --


    init {
        logger.d(TAG, "GlobalState initialized: $this")
        modeStream.publishWith(modePublisher)
    }

    @Deprecated("TBD")
    fun updateMode(
        newMode: GlobalMode,
        trackId: Long? = null,
        locationDataListBuilder: LocationDataList.Builder? = null
    ) {
        _mode = newMode
        currentTrackId = trackId
        currentLocationList = locationDataListBuilder
        modePublisher.publish(newMode)
    }
}
