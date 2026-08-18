import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'builtin/position_size_calculator_plugin.dart';
import 'plugin_interface.dart';

/// PRD §11.2 "Plugin Architecture" -- the registry every built-in plugin
/// signs up with. Which plugins are *enabled* persists across restarts
/// (a disabled plugin's [TradePilotPlugin.onLoad] never runs, and its
/// widgets never render); which plugins *exist at all* is fixed at compile
/// time (see plugin_interface.dart's scope note on why).
class PluginManager extends Notifier<List<TradePilotPlugin>> {
  static const _enabledKey = 'tradepilot_enabled_plugin_ids';

  /// Every plugin the app ships with, registered here once. Adding a new
  /// built-in plugin is exactly this: implement [TradePilotPlugin],
  /// add an instance to this list.
  static List<TradePilotPlugin> registry() => [
        PositionSizeCalculatorPlugin(),
      ];

  final Set<String> _enabledIds = {};

  @override
  List<TradePilotPlugin> build() {
    _restoreEnabled();
    return registry();
  }

  bool isEnabled(String pluginId) => _enabledIds.contains(pluginId);

  Future<void> _restoreEnabled() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final saved = prefs.getStringList(_enabledKey);
      if (saved == null) return;
      _enabledIds
        ..clear()
        ..addAll(saved);
      for (final plugin in state) {
        if (_enabledIds.contains(plugin.id)) await plugin.onLoad();
      }
      // Bumping `state` to the same list (new reference) so any widget
      // watching this provider re-renders once enabled/disabled status is
      // known -- `isEnabled` itself isn't part of `state`'s identity
      // otherwise.
      state = List.of(state);
    } catch (_) {}
  }

  Future<void> setEnabled(String pluginId, bool enabled) async {
    final plugin = state.where((p) => p.id == pluginId).firstOrNull;
    if (plugin == null) return;

    if (enabled && !_enabledIds.contains(pluginId)) {
      await plugin.onLoad();
      _enabledIds.add(pluginId);
    } else if (!enabled && _enabledIds.contains(pluginId)) {
      await plugin.onUnload();
      _enabledIds.remove(pluginId);
    }

    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setStringList(_enabledKey, _enabledIds.toList());
    } catch (_) {}

    state = List.of(state);
  }

  /// Broadcasts an event to every currently-enabled plugin -- see
  /// [TradePilotPlugin.handleEvent].
  Future<void> broadcast(String event, Map<String, dynamic> data) async {
    for (final plugin in state) {
      if (isEnabled(plugin.id)) {
        await plugin.handleEvent(event, data);
      }
    }
  }
}

final pluginManagerProvider = NotifierProvider<PluginManager, List<TradePilotPlugin>>(PluginManager.new);
