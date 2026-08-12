/// Conditional export -- real Dio-streamed file download everywhere except
/// web, which falls back to a Blob-based browser download.
library;

export 'download_target_io.dart' if (dart.library.html) 'download_target_web.dart';
