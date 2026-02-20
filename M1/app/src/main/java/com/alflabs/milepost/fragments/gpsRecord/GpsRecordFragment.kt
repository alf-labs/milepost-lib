package com.alflabs.milepost.fragments.gpsRecord

import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.alflabs.milepost.fragments.SensorsFragment
import com.alflabs.milepost.op.OperatorMode
import com.alflabs.milepost.tracks.GpsTrackDao
import com.alflabs.milepost.tracks.GpsTrackEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class GpsRecordFragment : SensorsFragment() {
    override val operatorMode = OperatorMode.RECORD

    @Inject
    lateinit var gpsTrackDao: GpsTrackDao

    override fun actionToPermissionsDlg(): NavDirections =
        GpsRecordFragmentDirections.actionRecordToPermissionsDlg()

    override fun onStartButton() {
        if (globalState.operatorMode == operatorMode) return

        lifecycleScope.launch {
            val track = GpsTrackEntity(
                trackLabel = "", // this triggers the title dialog in the review screen
                createdDateISO = Date().toString(),
            )
            val recordId = gpsTrackDao.insert(track)
            operator.startRecord(track.copy(id = recordId))
        }
    }

    override fun onStopButton() {
        operator.currentTrackId?.let {
            operator.stopRecord()
            findNavController().navigate(GpsRecordFragmentDirections.actionRecordToReview(it))
        }
    }
}
