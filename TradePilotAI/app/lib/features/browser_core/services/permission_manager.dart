import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

/// PRD 3.2.6 / CONSTITUTION.md "Permission Manager" -- CONSTITUTION.md
/// marks this WAJIB (mandatory): camera/mic/location access must be
/// explicit *per site*, never a single global toggle. This is the
/// persistence + policy layer; the actual prompt UI and the
/// `onPermissionRequest` hookup into flutter_inappwebview live in
/// browser_view.dart, which calls [PermissionManager.decisionFor] before
/// ever showing a prompt (so a remembered choice never re-prompts) and
/// [PermissionManager.remember] after the user answers.
enum PermissionDecision { allow, block }

class PermissionManager {
  static const _key = 'tradepilot_browser_site_permissions';

  /// In-memory cache mirroring persisted storage, keyed by
  /// `"$origin|$resourceType"` -- avoids an async round trip on every
  /// single permission check once loaded.
  static Map<String, String> _cache = {};
  static bool _loaded = false;

  static Future<void> _ensureLoaded() async {
    if (_loaded) return;
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_key);
      if (raw != null && raw.isNotEmpty) {
        _cache = Map<String, String>.from(jsonDecode(raw) as Map);
      }
    } catch (_) {
      _cache = {};
    }
    _loaded = true;
  }

  static String _keyFor(String origin, String resourceType) => '$origin|$resourceType';

  /// Returns the remembered decision for this exact (origin, resourceType)
  /// pair, or `null` if the user has never been asked -- callers should
  /// prompt in that case.
  static Future<PermissionDecision?> decisionFor(String origin, String resourceType) async {
    await _ensureLoaded();
    final raw = _cache[_keyFor(origin, resourceType)];
    if (raw == null) return null;
    return raw == 'allow' ? PermissionDecision.allow : PermissionDecision.block;
  }

  static Future<void> remember(String origin, String resourceType, PermissionDecision decision) async {
    await _ensureLoaded();
    _cache[_keyFor(origin, resourceType)] = decision == PermissionDecision.allow ? 'allow' : 'block';
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_key, jsonEncode(_cache));
    } catch (_) {}
  }

  /// All remembered per-site decisions, for a future "Site settings"
  /// management screen (PRD 3.2.6 checklist item) -- returns
  /// `{origin: {resourceType: 'allow'|'block'}}`.
  static Future<Map<String, Map<String, String>>> loadAllGrouped() async {
    await _ensureLoaded();
    final grouped = <String, Map<String, String>>{};
    for (final entry in _cache.entries) {
      final parts = entry.key.split('|');
      if (parts.length != 2) continue;
      grouped.putIfAbsent(parts[0], () => {})[parts[1]] = entry.value;
    }
    return grouped;
  }

  static Future<void> revoke(String origin, String resourceType) async {
    await _ensureLoaded();
    _cache.remove(_keyFor(origin, resourceType));
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_key, jsonEncode(_cache));
    } catch (_) {}
  }

  static Future<void> clearAll() async {
    _cache = {};
    _loaded = true;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_key);
    } catch (_) {}
  }
}
