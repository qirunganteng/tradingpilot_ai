import 'dart:async';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../core/network/app_config.dart';
import '../services/ai_stream_service.dart';
import '../services/chat_session_service.dart' as persisted;

class ChatMessage {
  final String role;
  final String content;
  final DateTime timestamp;

  ChatMessage({
    required this.role,
    required this.content,
    DateTime? timestamp,
  }) : timestamp = timestamp ?? DateTime.now();

  ChatMessage copyWith({String? content, DateTime? timestamp}) {
    return ChatMessage(
      role: role,
      content: content ?? this.content,
      timestamp: timestamp ?? this.timestamp,
    );
  }
}

final aiStreamServiceProvider = Provider((ref) {
  final config = ref.watch(appConfigProvider);
  return AiStreamService(
    backendUrl: config.gatewayUrl,
    apiKey: config.gatewayToken.isEmpty ? null : config.gatewayToken,
  );
});

class AiProviderNotifier extends Notifier<AiProvider> {
  @override
  AiProvider build() => AiProvider.gemini;

  void setProvider(AiProvider provider) {
    state = provider;
  }
}

final selectedAiProviderProvider = NotifierProvider<AiProviderNotifier, AiProvider>(AiProviderNotifier.new);

/// PRD "AI Pilot Workspace" / Chatbox reference (conversation management:
/// save sessions, switch, delete) -- `ChatSessionService` existed as a
/// complete, working class but was never actually called from anywhere,
/// so every conversation was lost on restart and there was no way to have
/// more than one. This wires it in: the active conversation persists
/// automatically, and [listSessions]/[loadSession]/[deleteSession] back a
/// history picker (see chat_view.dart) for switching between or removing
/// past ones.
const _activeSessionKey = 'tradepilot_active_chat_session';

class ChatNotifier extends Notifier<List<ChatMessage>> {
  String? _activeSessionId;

  @override
  List<ChatMessage> build() {
    _restoreActiveSession();
    return [];
  }

  Future<persisted.ChatSessionService> _service() async {
    final prefs = await SharedPreferences.getInstance();
    return persisted.ChatSessionService(prefs);
  }

  Future<void> _restoreActiveSession() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final lastId = prefs.getString(_activeSessionKey);
      if (lastId == null) return;
      final service = persisted.ChatSessionService(prefs);
      final session = await service.loadSession(lastId);
      if (session == null) return;
      _activeSessionId = session.id;
      state = session.messages.map((m) => ChatMessage(role: m.role, content: m.content, timestamp: m.timestamp)).toList();
    } catch (_) {
      // No saved session, or it's corrupt -- start with an empty chat
      // either way rather than blocking the UI on this.
    }
  }

  Future<void> _persist() async {
    if (state.isEmpty) return;
    try {
      final prefs = await SharedPreferences.getInstance();
      final service = persisted.ChatSessionService(prefs);
      _activeSessionId ??= 'chat_${DateTime.now().millisecondsSinceEpoch}';
      final firstUserMessage = state.firstWhere((m) => m.role == 'user', orElse: () => state.first).content;
      final title = firstUserMessage.length > 48 ? '${firstUserMessage.substring(0, 48)}\u2026' : firstUserMessage;
      final session = persisted.ChatSession(
        id: _activeSessionId!,
        title: title.isEmpty ? 'New chat' : title,
        messages: [
          for (var i = 0; i < state.length; i++)
            persisted.ChatMessage(
              id: '${_activeSessionId}_$i',
              role: state[i].role,
              content: state[i].content,
              timestamp: state[i].timestamp,
            ),
        ],
        createdAt: state.first.timestamp,
        updatedAt: DateTime.now(),
        aiProvider: ref.read(selectedAiProviderProvider).name,
      );
      await service.saveSession(session);
      await prefs.setString(_activeSessionKey, _activeSessionId!);
    } catch (_) {
      // Persistence is a convenience, never worth interrupting the chat over.
    }
  }

  Future<void> sendMessage(String prompt) async {
    final service = ref.read(aiStreamServiceProvider);
    final provider = ref.read(selectedAiProviderProvider);

    // Add user message
    state = [
      ...state,
      ChatMessage(role: 'user', content: prompt),
    ];
    unawaited(_persist());

    // Add empty assistant message for streaming
    state = [
      ...state,
      ChatMessage(role: 'assistant', content: ''),
    ];
    
    final aiMessageIndex = state.length - 1;

    try {
      // Stream chat response — useMock only kicks in if there's no gateway
      // token configured yet, so the UI still feels alive out of the box
      // instead of just erroring, while a real backend connection (once
      // the user sets a Gateway URL + token in Settings) takes over
      // automatically.
      final hasBackend = ref.read(appConfigProvider).gatewayToken.isNotEmpty;
      await for (final chunk in service.streamChat(
        prompt: prompt,
        provider: provider,
        useMock: !hasBackend,
      )) {
        final updatedMessages = List<ChatMessage>.from(state);
        final currentMessage = updatedMessages[aiMessageIndex];

        updatedMessages[aiMessageIndex] = currentMessage.copyWith(
          content: currentMessage.content + chunk,
        );
        state = updatedMessages;
      }
    } catch (e) {
      final updatedMessages = List<ChatMessage>.from(state);
      updatedMessages[aiMessageIndex] = updatedMessages[aiMessageIndex].copyWith(
        content: '❌ Error: ${e.toString()}',
      );
      state = updatedMessages;
    }
    unawaited(_persist());
  }

  /// Starts a brand-new conversation -- the previous one stays saved in
  /// history (see [listSessions]), it's just no longer the active one.
  Future<void> startNewChat() async {
    state = [];
    _activeSessionId = null;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_activeSessionKey);
    } catch (_) {}
  }

  /// Kept for the existing "Clear history" button -- same effect as
  /// starting a new chat (the old one remains in the history picker
  /// rather than being deleted, matching how Chatbox and most chat apps
  /// treat "clear" versus an explicit "delete").
  void clearHistory() => startNewChat();

  Future<List<persisted.ChatSession>> listSessions() async {
    final service = await _service();
    return service.getAllSessions();
  }

  Future<void> loadSession(String id) async {
    final service = await _service();
    final session = await service.loadSession(id);
    if (session == null) return;
    _activeSessionId = session.id;
    state = session.messages.map((m) => ChatMessage(role: m.role, content: m.content, timestamp: m.timestamp)).toList();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_activeSessionKey, id);
  }

  Future<void> deleteSession(String id) async {
    final service = await _service();
    await service.deleteSession(id);
    if (id == _activeSessionId) {
      await startNewChat();
    }
  }
}

final chatProvider = NotifierProvider<ChatNotifier, List<ChatMessage>>(ChatNotifier.new);
