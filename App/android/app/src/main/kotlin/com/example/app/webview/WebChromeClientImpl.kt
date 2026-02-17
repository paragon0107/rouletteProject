// android/app/src/main/kotlin/com/example/app/webview/WebChromeClientImpl.kt
/// Handles Chrome-related events like progress and page titles.

package com.example.app.webview

import android.webkit.WebChromeClient
import android.webkit.WebView

class WebChromeClientImpl(
    private val onProgressChanged: ((Int) -> Unit)? = null
) : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged?.invoke(newProgress)
    }
}
