import 'package:hooks_riverpod/hooks_riverpod.dart';
import '../../../core/network/app_config.dart';
import '../services/ai_stream_service.dart';

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

class ChatNotifier extends Notifier<List<ChatMessage>> {
  @override
  List<ChatMessage> build() => [];

  Future<void> sendMessage(String prompt) async {
    final service = ref.read(aiStreamServiceProvider);
    final provider = ref.read(selectedAiProviderProvider);

    // Add user message
    state = [
      ...state,
      ChatMessage(role: 'user', content: prompt),
    ];

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
  }

  void clearHistory() {
    state = [];
  }
}

final chatProvider = NotifierProvider<ChatNotifier, List<ChatMessage>>(ChatNotifier.new);
