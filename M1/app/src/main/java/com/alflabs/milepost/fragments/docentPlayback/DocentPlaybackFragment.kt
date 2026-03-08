package com.alflabs.milepost.fragments.docentPlayback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.DocentPlaybackFragmentBinding
import com.alflabs.milepost.db.DocentTrackDao
import com.alflabs.milepost.fragments.SensorsFragment
import com.alflabs.milepost.location.LatLongData
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.location.LocationFlags
import com.alflabs.milepost.location.hasFlag
import com.alflabs.milepost.op.MpDirection
import com.alflabs.milepost.op.MpEstimate
import com.alflabs.milepost.op.OperatorMode
import com.alflabs.milepost.db.GpsTrackDao
import com.alflabs.milepost.db.PlayTriggerDao
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
class DocentPlaybackFragment : SensorsFragment() {

    companion object {
        private val TAG = DocentPlaybackFragment::class.java.simpleName
    }

    override val operatorMode = OperatorMode.DOCENT_PLAYBACK

    // This property is only valid between onCreateView and onDestroyView.
    private var _playbackBinding: DocentPlaybackFragmentBinding? = null
    private val playbackBinding get() = _playbackBinding!!

    override val hasBinding get() = _playbackBinding != null
    override val startButton get() = playbackBinding.startButton
    override val stopButton get() = playbackBinding.stopButton
    override val mapView get() = playbackBinding.mapView
    override val statusText get() = playbackBinding.statusText
    override val locationText get() = playbackBinding.locationText
    override val locationAccText get() = playbackBinding.locationAccText
    override val speedText get() = playbackBinding.speedText
    override val speedAccText get() = null
    override val zoomInButton get() = playbackBinding.zoomInBtn
    override val zoomOutButton get() = playbackBinding.zoomOutBtn

    @Inject lateinit var gpsTrackDao: GpsTrackDao
    @Inject lateinit var docentTrackDao: DocentTrackDao
    @Inject lateinit var playTriggerDao: PlayTriggerDao


    private val viewModel: DocentPlaybackViewModel by viewModels()

    private val mpEstimateSubscriber: ISubscriber<in MpEstimate?> =
        ISubscriber<MpEstimate?> { _, event -> onMpEstimate(googleMap, event) }
    private var markerTrackStart: Marker? = null
    private var markerTrackEnd: Marker? = null
    private var markerMpCurrent: Marker? = null
    private var markerPositionCurrent: Marker? = null
    private var trackPolylineStart: Polyline? = null
    private var trackPolylineEnd: Polyline? = null
    private var lineColorFwd: Int = -1
    private var lineColorPrev: Int = -1
    private var docentPlaybackAdapter: DocentPlaybackAdapter? = null


    override fun actionToPermissionsDlg(): NavDirections =
        DocentPlaybackFragmentDirections.actionPlayToPermissionsDlg()

    override fun getLocationsList(): List<LocationData>? =
        operator.currentLocationsList ?: viewModel.filteredLocations

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        logger.d(TAG, "onCreateView")
        _playbackBinding = DocentPlaybackFragmentBinding.inflate(inflater, container, false)

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
            docentPlaybackAdapter = DocentPlaybackAdapter()

            val layoutManager = LinearLayoutManager(requireContext())
            playbackBinding.playTriggerList.adapter = docentPlaybackAdapter
            playbackBinding.playTriggerList.layoutManager = layoutManager

            val dividerItemDecoration = DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
            playbackBinding.playTriggerList.addItemDecoration(dividerItemDecoration)

