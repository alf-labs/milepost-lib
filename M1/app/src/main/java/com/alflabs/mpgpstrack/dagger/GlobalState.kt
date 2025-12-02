package com.alflabs.mpgpstrack.dagger

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
    private val TAG = GlobalState::class.java.simpleName

    val locationStream: IStream<LocationData?> = Streams.stream()
    val statusStream: IStream<StatusData?> = Streams.stream()
    val status: StatusData get() = _status ?: StatusData.STOPPED

    private var _status: StatusData? = null
    private val statusPublisher: IPublisher<StatusData?> = Publishers.latest()


    fun updateStatus(status: StatusData) {
        if (_status == null) {
            logger.d(TAG, "GlobalState initialized: $this")
            statusStream.publishWith(statusPublisher)
        }
        _status = status
        statusPublisher.publish(status)
    }
}
