import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import '../network/app_config.dart';
import '../plugins/plugin_manager_dialog.dart';

/// TradePilot's own app-level Settings dialog -- distinct from the
/// embedded browser's Chrome-style settings (which only configures the
/// browser widget itself). This one configures things that affect the
/// whole app: the Cloudflare AI Gateway URL + token the AI Pilot /
/// chart-analysis calls hit.
class AppSettingsDialog extends ConsumerStatefulWidget {
  const AppSettingsDialog({super.key});

  @override
  ConsumerState<AppSettingsDialog> createState() => _AppSettingsDialogState();
}

class _AppSettingsDialogState extends ConsumerState<AppSettingsDialog> {
  late final TextEditingController _urlController;
  late final TextEditingController _tokenController;
  bool _obscureToken = true;

  @override
  void initState() {
    super.initState();
    final config = ref.read(appConfigProvider);
    _urlController = TextEditingController(text: config.gatewayUrl);
    _tokenController = TextEditingController(text: config.gatewayToken);
  }

  @override
  void dispose() {
    _urlController.dispose();
    _tokenController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: const Color(0xFF2D2D30),
      child: SizedBox(
        width: 460,
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Row(
                children: [
                  Icon(Icons.settings, size: 18, color: Colors.grey),
                  SizedBox(width: 8),
                  Text('TradePilot Settings', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
                ],
              ),
              const SizedBox(height: 20),
              const Text(
                'Backend / AI Gateway URL',
                style: TextStyle(fontSize: 12.5, color: Colors.grey),
              ),
              const SizedBox(height: 6),
              TextField(
                controller: _urlController,
                style: const TextStyle(fontSize: 13),
                decoration: InputDecoration(
                  isDense: true,
                  filled: true,
                  fillColor: const Color(0xFF3C3C3C),
                  hintText: AppConfig.defaultGatewayUrl,
                  contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(6),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
              const SizedBox(height: 4),
              const Text(
                "Points every AI Pilot request (chat streaming, chart analysis) at your "
                "Cloudflare Worker / AI Gateway. Change this if you're running the "
                "backend locally on a different port, or against a deployed URL.",
                style: TextStyle(fontSize: 11, color: Colors.grey),
              ),
              const SizedBox(height: 18),
              const Text(
                'Gateway Token',
                style: TextStyle(fontSize: 12.5, color: Colors.grey),
              ),
              const SizedBox(height: 6),
              TextField(
                controller: _tokenController,
                obscureText: _obscureToken,
                style: const TextStyle(fontSize: 13),
                decoration: InputDecoration(
                  isDense: true,
                  filled: true,
                  fillColor: const Color(0xFF3C3C3C),
                  hintText: 'Matches the backend\'s GATEWAY_AUTH_TOKEN secret',
                  contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(6),
                    borderSide: BorderSide.none,
                  ),
                  suffixIcon: IconButton(
                    icon: Icon(_obscureToken ? Icons.visibility_off : Icons.visibility, size: 16),
                    onPressed: () => setState(() => _obscureToken = !_obscureToken),
                  ),
                ),
              ),
              const SizedBox(height: 4),
              const Text(
                'Sent as an Authorization header on every request so the Worker knows '
                'this is the TradePilot app talking, not per-provider AI keys (those '
                'stay on the backend only). Leave empty to use offline demo responses.',
                style: TextStyle(fontSize: 11, color: Colors.grey),
              ),
              const SizedBox(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  OutlinedButton.icon(
                    onPressed: () {
                      Navigator.pop(context);
                      showDialog(context: context, builder: (_) => const PluginManagerDialog());
                    },
                    icon: const Icon(Icons.extension_outlined, size: 16),
                    label: const Text('Manage Plugins'),
                  ),
                  Row(
                    children: [
                      TextButton(
                        onPressed: () {
                          _urlController.text = AppConfig.defaultGatewayUrl;
                        },
                        child: const Text('Reset to default'),
                      ),
                      const SizedBox(width: 8),
                      TextButton(
                        onPressed: () => Navigator.pop(context),
                        child: const Text('Cancel'),
                      ),
                      const SizedBox(width: 8),
                      FilledButton(
                        onPressed: () {
                          ref.read(appConfigProvider.notifier).setGatewayUrl(_urlController.text);
                          ref.read(appConfigProvider.notifier).setGatewayToken(_tokenController.text);
                          Navigator.pop(context);
                        },
                        child: const Text('Save'),
                      ),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
