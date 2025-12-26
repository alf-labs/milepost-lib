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
import com.alflabs.milepost.main.AppSettings
import com.alflabs.milepost.tracks.TrackEntity
import com.alflabs.milepost.tracks.TrackEntityDao
import com.alflabs.milepost.tracks.TrackSummary
import com.alflabs.milepost.tracks.trackSummary
import com.alflabs.milepost.utils.ImporterExporter
import com.alflabs.milepost.utils.MapStyle
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

        val adapter = ReviewLocationAdapter(useImperialUnits)
        val layoutManager = LinearLayoutManager(requireContext())
        binding.locationsList.adapter = adapter
        binding.locationsList.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            requireContext(),
            layoutManager.orientation
        )
        binding.locationsList.addItemDecoration(dividerItemDecoration)

        viewModel.track.observe(viewLifecycleOwner) { track ->
            Log.d(TAG, "Review observe track: ${track?.id} ${track?.label}")

            binding.label.text = track?.label ?: "No track found"
            binding.summary.text = track?.summary.toText(useImperialUnits)

            trackEntity = track
            reviewList = convert(track?.locationList)
            adapter.submitList(reviewList)
            binding.exportCsvButton.isEnabled = trackEntity != null
            binding.exportJsonButton.isEnabled = trackEntity != null
            loadMapTrack()
            updateTrackSummary(track)
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
            showReviewOptionsDialog()
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

    private fun showReviewOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_review_options, null)
        val labelEditText = dialogView.findViewById<TextInputEditText>(R.id.label_edittext)
        val mpStartValueEditText = dialogView.findViewById<TextInputEditText>(R.id.mp_start_value_edittext)
        val mpEndValueEditText = dialogView.findViewById<TextInputEditText>(R.id.mp_end_value_edittext)
        val clearMpStartButton = dialogView.findViewById<ImageButton>(R.id.clear_mp_start_button)
        val clearMpEndButton = dialogView.findViewById<ImageButton>(R.id.clear_mp_end_button)
        val ignoreCheckbox = dialogView.findViewById<CheckBox>(R.id.ignore_checkbox)
        val streamlineCheckbox = dialogView.findViewById<CheckBox>(R.id.streamline_checkbox)

        labelEditText.setText(trackEntity?.label)

        clearMpStartButton.setOnClickListener {
            mpStartValueEditText.text?.clear()
        }

        clearMpEndButton.setOnClickListener {
            mpEndValueEditText.text?.clear()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Review Options")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val label = labelEditText.text.toString()
                val mpValue = mpStartValueEditText.text.toString().toFloatOrNull()
                val mpEndValue = mpEndValueEditText.text.toString().toFloatOrNull()
                val isIgnored = ignoreCheckbox.isChecked
                val isStreamlined = streamlineCheckbox.isChecked

                logger.d(TAG, "@@Apply button clicked:")
                logger.d(TAG, "@@  Label: $label")
                logger.d(TAG, "@@  MP Value: $mpValue")
                logger.d(TAG, "@@  MP End Value: $mpEndValue")
                logger.d(TAG, "@@  Ignore: $isIgnored")
                logger.d(TAG, "@@  Streamline: $isStreamlined")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun convert(locationDataList: LocationDataList?): List<ReviewLocationData> {
        val result = mutableListOf<ReviewLocationData>()
        if (locationDataList == null ) {
            return result
        }
        val locationsList = locationDataList.locationsList
        if (locationsList.isEmpty()) {
            return result
        }

        // First row is a little dummy hack for headers
        result.add(ReviewLocationData(
            data = null
        ))

        val firstTS = locationsList[0].latLong.timestampMillis
        val distResults = FloatArray(1)
        var distanceMeters = 0f
        var lastLatLong: LatLongData? = null
        for (location in locationsList) {
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
                data = location,
                deltaTS = deltaTS,
                distanceMeters = distanceMeters
            ))

            lastLatLong = newLatLong
        }

        return result
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
        if (track == null) return

        val currentSummary = track.summary ?: TrackSummary.getDefaultInstance()

        // Do nothing if all fields are set
        if (currentSummary.hasMpMin()
            && currentSummary.hasMpMax()
            && currentSummary.hasLengthM()
            && currentSummary.hasDurationS()) {
            return
        }

        var mpMin : Float? = if (currentSummary.hasMpMin()) currentSummary.mpMin else null
        var mpMax : Float? = if (currentSummary.hasMpMax()) currentSummary.mpMax else null
        var lengthM = 0f
        var durationS = if (currentSummary.hasDurationS()) currentSummary.durationS else 0f

        val locations = track.locationList?.locationsList ?: emptyList<LocationData>()

        if (locations.size > 1) {
            val first = locations.first()
            val last = locations.last()
            durationS = (last.latLong.timestampMillis - first.latLong.timestampMillis)/1000f
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
                    distResults)
                lengthM += distResults[0]
            }
            lastLatLong = ll
        }

        val newSummary = trackSummary {
            mpMin?.let { this.mpMin = it }
            mpMax?.let { this.mpMax = it }
            this.lengthM = lengthM
            this.durationS = durationS
        }

        // Write it back to the DAO if summary has changed
        if (newSummary != currentSummary) {
            lifecycleScope.launch {
                logger.d(TAG, "@@ Update Summary from [${
                    currentSummary.toText(useImperialUnits)}] to [${
                    newSummary.toText(useImperialUnits)}]")
                trackEntityDao.updateSummary(track.id, newSummary)
            }
        }
    }

}
