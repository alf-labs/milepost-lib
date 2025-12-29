package com.alflabs.milepost.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.SensorsFragmentBinding
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.location.LocationService
import com.alflabs.milepost.main.AppSettings
import com.alflabs.milepost.main.GlobalState
import com.alflabs.milepost.main.GlobalMode
import com.alflabs.milepost.utils.MapStyle
import com.alflabs.milepost.utils.Units
import com.alflabs.rx.AndroidSchedulers
import com.alflabs.rx.ISubscriber
import com.alflabs.utils.ILogger
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
open class SensorsFragment: Fragment() {

    companion object {
        private val TAG = SensorsFragment::class.java.simpleName
        private const val PERM_REQUEST_CODE = 1
        private const val MAPS_ZOOM_LEVEL = 15f
    }

    internal open val runningMode = GlobalMode.SENSOR
    internal open fun actionToPermissionsDlg(): NavDirections =
        SensorsFragmentDirections.actionSensorsToPermissionsDlg()
    internal open fun onStopButton(trackId: Long?) {}

    private val dateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    // TBD: change to DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")

    private val locationSubscriber: ISubscriber<in LocationData?> =
        ISubscriber<LocationData?> { _, event -> onLocationChanged(event) }
    private val modeSubscriber: ISubscriber<in GlobalMode?> =
        ISubscriber<GlobalMode?> { _, event -> onGlobalModeChanged(event) }

    @Inject lateinit var logger: ILogger
    @Inject lateinit var globalState: GlobalState
    @Inject lateinit var appSettings: AppSettings

    private var setInitialMapZoom = true
    private var googleMap: GoogleMap? = null
    private var accuracyColorMissing: Int = 0
    private var accuracyColorPresent: Int = 0
    private var useImperialUnits = false

    // This property is only valid between onCreateView and onDestroyView.
    private var _binding: SensorsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        logger.d(TAG, "onCreateView")
        _binding = SensorsFragmentBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.d(TAG, "onViewCreated")

        useImperialUnits = appSettings.useImperialUnits(lifecycleScope)
        accuracyColorMissing = requireContext().getColor(R.color.accMissing)
        accuracyColorPresent = requireContext().getColor(R.color.accPresent)

        binding.startButton.text = when (runningMode) {
            GlobalMode.SENSOR -> "Test"
            GlobalMode.RECORD -> "Record"
            GlobalMode.PLAYBACK -> "Playback"
            else -> "Error"
        }

        binding.startButton.setOnClickListener {
            logger.d(TAG, "On click START button")

            // Sanity check:
            // - If the global state is already started, we should not be here.
            // - If mode cannot start, we should not be here.
            if (globalState.mode.start || !runningMode.start) {
                logger.d(TAG, "START --> already started / cannot start")
                return@setOnClickListener
            }

            if (!PermissionsFragment.allPermissionsGranted(requireContext())) {
                logger.d(TAG, "START --> request permissions")
                findNavController().navigate(actionToPermissionsDlg())
            } else {
                setInitialMapZoom = true
                val intent = Intent(requireContext(), LocationService::class.java)
                intent.putExtra(LocationService.EXTRA_RUNNING_MODE, runningMode)
                requireActivity().startForegroundService(intent)
            }
        }

        binding.stopButton.setOnClickListener {
            logger.d(TAG, "On click STOP button")
            val trackId = globalState.currentTrackId
            requireActivity().stopService(Intent(requireContext(), LocationService::class.java))
            onStopButton(trackId)
        }

        updateButtons(globalState.mode)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            googleMap = map
            googleMap?.let { MapStyle.applyMapStyle(
                requireContext(),
                it,
                logger) }
        }
    }

    override fun onResume() {
        super.onResume()
        logger.d(TAG, "onResume")
        binding.mapView.onResume()
        globalState.modeStream.subscribe(AndroidSchedulers.mainThread(), modeSubscriber)
        globalState.locationStream.subscribe(AndroidSchedulers.mainThread(), locationSubscriber)
        updateButtons(globalState.mode)
    }

    override fun onPause() {
        super.onPause()
        logger.d(TAG, "onPause")
        binding.mapView.onPause()
        globalState.locationStream.remove(locationSubscriber)
        globalState.modeStream.remove(modeSubscriber)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        logger.d(TAG, "onDestroyView")
        binding.mapView.onDestroy()
        _binding = null
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        logger.d(TAG, "onRequestPermissionsResult: $requestCode, $permissions, $grantResults")
        if (requestCode == PERM_REQUEST_CODE
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            logger.d(TAG, "onRequestPermissionsResult --> GRANTED")

            requireActivity()
                .startForegroundService(Intent(requireContext(), LocationService::class.java))
        } else {
            logger.d(TAG, "onRequestPermissionsResult --> NOT granted")
        }
    }

    private fun updateButtons(runningState: GlobalMode) {
        // Start button is enabled if running state is not already started
        binding.startButton.isEnabled = !runningState.start
        // Stop button is enabled if running state is not already stopped
        binding.stopButton.isEnabled = !runningState.stop
    }

    fun accuracyColor(value: Float?) : Int =
        if (value == null) accuracyColorMissing else accuracyColorPresent

    private fun onGlobalModeChanged(mode: GlobalMode?) {
        logger.d(TAG, "onGlobalModeChanged: $mode")
        if (mode == null) return
        binding.statusText.text = mode.label
        updateButtons(mode)
    }

    fun onLocationChanged(value: LocationData?) {
        logger.d(TAG, "onLocationChanged: $value")
        updateButtons(globalState.mode) // TBD FIXME why is first mode change never received?
        if (value == null) return

        val recordedLocationList = globalState.currentLocationList
        val numPoints = recordedLocationList?.locationsCount ?: 0

        val latLong = value.latLong
        binding.locationText.text = String.format(
            Locale.US,
            "%.5f, %.5f  # $numPoints",   // matches precision of google maps display
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

            // Compute the path of last 10 locations recorded
            // Note that the last location recorded may not match the current LocationData.
            recordedLocationList?.let {
                val locations = it.locationsList ?: emptyList()
                if (locations.size > 1) {
                    val lineColor = requireContext().getColor(R.color.colorPrimaryDark)
                    val polylineOptions = PolylineOptions()
                        .color(lineColor)
                        .width(10f)

                    for (location in locations) {
                        polylineOptions.add(
                            LatLng(location.latLong.latitude, location.latLong.longitude))
                    }
                    map.addPolyline(polylineOptions)
                }
            }

            // Place current marker
            map.addMarker(MarkerOptions().position(position))

            val update = if (setInitialMapZoom) {
                setInitialMapZoom = false
                CameraUpdateFactory.newLatLngZoom(position, MAPS_ZOOM_LEVEL)
            } else {
                CameraUpdateFactory.newLatLng(position)
            }
            map.animateCamera(update)
        }

        val instant = Instant.ofEpochMilli(latLong.timestampMillis)
        binding.statusText.text = dateTimeFormatter.format(instant)

        val speedBearing = value.speedBearing
        if (speedBearing != null) {
            binding.speedText.text = String.format(
                Locale.US,
                "%.1f %s",
                Units.mpsToUnit(speedBearing.speedMps, useImperialUnits),
                Units.mpsUnitName(useImperialUnits)
            )

            binding.bearingText.text = String.format(Locale.US,
                "%.1f°",
                speedBearing.bearingDeg)

            val speedAccText = binding.speedAccText
            speedBearing.speedAcc?.let { mps ->
                speedAccText.text = String.format(Locale.US,
                    "±%.3f",
                    Units.mpsToUnit(mps, useImperialUnits))
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
