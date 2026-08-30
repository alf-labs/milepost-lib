package com.alflabs.milepostlib.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.NavDirections
import com.alflabs.milepostlib.databinding.SensorsFragmentBinding
import com.alflabs.milepostlib.location.LocationData
import com.alflabs.milepostlib.op.OperatorMode
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
open class SensorsFragment: SensorsFragmentBase() {

    companion object {
        private val TAG = SensorsFragment::class.java.simpleName
    }

    // This property is only valid between onCreateView and onDestroyView.
    private var _sensorsBinding: SensorsFragmentBinding? = null
    private val sensorsBinding get() = _sensorsBinding!!

    override val hasBinding get() = _sensorsBinding != null
    override val startButton get() = sensorsBinding.startButton
    override val stopButton get() = sensorsBinding.stopButton
    override val mapView: MapView? get() = sensorsBinding.mapView
    override val mapViewOpt get() = if (hasBinding) mapView else null
    override val statusText get() = sensorsBinding.statusText
    override val locationText get() = sensorsBinding.locationText
    override val locationAccText get() = sensorsBinding.locationAccText
    override val speedText get() = sensorsBinding.speedText
    override val speedAccText: TextView?; get() = sensorsBinding.speedAccText
    override val zoomInButton: ImageButton? get() = sensorsBinding.zoomInBtn
    override val zoomOutButton: ImageButton? get() = sensorsBinding.zoomOutBtn

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        logger.d(TAG, "onCreateView")
        _sensorsBinding = SensorsFragmentBinding.inflate(inflater, container, false)
        return sensorsBinding.root
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
        _sensorsBinding = null
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
        logger.d(TAG, "@@ setupInitialMapZoom with zoom level = $zoomLevel")
        return CameraUpdateFactory.newLatLngZoom(position, zoomLevel)
    }

}
