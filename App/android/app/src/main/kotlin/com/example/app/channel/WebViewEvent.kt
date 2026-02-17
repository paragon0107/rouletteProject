// android/app/src/main/kotlin/com/example/app/channel/WebViewEvent.kt
/// This file defines the sealed class for structured WebView events.

package com.example.app.channel

import com.google.gson.Gson

sealed class WebViewEvent(protected val type: String) {
    fun toJson(): String {
        val data = mapOf("type" to type, "data" to this)
        return Gson().toJson(data)
    }
}

data class PageStarted(val url: String) : WebViewEvent("onPageStarted")
data class PageFinished(val url: String) : WebViewEvent("onPageFinished")
data class WebViewError(
    val errorCode: Int,
    val description: String,
    val failingUrl: String
) : WebViewEvent("onError")
