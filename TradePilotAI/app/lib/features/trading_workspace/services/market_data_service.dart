import 'dart:async';
import 'dart:convert';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

/// Live market data for the Trading panel (Watchlist, Orderbook, Alerts) --
/// backed by Binance's public WebSocket streams, which need no API key/auth
/// and are free for this kind of read-only ticker/depth data. This is
/// intentionally scoped to crypto pairs (Binance only lists those): forex
/// pairs (EUR/USD), metals (XAU/USD), and indices (US30, NAS100) have no
/// equivalent free, no-signup, real-time feed, so those stay as
/// manually-entered watchlist entries (see WatchlistManager) rather than
/// faking a "live" price that isn't actually live. See
/// docs/known-limitations.md for the full reasoning.
class TickerData {
  final String symbol; // Binance symbol, e.g. "BTCUSDT"
  final double price;
  final double changePercent24h;
  final DateTime updatedAt;
  const TickerData({required this.symbol, required this.price, required this.changePercent24h, required this.updatedAt});
}

/// One price level: [price, quantity].
typedef DepthLevel = (double price, double quantity);

class OrderBookData {
  final String symbol;
  final List<DepthLevel> bids; // highest first
  final List<DepthLevel> asks; // lowest first
  const OrderBookData({required this.symbol, required this.bids, required this.asks});
}

/// True for symbols Binance actually lists (a real, if imperfect, proxy:
/// Binance only trades crypto, so anything ending in a stablecoin/major
/// quote currency is worth trying; forex/indices/metals never match this
/// and fall back to the manual-price path everywhere this is checked).
bool isLikelyBinanceSymbol(String displaySymbol) {
  final normalized = toBinanceSymbol(displaySymbol);
  const cryptoQuotes = ['USDT', 'USDC', 'BUSD', 'BTC', 'ETH'];
  for (final quote in cryptoQuotes) {
    if (normalized.endsWith(quote) && normalized.length > quote.length) return true;
  }
  return false;
}

/// "BTC/USDT" -> "BTCUSDT" (Binance's wire format has no separator).
String toBinanceSymbol(String displaySymbol) => displaySymbol.replaceAll('/', '').replaceAll('-', '').toUpperCase();

class MarketDataNotifier extends Notifier<Map<String, TickerData>> {
  WebSocketChannel? _tickerChannel;
  StreamSubscription? _tickerSub;
  final Set<String> _subscribed = {};

  WebSocketChannel? _depthChannel;
  StreamSubscription? _depthSub;
  String? _depthSymbol;
  final _depthController = StreamController<OrderBookData>.broadcast();
  Stream<OrderBookData> get depthStream => _depthController.stream;

  @override
  Map<String, TickerData> build() {
    ref.onDispose(() {
      _tickerSub?.cancel();
      _tickerChannel?.sink.close();
      _depthSub?.cancel();
      _depthChannel?.sink.close();
      _depthController.close();
    });
    return {};
  }

  /// Subscribes to 24h mini-ticker updates for [displaySymbols] (any
  /// format, e.g. "BTC/USDT") -- reconnects with the full combined-stream
  /// URL whenever the watchlist's symbol set changes, since Binance's
  /// combined-stream endpoint is fixed at connect time.
  void syncSubscriptions(Iterable<String> displaySymbols) {
    final binanceSymbols = displaySymbols
        .where(isLikelyBinanceSymbol)
        .map((s) => toBinanceSymbol(s).toLowerCase())
        .toSet();
    if (binanceSymbols.isEmpty || setEquals(binanceSymbols, _subscribed)) {
      if (binanceSymbols.isEmpty) _closeTickerChannel();
      return;
    }
    _subscribed
      ..clear()
      ..addAll(binanceSymbols);
    _closeTickerChannel();

    final streams = binanceSymbols.map((s) => '$s@ticker').join('/');
    final uri = Uri.parse('wss://stream.binance.com:9443/stream?streams=$streams');
    try {
      _tickerChannel = WebSocketChannel.connect(uri);
      _tickerSub = _tickerChannel!.stream.listen(_handleTickerMessage, onError: (_) {}, cancelOnError: false);
    } catch (_) {
      // Offline / blocked network -- watchlist just keeps showing whatever
      // manual/last-known price it already had.
    }
  }

  void _closeTickerChannel() {
    _tickerSub?.cancel();
    _tickerChannel?.sink.close();
    _tickerChannel = null;
    _tickerSub = null;
  }

  void _handleTickerMessage(dynamic raw) {
    try {
      final decoded = jsonDecode(raw as String) as Map<String, dynamic>;
      final data = decoded['data'] as Map<String, dynamic>?;
      if (data == null) return;
      final symbol = data['s'] as String; // e.g. BTCUSDT
      final price = double.tryParse(data['c'] as String? ?? '') ?? 0;
      final changePct = double.tryParse(data['P'] as String? ?? '') ?? 0;
      state = {
        ...state,
        symbol: TickerData(symbol: symbol, price: price, changePercent24h: changePct, updatedAt: DateTime.now()),
      };
    } catch (_) {
      // Malformed/partial frame -- ignore, next tick will self-correct.
    }
  }

  /// Live order book depth (top 20 levels, updated every 100ms) for one
  /// symbol at a time -- used by OrderbookView for whichever crypto
  /// symbol is currently selected.
  void watchDepth(String displaySymbol) {
    final binanceSymbol = toBinanceSymbol(displaySymbol).toLowerCase();
    if (binanceSymbol == _depthSymbol) return;
    _depthSymbol = binanceSymbol;
    _depthSub?.cancel();
    _depthChannel?.sink.close();

    if (!isLikelyBinanceSymbol(displaySymbol)) return;

    final uri = Uri.parse('wss://stream.binance.com:9443/ws/$binanceSymbol@depth20@100ms');
    try {
      _depthChannel = WebSocketChannel.connect(uri);
      _depthSub = _depthChannel!.stream.listen((raw) {
        try {
          final decoded = jsonDecode(raw as String) as Map<String, dynamic>;
          final bids = (decoded['bids'] as List<dynamic>? ?? [])
              .map((e) => (double.parse(e[0] as String), double.parse(e[1] as String)))
              .toList();
          final asks = (decoded['asks'] as List<dynamic>? ?? [])
              .map((e) => (double.parse(e[0] as String), double.parse(e[1] as String)))
              .toList();
          if (!_depthController.isClosed) {
            _depthController.add(OrderBookData(symbol: displaySymbol, bids: bids, asks: asks));
          }
        } catch (_) {}
      }, onError: (_) {}, cancelOnError: false);
    } catch (_) {}
  }
}

bool setEquals<T>(Set<T> a, Set<T> b) => a.length == b.length && a.containsAll(b);

final marketDataProvider = NotifierProvider<MarketDataNotifier, Map<String, TickerData>>(MarketDataNotifier.new);
