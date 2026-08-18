import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import '../services/market_data_service.dart';
import '../services/watchlist_manager.dart';
import '../services/alert_manager.dart';

/// PRD 5.2.2 "Watchlist" -- real, persisted, editable symbol list with
/// live crypto prices (see market_data_service.dart). Replaces the old
/// static `_kDummyWatchlist` StatelessWidget.
class WatchlistView extends ConsumerStatefulWidget {
  const WatchlistView({super.key});

  @override
  ConsumerState<WatchlistView> createState() => _WatchlistViewState();
}

class _WatchlistViewState extends ConsumerState<WatchlistView> {
  @override
  Widget build(BuildContext context) {
    final items = ref.watch(watchlistProvider);
    final tickers = ref.watch(marketDataProvider);

    // Keep the live-price WebSocket subscription in sync with whichever
    // crypto symbols are actually on the watchlist right now -- added
    // symbols start streaming within a second or two, removed ones stop
    // (Binance's combined-stream endpoint requires a fresh connection
    // rather than a subscribe/unsubscribe message, so this reconnects
    // whenever the *set* of symbols changes -- see syncSubscriptions's own
    // early-exit-if-unchanged check, which keeps this cheap to call from
    // build()).
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(marketDataProvider.notifier).syncSubscriptions(items.map((w) => w.symbol));
      for (final item in items) {
        final live = tickers[toBinanceSymbol(item.symbol)];
        final price = live?.price ?? item.manualPrice;
        if (price != null) {
          ref.read(alertsProvider.notifier).checkPrice(item.symbol, price);
        }
      }
    });

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
                InkWell(
                  onTap: () => _showAddSymbolDialog(context),
                  borderRadius: BorderRadius.circular(4),
                  child: Icon(Icons.add, size: 15, color: Colors.grey[400]),
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: Color(0xFF2D2D30)),
          Expanded(
            child: items.isEmpty
                ? Center(
                    child: Text('No symbols yet. Tap + to add one.', style: TextStyle(color: Colors.grey[500], fontSize: 12.5)),
                  )
                : ListView.separated(
                    itemCount: items.length,
                    separatorBuilder: (_, __) => const Divider(height: 1, color: Color(0xFF2A2A2E)),
                    itemBuilder: (context, index) {
                      final item = items[index];
                      final isCrypto = isLikelyBinanceSymbol(item.symbol);
                      final live = tickers[toBinanceSymbol(item.symbol)];
                      final price = isCrypto ? live?.price : item.manualPrice;
                      final changePct = isCrypto ? live?.changePercent24h : item.manualChangePercent;
                      final isUp = (changePct ?? 0) >= 0;
                      final color = isUp ? const Color(0xFF26C485) : const Color(0xFFEF5350);

                      return InkWell(
                        onTap: () => isCrypto ? null : _showEditManualPriceDialog(context, item),
                        onLongPress: () => _showRemoveConfirm(context, item.symbol),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                          child: Row(
                            children: [
                              Expanded(
                                flex: 3,
                                child: Row(
                                  children: [
                                    if (isCrypto)
                                      Container(
                                        width: 5,
                                        height: 5,
                                        margin: const EdgeInsets.only(right: 5),
                                        decoration: BoxDecoration(
                                          color: live != null ? Colors.greenAccent : Colors.grey[700],
                                          shape: BoxShape.circle,
                                        ),
                                      ),
                                    Flexible(
                                      child: Text(
                                        item.symbol,
                                        overflow: TextOverflow.ellipsis,
                                        style: const TextStyle(fontSize: 11.5, fontWeight: FontWeight.w500),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Expanded(
                                flex: 2,
                                child: Text(
                                  price == null ? '—' : price.toStringAsFixed(price > 100 ? 2 : 4),
                                  textAlign: TextAlign.right,
                                  style: const TextStyle(fontSize: 11),
                                ),
                              ),
                              Expanded(
                                flex: 2,
                                child: Row(
                                  mainAxisAlignment: MainAxisAlignment.end,
                                  children: [
                                    if (changePct != null) ...[
                                      Icon(isUp ? Icons.arrow_drop_up : Icons.arrow_drop_down, color: color, size: 15),
                                      Text(
                                        '${changePct.abs().toStringAsFixed(2)}%',
                                        style: TextStyle(fontSize: 10.5, color: color, fontWeight: FontWeight.w500),
                                      ),
                                    ] else
                                      Text('—', style: TextStyle(fontSize: 10.5, color: Colors.grey[600])),
                                  ],
                                ),
                              ),
                            ],
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

  void _showAddSymbolDialog(BuildContext context) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: const Text('Add symbol', style: TextStyle(fontSize: 15)),
        content: TextField(
          controller: controller,
          autofocus: true,
          textCapitalization: TextCapitalization.characters,
          decoration: const InputDecoration(
            hintText: 'e.g. BTC/USDT, EUR/USD, XAU/USD',
            helperText: 'Crypto (Binance) pairs get live prices automatically.',
          ),
          onSubmitted: (v) {
            ref.read(watchlistProvider.notifier).addSymbol(v);
            Navigator.pop(ctx);
          },
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(
            onPressed: () {
              ref.read(watchlistProvider.notifier).addSymbol(controller.text);
              Navigator.pop(ctx);
            },
            child: const Text('Add'),
          ),
        ],
      ),
    );
  }

  void _showRemoveConfirm(BuildContext context, String symbol) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: const Text('Remove symbol?', style: TextStyle(fontSize: 15)),
        content: Text('Remove "$symbol" from your watchlist.', style: const TextStyle(fontSize: 13)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(
            onPressed: () {
              ref.read(watchlistProvider.notifier).removeSymbol(symbol);
              Navigator.pop(ctx);
            },
            child: const Text('Remove'),
          ),
        ],
      ),
    );
  }

  void _showEditManualPriceDialog(BuildContext context, WatchlistItem item) {
    final priceController = TextEditingController(text: item.manualPrice?.toString() ?? '');
    final changeController = TextEditingController(text: item.manualChangePercent?.toString() ?? '');
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: Text('Update ${item.symbol}', style: const TextStyle(fontSize: 15)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'No free live feed for this symbol -- enter its price manually.',
              style: TextStyle(fontSize: 11.5, color: Colors.grey[500]),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: priceController,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: 'Price'),
            ),
            TextField(
              controller: changeController,
              keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
              decoration: const InputDecoration(labelText: 'Change % (optional)'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(
            onPressed: () {
              final price = double.tryParse(priceController.text.trim());
              final change = double.tryParse(changeController.text.trim()) ?? 0;
              if (price != null) {
                ref.read(watchlistProvider.notifier).setManualPrice(item.symbol, price, change);
              }
              Navigator.pop(ctx);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }
}
