import 'dart:io';
import 'package:dio/dio.dart';
import 'package:path_provider/path_provider.dart';

/// Desktop/mobile: streams straight to disk via Dio with real progress
/// callbacks, saving to the OS Downloads folder when available (desktop),
/// falling back to the app's own documents directory (mobile, where
/// there's no single shared Downloads folder without extra platform
/// permissions) -- same fallback rule as screenshot_service_io.dart.
Future<String> downloadToFile(
  Dio dio,
  String url,
  String filename,
  void Function(int received, int total) onProgress,
) async {
  Directory dir;
  try {
    dir = await getDownloadsDirectory() ?? await getApplicationDocumentsDirectory();
  } catch (_) {
    dir = await getApplicationDocumentsDirectory();
  }
  // Avoid clobbering an existing file with the same name.
  var target = File('${dir.path}${Platform.pathSeparator}$filename');
  if (await target.exists()) {
    final dot = filename.lastIndexOf('.');
    final base = dot > 0 ? filename.substring(0, dot) : filename;
    final ext = dot > 0 ? filename.substring(dot) : '';
    target = File('${dir.path}${Platform.pathSeparator}${base}_${DateTime.now().millisecondsSinceEpoch}$ext');
  }

  await dio.download(
    url,
    target.path,
    onReceiveProgress: onProgress,
    options: Options(responseType: ResponseType.bytes),
  );
  return target.path;
}
