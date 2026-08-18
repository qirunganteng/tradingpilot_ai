import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import '../services/journal_manager.dart';

/// PRD 5.2.5 "Journal" -- real, persisted trade journal entries, added
/// manually via the "+" button. Replaces the old PlutoGrid rendering 20
/// fake `List.generate` trades that reset every launch.
class JournalView extends ConsumerWidget {
  const JournalView({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final entries = ref.watch(journalProvider);
    return Container(
      color: const Color(0xFF1E1E1E),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(10, 6, 8, 6),
            child: Row(
              children: [
                Icon(Icons.receipt_long, size: 14, color: Colors.grey[400]),
                const SizedBox(width: 6),
                const Text('Journal', style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.bold)),
                const Spacer(),
                InkWell(
                  onTap: () => _showAddEntryDialog(context, ref),
                  borderRadius: BorderRadius.circular(4),
                  child: Icon(Icons.add, size: 15, color: Colors.grey[400]),
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: Color(0xFF2D2D30)),
          Expanded(
            child: entries.isEmpty
                ? Center(
                    child: Text('No trades logged yet. Tap + to add one.', style: TextStyle(color: Colors.grey[500], fontSize: 12.5)),
                  )
                : ListView.separated(
                    itemCount: entries.length,
                    separatorBuilder: (_, __) => const Divider(height: 1, color: Color(0xFF2A2A2E)),
                    itemBuilder: (context, index) {
                      final e = entries[index];
                      final isBuy = e.side == 'BUY';
                      final isProfit = e.pnl >= 0;
                      return ListTile(
                        dense: true,
                        leading: Container(
                          width: 36,
                          height: 22,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            color: (isBuy ? Colors.greenAccent : Colors.redAccent).withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text(
                            e.side,
                            style: TextStyle(fontSize: 9.5, fontWeight: FontWeight.bold, color: isBuy ? Colors.greenAccent : Colors.redAccent),
                          ),
                        ),
                        title: Text('${e.pair} @ ${e.price}', style: const TextStyle(fontSize: 12.5)),
                        subtitle: Text(
                          '${e.time.hour.toString().padLeft(2, '0')}:${e.time.minute.toString().padLeft(2, '0')}'
                          '${e.notes != null && e.notes!.isNotEmpty ? ' \u2014 ${e.notes}' : ''}',
                          style: TextStyle(fontSize: 10.5, color: Colors.grey[500]),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              '${isProfit ? '+' : ''}${e.pnl.toStringAsFixed(2)}',
                              style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: isProfit ? const Color(0xFF26C485) : const Color(0xFFEF5350)),
                            ),
                            IconButton(
                              icon: const Icon(Icons.close, size: 14),
                              onPressed: () => ref.read(journalProvider.notifier).removeEntry(e.id),
                              padding: EdgeInsets.zero,
                              constraints: const BoxConstraints(minWidth: 22, minHeight: 22),
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

  void _showAddEntryDialog(BuildContext context, WidgetRef ref) {
    final pairController = TextEditingController();
    final priceController = TextEditingController();
    final pnlController = TextEditingController();
    final notesController = TextEditingController();
    var side = 'BUY';
    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF2D2D30),
          title: const Text('Log a trade', style: TextStyle(fontSize: 15)),
          content: SizedBox(
            width: 320,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: pairController,
                  textCapitalization: TextCapitalization.characters,
                  decoration: const InputDecoration(labelText: 'Pair', hintText: 'e.g. BTC/USDT'),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: DropdownButtonFormField<String>(
                        initialValue: side,
                        dropdownColor: const Color(0xFF2D2D30),
                        items: const [
                          DropdownMenuItem(value: 'BUY', child: Text('BUY', style: TextStyle(fontSize: 13))),
                          DropdownMenuItem(value: 'SELL', child: Text('SELL', style: TextStyle(fontSize: 13))),
                        ],
                        onChanged: (v) => setDialogState(() => side = v ?? 'BUY'),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: priceController,
                        keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        decoration: const InputDecoration(labelText: 'Entry price'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: pnlController,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
                  decoration: const InputDecoration(labelText: 'PNL (\$)', hintText: 'Positive or negative'),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: notesController,
                  decoration: const InputDecoration(labelText: 'Notes (optional)'),
                  maxLines: 2,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
            TextButton(
              onPressed: () {
                final price = double.tryParse(priceController.text.trim());
                final pnl = double.tryParse(pnlController.text.trim());
                if (pairController.text.trim().isEmpty || price == null || pnl == null) return;
                ref.read(journalProvider.notifier).addEntry(
                      pair: pairController.text,
                      side: side,
                      price: price,
                      pnl: pnl,
                      notes: notesController.text.trim().isEmpty ? null : notesController.text.trim(),
                    );
                Navigator.pop(ctx);
              },
              child: const Text('Save'),
            ),
          ],
        ),
      ),
    );
  }
}
