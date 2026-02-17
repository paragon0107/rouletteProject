import Flutter
import UIKit
import WebKit

class SceneDelegate: FlutterSceneDelegate {
  private enum ChannelConstants {
    static let channelName = "com.example.app/webview"
    static let openMethod = "open"
    static let onWebViewEventMethod = "onWebViewEvent"
  }

  private var webViewChannel: FlutterMethodChannel?

  override func scene(
    _ scene: UIScene,
    willConnectTo session: UISceneSession,
    options connectionOptions: UIScene.ConnectionOptions
  ) {
    super.scene(scene, willConnectTo: session, options: connectionOptions)
    configureWebViewChannelIfNeeded()
  }

  override func sceneDidBecomeActive(_ scene: UIScene) {
    super.sceneDidBecomeActive(scene)
    configureWebViewChannelIfNeeded()
  }

  private func configureWebViewChannelIfNeeded() {
    guard webViewChannel == nil,
      let flutterViewController = window?.rootViewController as? FlutterViewController
    else {
      return
    }

    let channel = FlutterMethodChannel(
      name: ChannelConstants.channelName,
      binaryMessenger: flutterViewController.binaryMessenger
    )

    channel.setMethodCallHandler { [weak self] call, result in
      self?.handleMethodCall(call: call, result: result)
    }

    webViewChannel = channel
  }

  private func handleMethodCall(call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case ChannelConstants.openMethod:
      guard
        let arguments = call.arguments as? [String: Any],
        let rawUrl = arguments["url"] as? String,
        let url = URL(string: rawUrl),
        let scheme = url.scheme?.lowercased(),
        scheme == "http" || scheme == "https"
      else {
        result(
          FlutterError(
            code: "INVALID_URL",
            message: "A valid http/https URL is required.",
            details: nil
          )
        )
        return
      }

      presentNativeWebView(with: url)
      result(nil)
    default:
      result(FlutterMethodNotImplemented)
    }
  }

  private func presentNativeWebView(with url: URL) {
    guard let rootViewController = window?.rootViewController else {
      return
    }

    let topViewController = topViewController(from: rootViewController)

    if let navigationController = topViewController as? UINavigationController,
      let existingWebViewController = navigationController.viewControllers.first
        as? NativeWebViewViewController
    {
      existingWebViewController.load(url: url)
      return
    }

    if let existingWebViewController = topViewController as? NativeWebViewViewController {
      existingWebViewController.load(url: url)
      return
    }

    let webViewController = NativeWebViewViewController(initialURL: url) { [weak self] event in
      self?.sendWebViewEvent(event)
    }

    let navigationController = UINavigationController(rootViewController: webViewController)
    navigationController.modalPresentationStyle = .fullScreen
    topViewController.present(navigationController, animated: true)
  }

  private func sendWebViewEvent(_ event: WebViewEventPayload) {
    guard let channel = webViewChannel, let payload = event.toJSONString() else {
      return
    }

    channel.invokeMethod(ChannelConstants.onWebViewEventMethod, arguments: payload)
  }

  private func topViewController(from rootViewController: UIViewController) -> UIViewController {
    var currentViewController = rootViewController
    while let presentedViewController = currentViewController.presentedViewController {
      currentViewController = presentedViewController
    }
    return currentViewController
  }
}

private struct WebViewEventPayload {
  let type: String
  let data: [String: Any]

  static func pageStarted(url: String) -> WebViewEventPayload {
    return WebViewEventPayload(type: "onPageStarted", data: ["url": url])
  }

  static func pageFinished(url: String) -> WebViewEventPayload {
    return WebViewEventPayload(type: "onPageFinished", data: ["url": url])
  }

  static func error(errorCode: Int, description: String, failingUrl: String) -> WebViewEventPayload {
    return WebViewEventPayload(
      type: "onError",
      data: [
        "errorCode": errorCode,
        "description": description,
        "failingUrl": failingUrl,
      ]
    )
  }

  func toJSONString() -> String? {
    let payload: [String: Any] = ["type": type, "data": data]
    guard JSONSerialization.isValidJSONObject(payload),
      let jsonData = try? JSONSerialization.data(withJSONObject: payload),
      let jsonString = String(data: jsonData, encoding: .utf8)
    else {
      return nil
    }
    return jsonString
  }
}

