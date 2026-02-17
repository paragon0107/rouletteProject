// lib/webview_controller.dart
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
