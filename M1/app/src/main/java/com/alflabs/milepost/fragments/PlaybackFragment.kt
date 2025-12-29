package com.alflabs.milepost.fragments

import androidx.navigation.NavDirections
import com.alflabs.milepost.main.GlobalMode
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PlaybackFragment : SensorsFragment() {
    override val runningMode = GlobalMode.PLAYBACK
    override fun actionToPermissionsDlg(): NavDirections =
        PlaybackFragmentDirections.actionPlayToPermissionsDlg()
}
