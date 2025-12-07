package com.alflabs.mpgpstrack.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
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
        binding.locationsList.adapter = adapter

        viewModel.track.observe(viewLifecycleOwner) { track ->
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
