// lib/webview_screen.dart
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
    // Open the native WebView immediately.
    _controller.openUrl(initialUrl);
  }

  @override
  Widget build(BuildContext context) {
    // Intentionally render nothing to avoid any Flutter-side loading UI.
    return const SizedBox.shrink();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }
}
