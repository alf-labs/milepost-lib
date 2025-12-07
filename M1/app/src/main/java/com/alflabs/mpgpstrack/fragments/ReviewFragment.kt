package com.alflabs.mpgpstrack.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.alflabs.mpgpstrack.databinding.PlaceholderFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class ReviewFragment : Fragment() {
    private val TAG = ReviewFragment::class.java.simpleName

    private var _binding: PlaceholderFragmentBinding? = null
    private val binding get() = _binding!!
    private val fragmentArgs: ReviewFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlaceholderFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
