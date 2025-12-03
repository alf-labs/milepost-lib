package com.alflabs.mpgpstrack.activity

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.alflabs.mpgpstrack.databinding.AboutFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutFragment : Fragment() {

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

        binding.webview.webViewClient = WebViewClient()
        binding.webview.settings.javaScriptEnabled = true

        val jsVersion = JSVersion(this)
        binding.webview.addJavascriptInterface(jsVersion, "JSAppVersion")

        // Make the webview transparent
        binding.webview.setBackgroundColor(0x00000000)

        binding.webview.loadUrl("file:///android_asset/about.html")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ---
private class JSVersion constructor(val fragment: AboutFragment) {
    private var version: String? = null

    @JavascriptInterface
    fun longVersion(): String {
        if (version == null) {
            val pm: PackageManager = fragment.requireContext().getPackageManager()
            val pi: PackageInfo
            try {
                pi = pm.getPackageInfo(fragment.requireContext().getPackageName(), 0)
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
