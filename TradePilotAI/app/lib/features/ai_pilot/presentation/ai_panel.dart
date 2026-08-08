import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import '../providers/chat_provider.dart';
import 'chat_view.dart';

class _QuickAction {
  final String label;
  final IconData icon;
  final String prompt;
  const _QuickAction(this.label, this.icon, this.prompt);
}

const List<_QuickAction> _kQuickActions = [
  _QuickAction('Analyze Chart', Icons.show_chart, 'Analisis chart yang sedang terbuka di browser saat ini.'),
  _QuickAction('Market Sentiment', Icons.insights, 'Bagaimana sentimen market untuk pair yang sedang aktif?'),
  _QuickAction('Risk Check', Icons.security, 'Cek risk/reward untuk posisi trading saya saat ini.'),
  _QuickAction('Explain Setup', Icons.school, 'Jelaskan setup supply & demand pada chart ini.'),
];

/// The right "AI" column — every AI feature (streaming chat, provider
/// selector, chart analysis shortcuts) is consolidated here so it is always
/// available regardless of which workspace mode is active.
class AiPanel extends ConsumerWidget {
  final VoidCallback? onClose;
  const AiPanel({super.key, this.onClose});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Container(
      color: const Color(0xFF121212),
      child: Column(
        children: [
          Container(
            width: double.infinity,
            color: const Color(0xFF1E1E1E),
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 10),
            child: Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final action in _kQuickActions)
                  ActionChip(
                    avatar: Icon(action.icon, size: 14, color: Colors.blueAccent[100]),
                    label: Text(action.label, style: const TextStyle(fontSize: 11)),
                    backgroundColor: const Color(0xFF252526),
                    side: BorderSide(color: Colors.grey[800]!),
                    onPressed: () => ref.read(chatProvider.notifier).sendMessage(action.prompt),
                  ),
              ],
            ),
          ),
          const Divider(height: 1, color: Color(0xFF2D2D30)),
          Expanded(child: ChatView(onClose: onClose)),
        ],
      ),
    );
  }
}
