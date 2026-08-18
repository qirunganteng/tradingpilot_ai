import 'package:flutter/widgets.dart';

/// PRD §11 "Plugin System" / AppFlowy reference (modular plugin
/// registration so functionality can be added "without changing the
/// core"). Matches the PRD's own interface definition exactly:
///
/// ```dart
/// abstract class TradePilotPlugin {
///   String get id;
///   String get name;
///   String get version;
///   Future<void> onLoad();
///   Future<void> onUnload();
///   Widget? get toolbarWidget();
///   Widget? get sidebarWidget();
///   Widget? get workspaceWidget();
///   Future<void> handleEvent(String event, Map<String, dynamic> data);
/// }
/// ```
///
/// Important, honest scope note: this is a *registration and lifecycle*
/// system for plugins compiled into the app (built-in "extensions", the
/// same architectural pattern VS Code's and AppFlowy's own bundled
/// features use internally) -- it is **not** a way to load arbitrary
/// third-party `.dart` code at runtime. Flutter/Dart apps are ahead-of-time
/// compiled; there is no `eval` or dynamic native-code loading available
/// without embedding a separate scripting VM (e.g. `hetu_script`), which
/// is a much larger undertaking than "a plugin interface" and out of
/// scope here. See docs/known-limitations.md for what real third-party
/// plugin support would require.
abstract class TradePilotPlugin {
  String get id;
  String get name;
  String get version;
  String get description;

  /// Called once when the plugin is enabled (including at app startup for
  /// plugins the person previously enabled) -- for setting up any
  /// resources the plugin needs (timers, listeners, local state).
  Future<void> onLoad();

  /// Called when the plugin is disabled -- must release whatever [onLoad]
  /// set up.
  Future<void> onUnload();

  /// An optional widget shown in a toolbar slot. `null` if this plugin
  /// doesn't contribute one.
  Widget? get toolbarWidget => null;

  /// An optional widget shown in the Plugins sidebar list (a compact
  /// summary/quick-action view).
  Widget? get sidebarWidget => null;

  /// The plugin's main UI, opened full-size from the Plugins manager.
  Widget? get workspaceWidget => null;

  /// Lets other parts of the app (or other plugins) notify this plugin of
  /// something, without either side needing a direct reference to the
  /// other -- e.g. `handleEvent('symbol_changed', {'symbol': 'BTC/USDT'})`.
  Future<void> handleEvent(String event, Map<String, dynamic> data) async {}
}

/// PRD §11.4 "Plugin Manifest" -- metadata shown in the Plugins manager UI.
/// For a built-in plugin this is authored alongside its
/// [TradePilotPlugin] implementation rather than parsed from a real
/// `manifest.json` on disk (see the scope note above), but the shape
/// matches the PRD's JSON format exactly so it's a drop-in fit if/when
/// on-disk manifests are added later.
class PluginManifest {
  final String id;
  final String name;
  final String version;
  final String description;
  final String author;
  final List<String> permissions;

  const PluginManifest({
    required this.id,
    required this.name,
    required this.version,
    required this.description,
    this.author = 'TradePilot',
    this.permissions = const [],
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'name': name,
        'version': version,
        'description': description,
        'author': author,
        'permissions': permissions,
      };
}
