// android/app/src/main/kotlin/com/example/app/webview/WebViewActivity.kt
/// This Activity hosts the native WebView and handles its lifecycle.

package com.example.app.webview

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebViewClient
import android.webkit.WebView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.app.MainActivity
import com.example.app.channel.ChannelMessenger

class WebViewActivity : Activity() {
    private lateinit var rootLayout: FrameLayout
    private lateinit var webView: WebView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var errorOverlay: LinearLayout
    private lateinit var errorMessageView: TextView

    private val messenger = ChannelMessenger(MainActivity.channel)
    private var onBackInvokedCallback: OnBackInvokedCallback? = null
    private var lastRootBackPressedAt: Long = 0L
    private var exitToast: Toast? = null
    private var lastRequestedUrl: String? = null
    private var hasMainFrameLoadError: Boolean = false

    companion object {
        private const val TAG = "WV_DEBUG"
        private const val EXIT_BACK_PRESS_INTERVAL_MS = 2000L
        private const val EXIT_TOAST_MESSAGE = "한 번 더 누르면 앱이 종료됩니다."
        private const val ERROR_TITLE_TEXT = "페이지를 불러올 수 없어요"
        private const val ERROR_RETRY_BUTTON_TEXT = "재시도"
        const val EXTRA_URL = "extra_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate()")

        val url = intent.getStringExtra(EXTRA_URL)
            ?: throw IllegalStateException("WebViewActivity requires an initial URL.")