            if (viewModel.docentPlaybackTrack == null) {
                // Get current op playback track if ongoing
                viewModel.docentPlaybackTrack = operator.getDocentPlaybackTrack()
                viewModel.gpsPlaybackTrack = operator.getGpsPlaybackTrack()
                if (viewModel.triggers.isEmpty()) {
                    viewModel.triggers.addAll(operator.getDocentPlaybackTriggers() ?: emptyList())
                }

                if (viewModel.docentPlaybackTrack == null) {
                    // If not, load the current track id from prefs
                    val docentTrackId = appSettings.docentPlaybackTrackId()
                    logger.d(TAG, "@@ Playback onResume: docentTrackId = $docentTrackId")

                    if (docentTrackId != -1L) {
                        viewModel.docentPlaybackTrack = docentTrackDao.getById(docentTrackId).first()

                        if (viewModel.triggers.isEmpty()) {
                            // Load triggers.
                            // Ensure they all have a trigger structure and a valid audio file id.
                            viewModel.triggers.addAll(
                                playTriggerDao
                                    .getTriggersForDocentTrack(docentTrackId)
                                    .first()
                                    .filter { it.playbackTrigger != null && it.audioFileId > 0 }
                                    .map { DocentPlaybackItem(it) }
                            )

                            // TBD we need to kick in some kind of audio file verification
                            // to ensure all audio files are playable BEFORE the play button
                            // can be enabled. If not, we should redirect the user to the audio
                            // files fragment so that they can fix the file references.
                        }
                    }
                }

                if (viewModel.gpsPlaybackTrack == null) {
                    val gpsTrackId = viewModel.docentPlaybackTrack?.gpsTrackId ?: -1L
                    logger.d(TAG, "@@ Playback onResume: trackId = $gpsTrackId")

                    if (gpsTrackId != -1L) {
                        viewModel.gpsPlaybackTrack = gpsTrackDao.getById(gpsTrackId).first()
                    }
                }
            }

