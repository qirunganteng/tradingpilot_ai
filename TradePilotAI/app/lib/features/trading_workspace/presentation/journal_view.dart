import 'package:flutter/material.dart';
import 'package:pluto_grid/pluto_grid.dart';

class JournalView extends StatefulWidget {
  const JournalView({super.key});

  @override
  State<JournalView> createState() => _JournalViewState();
}

class _JournalViewState extends State<JournalView> {
  final List<PlutoColumn> columns = [
    PlutoColumn(
      title: 'Time',
      field: 'time',
      type: PlutoColumnType.time(),
      width: 120,
    ),
    PlutoColumn(
      title: 'Pair',
      field: 'pair',
      type: PlutoColumnType.text(),
      width: 100,
    ),
    PlutoColumn(
      title: 'Side',
      field: 'side',
      type: PlutoColumnType.text(),
      width: 80,
    ),
    PlutoColumn(
      title: 'Price',
      field: 'price',
      type: PlutoColumnType.number(),
      width: 100,
    ),
    PlutoColumn(
      title: 'PNL',
      field: 'pnl',
      type: PlutoColumnType.number(),
      width: 100,
    ),
  ];

  late final List<PlutoRow> rows;

  @override
  void initState() {
    super.initState();
    rows = List.generate(
      20,
      (index) {
        final isBuy = index % 2 == 0;
        final pnl = isBuy ? (index * 12.5) : -(index * 5.0);
        return PlutoRow(
          cells: {
            'time': PlutoCell(value: '10:${index.toString().padLeft(2, '0')}'),
            'pair': PlutoCell(value: 'BTC/USDT'),
            'side': PlutoCell(value: isBuy ? 'BUY' : 'SELL'),
            'price': PlutoCell(value: 64000 + (index * 50)),
            'pnl': PlutoCell(value: pnl),
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF1E1E1E),
      padding: const EdgeInsets.all(6.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: PlutoGrid(
              columns: columns,
              rows: rows,
              configuration: PlutoGridConfiguration(
                style: PlutoGridStyleConfig.dark(
                  gridBackgroundColor: Colors.transparent,
                  rowColor: Colors.transparent,
                  gridBorderColor: Colors.grey[800]!,
                  borderColor: Colors.grey[800]!,
                  activatedColor: Colors.blueAccent.withOpacity(0.2),
                  cellTextStyle: const TextStyle(fontSize: 11.5),
                  columnTextStyle: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600),
                  rowHeight: 26,
                  columnHeight: 28,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
