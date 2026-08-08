import 'package:flutter/material.dart';

/// Shared header used at the top of every left-dock panel (Trading,
/// Community, Learning) and the AI panel, so each has a consistent title
/// row plus an "x" button to collapse/close that dock — mirroring VSCode's
/// sidebar panel headers.
class DockPanelHeader extends StatelessWidget {
  final String title;
  final VoidCallback? onClose;
  final Widget? trailing;

  const DockPanelHeader({
    super.key,
    required this.title,
    this.onClose,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF252526),
      height: 28,
      padding: const EdgeInsets.only(left: 10, right: 4),
      child: Row(
        children: [
          Expanded(
            child: Text(
              title,
              style: const TextStyle(
                fontSize: 10.5,
                fontWeight: FontWeight.bold,
                letterSpacing: 0.6,
                color: Colors.grey,
              ),
            ),
          ),
          if (trailing != null) trailing!,
          if (onClose != null)
            Tooltip(
              message: 'Close panel',
              child: InkWell(
                onTap: onClose,
                borderRadius: BorderRadius.circular(4),
                child: const Padding(
                  padding: EdgeInsets.all(4),
                  child: Icon(Icons.close, size: 14, color: Colors.grey),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
