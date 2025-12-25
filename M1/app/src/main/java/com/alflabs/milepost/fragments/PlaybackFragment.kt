package com.alflabs.milepost.fragments

import androidx.navigation.NavDirections
import com.alflabs.milepost.main.StatusData
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PlaybackFragment : SensorsFragment() {
    override val runningMode = StatusData.PLAYBACK
    override fun actionToPermissionsDlg(): NavDirections =
        PlaybackFragmentDirections.actionPlayToPermissionsDlg()
}