            if (viewModel.docentPlaybackTrack == null) {
                // Close the fragment if no track is found
                Toast.makeText(requireContext(), "Please select an existing Docent Track first.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }

            if (viewModel.gpsPlaybackTrack == null) {
                // Close the fragment if no track is found
                Toast.makeText(requireContext(), "Please select an existing GPS Track first.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }

            docentPlaybackAdapter?.submitList(viewModel.triggers)

            viewModel.docentPlaybackTrack?.let { docentTrack ->
                // TBD do we do not need the docent track entity?
                // We should at least display its name.
            }

            viewModel.gpsPlaybackTrack?.let { gpsTrack ->
                if (!gpsTrack.startLabel.isNullOrEmpty()) {
                    viewModel.startLabel = gpsTrack.startLabel
                }
                if (!gpsTrack.endLabel.isNullOrEmpty()) {
                    viewModel.endLabel = gpsTrack.endLabel
                }

                if (viewModel.filteredLocations.isEmpty()) {
                    gpsTrack.locationList?.locationsList?.let { list ->
                        viewModel.filteredLocations.clear()
                        // The playback track only displays non-ignored points.
                        val notIgnored = list.filter { loc ->
                            !loc.flags.hasFlag(LocationFlags.IGNORE)
                        }
                        viewModel.filteredLocations.addAll(notIgnored)
                    }
                }
            }

            if (viewModel.filteredLocations.isNotEmpty()) {
                val notIgnored = viewModel.filteredLocations
                playbackBinding.linearTrackView.setStations(
                    notIgnored.filterIndexed { index, loc ->
                        index == 0 || index == notIgnored.lastIndex || loc.hasPoiLabel()
                    }
                    .mapIndexed { index, loc ->
                        val title =
                            if (loc.hasPoiLabel()) loc.poiLabel
                            else if (index == 0) viewModel.startLabel
                            else if (index == notIgnored.lastIndex) viewModel.endLabel
                            else "--"
                        Pair(loc.mp, title)
                    }
                )
            }

            playbackBinding.linearTrackView.setSegments(
                viewModel.triggers.map {
                    Triple(it.id,
                        it.playbackTrigger!!.mpMin,
                        it.playbackTrigger.mpMax)
                }
            )

            playbackBinding.trackLabelText.text = viewModel.gpsPlaybackTrack?.trackLabel ?: "No track selected"
            if (globalState.operatorMode != OperatorMode.GPS_PLAYBACK) {
                playbackBinding.directionText.text = "Playback Not Started Yet"
            } else {
                // Restore UI from viewModel if already in playback
                viewModel.lastMp?.let { onMpEstimate(googleMap, it) }
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
            viewModel.docentPlaybackTrack?.let { docentTrack ->
                viewModel.gpsPlaybackTrack?.let { gpsTrack ->
                    playbackBinding.directionText.text = "--"
                    operator.startDocentPlayback(docentTrack, gpsTrack, viewModel.triggers)
                }
            }
        }
    }

    override fun onStopButton() {
        operator.currentDocentTrackId?.let {
            operator.stopDocentPlayback()
        }
    }

    override fun setupInitialMapZoom(map: GoogleMap,
                                     position: LatLng,
                                     zoomLevel: Float): CameraUpdate {
        val track = viewModel.gpsPlaybackTrack
        val locations = getLocationsList()
        if (track != null && !locations.isNullOrEmpty()) {
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
                    .title(if (locFirst.hasPoiLabel()) locFirst.poiLabel
                            else if (track.startLabel.isNullOrEmpty()) "Start"
                            else track.startLabel)
                    .position(startPos)
            )
            markerTrackEnd = map.addMarker(
                MarkerOptions()
                    .title(if (locLast.hasPoiLabel()) locLast.poiLabel
                            else if (track.endLabel.isNullOrEmpty()) "End"
                            else track.endLabel)
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

    private fun onMpEstimate(map: GoogleMap?, mp: MpEstimate) {
        logger.d(TAG, "onMpEstimate: $mp")
        // This may potentially be called outside of the proper binding lifecycle.
        if (!hasBinding) return

        val position = LatLng(mp.latLong.latitude, mp.latLong.longitude)
        if (viewModel.lastMp == null && map != null) {
            // This is the first MP. Center on mp location.
            val update = CameraUpdateFactory.newLatLngZoom(position, mapZoomLevel)
            map.animateCamera(update)
            logger.d(TAG, "@@ First MP update: $update with zoom level $mapZoomLevel")
        } else if (map != null && mp != viewModel.lastMp) {
            // Just center on the new MP location, no zoom.
            val update = CameraUpdateFactory.newLatLng(position)
            map.animateCamera(update)
        }

        viewModel.lastMp = mp

        playbackBinding.linearTrackView.setCurrentMp(mp.mp)
        playbackBinding.mpText.text = String.format("MP %.2f", mp.mp)

        playbackBinding.directionText.text = when(mp.direction) {
            MpDirection.TO_START -> viewModel.startLabel
            MpDirection.TO_END -> viewModel.endLabel
            else -> "--"
        } + " [# ${mp.locIndex}]" // DEBUG

        markerMpCurrent?.let {
            it.position = position
        }

        if (mp.locIndex >= 0 && (mp.locIndex != viewModel.lastMpIndexRendered || trackPolylineStart?.points?.isEmpty() == true)) {
            viewModel.lastMpIndexRendered = mp.locIndex

            getLocationsList()?.let { locations ->
                // Indices: [0 ... render ... size-1]
                // Number of segments is the usual off-by-one interval problem:
                // - From start: 0..render = render - 0 + 1
                // - From end  : render..size-1 = (size-1) - render + 1 = size - render
                trackPolylineStart?.points =
                    locations.take(viewModel.lastMpIndexRendered + 1).map {
                        LatLng(it.latLong.latitude, it.latLong.longitude)
                    }
                trackPolylineEnd?.points =
                    locations.takeLast(locations.size - viewModel.lastMpIndexRendered).map {
                        LatLng(it.latLong.latitude, it.latLong.longitude)
                    }
            }
        }
        if ((mp.direction != viewModel.lastMpDirectionRendered && mp.direction != MpDirection.UNKNOWN)
            || (mp.direction != MpDirection.UNKNOWN && markerTrackStart?.title == null)) { // title null means markers might need icon update

            viewModel.lastMpDirectionRendered = mp.direction
            when (mp.direction) {
                MpDirection.TO_START -> {
                    trackPolylineStart?.color = lineColorFwd
                    trackPolylineEnd?.color = lineColorPrev
                    markerTrackStart?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    markerTrackEnd?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    playbackBinding.linearTrackView.setDirectionTowardsStart(true)
                }
                MpDirection.TO_END -> {
                    trackPolylineStart?.color = lineColorPrev
                    trackPolylineEnd?.color = lineColorFwd
                    markerTrackStart?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    markerTrackEnd?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    playbackBinding.linearTrackView.setDirectionTowardsStart(false)
                }
                else -> { /* no-op */ }
            }
        }
    }

    override fun updateMapForCurrentLocation(
        currentLatLong: LatLongData,
        map: GoogleMap,
    ) {
        // Implementation detail: there is no "load map the first time" function as this can be used
        // during recording where we start with no map track data. Instead every call to this method
        // checks if member fields are null and only initializes them once if they are not set yet.

        val position = LatLng(currentLatLong.latitude, currentLatLong.longitude)
        if (markerPositionCurrent == null) {
            markerPositionCurrent = map.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_blue))
                    .position(position)
                    .anchor(0.5f, 0.5f)
            )
            // only move the map if the current MP is not known
            if (viewModel.lastMp == null) {
                val update = setupInitialMapZoom(map, position, mapZoomLevel)
                logger.d(TAG, "@@ First location update: $update with zoom level $mapZoomLevel")
                map.animateCamera(update)
            }
        } else {
            markerPositionCurrent!!.position = position
            if (viewModel.lastMp == null) {
                val update = CameraUpdateFactory.newLatLng(position)
                map.animateCamera(update)
            }
        }

        if (markerMpCurrent == null) {
            markerMpCurrent = map.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_purple))
                    .position(position)
                    .anchor(0.5f, 0.5f)
            )
        }

