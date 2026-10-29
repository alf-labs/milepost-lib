package com.alflabs.milepostlib.op

import com.alflabs.milepostlib.location.LocationData
import com.alflabs.milepostlib.app.GlobalState
import com.alflabs.milepostlib.db.DocentTrackEntity
import com.alflabs.milepostlib.db.GpsTrackEntity
import com.alflabs.milepostlib.fragments.docentPlayback.DocentPlaybackItem
import com.alflabs.milepostlib.trigger.PlaybackTriggerDirection
import com.alflabs.milepostlib.trigger.PlaybackTrigger
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
    private val audioPlayCommandPublisher: IPublisher<AudioPlayCommand?> = Publishers.latest()

    private val activeTriggers = mutableListOf<DocentPlaybackItem>()
    private val startIsLower: Boolean
    private var isColdStart = true

    init {
        logger.d(TAG, "init: ${docentTrack.id}")
        opGpsPlayback = opGpsPlaybackFactory.create(gpsTrack)
        globalState.mpEstimateStream.subscribe(Schedulers.sync(), mpEstimateSubscriber)
        globalState.audioPlayCommandStream.publishWith(audioPlayCommandPublisher)

        // Figure MpDirection (Start..End) vs PlaybackTriggerDirection (toLower/toHigher MP).
        // If unknown, we default to true.
        startIsLower = currentLocationsList?.let { locations ->
            if (locations.isNotEmpty()) {
                val startMp = locations.first().mp
                val endMp = locations.last().mp
                startMp <= endMp
            } else {
                true
            }
        } ?: true
    }

    suspend fun initAsync() {
        opGpsPlayback.initAsync()
    }

    override fun stop() {
        logger.d(TAG, "stop")
        opGpsPlayback.stop()
        globalState.audioPlayCommandStream.remove(audioPlayCommandPublisher)
        globalState.mpEstimateStream.remove(mpEstimateSubscriber)
    }

    private fun onMpEstimateUpdate(estimate: MpEstimate?) {
        if (estimate == null) return

        val mp = estimate.mp    // for debugging purposes

        // Split active filters based on whether they are still active.
        val (active, inactive) = activeTriggers.partition {
            matchEstimate(it.playbackTrigger, estimate)
        }

        // Figure new triggers to activate, ignore those already active.
        val newTriggers = triggers.filter {
            !active.contains(it) && matchEstimate(it.playbackTrigger, estimate)
        }

        // Remove any newTriggers from inactive.
        val newInactive = inactive.filter {
            !newTriggers.contains(it)
        }

        // Merge the active and the new triggers into activeTriggers.
        activeTriggers.clear()
        activeTriggers.addAll(active)
        activeTriggers.addAll(newTriggers)

        // Publish QueueRemove commands for all newInactive triggers.
        newInactive.forEach {
            logger.d(TAG, "@@ QueueRemove: MP $mp --> Trigger # ${it.triggerId}")
            audioPlayCommandPublisher.publish(
                AudioPlayCommand(
                    command = AudioPlayCommand.Command.QueueRemove,
                    triggerId = it.triggerId,
                    persistableUri = it.persistableUri,
                ))
        }

        // Publish QueueAdd commands for all newTriggers.
        newTriggers.forEach {
            logger.d(TAG, "@@ QueueAdd: MP $mp --> Trigger # ${it.triggerId}")
            audioPlayCommandPublisher.publish(
                AudioPlayCommand(
                    command = AudioPlayCommand.Command.QueueAdd,
                    triggerId = it.triggerId,
                    persistableUri = it.persistableUri,
                    startIfPaused = true,
                ))
        }

        // Disable cold start once at least one QueueAdd has been published.
        if (isColdStart) {
            isColdStart = newTriggers.isEmpty()
            logger.d(TAG, "@@ Cold Start: $isColdStart")
        }
    }

    private fun convertDirection(mpDirection: MpDirection): PlaybackTriggerDirection {
        return when (mpDirection) {
            MpDirection.UNKNOWN -> PlaybackTriggerDirection.DIRECTION_UNSPECIFIED
            MpDirection.TO_START -> {
                if (startIsLower) PlaybackTriggerDirection.DIRECTION_TO_LOWER_MP
                else PlaybackTriggerDirection.DIRECTION_TO_HIGHER_MP
            }
            MpDirection.TO_END -> {
                if (startIsLower) PlaybackTriggerDirection.DIRECTION_TO_HIGHER_MP
                else PlaybackTriggerDirection.DIRECTION_TO_LOWER_MP
            }
        }
    }

    private fun matchEstimate(
        trigger: PlaybackTrigger?,
        estimate: MpEstimate,
    ): Boolean {
        if (trigger == null) return false
        if (isColdStart && !trigger.includeInColdStart) return false

        val directionMatch =
            (trigger.mpDirection == PlaybackTriggerDirection.DIRECTION_UNSPECIFIED)
            || trigger.mpDirection == convertDirection(estimate.direction)

        val rangeMatch =
            (estimate.mp >= trigger.mpMin) && (estimate.mp <= trigger.mpMax)

        return directionMatch && rangeMatch
    }
}

