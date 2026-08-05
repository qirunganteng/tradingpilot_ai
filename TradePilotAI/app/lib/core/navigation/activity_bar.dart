import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

enum WorkspaceMode {
  trading,
  aiPilot,
  social,
  learning,
}

class ActivityBarItem {
  final WorkspaceMode mode;
  final IconData icon;
  final String label;
  final String tooltip;

  const ActivityBarItem({
    required this.mode,
    required this.icon,
    required this.label,
    required this.tooltip,
  });
}

class WorkspaceModeNotifier extends Notifier<WorkspaceMode> {
  @override
  WorkspaceMode build() => WorkspaceMode.trading;

  void setMode(WorkspaceMode mode) {
    state = mode;
  }
}

final workspaceModeProvider = NotifierProvider<WorkspaceModeNotifier, WorkspaceMode>(WorkspaceModeNotifier.new);

class ActivityBar extends ConsumerWidget {
  const ActivityBar({super.key});

  static const List<ActivityBarItem> items = [
    ActivityBarItem(
      mode: WorkspaceMode.trading,
      icon: Icons.show_chart,
      label: 'Trading',
      tooltip: 'Trading Workspace',
    ),
    ActivityBarItem(
      mode: WorkspaceMode.aiPilot,
      icon: Icons.smart_toy,
      label: 'AI Pilot',
      tooltip: 'AI Analysis & Chat',
    ),
    ActivityBarItem(
      mode: WorkspaceMode.social,
      icon: Icons.people_alt,
      label: 'Community',
      tooltip: 'Social & Signals',
    ),
    ActivityBarItem(
      mode: WorkspaceMode.learning,
      icon: Icons.school,
      label: 'Learning',
      tooltip: 'Educational Hub',
    ),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final currentMode = ref.watch(workspaceModeProvider);

    return Container(
      width: 60,
      color: const Color(0xFF1E1E1E),
      child: Column(
        children: [
          // TradePilot Logo/Icon
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: Colors.blueAccent.withOpacity(0.15),
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Icon(
                Icons.trending_up,
                color: Colors.blueAccent,
                size: 28,
              ),
            ),
          ),
          const Divider(color: Colors.grey),
          // Navigation Items
          Expanded(
            child: ListView.builder(
              itemCount: items.length,
              itemBuilder: (context, index) {
                final item = items[index];
                final isSelected = currentMode == item.mode;

                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
                  child: Tooltip(
                    message: item.tooltip,
                    preferBelow: false,
                    child: GestureDetector(
                      onTap: () {
                        ref.read(workspaceModeProvider.notifier).setMode(item.mode);
                      },
                      child: Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: isSelected
                              ? Colors.blueAccent.withOpacity(0.2)
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(8),
                          border: isSelected
                              ? Border(
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
                          size: 24,
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
          const Divider(color: Colors.grey),
          // Settings & Help
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
            child: Tooltip(
              message: 'Settings',
              preferBelow: false,
              child: Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                ),
                child: IconButton(
                  icon: const Icon(Icons.settings, color: Colors.grey),
                  onPressed: () {
                    // TODO: Open settings dialog
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
