package com.alflabs.milepost.fragments

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.alflabs.milepost.R
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.op.OperatorMode
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.milepost.tracks.TrackEntityDao
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class PlaybackFragment : SensorsFragment() {

    companion object {
        private val TAG = PlaybackFragment::class.java.simpleName
    }

    override val operatorMode = OperatorMode.PLAYBACK

    @Inject lateinit var trackEntityDao: TrackEntityDao

    private var playbackTrack: TrackEntity? = null

    override fun actionToPermissionsDlg(): NavDirections =
        PlaybackFragmentDirections.actionPlayToPermissionsDlg()

    override fun getLocationsList(): List<LocationData>? =
        playbackTrack?.locationList?.locationsList ?: operator.currentLocationsList


    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            if (playbackTrack == null) {
                // Get current op playback track if ongoing
                playbackTrack = operator.getPlaybackTrack()

                if (playbackTrack == null) {
                    // If not, load the current track id from prefs
                    val trackId = appSettings.playbackTrackId(lifecycleScope)
                    logger.d(TAG, "@@ Playback onResume: trackId = $trackId")

                    if (trackId != -1L) {
                        playbackTrack = trackEntityDao.getById(trackId).first()
                    }
                }
            }

            if (playbackTrack == null) {
                // Close the fragment if no track is found
                Toast.makeText(requireContext(), "Please select an existing track first.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

    }

    override fun onStartButton() {
        if (globalState.operatorMode == operatorMode) return


        lifecycleScope.launch {
            playbackTrack?.let {
                operator.startPlayback(it)
            }
        }
    }

    override fun onStopButton() {
        operator.currentTrackId?.let {
            operator.stopPlayback()
        }
    }

    override fun setupInitialMapZoom(map: GoogleMap,
                                     position: LatLng,
                                     zoomLevel: Float): CameraUpdate {
        val track = playbackTrack
        val locations = getLocationsList()
        if (track != null && locations != null && locations.isNotEmpty()) {
            val locFirst = locations.first()
            val locLast = locations.last()
            val startPos = LatLng(locFirst.latLong.latitude, locFirst.latLong.longitude)
            val endPos = LatLng(locLast.latLong.latitude, locLast.latLong.longitude)
            val boundsBuilder = LatLngBounds.Builder()
            boundsBuilder.include(startPos)
            boundsBuilder.include(endPos)
            map.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title(if (track.startLabel.isNullOrEmpty()) "Start" else track.startLabel)
                    .position(startPos)
            )
            map.addMarker(
                MarkerOptions()
                    .title(if (track.endLabel.isNullOrEmpty()) "End" else track.endLabel)
                    .position(endPos)
            )
            // Zoom to the requested bounds with offset from edges of the map in pixels
            val bounds = boundsBuilder.build()
            val padding = resources.getDimensionPixelSize(R.dimen.map_padding)
            val update = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            logger.d(TAG, "@@ Camera bounds: $bounds")
            return update
        }

        return super.setupInitialMapZoom(map, position, zoomLevel)
    }
}
