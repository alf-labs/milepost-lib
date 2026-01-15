package com.alflabs.milepost.fragments

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.webkit.WebViewClientCompat
import com.alflabs.milepost.R
import com.alflabs.milepost.databinding.AboutFragmentBinding
import com.alflabs.utils.ILogger
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import androidx.core.graphics.createBitmap

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

            val jsAppData = JSAppData(requireContext())
            binding.webview.addJavascriptInterface(jsAppData, "JSAppData")
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
private class JSAppData(val context: Context) {
    private val TAG = JSAppData::class.java.simpleName
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

    @JavascriptInterface
    fun getAppIcon(): String {
        val drawableId = R.mipmap.ic_launcher_mp
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return ""

        val density = context.resources.displayMetrics.density
        val widthPx = if (drawable.intrinsicWidth > 0) {
            (drawable.intrinsicWidth / density).toInt()
        } else {
            (25 * density).toInt()
        }
        val heightPx = if (drawable.intrinsicHeight > 0) {
            (drawable.intrinsicHeight / density).toInt()
        } else {
            (25 * density).toInt()
        }
        Log.d(TAG, "@@ getAppIcon: $density --> $widthPx x $heightPx")

        val bitmap = createBitmap(widthPx, heightPx)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
