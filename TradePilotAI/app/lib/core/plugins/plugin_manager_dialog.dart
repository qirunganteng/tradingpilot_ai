import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'plugin_manager.dart';

/// PRD §11 "Plugin System" management screen -- list every registered
/// plugin, toggle it on/off (persisted -- see PluginManager.setEnabled),
/// and open its [TradePilotPlugin.workspaceWidget] once enabled. Reached
/// from the app Settings dialog.
class PluginManagerDialog extends ConsumerWidget {
  const PluginManagerDialog({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final plugins = ref.watch(pluginManagerProvider);
    final manager = ref.read(pluginManagerProvider.notifier);

    return Dialog(
      backgroundColor: const Color(0xFF2D2D30),
      child: SizedBox(
        width: 440,
        height: 420,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Row(
                children: [
                  Icon(Icons.extension_outlined, size: 18, color: Colors.grey),
                  SizedBox(width: 8),
                  Text('Plugins', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
                ],
              ),
              const SizedBox(height: 4),
              Text(
                'Built-in extensions -- enable one to use it. See docs/known-limitations.md '
                'for why third-party plugin installs aren\'t supported yet.',
                style: TextStyle(fontSize: 11, color: Colors.grey[500]),
              ),
              const SizedBox(height: 12),
              Expanded(
                child: plugins.isEmpty
                    ? Center(child: Text('No plugins registered.', style: TextStyle(color: Colors.grey[500], fontSize: 13)))
                    : ListView.separated(
                        itemCount: plugins.length,
                        separatorBuilder: (_, __) => const Divider(height: 1, color: Color(0xFF1E1E1E)),
                        itemBuilder: (context, index) {
                          final plugin = plugins[index];
                          final enabled = manager.isEnabled(plugin.id);
                          return ListTile(
                            contentPadding: EdgeInsets.zero,
                            title: Text(plugin.name, style: const TextStyle(fontSize: 13)),
                            subtitle: Text(
                              '${plugin.description}\nv${plugin.version}',
                              style: TextStyle(fontSize: 11, color: Colors.grey[500]),
                            ),
                            isThreeLine: true,
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                if (enabled && plugin.workspaceWidget != null)
                                  IconButton(
                                    icon: const Icon(Icons.open_in_new, size: 18),
                                    tooltip: 'Open',
                                    onPressed: () {
                                      Navigator.pop(context);
                                      showDialog(
                                        context: context,
                                        builder: (ctx) => Dialog(
                                          backgroundColor: const Color(0xFF1E1E1E),
                                          child: SizedBox(
                                            width: 420,
                                            height: 480,
                                            child: plugin.workspaceWidget,
                                          ),
                                        ),
                                      );
                                    },
                                  ),
                                Switch(
                                  value: enabled,
                                  onChanged: (value) => manager.setEnabled(plugin.id, value),
                                  activeThumbColor: Colors.blueAccent,
                                ),
                              ],
                            ),
                          );
                        },
                      ),
              ),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(onPressed: () => Navigator.pop(context), child: const Text('Close')),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
