import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import '../services/market_data_service.dart';
import '../services/watchlist_manager.dart';

/// PRD 5.2 area, live order book -- real bid/ask depth from Binance's
/// public depth stream (see market_data_service.dart) for whichever
/// crypto pair is currently selected, replacing the old PlutoGrid filled
/// with a fake `65000 + index * 10` price ladder. Forex/indices have no
/// free public order-book feed (that data is broker-proprietary), so this
/// view only has something live to show for crypto symbols -- see
/// docs/known-limitations.md.
class OrderbookView extends ConsumerStatefulWidget {
  const OrderbookView({super.key});

  @override
  ConsumerState<OrderbookView> createState() => _OrderbookViewState();
}

class _OrderbookViewState extends ConsumerState<OrderbookView> {
  String? _selectedSymbol;

  @override
  Widget build(BuildContext context) {
    final watchlist = ref.watch(watchlistProvider);
    final cryptoSymbols = watchlist.map((w) => w.symbol).where(isLikelyBinanceSymbol).toList();

    // Default to the first crypto symbol on the watchlist the first time
    // there is one; keeps whatever the person explicitly picked otherwise.
    _selectedSymbol ??= cryptoSymbols.isNotEmpty ? cryptoSymbols.first : null;
    if (_selectedSymbol != null && !cryptoSymbols.contains(_selectedSymbol)) {
      _selectedSymbol = cryptoSymbols.isNotEmpty ? cryptoSymbols.first : null;
    }

    if (_selectedSymbol != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ref.read(marketDataProvider.notifier).watchDepth(_selectedSymbol!);
      });
    }

    return Container(
      color: const Color(0xFF1E1E1E),
      padding: const EdgeInsets.all(6.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(bottom: 6.0, left: 2, right: 2),
            child: Row(
              children: [
                if (cryptoSymbols.isEmpty)
                  Text(
                    'Add a crypto pair to your Watchlist to see its order book.',
                    style: TextStyle(fontSize: 11, color: Colors.grey[500]),
                  )
                else
                  DropdownButtonHideUnderline(
                    child: DropdownButton<String>(
                      value: _selectedSymbol,
                      isDense: true,
                      dropdownColor: const Color(0xFF2D2D30),
                      style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: Colors.white),
                      items: [
                        for (final s in cryptoSymbols) DropdownMenuItem(value: s, child: Text(s)),
                      ],
                      onChanged: (v) => setState(() => _selectedSymbol = v),
                    ),
                  ),
              ],
            ),
          ),
          if (_selectedSymbol != null)
            Expanded(
              child: StreamBuilder<OrderBookData>(
                stream: ref.watch(marketDataProvider.notifier).depthStream,
                builder: (context, snapshot) {
                  final data = snapshot.data;
                  if (data == null || data.symbol != _selectedSymbol) {
                    return const Center(child: CircularProgressIndicator(strokeWidth: 2));
                  }
                  return Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(child: _DepthColumn(levels: data.bids, isBid: true)),
                      const SizedBox(width: 6),
                      Expanded(child: _DepthColumn(levels: data.asks, isBid: false)),
                    ],
                  );
                },
              ),
            ),
        ],
      ),
    );
  }
}

class _DepthColumn extends StatelessWidget {
  final List<DepthLevel> levels;
  final bool isBid;
  const _DepthColumn({required this.levels, required this.isBid});

  @override
  Widget build(BuildContext context) {
    final color = isBid ? const Color(0xFF26C485) : const Color(0xFFEF5350);
    final maxQty = levels.isEmpty ? 1.0 : levels.map((l) => l.$2).reduce((a, b) => a > b ? a : b);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: [
            Expanded(child: Text(isBid ? 'Bid' : 'Ask', style: TextStyle(fontSize: 10.5, color: Colors.grey[500], fontWeight: FontWeight.w600))),
            Text('Qty', style: TextStyle(fontSize: 10.5, color: Colors.grey[500], fontWeight: FontWeight.w600)),
          ],
        ),
        const Divider(height: 6, color: Color(0xFF2A2A2E)),
        for (final level in levels.take(20))
          Stack(
            children: [
              Positioned.fill(
                child: FractionallySizedBox(
                  alignment: isBid ? Alignment.centerLeft : Alignment.centerRight,
                  widthFactor: (level.$2 / maxQty).clamp(0.02, 1.0),
                  child: Container(color: color.withValues(alpha: 0.10)),
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 1.5),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        level.$1.toStringAsFixed(level.$1 > 100 ? 2 : 4),
                        style: TextStyle(fontSize: 11, color: color, fontFeatures: const [FontFeature.tabularFigures()]),
                      ),
                    ),
                    Text(
                      level.$2.toStringAsFixed(4),
                      style: const TextStyle(fontSize: 10.5, color: Colors.white70, fontFeatures: [FontFeature.tabularFigures()]),
                    ),
                  ],
                ),
              ),
            ],
          ),
      ],
    );
  }
}
