package com.alflabs.milepostlib.fragments.gpsRecord

import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.alflabs.milepostlib.fragments.SensorsFragment
import com.alflabs.milepostlib.op.OperatorMode
import com.alflabs.milepostlib.db.GpsTrackDao
import com.alflabs.milepostlib.db.GpsTrackEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class GpsRecordFragment : SensorsFragment() {
    override val operatorMode = OperatorMode.GPS_RECORD

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
            operator.startGpsRecord(track.copy(id = recordId))
        }
    }

    override fun onStopButton() {
        operator.currentGpsTrackId?.let {
            operator.stopGpsRecord()
            findNavController().navigate(GpsRecordFragmentDirections.actionRecordToReview(it))
        }
    }
}
