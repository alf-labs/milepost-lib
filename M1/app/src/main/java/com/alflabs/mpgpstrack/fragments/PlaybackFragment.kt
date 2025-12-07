package com.alflabs.mpgpstrack.fragments

import androidx.navigation.NavDirections
import com.alflabs.mpgpstrack.main.StatusData
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PlaybackFragment : SensorsFragment() {
    override val runningMode = StatusData.PLAYBACK
    override fun actionToPermissionsDlg(): NavDirections =
        PlaybackFragmentDirections.actionPlayToPermissionsDlg()
}
