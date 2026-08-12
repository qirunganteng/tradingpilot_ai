import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:media_kit/media_kit.dart';
import 'package:tray_manager/tray_manager.dart';
import 'dart:io' show Platform;

import 'app.dart';
import 'core/window_management/window_setup.dart';
import 'core/window_management/tray_setup.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Flutter's default release-mode ErrorWidget is a plain gray box with no
  // text -- an uncaught error while building any part of the tree (e.g. a
  // RangeError from an empty list) is otherwise silently invisible to
  // whoever's using the shipped .exe/.apk, with no way to tell "the app is
  // broken" from "the app is just still loading". This makes that error
  // visible (still only where it happened -- the rest of the UI keeps
  // working) in every build, not just debug ones.
  ErrorWidget.builder = (FlutterErrorDetails details) {
    return Container(
      color: const Color(0xFF1E1E1E),
      alignment: Alignment.center,
      padding: const EdgeInsets.all(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, color: Colors.redAccent, size: 28),
          const SizedBox(height: 8),
          // Shown in every build (not just debug) while the app is still
          // in active development -- the alternative, a generic "something
          // went wrong" with no detail, makes bug reports from anyone but a
          // developer with a debugger attached nearly impossible. Revisit
          // hiding this behind kReleaseMode once the app is stable.
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Text(
              details.exceptionAsString(),
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.white70, fontSize: 11.5),
            ),
          ),
          const SizedBox(height: 10),
          TextButton.icon(
            onPressed: () => Clipboard.setData(
              ClipboardData(text: '${details.exceptionAsString()}\n\n${details.stack}'),
            ),
            icon: const Icon(Icons.copy, size: 14),
            label: const Text('Copy error details', style: TextStyle(fontSize: 11.5)),
          ),
        ],
      ),
    );
  };

  // media_kit (libmpv) backs the Lofi Radio player -- must be initialized
  // once, before any Player() is created. Web doesn't use media_kit's
  // native backend (it falls back to the browser's own <audio> element
  // internally), so this is skipped there.
  if (!kIsWeb) {
    MediaKit.ensureInitialized();
  }

  // Initialize Window Management & System Tray for Desktop
  if (!kIsWeb && (Platform.isWindows || Platform.isLinux || Platform.isMacOS)) {
    await setupWindow();
    await setupTray();

    final trayListener = TrayListenerManager();
    trayManager.addListener(trayListener);
  }

  runApp(
    const ProviderScope(
      child: TradePilotApp(),
    ),
  );
}
