package com.alflabs.milepost.op

import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.app.GlobalState
import com.alflabs.milepost.db.DocentTrackEntity
import com.alflabs.milepost.db.GpsTrackEntity
import com.alflabs.milepost.fragments.docentPlayback.DocentPlaybackItem
import com.alflabs.rx.IPublisher
import com.alflabs.rx.ISubscriber
import com.alflabs.rx.Publishers
import com.alflabs.rx.Schedulers
import com.alflabs.utils.ILogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * The Docent Playback OpActor subscribes to the global MP event stream and
 * computes the current and next triggers.
 * These are then sent to a global trigger event stream.
 *
 * The class is a bit of a misnomer as it does not actually play audio files.
 * That action should only be done from the context of an activity/fragment or a foreground service.
 */
class OpDocentPlayback @AssistedInject constructor(
    @Assisted val docentTrack: DocentTrackEntity,
    @Assisted val gpsTrack: GpsTrackEntity,
    @Assisted val triggers: List<DocentPlaybackItem>,
    private val logger: ILogger,
    private val globalState: GlobalState,
    opGpsPlaybackFactory: OpGpsPlayback.Factory,
) : IOpDocentActor {

    @AssistedFactory
    interface Factory {
        fun create(
            docentTrack: DocentTrackEntity,
            gpsTrack: GpsTrackEntity,
            triggers: List<DocentPlaybackItem>,
        ): OpDocentPlayback
    }

    companion object {
        private val TAG = OpDocentPlayback::class.java.simpleName
    }

    private val opGpsPlayback: OpGpsPlayback

    val docentPlaybackTrack: DocentTrackEntity
        get() = docentTrack

    override val currentDocentTrackId: Long
        get() = docentTrack.id

    val gpsPlaybackTrack: GpsTrackEntity
        get() = gpsTrack

    override val currentGpsTrackId: Long?
        get() = opGpsPlayback.currentGpsTrackId

    override val currentLocationsList: List<LocationData>?
        get() = opGpsPlayback.currentLocationsList

    private val mpEstimateSubscriber: ISubscriber<in MpEstimate?> =
        ISubscriber<MpEstimate?> { _, event -> onMpEstimateUpdate(event) }
    private val triggerEventPublisher: IPublisher<TriggerEvent?> = Publishers.latest()

    init {
        logger.d(TAG, "init: ${docentTrack.id}")
        opGpsPlayback = opGpsPlaybackFactory.create(gpsTrack)
        globalState.mpEstimateStream.subscribe(Schedulers.sync(), mpEstimateSubscriber)
        globalState.triggerEventStream.publishWith(triggerEventPublisher)
    }

    override fun stop() {
        logger.d(TAG, "stop")
        opGpsPlayback.stop()
        globalState.triggerEventStream.remove(triggerEventPublisher)
        globalState.mpEstimateStream.remove(mpEstimateSubscriber)
    }

    fun onMpEstimateUpdate(mp: MpEstimate?) {
        if (mp == null) return
    }
}

