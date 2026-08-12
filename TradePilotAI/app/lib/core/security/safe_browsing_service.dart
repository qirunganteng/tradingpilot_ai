/// PRD 3.2.5 "Safe Browsing" -- warns/blocks known phishing & malware
/// destinations before the WebView ever loads them.
///
/// The static [_kBlocklist] below is intentionally small and illustrative
/// (same two example domains the PRD mockup implies). A production
/// deployment should back this with a real, constantly-updated threat-intel
/// feed -- Google's Safe Browsing Lookup API is the natural choice since
/// the AI Gateway backend already proxies third-party APIs -- fetched
/// periodically by `SafeBrowsingService.refreshFromBackend()` and cached
/// locally so a lookup never blocks page navigation on a network round
/// trip. That wiring is a `TODO(backend)`; the enforcement point in the
/// browser (this class + its call site in browser_view.dart) is real and
/// already wired end-to-end.
class SafeBrowsingVerdict {
  final bool isBlocked;
  final String? matchedPattern;
  const SafeBrowsingVerdict({required this.isBlocked, this.matchedPattern});
}

class SafeBrowsingService {
  const SafeBrowsingService._();

  static const List<String> _kBlocklist = [
    'phishing-example.com',
    'malware-test.com',
  ];

  /// Extra domains an admin/user has locally flagged -- kept separate from
  /// the built-in list so [refreshFromBackend] can safely replace one
  /// without clobbering the other.
  static final List<String> _localAdditions = [];

  static SafeBrowsingVerdict check(String host) {
    if (host.isEmpty) return const SafeBrowsingVerdict(isBlocked: false);
    for (final bad in [..._kBlocklist, ..._localAdditions]) {
      if (host == bad || host.endsWith('.$bad')) {
        return SafeBrowsingVerdict(isBlocked: true, matchedPattern: bad);
      }
    }
    return const SafeBrowsingVerdict(isBlocked: false);
  }

  /// TODO(backend): call the Cloudflare AI Gateway's `/api/v1/security/
  /// safe-browsing` (or a dedicated Worker route proxying Google Safe
  /// Browsing) on an interval/app-start and populate [_localAdditions].
  /// Left as a no-op stub so the call site can be wired in browser_view.dart
  /// today without waiting on that backend route to exist.
  static Future<void> refreshFromBackend() async {}

  static void addLocalBlock(String domain) {
    if (!_localAdditions.contains(domain)) _localAdditions.add(domain);
  }
}
