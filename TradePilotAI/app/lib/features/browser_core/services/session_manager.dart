import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

/// PRD 2.2.6 "Session Manager" -- persists the list of open (non-incognito)
/// tabs so closing and reopening the app resumes where you left off,
/// exactly like Chrome/Firefox's "Continue where you left off".
class SavedTab {
  final String url;
  final String title;
  const SavedTab({required this.url, required this.title});

  Map<String, dynamic> toJson() => {'url': url, 'title': title};
  factory SavedTab.fromJson(Map<String, dynamic> json) =>
      SavedTab(url: json['url'] as String, title: json['title'] as String);
}

class SessionManager {
  static const _key = 'tradepilot_browser_session';

  static Future<void> saveSession(List<SavedTab> tabs) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = jsonEncode(tabs.map((t) => t.toJson()).toList());
      await prefs.setString(_key, raw);
    } catch (_) {
      // Session persistence is a convenience, not critical -- never let a
      // storage failure interrupt normal browsing.
    }
  }

  static Future<List<SavedTab>> restoreSession() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_key);
      if (raw == null || raw.isEmpty) return [];
      final list = jsonDecode(raw) as List<dynamic>;
      return list.map((e) => SavedTab.fromJson(e as Map<String, dynamic>)).toList();
    } catch (_) {
      return [];
    }
  }

  static Future<void> clearSession() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_key);
    } catch (_) {}
  }
}
