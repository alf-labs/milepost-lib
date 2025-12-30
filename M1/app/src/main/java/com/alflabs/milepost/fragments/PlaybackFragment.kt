package com.alflabs.milepost.fragments

import androidx.navigation.NavDirections
import com.alflabs.milepost.main.OperatorMode
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PlaybackFragment : SensorsFragment() {
    override val operatorMode = OperatorMode.PLAYBACK

    override fun actionToPermissionsDlg(): NavDirections =
        PlaybackFragmentDirections.actionPlayToPermissionsDlg()
}
