// android/app/src/main/kotlin/com/example/app/webview/WebViewClientImpl.kt
/// Handles navigation, loading, and error events within the WebView.

package com.example.app.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.app.channel.ChannelMessenger
import com.example.app.channel.PageFinished
import com.example.app.channel.PageStarted
import com.example.app.channel.WebViewError

class WebViewClientImpl(
    private val messenger: ChannelMessenger,
    private val uiCallback: UiCallback? = null
) : WebViewClient() {
    interface UiCallback {
        fun onPageStarted(url: String)
        fun onPageFinished(url: String)
        fun onMainFrameError(errorCode: Int, description: String, failingUrl: String)
        fun onRenderProcessGone(didCrash: Boolean, failingUrl: String)
    }

    companion object {
        private const val TAG = "WV_DEBUG"
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d(TAG, "onPageStarted(): $url")
        uiCallback?.onPageStarted(url)
        messenger.sendEvent(PageStarted(url = url))
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        Log.d(TAG, "onPageFinished(): $url")
        uiCallback?.onPageFinished(url)
        messenger.sendEvent(PageFinished(url = url))
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        Log.e(
            TAG,
            "onReceivedError(): code=${error.errorCode} mainFrame=${request.isForMainFrame} url=${request.url}"
        )
        val description = error.description.toString()
        val failingUrl = request.url.toString()

        messenger.sendEvent(
            WebViewError(
                errorCode = error.errorCode,
                description = description,
                failingUrl = failingUrl
            )
        )

        if (request.isForMainFrame) {
            uiCallback?.onMainFrameError(
                errorCode = error.errorCode,
                description = description,
                failingUrl = failingUrl
            )
        }
    }

    @Deprecated("Deprecated in API 23")
    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return
        }

        super.onReceivedError(view, errorCode, description, failingUrl)
        val safeDescription = description.orEmpty()
        val safeFailingUrl = failingUrl.orEmpty()

        Log.e(
            TAG,
            "onReceivedError(legacy): code=$errorCode url=$safeFailingUrl desc=$safeDescription"
        )

        messenger.sendEvent(
            WebViewError(
                errorCode = errorCode,
                description = safeDescription,
                failingUrl = safeFailingUrl
            )
        )

        uiCallback?.onMainFrameError(
            errorCode = errorCode,
            description = safeDescription,
            failingUrl = safeFailingUrl
        )
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (!request.isForMainFrame) {
            return
        }

        val statusCode = errorResponse.statusCode
        val description = "HTTP $statusCode"
        val failingUrl = request.url.toString()

        Log.e(TAG, "onReceivedHttpError(): statusCode=$statusCode url=$failingUrl")

        messenger.sendEvent(
            WebViewError(
                errorCode = statusCode,
                description = description,
                failingUrl = failingUrl
            )
        )

        uiCallback?.onMainFrameError(
            errorCode = statusCode,
            description = description,
            failingUrl = failingUrl
        )
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        super.onReceivedSslError(view, handler, error)
        val description = "SSL Error: $error"
        val failingUrl = error.url.orEmpty()

        Log.e(TAG, "onReceivedSslError(): $failingUrl primaryError=${error.primaryError}")

        // SSL errors are critical security risks. The default behavior is to cancel the load.
        // We notify Flutter of this specific type of error.
        messenger.sendEvent(
            WebViewError(
                errorCode = error.primaryError,
                description = description,
                failingUrl = failingUrl
            )
        )

        uiCallback?.onMainFrameError(
            errorCode = error.primaryError,
            description = description,
            failingUrl = failingUrl
        )

        handler.cancel() // Always deny insecure connections.
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        val didCrash = detail.didCrash()
        val priority = detail.rendererPriorityAtExit()
        val currentUrl = view.url.orEmpty()

        Log.e(
            TAG,
            "onRenderProcessGone(): didCrash=$didCrash priority=$priority url=$currentUrl"
        )

        uiCallback?.onRenderProcessGone(didCrash = didCrash, failingUrl = currentUrl)

        messenger.sendEvent(
            WebViewError(
                errorCode = -10001,
                description = "WebView renderer process gone. didCrash=$didCrash",
                failingUrl = currentUrl
            )
        )

        runCatching {
            view.stopLoading()
            view.removeAllViews()
            (view.parent as? ViewGroup)?.removeView(view)
            view.destroy()
        }.onFailure {
            Log.e(TAG, "Failed to cleanup WebView after renderer gone", it)
        }

        // We handled it to avoid process termination.
        return true
    }
}
