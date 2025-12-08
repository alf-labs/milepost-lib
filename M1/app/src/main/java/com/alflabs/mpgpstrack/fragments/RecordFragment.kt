package com.alflabs.mpgpstrack.fragments

import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.alflabs.mpgpstrack.main.StatusData
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class RecordFragment : SensorsFragment() {
    override val runningMode = StatusData.RECORD

    override fun actionToPermissionsDlg(): NavDirections =
        RecordFragmentDirections.actionRecordToPermissionsDlg()

    override fun onStopButton(trackId: Long?) {
        trackId?.let {
            findNavController().navigate(RecordFragmentDirections.actionRecordToReview(it))
        }
    }
}
