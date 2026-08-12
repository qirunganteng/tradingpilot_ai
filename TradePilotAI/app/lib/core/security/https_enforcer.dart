/// PRD 3.2.1 "HTTPS Only" + 3.2.2 "TLS 1.3".
///
/// CONSTITUTION.md marks Connection & Transport Security as *non-negotiable*.
/// Two things live here:
///
/// 1. [HttpsEnforcer] -- rewrites any explicit `http://` navigation to
///    `https://` before it ever reaches the WebView (client-side upgrade,
///    matching Chrome's own "Always Use Secure Connections").
/// 2. A note on TLS 1.3 / certificate validation: those are **not**
///    reimplemented here. Per CONSTITUTION.md's WEBVIEW ENGINE section,
///    TradePilot deliberately has no second WebView/TLS stack of its own --
///    flutter_inappwebview delegates transport security entirely to the
///    platform's native engine (Chromium on Windows/Android, WebKit on
///    iOS/macOS), which negotiates TLS 1.3 and validates the certificate
///    chain/expiry/hostname/revocation on every connection by default. That
///    is the correct behavior: reimplementing TLS or cert-chain validation
///    in Dart would be both redundant and a *weaker* guarantee than the
///    native engine's own, battle-tested stack.
class HttpsEnforcer {
  const HttpsEnforcer._();

  /// Rewrites a bare `http://` URL to `https://`. Leaves everything else
  /// (already-https, custom schemes, search queries resolved elsewhere)
  /// untouched.
  static String enforceHttps(String url) {
    if (url.startsWith('http://')) {
      return url.replaceFirst('http://', 'https://');
    }
    return url;
  }

  /// True if [url] is not on an encrypted transport we understand. Used to
  /// drive the address bar's lock/info icon and any "Not secure" warning
  /// chip.
  static bool isInsecure(String url) {
    return url.startsWith('http://');
  }
}
