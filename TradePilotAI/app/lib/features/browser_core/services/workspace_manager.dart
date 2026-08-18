import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

/// PRD 2.2.5 "Workspace" -- named, switchable sets of browser tabs (e.g.
/// "Forex Analysis", "Crypto", "Stocks") so a trader can jump between
/// contexts instantly instead of closing/reopening a dozen tabs by hand.
///
/// Each [Workspace] remembers its own tab list. Switching workspaces saves
/// whatever is currently open into the workspace being left, then restores
/// whatever was saved for the workspace being entered -- the same mental
/// model as switching a Chrome *window* rather than a single tab.
class SavedWorkspaceTab {
  final String url;
  final String title;
  const SavedWorkspaceTab({required this.url, required this.title});

  Map<String, dynamic> toJson() => {'url': url, 'title': title};
  factory SavedWorkspaceTab.fromJson(Map<String, dynamic> json) => SavedWorkspaceTab(
        url: json['url'] as String,
        title: json['title'] as String,
      );
}

class Workspace {
  final String id;
  String name;
  String icon; // a single emoji shown next to the name in the switcher
  List<SavedWorkspaceTab> tabs;

  Workspace({
    required this.id,
    required this.name,
    this.icon = '🗂️',
    List<SavedWorkspaceTab>? tabs,
  }) : tabs = tabs ?? [];

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'icon': icon,
        'tabs': tabs.map((t) => t.toJson()).toList(),
      };

  factory Workspace.fromJson(Map<String, dynamic> json) => Workspace(
        id: json['id'] as String,
        name: json['name'] as String,
        icon: json['icon'] as String? ?? '🗂️',
        tabs: (json['tabs'] as List<dynamic>? ?? [])
            .map((e) => SavedWorkspaceTab.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

class WorkspaceManager {
  static const _workspacesKey = 'tradepilot_browser_workspaces';
  static const _activeKey = 'tradepilot_browser_active_workspace_id';

  static Future<List<Workspace>> loadAll() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_workspacesKey);
      if (raw == null || raw.isEmpty) return [];
      final list = jsonDecode(raw) as List<dynamic>;
      return list.map((e) => Workspace.fromJson(e as Map<String, dynamic>)).toList();
    } catch (_) {
      return [];
    }
  }

  static Future<void> saveAll(List<Workspace> workspaces) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = jsonEncode(workspaces.map((w) => w.toJson()).toList());
      await prefs.setString(_workspacesKey, raw);
    } catch (_) {
      // Workspace persistence is a convenience, not critical -- never let a
      // storage failure interrupt normal browsing.
    }
  }

  static Future<String?> loadActiveId() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getString(_activeKey);
    } catch (_) {
      return null;
    }
  }

  static Future<void> saveActiveId(String id) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_activeKey, id);
    } catch (_) {}
  }

  /// Backs the Settings dialog's "Reset browsing data" action -- wipes
  /// every saved workspace/tab so the next launch starts genuinely fresh
  /// (a blank "Default" workspace with one New Tab page), instead of
  /// forever restoring whatever was open at some point in the past.
  static Future<void> clearAll() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_workspacesKey);
      await prefs.remove(_activeKey);
    } catch (_) {}
  }
}
