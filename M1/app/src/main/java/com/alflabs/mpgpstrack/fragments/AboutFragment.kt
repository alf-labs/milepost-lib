package com.alflabs.mpgpstrack.fragments

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import androidx.fragment.app.Fragment
import androidx.webkit.WebViewClientCompat
import com.alflabs.mpgpstrack.databinding.AboutFragmentBinding
import com.alflabs.utils.ILogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AboutFragment : Fragment() {

    companion object {
        private val TAG = AboutFragment::class.java.simpleName
    }

    @Inject lateinit var logger: ILogger

    private var _binding: AboutFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AboutFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            binding.webview.webViewClient = WebViewClientCompat()
            binding.webview.settings.javaScriptEnabled = true

            val jsVersion = JSVersion(requireContext())
            binding.webview.addJavascriptInterface(jsVersion, "JSAppVersion")
            // Make the webview transparent
            binding.webview.setBackgroundColor(0x00000000)
            binding.webview.loadUrl("file:///android_asset/about.html")
            logger.d(TAG, "WebView loaded")
        } catch (e: Exception) {
            logger.d(TAG, "WebView Error:", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ---
private class JSVersion(val context: Context) {
    private val TAG = JSVersion::class.java.simpleName
    private var version: String? = null

    @JavascriptInterface
    fun longVersion(): String {
        if (version == null) {
            val pm: PackageManager = context.getPackageManager()
            val pi: PackageInfo
            try {
                pi = pm.getPackageInfo(context.getPackageName(), 0)
                version = pi.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                version = "" // failed, ignored
            }
        }
        return version!!
    }

    @JavascriptInterface
    fun shortVersion(): String {
        var v = longVersion()
        v = v.substring(0, v.lastIndexOf('.'))
        return v
    }
}
