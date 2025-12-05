package com.alflabs.mpgpstrack.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.widget.CompoundButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.alflabs.mpgpstrack.databinding.PermissionsFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionsFragment : DialogFragment() {
    private val TAG = PermissionsFragment::class.java.simpleName
    private val fragmentArgs: PermissionsFragmentArgs by navArgs()


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

        fun allPermissionsGranted(context: Context): Boolean =
            isGpsGranted(context) && isNotificationGranted(context)
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
        Log.d(TAG, "Switch: ${switchBtn.isChecked}")
        if (switchBtn.isChecked) {
            checkMethod()
        }
    }

    private fun updateAllStates() {
        updateGps()
        updateNotification()
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

    private fun checkGpsPermissions() {
        checkPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            "MilePost Trackr needs access to the GPS fine location. Please allow access in the next dialog to enable the feature.",
            ::isGpsGranted,
        )
    }

    private fun checkNotificationPermissions() {
        checkPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
            "MilePost Trackr needs to show Notifications. Please allow access in the next dialog to enable the feature.",
            ::isNotificationGranted,
        )
    }

    private fun checkPermissions(
        permission: String,
        rationale: String,
        isGranted: (Context) -> Boolean,
     ) {
        val granted = isGranted(requireContext())
        Log.d(TAG, "checkNotificationPermissions: $permission --> granted $granted")
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
        Log.d(TAG, "Permission Needs Rationale: $needsRationale")
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

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionsResult: $requestCode, $permissions, $grantResults")
        if (requestCode == PERM_REQUEST_CODE) {
            updateAllStates()
        }
    }

}
