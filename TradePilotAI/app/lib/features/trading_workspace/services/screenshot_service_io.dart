import 'dart:io';
import 'dart:typed_data';
import 'package:path_provider/path_provider.dart';

/// Saves to the OS Downloads folder when available (desktop), falling back
/// to the app's own documents directory (mobile, where there's no single
/// shared Downloads folder without extra platform permissions).
Future<String> saveScreenshot(Uint8List bytes, String filename) async {
  Directory dir;
  try {
    dir = await getDownloadsDirectory() ?? await getApplicationDocumentsDirectory();
  } catch (_) {
    dir = await getApplicationDocumentsDirectory();
  }
  final file = File('${dir.path}${Platform.pathSeparator}$filename');
  await file.writeAsBytes(bytes);
  return file.path;
}
