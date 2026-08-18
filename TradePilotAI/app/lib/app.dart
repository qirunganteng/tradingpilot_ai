import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'dart:io' show Platform;

import 'core/window_management/custom_title_bar.dart';
import 'core/window_management/browser_fullscreen.dart';
import 'core/navigation/activity_bar.dart';
import 'core/navigation/deep_link_handler.dart';
import 'core/navigation/workspace_content.dart';
import 'core/updater/update_checker.dart';

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
        visualDensity: VisualDensity.compact,
      ),
      home: const MainWorkspace(),
    );
  }
}

class MainWorkspace extends ConsumerStatefulWidget {
  const MainWorkspace({super.key});

  @override
  ConsumerState<MainWorkspace> createState() => _MainWorkspaceState();
}

class _MainWorkspaceState extends ConsumerState<MainWorkspace> {
  bool _isWebFullscreen = false;
  final FocusNode _escapeFocusNode = FocusNode();
  bool _consumedInitialDeepLink = false;

  @override
  void dispose() {
    _escapeFocusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isDesktop = !kIsWeb && (Platform.isWindows || Platform.isLinux || Platform.isMacOS);

    // FlClash-style deep-link handling (tradepilot://trading,
    // tradepilot://ai-pilot, ...): a link received while the app is already
    // running arrives on deepLinkStreamProvider; a link the app was *launched
    // with* (cold start) arrives once via initialDeepLinkProvider instead --
    // both funnel into the same DeepLinkRouter so either path opens the same
    // panel. ref.listen (not .watch) since this is a one-shot side effect,
    // not something the build method should re-render off of.
    ref.listen(deepLinkStreamProvider, (previous, next) {
      next.whenData((link) => ref.read(deepLinkRouterProvider).handle(context, ref, link));
    });
    ref.listen(initialDeepLinkProvider, (previous, next) {
      if (_consumedInitialDeepLink) return;
      next.whenData((link) {
        if (link == null) return;
        _consumedInitialDeepLink = true;
        ref.read(deepLinkRouterProvider).handle(context, ref, link);
      });
    });

    // Note: the old fixed-width, always-empty "Sidebar" panel was removed.
    // WorkspaceContentBuilder below now renders one persistent 3-column
    // layout (Trading/Community/Learning dock | Browser | AI dock) — the
    // browser never gets unmounted when switching activity bar icons.
    Widget mainContent = Row(
      children: [
        // Activity Bar (Left Navigation)
        const ActivityBar(),
        // Main Content Area
        Expanded(
          child: const WorkspaceContentBuilder(),
        ),
      ],
    );

    Widget scaffoldBody = Column(
      children: [
        // On the real Windows/macOS/Linux desktop build this is the native
        // window title bar (minimize/maximize/close already wired to the OS
        // window via window_manager/bitsdojo_window). Everywhere else (web,
        // mobile) we render a matching top bar with the closest equivalents.
        if (isDesktop)
          const CustomTitleBar()
        else
          _FallbackTopBar(
            isFullscreen: _isWebFullscreen,
            onMinimize: _handleMinimize,
            onToggleMaximize: _handleToggleMaximize,
          ),
        Expanded(child: mainContent),
      ],
    );

    // Update-available banner sits above everything else, non-blocking --
    // see core/updater/update_checker.dart.
    scaffoldBody = Column(
      children: [
        const UpdateBanner(),
        Expanded(child: scaffoldBody),
      ],
    );

    // Escape closes whichever fullscreen mode is active: the browser's own
    // "maximize" (docks hidden) first, then a real web/OS fullscreen.
    return Focus(
      focusNode: _escapeFocusNode,
      autofocus: true,
      onKeyEvent: (node, event) {
        if (event is! KeyDownEvent || event.logicalKey != LogicalKeyboardKey.escape) {
          return KeyEventResult.ignored;
        }
        var handled = false;
        if (ref.read(browserMaximizedProvider)) {
          ref.read(browserMaximizedProvider.notifier).toggle();
          handled = true;
        }
        if (kIsWeb && isBrowserFullscreen) {
          toggleBrowserFullscreen();
          setState(() => _isWebFullscreen = false);
          handled = true;
        }
        return handled ? KeyEventResult.handled : KeyEventResult.ignored;
      },
      child: Scaffold(body: scaffoldBody),
    );
  }

  void _handleMinimize() {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          kIsWeb
              ? "Browsers don't let a page minimize its own tab/window — use the browser's own minimize button, or run the Windows/macOS/Linux desktop build for a real window control."
              : 'Minimize is available on the Windows/macOS/Linux desktop build.',
        ),
      ),
    );
  }

  Future<void> _handleToggleMaximize() async {
    if (kIsWeb) {
      await toggleBrowserFullscreen();
      setState(() => _isWebFullscreen = isBrowserFullscreen);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Maximize is available on the Windows/macOS/Linux desktop build.')),
      );
    }
  }
}

/// Top bar used when the real OS window title bar isn't available (web,
/// mobile). Mirrors the desktop title bar's brand mark + minimize/maximize
/// controls as closely as each platform allows.
class _FallbackTopBar extends StatelessWidget {
  final bool isFullscreen;
  final VoidCallback onMinimize;
  final VoidCallback onToggleMaximize;

  const _FallbackTopBar({
    required this.isFullscreen,
    required this.onMinimize,
    required this.onToggleMaximize,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 32,
      color: const Color(0xFF1E1E1E),
      child: Row(
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 12),
            child: AppBrandMark(size: 18),
          ),
          const Spacer(),
          IconButton(
            icon: const Icon(Icons.minimize, size: 16, color: Colors.grey),
            tooltip: 'Minimize',
            onPressed: onMinimize,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 36, minHeight: 32),
          ),
          IconButton(
            icon: Icon(
              isFullscreen ? Icons.fullscreen_exit : Icons.crop_square,
              size: 14,
              color: Colors.grey,
            ),
            tooltip: isFullscreen ? 'Restore (Esc)' : 'Maximize',
            onPressed: onToggleMaximize,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 36, minHeight: 32),
          ),
          const SizedBox(width: 6),
        ],
      ),
    );
  }
}
