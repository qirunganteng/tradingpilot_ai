/// PRD 3.2.4 "Certificate Pinning" -- CONSTITUTION.md marks this WAJIB
/// specifically for TradePilot's *own* backend domains (Cloudflare
/// Workers), not third-party sites browsed in a tab. Pinning arbitrary
/// browsed sites would break the moment any of them rotate a cert (which
/// happens constantly and is exactly why browsers don't pin the public
/// web) -- pinning is only sound for a small, first-party domain set you
/// control the release cadence of.
///
/// How this is enforced:
/// - [kPinnedBackendHosts] is the allowlist of TradePilot-owned hosts this
///   applies to.
/// - [kPinnedPublicKeyHashes] holds the expected SHA-256 SPKI pins for
///   those hosts, **base64-encoded**, in the same format `openssl x509
///   -pubkey | openssl pkey -pubin -outform der | openssl dgst -sha256
///   -binary | base64` produces. Populate this at deploy time once the
///   production Worker's certificate is issued -- shipping a placeholder
///   pin would either do nothing (if left empty, which is the current,
///   honest state) or brick every connection (if filled with a fake
///   value), so this intentionally ships *empty* rather than fabricated.
/// - [isPinningConfigured] lets the network layer (Dio interceptor) and
///   the embedded WebView both check "do we actually have pins to
///   enforce yet?" before trying to enforce anything.
///
/// Enforcement point: TradePilot's own API traffic goes through Dio
/// (lib/core/network/api_client.dart), not the WebView -- browsed tabs
/// never talk to `*.tradepilot.workers.dev` directly. So pinning is wired
/// as a `BadCertificateCallback`/`SecurityContext` check in the Dio client
/// once [kPinnedPublicKeyHashes] is non-empty, not in InAppWebView's
/// `onReceivedServerTrustAuthRequest` (which governs browsed sites, and
/// per PRD 3.2.3 those already get standard certificate validation from
/// the platform's native engine -- see https_enforcer.dart).
class CertificatePinningConfig {
  const CertificatePinningConfig._();

  static const List<String> kPinnedBackendHosts = [
    // e.g. 'tradepilot-ai.<account>.workers.dev'
  ];

  /// host -> list of acceptable base64 SHA-256 SPKI pins (include the
  /// current leaf *and* a backup/intermediate pin so a routine cert
  /// rotation doesn't lock out every client -- see RFC 7469 §2.5).
  static const Map<String, List<String>> kPinnedPublicKeyHashes = {};

  static bool get isPinningConfigured => kPinnedPublicKeyHashes.isNotEmpty;
}
