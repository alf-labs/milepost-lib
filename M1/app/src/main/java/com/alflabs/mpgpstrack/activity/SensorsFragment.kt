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
import com.alflabs.mpgpstrack.dagger.CurrentLocationData
import com.alflabs.mpgpstrack.databinding.SensorsFragmentBinding
import com.alflabs.rx.AndroidSchedulers
import com.alflabs.rx.ISubscriber
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@AndroidEntryPoint
class SensorsFragment : Fragment() {
    private val TAG = SensorsFragment::class.java.simpleName

    @Inject lateinit var currentLocation: CurrentLocation

    private var _binding: SensorsFragmentBinding? = null

    private val dateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    // TBD: change to DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")

    private val locationSubscriber: ISubscriber<in CurrentLocationData?> =
        ISubscriber<CurrentLocationData?> { _, event -> onLocationChanged(event) }

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

    fun onLocationChanged(value: CurrentLocationData?) {
        if (value == null) return
        Log.d(TAG, "onLocationChanged: $value")


        val latLong = value.latLong
        if (latLong != null) {
            binding.locationText.text = String.format(
                Locale.US,
                "%.5f, %.5f",   // matches precision of google maps display
                latLong.lat, latLong.longi)

            val instant = Instant.ofEpochMilli(latLong.ts)
            binding.statusText.text = dateTimeFormatter.format(instant)
        } else {
            binding.statusText.text = value.status
        }

        val speedBearing = value.speedBearing
        if (speedBearing != null) {
            val mph = speedBearing.speedMps / 1609.34 * 3600
            val acc_mph = speedBearing.speedAcc / 1609.34 * 3600
            binding.speedText.text = String.format(Locale.US,
                "%.1f MPH +/- %.1f",
                mph,
                acc_mph)

            binding.bearingText.text = String.format(Locale.US,
                "%.1f° +/- %.1f°",
                speedBearing.bearingDeg,
                speedBearing.bearingAcc)
        }
    }
}
