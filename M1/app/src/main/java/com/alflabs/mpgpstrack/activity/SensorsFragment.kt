package com.alflabs.mpgpstrack.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.alflabs.mpgpstrack.app.LocationService
import com.alflabs.mpgpstrack.dagger.CurrentLocation
import com.alflabs.mpgpstrack.databinding.SensorsFragmentBinding
import com.alflabs.rx.AndroidSchedulers
import com.alflabs.rx.ISubscriber
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@AndroidEntryPoint
class SensorsFragment : Fragment() {
    private val TAG = SensorsFragment::class.java.simpleName

    @Inject lateinit var currentLocation: CurrentLocation

    private var _binding: SensorsFragmentBinding? = null

    private val locationSubscriber: ISubscriber<in String?> =
        ISubscriber<String?> { _, event -> onLocationChanged(event) }

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        currentLocation.start()
        _binding = SensorsFragmentBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

//        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }

        binding.startButton.setOnClickListener {
            Log.d(TAG, "On click START button")
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            } else {
                requireActivity().startForegroundService(Intent(requireContext(), LocationService::class.java))
            }
        }

        binding.stopButton.setOnClickListener {
            Log.d(TAG, "On click STOP button")
            requireActivity().stopService(Intent(requireContext(), LocationService::class.java))
        }
    }

    fun onLocationChanged(value: String?) {
        if (value == null) return
        Log.d(TAG, "onLocationChanged: $value")
        binding.locationText.text = value
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        currentLocation.location.subscribe(AndroidSchedulers.mainThread(), locationSubscriber)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        currentLocation.location.remove(locationSubscriber)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult: $requestCode, $permissions, $grantResults")
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requireActivity().startForegroundService(Intent(requireContext(), LocationService::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }
}
