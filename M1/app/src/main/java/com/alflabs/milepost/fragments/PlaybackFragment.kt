package com.alflabs.milepost.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.PlaybackFragmentBinding
import com.alflabs.milepost.location.LatLongData
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.op.MpDirection
import com.alflabs.milepost.op.MpEstimate
import com.alflabs.milepost.op.OperatorMode
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.milepost.tracks.TrackEntityDao
import com.alflabs.rx.AndroidSchedulers
import com.alflabs.rx.ISubscriber
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
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

    // This property is only valid between onCreateView and onDestroyView.
    private var _playbackBinding: PlaybackFragmentBinding? = null
    private val playbackBinding get() = _playbackBinding!!

    override val hasBinding get() = _playbackBinding != null
    override val startButton get() = playbackBinding.startButton
    override val stopButton get() = playbackBinding.stopButton
    override val mapView get() = playbackBinding.mapView
    override val statusText get() = playbackBinding.statusText
    override val locationText get() = playbackBinding.locationText
    override val locationAccText get() = playbackBinding.locationAccText
    override val speedText get() = playbackBinding.speedText
    override val bearingText get() = null
    override val speedAccText get() = null
    override val bearingAccText get() = null

    @Inject lateinit var trackEntityDao: TrackEntityDao

    private var playbackTrack: TrackEntity? = null
    private var startLabel = "Startbound"
    private var endLabel = "Endbound"
    private val mpEstimateSubscriber: ISubscriber<in MpEstimate?> =
        ISubscriber<MpEstimate?> { _, event -> onMpEstimate(event) }
    private var markerTrackStart: Marker? = null
    private var markerTrackEnd: Marker? = null
    private var markerMpCurrent: Marker? = null
    private var markerPositionCurrent: Marker? = null
    private var trackPolyline1: Polyline? = null
    private var trackPolyline2: Polyline? = null
    private var lastMp: MpEstimate? = null
    private var lastMpIndexRendered = -1
    private var lineColorFwd: Int = -1
    private var lineColorPrev: Int = -1


    override fun actionToPermissionsDlg(): NavDirections =
        PlaybackFragmentDirections.actionPlayToPermissionsDlg()

    override fun getLocationsList(): List<LocationData>? =
        playbackTrack?.locationList?.locationsList ?: operator.currentLocationsList


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        logger.d(TAG, "onCreateView")
        _playbackBinding = PlaybackFragmentBinding.inflate(inflater, container, false)

        lineColorFwd = requireContext().getColor(R.color.map_track)
        lineColorPrev = requireContext().getColor(R.color.map_track_prev)

        return playbackBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.d(TAG, "onViewCreated")
    }

    override fun onResume() {
        super.onResume()

        globalState.mpEstimateStream.subscribe(AndroidSchedulers.mainThread(), mpEstimateSubscriber)

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

            playbackTrack?.let { track ->
                if (!track.startLabel.isNullOrEmpty()) {
                    startLabel = track.startLabel
                }
                if (!track.endLabel.isNullOrEmpty()) {
                    endLabel = track.endLabel
                }
            }

            playbackBinding.trackLabelText.text = playbackTrack?.trackLabel ?: "No track selected"
            if (globalState.operatorMode != OperatorMode.PLAYBACK) {
                playbackBinding.directionText.text = "Playback Not Started Yet"
            }
        }
    }

    override fun onPause() {
        super.onPause()
        globalState.mpEstimateStream.remove(mpEstimateSubscriber)
    }

    override fun onStartButton() {
        if (globalState.operatorMode == operatorMode) return

        lifecycleScope.launch {
            playbackTrack?.let {
                playbackBinding.directionText.text = "--"
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
            markerTrackStart = map.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title(if (track.startLabel.isNullOrEmpty()) "Start" else track.startLabel)
                    .position(startPos)
            )
            markerTrackEnd = map.addMarker(
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

    private fun onMpEstimate(event: MpEstimate) {
        logger.d(TAG, "onMpEstimate: $event")
        // This may potentially be called outside of the proper binding lifecycle.
        _playbackBinding?.let {
            lastMp = event
            it.mpText.text = String.format("MP %.1f", event.mp)

            it.directionText.text = when(event.direction) {
                MpDirection.TO_START -> startLabel
                MpDirection.TO_END -> endLabel
                else -> "--"
            } + " [# ${event.locIndex}]" // DEBUG
        }
    }

    override fun updateMapForCurrentLocation(
        currentLatLong: LatLongData,
        map: GoogleMap,
    ) {
        if (markerPositionCurrent == null) {
            val position = LatLng(currentLatLong.latitude, currentLatLong.longitude)
            markerPositionCurrent = map.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_purple))
                    .position(position)
                    .anchor(0.5f, 0.5f)
            )
            val update = setupInitialMapZoom(map, position, MAPS_ZOOM_LEVEL)
            map.animateCamera(update)
        } else {
            val position = LatLng(currentLatLong.latitude, currentLatLong.longitude)
            markerPositionCurrent!!.position = position
        }

        lastMp?.let { mp ->
            if (markerMpCurrent == null) {
                val position = LatLng(mp.latLong.latitude, mp.latLong.longitude)
                markerMpCurrent = map.addMarker(
                    MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_blue))
                        .position(position)
                        .anchor(0.5f, 0.5f)
                )
            }
        }

        val locationsList = getLocationsList()
        locationsList?.let { locations ->
            if (locations.size > 1) {
                if (trackPolyline1 == null) {
                    val width = resources.getDimensionPixelSize(R.dimen.map_track)
                    val polylineOptions = PolylineOptions()
                        .color(lineColorFwd)
                        .width(width.toFloat())
                    trackPolyline1 = map.addPolyline(polylineOptions)
                    trackPolyline1?.points =
                        locations.map {
                            LatLng(it.latLong.latitude, it.latLong.longitude)
                        }
                }
                if (trackPolyline2 == null) {
                    val width = resources.getDimensionPixelSize(R.dimen.map_track)
                    val polylineOptions = PolylineOptions()
                        .color(lineColorPrev)
                        .width(width.toFloat())
                    trackPolyline2 = map.addPolyline(polylineOptions)
                }
            }
        }

        lastMp?.let { mp ->
            if (mp.locIndex >= 0 && lastMpIndexRendered >= 0 && mp.locIndex != lastMpIndexRendered) {
                lastMpIndexRendered = mp.locIndex

                locationsList?.let { locations ->
                    trackPolyline1?.points =
                        locations.take(lastMpIndexRendered).map {
                            LatLng(it.latLong.latitude, it.latLong.longitude)
                        }
                    trackPolyline2?.points =
                        locations.takeLast(locations.size - 1 - lastMpIndexRendered).map {
                            LatLng(it.latLong.latitude, it.latLong.longitude)
                        }
                    when (mp.direction) {
                        MpDirection.TO_START -> {
                            trackPolyline1?.color = lineColorFwd
                            trackPolyline2?.color = lineColorPrev
                        }
                        MpDirection.UNKNOWN,
                        MpDirection.TO_END -> {
                            trackPolyline1?.color = lineColorPrev
                            trackPolyline2?.color = lineColorFwd
                        }
                    }
                }

                val position = LatLng(mp.latLong.latitude, mp.latLong.longitude)
                markerMpCurrent?.position = position
                val update = CameraUpdateFactory.newLatLng(position)
                map.animateCamera(update)
            }
        }
    }
}
