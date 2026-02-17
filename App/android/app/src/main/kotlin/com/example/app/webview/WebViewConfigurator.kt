// android/app/src/main/kotlin/com/example/app/webview/WebViewConfigurator.kt
/// This file centralizes WebView settings to ensure secure defaults.

package com.example.app.webview

import android.annotation.SuppressLint
import android.webkit.WebView

object WebViewConfigurator {

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(webView: WebView, enableJavaScript: Boolean = false) {
        webView.settings.apply {
            // Security Defaults
            javaScriptEnabled = enableJavaScript // Must be explicitly enabled
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(false)
            allowUniversalAccessFromFileURLs = false
            allowFileAccessFromFileURLs = false

            // Performance & Usability
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }
    }
}
