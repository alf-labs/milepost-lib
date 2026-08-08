package com.alflabs.milepost.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.widget.CompoundButton
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.alflabs.milepost.app.AppSettings
import com.alflabs.milepost.databinding.PermissionsFragmentBinding
import com.alflabs.milepost.service.ExternalGpsProvider
import com.alflabs.milepost.service.ExternalGpsProvider.Companion.ACTION_USB_PERMISSION
import com.alflabs.utils.ILogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PermissionsFragment : DialogFragment() {
    private val TAG = PermissionsFragment::class.java.simpleName
    private val fragmentArgs: PermissionsFragmentArgs by navArgs()

    @Inject lateinit var logger: ILogger
    @Inject lateinit var appSettings: AppSettings
    @Inject lateinit var externalGpsProvider: ExternalGpsProvider

    // This property is only valid between onCreateView and onDestroyView.
    private var _binding: PermissionsFragmentBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PERM_REQUEST_CODE = 1

        fun isGpsGranted(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        fun isNotificationGranted(context: Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        suspend fun isUsbGpsPermissionGranted(externalGpsProvider: ExternalGpsProvider): Boolean =
            externalGpsProvider.isUsbPermissionNeeded() && externalGpsProvider.isUsbPermissionGranted()

        suspend fun allPermissionsGranted(context: Context, externalGpsProvider: ExternalGpsProvider): Boolean =
            isGpsGranted(context) && isNotificationGranted(context) && isUsbGpsPermissionGranted(externalGpsProvider)
    }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                 val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                 val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                logger.d(TAG, "@@ USB Permission: granted=$granted for device $device")
                updateAllStates()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            usbPermissionReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        try {
            requireContext().unregisterReceiver(usbPermissionReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver was already unregistered
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PermissionsFragmentBinding.inflate(inflater, container, false)

        binding.gpsSwitch.setOnCheckedChangeListener {
                btn, _ -> onButtonCheckedChanged(btn) { checkGpsPermissions() }
        }
        binding.notificationSwitch.setOnCheckedChangeListener {
                btn, _ -> onButtonCheckedChanged(btn) { checkNotificationPermissions() }
        }
        binding.usbGpsSwitch.setOnCheckedChangeListener {
                btn, _ -> onButtonCheckedChanged(btn) {
                    lifecycleScope.launch {
                        externalGpsProvider.requestUsbPermission(requireContext())
                    }
                }
        }

        binding.closeButton.visibility = if (fragmentArgs.showClose) View.VISIBLE else View.GONE
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateAllStates()

        binding.webview.webViewClient = WebViewClient()
        // Make the webview transparent
        binding.webview.setBackgroundColor(0x00000000)
        binding.webview.loadUrl("file:///android_asset/permissions.html")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateAllStates()
    }

    private fun onButtonCheckedChanged(switchBtn: CompoundButton, checkMethod: () -> Unit) {
        logger.d(TAG, "Switch: ${switchBtn.isChecked}")
        if (switchBtn.isChecked) {
            checkMethod()
        }
    }

    private fun updateAllStates() {
        updateGps()
        updateNotification()
        lifecycleScope.launch {
            updateUsbGps()
        }
    }

    private fun updateGps() {
        val granted = isGpsGranted(requireContext())
        // Permission switch can only be set to on, not to off
        binding.gpsSwitch.isChecked = granted
        binding.gpsSwitch.isEnabled = !granted
        binding.gpsSwitch.text = if (granted)
            "GPS fine location is enabled"
        else
            "GPS access is not granted yet"
    }

    private fun updateNotification() {
        val granted = isNotificationGranted(requireContext())
        // Permission switch can only be set to on, not to off
        binding.notificationSwitch.isChecked = granted
        binding.notificationSwitch.isEnabled = !granted
        binding.notificationSwitch.text = if (granted)
            "Notifications are enabled."
        else
            "Notifications are not granted yet."
    }

    private suspend fun updateUsbGps() {
        val settings = appSettings.useExternalGps()
        val needed = externalGpsProvider.isUsbPermissionNeeded()
        val granted = externalGpsProvider.isUsbPermissionGranted()
        // Permission switch can only be set to on, not to off
        binding.usbGpsSwitch.isChecked = needed && granted
        binding.usbGpsSwitch.isEnabled = needed && !granted
        binding.usbGpsSwitch.text = when {
            !settings -> "USB GPS Receiver is not enabled in settings."
            !needed -> "No USB GPS Receiver device found."
            granted -> "USB GPS Receiver: Permission is enabled."
            else -> "USB GPS Receiver: Permission is not granted yet."
        }
    }

    private fun checkGpsPermissions() {
        checkPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            "MilePost Docent needs access to the GPS fine location. Please allow access in the next dialog to enable the feature.",
            ::isGpsGranted,
        )
    }

    private fun checkNotificationPermissions() {
        checkPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
            "MilePost Docent needs to show Notifications. Please allow access in the next dialog to enable the feature.",
            ::isNotificationGranted,
        )
    }

    private fun checkPermissions(
        permission: String,
        rationale: String,
        isGranted: (Context) -> Boolean,
     ) {
        val granted = isGranted(requireContext())
        logger.d(TAG, "checkNotificationPermissions: $permission --> granted $granted")
        if (granted) {
            updateAllStates()
            return
        }

        val action = Runnable {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(permission),
                PERM_REQUEST_CODE
            )
        }

        val needsRationale = shouldShowRequestPermissionRationale(permission)
        logger.d(TAG, "Permission Needs Rationale: $needsRationale")
        if (needsRationale) {
            AlertDialog.Builder(requireContext())
                .setMessage(rationale)
                .setPositiveButton("Continue", null)
                .setOnDismissListener { _ -> action.run() }
                .show()
        } else {
            action.run()
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        logger.d(TAG, "onRequestPermissionsResult: $requestCode, $permissions, $grantResults")
        if (requestCode == PERM_REQUEST_CODE) {
            updateAllStates()
        }
    }
}
