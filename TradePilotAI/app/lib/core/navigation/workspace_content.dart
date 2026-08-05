import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:multi_split_view/multi_split_view.dart';
import 'package:tradepilot/features/trading_workspace/presentation/browser_view.dart';
import 'package:tradepilot/features/trading_workspace/presentation/orderbook_view.dart';
import 'package:tradepilot/features/trading_workspace/presentation/journal_view.dart';
import 'package:tradepilot/features/ai_pilot/presentation/chat_view.dart';
import 'package:tradepilot/features/social_community/presentation/social_view.dart';
import 'package:tradepilot/features/learning_lofi/presentation/educational_view.dart';
import 'activity_bar.dart';

class WorkspaceContentBuilder extends ConsumerWidget {
  const WorkspaceContentBuilder({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final mode = ref.watch(workspaceModeProvider);

    return switch (mode) {
      WorkspaceMode.trading => const TradingWorkspaceContent(),
      WorkspaceMode.aiPilot => const AiPilotWorkspaceContent(),
      WorkspaceMode.social => const SocialWorkspaceContent(),
      WorkspaceMode.learning => const LearningWorkspaceContent(),
      _ => const Placeholder(),
    };
  }
}

/// Trading Workspace: Browser + Chart + Orderbook + Journal
class TradingWorkspaceContent extends StatefulWidget {
  const TradingWorkspaceContent({super.key});

  @override
  State<TradingWorkspaceContent> createState() => _TradingWorkspaceContentState();
}

class _TradingWorkspaceContentState extends State<TradingWorkspaceContent> {
  final MultiSplitViewController _controller = MultiSplitViewController(
    areas: [
      Area(
        flex: 0.15,
        min: 0.1,
        builder: (context, area) => const OrderbookView(),
      ),
      Area(
        flex: 0.55,
        min: 0.3,
        builder: (context, area) => const BrowserView(),
      ),
      Area(
        flex: 0.3,
        min: 0.2,
        builder: (context, area) => const JournalView(),
      ),
    ],
  );

  @override
  Widget build(BuildContext context) {
    return MultiSplitViewTheme(
      data: MultiSplitViewThemeData(
        dividerPainter: DividerPainters.grooved1(
          color: Colors.grey[800]!,
          highlightedColor: Colors.blue,
        ),
      ),
      child: MultiSplitView(
        controller: _controller,
      ),
    );
  }
}

/// AI Pilot Workspace: Focus on chat interface
class AiPilotWorkspaceContent extends StatelessWidget {
  const AiPilotWorkspaceContent({super.key});

  @override
  Widget build(BuildContext context) {
    return const ChatView();
  }
}

/// Social & Community Workspace
class SocialWorkspaceContent extends StatelessWidget {
  const SocialWorkspaceContent({super.key});

  @override
  Widget build(BuildContext context) {
    return const SocialView();
  }
}

/// Learning & Entertainment Workspace
class LearningWorkspaceContent extends StatelessWidget {
  const LearningWorkspaceContent({super.key});

  @override
  Widget build(BuildContext context) {
    return const EducationalView();
  }
}
