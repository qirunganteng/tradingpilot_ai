/// Conditional export -- picks the `dart:io`-based pinning implementation
/// everywhere except web, where it falls back to a documented no-op.
library;

export 'certificate_pinning_io.dart' if (dart.library.html) 'certificate_pinning_web.dart';
