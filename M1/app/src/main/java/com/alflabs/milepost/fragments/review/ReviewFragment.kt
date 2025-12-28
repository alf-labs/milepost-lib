package com.alflabs.milepost.fragments.review

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.ReviewFragmentBinding
import com.alflabs.milepost.fragments.tracks.toText
import com.alflabs.milepost.location.LatLongData
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.location.LocationDataList
import com.alflabs.milepost.location.LocationFlags
import com.alflabs.milepost.location.changeFlag
import com.alflabs.milepost.location.clearFlag
import com.alflabs.milepost.location.copy
import com.alflabs.milepost.location.hasFlag
import com.alflabs.milepost.location.setFlag
import com.alflabs.milepost.main.AppSettings
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.milepost.tracks.TrackEntityDao
import com.alflabs.milepost.tracks.TrackSummary
import com.alflabs.milepost.tracks.trackSummary
import com.alflabs.milepost.utils.ImporterExporter
import com.alflabs.milepost.utils.MapStyle
import com.alflabs.milepost.utils.Units
import com.alflabs.utils.ILogger
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
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
    }

    @Inject lateinit var logger: ILogger
    @Inject lateinit var importerExporter: ImporterExporter
    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var trackEntityDao: TrackEntityDao

    private var _binding: ReviewFragmentBinding? = null
    private val binding get() = _binding!!
    private val fragmentArgs: ReviewFragmentArgs by navArgs()
    private val viewModel: ReviewViewModel by viewModels()
    private var trackEntity: TrackEntity? = null
    private var googleMap: GoogleMap? = null
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

        useImperialUnits = appSettings.useImperialUnits(lifecycleScope)

        val adapter = ReviewLocationAdapter(useImperialUnits) {
            showReviewMpDialog(it)
        }
        val layoutManager = LinearLayoutManager(requireContext())
        binding.locationsList.adapter = adapter
        binding.locationsList.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            requireContext(),
            layoutManager.orientation
        )
        binding.locationsList.addItemDecoration(dividerItemDecoration)

        viewModel.track.observe(viewLifecycleOwner) { track ->
            logger.d(TAG, "@@ Review observe track: ${track?.id} ${track?.label}")

            if (track == null) {
                binding.label.text = "No track found"
            } else {
                binding.label.text = track.label?.ifBlank { "(Tap to Add Title)" }
            }
            binding.summary.text = track?.summary.toText(useImperialUnits)

            trackEntity = track
            reviewList = convert(track?.locationList)
            updateInterpolatedMp(track, reviewList)
            adapter.submitList(reviewList)
            binding.exportCsvButton.isEnabled = trackEntity != null
            binding.exportJsonButton.isEnabled = trackEntity != null
            loadMapTrack()
            updateTrackSummary(track)
            logger.d(TAG, "@@ is first load: $firstLoad")
            if (track != null && firstLoad) {
                firstLoad = false
                if (track.label.isNullOrEmpty()) {
                    showReviewTitleDialog(track)
                }
            }
        }

        viewModel.loadTrack(fragmentArgs.trackId)

        binding.exportCsvButton.isEnabled = trackEntity != null
        binding.exportCsvButton.setOnClickListener {
            val exportFile = importerExporter.createCsvExportFile(requireContext(), trackEntity)
            exportFile?.let {
                importerExporter.shareFile(requireContext(), it)
            }
        }
        binding.exportJsonButton.isEnabled = trackEntity != null
        binding.exportJsonButton.setOnClickListener {
            val exportFile = importerExporter.createJsonExportFile(requireContext(), trackEntity)
            exportFile?.let {
                importerExporter.shareFile(requireContext(), it)
            }
        }
        binding.label.setOnClickListener {
            showReviewTitleDialog(trackEntity)
        }

        binding.mapView?.onCreate(savedInstanceState)
        binding.mapView?.getMapAsync { map ->
            googleMap = map
            googleMap?.let { MapStyle.applyMapStyle(
                requireContext(),
                it,
                logger) }
            loadMapTrack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showReviewTitleDialog(track: TrackEntity?) {
        if (track == null) return

        val dialogView = layoutInflater.inflate(R.layout.review_title_dialog, null)
        val labelEditText = dialogView.findViewById<TextInputEditText>(R.id.label_edittext)
        val mpStartValueEditText = dialogView.findViewById<TextInputEditText>(R.id.mp_start_value_edittext)
        val mpEndValueEditText = dialogView.findViewById<TextInputEditText>(R.id.mp_end_value_edittext)
        val clearMpStartButton = dialogView.findViewById<ImageButton>(R.id.clear_mp_start_button)
        val clearMpEndButton = dialogView.findViewById<ImageButton>(R.id.clear_mp_end_button)

        labelEditText.setText(track.label)

        val locations = track.locationList?.locationsList ?: emptyList<LocationData>()
        val mpStart : Float? = if (locations.isNotEmpty()) locations.first().mp else null
        val mpEnd : Float? = if (locations.size > 1) locations.last().mp else null

        mpStart?.let { mpStartValueEditText.setText(it.toString()) }
        mpEnd?.let { mpEndValueEditText.setText(it.toString()) }

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
                val newLabel = labelEditText.text.toString().trim()
                val newMpStart = mpStartValueEditText.text.toString().toFloatOrNull()
                val newMpEnd = mpEndValueEditText.text.toString().toFloatOrNull()

                logger.d(TAG, "@@Apply TITLE clicked:")
                logger.d(TAG, "@@  Label: $newLabel")
                logger.d(TAG, "@@  MP Start Value: $newMpStart")
                logger.d(TAG, "@@  MP End Value: $newMpEnd")

                var changed = false
                var newTrackEntity: TrackEntity = track

                if (newLabel != track.label) {
                    changed = true
                    newTrackEntity = newTrackEntity.copy(label = newLabel)
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
                    newTrackEntity = newTrackEntity.copy(locationList = listBuilder.build())
                }
                if (changed) {
                    lifecycleScope.launch {
                        trackEntityDao.update(newTrackEntity)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReviewMpDialog(item: ReviewLocationData) {
        val index = item.index
        val track = trackEntity
        if (index < 0 || item.data == null || track == null) return
        val ld: LocationData = item.data

        val layoutInflater = LayoutInflater.from(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.review_mp_dialog, null)
        val mpValueEditText = dialogView.findViewById<TextInputEditText>(R.id.mp_value_edittext)
        val clearMpButton = dialogView.findViewById<ImageButton>(R.id.clear_mp_button)
        val ignoreCheckbox = dialogView.findViewById<CheckBox>(R.id.ignore_checkbox)
        val streamlineCheckbox = dialogView.findViewById<CheckBox>(R.id.streamline_checkbox)
        val mpStart : Float? = if (ld.hasMp()) ld.mp else null
        val ignoreStart = ld.flags.hasFlag(LocationFlags.IGNORE)
        val streamlineStart = ld.flags.hasFlag(LocationFlags.STREAMLINE)

        mpStart?.let {
            mpValueEditText.setText(it.toString())
        }

        clearMpButton.setOnClickListener {
            mpValueEditText.text?.clear()
        }

        ignoreCheckbox.isChecked = ignoreStart
        streamlineCheckbox.isChecked = streamlineStart

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Milepost")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val mpNew = mpValueEditText.text.toString().toFloatOrNull()
                val ignoredNew = ignoreCheckbox.isChecked
                val streamlinedNew = streamlineCheckbox.isChecked

                logger.d(TAG, "@@Apply MP clicked:")
                logger.d(TAG, "@@  Item: ${item.data?.latLong?.timestampMillis}")
                logger.d(TAG, "@@  MP Value: $mpNew")
                logger.d(TAG, "@@  Ignore: $ignoredNew")
                logger.d(TAG, "@@  Streamline: $streamlinedNew")

                if (track.locationList != null
                    && (mpNew != mpStart
                        || ignoredNew != ignoreStart
                        || streamlinedNew != streamlineStart)
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
                        flags = flags.changeFlag(LocationFlags.STREAMLINE, streamlinedNew)
                    }
                    listBuilder.setLocations(index, loc)
                    val newTrackEntity = track.copy(locationList = listBuilder.build())
                    lifecycleScope.launch {
                        trackEntityDao.update(newTrackEntity)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun loadMapTrack() {
        val map = googleMap ?: return
        val track = trackEntity ?: return

        map.clear()

        var movePos: LatLng? = null
        var startPos: LatLng? = null
        var endPos: LatLng? = null
        val locations = track.locationList?.locationsList ?: emptyList<LocationData>()
        if (locations.size == 1) {
            val location = locations[0]
            movePos = LatLng(location.latLong.latitude, location.latLong.longitude)
            startPos = movePos
        } else if (locations.size > 1) {
            val lineColor = requireContext().getColor(R.color.colorPrimaryDark)
            val polylineOptions = PolylineOptions()
                .color(lineColor)
                .width(10f)

            for (location in locations) {
                polylineOptions.add(
                    LatLng(location.latLong.latitude, location.latLong.longitude)
                )
            }
            map.addPolyline(polylineOptions)

            val location0 = locations.first()
            val location1 = locations[locations.size / 2]
            val location2 = locations.last()
            startPos = LatLng(location0.latLong.latitude, location0.latLong.longitude)
            movePos = LatLng(location1.latLong.latitude, location1.latLong.longitude)
            endPos = LatLng(location2.latLong.latitude, location2.latLong.longitude)
        }

        val markerOptions = MarkerOptions()
        startPos?.let { map.addMarker(markerOptions.position(it)) }
        endPos?.let { map.addMarker(markerOptions.position(it)) }

        movePos?.let {
            val update = CameraUpdateFactory.newLatLngZoom(it, MAPS_ZOOM_LEVEL)
            map.moveCamera(update)
        }
    }

    fun updateTrackSummary(track: TrackEntity?) {
        logger.d(TAG, "@@ updateTrackSummary")
        if (track == null) return

        val currentSummary = track.summary ?: TrackSummary.getDefaultInstance()

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
                trackEntityDao.updateSummary(track.id, newSummary)
            }
        }
    }

    fun updateInterpolatedMp(track: TrackEntity?, locList: List<ReviewLocationData>) {
        logger.d(TAG, "@@ updateInterpolatedMp")
        if (track == null || track.locationList == null) return

        val newLocList = computeInterpolatedMp(locList)

        if (newLocList != null) {
            logger.d(TAG, "@@ Interpolated MP Changed")

            val listBuilder = track.locationList.toBuilder()
            for (locData in newLocList) {
                val index = locData.index
                val loc = track.locationList.getLocations(index).copy {
                    if (!flags.hasFlag(LocationFlags.USER_MP)) {
                        mp = locData.data!!.mp
                    }
                }
                listBuilder.setLocations(index, loc)
            }

            val newTrackEntity = track.copy(locationList = listBuilder.build())
            lifecycleScope.launch {
                trackEntityDao.update(newTrackEntity)
            }
        }
    }
}

internal fun computeTrackSummary(currentSummary: TrackSummary, track: TrackEntity): TrackSummary {
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

    return trackSummary {
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
        index = -1,
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
            index = index,
            data = location,
            deltaTS = deltaTS,
            distanceMeters = distanceMeters,
            distanceMiles = Units.m_to_miles(distanceMeters),
        ))

        lastLatLong = newLatLong
    }

    return result
}

internal fun computeInterpolatedMp(locList: List<ReviewLocationData>) : List<ReviewLocationData>? {
    // Keep only the valid entries (not header, with actual data)
    val mutLocList = locList.filter { it.index >= 0 && it.data != null }.toMutableList()

    var changed = false
    val count = mutLocList.size
    if (count < 2) return null

    // First create a list of all the known user-mp entries.
    val knownMpList = mutLocList.filter {
        it.data != null && it.data.hasMp() && it.data.flags.hasFlag(LocationFlags.USER_MP)
    }.toMutableList()

    if (knownMpList.isEmpty()) {
        // If there are no user-mp, simply fill with known distances.
        val firstLoc = mutLocList.first()
        val lastLoc = mutLocList.last()
        knownMpList.add(setInterpolatedMp(0, mutLocList, firstLoc.distanceMiles))
        knownMpList.add(setInterpolatedMp(count - 1, mutLocList, lastLoc.distanceMiles))
        changed = true
    } else {
        // Recompute index 0 if missing
        val firstUserMp = knownMpList.first()
        if (firstUserMp.index != mutLocList.first().index) {
            knownMpList.add(0, setInterpolatedMp(0, mutLocList, -firstUserMp.distanceMiles))
            changed = true
        }

        // Compute last index if missing
        val lastUserMp = knownMpList.last()
        if (lastUserMp.index != mutLocList.last().index) {
            val lastLoc = mutLocList.last()
            val distance = lastLoc.distanceMiles - lastUserMp.distanceMiles
            knownMpList.add(setInterpolatedMp(count - 1, mutLocList, lastUserMp.data!!.mp + distance))
            changed = true
        }
    }

    // Now fill in-place
    var nextKnownIndex = 0
    var nextKnownMp = knownMpList[0]
    var prevKnownMp = nextKnownMp
    var distance = 0.0f
    for ((index, locData) in mutLocList.withIndex()) {
        if (locData.index == nextKnownMp.index) {
            prevKnownMp = nextKnownMp
            nextKnownIndex++
            if (nextKnownIndex < count) {
                nextKnownMp = knownMpList[nextKnownIndex]
            }
            distance = nextKnownMp.data!!.mp - prevKnownMp.data!!.mp
        }

        if (distance > 0) {
            val mp = prevKnownMp.data!!.mp + (locData.data!!.mp - prevKnownMp.data.mp) / distance
            if (locData.data.mp != mp) {
                setInterpolatedMp(index, mutLocList, mp)
                changed = true
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
