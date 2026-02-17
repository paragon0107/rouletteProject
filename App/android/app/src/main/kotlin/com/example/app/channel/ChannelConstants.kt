// android/app/src/main/kotlin/com/example/app/channel/ChannelConstants.kt
/// This file defines constant values used for method channel communication.

package com.example.app.channel

object ChannelConstants {
    const val CHANNEL_NAME = "com.example.app/webview"
    const val METHOD_OPEN = "open"
    const val METHOD_ON_WEB_VIEW_EVENT = "onWebViewEvent"
}