private final class NativeWebViewViewController: UIViewController, WKNavigationDelegate {
  private enum UIConstants {
    static let title = "룰렛"
    static let closeButtonTitle = "닫기"
    static let backButtonTitle = "뒤로"
    static let errorTitle = "페이지를 불러올 수 없어요"
    static let retryButtonTitle = "재시도"
    static let unknownRetryErrorMessage = "재시도할 주소를 찾을 수 없습니다."
  }

  private let initialURL: URL
  private let sendEvent: (WebViewEventPayload) -> Void
  private let webView: WKWebView

  private let loadingOverlay = UIView()
  private let loadingIndicator = UIActivityIndicatorView(style: .large)
  private let errorOverlay = UIView()
  private let errorMessageLabel = UILabel()

  private var lastRequestedURL: URL?
  private var hasMainFrameLoadError = false

  init(initialURL: URL, sendEvent: @escaping (WebViewEventPayload) -> Void) {
    self.initialURL = initialURL
    self.sendEvent = sendEvent

    let webViewPreferences = WKPreferences()
    webViewPreferences.javaScriptEnabled = true
    webViewPreferences.javaScriptCanOpenWindowsAutomatically = false

    let configuration = WKWebViewConfiguration()
    configuration.preferences = webViewPreferences
    configuration.websiteDataStore = .default()

    self.webView = WKWebView(frame: .zero, configuration: configuration)
    super.init(nibName: nil, bundle: nil)
  }

  @available(*, unavailable)
  required init?(coder: NSCoder) {
    return nil
  }

  deinit {
    webView.navigationDelegate = nil
  }

  override func viewDidLoad() {
    super.viewDidLoad()

    title = UIConstants.title
    view.backgroundColor = .systemBackground

    configureNavigationItems()
    configureWebView()
    configureLoadingOverlay()
    configureErrorOverlay()

    load(url: initialURL)
  }

  func load(url: URL) {
    lastRequestedURL = url
    hasMainFrameLoadError = false
    hideErrorOverlay()
    showLoadingIndicator()
    updateLeftNavigationButton()
    webView.load(URLRequest(url: url))
  }

  private func configureNavigationItems() {
    navigationItem.largeTitleDisplayMode = .never
    navigationItem.rightBarButtonItem = UIBarButtonItem(
      barButtonSystemItem: .refresh,
      target: self,
      action: #selector(retryLoading)
    )
    updateLeftNavigationButton()
  }

  private func configureWebView() {
    webView.navigationDelegate = self
    webView.allowsBackForwardNavigationGestures = true
    webView.translatesAutoresizingMaskIntoConstraints = false
    view.addSubview(webView)

    NSLayoutConstraint.activate([
      webView.topAnchor.constraint(equalTo: view.topAnchor),
      webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
    ])
  }

  private func configureLoadingOverlay() {
    loadingOverlay.backgroundColor = UIColor(white: 1.0, alpha: 0.45)
    loadingOverlay.isHidden = true
    loadingOverlay.isUserInteractionEnabled = true
    loadingOverlay.translatesAutoresizingMaskIntoConstraints = false

    loadingIndicator.hidesWhenStopped = true
    loadingIndicator.translatesAutoresizingMaskIntoConstraints = false

    loadingOverlay.addSubview(loadingIndicator)
    view.addSubview(loadingOverlay)

    NSLayoutConstraint.activate([
      loadingOverlay.topAnchor.constraint(equalTo: view.topAnchor),
      loadingOverlay.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      loadingOverlay.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      loadingOverlay.bottomAnchor.constraint(equalTo: view.bottomAnchor),
      loadingIndicator.centerXAnchor.constraint(equalTo: loadingOverlay.centerXAnchor),
      loadingIndicator.centerYAnchor.constraint(equalTo: loadingOverlay.centerYAnchor),
    ])
  }

