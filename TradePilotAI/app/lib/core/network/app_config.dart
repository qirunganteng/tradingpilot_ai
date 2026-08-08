import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Centralized, persisted configuration for how the Flutter client talks to
/// the TradePilot backend (Cloudflare Workers / AI Gateway / D1 / R2).
class AppConfig {
  final String gatewayUrl;

  /// Sent as `Authorization: Bearer <token>` on every backend request.
  /// This is *not* a per-provider AI API key (those stay server-side only,
  /// per CONSTITUTION.md security rules) -- it's the shared app-level
  /// token that matches the backend's own GATEWAY_AUTH_TOKEN secret, so
  /// the Worker can tell "this is the TradePilot app" apart from anyone
  /// else hitting the endpoint.
  final String gatewayToken;

  const AppConfig({required this.gatewayUrl, this.gatewayToken = ''});

  AppConfig copyWith({String? gatewayUrl, String? gatewayToken}) {
    return AppConfig(
      gatewayUrl: gatewayUrl ?? this.gatewayUrl,
      gatewayToken: gatewayToken ?? this.gatewayToken,
    );
  }

  static const String defaultGatewayUrl = 'https://tradepilot-ai-gateway.servisand.workers.dev';
}

class AppConfigNotifier extends Notifier<AppConfig> {
  static const _gatewayUrlKey = 'tradepilot_gateway_url';
  static const _gatewayTokenKey = 'tradepilot_gateway_token';

  @override
  AppConfig build() {
    _loadPersisted();
    return const AppConfig(gatewayUrl: AppConfig.defaultGatewayUrl);
  }

  Future<void> _loadPersisted() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final savedUrl = prefs.getString(_gatewayUrlKey);
      final savedToken = prefs.getString(_gatewayTokenKey);
      state = state.copyWith(
        gatewayUrl: (savedUrl != null && savedUrl.isNotEmpty) ? savedUrl : null,
        gatewayToken: savedToken ?? '',
      );
    } catch (_) {}
  }

  Future<void> setGatewayUrl(String url) async {
    final trimmed = url.trim();
    if (trimmed.isEmpty) return;
    state = state.copyWith(gatewayUrl: trimmed);
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_gatewayUrlKey, trimmed);
    } catch (_) {}
  }

  Future<void> setGatewayToken(String token) async {
    final trimmed = token.trim();
    state = state.copyWith(gatewayToken: trimmed);
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_gatewayTokenKey, trimmed);
    } catch (_) {}
  }

  Future<void> resetToDefault() => setGatewayUrl(AppConfig.defaultGatewayUrl);
}

final appConfigProvider = NotifierProvider<AppConfigNotifier, AppConfig>(AppConfigNotifier.new);
