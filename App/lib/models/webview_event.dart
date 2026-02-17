// lib/models/webview_event.dart
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
