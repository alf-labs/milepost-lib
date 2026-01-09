package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.app.GlobalState
import com.alflabs.milepost.tracks.GpsTrackEntity
import com.alflabs.rx.IPublisher
import com.alflabs.rx.Publishers
import com.alflabs.utils.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Operator @Inject constructor(
    private val opRecordFactory: OpRecord.Factory,
    private val opPlaybackFactory: OpPlayback.Factory,
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

    val currentTrackId: Long?
        get() = currentOp.currentTrackId

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

    suspend fun startRecord(track: GpsTrackEntity) {
        if (operatorMode == OperatorMode.RECORD) return
        currentOp.stop()
        logger.d(TAG, "startRecord id=${track.id}")
        currentOp = opRecordFactory.create(track).initAsync()
        changeMode(OperatorMode.RECORD)
    }

    fun stopRecord() {
        if (operatorMode != OperatorMode.RECORD) return
        logger.d(TAG, "stopRecord id=${currentOp.currentTrackId}")
        currentOp.stop()
        currentOp = opIdle
        changeMode(OperatorMode.IDLE)
    }

    fun startPlayback(track: GpsTrackEntity) {
        if (operatorMode == OperatorMode.PLAYBACK) return
        currentOp.stop()
        logger.d(TAG, "startPlayback id=${track.id}")
        currentOp = opPlaybackFactory.create(track)
        changeMode(OperatorMode.PLAYBACK)
    }

    fun getPlaybackTrack(): GpsTrackEntity? {
        return if (currentOp is OpPlayback) {
            (currentOp as OpPlayback).playbackTrack
        } else {
            null
        }
    }

    fun stopPlayback() {
        if (operatorMode != OperatorMode.PLAYBACK) return
        logger.d(TAG, "stopPlayback id=${currentOp.currentTrackId}")
        currentOp.stop()
        currentOp = opIdle
        changeMode(OperatorMode.IDLE)
    }

}
