import 'dart:io' show Platform;
import 'package:window_manager/window_manager.dart';

// Desktop implementation: real OS-level fullscreen (hides the title bar,
// exactly like pressing F11 in a real browser). Mobile (Android/iOS) has
// no window chrome to hide the same way, so it's a no-op there -- the
// in-app "hide side panels" behavior already gives an equivalent focused
// view on mobile.
bool _isFullscreen = false;

Future<void> toggleBrowserFullscreen() async {
  if (Platform.isWindows || Platform.isLinux || Platform.isMacOS) {
    _isFullscreen = !_isFullscreen;
    await windowManager.setFullScreen(_isFullscreen);
  }
}

bool get isBrowserFullscreen => _isFullscreen;
