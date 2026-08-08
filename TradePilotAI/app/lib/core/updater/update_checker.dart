import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:url_launcher/url_launcher.dart';

/// Fase 8 -- lightweight auto-updater for the desktop/web builds.
///
/// TradePilot doesn't ship through an app store with its own update
/// mechanism, so this checks GitHub Releases directly: compare the running
/// app's version (from pubspec.yaml, via package_info_plus) against the
/// latest published release tag, and offer a one-click link to the
/// download page when a newer one exists. Silent no-op on any network
/// failure -- an update check should never block or crash the app.
class UpdateInfo {
  final String latestVersion;
  final String currentVersion;
  final String releaseUrl;
  final String releaseNotes;
  const UpdateInfo({
    required this.latestVersion,
    required this.currentVersion,
    required this.releaseUrl,
    required this.releaseNotes,
  });
}

const String kUpdateRepoOwner = 'qirunganteng';
const String kUpdateRepoName = 'tradingpilot_ai';

class UpdateChecker {
  static Future<UpdateInfo?> checkForUpdate() async {
    try {
      final info = await PackageInfo.fromPlatform();
      final currentVersion = info.version;

      final dio = Dio(BaseOptions(connectTimeout: const Duration(seconds: 8), receiveTimeout: const Duration(seconds: 8)));
      final response = await dio.get<Map<String, dynamic>>(
        'https://api.github.com/repos/$kUpdateRepoOwner/$kUpdateRepoName/releases/latest',
        options: Options(headers: {'Accept': 'application/vnd.github+json'}),
      );

      final json = response.data;
      if (json == null) return null;

      final tagName = (json['tag_name'] as String? ?? '').replaceFirst(RegExp('^v'), '');
      final htmlUrl = json['html_url'] as String? ?? '';
      final body = json['body'] as String? ?? '';

      if (tagName.isEmpty || !_isNewer(tagName, currentVersion)) return null;

      return UpdateInfo(
        latestVersion: tagName,
        currentVersion: currentVersion,
        releaseUrl: htmlUrl.isNotEmpty
            ? htmlUrl
            : 'https://github.com/$kUpdateRepoOwner/$kUpdateRepoName/releases/latest',
        releaseNotes: body,
      );
    } catch (_) {
      // Offline, rate-limited, 404 (no releases yet) -- fine, just skip
      // the update check silently.
      return null;
    }
  }

  /// Simple semver-ish comparison: "1.2.3" > "1.2.0". Falls back to string
  /// inequality if either side isn't in a clean numeric-dot form.
  static bool _isNewer(String latest, String current) {
    final l = latest.split('+').first.split('.').map((s) => int.tryParse(s) ?? 0).toList();
    final c = current.split('+').first.split('.').map((s) => int.tryParse(s) ?? 0).toList();
    for (var i = 0; i < l.length || i < c.length; i++) {
      final lv = i < l.length ? l[i] : 0;
      final cv = i < c.length ? c[i] : 0;
      if (lv != cv) return lv > cv;
    }
    return false;
  }
}

/// Riverpod provider so the check only ever runs once per app session and
/// any widget can watch its result.
final updateInfoProvider = FutureProvider<UpdateInfo?>((ref) => UpdateChecker.checkForUpdate());

/// Small dismissible banner shown at the top of the workspace when a newer
/// release is available. Non-blocking -- the person can keep working and
/// dismiss it, or click through to the GitHub release page to grab it.
class UpdateBanner extends ConsumerStatefulWidget {
  const UpdateBanner({super.key});

  @override
  ConsumerState<UpdateBanner> createState() => _UpdateBannerState();
}

class _UpdateBannerState extends ConsumerState<UpdateBanner> {
  bool _dismissed = false;

  @override
  Widget build(BuildContext context) {
    if (_dismissed) return const SizedBox.shrink();
    final updateAsync = ref.watch(updateInfoProvider);

    return updateAsync.when(
      data: (info) {
        if (info == null) return const SizedBox.shrink();
        return Container(
          color: Colors.blueAccent.withValues(alpha: 0.15),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          child: Row(
            children: [
              const Icon(Icons.system_update, size: 15, color: Colors.blueAccent),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'TradePilot ${info.latestVersion} is available (you\'re on ${info.currentVersion}).',
                  style: const TextStyle(fontSize: 12),
                ),
              ),
              TextButton(
                onPressed: () => launchUrl(Uri.parse(info.releaseUrl), mode: LaunchMode.externalApplication),
                child: const Text('Download', style: TextStyle(fontSize: 12)),
              ),
              IconButton(
                icon: const Icon(Icons.close, size: 15),
                onPressed: () => setState(() => _dismissed = true),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(minWidth: 26, minHeight: 26),
              ),
            ],
          ),
        );
      },
      loading: () => const SizedBox.shrink(),
      error: (_, __) => const SizedBox.shrink(),
    );
  }
}
