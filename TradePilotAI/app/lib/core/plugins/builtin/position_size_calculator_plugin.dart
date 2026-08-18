import 'package:flutter/material.dart';
import '../plugin_interface.dart';

/// PRD §5.3.1 "Lot Size Calculator" -- built-in demonstration plugin that
/// proves the plugin system (plugin_manager.dart) actually works
/// end-to-end: registered through [PluginManager.registry], toggled
/// on/off from the Plugins manager (see plugin_manager_dialog.dart), and
/// its [workspaceWidget] is real, working calculator UI, not a stub.
///
/// Formula per the PRD: Lot Size = (Account Balance * Risk %) / (Stop Loss
/// in Pips * Pip Value).
class PositionSizeCalculatorPlugin extends TradePilotPlugin {
  @override
  String get id => 'position_size_calculator';
  @override
  String get name => 'Position Size Calculator';
  @override
  String get version => '1.0.0';
  @override
  String get description => 'Calculates lot size from account balance, risk %, and stop-loss distance (PRD 5.3.1).';

  @override
  Future<void> onLoad() async {}

  @override
  Future<void> onUnload() async {}

  @override
  Widget get workspaceWidget => const _PositionSizeCalculatorView();
}

class _PositionSizeCalculatorView extends StatefulWidget {
  const _PositionSizeCalculatorView();

  @override
  State<_PositionSizeCalculatorView> createState() => _PositionSizeCalculatorViewState();
}

class _PositionSizeCalculatorViewState extends State<_PositionSizeCalculatorView> {
  final _balanceController = TextEditingController(text: '10000');
  final _riskController = TextEditingController(text: '1');
  final _stopLossController = TextEditingController(text: '50');
  final _pipValueController = TextEditingController(text: '1.0');

  double? _lotSize;
  double? _riskAmount;

  @override
  void dispose() {
    _balanceController.dispose();
    _riskController.dispose();
    _stopLossController.dispose();
    _pipValueController.dispose();
    super.dispose();
  }

  void _calculate() {
    final balance = double.tryParse(_balanceController.text);
    final riskPercent = double.tryParse(_riskController.text);
    final stopLossPips = double.tryParse(_stopLossController.text);
    final pipValue = double.tryParse(_pipValueController.text);

    if (balance == null || riskPercent == null || stopLossPips == null || pipValue == null || stopLossPips <= 0 || pipValue <= 0) {
      setState(() {
        _lotSize = null;
        _riskAmount = null;
      });
      return;
    }

    final riskAmount = balance * (riskPercent / 100);
    final lotSize = riskAmount / (stopLossPips * pipValue);
    setState(() {
      _riskAmount = riskAmount;
      _lotSize = lotSize;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF1E1E1E),
      padding: const EdgeInsets.all(20),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 360),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Icon(Icons.calculate_outlined, color: Colors.blueAccent[100]),
                const SizedBox(width: 8),
                const Text('Position Size Calculator', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 16),
            _field('Account Balance (\$)', _balanceController),
            _field('Risk (%)', _riskController),
            _field('Stop Loss (pips)', _stopLossController),
            _field('Pip Value (\$ per pip)', _pipValueController),
            const SizedBox(height: 8),
            FilledButton(onPressed: _calculate, child: const Text('Calculate')),
            if (_lotSize != null) ...[
              const SizedBox(height: 20),
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: const Color(0xFF252526),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey[800]!),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Result: ${_lotSize!.toStringAsFixed(2)} Lots', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.greenAccent)),
                    const SizedBox(height: 6),
                    Text('Risk amount: \$${_riskAmount!.toStringAsFixed(2)}', style: TextStyle(fontSize: 12.5, color: Colors.grey[400])),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _field(String label, TextEditingController controller) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: TextField(
        controller: controller,
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        style: const TextStyle(fontSize: 13),
        decoration: InputDecoration(
          labelText: label,
          isDense: true,
          filled: true,
          fillColor: const Color(0xFF2D2D30),
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(6), borderSide: BorderSide.none),
        ),
      ),
    );
  }
}
