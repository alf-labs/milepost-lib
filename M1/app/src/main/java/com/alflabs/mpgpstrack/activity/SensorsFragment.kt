package com.alflabs.mpgpstrack.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alflabs.mpgpstrack.R
import com.alflabs.mpgpstrack.app.LocationService
import com.alflabs.mpgpstrack.app.GlobalState
import com.alflabs.mpgpstrack.app.LocationData
import com.alflabs.mpgpstrack.app.StatusData
import com.alflabs.mpgpstrack.databinding.SensorsFragmentBinding
import com.alflabs.rx.AndroidSchedulers
import com.alflabs.rx.ISubscriber
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject



@AndroidEntryPoint
class SensorsFragment : Fragment() {
    private val TAG = SensorsFragment::class.java.simpleName

    @Inject lateinit var globalState: GlobalState

    companion object {
        private const val PERM_REQUEST_CODE = 1
        private const val MAPS_ZOOM_LEVEL = 15f
        private const val SECONDS_PER_HOUR = 3600
        private const val METERS_PER_MILE = 1609.344
        private const val MPS_TO_MPH = SECONDS_PER_HOUR / METERS_PER_MILE
    }

    private var _binding: SensorsFragmentBinding? = null
    private val dateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    // TBD: change to DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")

    private val locationSubscriber: ISubscriber<in LocationData?> =
        ISubscriber<LocationData?> { _, event -> onLocationChanged(event) }
    private val statusSubscriber: ISubscriber<in StatusData?> =
        ISubscriber<StatusData?> { _, event -> onStatusChanged(event) }

    private var setInitialMapZoom = true
    private var googleMap: GoogleMap? = null
    private var accuracyColorMissing: Int = 0
    private var accuracyColorPresent: Int = 0

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = SensorsFragmentBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        accuracyColorMissing = requireContext().getColor(R.color.accMissing)
        accuracyColorPresent = requireContext().getColor(R.color.accPresent)

        binding.startButton.setOnClickListener {
            Log.d(TAG, "On click START button")

            if (!PermissionsFragment.allPermissionsGranted(requireContext())) {
                Log.d(TAG, "START --> request permissions")
                val action = SensorsFragmentDirections.actionSensorsToPermissionsDlg()
                findNavController().navigate(action)
            } else {
                setInitialMapZoom = true
                requireActivity().startForegroundService(Intent(requireContext(), LocationService::class.java))
            }
        }

        binding.stopButton.setOnClickListener {
            Log.d(TAG, "On click STOP button")
            requireActivity().stopService(Intent(requireContext(), LocationService::class.java))
        }

        updateButtons(globalState.status)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            googleMap = map
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        binding.mapView.onResume()
        globalState.statusStream.subscribe(AndroidSchedulers.mainThread(), statusSubscriber)
        globalState.locationStream.subscribe(AndroidSchedulers.mainThread(), locationSubscriber)
        updateButtons(globalState.status)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        binding.mapView.onPause()
        globalState.locationStream.remove(locationSubscriber)
        globalState.statusStream.remove(statusSubscriber)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        binding.mapView.onDestroy()
        _binding = null
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult: $requestCode, $permissions, $grantResults")
        if (requestCode == PERM_REQUEST_CODE
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onRequestPermissionsResult --> GRANTED")

            requireActivity()
                .startForegroundService(Intent(requireContext(), LocationService::class.java))
        } else {
            Log.d(TAG, "onRequestPermissionsResult --> NOT granted")
        }
    }

    fun accuracyColor(value: Float?) : Int =
        if (value == null) accuracyColorMissing else accuracyColorPresent

    private fun onStatusChanged(state: StatusData?) {
        Log.d(TAG, "onStatusChanged: $state")
        if (state == null) return
        binding.statusText.text = state.status
        updateButtons(state)
    }

    private fun updateButtons(state: StatusData) {
        binding.startButton.isEnabled = state == StatusData.STOPPED
        binding.stopButton.isEnabled = state == StatusData.SENSOR
    }

    fun onLocationChanged(value: LocationData?) {
        Log.d(TAG, "onLocationChanged: $value")
        updateButtons(globalState.status) // TBD FIXME why is first status never received?
        if (value == null) return

        val latLong = value.latLong
        if (latLong != null) {
            binding.locationText.text = String.format(
                Locale.US,
                "%.5f, %.5f",   // matches precision of google maps display
                latLong.latitude, latLong.longitude)

            val locationAccText = binding.locationAccText
            latLong.accuracy?.let { meters ->
                locationAccText.text = String.format(
                    Locale.US,
                    "±%.2f m",
                    meters)
            }
            locationAccText.setTextColor(accuracyColor(latLong.accuracy))

            googleMap?.let { map ->
                val position = LatLng(latLong.latitude, latLong.longitude)
                map.clear()
                map.addMarker(MarkerOptions().position(position))
                val update = if (setInitialMapZoom) {
                    setInitialMapZoom = false
                    CameraUpdateFactory.newLatLngZoom(position, MAPS_ZOOM_LEVEL)
                } else {
                    CameraUpdateFactory.newLatLng(position)
                }
                map.moveCamera(update)
            }

            val instant = Instant.ofEpochMilli(latLong.timestampMillis)
            binding.statusText.text = dateTimeFormatter.format(instant)
        }

        val speedBearing = value.speedBearing
        if (speedBearing != null) {
            val spd_mph = speedBearing.speedMps * MPS_TO_MPH
            binding.speedText.text = String.format(Locale.US,
                "%.1f MPH",
                spd_mph)

            binding.bearingText.text = String.format(Locale.US,
                "%.1f°",
                speedBearing.bearingDeg)

            val speedAccText = binding.speedAccText
            speedBearing.speedAcc?.let { mps ->
                val acc_mph = mps * MPS_TO_MPH
                speedAccText.text = String.format(Locale.US,
                    "±%.3f",
                    acc_mph)
            }
            speedAccText.setTextColor(accuracyColor(speedBearing.speedAcc))

            val bearingAccText = binding.bearingAccText
            speedBearing.bearingAcc?.let { deg ->
                bearingAccText.text = String.format(Locale.US,
                    "±%.3f°",
                    deg)
            }
            bearingAccText.setTextColor(accuracyColor(speedBearing.bearingAcc))
        }
    }
}
