# Design Overview

This document outlines the design and implementation of a Flutter application that uses a native Android WebView to display web content.

## Chosen Approach: MethodChannel + Native Activity

For this project, we are choosing the **MethodChannel + Native Activity** approach over `PlatformView`.

### Why this approach?

- **Maintainability & Clean Separation:** This model creates a strong boundary between Flutter and native code. The `WebView` and all its related logic live entirely within a standard Android `Activity`. Flutter's role is simply to launch this activity and communicate with it. This separation is easier to understand, test, and maintain, especially for developers who may not be deep experts in both Flutter and native Android development.
- **Performance:** By running the `WebView` in its own dedicated `Activity`, we give it a standard Android execution environment. This avoids the potential performance overhead and complexities (known as "stitching") associated with rendering a native `AndroidView` inside the Flutter widget tree.
- **Simplicity:** For full-screen web content, this approach is often simpler. We don't need to manage complex widget compositions, focus issues, or keyboard interactions between Flutter and the native view.

### Disadvantages

- **No Hybrid Composition:** The primary trade-off is that the `WebView` cannot be composed with other Flutter widgets (e.g., placing a Flutter button on top of the web content). The `WebView` lives in its own native screen. For this project's requirements, that is an acceptable trade-off.

## File/Folder Tree

```
lib/
├── models/
│   └── webview_event.dart
├── native_webview_bridge.dart
├── webview_controller.dart
└── webview_screen.dart

android/app/src/main/kotlin/com/example/app/
├── channel/
│   ├── ChannelConstants.kt
│   ├── ChannelMessenger.kt
│   └── WebViewEvent.kt
├── webview/
│   ├── WebViewActivity.kt
│   ├── WebViewClientImpl.kt
│   ├── WebChromeClientImpl.kt
│   └── WebViewConfigurator.kt
```

## Data Flow Diagram

The flow of information between Flutter and the native Android code is as follows:

```
+---------------------------+       +---------------------------------+
|      Flutter (UI)         |       |    Android (Native)             |
+---------------------------+       +---------------------------------+
|                           |       |                                 |
|  WebViewScreen            |       |  WebViewActivity                |
|      (Button Click)       |       |      (Hosts WebView)            |
|           |               |       |                                 |
|           v               |       |                                 |
|  NativeWebViewBridge.open | --(1)-> | Starts WebViewActivity        |
|     (MethodChannel)       |       |   with initialUrl               |
|                           |       |                                 |
|           |               |       |           |                     |
|           v               |       |           v                     |
|  Listens for events       |       |  WebViewClient/ChromeClient     |
|                           |       |   (onPageFinished, onError)     |
|           |               |       |                                 |
|           v               |       |           |                     |
|  WebViewController        |       |           v                     |
|     (Updates State)       | <-(2)-- |  ChannelMessenger.sendEvent   |
|                           |       |     (MethodChannel)             |
|                           |       |                                 |
+---------------------------+       +---------------------------------+
```

1.  **Flutter to Native:** `WebViewScreen` calls `NativeWebViewBridge.open()`. This invokes a method on the `MethodChannel`, passing the `initialUrl`. The native side receives this call and starts the `WebViewActivity`.
2.  **Native to Flutter:** As events occur in the `WebView` (e.g., page loads, errors), `WebViewClientImpl` sends structured event data back to Flutter using the `ChannelMessenger`. The `NativeWebViewBridge` receives these events and passes them to the `WebViewController` to update the UI state.

# Project-Specific Implementation Details

Based on the analysis of the `front` project, here are the specific details for integration.

### Target Web Application

-   **Framework**: React + TypeScript with Vite.
-   **Routing**: The app is a Single-Page Application (SPA) that does not use URL-based routing (e.g., `react-router-dom`). Page navigation is handled internally within the React application's state.
-   **Authentication**: Login state is managed by the `useAuthSession` custom hook, which persists the session object in **`localStorage`**.

### Initial URL for WebView

