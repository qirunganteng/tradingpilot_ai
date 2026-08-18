import 'dart:convert';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// PRD 5.2.5 "Journal" -- replaces the old `_JournalViewState` (a
/// StatefulWidget whose `rows` were `List.generate(20, ...)` fake trades
/// that reset every app launch) with real, persisted, user-entered trade
/// journal entries.
class JournalEntry {
  final String id;
  final DateTime time;
  final String pair;
  final String side; // "BUY" | "SELL"
  final double price;
  final double pnl;
  final String? notes;

  const JournalEntry({
    required this.id,
    required this.time,
    required this.pair,
    required this.side,
    required this.price,
    required this.pnl,
    this.notes,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'time': time.toIso8601String(),
        'pair': pair,
        'side': side,
        'price': price,
        'pnl': pnl,
        'notes': notes,
      };

  factory JournalEntry.fromJson(Map<String, dynamic> json) => JournalEntry(
        id: json['id'] as String,
        time: DateTime.parse(json['time'] as String),
        pair: json['pair'] as String,
        side: json['side'] as String,
        price: (json['price'] as num).toDouble(),
        pnl: (json['pnl'] as num).toDouble(),
        notes: json['notes'] as String?,
      );
}

class JournalNotifier extends Notifier<List<JournalEntry>> {
  static const _key = 'tradepilot_journal_v1';

  @override
  List<JournalEntry> build() {
    _load();
    return [];
  }

  Future<void> _load() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_key);
      if (raw == null) return;
      final list = (jsonDecode(raw) as List<dynamic>).map((e) => JournalEntry.fromJson(e as Map<String, dynamic>)).toList();
      list.sort((a, b) => b.time.compareTo(a.time));
      state = list;
    } catch (_) {}
  }

  Future<void> _save() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_key, jsonEncode(state.map((e) => e.toJson()).toList()));
    } catch (_) {}
  }

  void addEntry({required String pair, required String side, required double price, required double pnl, String? notes}) {
    final entry = JournalEntry(
      id: 'jrn_${DateTime.now().millisecondsSinceEpoch}',
      time: DateTime.now(),
      pair: pair.trim().toUpperCase(),
      side: side,
      price: price,
      pnl: pnl,
      notes: notes,
    );
    state = [entry, ...state];
    _save();
  }

  void removeEntry(String id) {
    state = state.where((e) => e.id != id).toList();
    _save();
  }
}

final journalProvider = NotifierProvider<JournalNotifier, List<JournalEntry>>(JournalNotifier.new);
