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
import com.alflabs.mpgpstrack.databinding.ReviewFragmentBinding
import com.alflabs.mpgpstrack.location.LatLongData
import com.alflabs.mpgpstrack.location.LocationDataList
import com.alflabs.mpgpstrack.tracks.TrackEntity
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ReviewFragment : Fragment() {
    private val TAG = ReviewFragment::class.java.simpleName

    private var _binding: ReviewFragmentBinding? = null
    private val binding get() = _binding!!
    private val fragmentArgs: ReviewFragmentArgs by navArgs()
    private val viewModel: ReviewViewModel by viewModels()
    private var trackEntity: TrackEntity? = null
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
        }

        viewModel.loadTrack(fragmentArgs.trackId)

        binding.exportButton.isEnabled = trackEntity != null
        binding.exportButton.setOnClickListener {
            val exportFile = Exporter.createCsvExportFile(requireContext(), trackEntity)
            exportFile?.let {
                Exporter.shareFile(requireContext(), it)
;            }
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
}
