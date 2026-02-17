// android/app/src/main/kotlin/com/example/app/channel/ChannelMessenger.kt
/// This file handles sending structured events from native to Flutter.

package com.example.app.channel

import android.util.Log
import io.flutter.plugin.common.MethodChannel

class ChannelMessenger(private val channel: MethodChannel) {
    companion object {
        private const val TAG = "WV_DEBUG"
    }

    fun sendEvent(event: WebViewEvent) {
        runCatching {
            channel.invokeMethod(ChannelConstants.METHOD_ON_WEB_VIEW_EVENT, event.toJson())
            Log.d(TAG, "sendEvent(): ${event.javaClass.simpleName}")
        }.onFailure {
            Log.w(TAG, "sendEvent() failed: ${event.javaClass.simpleName}", it)
        }
    }
}
