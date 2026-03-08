package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.app.GlobalState
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.milepost.fragments.docentPlayback.DocentPlaybackItem
import com.alflabs.rx.IPublisher
import com.alflabs.rx.Publishers
import com.alflabs.utils.ILogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Operator @Inject constructor(
    private val opGpsRecordFactory: OpGpsRecord.Factory,
    private val opGpsPlaybackFactory: OpGpsPlayback.Factory,
    private val opDocentPlaybackFactory: OpDocentPlayback.Factory,
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

    val currentDocentTrackId: Long?
        get() = when(val op = currentOp) {
            is IOpDocentActor -> op.currentDocentTrackId
            else -> null
        }

    val currentGpsTrackId: Long?
        get() = when(val op = currentOp) {
            is IOpGpsActor -> op.currentGpsTrackId
            else -> null
        }

    val currentLocationsList: List<LocationData>?
        get() = when(val op = currentOp) {
            is IOpGpsActor -> op.currentLocationsList
            else -> null
        }

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
        val op = currentOp as OpGpsRecord
        logger.d(TAG, "stopGpsRecord id=${op.currentGpsTrackId}")
        currentOp.stop()
        currentOp = opIdle
        changeMode(OperatorMode.IDLE)
    }

    fun startGpsPlayback(gpsTrack: GpsTrackEntity) {
        if (operatorMode == OperatorMode.GPS_PLAYBACK) return
        currentOp.stop()
        logger.d(TAG, "startGpsPlayback id=${gpsTrack.id}")
        currentOp = opGpsPlaybackFactory.create(gpsTrack)
        changeMode(OperatorMode.GPS_PLAYBACK)
    }

    fun getGpsPlaybackTrack(): GpsTrackEntity? {
        return when (val op = currentOp) {
            is OpGpsPlayback -> op.gpsPlaybackTrack
            is OpDocentPlayback -> op.gpsPlaybackTrack
            else -> null
        }
    }

    fun stopGpsPlayback() {
        if (operatorMode != OperatorMode.GPS_PLAYBACK) return
        val op = currentOp as OpGpsRecord
        logger.d(TAG, "stopGpsPlayback id=${op.currentGpsTrackId}")
        currentOp.stop()
        currentOp = opIdle
        changeMode(OperatorMode.IDLE)
    }

    fun startDocentPlayback(
        docenTrack: DocentTrackEntity,
        gpsTrack: GpsTrackEntity,
        triggers: List<DocentPlaybackItem>
    ) {
        if (operatorMode == OperatorMode.DOCENT_PLAYBACK) return
        currentOp.stop()
        logger.d(TAG, "startDocentPlayback id=${docenTrack.id}")
        currentOp = opDocentPlaybackFactory.create(docenTrack, gpsTrack, triggers)
        changeMode(OperatorMode.DOCENT_PLAYBACK)
    }

    fun getDocentPlaybackTrack(): DocentTrackEntity? {
        return when (val op = currentOp) {
            is OpDocentPlayback -> op.docentPlaybackTrack
            else -> null
        }
    }

    fun getDocentPlaybackTriggers(): List<DocentPlaybackItem>? {
        return when (val op = currentOp) {
            is OpDocentPlayback -> op.triggers
            else -> null
        }
    }

    fun stopDocentPlayback() {
        if (operatorMode != OperatorMode.DOCENT_PLAYBACK) return
        val op = currentOp as OpDocentPlayback
        logger.d(TAG, "stopDocentPlayback id=${op.currentDocentTrackId}")
        currentOp.stop()
        currentOp = opIdle
        changeMode(OperatorMode.IDLE)
    }

}
