import 'package:app_links/app_links.dart';
import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:tradepilot/core/navigation/activity_bar.dart';
import 'package:tradepilot/core/navigation/app_settings_dialog.dart';

/// Deep link routes for the application
/// Format: tradepilot://command/params
enum DeepLinkCommand {
  trading,
  aiPilot,
  community,
  learning,
  orderbook,
  chart,
  portfolio,
  alerts,
  settings,
  unknown,
}

class DeepLinkData {
  final DeepLinkCommand command;
  final Map<String, String> params;
  final String rawUrl;

  DeepLinkData({
    required this.command,
    required this.params,
    required this.rawUrl,
  });

  factory DeepLinkData.fromUri(Uri uri) {
    final pathSegments = uri.pathSegments;

    // A URI like tradepilot://trading has "trading" as the *host*, not a
    // path segment (there's no path at all) -- while tradepilot:///trading
    // (three slashes) puts it in pathSegments[0] instead. Accept both forms
    // since it's easy for anyone hand-writing a link to use either.
    final commandStr = pathSegments.isNotEmpty
        ? pathSegments[0]
        : (uri.host.isNotEmpty ? uri.host : 'unknown');

    return DeepLinkData(
      command: _parseCommand(commandStr),
      params: uri.queryParameters,
      rawUrl: uri.toString(),
    );
  }

  static DeepLinkCommand _parseCommand(String commandStr) {
    return DeepLinkCommand.values.firstWhere(
      (e) => e.name.toLowerCase() == commandStr.toLowerCase(),
      orElse: () => DeepLinkCommand.unknown,
    );
  }

  @override
  String toString() => 'DeepLinkData(command: $command, params: $params)';
}

/// Service to handle deep links
class DeepLinkService {
  final AppLinks _appLinks = AppLinks();

  Stream<DeepLinkData> get deepLinkStream => _appLinks.uriLinkStream.map(
    (uri) => DeepLinkData.fromUri(uri),
  );

  /// Get initial link if app was launched via deep link
  Future<DeepLinkData?> getInitialLink() async {
    try {
      final uri = await _appLinks.getInitialLink();
      if (uri != null) {
        return DeepLinkData.fromUri(uri);
      }
    } catch (e) {
      debugPrintDeepLinkError('Error getting initial link: $e');
    }
    return null;
  }
}

void debugPrintDeepLinkError(String message) {
  assert(() {
    // ignore: avoid_print
    print(message);
    return true;
  }());
}

/// Applies a parsed [DeepLinkData] directly to the current dock-based
/// navigation model (see activity_bar.dart's [LeftDockMode] /
/// [aiPanelVisibleProvider]) -- this replaces an earlier version that
/// mapped onto a `WorkspaceMode` enum from an older full-workspace-switch
/// navigation design that the UI no longer uses at all, which meant every
/// deep link silently did nothing no matter how it was triggered.
///
/// Deliberately force-*opens* the target panel (`.set(...)`) rather than
/// toggling it -- a link should always land you on the thing it names, not
/// sometimes close it if you happened to already have it open.
class DeepLinkRouter {
  const DeepLinkRouter();

  void handle(BuildContext context, WidgetRef ref, DeepLinkData link) {
    switch (link.command) {
      case DeepLinkCommand.trading:
      case DeepLinkCommand.orderbook:
      case DeepLinkCommand.chart:
      case DeepLinkCommand.portfolio:
      case DeepLinkCommand.alerts:
        ref.read(leftDockModeProvider.notifier).set(LeftDockMode.trading);
        break;

      case DeepLinkCommand.aiPilot:
        ref.read(aiPanelVisibleProvider.notifier).set(true);
        break;

      case DeepLinkCommand.community:
        ref.read(leftDockModeProvider.notifier).set(LeftDockMode.social);
        break;

      case DeepLinkCommand.learning:
        ref.read(leftDockModeProvider.notifier).set(LeftDockMode.learning);
        break;

      case DeepLinkCommand.settings:
        showDialog(context: context, builder: (_) => const AppSettingsDialog());
        break;

      case DeepLinkCommand.unknown:
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Unrecognized link: ${link.rawUrl}')),
        );
        break;
    }
  }
}

/// Riverpod provider for deep link service
final deepLinkServiceProvider = Provider((_) => DeepLinkService());

final deepLinkRouterProvider = Provider((_) => const DeepLinkRouter());

/// Stream of deep link events
final deepLinkStreamProvider = StreamProvider((ref) {
  final service = ref.watch(deepLinkServiceProvider);
  return service.deepLinkStream;
});

/// Get initial deep link (if app was launched via deep link)
final initialDeepLinkProvider = FutureProvider((ref) async {
  final service = ref.watch(deepLinkServiceProvider);
  return await service.getInitialLink();
});
