package com.alflabs.milepost.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.alflabs.milepost.R
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.main.AppSettings
import com.alflabs.milepost.main.GlobalState
import com.alflabs.milepost.op.Operator
import com.alflabs.milepost.op.OperatorMode
import com.alflabs.milepost.service.LocationService
import com.alflabs.milepost.service.ServiceMode
import com.alflabs.milepost.utils.MapStyle
import com.alflabs.milepost.utils.Units
import com.alflabs.rx.AndroidSchedulers
import com.alflabs.rx.ISubscriber
import com.alflabs.utils.ILogger
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject


open abstract class SensorsFragmentBase: Fragment() {

    companion object {
        private val TAG = SensorsFragmentBase::class.java.simpleName
        protected const val MAPS_ZOOM_LEVEL = 15f
    }

    protected val dateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    // TBD: change to DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")

    private val locationSubscriber: ISubscriber<in LocationData?> =
        ISubscriber<LocationData?> { _, event -> onLocationChanged(event) }
    private val serviceModeSubscriber: ISubscriber<in ServiceMode?> =
        ISubscriber<ServiceMode?> { _, event -> onGlobalModeChanged(event) }
    private val operatorModeSubscriber: ISubscriber<in OperatorMode?> =
        ISubscriber<OperatorMode?> { _, event -> onGlobalModeChanged(event) }

    @Inject lateinit var logger: ILogger
    @Inject lateinit var globalState: GlobalState
    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var operator: Operator

    private var setInitialMapZoom = true
    protected  var googleMap: GoogleMap? = null
    protected  var accuracyColorMissing: Int = 0
        private set
    protected  var accuracyColorPresent: Int = 0
        private set
    protected var useImperialUnits = false
    protected var serviceStartedAutomatically = false
        private set

    protected abstract val hasBinding: Boolean
    protected abstract val startButton: Button
    protected abstract val stopButton: Button
    protected abstract val mapView: MapView
    protected abstract val statusText: TextView
    protected abstract val locationText: TextView
    protected abstract val locationAccText: TextView?
    protected abstract val speedText: TextView
    protected abstract val bearingText: TextView?
    protected abstract val speedAccText: TextView?
    protected abstract val bearingAccText: TextView?

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.d(TAG, "onViewCreated")

        useImperialUnits = appSettings.useImperialUnits(lifecycleScope)
        accuracyColorMissing = requireContext().getColor(R.color.acc_missing)
        accuracyColorPresent = requireContext().getColor(R.color.acc_present)

        startButton.text = when (operatorMode) {
            OperatorMode.IDLE, OperatorMode.SENSORS -> "Test"
            OperatorMode.RECORD -> "Record"
            OperatorMode.PLAYBACK -> "Playback"
        }

        startButton.setOnClickListener {
            logger.d(TAG, "On click START button")
            if (!PermissionsFragment.allPermissionsGranted(requireContext())) {
                logger.d(TAG, "START --> request permissions")
                findNavController().navigate(actionToPermissionsDlg())
            } else {
                setInitialMapZoom = true
                LocationService.startService(requireActivity())
                onStartButton()
            }
        }

        stopButton.setOnClickListener {
            logger.d(TAG, "On click STOP button")
            LocationService.stopService(requireActivity())
            onStopButton()
        }

        updateButtons()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
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
        mapView.onResume()
        globalState.locationStream.subscribe(AndroidSchedulers.mainThread(), locationSubscriber)
        globalState.serviceModeStream.subscribe(AndroidSchedulers.mainThread(), serviceModeSubscriber)
        globalState.operatorModeStream.subscribe(AndroidSchedulers.mainThread(), operatorModeSubscriber)

