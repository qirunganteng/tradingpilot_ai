/// Conditional export -- real self-update (Windows self-replace via a
/// generated batch script, Android APK download + install-intent)
/// everywhere except web, which has neither a filesystem to stage an
/// update in nor an OS installer to hand one to.
library;

export 'self_updater_io.dart' if (dart.library.html) 'self_updater_web.dart';
