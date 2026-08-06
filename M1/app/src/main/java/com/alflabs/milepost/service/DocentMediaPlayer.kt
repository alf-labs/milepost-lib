package com.alflabs.milepost.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.net.toUri
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
    private val mediaPlayerWrapperFactory: MediaPlayerWrapper.Factory,
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

    private val mediaPlayerWrapper = mediaPlayerWrapperFactory.create(context)
    private var current: AudioInfo? = null
    private var pending: AudioInfo? = null

    init {
        globalState.audioPlayCommandStream.subscribe(Schedulers.sync(), audioPlayCommandSubscriber)
        globalState.audioPlayingStatusStream.publishWith(audioPlayingStatusPublisher)
    }

    fun onDestroy() {
        stopPlaying()
        globalState.audioPlayingStatusStream.remove(audioPlayingStatusPublisher)
        globalState.audioPlayCommandStream.remove(audioPlayCommandSubscriber)
    }

    private fun onAudioPlayCommand(command: AudioPlayCommand?) {
        if (command == null) return
        logger.d(TAG, "@@ command: $command")

        when(command.command) {
            AudioPlayCommand.Command.QueueAdd -> {
                val audio = AudioInfo(
                    triggerId = command.triggerId,
                    persistableUri = command.persistableUri)
                if (current == null || (command.startIfPaused && (current?.isPaused ?: false) )) {
                    startPlaying(audio)
                } else {
                    pending = audio
                    sendStatus(AudioPlayingStatus.Type.Pending, pending)
                }
            }
            AudioPlayCommand.Command.Force -> {
                val audio = AudioInfo(
                    triggerId = command.triggerId,
                    persistableUri = command.persistableUri)
                startPlaying(audio)
            }
            AudioPlayCommand.Command.QueueRemove -> {
                pending?.let {
                    if (it.triggerId == command.triggerId) {
                        pending = null
                        sendStatus(AudioPlayingStatus.Type.Pending, pending)
                    }
                }
            }

            AudioPlayCommand.Command.Status -> {
                sendStatus(AudioPlayingStatus.Type.Current, current)
                sendStatus(AudioPlayingStatus.Type.Pending, pending)
            }

            AudioPlayCommand.Command.Pause -> {
                current?.let { info ->
                    if (info.triggerId == command.triggerId) {
                        mediaPlayerWrapper.pausePlayer()
                        info.isPaused = true
                        sendStatus(AudioPlayingStatus.Type.Current, info)
                    }
                }
            }

            AudioPlayCommand.Command.Unpause -> {
                current?.let { info ->
                    if (info.triggerId == command.triggerId) {
                        mediaPlayerWrapper.resumePlayer()
                        info.isPaused = false
                        // updateProgressRunnable will send a status immediately
                        progressHandler.post(updateProgressRunnable)
                    }
                }
            }
        }
    }

    private fun sendStatus(type: AudioPlayingStatus.Type, audio: AudioInfo?) {
        val status = AudioPlayingStatus(
            type = type,
            isPlaying = audio?.isPlaying ?: false,
            isPaused = audio?.isPaused ?: false,
            triggerId = audio?.triggerId ?: -1L,
            currentMs = audio?.currentMs ?: 0,
            durationMs = audio?.durationMs ?: 0,
        )
        audioPlayingStatusPublisher.publish(status)
    }

    private fun startPlaying(audio: AudioInfo) {
        stopPlaying()
        try {
            val uri = audio.persistableUri.toUri()
            mediaPlayerWrapper.startPlaying(uri,
                completionCallback = ::onCompletion,
                stopCallback = ::stopPlaying)
            logger.d(TAG, "@@ startPlaying: ${audio.triggerId}, $uri")
            current = audio.copy(isPlaying = true)
            sendStatus(AudioPlayingStatus.Type.Current, current)
            progressHandler.post(updateProgressRunnable)
        } catch (e: Exception) {
            logger.d(TAG, "Error playing audio", e)
            Toast.makeText(context, "Error playing audio file", Toast.LENGTH_LONG).show()
            stopPlaying()
        }
    }

    private fun onCompletion() {
        stopPlaying()
        pending?.let {
            pending = null
            sendStatus(AudioPlayingStatus.Type.Pending, pending)
            logger.d(TAG, "@@ start Pending: ${it.triggerId}, ${it.persistableUri}")
            startPlaying(it)
        }
    }

    private fun stopPlaying() {
        progressHandler.removeCallbacks(updateProgressRunnable)
        mediaPlayerWrapper.stopPlaying()
        current?.let {
            current = it.copy(isPlaying = false)
            sendStatus(AudioPlayingStatus.Type.Current, current)
            logger.d(TAG, "@@ stopPlaying: ${it.triggerId}, ${it.persistableUri}")
            current = null
        }
    }

    private val progressHandler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayerWrapper.player()?.let { player ->
                if (player.isPlaying) {
                    current?.let { info ->
                        info.currentMs = player.currentPosition
                        info.durationMs = player.duration
                        sendStatus(AudioPlayingStatus.Type.Current, info)
                    }
                    progressHandler.postDelayed(this, 1000)
                }
            }
        }
    }
}

private data class AudioInfo(
    val triggerId: Long,
    val persistableUri: String,
    var isPlaying: Boolean = false,
    var isPaused: Boolean = false,
    var currentMs: Int = 0,
    var durationMs: Int = 0,
)
