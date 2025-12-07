package com.alflabs.mpgpstrack.fragments.review

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
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ReviewFragment : Fragment() {
    private val TAG = ReviewFragment::class.java.simpleName

    private var _binding: ReviewFragmentBinding? = null
    private val binding get() = _binding!!
    private val fragmentArgs: ReviewFragmentArgs by navArgs()
    private val viewModel: ReviewViewModel by viewModels()

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

            track?.locationList?.let {
                adapter.submitList(it.locationsList)
            }
        }

        viewModel.loadTrack(fragmentArgs.trackId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