-   **Development**: The Vite development server runs on `http://localhost:5173`. This will be the initial URL for the WebView during development.
-   **Production**: When deploying, this URL must be changed to the live production URL of the web application.

### Login Persistence Mechanism

The requirement to "maintain login state" is handled seamlessly with the current WebView configuration.

-   The web app saves the authentication session to `localStorage`.
-   Our native `WebViewConfigurator.kt` has `domStorageEnabled = true`.
-   This setting enables the WebView to use `localStorage`, so the session data will be correctly saved on the device and will persist even after the app is closed and reopened. No extra code is needed for this to work.

### Android Network Security (for Development)

Android, by default, blocks "cleartext" (non-HTTPS) traffic. Since our development server uses `http://localhost:5173`, we must temporarily allow it for development and debugging.

-   **Action**: Add `android:usesCleartextTraffic="true"` to the `<application>` tag in `AndroidManifest.xml`.
-   **⚠️ Important**: This flag **must be removed** for production builds that point to a secure (HTTPS) URL.

# Flutter Code

## `lib/models/webview_event.dart`

```dart
// lib/models/webview_event.dart
/// A short header explaining the file's responsibility.
/// This file defines the data models for events sent from the native WebView.

// Using a sealed class pattern for strong typing of events.
sealed class WebViewEvent {
  const WebViewEvent();

  factory WebViewEvent.fromJson(Map<String, dynamic> json) {
    final type = json['type'];
    final data = Map<String, dynamic>.from(json['data'] ?? {});

    switch (type) {
      case 'onPageStarted':
        return PageStarted.fromJson(data);
      case 'onPageFinished':
        return PageFinished.fromJson(data);
      case 'onError':
        return WebViewError.fromJson(data);
      default:
        throw ArgumentError('Unknown WebViewEvent type: $type');
    }
  }
}

class PageStarted extends WebViewEvent {
  final String url;
  const PageStarted({required this.url});

  factory PageStarted.fromJson(Map<String, dynamic> json) {
    return PageStarted(url: json['url'] as String);
  }
}

class PageFinished extends WebViewEvent {
  final String url;
  const PageFinished({required this.url});

  factory PageFinished.fromJson(Map<String, dynamic> json) {
    return PageFinished(url: json['url'] as String);
  }
}

class WebViewError extends WebViewEvent {
  final int errorCode;
  final String description;
  final String failingUrl;

  const WebViewError({
    required this.errorCode,
    required this.description,
    required this.failingUrl,
  });

  factory WebViewError.fromJson(Map<String, dynamic> json) {
    return WebViewError(
      errorCode: json['errorCode'] as int,
      description: json['description'] as String,
      failingUrl: json['failingUrl'] as String,
    );
  }
}

```

## `lib/native_webview_bridge.dart`

```dart
// lib/native_webview_bridge.dart
/// A short header explaining the file's responsibility.
/// This file acts as a bridge to the native platform, handling MethodChannel communication.

import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:app/models/webview_event.dart';

class NativeWebViewBridge {
  static const MethodChannel _channel = MethodChannel('com.example.app/webview');

  // Private constructor to prevent instantiation.
  NativeWebViewBridge._();

  static final StreamController<WebViewEvent> _eventStreamController =
      StreamController.broadcast();

  static Stream<WebViewEvent> get onEvent => _eventStreamController.stream;

  static void initialize() {
    _channel.setMethodCallHandler(_handleNativeMethodCall);
  }

  static Future<void> _handleNativeMethodCall(MethodCall call) async {
    if (call.method == 'onWebViewEvent') {
      final eventJson = jsonDecode(call.arguments as String);
      final event = WebViewEvent.fromJson(eventJson);
      _eventStreamController.add(event);
    }
  }

  static Future<void> open(String url) async {
    try {
      await _channel.invokeMethod('open', {'url': url});
    } on PlatformException catch (e) {
      // Handle potential errors, e.g., if the method is not implemented.
      print("Failed to open WebView: '${e.message}'.");
    }
  }

  static void dispose() {
    _eventStreamController.close();
  }
}
```

