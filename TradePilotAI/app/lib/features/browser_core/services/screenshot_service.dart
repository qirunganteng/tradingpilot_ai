/// PRD 2.2.16 "Screenshot" -- saves captured page-screenshot bytes to disk
/// (desktop/mobile) or triggers a browser download (web).
library;

export 'screenshot_service_io.dart' if (dart.library.html) 'screenshot_service_web.dart';