        getLocationsList()?.let { locations ->
            if (locations.size > 1) {
                // This creates the track path before "playback" starts.
                // All polyline segments are initially in the "start polyline" with
                // the "forward color" to appear the expected blue.
                if (trackPolylineStart == null) {
                    val width = resources.getDimensionPixelSize(R.dimen.map_track)
                    val polylineOptions = PolylineOptions()
                        .color(lineColorFwd)
                        .width(width.toFloat())
                    trackPolylineStart = map.addPolyline(polylineOptions)
                    trackPolylineStart?.points =
                        locations.map {
                            LatLng(it.latLong.latitude, it.latLong.longitude)
                        }

                    // Create the POI markers once.
                    val locFirst = locations.first()
                    val locLast = locations.last()
                    for (location in locations) {
                        val pos = LatLng(location.latLong.latitude, location.latLong.longitude)
                        if (location.hasPoiLabel() && location != locFirst && location != locLast) {
                            map.addMarker(
                                MarkerOptions()
                                    .position(pos)
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_YELLOW))
                                    .title(location.poiLabel)
                                    .alpha(0.5f)
                            )
                        }
                    }
                }
                if (trackPolylineEnd == null) {
                    val width = resources.getDimensionPixelSize(R.dimen.map_track)
                    val polylineOptions = PolylineOptions()
                        .color(lineColorFwd)
                        .width(width.toFloat())
                    trackPolylineEnd = map.addPolyline(polylineOptions)
                }

                // If we are restoring state, we might need to re-apply the MP rendering logic
                // if it hasn't happened yet because onMpEstimate was called before the map was fully ready
                // or before polylines were initialized here.
                viewModel.lastMp?.let { onMpEstimate(map, it) }
            }
        }
    }
}
