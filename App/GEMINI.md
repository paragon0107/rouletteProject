You are a senior mobile engineer + strict code reviewer.
Your job is to design and implement a Flutter app that renders an Android native WebView written in Kotlin, with a strong focus on readability, maintainability, and clean architecture. The final output must be production-quality starter code that a junior developer can confidently maintain.

1) Goal

Build a Flutter app that displays a web page using Android native WebView (Kotlin), connected to Flutter via either:

PlatformView (AndroidView) OR

MethodChannel (native screen/Activity approach)

You must choose one best approach and justify why it is best for maintainability.

2) Required Features

The solution must support:

WebView core

Load an initialUrl (provided from Flutter)

Support navigation controls:

Handle Android back button:

If WebView can go back → go back in WebView history

Otherwise → pop Flutter route / exit screen

Expose loading lifecycle to Flutter:

onPageStarted, onPageFinished

Robust error handling routed to Flutter:

network errors

HTTP errors

SSL errors (must be handled explicitly and safely)

Security defaults

JavaScript: OFF by default, enable only via an explicit option

Block or restrict risky settings by default:

file access OFF

mixed content blocked (or safe default)

universal access from file URLs OFF

Configuration

Android minSdk assumed typical (e.g., 23+), but do not hardcode it in code—keep it in Gradle.

Use minimal external libraries. If you introduce one, explain why and provide an alternative.

3) Architecture & Maintainability Rules (Strict)

Your code MUST follow these rules:

No “one giant file” implementations.

Separate responsibilities clearly:

Flutter (example split)

webview_screen.dart → UI layer only

webview_controller.dart → state + event handling (loading/errors/navigation)

native_webview_bridge.dart → platform channel / platform view wrapper

models/ → typed events, config objects

Android/Kotlin (example split)

FlutterWebViewFactory (if PlatformView) OR WebViewActivity (if MethodChannel approach)

WebViewConfigurator → owns settings and safe defaults

WebViewClientImpl → navigation + loading + error callbacks

WebChromeClientImpl → optional progress/title handling

ChannelMessenger → sending structured events to Flutter

Naming must be explicit and intention-revealing.

Avoid abbreviations and “clever” shortcuts.

Add only minimal comments, but each file must start with a short 2–3 line header explaining its responsibility.

Avoid magic strings/numbers → extract constants.

Use typed event models:

Kotlin: sealed class for events

Dart: sealed class / freezed-style pattern if not using packages, use enums + classes

4) Implementation Choice (You MUST Decide)

Choose exactly ONE of the following and implement it fully:

PlatformView (AndroidView): render native WebView directly inside Flutter widget tree

MethodChannel + Native Activity: open a dedicated native WebView screen from Flutter

Before coding, explain:

why you chose it

pros/cons

how it impacts performance and maintenance

5) Output Format (Very Important)

Return the answer in this exact structure:

Design Overview

chosen approach

file/folder tree

data flow diagram (text-based is fine)

Flutter Code (complete)

show each file separately with clear filenames

Android/Kotlin Code (complete)

show each file separately with clear filenames

Android Setup

AndroidManifest.xml changes

build.gradle changes

required permissions

Junior-Friendly Pitfalls Checklist

common mistakes and how to avoid them

Extension Points

JS bridge

cookies/session handling

file upload / camera

deep links / external intents

authentication flows

6) Code Quality Requirements

Null-safety and defensive coding

Do not swallow exceptions—log consistently and send meaningful error events to Flutter

Consistent logging tags

Clear event payloads (no ad-hoc maps; define schemas/models)

Prefer small functions and single responsibility

7) Result Expectation

Deliver code that can be copied into a fresh Flutter project and run, with only minimal configuration required.

Now produce the complete implementation following all constraints above.