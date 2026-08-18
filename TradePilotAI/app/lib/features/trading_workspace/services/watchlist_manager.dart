import 'dart:convert';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// PRD 5.2.2 "Watchlist" -- replaces the old hardcoded `_kDummyWatchlist`
/// (a StatelessWidget rendering a `const List` that could never change)
/// with a real, persisted, user-editable list. `manualPrice`/
/// `manualChangePercent` back symbols with no free live feed (forex,
/// metals, indices -- see market_data_service.dart's header comment for
/// why); crypto symbols ignore these two fields entirely once
/// MarketDataNotifier's live ticker data arrives.
class WatchlistItem {
  final String symbol; // display form, e.g. "BTC/USDT" or "EUR/USD"
  final double? manualPrice;
  final double? manualChangePercent;

  const WatchlistItem({required this.symbol, this.manualPrice, this.manualChangePercent});

  WatchlistItem copyWith({double? manualPrice, double? manualChangePercent}) => WatchlistItem(
        symbol: symbol,
        manualPrice: manualPrice ?? this.manualPrice,
        manualChangePercent: manualChangePercent ?? this.manualChangePercent,
      );

  Map<String, dynamic> toJson() => {'symbol': symbol, 'manualPrice': manualPrice, 'manualChangePercent': manualChangePercent};
  factory WatchlistItem.fromJson(Map<String, dynamic> json) => WatchlistItem(
        symbol: json['symbol'] as String,
        manualPrice: (json['manualPrice'] as num?)?.toDouble(),
        manualChangePercent: (json['manualChangePercent'] as num?)?.toDouble(),
      );
}

/// Same eight symbols the old dummy list shipped with, now used only as
/// the *first-run seed* -- once a person edits their watchlist, this
/// constant is never consulted again.
const List<WatchlistItem> kDefaultWatchlistSeed = [
  WatchlistItem(symbol: 'BTC/USDT'),
  WatchlistItem(symbol: 'ETH/USDT'),
  WatchlistItem(symbol: 'SOL/USDT'),
  WatchlistItem(symbol: 'XAU/USD', manualPrice: 2412.80, manualChangePercent: 0.14),
  WatchlistItem(symbol: 'EUR/USD', manualPrice: 1.0842, manualChangePercent: -0.08),
  WatchlistItem(symbol: 'GBP/USD', manualPrice: 1.2731, manualChangePercent: 0.22),
  WatchlistItem(symbol: 'US30', manualPrice: 39250.0, manualChangePercent: 0.41),
  WatchlistItem(symbol: 'NAS100', manualPrice: 18620.5, manualChangePercent: -0.17),
];

class WatchlistNotifier extends Notifier<List<WatchlistItem>> {
  static const _key = 'tradepilot_watchlist_v1';

  @override
  List<WatchlistItem> build() {
    _load();
    return List.of(kDefaultWatchlistSeed);
  }

  Future<void> _load() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_key);
      if (raw == null) {
        // First run -- persist the seed immediately so future edits have
        // something real to modify instead of re-seeding every launch.
        await _save(state);
        return;
      }
      final list = (jsonDecode(raw) as List<dynamic>).map((e) => WatchlistItem.fromJson(e as Map<String, dynamic>)).toList();
      state = list;
    } catch (_) {}
  }

  Future<void> _save(List<WatchlistItem> items) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_key, jsonEncode(items.map((e) => e.toJson()).toList()));
    } catch (_) {}
  }

  void addSymbol(String symbol, {double? manualPrice, double? manualChangePercent}) {
    final normalized = symbol.trim().toUpperCase();
    if (normalized.isEmpty || state.any((w) => w.symbol == normalized)) return;
    state = [...state, WatchlistItem(symbol: normalized, manualPrice: manualPrice, manualChangePercent: manualChangePercent)];
    _save(state);
  }

  void removeSymbol(String symbol) {
    state = state.where((w) => w.symbol != symbol).toList();
    _save(state);
  }

  void setManualPrice(String symbol, double price, double changePercent) {
    state = [
      for (final w in state)
        if (w.symbol == symbol) w.copyWith(manualPrice: price, manualChangePercent: changePercent) else w,
    ];
    _save(state);
  }
}

final watchlistProvider = NotifierProvider<WatchlistNotifier, List<WatchlistItem>>(WatchlistNotifier.new);
