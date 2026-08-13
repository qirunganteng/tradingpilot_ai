import 'dart:io' show Platform;
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

/// PRD 3.3.2 "Tracking Protection" -- blocks network requests to a
/// baseline list of the most common ad/tracker domains via
/// flutter_inappwebview's native `ContentBlocker`. Applied to every tab,
/// always on -- but **only where the underlying engine actually supports
/// it**.
///
/// `ContentBlocker`/`ContentBlockerActionType` are backed by WKWebView's
/// content-rule-list API (iOS/macOS) and Android's request-interception
/// equivalent -- there is no WebView2 (Windows) or WebKitGTK (Linux)
/// counterpart, and critically, even just *constructing*
/// `ContentBlockerActionType.BLOCK` throws `type 'Null' is not a subtype
/// of type 'String'` on those platforms (the enum's internal per-platform
/// value table has no entry for them) -- this isn't a "feature quietly
/// does nothing" gap, it's a hard crash at construction time, before a
/// WebView is even involved. So [buildTrackerContentBlockers] must never
/// construct a `ContentBlocker` at all outside iOS/macOS/Android; an empty
/// list there is the correct, deliberate behavior, not a fallback -- see
/// docs/known-limitations.md for the tracking-protection gap this leaves
/// on Windows/Linux/web.
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

bool get _supportsContentBlockers =>
    !kIsWeb && (Platform.isIOS || Platform.isMacOS || Platform.isAndroid);

List<ContentBlocker> buildTrackerContentBlockers() {
  if (!_supportsContentBlockers) return const [];
  return kTrackerDomains.map((domain) {
    return ContentBlocker(
      trigger: ContentBlockerTrigger(urlFilter: '.*$domain.*'),
      action: ContentBlockerAction(type: ContentBlockerActionType.BLOCK),
    );
  }).toList();
}
