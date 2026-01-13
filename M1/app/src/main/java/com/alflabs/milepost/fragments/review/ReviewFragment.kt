package com.alflabs.milepost.fragments.review

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.ReviewFragmentBinding
import com.alflabs.milepost.fragments.tracks.toText
import com.alflabs.milepost.location.LatLongData
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.location.LocationDataList
import com.alflabs.milepost.location.LocationDistance
import com.alflabs.milepost.location.LocationFlags
import com.alflabs.milepost.location.changeFlag
import com.alflabs.milepost.location.clearFlag
import com.alflabs.milepost.location.copy
import com.alflabs.milepost.location.hasFlag
import com.alflabs.milepost.location.setFlag
import com.alflabs.milepost.app.AppSettings
import com.alflabs.milepost.tracks.GpsTrackEntity
import com.alflabs.milepost.tracks.GpsTrackEntityDao
import com.alflabs.milepost.tracks.GpsTrackSummary
import com.alflabs.milepost.tracks.copy
import com.alflabs.milepost.tracks.gpsTrackSummary
import com.alflabs.milepost.utils.ImporterExporter
import com.alflabs.milepost.utils.MapStyle
import com.alflabs.milepost.utils.Units
import com.alflabs.utils.ApiHelper
import com.alflabs.utils.ILogger
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class ReviewFragment : Fragment() {
    companion object {
        private val TAG = ReviewFragment::class.java.simpleName
        private const val MAPS_ZOOM_LEVEL = 15f
        private const val KEY_SELECTED_POSITION = "selected-position"
    }

    @Inject lateinit var logger: ILogger
    @Inject lateinit var importerExporter: ImporterExporter
    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var gpsTrackEntityDao: GpsTrackEntityDao

    private var _binding: ReviewFragmentBinding? = null
    private val binding get() = _binding!!
    private val fragmentArgs: ReviewFragmentArgs by navArgs()
    private val viewModel: ReviewViewModel by viewModels()
    private var reviewLocationAdapter: ReviewLocationAdapter? = null
    private var gpsTrackEntity: GpsTrackEntity? = null
    private var googleMap: GoogleMap? = null
    private var selectedLocationMarker: Marker? = null
    private var useImperialUnits = false
    private var firstLoad = true
    private lateinit var reviewList: List<ReviewLocationData>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ReviewFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.label.text = "Loading track data, please wait"

        setupMenu()

        lifecycleScope.launch {
            useImperialUnits = appSettings.useImperialUnits()

            reviewLocationAdapter = ReviewLocationAdapter(
                useImperialUnits,
                onMpClicked = ::showReviewMpDialog,
                onSelection = ::onLocationClicked
            )

            val layoutManager = LinearLayoutManager(requireContext())
            binding.locationsList.adapter = reviewLocationAdapter
            binding.locationsList.layoutManager = layoutManager

            savedInstanceState?.let {
                val selectedPosition = it.getInt(KEY_SELECTED_POSITION, RecyclerView.NO_POSITION)
                reviewLocationAdapter?.setSelectedPosition(selectedPosition)
            }

            val dividerItemDecoration = DividerItemDecoration(
                requireContext(),
                layoutManager.orientation
            )
            binding.locationsList.addItemDecoration(dividerItemDecoration)

            viewModel.track.observe(viewLifecycleOwner) { track ->
                logger.d(TAG, "@@ Review observe track: ${track?.id} ${track?.trackLabel}")

                if (track == null) {
                    binding.label.text = "No track found"
                } else {
                    binding.label.text = track.trackLabel?.ifBlank { "(Tap to Add Title)" }
                }
                binding.summary.text = track?.summary.toText(useImperialUnits)

                gpsTrackEntity = track
                reviewList = convert(track?.locationList)
                updateInterpolatedMp(track, reviewList)
                reviewLocationAdapter?.submitList(reviewList)
                updateTrackSummary(track)
                logger.d(TAG, "@@ is first load: $firstLoad")
                if (track != null && firstLoad) {
                    firstLoad = false
                    if (track.trackLabel.isNullOrEmpty()
                        && track.startLabel.isNullOrEmpty()
                        && track.endLabel.isNullOrEmpty()
                    ) {
                        showReviewTitleDialog(track)
                    }
                }
                lifecycleScope.launch {
                    loadMapTrack()
                    val pos = reviewLocationAdapter?.selectedPosition ?: RecyclerView.NO_POSITION
                    if (pos != RecyclerView.NO_POSITION) {
                        logger.d(TAG, "@@ RELOAD - Scroll to position: $pos")
                        binding.locationsList.scrollToPosition(pos)
                        reviewList.getOrNull(pos)?.let { item ->
                            onLocationClicked(item)
                        }
                    }
                }
            }

            viewModel.loadTrack(fragmentArgs.trackId)
        }

        binding.label.setOnClickListener {
            showReviewTitleDialog(gpsTrackEntity)
        }

        try {
            binding.mapView?.onCreate(savedInstanceState)
            binding.mapView?.getMapAsync { map ->
                googleMap = map
                googleMap?.let {
                    MapStyle.applyMapStyle(
                        requireContext(),
                        it,
                        logger
                    )
                }
                lifecycleScope.launch {
                    loadMapTrack()
                }
            }
        } catch (e: Exception) {
            googleMap = null
            logger.d(TAG, "@@ onCreate MapView failed", e)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        reviewLocationAdapter?.let { adapter ->
            logger.d(TAG, "@@ onSaveInstanceState: ${adapter.selectedPosition}")
            outState.putInt(KEY_SELECTED_POSITION, adapter.selectedPosition)
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.review_menu, menu)
                ApiHelper.Menu_setOptionalIconsVisible(menu, true)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
                logger.d(TAG, "@@ onPrepareMenu -- track entity enabled = ${gpsTrackEntity != null}")
                menu.findItem(R.id.action_playback).isEnabled = (gpsTrackEntity != null)
                menu.findItem(R.id.action_export_csv).isEnabled = (gpsTrackEntity != null)
                menu.findItem(R.id.action_export_json).isEnabled = (gpsTrackEntity != null)
                menu.findItem(R.id.action_delete).isEnabled = (gpsTrackEntity != null)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                logger.d(TAG, "@@ onMenuItemSelected: ${menuItem.itemId}")
                return when (menuItem.itemId) {
                    R.id.action_playback -> {
                        playbackTrack(gpsTrackEntity)
                        true
                    }

                    R.id.action_export_csv -> {
                        val exportFile =
                            importerExporter.createCsvExportFile(requireContext(), gpsTrackEntity)
                        exportFile?.let {
                            importerExporter.shareFile(requireContext(), it)
                        }
                        true
                    }

                    R.id.action_export_json -> {
                        val exportFile =
                            importerExporter.createJsonExportFile(requireContext(), gpsTrackEntity)
                        exportFile?.let {
                            importerExporter.shareFile(requireContext(), it)
                        }
                        true
                    }

                    R.id.action_delete -> {
                        askAndMaybeDeleteTrack(gpsTrackEntity)
                        true
                    }

                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun playbackTrack(track: GpsTrackEntity?) {
        if (track == null) return
        lifecycleScope.launch {
            logger.d(TAG, "@@ Set prefs Playback track: ${track.id}")
            appSettings.setPlaybackTrackId(track.id)
            val action = ReviewFragmentDirections.actionReviewToPlay()
            findNavController().navigate(action)
        }
    }

    private fun askAndMaybeDeleteTrack(track: GpsTrackEntity?) {
        if (track == null) return

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Track")
            .setMessage("Are you sure you want to delete this track? This action cannot be reversed.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    if (appSettings.playbackTrackId() == track.id) {
                        logger.d(TAG, "@@ Clear prefs Playback track: -1")
                        appSettings.setPlaybackTrackId(-1)
                    }
                    gpsTrackEntityDao.deleteById(track.id)
                    findNavController().popBackStack()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReviewTitleDialog(track: GpsTrackEntity?) {
        if (track == null) return

        val dialogView = layoutInflater.inflate(R.layout.review_title_dialog, null)
        val trackLabelEditText = dialogView.findViewById<TextInputEditText>(R.id.track_label_edittext)
        val startLabelEditText = dialogView.findViewById<TextInputEditText>(R.id.start_label_edittext)
        val endLabelEditText = dialogView.findViewById<TextInputEditText>(R.id.end_label_edittext)
        val mpStartValueEditText = dialogView.findViewById<TextInputEditText>(R.id.mp_start_value_edittext)
        val mpEndValueEditText = dialogView.findViewById<TextInputEditText>(R.id.mp_end_value_edittext)
        val clearMpStartButton = dialogView.findViewById<ImageButton>(R.id.clear_mp_start_button)
        val clearMpEndButton = dialogView.findViewById<ImageButton>(R.id.clear_mp_end_button)
        val trackLengthText = dialogView.findViewById<TextView>(R.id.track_length)

        val locations = track.locationList?.locationsList ?: emptyList<LocationData>()
        val locFirst = locations.firstOrNull()
        val locLast = locations.lastOrNull()

        val (startHint, endHint) = computeHintStartEndLabels(
            track.startLabel,
            track.endLabel,
            locFirst,
            locLast
        )

        trackLengthText.text = String.format("%.2f mi", Units.m_to_miles(computeTrackLengthMeters(track)))
        trackLabelEditText.setText(track.trackLabel)
        startLabelEditText.setText(startHint)
        endLabelEditText.setText(endHint)

        val mpStart : Float? = if (locFirst != null && locFirst.hasMp()) locFirst.mp else null
        val mpEnd : Float? = if (locations.size > 1 && locLast != null && locLast.hasMp()) locations.last().mp else null

        mpStartValueEditText.setText(mpStart?.toString())
        mpEndValueEditText.setText(mpEnd?.toString())

        mpStartValueEditText.isEnabled = locations.isNotEmpty()
        clearMpStartButton.isEnabled = locations.isNotEmpty()
        clearMpStartButton.setOnClickListener {
            mpStartValueEditText.text?.clear()
        }
        mpEndValueEditText.isEnabled = locations.size > 1
        clearMpEndButton.isEnabled = locations.size > 1
        clearMpEndButton.setOnClickListener {
            mpEndValueEditText.text?.clear()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Review Options")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val newLabel = trackLabelEditText.text.toString().trim()
                val newStartLabel = startLabelEditText.text.toString().trim()
                val newEndLabel = endLabelEditText.text.toString().trim()
                val newMpStart = mpStartValueEditText.text.toString().toFloatOrNull()
                val newMpEnd = mpEndValueEditText.text.toString().toFloatOrNull()

                logger.d(TAG, "@@Apply TITLE clicked:")
                logger.d(TAG, "@@  Label: $newLabel")
                logger.d(TAG, "@@  Start Label: $newStartLabel")
                logger.d(TAG, "@@  End Label: $newEndLabel")
                logger.d(TAG, "@@  MP Start Value: $newMpStart")
                logger.d(TAG, "@@  MP End Value: $newMpEnd")

                var changed = false
                var newGpsTrackEntity: GpsTrackEntity = track

                if (newLabel != track.trackLabel) {
                    changed = true
                    newGpsTrackEntity = newGpsTrackEntity.copy(trackLabel = newLabel)
                }
                if (newStartLabel != track.startLabel) {
                    changed = true
                    newGpsTrackEntity = newGpsTrackEntity.copy(startLabel = newStartLabel)
                }
                if (newEndLabel != track.endLabel) {
                    changed = true
                    newGpsTrackEntity = newGpsTrackEntity.copy(endLabel = newEndLabel)
                }
                if (track.locationList != null && (newMpStart != mpStart || newMpEnd != mpEnd)) {
                    changed = true
                    val listBuilder = track.locationList.toBuilder()
                    if (newMpStart != mpStart) {
                        val loc = track.locationList.getLocations(0).copy {
                            if (newMpStart != null) {
                                mp = newMpStart
                                flags = flags.setFlag(LocationFlags.USER_MP)
                            } else {
                                clearMp()
                                flags = flags.clearFlag(LocationFlags.USER_MP)
                            }
                        }
                        listBuilder.setLocations(0, loc)
                    }
                    if (newMpEnd != mpEnd) {
                        val idx = track.locationList.locationsCount - 1
                        val loc = track.locationList.getLocations(idx).copy {
                            if (newMpEnd != null) {
                                mp = newMpEnd
                                flags = flags.setFlag(LocationFlags.USER_MP)
                            } else {
                                clearMp()
                                flags = flags.clearFlag(LocationFlags.USER_MP)
                            }
                        }
                        listBuilder.setLocations(idx, loc)
                    }
                    newGpsTrackEntity = newGpsTrackEntity.copy(locationList = listBuilder.build())
                }
                if (changed) {
                    lifecycleScope.launch {
                        gpsTrackEntityDao.update(newGpsTrackEntity)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReviewMpDialog(item: ReviewLocationData) {
        val index = item.locIdx
        val track = gpsTrackEntity
        if (index < 0 || item.data == null || track == null) return
        val ld: LocationData = item.data

        val layoutInflater = LayoutInflater.from(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.review_mp_dialog, null)
        val mpValueEditText = dialogView.findViewById<TextInputEditText>(R.id.mp_value_edittext)
        val clearMpButton = dialogView.findViewById<ImageButton>(R.id.clear_mp_button)
        val ignoreCheckbox = dialogView.findViewById<CheckBox>(R.id.ignore_checkbox)
        val mpStart : Float? = if (ld.hasMp()) ld.mp else null
        val ignoreStart = ld.flags.hasFlag(LocationFlags.IGNORE)

        mpStart?.let {
            mpValueEditText.setText(it.toString())
        }

        clearMpButton.setOnClickListener {
            mpValueEditText.text?.clear()
        }

        ignoreCheckbox.isChecked = ignoreStart

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Milepost")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val mpNew = mpValueEditText.text.toString().toFloatOrNull()
                val ignoredNew = ignoreCheckbox.isChecked

                logger.d(TAG, "@@Apply MP clicked:")
                logger.d(TAG, "@@  Item: ${item.data?.latLong?.timestampMillis}")
                logger.d(TAG, "@@  MP Value: $mpNew")
                logger.d(TAG, "@@  Ignore: $ignoredNew")

                if (track.locationList != null
                    && (mpNew != mpStart
                        || ignoredNew != ignoreStart)
                    && index < track.locationList.locationsCount) {
                    val listBuilder = track.locationList.toBuilder()
                    val loc = track.locationList.getLocations(index).copy {
                        if (mpNew != null) {
                            mp = mpNew
                            flags = flags.setFlag(LocationFlags.USER_MP)
                        } else {
                            clearMp()
                            flags = flags.clearFlag(LocationFlags.USER_MP)
                        }
                        flags = flags.changeFlag(LocationFlags.IGNORE, ignoredNew)
                    }
                    listBuilder.setLocations(index, loc)
                    val newTrackEntity = track.copy(locationList = listBuilder.build())
                    lifecycleScope.launch {
                        gpsTrackEntityDao.update(newTrackEntity)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onLocationClicked(item: ReviewLocationData) {
        val locationData = item.data ?: return
        val map = googleMap ?: return

        val position = LatLng(locationData.latLong.latitude, locationData.latLong.longitude)

        if (selectedLocationMarker == null) {
            selectedLocationMarker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        } else {
            selectedLocationMarker!!.position = position
        }

        val update = CameraUpdateFactory.newLatLng(position)
        map.animateCamera(update)
    }

    fun loadMapTrack() {
        val map = googleMap ?: return
        val track = gpsTrackEntity ?: return

        try {
            map.clear()

            val locations = track.locationList?.locationsList ?: emptyList<LocationData>()
            if (locations.size == 1) {
                val location = locations[0]
                val movePos = LatLng(location.latLong.latitude, location.latLong.longitude)

                map.addMarker(MarkerOptions().position(movePos))
                val update = CameraUpdateFactory.newLatLngZoom(movePos, MAPS_ZOOM_LEVEL)
                logger.d(TAG, "@@ Camera move to: $movePos")
                map.moveCamera(update)

            } else if (locations.size > 1) {
                val width = resources.getDimensionPixelSize(R.dimen.map_track)
                val lineColor = requireContext().getColor(R.color.map_track)
                val polylineOptions = PolylineOptions()
                    .color(lineColor)
                    .width(width.toFloat())

                for (location in locations) {
                    polylineOptions.add(
                        LatLng(location.latLong.latitude, location.latLong.longitude)
                    )
                }
                map.addPolyline(polylineOptions)

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
                map.moveCamera(update)
            }
        } catch (e: Exception) {
            logger.d(TAG, "@@ loadMapTrack", e)
        }
    }

    fun updateTrackSummary(track: GpsTrackEntity?) {
        logger.d(TAG, "@@ updateTrackSummary")
        if (track == null) return

        val currentSummary = track.summary ?: GpsTrackSummary.getDefaultInstance()

        // Do nothing if all fields are set
        if (currentSummary.hasMpMin()
            && currentSummary.hasMpMax()
            && currentSummary.hasLengthM()
            && currentSummary.hasDurationS()) {
            return
        }

        val newSummary = computeTrackSummary(currentSummary, track)

        // Write it back to the DAO if summary has changed
        if (newSummary != currentSummary) {
            lifecycleScope.launch {
                logger.d(TAG, "@@ Summary Changed --> Update Summary from [${
                    currentSummary.toText(useImperialUnits)}] to [${
                    newSummary.toText(useImperialUnits)}]")
                gpsTrackEntityDao.updateSummary(track.id, newSummary)
            }
        }
    }

    fun updateInterpolatedMp(track: GpsTrackEntity?, locList: List<ReviewLocationData>) {
        logger.d(TAG, "@@ updateInterpolatedMp")
        if (track == null || track.locationList == null) return

        val newLocList = computeInterpolatedMp(locList)

        if (newLocList != null) {
            logger.d(TAG, "@@ Interpolated MP Changed")

            // Update all the interpolated MPs in the track entity
            val listBuilder = track.locationList.toBuilder()
            for (locData in newLocList) {
                val index = locData.locIdx
                val loc = track.locationList.getLocations(index).copy {
                    if (!flags.hasFlag(LocationFlags.USER_MP)) {
                        mp = locData.data!!.mp
                    }
                }
                listBuilder.setLocations(index, loc)
            }
            val newLocList = listBuilder.build()

            // Update the track summary to use the min/max new MPs
            val currentSummary = track.summary ?: GpsTrackSummary.getDefaultInstance()
            var mpMin: Float? = null
            var mpMax: Float? = null
            newLocList.locationsList.forEach { data ->
                if (data.hasMp()) {
                    val mp = data.mp
                    mpMin = mpMin?.coerceAtMost(mp) ?: mp
                    mpMax = mpMax?.coerceAtLeast(mp) ?: mp
                }
            }

            val newTrackEntity = track.copy(
                locationList = newLocList,
                summary = currentSummary.copy {
                    mpMin?.let { this.mpMin = it }
                    mpMax?.let { this.mpMax = it }
                }
            )
            lifecycleScope.launch {
                gpsTrackEntityDao.update(newTrackEntity)
            }
        }
    }
}

internal fun computeTrackLengthMeters(track: GpsTrackEntity): Float {
    track.summary?.let { summary ->
        if (summary.hasLengthM()) {
            return@computeTrackLengthMeters summary.lengthM
        }
    }

    val ld = LocationDistance()
    var meters = 0f

    var lastLoc: LocationData? = null
    track.locationList?.locationsList?.forEach { loc ->
        lastLoc?.let { last ->
            meters += ld.between(last.latLong, loc.latLong)
        }
        lastLoc = loc
    }

    return meters
}

internal fun computeHintStartEndLabels(
    startLabel: String?,
    endLabel: String?,
    locFirst: LocationData?,
    locLast: LocationData?
) : Pair<String, String> {
    val start = startLabel ?: ""
    val end = endLabel ?: ""

    // We will only guess a direction if:
    // - there are no labels already.
    // - there are at least 2 points and they are not the same
    if (start.isNotEmpty()
        || end.isNotEmpty()
        || locFirst == null
        || locLast == null
        || locFirst == locLast
    ) {
        return Pair(start, end)
    }

    val startPos = toLocation(locFirst.latLong.latitude, locFirst.latLong.longitude)
    val endPos = toLocation(locLast.latLong.latitude, locLast.latLong.longitude)
    var bearing = startPos.bearingTo(endPos)

    if (bearing < 0) {
        bearing += 360
    }

    return when (bearing) {
        in  45.0..<135.0 -> Pair("Westbound", "Eastbound")
        in 135.0..<225.0 -> Pair("Northbound", "Southbound")
        in 225.0..<315.0 -> Pair("Eastbound", "Westbound")
        else -> Pair("Southbound", "Northbound")
    }
}

private fun toLocation(latitude: Double, longitude: Double): Location {
    val location = Location("internal")
    location.latitude = latitude
    location.longitude = longitude
    return location
}

internal fun computeTrackSummary(currentSummary: GpsTrackSummary, track: GpsTrackEntity): GpsTrackSummary {
    var mpMin: Float? = if (currentSummary.hasMpMin()) currentSummary.mpMin else null
    var mpMax: Float? = if (currentSummary.hasMpMax()) currentSummary.mpMax else null
    var lengthM = 0f
    var durationS = if (currentSummary.hasDurationS()) currentSummary.durationS else 0f

    val locations = track.locationList?.locationsList ?: emptyList<LocationData>()

    if (locations.size > 1) {
        val first = locations.first()
        val last = locations.last()
        durationS = (last.latLong.timestampMillis - first.latLong.timestampMillis) / 1000f
    }

    var lastLatLong: LatLongData? = null
    val distResults = FloatArray(1)
    locations.forEach { data ->
        if (data.hasMp()) {
            val mp = data.mp
            mpMin = mpMin?.coerceAtMost(mp) ?: mp
            mpMax = mpMax?.coerceAtLeast(mp) ?: mp
        }

        val ll = data.latLong
        lastLatLong?.let {
            Location.distanceBetween(
                it.latitude, it.longitude,
                ll.latitude, ll.longitude,
                distResults
            )
            lengthM += distResults[0]
        }
        lastLatLong = ll
    }

    return gpsTrackSummary {
        mpMin?.let { this.mpMin = it }
        mpMax?.let { this.mpMax = it }
        this.lengthM = lengthM
        this.durationS = durationS
    }
}

internal fun convert(locationDataList: LocationDataList?): List<ReviewLocationData> {
    val result = mutableListOf<ReviewLocationData>()

    // First row is a little dummy hack for headers
    result.add(ReviewLocationData(
        locIdx = -1,
        data = null,
    ))

    if (locationDataList == null ) {
        return result
    }
    val locationsList = locationDataList.locationsList
    if (locationsList.isEmpty()) {
        return result
    }

    val firstTS = locationsList[0].latLong.timestampMillis
    val distResults = FloatArray(1)
    var distanceMeters = 0f
    var lastLatLong: LatLongData? = null
    for ((index, location) in locationsList.withIndex()) {
        val newLatLong = location.latLong
        val deltaTS = newLatLong.timestampMillis - firstTS

        lastLatLong?.let {
            Location.distanceBetween(
                it.latitude, it.longitude,
                newLatLong.latitude, newLatLong.longitude,
                distResults)
            distanceMeters += distResults[0]
        }

        result.add(ReviewLocationData(
            locIdx = index,
            data = location,
            deltaTS = deltaTS,
            distanceMiles = Units.m_to_miles(distanceMeters),
        ))

        lastLatLong = newLatLong
    }

    return result
}

internal fun computeInterpolatedMp(locList: List<ReviewLocationData>) : List<ReviewLocationData>? {
    // Keep only the valid entries (not header, with actual data, not ignore)
    val mutLocList = locList.filter {
        it.locIdx >= 0
        && it.data != null
        && !it.data.flags.hasFlag(LocationFlags.IGNORE)
    }.toMutableList()

    var changed = false
    val count = mutLocList.size
    if (count < 2) return null

    // First create a list of all the known user-mp entries, excluding ignored locations.
    val knownMpList = mutLocList.filter {
        it.data != null
        && it.data.hasMp()
        && it.data.flags.hasFlag(LocationFlags.USER_MP)
    }.toMutableList()

    val firstLoc = mutLocList.first()
    val lastLoc = mutLocList.last()
    if (knownMpList.isEmpty()) {
        // If there are no user-mp, simply fill with known distances.
        val oldFirstMp = firstLoc.data!!.mp
        val oldLastMp = lastLoc.data!!.mp
        knownMpList.add(setInterpolatedMp(0, mutLocList, firstLoc.distanceMiles))
        knownMpList.add(setInterpolatedMp(count - 1, mutLocList, lastLoc.distanceMiles))
        changed = (oldFirstMp != mutLocList.first().data!!.mp)
                || (oldLastMp != mutLocList.last().data!!.mp)
    } else {
        // Recompute index 0 if missing
        val firstUserMp = knownMpList.first()
        if (firstUserMp.locIdx != firstLoc.locIdx) {
            val oldMp = firstLoc.data!!.mp
            val newMp = firstUserMp.data!!.mp - firstUserMp.distanceMiles
            knownMpList.add(0, setInterpolatedMp(0, mutLocList, newMp))
            changed = changed || oldMp != newMp
        }

        // Compute last index if missing
        val lastUserMp = knownMpList.last()
        if (lastUserMp.locIdx != mutLocList.last().locIdx) {
            val oldMp = lastLoc.data!!.mp
            val distance = lastLoc.distanceMiles - lastUserMp.distanceMiles
            val newMp = lastUserMp.data!!.mp + distance
            knownMpList.add(setInterpolatedMp(count - 1, mutLocList, newMp))
            changed = changed || oldMp != newMp
        }
    }

    // Now fill in-place
    var nextKnownIndex = 0
    var nextKnownMp = knownMpList[0]
    var prevKnownMp = nextKnownMp
    var distanceMp = 0.0f
    var distanceMile = 0.0f
    for ((index, locData) in mutLocList.withIndex()) {
        if (locData.locIdx == nextKnownMp.locIdx) {
            prevKnownMp = nextKnownMp
            nextKnownIndex++
            if (nextKnownIndex < knownMpList.size) {
                nextKnownMp = knownMpList[nextKnownIndex]
            }
            distanceMp = nextKnownMp.data!!.mp - prevKnownMp.data!!.mp
            distanceMile = nextKnownMp.distanceMiles - prevKnownMp.distanceMiles
            continue
        }

        if (distanceMile != 0f) {
            val oldMp = locData.data!!.mp
            val ratio = (locData.distanceMiles - prevKnownMp.distanceMiles) / distanceMile
            val newMp = prevKnownMp.data!!.mp + ratio * distanceMp
            if (oldMp != newMp) {
                setInterpolatedMp(index, mutLocList, newMp)
                changed = changed || oldMp != newMp
            }
        }
    }

    return if (changed) mutLocList else null
}

internal fun setInterpolatedMp(index: Int, list: MutableList<ReviewLocationData>, mp: Float): ReviewLocationData {
    val data = list[index].data
    if (data != null) {
        list[index] = list[index].copy(data = data.copy {
            this.mp = mp
        })
    }
    return list[index]
}