## `lib/webview_controller.dart`

```dart
// lib/webview_controller.dart
/// A short header explaining the file's responsibility.
/// This file contains the business logic for the WebView screen, handling state and events.

import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:app/models/webview_event.dart';
import 'package:app/native_webview_bridge.dart';

enum WebViewLoadingState {
  idle,
  loading,
  finished,
  error,
}

class WebViewController extends ChangeNotifier {
  late final StreamSubscription<WebViewEvent> _eventSubscription;

  WebViewLoadingState _loadingState = WebViewLoadingState.idle;
  WebViewLoadingState get loadingState => _loadingState;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  WebViewController() {
    NativeWebViewBridge.initialize();
    _eventSubscription = NativeWebViewBridge.onEvent.listen(_handleWebViewEvent);
  }

  void _handleWebViewEvent(WebViewEvent event) {
    switch (event) {
      case PageStarted():
        _loadingState = WebViewLoadingState.loading;
        _errorMessage = null;
        break;
      case PageFinished():
        _loadingState = WebViewLoadingState.finished;
        break;
      case WebViewError():
        _loadingState = WebViewLoadingState.error;
        _errorMessage = "Error (${event.errorCode}): ${event.description}";
        break;
    }
    notifyListeners();
  }

  void openUrl(String url) {
    NativeWebViewBridge.open(url);
  }

  @override
  void dispose() {
    _eventSubscription.cancel();
    NativeWebViewBridge.dispose();
    super.dispose();
  }
}

```

## `lib/webview_screen.dart`

```dart
// lib/webview_screen.dart
/// A short header explaining the file's responsibility.
/// This file contains the UI for the WebView example screen.

import 'package:flutter/material.dart';
import 'package:app/webview_controller.dart';

class WebViewScreen extends StatefulWidget {
  const WebViewScreen({super.key});

  @override
  State<WebViewScreen> createState() => _WebViewScreenState();
}

class _WebViewScreenState extends State<WebViewScreen> {
  final WebViewController _controller = WebViewController();
  // Production URL for the 'front' web project.
  static const String initialUrl = "https://roulette-front-psi.vercel.app/";

  @override
  void initState() {
    super.initState();
    _controller.addListener(_onControllerChanged);
  }

  void _onControllerChanged() {
    setState(() {
      // Rebuild to reflect the controller's state changes
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Native WebView"),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            if (_controller.loadingState == WebViewLoadingState.loading)
              const CircularProgressIndicator(),
            if (_controller.loadingState == WebViewLoadingState.error)
              Text(
                _controller.errorMessage ?? 'An unknown error occurred.',
                style: const TextStyle(color: Colors.red),
              ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: () => _controller.openUrl(initialUrl),
              child: const Text('Open Web App'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  void dispose() {
    _controller.removeListener(_onControllerChanged);
    _controller.dispose();
    super.dispose();
  }
}
```

# Android/Kotlin Code

## `android/app/src/main/kotlin/com/example/app/channel/ChannelConstants.kt`

```kotlin
// android/app/src/main/kotlin/com/example/app/channel/ChannelConstants.kt
/// A short header explaining the file's responsibility.
/// This file defines constant values used for method channel communication.

package com.example.app.channel

object ChannelConstants {
    const val CHANNEL_NAME = "com.example.app/webview"
    const val METHOD_OPEN = "open"
    const val METHOD_ON_WEB_VIEW_EVENT = "onWebViewEvent"
}
```

## `android/app/src/main/kotlin/com/example/app/channel/WebViewEvent.kt`

```kotlin
// android/app/src/main/kotlin/com/example/app/channel/WebViewEvent.kt
/// A short header explaining the file's responsibility.
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
```

## `android/app/src/main/kotlin/com/example/app/channel/ChannelMessenger.kt`

