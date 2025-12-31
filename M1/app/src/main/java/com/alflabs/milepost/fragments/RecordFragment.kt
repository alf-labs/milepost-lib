package com.alflabs.milepost.fragments

import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.alflabs.milepost.op.OperatorMode
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.milepost.tracks.TrackEntityDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject


@AndroidEntryPoint
class RecordFragment : SensorsFragment() {
    override val operatorMode = OperatorMode.RECORD

    @Inject lateinit var trackEntityDao: TrackEntityDao

    override fun actionToPermissionsDlg(): NavDirections =
        RecordFragmentDirections.actionRecordToPermissionsDlg()

    override fun onStartButton() {
        if (globalState.operatorMode == operatorMode) return

        lifecycleScope.launch {
            val track = TrackEntity(
                    trackLabel = "", // this triggers the title dialog in the review screen
                    createdDateISO = Date().toString(),
                )
            val recordId = trackEntityDao.insert(track)
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
