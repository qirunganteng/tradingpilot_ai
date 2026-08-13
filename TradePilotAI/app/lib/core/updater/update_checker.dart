import 'dart:io' show Platform;
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:url_launcher/url_launcher.dart';
import 'self_updater.dart';

/// Fase 8 -- auto-updater for the desktop/mobile builds.
///
/// TradePilot doesn't ship through an app store with its own update
/// mechanism, so this checks GitHub Releases directly: compare the running
/// app's version (from pubspec.yaml, via package_info_plus) against the
/// latest published release tag, and -- where [SelfUpdater.isSupported] --
/// download and install the update in-app (see self_updater.dart) rather
/// than just linking out to a page the person then has to act on
/// themselves. Silent no-op on any network failure -- an update check
/// should never block or crash the app.
class UpdateInfo {
  final String latestVersion;
  final String currentVersion;
  final String releaseUrl;
  final String releaseNotes;
  final String? assetDownloadUrl;
  const UpdateInfo({
    required this.latestVersion,
    required this.currentVersion,
    required this.releaseUrl,
    required this.releaseNotes,
    required this.assetDownloadUrl,
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

      final assets = (json['assets'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
      String? assetUrl;
      if (!kIsWeb) {
        if (Platform.isWindows) {
          assetUrl = _findAsset(assets, (name) => name.contains('windows'));
        } else if (Platform.isAndroid) {
          assetUrl = _findAsset(assets, (name) => name.endsWith('.apk'));
        }
      }

      return UpdateInfo(
        latestVersion: tagName,
        currentVersion: currentVersion,
        releaseUrl: htmlUrl.isNotEmpty
            ? htmlUrl
            : 'https://github.com/$kUpdateRepoOwner/$kUpdateRepoName/releases/latest',
        releaseNotes: body,
        assetDownloadUrl: assetUrl,
      );
    } catch (_) {
      // Offline, rate-limited, 404 (no releases yet) -- fine, just skip
      // the update check silently.
      return null;
    }
  }

  static String? _findAsset(List<Map<String, dynamic>> assets, bool Function(String lowerName) matches) {
    for (final asset in assets) {
      final name = (asset['name'] as String? ?? '').toLowerCase();
      if (matches(name)) return asset['browser_download_url'] as String?;
    }
    return null;
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
/// dismiss it. Where in-app self-update is available for this platform and
/// a matching release asset was found, "Update Now" downloads and installs
/// it directly; otherwise it falls back to opening the GitHub release page.
class UpdateBanner extends ConsumerStatefulWidget {
  const UpdateBanner({super.key});

  @override
  ConsumerState<UpdateBanner> createState() => _UpdateBannerState();
}

class _UpdateBannerState extends ConsumerState<UpdateBanner> {
  bool _dismissed = false;
  bool _updating = false;
  final ValueNotifier<(double, String)> _progressNotifier = ValueNotifier((0.0, 'Downloading update\u2026'));

  @override
  void dispose() {
    _progressNotifier.dispose();
    super.dispose();
  }

  Future<void> _startSelfUpdate(UpdateInfo info) async {
    setState(() => _updating = true);
    _progressNotifier.value = (0.0, 'Downloading update\u2026');

    if (!mounted) return;
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: const Text('Updating TradePilot', style: TextStyle(fontSize: 15)),
        content: SizedBox(
          width: 300,
          child: ValueListenableBuilder<(double, String)>(
            valueListenable: _progressNotifier,
            builder: (context, value, _) {
              final (progress, phase) = value;
              return Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(phase, style: const TextStyle(fontSize: 12.5)),
                  const SizedBox(height: 12),
                  LinearProgressIndicator(value: progress > 0 ? progress : null, minHeight: 4),
                  const SizedBox(height: 8),
                  Text(
                    !kIsWeb && Platform.isWindows
                        ? 'TradePilot will restart automatically when this finishes.'
                        : 'Confirm the install prompt when it appears.',
                    style: TextStyle(fontSize: 11, color: Colors.grey[500]),
                  ),
                ],
              );
            },
          ),
        ),
      ),
    );

    try {
      await SelfUpdater.downloadAndInstall(
        info.assetDownloadUrl!,
        onProgress: (p, ph) => _progressNotifier.value = (p, ph),
      );
      // Windows: the process exits inside downloadAndInstall on success,
      // so nothing below this line runs there. Android: OpenFilex.open()
      // hands off to the system installer and returns -- close our
      // dialog and let the person finish the OS install prompt.
      if (mounted) Navigator.of(context, rootNavigator: true).pop();
    } catch (e) {
      if (mounted) Navigator.of(context, rootNavigator: true).pop();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Update failed: opening the release page instead.')),
        );
        launchUrl(Uri.parse(info.releaseUrl), mode: LaunchMode.externalApplication);
      }
    } finally {
      if (mounted) setState(() => _updating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_dismissed) return const SizedBox.shrink();
    final updateAsync = ref.watch(updateInfoProvider);

    return updateAsync.when(
      data: (info) {
        if (info == null) return const SizedBox.shrink();
        final canSelfUpdate = !kIsWeb && SelfUpdater.isSupported && info.assetDownloadUrl != null;
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
                onPressed: _updating
                    ? null
                    : () => canSelfUpdate
                        ? _startSelfUpdate(info)
                        : launchUrl(Uri.parse(info.releaseUrl), mode: LaunchMode.externalApplication),
                child: Text(canSelfUpdate ? 'Update now' : 'Download', style: const TextStyle(fontSize: 12)),
              ),
              IconButton(
                icon: const Icon(Icons.close, size: 15),
                onPressed: _updating ? null : () => setState(() => _dismissed = true),
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
