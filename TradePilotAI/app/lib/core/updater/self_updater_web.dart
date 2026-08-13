/// Web has no filesystem to download an installer to and no OS package
/// manager to hand it to -- there is nothing to self-update in-app here.
/// Callers should check [SelfUpdater.isSupported] (false on web) and fall
/// back to opening the browser to the GitHub release page instead.
class SelfUpdater {
  static bool get isSupported => false;

  static Future<void> downloadAndInstall(
    String assetUrl, {
    required void Function(double progress, String phase) onProgress,
  }) async {
    throw UnsupportedError('In-app self-update is not available on web.');
  }
}
