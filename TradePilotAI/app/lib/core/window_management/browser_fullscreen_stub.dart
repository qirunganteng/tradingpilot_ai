// Stub used on non-web platforms (Windows/macOS/Linux/Android/iOS), where
// the actual window is controlled via window_manager/bitsdojo_window
// instead (see custom_title_bar.dart).
Future<void> toggleBrowserFullscreen() async {}

bool get isBrowserFullscreen => false;
