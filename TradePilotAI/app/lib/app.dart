import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'dart:io' show Platform;

import 'core/window_management/custom_title_bar.dart';
import 'core/navigation/activity_bar.dart';
import 'core/navigation/sidebar.dart';
import 'core/navigation/workspace_content.dart';
import 'features/learning_lofi/presentation/mini_player_view.dart';

class TradePilotApp extends ConsumerWidget {
  const TradePilotApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp(
      title: 'TradePilot AI',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        colorSchemeSeed: Colors.blueAccent,
        useMaterial3: true,
      ),
      home: const MainWorkspace(),
    );
  }
}

class MainWorkspace extends StatefulWidget {
  const MainWorkspace({super.key});

  @override
  State<MainWorkspace> createState() => _MainWorkspaceState();
}

class _MainWorkspaceState extends State<MainWorkspace> {
  String? _selectedSidebarItem;

  @override
  Widget build(BuildContext context) {
    final isDesktop = !kIsWeb && (Platform.isWindows || Platform.isLinux || Platform.isMacOS);

    Widget mainContent = Row(
      children: [
        // Activity Bar (Left Navigation)
        const ActivityBar(),
        // Sidebar (Feature-specific items)
        Sidebar(
          items: const [], // Will be populated based on workspace mode
          selectedItemId: _selectedSidebarItem,
        ),
        // Main Content Area
        Expanded(
          child: Column(
            children: [
              Expanded(
                child: const WorkspaceContentBuilder(),
              ),
              const MiniPlayerView(), // Bottom Bar (Lofi Player)
            ],
          ),
        ),
      ],
    );

    Widget scaffoldBody = Column(
      children: [
        if (isDesktop) const CustomTitleBar(),
        Expanded(child: mainContent),
      ],
    );

    if (isDesktop) {
      return Scaffold(
        body: scaffoldBody,
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('TradePilot AI')),
      body: scaffoldBody,
    );
  }
}