        lifecycleScope.launch {
            if (PermissionsFragment.allPermissionsGranted(requireContext())
                && globalState.serviceMode != ServiceMode.RUNNING) {
                serviceStartedAutomatically = true
                LocationService.startService(requireActivity())
            }
            updateButtons()
        }
    }

    override fun onPause() {
        super.onPause()
        logger.d(TAG, "onPause")

        if (serviceStartedAutomatically && globalState.operatorMode == OperatorMode.IDLE) {
            serviceStartedAutomatically = false
            LocationService.stopService(requireActivity())
        }

        mapView.onPause()
        globalState.locationStream.remove(locationSubscriber)
        globalState.serviceModeStream.remove(serviceModeSubscriber)
        globalState.operatorModeStream.remove(operatorModeSubscriber)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (hasBinding) {
            mapView.onLowMemory()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        logger.d(TAG, "onDestroyView")
    }

    private fun updateButtons() {
        val isRunning = globalState.serviceMode == ServiceMode.RUNNING
        val isOperatorMode = globalState.operatorMode == operatorMode

        // Start button is enabled if running state is not already started or not desired mode
        startButton.isEnabled = !isRunning || !isOperatorMode
        stopButton.isEnabled = !startButton.isEnabled
    }

    protected fun accuracyColor(value: Float?) : Int =
        if (value == null) accuracyColorMissing else accuracyColorPresent

    protected fun onGlobalModeChanged(event: Any?) {
        logger.d(TAG, "onGlobalModeChanged: $event")
        // TBD this is temporary, change to a better display
        // This may potentially be called outside of the proper binding lifecycle.
        if (hasBinding) {
            statusText.text = "${globalState.operatorMode} ${globalState.serviceMode}"
            updateButtons()
        }
    }

    protected fun onLocationChanged(value: LocationData?) {
        logger.d(TAG, "onLocationChanged: ${value?.latLong?.timestampMillis}")
        // This may potentially be called outside of the proper binding lifecycle.
        if (value == null || !hasBinding) return

        val recordedLocationsList = getLocationsList()
        val numPoints = recordedLocationsList?.size ?: 0

        val latLong = value.latLong
        locationText.text = String.format(
            Locale.US,
            "%.5f, %.5f  # $numPoints",   // matches precision of google maps display
            latLong.latitude, latLong.longitude)

        latLong.accuracy.let { meters ->
            locationAccText?.text = String.format(
                Locale.US,
                "±%.2f m",
                meters)
        }
        locationAccText?.setTextColor(accuracyColor(latLong.accuracy))

        googleMap?.let { map ->
            val position = LatLng(latLong.latitude, latLong.longitude)
            map.clear()

            // Create a polyline path for the recorded locations
            // TBD only recompute polyline if list has changed (during recording) or fragment has resumed
            recordedLocationsList?.let { locations ->
                if (locations.size > 1) {
                    val width = resources.getDimensionPixelSize(R.dimen.map_track)
                    val lineColor = requireContext().getColor(R.color.map_track)
                    val polylineOptions = PolylineOptions()
                        .color(lineColor)
                        .width(width.toFloat())

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
                setupInitialMapZoom(map, position, MAPS_ZOOM_LEVEL)
            } else {
                CameraUpdateFactory.newLatLng(position)
            }
            map.animateCamera(update)
        }

        val instant = Instant.ofEpochMilli(latLong.timestampMillis)
        statusText.text = dateTimeFormatter.format(instant)

        val speedBearing = value.speedBearing
        if (speedBearing != null) {
            speedText.text = String.format(
                Locale.US,
                "%.1f %s",
                Units.mpsToUnit(speedBearing.speedMps, useImperialUnits),
                Units.mpsUnitName(useImperialUnits)
            )

            bearingText?.text = String.format(Locale.US,
                "%.1f°",
                speedBearing.bearingDeg)

            speedBearing.speedAcc.let { mps ->
                speedAccText?.text = String.format(Locale.US,
                    "±%.3f",
                    Units.mpsToUnit(mps, useImperialUnits))
            }
            speedAccText?.setTextColor(accuracyColor(speedBearing.speedAcc))

            speedBearing.bearingAcc.let { deg ->
                bearingAccText?.text = String.format(Locale.US,
                    "±%.3f°",
                    deg)
            }
            bearingAccText?.setTextColor(accuracyColor(speedBearing.bearingAcc))
        }
    }

    // Interface customized by derived classes

    protected abstract val operatorMode: OperatorMode

    protected abstract fun actionToPermissionsDlg(): NavDirections

    protected abstract fun onStartButton()

    protected abstract fun onStopButton()

    protected abstract fun getLocationsList(): List<LocationData>?

    protected abstract fun setupInitialMapZoom(map: GoogleMap,
                                          position: LatLng,
                                          zoomLevel: Float): CameraUpdate

}
