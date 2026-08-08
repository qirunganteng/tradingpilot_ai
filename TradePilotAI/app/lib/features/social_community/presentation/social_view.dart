import 'package:flutter/material.dart';
import '../../../core/navigation/dock_panel_header.dart';

class SocialView extends StatelessWidget {
  final VoidCallback? onClose;
  const SocialView({super.key, this.onClose});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF1E1E1E),
      child: Column(
        children: [
          DockPanelHeader(title: 'COMMUNITY', onClose: onClose),
          Expanded(
            child: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.people_alt_outlined, size: 64, color: Colors.blue[300]),
                  const SizedBox(height: 16),
                  const Text(
                    'Social & Community',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  const Padding(
                    padding: EdgeInsets.symmetric(horizontal: 16),
                    child: Text(
                      'Chat Room Trader & Signal Sharing\n(Coming Soon)',
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
