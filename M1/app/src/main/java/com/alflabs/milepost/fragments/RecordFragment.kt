package com.alflabs.milepost.fragments

import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.alflabs.milepost.op.OperatorMode
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class RecordFragment : SensorsFragment() {
    override val operatorMode = OperatorMode.RECORD

    override fun actionToPermissionsDlg(): NavDirections =
        RecordFragmentDirections.actionRecordToPermissionsDlg()

    override fun onStopButton(trackId: Long?) {
        trackId?.let {
            findNavController().navigate(RecordFragmentDirections.actionRecordToReview(it))
        }
    }
}
