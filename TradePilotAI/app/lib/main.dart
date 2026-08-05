import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:tray_manager/tray_manager.dart';
import 'dart:io' show Platform;

import 'app.dart';
import 'core/window_management/window_setup.dart';
import 'core/window_management/tray_setup.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

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
