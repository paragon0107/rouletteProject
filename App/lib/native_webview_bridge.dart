// lib/native_webview_bridge.dart
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
