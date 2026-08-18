import 'dart:convert';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// PRD 5.2.3 "Price Alerts" -- replaces the old `_AlertsView` (a static
/// "No active alerts" screen whose "New price alert" button had an empty
/// `onPressed: () {}`) with real, persisted alerts that actually get
/// checked against live/manual watchlist prices.
enum AlertCondition { above, below }

class PriceAlert {
  final String id;
  final String symbol;
  final AlertCondition condition;
  final double targetPrice;
  final DateTime createdAt;
  bool isActive;
  bool isTriggered;

  PriceAlert({
    required this.id,
    required this.symbol,
    required this.condition,
    required this.targetPrice,
    required this.createdAt,
    this.isActive = true,
    this.isTriggered = false,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'symbol': symbol,
        'condition': condition.name,
        'targetPrice': targetPrice,
        'createdAt': createdAt.toIso8601String(),
        'isActive': isActive,
        'isTriggered': isTriggered,
      };

  factory PriceAlert.fromJson(Map<String, dynamic> json) => PriceAlert(
        id: json['id'] as String,
        symbol: json['symbol'] as String,
        condition: AlertCondition.values.firstWhere((c) => c.name == json['condition'], orElse: () => AlertCondition.above),
        targetPrice: (json['targetPrice'] as num).toDouble(),
        createdAt: DateTime.parse(json['createdAt'] as String),
        isActive: json['isActive'] as bool? ?? true,
        isTriggered: json['isTriggered'] as bool? ?? false,
      );
}

class AlertNotifier extends Notifier<List<PriceAlert>> {
  static const _key = 'tradepilot_price_alerts_v1';

  @override
  List<PriceAlert> build() {
    _load();
    return [];
  }

  Future<void> _load() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_key);
      if (raw == null) return;
      state = (jsonDecode(raw) as List<dynamic>).map((e) => PriceAlert.fromJson(e as Map<String, dynamic>)).toList();
    } catch (_) {}
  }

  Future<void> _save() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_key, jsonEncode(state.map((e) => e.toJson()).toList()));
    } catch (_) {}
  }

  void addAlert(String symbol, AlertCondition condition, double targetPrice) {
    state = [
      ...state,
      PriceAlert(
        id: 'alert_${DateTime.now().millisecondsSinceEpoch}',
        symbol: symbol.trim().toUpperCase(),
        condition: condition,
        targetPrice: targetPrice,
        createdAt: DateTime.now(),
      ),
    ];
    _save();
  }

  void removeAlert(String id) {
    state = state.where((a) => a.id != id).toList();
    _save();
  }

  void togglePause(String id) {
    state = [
      for (final a in state)
        if (a.id == id) (a..isActive = !a.isActive) else a,
    ];
    _save();
  }

  /// Called whenever a symbol's price updates (from live ticker data or a
  /// manual edit) -- flips `isTriggered` on any matching active alert.
  /// Returns the alerts that *just* triggered (for a one-shot
  /// notification), leaving already-triggered ones out so the same alert
  /// doesn't re-notify every single price tick.
  List<PriceAlert> checkPrice(String symbol, double price) {
    final justTriggered = <PriceAlert>[];
    var changed = false;
    for (final a in state) {
      if (a.symbol == symbol && a.isActive && !a.isTriggered) {
        final crossed = a.condition == AlertCondition.above ? price >= a.targetPrice : price <= a.targetPrice;
        if (crossed) {
          a.isTriggered = true;
          changed = true;
          justTriggered.add(a);
        }
      }
    }
    if (changed) {
      state = List.of(state);
      _save();
    }
    return justTriggered;
  }
}

final alertsProvider = NotifierProvider<AlertNotifier, List<PriceAlert>>(AlertNotifier.new);
