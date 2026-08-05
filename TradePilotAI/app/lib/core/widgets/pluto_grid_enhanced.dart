import 'dart:async';
import 'package:flutter/material.dart';
import 'package:pluto_grid/pluto_grid.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

/// Helper class for orderbook real-time updates
class OrderbookUpdate {
  final String price;
  final String amount;
  final String total;
  final int rowIndex;

  OrderbookUpdate({
    required this.price,
    required this.amount,
    required this.total,
    required this.rowIndex,
  });

  PlutoCell toCell(String value) => PlutoCell(value: value);

  Map<String, PlutoCell> toCells() => {
    'price': toCell(price),
    'amount': toCell(amount),
    'total': toCell(total),
  };
}

/// Helper class for trade journal real-time updates
class TradeUpdate {
  final String time;
  final String pair;
  final String side;
  final String price;
  final String pnl;
  final int rowIndex;

  TradeUpdate({
    required this.time,
    required this.pair,
    required this.side,
    required this.price,
    required this.pnl,
    required this.rowIndex,
  });

  Map<String, PlutoCell> toCells() => {
    'time': PlutoCell(value: time),
    'pair': PlutoCell(value: pair),
    'side': PlutoCell(value: side),
    'price': PlutoCell(value: price),
    'pnl': PlutoCell(value: pnl),
  };
}

/// PlutoGrid configuration presets for dark theme
class PlutoGridConfigs {
  static PlutoGridConfiguration createDarkConfig() {
    return PlutoGridConfiguration(
      style: PlutoGridStyleConfig.dark(
        gridBackgroundColor: const Color(0xFF1E1E1E),
        rowColor: const Color(0xFF252526),
        gridBorderColor: const Color(0xFF3F3F46),
        borderColor: const Color(0xFF3F3F46),
        activatedColor: const Color(0xFF2196F3).withOpacity(0.2),
        activatedBorderColor: const Color(0xFF2196F3),
        cellTextStyle: const TextStyle(
          color: Color(0xFFFFFFFF),
          fontSize: 12,
        ),
        columnTextStyle: const TextStyle(
          color: Color(0xFFB0B0B0),
          fontSize: 12,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  static PlutoGridConfiguration createCompactConfig() {
    return PlutoGridConfiguration(
      style: PlutoGridStyleConfig.dark(
        gridBackgroundColor: const Color(0xFF1E1E1E),
        rowHeight: 32,
        columnHeight: 36,
        gridBorderColor: const Color(0xFF3F3F46),
        activatedColor: const Color(0xFF2196F3).withOpacity(0.15),
        cellTextStyle: const TextStyle(
          color: Color(0xFFFFFFFF),
          fontSize: 11,
        ),
      ),
    );
  }
}
