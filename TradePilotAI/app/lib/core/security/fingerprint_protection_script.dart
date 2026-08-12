/// PRD 3.3.3 "Fingerprint Protection" -- a lightweight, always-on script
/// injected before every page's own scripts run. Real anti-fingerprinting
/// is a deep rabbit hole (canvas/audio/WebGL noise, timing attacks, etc.);
/// this covers the handful of cheap, high-signal checks sites commonly use
/// first (navigator.webdriver, a suspiciously-empty plugins list) without
/// touching canvas/WebGL output, which risks visibly breaking charting
/// libraries like TradingView that this app depends on rendering
/// correctly.
const String kFingerprintProtectionScript = '''
(function() {
  try {
    Object.defineProperty(navigator, 'webdriver', { get: () => false });
    if (navigator.plugins && navigator.plugins.length === 0) {
      Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3] });
    }
  } catch (e) {}
})();
''';
