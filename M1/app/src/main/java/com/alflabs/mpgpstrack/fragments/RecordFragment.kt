package com.alflabs.mpgpstrack.fragments

import androidx.navigation.NavDirections
import com.alflabs.mpgpstrack.main.StatusData
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class RecordFragment : SensorsFragment() {
    override val runningMode = StatusData.RECORD
    override fun actionToPermissionsDlg(): NavDirections =
        RecordFragmentDirections.actionRecordToPermissionsDlg()
}
