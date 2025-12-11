package com.alflabs.mpgpstrack.fragments.review

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alflabs.mpgpstrack.R
import com.alflabs.mpgpstrack.databinding.ReviewFragmentBinding
import com.alflabs.mpgpstrack.location.LatLongData
import com.alflabs.mpgpstrack.location.LocationData
import com.alflabs.mpgpstrack.location.LocationDataList
import com.alflabs.mpgpstrack.tracks.TrackEntity
import com.alflabs.mpgpstrack.utils.MapStyle
import com.alflabs.utils.ILogger
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ReviewFragment : Fragment() {
    companion object {
        private val TAG = ReviewFragment::class.java.simpleName
        private const val MAPS_ZOOM_LEVEL = 15f
    }

    @Inject lateinit var logger: ILogger

    private var _binding: ReviewFragmentBinding? = null
    private val binding get() = _binding!!
    private val fragmentArgs: ReviewFragmentArgs by navArgs()
    private val viewModel: ReviewViewModel by viewModels()
    private var trackEntity: TrackEntity? = null
    private var googleMap: GoogleMap? = null
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

        val adapter = ReviewLocationAdapter()
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
            binding.summary.text = track?.summary?.toString() ?: "No summary found"

            trackEntity = track
            reviewList = convert(track?.locationList)
            adapter.submitList(reviewList)
            binding.exportButton.isEnabled = trackEntity != null
            loadMapTrack()
        }

        viewModel.loadTrack(fragmentArgs.trackId)

        binding.exportButton.isEnabled = trackEntity != null
        binding.exportButton.setOnClickListener {
            val exportFile = Exporter.createCsvExportFile(requireContext(), trackEntity)
            exportFile?.let {
                Exporter.shareFile(requireContext(), it)
            }
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

}
