import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'activity_bar.dart';

class SidebarItem {
  final String id;
  final String label;
  final IconData icon;
  final VoidCallback onTap;

  const SidebarItem({
    required this.id,
    required this.label,
    required this.icon,
    required this.onTap,
  });
}

class Sidebar extends ConsumerWidget {
  final List<SidebarItem> items;
  final String? selectedItemId;

  const Sidebar({
    super.key,
    required this.items,
    this.selectedItemId,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Container(
      width: 250,
      color: const Color(0xFF252526),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Sidebar Header
          Container(
            padding: const EdgeInsets.all(16),
            child: Text(
              _getSidebarTitle(ref),
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
          ),
          const Divider(color: Colors.grey, height: 1),
          // Sidebar Items
          Expanded(
            child: ListView.builder(
              itemCount: items.length,
              itemBuilder: (context, index) {
                final item = items[index];
                final isSelected = item.id == selectedItemId;

                return Container(
                  margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  child: Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTap: item.onTap,
                      hoverColor: Colors.white.withOpacity(0.05),
                      borderRadius: BorderRadius.circular(4),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                        decoration: BoxDecoration(
                          color: isSelected
                              ? Colors.blueAccent.withOpacity(0.15)
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(4),
                          border: isSelected
                              ? Border(
                                  left: BorderSide(
                                    color: Colors.blueAccent,
                                    width: 3,
                                  ),
                                )
                              : null,
                        ),
                        child: Row(
                          children: [
                            Icon(
                              item.icon,
                              size: 18,
                              color: isSelected ? Colors.blueAccent : Colors.grey[400],
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Text(
                                item.label,
                                style: TextStyle(
                                  fontSize: 13,
                                  color: isSelected ? Colors.white : Colors.grey[400],
                                  fontWeight: isSelected ? FontWeight.w500 : FontWeight.normal,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  String _getSidebarTitle(WidgetRef ref) {
    final mode = ref.watch(workspaceModeProvider);
    return switch (mode) {
      WorkspaceMode.trading => 'TRADING',
      WorkspaceMode.aiPilot => 'AI PILOT',
      WorkspaceMode.social => 'COMMUNITY',
      WorkspaceMode.learning => 'LEARNING',
      _ => 'UNKNOWN',
    };
  }
}