  private func configureErrorOverlay() {
    errorOverlay.backgroundColor = .white
    errorOverlay.isHidden = true
    errorOverlay.translatesAutoresizingMaskIntoConstraints = false

    let titleLabel = UILabel()
    titleLabel.text = UIConstants.errorTitle
    titleLabel.font = UIFont.systemFont(ofSize: 22, weight: .bold)
    titleLabel.textColor = UIColor(red: 0.07, green: 0.09, blue: 0.16, alpha: 1.0)
    titleLabel.textAlignment = .center
    titleLabel.numberOfLines = 0

    errorMessageLabel.font = UIFont.systemFont(ofSize: 15, weight: .regular)
    errorMessageLabel.textColor = UIColor(red: 0.42, green: 0.45, blue: 0.50, alpha: 1.0)
    errorMessageLabel.textAlignment = .center
    errorMessageLabel.numberOfLines = 0

    let retryButton = UIButton(type: .system)
    retryButton.setTitle(UIConstants.retryButtonTitle, for: .normal)
    retryButton.addTarget(self, action: #selector(retryLoading), for: .touchUpInside)

    let stackView = UIStackView(arrangedSubviews: [titleLabel, errorMessageLabel, retryButton])
    stackView.axis = .vertical
    stackView.alignment = .center
    stackView.spacing = 14
    stackView.translatesAutoresizingMaskIntoConstraints = false

    errorOverlay.addSubview(stackView)
    view.addSubview(errorOverlay)

    NSLayoutConstraint.activate([
      errorOverlay.topAnchor.constraint(equalTo: view.topAnchor),
      errorOverlay.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      errorOverlay.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      errorOverlay.bottomAnchor.constraint(equalTo: view.bottomAnchor),
      stackView.centerXAnchor.constraint(equalTo: errorOverlay.centerXAnchor),
      stackView.centerYAnchor.constraint(equalTo: errorOverlay.centerYAnchor),
      stackView.leadingAnchor.constraint(greaterThanOrEqualTo: errorOverlay.leadingAnchor, constant: 24),
      stackView.trailingAnchor.constraint(lessThanOrEqualTo: errorOverlay.trailingAnchor, constant: -24),
    ])
  }

  @objc private func retryLoading() {
    guard let retryURL = resolveRetryURL() else {
      showErrorOverlay(errorCode: -1, description: UIConstants.unknownRetryErrorMessage, failingUrl: "")
      return
    }

    hasMainFrameLoadError = false
    hideErrorOverlay()
    showLoadingIndicator()

    if webView.url == retryURL {
      webView.reload()
      return
    }

    load(url: retryURL)
  }

  @objc private func handleLeftNavigationButtonTap() {
    if webView.canGoBack {
      webView.goBack()
      return
    }

    dismiss(animated: true)
  }

  private func updateLeftNavigationButton() {
    let title = webView.canGoBack ? UIConstants.backButtonTitle : UIConstants.closeButtonTitle
    navigationItem.leftBarButtonItem = UIBarButtonItem(
      title: title,
      style: .plain,
      target: self,
      action: #selector(handleLeftNavigationButtonTap)
    )
  }

  private func resolveRetryURL() -> URL? {
    if let currentURL = webView.url, !currentURL.absoluteString.isEmpty,
      !currentURL.absoluteString.lowercased().elementsEqual("about:blank")
    {
      return currentURL
    }

    if let lastRequestedURL = lastRequestedURL {
      return lastRequestedURL
    }

    return initialURL
  }

  private func showLoadingIndicator() {
    if loadingOverlay.isHidden {
      loadingOverlay.isHidden = false
    }
    loadingIndicator.startAnimating()
  }

  private func hideLoadingIndicator() {
    if !loadingOverlay.isHidden {
      loadingOverlay.isHidden = true
    }
    loadingIndicator.stopAnimating()
  }

  private func hideErrorOverlay() {
    if !errorOverlay.isHidden {
      errorOverlay.isHidden = true
    }
  }

  private func showErrorOverlay(errorCode: Int, description: String, failingUrl: String) {
    hasMainFrameLoadError = true
    hideLoadingIndicator()
    errorMessageLabel.text = buildErrorMessage(errorCode: errorCode, description: description)
    errorOverlay.isHidden = false
    sendEvent(
      .error(errorCode: errorCode, description: description, failingUrl: failingUrl)
    )
  }

  private func buildErrorMessage(errorCode: Int, description: String) -> String {
    switch errorCode {
    case NSURLErrorCannotFindHost,
      NSURLErrorCannotConnectToHost,
      NSURLErrorTimedOut,
      NSURLErrorNetworkConnectionLost,
      NSURLErrorDNSLookupFailed,
      NSURLErrorNotConnectedToInternet,
      NSURLErrorUserAuthenticationRequired:
      return "인터넷 연결 상태를 확인한 후 다시 시도해주세요."
    case NSURLErrorSecureConnectionFailed,
      NSURLErrorServerCertificateUntrusted,
      NSURLErrorServerCertificateHasBadDate,
      NSURLErrorServerCertificateHasUnknownRoot,
      NSURLErrorServerCertificateNotYetValid,
      NSURLErrorClientCertificateRejected,
      NSURLErrorClientCertificateRequired:
      return "보안 연결에 실패했습니다. 잠시 후 다시 시도해주세요."
    case 400...599:
      return "서버 응답에 문제가 발생했습니다. 잠시 후 다시 시도해주세요. (HTTP \(errorCode))"
    default:
      if description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
        return "페이지를 불러오지 못했습니다. 다시 시도해주세요."
      }
      return "페이지를 불러오지 못했습니다.\n\(description)"
    }
  }

