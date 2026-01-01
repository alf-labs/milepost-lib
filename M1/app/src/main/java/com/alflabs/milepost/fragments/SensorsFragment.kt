package com.alflabs.milepost.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavDirections
import com.alflabs.milepost.databinding.SensorsFragmentBinding
import com.alflabs.milepost.location.LocationData
import com.alflabs.milepost.op.OperatorMode
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
open class SensorsFragment: SensorsFragmentBase() {

    companion object {
        private val TAG = SensorsFragment::class.java.simpleName
    }

    // This property is only valid between onCreateView and onDestroyView.
    protected var _binding: SensorsFragmentBinding? = null
    protected val binding get() = _binding!!

    override val hasBinding get() = _binding != null
    override val startButton get() = binding.startButton
    override val stopButton get() = binding.stopButton
    override val mapView get() = binding.mapView
    override val statusText get() = binding.statusText
    override val locationText get() = binding.locationText
    override val locationAccText get() = binding.locationAccText
    override val speedText get() = binding.speedText
    override val bearingText get() = binding.bearingText
    override val speedAccText get() = binding.speedAccText
    override val bearingAccText get() = binding.bearingAccText

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
    }

    override fun onResume() {
        super.onResume()
        logger.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        logger.d(TAG, "onPause")
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        logger.d(TAG, "onDestroyView")
        mapView.onDestroy()
        _binding = null
    }

    // Interface customized by derived classes

    override val operatorMode = OperatorMode.SENSORS

    override fun actionToPermissionsDlg(): NavDirections =
        SensorsFragmentDirections.actionSensorsToPermissionsDlg()

    override fun onStartButton() {
        operator.startSensors()
    }

    override fun onStopButton() {
        operator.stopSensors()
    }

    override fun getLocationsList(): List<LocationData>? = operator.currentLocationsList

    override fun setupInitialMapZoom(map: GoogleMap,
                                     position: LatLng,
                                     zoomLevel: Float): CameraUpdate {
        return CameraUpdateFactory.newLatLngZoom(position, zoomLevel)
    }

}
