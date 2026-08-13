import 'dart:io';
import 'package:archive/archive_io.dart';
import 'package:dio/dio.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';

/// Desktop/mobile self-update (everything except web, which has no
/// filesystem to download an installer/APK to in the first place -- see
/// self_updater_web.dart).
class SelfUpdater {
  static bool get isSupported => Platform.isWindows || Platform.isAndroid;

  static Future<void> downloadAndInstall(
    String assetUrl, {
    required void Function(double progress, String phase) onProgress,
  }) async {
    if (Platform.isWindows) {
      await _windowsSelfReplace(assetUrl, onProgress);
    } else if (Platform.isAndroid) {
      await _androidInstallApk(assetUrl, onProgress);
    } else {
      throw UnsupportedError('In-app self-update is not available on this platform.');
    }
  }

  /// Android: downloads the release APK, then hands it to the OS's own
  /// package installer via an ACTION_VIEW intent (`open_filex`, which
  /// wires up the required FileProvider under the hood). This is as
  /// close to "no manual download+install dance" as Android's security
  /// model allows for an app distributed outside the Play Store --
  /// installing an APK always requires one explicit tap on the system
  /// "Install" confirmation (Android deliberately disallows silent
  /// self-install for anything that isn't the device owner/an MDM
  /// profile), but everything before that tap now happens automatically.
  static Future<void> _androidInstallApk(String assetUrl, void Function(double, String) onProgress) async {
    final dio = Dio();
    final dir = await getTemporaryDirectory();
    final apkPath = '${dir.path}/tradepilot_update.apk';

    onProgress(0, 'Downloading update\u2026');
    await dio.download(
      assetUrl,
      apkPath,
      onReceiveProgress: (received, total) {
        if (total > 0) onProgress(received / total, 'Downloading update\u2026');
      },
    );

    onProgress(1, 'Opening installer\u2026');
    await OpenFilex.open(apkPath);
  }

  /// Windows: downloads the release zip, extracts it, then hands off to a
  /// small generated batch script that waits for this process to exit,
  /// copies the new files over the current install directory, and
  /// relaunches the app -- so the person never has to manually
  /// uninstall/reinstall for a new version.
  ///
  /// Why a batch script and not just overwriting files directly: a
  /// running Windows executable has its own .exe (and any loaded DLLs)
  /// locked by the OS -- nothing in-process, Dart or otherwise, can
  /// overwrite `tradepilot.exe` while `tradepilot.exe` is the one
  /// running. The standard, well-established pattern for this (used by
  /// everything from VS Code to Discord's own updaters) is exactly this:
  /// a tiny external helper that starts *after* the app exits, does the
  /// file copy, then restarts the app. `cmd.exe` + `xcopy` (both built
  /// into every Windows install, no extra dependency) is that helper
  /// here.
  static Future<void> _windowsSelfReplace(String assetUrl, void Function(double, String) onProgress) async {
    final dio = Dio();
    final tempDir = await getTemporaryDirectory();
    final updateRoot = Directory('${tempDir.path}${Platform.pathSeparator}tradepilot_update');
    if (await updateRoot.exists()) {
      await updateRoot.delete(recursive: true);
    }
    await updateRoot.create(recursive: true);

    final zipPath = '${updateRoot.path}${Platform.pathSeparator}update.zip';
    onProgress(0, 'Downloading update\u2026');
    await dio.download(
      assetUrl,
      zipPath,
      onReceiveProgress: (received, total) {
        if (total > 0) onProgress(received / total, 'Downloading update\u2026');
      },
    );

    onProgress(1, 'Extracting\u2026');
    final stagingDir = Directory('${updateRoot.path}${Platform.pathSeparator}staged');
    await stagingDir.create(recursive: true);
    await extractFileToDisk(zipPath, stagingDir.path);

    // The release zip may contain the app files directly, or wrapped in a
    // single top-level folder (both are common depending on how the CI
    // zip step was written) -- if extraction produced exactly one
    // directory and nothing else at the top level, treat *that* as the
    // real staging root instead.
    final entries = stagingDir.listSync();
    final effectiveStagingDir = (entries.length == 1 && entries.first is Directory)
        ? entries.first as Directory
        : stagingDir;

    // The running exe's own directory is the install directory -- this
    // app is distributed as a flat extracted zip, not installed to a
    // fixed Program Files path, so "wherever it's currently running
    // from" *is* the install location.
    final exePath = Platform.resolvedExecutable;
    final installDir = File(exePath).parent.path;
    final exeName = exePath.split(Platform.pathSeparator).last;

    onProgress(1, 'Restarting to finish updating\u2026');
    final batPath = '${updateRoot.path}${Platform.pathSeparator}apply_update.bat';
    final batFile = File(batPath);
    // Every path is double-quoted -- both this app's own install path and
    // the user's temp path can (and often do, e.g. "C:\Users\John Doe\...")
    // contain spaces.
    await batFile.writeAsString('''
@echo off
setlocal
set "TARGET_PID=${pid.toString()}"
set "INSTALL_DIR=$installDir"
set "STAGING_DIR=${effectiveStagingDir.path}"
set "EXE_NAME=$exeName"

:waitloop
tasklist /FI "PID eq %TARGET_PID%" 2>NUL | find /I "%TARGET_PID%" >NUL
if "%ERRORLEVEL%"=="0" (
  timeout /t 1 /nobreak >NUL
  goto waitloop
)

xcopy "%STAGING_DIR%\\*" "%INSTALL_DIR%\\" /E /I /Y /Q >NUL

start "" "%INSTALL_DIR%\\%EXE_NAME%"

rmdir /S /Q "${updateRoot.path}" >NUL 2>&1
endlocal
''');

    await Process.start(
      'cmd.exe',
      ['/c', batPath],
      mode: ProcessStartMode.detached,
      runInShell: false,
    );

    // Give the detached process a moment to actually spawn and open its
    // own handle on the batch file before this process (and everything
    // it has open) exits.
    await Future.delayed(const Duration(milliseconds: 300));
    exit(0);
  }
}
