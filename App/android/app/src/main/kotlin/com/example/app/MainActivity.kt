package com.example.app

import android.content.Intent
import android.util.Log
import com.example.app.channel.ChannelConstants
import com.example.app.webview.WebViewActivity
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    companion object {
        private const val TAG = "WV_DEBUG"
        // Expose channel to be accessible by WebViewActivity
        lateinit var channel: MethodChannel
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "MainActivity.onCreate()")
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        Log.i(TAG, "configureFlutterEngine()")

        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, ChannelConstants.CHANNEL_NAME)

        channel.setMethodCallHandler { call, result ->
            if (call.method == ChannelConstants.METHOD_OPEN) {
                val url = call.argument<String>("url")
                Log.i(TAG, "METHOD_OPEN received. url=$url")
                if (url != null) {
                    val intent = Intent(this, WebViewActivity::class.java).apply {
                        putExtra(WebViewActivity.EXTRA_URL, url)
                    }
                    startActivity(intent)
                    Log.i(TAG, "WebViewActivity started")
                    result.success(null)
                } else {
                    result.error("MISSING_URL", "URL is required.", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "MainActivity.onDestroy()")
        super.onDestroy()
    }
}
