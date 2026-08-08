import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:tradepilot/features/trading_workspace/presentation/browser_view.dart';
import 'package:tradepilot/features/trading_workspace/presentation/trading_panel.dart';
import 'package:tradepilot/features/ai_pilot/presentation/ai_panel.dart';
import 'package:tradepilot/features/social_community/presentation/social_view.dart';
import 'package:tradepilot/features/learning_lofi/presentation/educational_view.dart';
import 'activity_bar.dart';

/// VSCode-style persistent workspace body.
///
/// The [BrowserView] in the center is created exactly ONCE and never leaves
/// the widget tree — clicking activity bar icons (Trading / AI Pilot /
/// Community / Learning) only shows/hides the docks around it via
/// [leftDockModeProvider] and [aiPanelVisibleProvider]. Every child here has
/// a stable [Key], so Flutter's element reconciliation keeps their State
/// (browser tabs, AI chat history, etc.) alive no matter which docks are
/// toggled open/closed — including going fully fullscreen when both docks
/// are closed, exactly like VSCode's editor area.
class WorkspaceContentBuilder extends ConsumerStatefulWidget {
  const WorkspaceContentBuilder({super.key});

  @override
  ConsumerState<WorkspaceContentBuilder> createState() => _WorkspaceContentBuilderState();
}

class _WorkspaceContentBuilderState extends ConsumerState<WorkspaceContentBuilder> {
  double _leftWidth = 240;
  double _rightWidth = 300;

  static const double _minPanelWidth = 200;
  static const double _maxPanelWidth = 520;
  static const double _handleWidth = 4;

  @override
  Widget build(BuildContext context) {
    final leftMode = ref.watch(leftDockModeProvider);
    final aiVisible = ref.watch(aiPanelVisibleProvider);

    Widget? leftPanel;
    switch (leftMode) {
      case LeftDockMode.trading:
        leftPanel = TradingPanel(onClose: () => ref.read(leftDockModeProvider.notifier).close());
        break;
      case LeftDockMode.social:
        leftPanel = SocialView(onClose: () => ref.read(leftDockModeProvider.notifier).close());
        break;
      case LeftDockMode.learning:
        leftPanel = EducationalView(onClose: () => ref.read(leftDockModeProvider.notifier).close());
        break;
      case null:
        leftPanel = null;
        break;
    }

    return Row(
      children: [
        if (leftPanel != null) ...[
          SizedBox(
            key: const ValueKey('left-dock'),
            width: _leftWidth,
            child: leftPanel,
          ),
          _ResizeHandle(
            key: const ValueKey('left-handle'),
            width: _handleWidth,
            onDrag: (dx) {
              setState(() => _leftWidth = (_leftWidth + dx).clamp(_minPanelWidth, _maxPanelWidth));
            },
          ),
        ],
        const Expanded(
          key: ValueKey('browser-expanded'),
          child: BrowserView(key: ValueKey('main-browser')),
        ),
        if (aiVisible) ...[
          _ResizeHandle(
            key: const ValueKey('right-handle'),
            width: _handleWidth,
            onDrag: (dx) {
              setState(() => _rightWidth = (_rightWidth - dx).clamp(_minPanelWidth, _maxPanelWidth));
            },
          ),
          SizedBox(
            key: const ValueKey('right-dock'),
            width: _rightWidth,
            child: AiPanel(
              key: const ValueKey('ai-panel'),
              onClose: () => ref.read(aiPanelVisibleProvider.notifier).close(),
            ),
          ),
        ],
      ],
    );
  }
}

/// A thin, draggable divider between docks and the browser, with a resize
/// cursor on desktop — matches VSCode's panel-resize handles.
class _ResizeHandle extends StatelessWidget {
  final double width;
  final ValueChanged<double> onDrag;
  const _ResizeHandle({super.key, required this.width, required this.onDrag});

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      cursor: SystemMouseCursors.resizeColumn,
      child: GestureDetector(
        behavior: HitTestBehavior.translucent,
        onHorizontalDragUpdate: (details) => onDrag(details.delta.dx),
        child: SizedBox(
          width: width,
          child: Center(
            child: Container(width: 1, color: const Color(0xFF2D2D30)),
          ),
        ),
      ),
    );
  }
}
