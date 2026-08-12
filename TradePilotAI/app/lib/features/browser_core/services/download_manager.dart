import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'download_target.dart';

/// PRD 2.2.9 "Downloads" -- replaces the old placeholder (which just
/// re-opened the URL in a new tab and called that a "download") with a
/// real transfer: bytes actually land on disk (desktop/mobile) or go
/// through the browser's own download pipeline (web), with live
/// progress, persisted history across restarts, and open/retry actions
/// that operate on the *saved file* rather than re-navigating.
enum DownloadStatus { inProgress, completed, failed }

class DownloadRecord {
  final String id;
  final String url;
  final String filename;
  String? path; // null until completed
  int downloadedBytes;
  int totalBytes; // -1 if unknown
  DownloadStatus status;
  final DateTime startedAt;
  DateTime? completedAt;

  DownloadRecord({
    required this.id,
    required this.url,
    required this.filename,
    this.path,
    this.downloadedBytes = 0,
    this.totalBytes = -1,
    this.status = DownloadStatus.inProgress,
    required this.startedAt,
    this.completedAt,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'url': url,
        'filename': filename,
        'path': path,
        'downloadedBytes': downloadedBytes,
        'totalBytes': totalBytes,
        'status': status.name,
        'startedAt': startedAt.toIso8601String(),
        'completedAt': completedAt?.toIso8601String(),
      };

  factory DownloadRecord.fromJson(Map<String, dynamic> json) => DownloadRecord(
        id: json['id'] as String,
        url: json['url'] as String,
        filename: json['filename'] as String,
        path: json['path'] as String?,
        downloadedBytes: json['downloadedBytes'] as int? ?? 0,
        totalBytes: json['totalBytes'] as int? ?? -1,
        status: DownloadStatus.values.firstWhere(
          (s) => s.name == json['status'],
          orElse: () => DownloadStatus.failed,
        ),
        startedAt: DateTime.parse(json['startedAt'] as String),
        completedAt: json['completedAt'] != null ? DateTime.parse(json['completedAt'] as String) : null,
      );
}

/// One instance lives for the lifetime of the browser view. Exposes a
/// [ValueNotifier] so the Downloads dialog/menu badge can rebuild reactively
/// without the browser view having to manually thread progress updates
/// through setState on every single progress tick.
class DownloadManager {
  static const _key = 'tradepilot_browser_downloads';
  final Dio _dio;
  final ValueNotifier<List<DownloadRecord>> records = ValueNotifier([]);

  DownloadManager({Dio? dio}) : _dio = dio ?? Dio();

  Future<void> loadPersisted() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_key);
      if (raw == null || raw.isEmpty) return;
      final list = (jsonDecode(raw) as List<dynamic>)
          .map((e) => DownloadRecord.fromJson(e as Map<String, dynamic>))
          .toList();
      records.value = list;
    } catch (_) {
      // Ignore corrupt/missing persisted download history.
    }
  }

  Future<void> _persist() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = jsonEncode(records.value.map((e) => e.toJson()).toList());
      await prefs.setString(_key, raw);
    } catch (_) {}
  }

  /// Starts a real download of [url], tracked in [records] with live
  /// progress. Returns the created record's id.
  String start(String url, {String? suggestedFilename}) {
    final filename = suggestedFilename?.isNotEmpty == true
        ? suggestedFilename!
        : (Uri.tryParse(url)?.pathSegments.isNotEmpty == true ? Uri.parse(url).pathSegments.last : 'download');
    final record = DownloadRecord(
      id: 'dl_${DateTime.now().millisecondsSinceEpoch}',
      url: url,
      filename: filename,
      startedAt: DateTime.now(),
    );
    records.value = [record, ...records.value];
    _persist();

    downloadToFile(_dio, url, filename, (received, total) {
      record.downloadedBytes = received;
      record.totalBytes = total;
      // Reassigning (rather than calling the protected notifyListeners())
      // triggers ValueNotifier's own change notification since a freshly
      // allocated List is never `==` to the previous one.
      records.value = List.of(records.value);
    }).then((savedPath) {
      record.status = DownloadStatus.completed;
      record.path = savedPath;
      record.completedAt = DateTime.now();
      records.value = List.of(records.value);
      _persist();
    }).catchError((_) {
      record.status = DownloadStatus.failed;
      records.value = List.of(records.value);
      _persist();
    });

    return record.id;
  }

  Future<void> clearAll() async {
    records.value = [];
    await _persist();
  }

  void dispose() => records.dispose();
}
