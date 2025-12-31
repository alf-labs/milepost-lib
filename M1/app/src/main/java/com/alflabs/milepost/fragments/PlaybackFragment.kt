package com.alflabs.milepost.fragments

import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import com.alflabs.milepost.op.Operator
import com.alflabs.milepost.op.OperatorMode
import com.alflabs.milepost.tracks.TrackEntityDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class PlaybackFragment : SensorsFragment() {
    override val operatorMode = OperatorMode.PLAYBACK

    @Inject lateinit var operator: Operator
    @Inject lateinit var trackEntityDao: TrackEntityDao

    override fun actionToPermissionsDlg(): NavDirections =
        PlaybackFragmentDirections.actionPlayToPermissionsDlg()

    override fun onStartButton() {
        if (globalState.operatorMode == operatorMode) return

        val trackId = appSettings.playbackTrackId(lifecycleScope)

        lifecycleScope.launch {
            val track = trackEntityDao.getById(trackId).first()
            track?.let {
                operator.startPlayback(track)
            }
        }
    }

    override fun onStopButton() {
        operator.currentTrackId?.let {
            operator.stopPlayback()
        }
    }
}
