package com.alflabs.milepost.fragments

import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.alflabs.milepost.op.OperatorMode
import com.alflabs.milepost.tracks.GpsTrackEntity
import com.alflabs.milepost.tracks.GpsTrackEntityDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject


@AndroidEntryPoint
class RecordFragment : SensorsFragment() {
    override val operatorMode = OperatorMode.RECORD

    @Inject lateinit var gpsTrackEntityDao: GpsTrackEntityDao

    override fun actionToPermissionsDlg(): NavDirections =
        RecordFragmentDirections.actionRecordToPermissionsDlg()

    override fun onStartButton() {
        if (globalState.operatorMode == operatorMode) return

        lifecycleScope.launch {
            val track = GpsTrackEntity(
                    trackLabel = "", // this triggers the title dialog in the review screen
                    createdDateISO = Date().toString(),
                )
            val recordId = gpsTrackEntityDao.insert(track)
            operator.startRecord(track.copy(id = recordId))
        }
    }

    override fun onStopButton() {
        operator.currentTrackId?.let {
            operator.stopRecord()
            findNavController().navigate(RecordFragmentDirections.actionRecordToReview(it))
        }
    }
}