        setupContentView()
        registerSystemBackCallbackIfNeeded()
        setupWebView()
        loadUrlWithLoading(url)
    }

    private fun setupContentView() {
        rootLayout = FrameLayout(this)
        webView = WebView(this)
        loadingOverlay = createLoadingOverlay()
        errorOverlay = createErrorOverlay()

        rootLayout.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        rootLayout.addView(
            loadingOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        rootLayout.addView(
            errorOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(rootLayout)
    }

    private fun createLoadingOverlay(): FrameLayout {
        val overlay = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(0x66FFFFFF)
            isClickable = true
        }

        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
        }

        overlay.addView(
            progressBar,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        return overlay
    }

    private fun createErrorOverlay(): LinearLayout {
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            visibility = View.GONE
            setPadding(24.dp(), 24.dp(), 24.dp(), 24.dp())
        }

        val titleView = TextView(this).apply {
            text = ERROR_TITLE_TEXT
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(0xFF111827.toInt())
        }

        errorMessageView = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFF6B7280.toInt())
        }

        val retryButton = Button(this).apply {
            text = ERROR_RETRY_BUTTON_TEXT
            setOnClickListener { retryLoading() }
        }

        val titleParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val messageParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 12.dp()
        }
        val buttonParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 20.dp()
        }

        overlay.addView(titleView, titleParams)
        overlay.addView(errorMessageView, messageParams)
        overlay.addView(retryButton, buttonParams)
        return overlay
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun setupWebView() {
        WebViewConfigurator.configure(webView, enableJavaScript = true) // Enable JS for modern web apps
        webView.webViewClient = WebViewClientImpl(
            messenger = messenger,
            uiCallback = object : WebViewClientImpl.UiCallback {
                override fun onPageStarted(url: String) {
                    hasMainFrameLoadError = false
                    lastRequestedUrl = url
                    hideErrorOverlay()
                    showLoadingIndicator()
                    Log.d(TAG, "UI callback onPageStarted: $url")
                }

                override fun onPageFinished(url: String) {
                    lastRequestedUrl = url
                    if (!hasMainFrameLoadError) {
                        hideLoadingIndicator()
                        hideErrorOverlay()
                    }
                    Log.d(TAG, "UI callback onPageFinished: $url error=$hasMainFrameLoadError")
                }

                override fun onMainFrameError(errorCode: Int, description: String, failingUrl: String) {
                    hasMainFrameLoadError = true
                    if (failingUrl.isNotBlank()) {
                        lastRequestedUrl = failingUrl
                    }
                    showErrorOverlay(errorCode, description, failingUrl)
                }

                override fun onRenderProcessGone(didCrash: Boolean, failingUrl: String) {
                    hasMainFrameLoadError = true
                    if (failingUrl.isNotBlank()) {
                        lastRequestedUrl = failingUrl
                    }
                    val description = if (didCrash) {
                        "WebView 렌더러가 비정상 종료되었습니다."
                    } else {
                        "WebView 렌더러를 사용할 수 없습니다."
                    }
                    showErrorOverlay(-10001, description, failingUrl)
                }
            }
        )

        webView.webChromeClient = WebChromeClientImpl { progress ->
            onPageProgressChanged(progress)
        }
        Log.d(TAG, "WebView configured")
    }

    private fun onPageProgressChanged(progress: Int) {
        if (hasMainFrameLoadError) {
            return
        }

        if (progress in 1..99) {
            showLoadingIndicator()
            return
        }

        if (progress >= 100) {
            hideLoadingIndicator()
        }
    }

    private fun loadUrlWithLoading(url: String) {
        hasMainFrameLoadError = false
        lastRequestedUrl = url
        hideErrorOverlay()
        showLoadingIndicator()
        Log.i(TAG, "loadUrlWithLoading(): $url")
        webView.loadUrl(url)
    }

    private fun retryLoading() {
        resetExitGuard()
        val retryUrl = resolveRetryUrl()

        if (retryUrl == null) {
            showErrorOverlay(
                errorCode = -1,
                description = "재시도할 주소를 찾을 수 없습니다.",
                failingUrl = ""
            )
            return
        }

        hasMainFrameLoadError = false
        hideErrorOverlay()
        showLoadingIndicator()
        Log.i(TAG, "retryLoading(): $retryUrl")

        if (webView.url == retryUrl) {
            webView.reload()
        } else {
            webView.loadUrl(retryUrl)
        }
    }

    private fun resolveRetryUrl(): String? {
        val currentUrl = webView.url?.trim().orEmpty()
        if (currentUrl.isNotEmpty() && !currentUrl.equals("about:blank", ignoreCase = true)) {
            return currentUrl
        }

        val requestedUrl = lastRequestedUrl?.trim().orEmpty()
        if (requestedUrl.isNotEmpty()) {
            return requestedUrl
        }

        return intent.getStringExtra(EXTRA_URL)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun showLoadingIndicator() {
        if (loadingOverlay.visibility != View.VISIBLE) {
            loadingOverlay.visibility = View.VISIBLE
            Log.d(TAG, "showLoadingIndicator()")
        }
    }

    private fun hideLoadingIndicator() {
        if (loadingOverlay.visibility != View.GONE) {
            loadingOverlay.visibility = View.GONE
            Log.d(TAG, "hideLoadingIndicator()")
        }
    }

    private fun hideErrorOverlay() {
        if (errorOverlay.visibility != View.GONE) {
            errorOverlay.visibility = View.GONE
            Log.d(TAG, "hideErrorOverlay()")
        }
    }

    private fun showErrorOverlay(errorCode: Int, description: String, failingUrl: String) {
        hideLoadingIndicator()
        errorMessageView.text = buildErrorMessage(errorCode, description)
        errorOverlay.visibility = View.VISIBLE
        Log.w(
            TAG,
            "showErrorOverlay(): code=$errorCode failingUrl=$failingUrl description=$description"
        )
    }

    private fun buildErrorMessage(errorCode: Int, description: String): String {
        return when (errorCode) {
            WebViewClient.ERROR_HOST_LOOKUP,
            WebViewClient.ERROR_CONNECT,
            WebViewClient.ERROR_TIMEOUT,
            WebViewClient.ERROR_IO,
            WebViewClient.ERROR_PROXY_AUTHENTICATION,
            WebViewClient.ERROR_AUTHENTICATION -> {
                "인터넷 연결 상태를 확인한 후 다시 시도해주세요."
            }
            WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> {
                "보안 연결에 실패했습니다. 잠시 후 다시 시도해주세요."
            }
            in 400..599 -> {
                "서버 응답에 문제가 발생했습니다. 잠시 후 다시 시도해주세요. (HTTP $errorCode)"
            }
            -10001 -> {
                "페이지 렌더링 중 문제가 발생했습니다. 재시도해주세요."
            }
            else -> {
                if (description.isBlank()) {
                    "페이지를 불러오지 못했습니다. 다시 시도해주세요."
                } else {
                    "페이지를 불러오지 못했습니다.\n$description"
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart()")
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume()")
    }

    override fun onPause() {
        Log.i(TAG, "onPause()")
        super.onPause()
    }

    override fun onStop() {
        Log.i(TAG, "onStop()")
        super.onStop()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses OnBackInvokedDispatcher callback registered in onCreate().
            return
        }
        Log.d(TAG, "onBackPressed()")
        handleBackPress(source = "onBackPressed")
    }

    private fun handleBackPress(source: String) {
        // 1) Standard browser history (URL-level navigation)
        if (canGoBackToRealPage()) {
            resetExitGuard()
            Log.i(TAG, "handleBackPress($source): webView.goBack()")
            webView.goBack()
            return
        }

        // 2) SPA history bridge (state-level navigation)
        evaluateSpaBackPress { isHandledByWebApp ->
            if (!isHandledByWebApp) {
                if (shouldCloseOnSecondBackPress()) {
                    Log.i(TAG, "handleBackPress($source): second back at root, closing app")
                    closeApplication()
                } else {
                    Log.i(TAG, "handleBackPress($source): first back at root, showing exit toast")
                    showExitToast()
                }
            } else {
                resetExitGuard()
                Log.i(TAG, "handleBackPress($source): handled by web app")
            }
        }
    }

    private fun shouldCloseOnSecondBackPress(): Boolean {
        val now = SystemClock.elapsedRealtime()
        val shouldClose = now - lastRootBackPressedAt <= EXIT_BACK_PRESS_INTERVAL_MS
        lastRootBackPressedAt = now
        return shouldClose
    }

    private fun showExitToast() {
        exitToast?.cancel()
        exitToast = Toast.makeText(this, EXIT_TOAST_MESSAGE, Toast.LENGTH_SHORT)
        exitToast?.show()
    }

    private fun resetExitGuard() {
        lastRootBackPressedAt = 0L
        exitToast?.cancel()
        exitToast = null
    }

    private fun closeApplication() {
        if (isFinishing) {
            return
        }

        resetExitGuard()
        runCatching {
            if (!moveTaskToBack(true)) {
                finishAffinity()
            }
        }.onFailure {
            Log.w(TAG, "finishAffinity() failed, fallback to finish()", it)
            finish()
        }
    }

    private fun evaluateSpaBackPress(onResult: (Boolean) -> Unit) {
        val script = """
            (function() {
                try {
                    if (typeof window.handleAppBackPress === 'function') {
                        return !!window.handleAppBackPress();
                    }
                    if (window.history && window.history.length > 1) {
                        window.history.back();
                        return true;
                    }
                } catch (e) {}
                return false;
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { rawResult ->
            val normalizedResult = rawResult?.trim()?.removeSurrounding("\"")
            val handled = normalizedResult.equals("true", ignoreCase = true)
            Log.d(
                TAG,
                "evaluateSpaBackPress(): raw=$rawResult normalized=$normalizedResult handled=$handled"
            )
            onResult(handled)
        }
    }

    private fun canGoBackToRealPage(): Boolean {
        if (!webView.canGoBack()) {
            Log.d(TAG, "canGoBackToRealPage(): canGoBack=false")
            return false
        }

        val historyList = webView.copyBackForwardList()
        val previousIndex = historyList.currentIndex - 1
        if (previousIndex < 0) {
            Log.d(
                TAG,
                "canGoBackToRealPage(): invalid previousIndex=$previousIndex size=${historyList.size}"
            )
            return false
        }

        val previousUrl = historyList.getItemAtIndex(previousIndex)?.url.orEmpty().trim()
        if (previousUrl.isEmpty()) {
            Log.d(TAG, "canGoBackToRealPage(): previousUrl is empty")
            return false
        }

        if (previousUrl.equals("about:blank", ignoreCase = true)) {
            Log.d(TAG, "canGoBackToRealPage(): previousUrl is about:blank")
            return false
        }

        Log.d(
            TAG,
            "canGoBackToRealPage(): true currentIndex=${historyList.currentIndex} size=${historyList.size} previousUrl=$previousUrl currentUrl=${webView.url}"
        )
        return true
    }

    private fun registerSystemBackCallbackIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val callback = OnBackInvokedCallback {
                Log.d(TAG, "OnBackInvokedCallback invoked")
                handleBackPress(source = "onBackInvoked")
            }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                callback
            )
            onBackInvokedCallback = callback
            Log.i(TAG, "OnBackInvokedCallback registered")
        }
    }

    private fun unregisterSystemBackCallbackIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val callback = onBackInvokedCallback ?: return
            runCatching {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
            }.onFailure {
                Log.w(TAG, "Failed to unregister OnBackInvokedCallback", it)
            }
            onBackInvokedCallback = null
            Log.i(TAG, "OnBackInvokedCallback unregistered")
        }
    }

    override fun onDestroy() {
        Log.w(TAG, "onDestroy()", RuntimeException("WebViewActivity.onDestroy stack"))
        resetExitGuard()
        unregisterSystemBackCallbackIfNeeded()

        // Clean up WebView safely to avoid "destroy while attached" warnings.
        runCatching {
            hideLoadingIndicator()
            hideErrorOverlay()
            webView.stopLoading()
            webView.removeAllViews()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }.onFailure {
            Log.e(TAG, "WebView cleanup failed in onDestroy()", it)
        }

        super.onDestroy()
        Log.i(TAG, "onDestroy() complete")
    }
}
