import 'package:flutter/material.dart';

class _WatchItem {
  final String symbol;
  final double price;
  final double changePct;
  const _WatchItem(this.symbol, this.price, this.changePct);
}

const List<_WatchItem> _kDummyWatchlist = [
  _WatchItem('BTC/USDT', 65420.10, 1.82),
  _WatchItem('ETH/USDT', 3185.55, -0.64),
  _WatchItem('SOL/USDT', 168.32, 3.21),
  _WatchItem('XAU/USD', 2412.80, 0.14),
  _WatchItem('EUR/USD', 1.0842, -0.08),
  _WatchItem('GBP/USD', 1.2731, 0.22),
  _WatchItem('US30', 39250.0, 0.41),
  _WatchItem('NAS100', 18620.5, -0.17),
];

/// Consolidated watchlist tab — part of the "Trading" column alongside
/// Orderbook, Journal and Alerts.
class WatchlistView extends StatelessWidget {
  const WatchlistView({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF1E1E1E),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(10, 6, 8, 6),
            child: Row(
              children: [
                Icon(Icons.star_outline, size: 14, color: Colors.grey[400]),
                const SizedBox(width: 6),
                const Text('Watchlist', style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold)),
                const Spacer(),
                Icon(Icons.add, size: 15, color: Colors.grey[400]),
              ],
            ),
          ),
          const Divider(height: 1, color: Color(0xFF2D2D30)),
          Expanded(
            child: ListView.separated(
              itemCount: _kDummyWatchlist.length,
              separatorBuilder: (_, __) => const Divider(height: 1, color: Color(0xFF2A2A2E)),
              itemBuilder: (context, index) {
                final item = _kDummyWatchlist[index];
                final isUp = item.changePct >= 0;
                final color = isUp ? const Color(0xFF26C485) : const Color(0xFFEF5350);
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                  child: Row(
                    children: [
                      Expanded(
                        flex: 3,
                        child: Text(
                          item.symbol,
                          style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w500),
                        ),
                      ),
                      Expanded(
                        flex: 2,
                        child: Text(
                          item.price.toStringAsFixed(item.price > 100 ? 2 : 4),
                          textAlign: TextAlign.right,
                          style: const TextStyle(fontSize: 11),
                        ),
                      ),
                      Expanded(
                        flex: 2,
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: [
                            Icon(isUp ? Icons.arrow_drop_up : Icons.arrow_drop_down, color: color, size: 15),
                            Text(
                              '${item.changePct.abs().toStringAsFixed(2)}%',
                              style: TextStyle(fontSize: 10.5, color: color, fontWeight: FontWeight.w500),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
