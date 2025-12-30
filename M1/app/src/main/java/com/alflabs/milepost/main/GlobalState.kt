package com.alflabs.milepost.main

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.location.LocationDataList
import com.alflabs.milepost.location.LocationService
import com.alflabs.rx.IStream
import com.alflabs.rx.Schedulers
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

    val locationStream: IStream<LocationData?> = Streams.stream<LocationData?>().on(Schedulers.sync())
    val serviceModeStream: IStream<ServiceMode?> = Streams.stream<ServiceMode?>().on(Schedulers.sync())
    val operatorModeStream: IStream<OperatorMode?> = Streams.stream<OperatorMode?>().on(Schedulers.sync())

    val serviceMode: ServiceMode get() = LocationService.serviceMode
    val operatorMode: OperatorMode get() = Operator.operatorMode

    var currentTrackId: Long? = null
        private set
    var currentLocationList: LocationDataList.Builder? = null
        private set


    init {
        logger.d(TAG, "GlobalState initialized: $this")
    }

    @Deprecated("TBD")
    fun updateMode(
        newMode: GlobalMode,
        trackId: Long? = null,
        locationDataListBuilder: LocationDataList.Builder? = null
    ) {
//        _mode = newMode
        currentTrackId = trackId
        currentLocationList = locationDataListBuilder
//        modePublisher.publish(newMode)
    }
}