```kotlin
// android/app/src/main/kotlin/com/example/app/channel/ChannelMessenger.kt
/// A short header explaining the file's responsibility.
/// This file handles sending structured events from native to Flutter.

package com.example.app.channel

import io.flutter.plugin.common.MethodChannel

class ChannelMessenger(private val channel: MethodChannel) {

    fun sendEvent(event: WebViewEvent) {
        channel.invokeMethod(ChannelConstants.METHOD_ON_WEB_VIEW_EVENT, event.toJson())
    }
}
```

## `android/app/src/main/kotlin/com/example/app/webview/WebViewConfigurator.kt`

```kotlin
// android/app/src/main/kotlin/com/example/app/webview/WebViewConfigurator.kt
/// A short header explaining the file's responsibility.
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
```

## `android/app/src/main/kotlin/com/example/app/webview/WebChromeClientImpl.kt`

```kotlin
// android/app/src/main/kotlin/com/example/app/webview/WebChromeClientImpl.kt
/// A short header explaining the file's responsibility.
/// Handles Chrome-related events like progress and page titles.

package com.example.app.webview

import android.webkit.WebChromeClient
import android.webkit.WebView

class WebChromeClientImpl : WebChromeClient() {
    // In the future, this can be used to handle progress updates,
    // window alerts, etc., and forward them to Flutter if needed.
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
    }
}
```

## `android/app/src/main/kotlin/com/example/app/webview/WebViewClientImpl.kt`

```kotlin
// android/app/src/main/kotlin/com/example/app/webview/WebViewClientImpl.kt
/// A short header explaining the file's responsibility.
/// Handles navigation, loading, and error events within the WebView.

package com.example.app.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.app.channel.ChannelMessenger
import com.example.app.channel.PageFinished
import com.example.app.channel.PageStarted
import com.example.app.channel.WebViewError

class WebViewClientImpl(private val messenger: ChannelMessenger) : WebViewClient() {

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        messenger.sendEvent(PageStarted(url = url))
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        messenger.sendEvent(PageFinished(url = url))
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        // This method is for API 23+
        messenger.sendEvent(
            WebViewError(
                errorCode = error.errorCode,
                description = error.description.toString(),
                failingUrl = request.url.toString()
            )
        )
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        super.onReceivedSslError(view, handler, error)
        // SSL errors are critical security risks. The default behavior is to cancel the load.
        // We notify Flutter of this specific type of error.
        messenger.sendEvent(
            WebViewError(
                errorCode = error.primaryError,
                description = "SSL Error: ${error.toString()}",
                failingUrl = error.url
            )
        )
        handler.cancel() // Always deny insecure connections.
    }
}
```

## `android/app/src/main/kotlin/com/example/app/webview/WebViewActivity.kt`

```kotlin
// android/app/src/main/kotlin/com/example/app/webview/WebViewActivity.kt
/// A short header explaining the file's responsibility.
/// This Activity hosts the native WebView and handles its lifecycle.

package com.example.app.webview

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import com.example.app.MainActivity
import com.example.app.channel.ChannelMessenger

class WebViewActivity : Activity() {
    private lateinit var webView: WebView
    private val messenger = ChannelMessenger(MainActivity.channel)

    companion object {
        const val EXTRA_URL = "extra_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        val url = intent.getStringExtra(EXTRA_URL)
            ?: throw IllegalStateException("WebViewActivity requires an initial URL.")

        setupWebView()
        webView.loadUrl(url)
    }

    private fun setupWebView() {
        WebViewConfigurator.configure(webView, enableJavaScript = true) // Enable JS for modern web apps
        webView.webViewClient = WebViewClientImpl(messenger)
        webView.webChromeClient = WebChromeClientImpl()
    }

    // Handle the Android back button
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true // Consume the event
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        // Clean up WebView to prevent memory leaks
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
```

## `android/app/src/main/kotlin/com/example/app/MainActivity.kt`

