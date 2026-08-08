import 'package:flutter/material.dart';
import '../../../core/navigation/dock_panel_header.dart';

class EducationalView extends StatelessWidget {
  final VoidCallback? onClose;
  const EducationalView({super.key, this.onClose});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF1E1E1E),
      child: Column(
        children: [
          DockPanelHeader(title: 'LEARNING', onClose: onClose),
          Expanded(
            child: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.school_outlined, size: 64, color: Colors.green[300]),
                  const SizedBox(height: 16),
                  const Text(
                    'Educational Hub & Paper Trading',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 16),
                    child: Text(
                      'Interactive Market Lessons\n(Coming Soon)',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.grey, fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