  private func handleNavigationError(_ error: Error) {
    let nsError = error as NSError
    if nsError.code == NSURLErrorCancelled {
      return
    }

    let failingUrl = extractFailingURL(from: nsError)
    showErrorOverlay(
      errorCode: nsError.code,
      description: nsError.localizedDescription,
      failingUrl: failingUrl
    )
    updateLeftNavigationButton()
  }

  private func extractFailingURL(from error: NSError) -> String {
    if let failingURLString = error.userInfo[NSURLErrorFailingURLStringErrorKey] as? String,
      !failingURLString.isEmpty
    {
      return failingURLString
    }

    if let failingURL = error.userInfo[NSURLErrorFailingURLErrorKey] as? URL {
      return failingURL.absoluteString
    }

    return lastRequestedURL?.absoluteString ?? ""
  }

  func webView(
    _ webView: WKWebView,
    decidePolicyFor navigationAction: WKNavigationAction,
    decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
  ) {
    if let requestURL = navigationAction.request.url {
      lastRequestedURL = requestURL
    }
    decisionHandler(.allow)
  }

  func webView(
    _ webView: WKWebView,
    decidePolicyFor navigationResponse: WKNavigationResponse,
    decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void
  ) {
    guard navigationResponse.isForMainFrame,
      let response = navigationResponse.response as? HTTPURLResponse
    else {
      decisionHandler(.allow)
      return
    }

    let statusCode = response.statusCode
    guard (400...599).contains(statusCode) else {
      decisionHandler(.allow)
      return
    }

    let failingUrl = response.url?.absoluteString ?? lastRequestedURL?.absoluteString ?? ""
    showErrorOverlay(
      errorCode: statusCode,
      description: "HTTP \(statusCode)",
      failingUrl: failingUrl
    )
    updateLeftNavigationButton()
    decisionHandler(.cancel)
  }

  func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
    hasMainFrameLoadError = false
    hideErrorOverlay()
    showLoadingIndicator()

    let url = webView.url?.absoluteString ?? lastRequestedURL?.absoluteString ?? ""
    sendEvent(.pageStarted(url: url))
    updateLeftNavigationButton()
  }

  func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
    if !hasMainFrameLoadError {
      hideLoadingIndicator()
      hideErrorOverlay()
    }

    let url = webView.url?.absoluteString ?? lastRequestedURL?.absoluteString ?? ""
    sendEvent(.pageFinished(url: url))
    updateLeftNavigationButton()
  }

  func webView(
    _ webView: WKWebView,
    didFailProvisionalNavigation navigation: WKNavigation!,
    withError error: Error
  ) {
    handleNavigationError(error)
  }

  func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
    handleNavigationError(error)
  }
}
