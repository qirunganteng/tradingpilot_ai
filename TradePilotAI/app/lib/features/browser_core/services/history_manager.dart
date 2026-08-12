import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

/// PRD 2.2.8 "History" -- persists browsing history across app restarts
/// (the in-memory-only version this replaces lost every entry on close,
/// which fails the PRD's own acceptance criteria of a browsable, dated
/// history that survives a relaunch).
///
/// Deliberately excludes incognito tabs entirely (never even passed in --
/// see PRD 3.3.1 / the call site in browser_view.dart), and caps at
/// [_maxEntries] so history can't grow unbounded in local storage.
class HistoryEntry {
  final String url;
  final String title;
  final DateTime visitedAt;

  const HistoryEntry({required this.url, required this.title, required this.visitedAt});

  Map<String, dynamic> toJson() => {
        'url': url,
        'title': title,
        'visitedAt': visitedAt.toIso8601String(),
      };

  factory HistoryEntry.fromJson(Map<String, dynamic> json) => HistoryEntry(
        url: json['url'] as String,
        title: json['title'] as String,
        visitedAt: DateTime.parse(json['visitedAt'] as String),
      );
}

class HistoryManager {
  static const _key = 'tradepilot_browser_history';
  static const _maxEntries = 1000;

  static Future<List<HistoryEntry>> loadAll() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_key);
      if (raw == null || raw.isEmpty) return [];
      final list = jsonDecode(raw) as List<dynamic>;
      return list.map((e) => HistoryEntry.fromJson(e as Map<String, dynamic>)).toList();
    } catch (_) {
      return [];
    }
  }

  static Future<void> saveAll(List<HistoryEntry> entries) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final trimmed = entries.length > _maxEntries ? entries.sublist(0, _maxEntries) : entries;
      final raw = jsonEncode(trimmed.map((e) => e.toJson()).toList());
      await prefs.setString(_key, raw);
    } catch (_) {
      // History persistence is a convenience, not critical -- never let a
      // storage failure interrupt normal browsing.
    }
  }

  static Future<void> clear() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_key);
    } catch (_) {}
  }

  /// Removes every entry visited "today" (device-local calendar day) --
  /// backs the toolbar's Ctrl+Shift+Delete quick-clear, matching the PRD
  /// mockup's "[Clear Today]" button.
  static Future<List<HistoryEntry>> clearToday(List<HistoryEntry> current) async {
    final now = DateTime.now();
    final remaining = current.where((e) {
      final v = e.visitedAt;
      return !(v.year == now.year && v.month == now.month && v.day == now.day);
    }).toList();
    await saveAll(remaining);
    return remaining;
  }
}
