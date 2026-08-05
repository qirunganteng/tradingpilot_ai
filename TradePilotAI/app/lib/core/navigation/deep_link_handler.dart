import 'package:app_links/app_links.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:tradepilot/core/navigation/activity_bar.dart';

/// Deep link routes for the application
/// Format: tradingpilot://command/params
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
    
    final command = _parseCommand(
      pathSegments.isNotEmpty ? pathSegments[0] : 'unknown'
    );

    return DeepLinkData(
      command: command,
      params: uri.queryParameters,
      rawUrl: uri.toString(),
    );
  }

  static DeepLinkCommand _parseCommand(String commandStr) {
    try {
      return DeepLinkCommand.values.firstWhere(
        (e) => e.name == commandStr,
        orElse: () => DeepLinkCommand.unknown,
      );
    } catch (e) {
      return DeepLinkCommand.unknown;
    }
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
      print('Error getting initial link: $e');
    }
    return null;
  }
}

/// Handler for processing deep links and navigating
class DeepLinkHandler {
  final DeepLinkService _deepLinkService;

  DeepLinkHandler(this._deepLinkService);

  /// Process deep link and return workspace to navigate to
  WorkspaceMode? processDeepLink(DeepLinkData link) {
    switch (link.command) {
      case DeepLinkCommand.trading:
      case DeepLinkCommand.orderbook:
      case DeepLinkCommand.chart:
      case DeepLinkCommand.portfolio:
        return WorkspaceMode.trading;
      
      case DeepLinkCommand.aiPilot:
        return WorkspaceMode.aiPilot;
      
      case DeepLinkCommand.community:
        return WorkspaceMode.social;
      
      case DeepLinkCommand.learning:
        return WorkspaceMode.learning;
      
      case DeepLinkCommand.alerts:
      case DeepLinkCommand.settings:
      case DeepLinkCommand.unknown:
        return null;
    }
  }

  /// Get additional context from deep link params
  Map<String, String> getContext(DeepLinkData link) => link.params;
}

/// Riverpod provider for deep link service
final deepLinkServiceProvider = Provider((_) => DeepLinkService());

/// Riverpod provider for deep link handler
final deepLinkHandlerProvider = Provider((ref) {
  final service = ref.watch(deepLinkServiceProvider);
  return DeepLinkHandler(service);
});

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
