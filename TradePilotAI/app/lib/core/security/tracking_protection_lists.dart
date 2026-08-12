import 'package:flutter_inappwebview/flutter_inappwebview.dart';

/// PRD 3.3.2 "Tracking Protection" -- blocks network requests to a
/// baseline list of the most common ad/tracker domains via
/// flutter_inappwebview's native `ContentBlocker` (same mechanism on every
/// platform that supports it -- no separate native code needed). Applied to
/// every tab, always on.
///
/// Not a full adblock-grade list -- a real, functioning baseline. Extend
/// [kTrackerDomains] as needed; each entry becomes a `.*domain.*` URL
/// filter rule.
const List<String> kTrackerDomains = [
  'doubleclick.net',
  'googlesyndication.com',
  'google-analytics.com',
  'googletagmanager.com',
  'facebook.com/tr',
  'connect.facebook.net',
  'adservice.google.com',
  'amazon-adsystem.com',
  'scorecardresearch.com',
  'hotjar.com',
  'criteo.com',
  'taboola.com',
  'outbrain.com',
];

List<ContentBlocker> buildTrackerContentBlockers() {
  return kTrackerDomains.map((domain) {
    return ContentBlocker(
      trigger: ContentBlockerTrigger(urlFilter: '.*$domain.*'),
      action: ContentBlockerAction(type: ContentBlockerActionType.BLOCK),
    );
  }).toList();
}