```kotlin
// android/app/src/main/kotlin/com/example/app/MainActivity.kt
package com.example.app

import android.content.Intent
import com.example.app.channel.ChannelConstants
import com.example.app.webview.WebViewActivity
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    companion object {
        // Expose channel to be accessible by WebViewActivity
        lateinit var channel: MethodChannel
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, ChannelConstants.CHANNEL_NAME)

        channel.setMethodCallHandler { call, result ->
            if (call.method == ChannelConstants.METHOD_OPEN) {
                val url = call.argument<String>("url")
                if (url != null) {
                    val intent = Intent(this, WebViewActivity::class.java).apply {
                        putExtra(WebViewActivity.EXTRA_URL, url)
                    }
                    startActivity(intent)
                    result.success(null)
                } else {
                    result.error("MISSING_URL", "URL is required.", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }
}
```

# Android Setup

## `android/app/build.gradle.kts`

Add the GSON library for JSON serialization.

```kotlin
// android/app/build.gradle.kts
dependencies {
    // ... other dependencies
    implementation("com.google.code.gson:gson:2.10.1")
}
```

## `android/app/src/main/AndroidManifest.xml`



1.  Add the `INTERNET` permission.

2.  Register the `WebViewActivity`.



```xml

<manifest xmlns:android="http://schemas.android.com/apk/res/android">



    <!-- Required for WebView -->

    <uses-permission android:name="android.permission.INTERNET" />



    <application

        android:label="app"

        android:name="${applicationName}"

        android:icon="@mipmap/ic_launcher">



        <!-- ... other activities -->



        <activity

            android:name=".MainActivity"

            android:exported="true"

            ...>

        </activity>



        <!-- Add the WebViewActivity -->

        <activity

            android:name=".webview.WebViewActivity"

            android:exported="false"

            android:configChanges="orientation|screenSize|keyboardHidden" />



        <meta-data

            android:name="flutterEmbedding"

            android:value="2" />

    </application>

</manifest>


```

# Junior-Friendly Pitfalls Checklist

1.  **Forgetting `INTERNET` Permission**: The WebView will fail to load any URL without it. The error is often silent. Always check `AndroidManifest.xml`.
2.  **Back Button Not Handled**: Users expect the back button to navigate WebView history. Forgetting to override `onKeyDown` in `WebViewActivity` leads to a poor user experience where the entire activity closes instead of going back one page.
3.  **Insecure WebView Settings**: The defaults are safer, but developers often enable JavaScript and then forget to disable other risky features like `allowFileAccess`. Always use a `WebViewConfigurator` to manage settings in one place.
4.  **MethodChannel Name Mismatch**: The string identifier for the `MethodChannel` (`com.example.app/webview`) must be identical in both Dart and Kotlin. A typo will cause all communication to fail silently.
5.  **Memory Leaks**: A `WebView` holds a lot of resources. Forgetting to call `webView.destroy()` in `onDestroy()` can lead to memory leaks, especially if the activity is opened and closed multiple times.

# Extension Points

-   **JS Bridge**: In `WebViewConfigurator`, you can add a JavaScript interface (`webView.addJavascriptInterface(...)`). This requires defining a class with `@JavascriptInterface` methods that can be called from the web page's JavaScript.
-   **Cookies/Session Handling**: Use `CookieManager.getInstance()` to get, set, and persist cookies. This is crucial for maintaining login sessions between the app and the website.
-   **File Uploads / Camera**: This requires more complex handling in `WebChromeClientImpl` by overriding `onShowFileChooser` and managing file URI permissions.
-   **Deep Links / External Intents**: Override `shouldOverrideUrlLoading` in `WebViewClientImpl`. You can inspect the URL there. If it's a deep link for your app (`myapp://...`) or an external app (like a map or phone call), you can fire an Android `Intent` instead of letting the WebView handle it.
-   **Authentication Flows**: For OAuth, you can intercept redirect URLs in `shouldOverrideUrlLoading` to capture the auth token and then close the `WebViewActivity`, passing the token back to Flutter.
