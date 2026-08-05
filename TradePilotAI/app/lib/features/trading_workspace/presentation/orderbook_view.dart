import 'package:flutter/material.dart';
import 'package:pluto_grid/pluto_grid.dart';

class OrderbookView extends StatefulWidget {
  const OrderbookView({super.key});

  @override
  State<OrderbookView> createState() => _OrderbookViewState();
}

class _OrderbookViewState extends State<OrderbookView> {
  final List<PlutoColumn> columns = [
    PlutoColumn(
      title: 'Price',
      field: 'price',
      type: PlutoColumnType.number(),
      width: 100,
      backgroundColor: Colors.transparent,
    ),
    PlutoColumn(
      title: 'Amount',
      field: 'amount',
      type: PlutoColumnType.number(),
      width: 100,
      backgroundColor: Colors.transparent,
    ),
    PlutoColumn(
      title: 'Total',
      field: 'total',
      type: PlutoColumnType.number(),
      width: 100,
      backgroundColor: Colors.transparent,
    ),
  ];

  late final List<PlutoRow> rows;

  @override
  void initState() {
    super.initState();
    // Generate dummy orderbook data
    rows = List.generate(
      50,
      (index) => PlutoRow(
        cells: {
          'price': PlutoCell(value: 65000 + (index * 10)),
          'amount': PlutoCell(value: (index % 5 + 1) * 0.5),
          'total': PlutoCell(value: (65000 + (index * 10)) * ((index % 5 + 1) * 0.5)),
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF1E1E1E),
      padding: const EdgeInsets.all(8.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.only(bottom: 8.0),
            child: Text(
              'Orderbook (BTC/USDT)',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ),
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
                  activatedBorderColor: Colors.blueAccent,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
