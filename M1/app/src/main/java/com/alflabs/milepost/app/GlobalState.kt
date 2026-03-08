package com.alflabs.milepost.app

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.op.MpEstimate
import com.alflabs.milepost.op.Operator
import com.alflabs.milepost.op.OperatorMode
import com.alflabs.milepost.op.TriggerEvent
import com.alflabs.milepost.service.LocationService
import com.alflabs.milepost.service.ServiceMode
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
    val mpEstimateStream: IStream<MpEstimate?> = Streams.stream<MpEstimate?>().on(Schedulers.sync())
    val triggerEventStream: IStream<TriggerEvent?> = Streams.stream<TriggerEvent?>().on(Schedulers.sync())
    val serviceModeStream: IStream<ServiceMode?> = Streams.stream<ServiceMode?>().on(Schedulers.sync())
    val operatorModeStream: IStream<OperatorMode?> = Streams.stream<OperatorMode?>().on(Schedulers.sync())

    val serviceMode: ServiceMode get() = LocationService.serviceMode
    val operatorMode: OperatorMode get() = Operator.operatorMode

    init {
        logger.d(TAG, "GlobalState initialized: $this")
    }
}
