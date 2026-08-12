# Known limitations

Per CONSTITUTION.md, a PRD item that isn't implemented must be recorded here
explicitly rather than silently skipped. This file tracks the browser-engine
/ security items from `TradePilot_AI_PRD_SSD.pdf` that are intentionally not
(yet) implemented, why, and what a real implementation would need.

## PRD 2.2.17 "Screen Recorder"

**Status:** not implemented.

**Why:** real screen-video recording is a fundamentally different problem
per platform -- Windows needs Desktop Duplication/Media Foundation, Android
needs `MediaProjection`, macOS needs `ScreenCaptureKit`, and there's no
single Flutter plugin that wraps all of them to a common, actively
maintained API today. The PRD's own dependency list (`record` +
`ffmpeg_kit_flutter`) doesn't actually solve this either: `record` captures
*audio*, not video, and `ffmpeg_kit_flutter` was deprecated/pulled from
pub.dev in 2025, would need to be forked, and still wouldn't supply the
missing platform-native *capture* step -- only encoding once you already
have frames. Shipping a "screen recorder" built on packages that don't
record the screen would be worse than not having the feature: it would
silently produce empty or corrupt output.

**What a real implementation needs:** per-platform native capture
(`flutter_screen_recording` or a hand-rolled platform channel per OS),
wired through a shared Dart-side `ScreenRecorderManager` interface so the
rest of the app (the toolbar button, the recording indicator) doesn't need
to know which platform it's on. This is native-code work, not something
safely done as a drive-by addition alongside the rest of the browser engine.

## PRD 2.2.18 "Sync" (cross-device)

**Status:** not implemented (client-side hooks exist, no backend to call).

**Why:** Sync is explicitly PRD Phase 4 ("Advanced Features", after AI
Integration) in the roadmap (§15.4), and requires backend routes
(`POST /api/v1/browser/sync`) that don't exist yet in
`backend/workers/api-gateway`. Building a sync *client* against a sync
endpoint that doesn't exist would mean either mocking the backend (giving a
false sense of the feature working) or leaving the client half-wired and
untestable.

**What exists today in its place:** local persistence for every syncable
data type (workspaces, tabs/sessions, bookmarks, history, passwords,
site permissions, downloads) already goes through a dedicated manager
(`WorkspaceManager`, `SessionManager`, `HistoryManager`, `PasswordVault`,
`PermissionManager`, `DownloadManager` -- all in
`lib/features/browser_core/services/`), each already serializing to/from
JSON. That's deliberate: it means wiring real sync later is "add a
`POST`/`GET` call around this JSON on each manager", not "invent a
serialization format from scratch."

## PRD 3.2.4 "Certificate Pinning" -- scope note

**Status:** implemented, but only for TradePilot's own backend domains, not
arbitrary browsed sites.

This isn't a gap so much as a deliberate scope boundary worth documenting:
see the header comment in
`lib/core/network/certificate_pinning.dart` for the reasoning (pinning the
public web breaks the moment any site rotates its certificate, which is
routine and expected there). The mechanism is real and wired into the
shared Dio client (`lib/core/network/api_client.dart`); it is currently
inert because `CertificatePinningConfig.kPinnedPublicKeyHashes` is
intentionally empty until the production Cloudflare Worker's certificate
pins are known at deploy time.

## PRD 3.3.4 "DNS over HTTPS"

**Status:** not implemented at the app level.

**Why:** flutter_inappwebview doesn't expose a DNS resolver override --
DNS-over-HTTPS is configured at the OS/network-stack level (Windows 11 and
Android both support enabling DoH system-wide), not something a Flutter
app can safely intercept for a single embedded WebView without a custom
native network stack (which flutter_inappwebview deliberately avoids by
design -- see the note in `https_enforcer.dart` about not reimplementing
the platform's transport layer). The practical mitigation already in place
is that PRD 3.2.1/3.2.2 (HTTPS-only + TLS 1.3) mean the *content* of every
request is encrypted end-to-end even if the DNS lookup that resolved the
hostname wasn't -- DoH would additionally hide *which* hostnames are being
looked up from a network observer, which is a narrower privacy property.

**What a real implementation needs:** platform-channel code per OS to
either (a) point the app's own resolver at a DoH endpoint (Windows) or (b)
prompt the user to enable Android's built-in "Private DNS" setting, since
neither is reachable through Flutter/Dart alone.
