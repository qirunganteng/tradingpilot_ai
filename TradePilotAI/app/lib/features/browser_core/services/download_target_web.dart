// ignore_for_file: deprecated_member_use
import 'dart:html' as html;
import 'package:dio/dio.dart';

/// Web: there's no filesystem to stream to, so the full response is
/// fetched into memory then handed to the browser's own download pipeline
/// via a Blob + temporary anchor click (same technique as
/// screenshot_service_web.dart). Progress is reported once at the end
/// rather than incrementally -- byte-range progress events aren't
/// available through this path, and the browser's own download UI takes
/// over the moment the anchor is clicked anyway.
Future<String> downloadToFile(
  Dio dio,
  String url,
  String filename,
  void Function(int received, int total) onProgress,
) async {
  final response = await dio.get<List<int>>(
    url,
    options: Options(responseType: ResponseType.bytes),
  );
  final bytes = response.data ?? const <int>[];
  onProgress(bytes.length, bytes.length);

  final blob = html.Blob([bytes]);
  final blobUrl = html.Url.createObjectUrlFromBlob(blob);
  html.AnchorElement(href: blobUrl)
    ..setAttribute('download', filename)
    ..click();
  html.Url.revokeObjectUrl(blobUrl);
  return 'Downloads/$filename';
}
