import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import '../../../core/navigation/dock_panel_header.dart';
import '../../learning_lofi/providers/audio_provider.dart';
import 'orderbook_view.dart';
import 'journal_view.dart';
import 'watchlist_view.dart';

class _TradingTab {
  final String label;
  final IconData icon;
  final Widget view;
  const _TradingTab(this.label, this.icon, this.view);
}

/// The left "Trading" column — every trading-related feature (watchlist,
/// live orderbook, journal/history, alerts) lives here as tabs, so this
/// single column is the one place for all trading tools.
class TradingPanel extends StatefulWidget {
  final VoidCallback? onClose;
  const TradingPanel({super.key, this.onClose});

  @override
  State<TradingPanel> createState() => _TradingPanelState();
}

class _TradingPanelState extends State<TradingPanel> {
  int _activeIndex = 0;

  static const List<_TradingTab> _tabs = [
    _TradingTab('Watchlist', Icons.star_outline, WatchlistView()),
    _TradingTab('Orderbook', Icons.list_alt, OrderbookView()),
    _TradingTab('Journal', Icons.receipt_long, JournalView()),
    _TradingTab('Alerts', Icons.notifications_none, _AlertsView()),
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF1E1E1E),
      child: Column(
        children: [
          DockPanelHeader(title: 'TRADING', onClose: widget.onClose),
          // Standalone quick-switch buttons — same chip style as the AI
          // panel's "Analyze Chart / Market Sentiment / ..." row — instead
          // of a traditional underlined TabBar.
          Container(
            width: double.infinity,
            color: const Color(0xFF1E1E1E),
            padding: const EdgeInsets.fromLTRB(10, 8, 10, 10),
            child: Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (var i = 0; i < _tabs.length; i++)
                  ChoiceChip(
                    avatar: Icon(
                      _tabs[i].icon,
                      size: 14,
                      color: _activeIndex == i ? Colors.white : Colors.blueAccent[100],
                    ),
                    label: Text(_tabs[i].label, style: const TextStyle(fontSize: 11)),
                    selected: _activeIndex == i,
                    onSelected: (_) => setState(() => _activeIndex = i),
                    backgroundColor: const Color(0xFF252526),
                    selectedColor: Colors.blueAccent,
                    side: BorderSide(color: Colors.grey[800]!),
                    labelStyle: TextStyle(color: _activeIndex == i ? Colors.white : Colors.grey[300]),
                    visualDensity: VisualDensity.compact,
                    materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  ),
              ],
            ),
          ),
          const Divider(height: 1, color: Color(0xFF2D2D30)),
          Expanded(
            child: IndexedStack(
              index: _activeIndex,
              children: [for (final t in _tabs) t.view],
            ),
          ),
          // Lofi Radio lives here, inside the Trading column, instead of a
          // full-width footer bar spanning under the browser -- keeps it
          // out of the way while still always reachable.
          const _LofiMiniBar(),
        ],
      ),
    );
  }
}

/// Compact, single-row Lofi Radio control sized to fit inside the narrow
/// Trading column rather than the full window width.
class _LofiMiniBar extends ConsumerWidget {
  const _LofiMiniBar();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isPlaying = ref.watch(isPlayingProvider).value ?? false;
    final processingState = ref.watch(processingStateProvider).value ?? AudioProcessingState.idle;
    final volume = ref.watch(volumeProvider);
    final audioService = ref.read(audioServiceProvider);
    final isBuffering = processingState == AudioProcessingState.buffering;

    return Container(
      decoration: const BoxDecoration(
        color: Color(0xFF181818),
        border: Border(top: BorderSide(color: Color(0xFF2D2D30))),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      child: Row(
        children: [
          Container(
            width: 26,
            height: 26,
            decoration: BoxDecoration(
              color: Colors.blueAccent.withValues(alpha: 0.2),
              borderRadius: BorderRadius.circular(6),
            ),
            child: const Icon(Icons.radio, color: Colors.blueAccent, size: 15),
          ),
          const SizedBox(width: 8),
          const Expanded(
            child: Text(
              'Lofi Trading Radio',
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w500),
            ),
          ),
          if (isBuffering)
            const SizedBox(
              width: 16,
              height: 16,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          else
            InkWell(
              borderRadius: BorderRadius.circular(14),
              onTap: () => isPlaying ? audioService.pause() : audioService.play(),
              child: Icon(
                isPlaying ? Icons.pause_circle_filled : Icons.play_circle_fill,
                size: 26,
                color: Colors.white,
              ),
            ),
          PopupMenuButton<void>(
            tooltip: 'Volume',
            padding: EdgeInsets.zero,
            icon: const Icon(Icons.volume_up, size: 16, color: Colors.grey),
            itemBuilder: (context) => [
              PopupMenuItem(
                enabled: false,
                padding: EdgeInsets.zero,
                child: StatefulBuilder(
                  builder: (context, setMenuState) => SizedBox(
                    width: 140,
                    child: Slider(
                      value: volume,
                      min: 0.0,
                      max: 1.0,
                      onChanged: (val) {
                        ref.read(volumeProvider.notifier).setVolume(val);
                        audioService.setVolume(val);
                        setMenuState(() {});
                      },
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _AlertsView extends StatelessWidget {
  const _AlertsView();

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF1E1E1E),
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Icon(Icons.notifications_none, size: 40, color: Colors.grey[700]),
          const SizedBox(height: 12),
          Text('No active alerts', style: TextStyle(color: Colors.grey[400], fontSize: 13)),
          const SizedBox(height: 16),
          OutlinedButton.icon(
            onPressed: () {},
            icon: const Icon(Icons.add, size: 16),
            label: const Text('New price alert'),
            style: OutlinedButton.styleFrom(
              foregroundColor: Colors.blueAccent,
              side: const BorderSide(color: Colors.blueAccent),
            ),
          ),
        ],
      ),
    );
  }
}
