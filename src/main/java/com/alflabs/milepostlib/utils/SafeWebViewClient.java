package com.alflabs.milepostlib.utils;

import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import androidx.webkit.WebViewClientCompat;
import com.alflabs.utils.ILogger;

/**
 * WebViewClient that implements onRenderProcessGone.
 * <p/>
 * Based on developer guide:
 * https://developer.android.com/develop/ui/views/layout/webapps/handle-termination
 * <p/>
 * Implementation detail: this is in Java and not Kotlin because the Linter warning
 * MissingOnRenderProcessGone seems broken and does not resolve properly on the Kotlin
 * implementation.
 */
public class SafeWebViewClient extends WebViewClientCompat {
    private final ILogger mLogger;
    private final static String TAG = SafeWebViewClient.class.getSimpleName();

    public SafeWebViewClient(ILogger logger) {
        this.mLogger = logger;
    }

    @Override
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        mLogger.d(TAG, "WebView render process gone. Did crash: " + detail.didCrash());

        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(view);
        }

        view.destroy();
        return true;
    }
}
