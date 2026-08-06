package com.alflabs.milepost.service

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.DynamicsProcessing.Config
import android.media.audiofx.DynamicsProcessing.Limiter
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.widget.Toast
import com.alflabs.milepost.app.AppSettings
import com.alflabs.utils.ILogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.Optional


class MediaPlayerWrapper @AssistedInject constructor(
    @Assisted private val context: Context,
    private val logger: ILogger,
    private val appSettings: AppSettings,
    private val audioManager: Optional<AudioManager>,
) {
    @AssistedFactory
    interface Factory {
        fun create(context: Context): MediaPlayerWrapper
    }

    companion object {
        private val TAG = MediaPlayerWrapper::class.java.simpleName
    }

    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var stopPlayingCallback: (() -> Unit)? = null


    fun player(): MediaPlayer? {
        return mediaPlayer
    }

    fun pausePlayer() {
        mediaPlayer?.pause()
    }

    fun resumePlayer() {
        mediaPlayer?.start()
    }

    /**
     * Stop current player and start with the new URI.
     *
     * Completion callback: called when a playback finished (to trigger the next one).
     *
     * Stop callback: called when the player needs to be terminated. The callback MUST
     * call [stopPlaying()].
     */
    suspend fun startPlaying(uri: Uri, completionCallback: () -> Unit, stopCallback: () -> Unit) {
        stopPlayingCallback = stopCallback
        stopPlayerWithCallback()
        try {
            mediaPlayer = MediaPlayer().apply {
                logger.d(TAG, "@@ startPlaying: $uri")
                setDataSource(context, uri)
                if (appSettings.useUsbAudio()) {
                    routeAudioToUsbDac(this)
                }
                prepare()
                setupAudioEffects(audioSessionId)
                start()
                setOnCompletionListener {
                    completionCallback()
                }
            }
        } catch (e: Exception) {
            logger.d(TAG, "Error playing audio", e)
            Toast.makeText(context, "Error playing audio file", Toast.LENGTH_LONG).show()
            stopPlayerWithCallback()
        }

    }

    private suspend fun setupAudioEffects(audioSessionId: Int) {
        try {
            if (appSettings.useLoundnessEnhancer()) {
                // LoudnessEnhancer (API 19+)
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    // Boost by 8.0 dB (800 mB). Adjust this value as needed.
                    // TBD add value in prefs
                    setTargetGain(800)
                    enabled = true
                }
            }

            if (appSettings.useSoundLimiter()) {
                // DynamicsProcessing (API 28+)
                // The DynamicsProcessing Limiter is a single-band compressor located at the
                // final stage of the Android DynamicsProcessing audio effect chain. It is
                // primarily used to prevent audio clipping, overloading, and distortion by
                // ensuring the signal does not exceed a defined threshold.
                val limiter = Limiter(
                    /*inUse=*/ true,
                    /*enabled=*/ true,
                    /*linkGroup=*/ 0,
                    /*attackTime=*/ 1.0f,
                    /*releaseTime=*/ 60.0f,
                    /*ratio=*/ 20.0f,
                    /*threshold=*/ -2.0f,
                    /*postGain=*/ 0.0f
                )

                val config = Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1,                                  // Mono OGG files.
                    /*preEqInUse=*/ false,
                    /*preEqBandCount=*/ 1,
                    /*mbcInUse=*/ false,
                    /*mbcBandCount=*/ 1,
                    /*postEqInUse=*/ false,
                    /*postEqBandCount=*/ 1,
                    /*limiterInUse=*/ true
                )
                    .setPreferredFrameDuration(10.0f)
                    .setLimiterAllChannelsTo(limiter)
                    .build()

                dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                    enabled = true
                }
            }
        } catch (e: Exception) {
            logger.d(TAG, "Error setting up audio effects", e)
        }
    }

    fun routeAudioToUsbDac(mediaPlayer: MediaPlayer) {
        audioManager.ifPresent { manager ->
            // Fetch all available audio output devices
            val outputDevices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

            // Find the USB-C DAC / Headset (vs Built-in, Bluetooth, or Wired)
            val usbDacDevice = outputDevices.firstOrNull { device ->
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                        device.type == AudioDeviceInfo.TYPE_USB_DEVICE
            }

            if (usbDacDevice != null) {
                // Explicitly bind MediaPlayer playback to the USB DAC
                val success = mediaPlayer.setPreferredDevice(usbDacDevice)
                logger.d(
                    TAG,
                    "@@ Routing USB DAC: ${outputDevices.size} devices found, success = $success, device = $usbDacDevice"
                )
            } else {
                logger.d(
                    TAG,
                    "@@ Routing USB DAC: No USB device found among ${outputDevices.size} devices"
                )
            }
        }
    }

    fun stopPlayerWithCallback() {
        val callback = stopPlayingCallback
        if (callback == null) {
            stopPlaying()
        } else {
            callback.invoke()
        }
    }

    fun stopPlaying() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        dynamicsProcessing?.release()
        dynamicsProcessing = null

        mediaPlayer?.release()
        mediaPlayer = null
    }
}
