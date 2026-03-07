package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.app.GlobalState
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.rx.IPublisher
import com.alflabs.rx.Publishers
import com.alflabs.utils.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Operator @Inject constructor(
    private val opGpsRecordFactory: OpGpsRecord.Factory,
    private val opGpsPlaybackFactory: OpGpsPlayback.Factory,
    private val logger: ILogger,
    private val globalState: GlobalState,
) {
    companion object {
        private val TAG = Operator::class.java.simpleName

        var operatorMode: OperatorMode = OperatorMode.IDLE
            private set
    }

    private val operatorModePublisher: IPublisher<OperatorMode?> = Publishers.latest()
    private val opIdle = OpIdle()
    private var currentOp: IOpActor = opIdle

    val currentGpsTrackId: Long?
        get() = currentOp.currentGpsTrackId

    val currentLocationsList: List<LocationData>?
        get() = currentOp.currentLocationsList


    init {
        globalState.operatorModeStream.publishWith(operatorModePublisher)
    }

    private fun changeMode(newMode: OperatorMode) {
        operatorMode = newMode
        operatorModePublisher.publish(operatorMode)
    }

    fun startSensors() {
        if (operatorMode == OperatorMode.SENSORS) return
        currentOp.stop()
        logger.d(TAG, "startSensors")
        currentOp = opIdle
        changeMode(OperatorMode.SENSORS)
    }

    fun stopSensors() {
        if (operatorMode != OperatorMode.SENSORS) return
        logger.d(TAG, "stopSensors")
        currentOp.stop()
        currentOp = opIdle
        changeMode(OperatorMode.IDLE)
    }

    suspend fun startGpsRecord(gpsTrack: GpsTrackEntity) {
        if (operatorMode == OperatorMode.GPS_RECORD) return
        currentOp.stop()
        logger.d(TAG, "startGpsRecord id=${gpsTrack.id}")
        currentOp = opGpsRecordFactory.create(gpsTrack).initAsync()
        changeMode(OperatorMode.GPS_RECORD)
    }

    fun stopGpsRecord() {
        if (operatorMode != OperatorMode.GPS_RECORD) return
        logger.d(TAG, "stopGpsRecord id=${currentOp.currentGpsTrackId}")
        currentOp.stop()
        currentOp = opIdle
        changeMode(OperatorMode.IDLE)
    }

    fun startGpsPlayback(track: GpsTrackEntity) {
        if (operatorMode == OperatorMode.GPS_PLAYBACK) return
        currentOp.stop()
        logger.d(TAG, "startGpsPlayback id=${track.id}")
        currentOp = opGpsPlaybackFactory.create(track)
        changeMode(OperatorMode.GPS_PLAYBACK)
    }

    fun getGpsPlaybackTrack(): GpsTrackEntity? {
        return if (currentOp is OpGpsPlayback) {
            (currentOp as OpGpsPlayback).gpsPlaybackTrack
        } else {
            null
        }
    }

    fun stopGpsPlayback() {
        if (operatorMode != OperatorMode.GPS_PLAYBACK) return
        logger.d(TAG, "stopGpsPlayback id=${currentOp.currentGpsTrackId}")
        currentOp.stop()
        currentOp = opIdle
        changeMode(OperatorMode.IDLE)
    }

}
