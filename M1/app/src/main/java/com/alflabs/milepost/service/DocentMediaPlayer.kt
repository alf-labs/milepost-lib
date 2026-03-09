package com.alflabs.milepost.service

import android.content.Context
import com.alflabs.milepost.app.GlobalState
import com.alflabs.milepost.op.AudioPlayCommand
import com.alflabs.milepost.op.AudioPlayingStatus
import com.alflabs.rx.IPublisher
import com.alflabs.rx.ISubscriber
import com.alflabs.rx.Publishers
import com.alflabs.rx.Schedulers
import com.alflabs.utils.ILogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DocentMediaPlayer @AssistedInject constructor(
    @Assisted val context: Context,
    private val logger: ILogger,
    private val globalState: GlobalState,
) {
    @AssistedFactory
    interface Factory {
        fun create(context: Context): DocentMediaPlayer
    }

    companion object {
        private val TAG = DocentMediaPlayer::class.java.simpleName
    }

    private val audioPlayCommandSubscriber: ISubscriber<in AudioPlayCommand?> =
        ISubscriber<AudioPlayCommand?> { _, event -> onAudioPlayCommand(event) }
    private val audioPlayingStatusPublisher: IPublisher<AudioPlayingStatus?> = Publishers.latest()

    init {
        globalState.audioPlayCommandStream.subscribe(Schedulers.sync(), audioPlayCommandSubscriber)
        globalState.audioPlayingStatusStream.publishWith(audioPlayingStatusPublisher)
    }

    fun onDestroy() {
        globalState.audioPlayingStatusStream.remove(audioPlayingStatusPublisher)
        globalState.audioPlayCommandStream.remove(audioPlayCommandSubscriber)
    }

    private fun onAudioPlayCommand(command: AudioPlayCommand?) {
        if (command == null) return
        logger.d(TAG, "@@ command: $command")
    }
}
