// ignore_for_file: deprecated_member_use
import 'dart:html' as html;
import 'dart:typed_data';

/// Web: triggers a normal browser file download via a Blob + temporary
/// anchor click -- there's no filesystem to write to directly.
Future<String> saveScreenshot(Uint8List bytes, String filename) async {
  final blob = html.Blob([bytes], 'image/png');
  final url = html.Url.createObjectUrlFromBlob(blob);
  html.AnchorElement(href: url)
    ..setAttribute('download', filename)
    ..click();
  html.Url.revokeObjectUrl(url);
  return 'Downloads/$filename';
}
