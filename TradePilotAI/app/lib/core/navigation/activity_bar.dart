import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import '../window_management/browser_fullscreen.dart';
import 'app_settings_dialog.dart';

/// Legacy full-workspace-switch enum — kept only so [deep_link_handler.dart]
/// and other older references still compile. The live UI now uses
/// [LeftDockMode] + [aiPanelVisibleProvider] instead (see below), which
/// toggle docks around a persistent browser rather than swapping the whole
/// workspace.
enum WorkspaceMode {
  trading,
  aiPilot,
  social,
  learning,
}

class WorkspaceModeNotifier extends Notifier<WorkspaceMode> {
  @override
  WorkspaceMode build() => WorkspaceMode.trading;

  void setMode(WorkspaceMode mode) {
    state = mode;
  }
}

final workspaceModeProvider = NotifierProvider<WorkspaceModeNotifier, WorkspaceMode>(WorkspaceModeNotifier.new);

// ---------------------------------------------------------------------------
// New VSCode-style dock model
// ---------------------------------------------------------------------------

/// Which tool panel is shown in the LEFT dock. `null` = dock is collapsed.
enum LeftDockMode { trading, social, learning }

class LeftDockModeNotifier extends Notifier<LeftDockMode?> {
  @override
  LeftDockMode? build() => LeftDockMode.trading;

  /// Selecting the already-active mode collapses the dock, matching
  /// VSCode's "click the active activity-bar icon again to hide it".
  void select(LeftDockMode mode) {
    state = (state == mode) ? null : mode;
  }

  void close() => state = null;

  /// Sets the dock to an explicit value (used when restoring a saved
  /// layout, where toggle semantics would be wrong).
  void set(LeftDockMode? mode) => state = mode;
}

final leftDockModeProvider = NotifierProvider<LeftDockModeNotifier, LeftDockMode?>(LeftDockModeNotifier.new);

/// Whether the RIGHT "AI" dock is visible.
class AiPanelVisibleNotifier extends Notifier<bool> {
  @override
  bool build() => true;

  void toggle() => state = !state;
  void close() => state = false;
  void set(bool value) => state = value;
}

final aiPanelVisibleProvider = NotifierProvider<AiPanelVisibleNotifier, bool>(AiPanelVisibleNotifier.new);

/// Whether the browser is "maximized" (both side docks hidden so it fills
/// the whole workspace) — remembers the prior dock layout so toggling back
/// restores exactly what was open before. This is the single source of
/// truth so both the browser's own controls and the global Escape-key
/// handler in app.dart agree on the state.
class BrowserMaximizedNotifier extends Notifier<bool> {
  LeftDockMode? _savedLeftMode;
  bool _savedAiVisible = true;

  @override
  bool build() => false;

  void toggle() {
    final leftNotifier = ref.read(leftDockModeProvider.notifier);
    final aiNotifier = ref.read(aiPanelVisibleProvider.notifier);

    if (state) {
      leftNotifier.set(_savedLeftMode);
      aiNotifier.set(_savedAiVisible);
      state = false;
      if (isBrowserFullscreen) toggleBrowserFullscreen();
    } else {
      _savedLeftMode = ref.read(leftDockModeProvider);
      _savedAiVisible = ref.read(aiPanelVisibleProvider);
      leftNotifier.set(null);
      aiNotifier.set(false);
      state = true;
      if (!isBrowserFullscreen) toggleBrowserFullscreen();
    }
  }

  /// Force-restores the saved layout (used by the Escape key) without
  /// toggling into maximize if it isn't already active.
  void restoreIfMaximized() {
    if (state) toggle();
  }
}

final browserMaximizedProvider = NotifierProvider<BrowserMaximizedNotifier, bool>(BrowserMaximizedNotifier.new);

class ActivityBarItem {
  final IconData icon;
  final String label;
  final String tooltip;
  final VoidCallback Function(WidgetRef ref) onTap;
  final bool Function(WidgetRef ref) isSelected;

  const ActivityBarItem({
    required this.icon,
    required this.label,
    required this.tooltip,
    required this.onTap,
    required this.isSelected,
  });
}

class ActivityBar extends ConsumerWidget {
  const ActivityBar({super.key});

  static final List<ActivityBarItem> items = [
    ActivityBarItem(
      icon: Icons.show_chart,
      label: 'Trading',
      tooltip: 'Toggle Trading panel',
      onTap: (ref) => () => ref.read(leftDockModeProvider.notifier).select(LeftDockMode.trading),
      isSelected: (ref) => ref.watch(leftDockModeProvider) == LeftDockMode.trading,
    ),
    ActivityBarItem(
      icon: Icons.smart_toy,
      label: 'AI Pilot',
      tooltip: 'Toggle AI panel',
      onTap: (ref) => () => ref.read(aiPanelVisibleProvider.notifier).toggle(),
      isSelected: (ref) => ref.watch(aiPanelVisibleProvider),
    ),
    ActivityBarItem(
      icon: Icons.people_alt,
      label: 'Community',
      tooltip: 'Toggle Community panel',
      onTap: (ref) => () => ref.read(leftDockModeProvider.notifier).select(LeftDockMode.social),
      isSelected: (ref) => ref.watch(leftDockModeProvider) == LeftDockMode.social,
    ),
    // The old Learning (graduation cap) icon was removed: its panel was a
    // static Coming Soon placeholder, and the one real feature under that
    // umbrella (Lofi Radio) already lives in the always-visible footer, so
    // the button had nothing unique to open.
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Container(
      width: 38,
      color: const Color(0xFF1E1E1E),
      child: Column(
        children: [
          // No app-brand logo here anymore — it was a second, non-interactive
          // shield icon sitting directly under the actual browser tab/window
          // icon, adding nothing but visual clutter. The activity bar now
          // starts straight at the (functional) navigation icons.
          const SizedBox(height: 6),
          // Navigation Items — these only toggle docks, the browser in the
          // center is never rebuilt/unmounted by clicking these.
          Expanded(
            child: ListView.builder(
              itemCount: items.length,
              itemBuilder: (context, index) {
                final item = items[index];
                final isSelected = item.isSelected(ref);

                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 3, vertical: 3),
                  child: Tooltip(
                    message: item.tooltip,
                    preferBelow: false,
                    child: GestureDetector(
                      onTap: item.onTap(ref),
                      child: Container(
                        width: 32,
                        height: 32,
                        decoration: BoxDecoration(
                          color: isSelected
                              ? Colors.blueAccent.withValues(alpha: 0.2)
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(8),
                          border: isSelected
                              ? const Border(
                                  left: BorderSide(
                                    color: Colors.blueAccent,
                                    width: 3,
                                  ),
                                )
                              : null,
                        ),
                        child: Icon(
                          item.icon,
                          color: isSelected ? Colors.blueAccent : Colors.grey,
                          size: 19,
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
          Divider(color: Colors.grey[800], height: 1, indent: 8, endIndent: 8),
          // Settings
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 3, vertical: 6),
            child: Tooltip(
              message: 'Settings',
              preferBelow: false,
              child: SizedBox(
                width: 32,
                height: 32,
                child: IconButton(
                  icon: const Icon(Icons.settings, color: Colors.grey, size: 19),
                  onPressed: () {
                    showDialog(
                      context: context,
                      builder: (_) => const AppSettingsDialog(),
                    );
                  },
                  padding: EdgeInsets.zero,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

