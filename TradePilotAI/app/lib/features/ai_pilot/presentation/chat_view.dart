import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import '../providers/chat_provider.dart';
import '../services/ai_stream_service.dart';
import '../services/chat_session_service.dart' as persisted;

class ChatView extends ConsumerStatefulWidget {
  final VoidCallback? onClose;
  const ChatView({super.key, this.onClose});

  @override
  ConsumerState<ChatView> createState() => _ChatViewState();
}

class _ChatViewState extends ConsumerState<ChatView> {
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  void _sendMessage() {
    if (_controller.text.trim().isEmpty) return;
    
    ref.read(chatProvider.notifier).sendMessage(_controller.text.trim());
    _controller.clear();
    
    Future.delayed(const Duration(milliseconds: 100), () {
      _scrollToBottom();
    });
  }

  void _scrollToBottom() {
    if (_scrollController.hasClients) {
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeOut,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final messages = ref.watch(chatProvider);
    final selectedProvider = ref.watch(selectedAiProviderProvider);
    
    // Auto scroll when messages update
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scrollToBottom();
    });

    return Container(
      color: const Color(0xFF121212),
      child: Column(
        children: [
          // Header with provider selector
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            color: const Color(0xFF1E1E1E),
            child: Row(
              children: [
                const Expanded(
                  child: Text(
                    'AI Pilot',
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
                  ),
                ),
                // AI Provider selector — a compact popup menu (not a
                // DropdownButton) since Flutter's DropdownButton forces each
                // item to be at least 48px tall for accessibility, which is
                // what made this list look so oversized before.
                PopupMenuButton<AiProvider>(
                  tooltip: 'Choose AI provider',
                  initialValue: selectedProvider,
                  color: const Color(0xFF2D2D30),
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(minWidth: 130, maxWidth: 150),
                  onSelected: (value) => ref.read(selectedAiProviderProvider.notifier).setProvider(value),
                  itemBuilder: (context) => [
                    for (final provider in AiProvider.values)
                      PopupMenuItem(
                        value: provider,
                        height: 30,
                        padding: const EdgeInsets.symmetric(horizontal: 12),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            if (provider == selectedProvider)
                              const Icon(Icons.check, size: 13, color: Colors.blueAccent)
                            else
                              const SizedBox(width: 13),
                            const SizedBox(width: 6),
                            Text(provider.name.toUpperCase(), style: const TextStyle(fontSize: 11.5)),
                          ],
                        ),
                      ),
                  ],
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.grey[900],
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          selectedProvider.name.toUpperCase(),
                          style: const TextStyle(fontSize: 10.5, color: Colors.white),
                        ),
                        const SizedBox(width: 3),
                        const Icon(Icons.arrow_drop_down, size: 15, color: Colors.grey),
                      ],
                    ),
                  ),
                ),
                // New chat -- starts a fresh conversation, the previous
                // one stays saved and reachable via the history button.
                IconButton(
                  icon: const Icon(Icons.add_comment_outlined, size: 15),
                  tooltip: 'New chat',
                  onPressed: () {
                    ref.read(chatProvider.notifier).startNewChat();
                  },
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(minWidth: 26, minHeight: 26),
                ),
                // Chat history — PRD / Chatbox-reference "conversation
                // management": browse, resume, or delete past chats.
                IconButton(
                  icon: const Icon(Icons.history, size: 15),
                  tooltip: 'Chat history',
                  onPressed: () => _showHistoryDialog(context, ref),
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(minWidth: 26, minHeight: 26),
                ),
                // Clear history button
                IconButton(
                  icon: const Icon(Icons.delete_outline, size: 15),
                  tooltip: 'Clear history',
                  onPressed: () {
                    ref.read(chatProvider.notifier).clearHistory();
                  },
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(minWidth: 26, minHeight: 26),
                ),
                if (widget.onClose != null)
                  Tooltip(
                    message: 'Close panel',
                    child: InkWell(
                      onTap: widget.onClose,
                      borderRadius: BorderRadius.circular(4),
                      child: const Padding(
                        padding: EdgeInsets.all(5),
                        child: Icon(Icons.close, size: 14, color: Colors.grey),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          Expanded(
            child: messages.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.smart_toy_outlined,
                          size: 64,
                          color: Colors.grey[600],
                        ),
                        const SizedBox(height: 16),
                        Text(
                          'Start chatting with AI',
                          style: TextStyle(
                            color: Colors.grey[400],
                            fontSize: 14,
                          ),
                        ),
                      ],
                    ),
                  )
                : ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.all(10),
                    itemCount: messages.length,
                    itemBuilder: (context, index) {
                      final msg = messages[index];
                      final isUser = msg.role == 'user';
                      return Align(
                        alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
                        child: Container(
                          margin: const EdgeInsets.only(bottom: 8),
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
                          decoration: BoxDecoration(
                            color: isUser ? Colors.blue[800] : Colors.grey[800],
                            borderRadius: BorderRadius.circular(10),
                          ),
                          constraints: BoxConstraints(
                            maxWidth: MediaQuery.of(context).size.width * 0.8,
                          ),
                          child: MarkdownBody(
                            data: msg.content,
                            styleSheet: MarkdownStyleSheet(
                              p: const TextStyle(color: Colors.white, fontSize: 12.5, height: 1.35),
                              code: const TextStyle(
                                backgroundColor: Colors.black45,
                                color: Colors.greenAccent,
                                fontSize: 11.5,
                              ),
                              codeblockDecoration: BoxDecoration(
                                color: Colors.black87,
                                borderRadius: BorderRadius.circular(6),
                              ),
                              codeblockPadding: const EdgeInsets.all(8),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(10, 6, 10, 10),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    style: const TextStyle(fontSize: 12.5),
                    onSubmitted: (_) => _sendMessage(),
                    decoration: InputDecoration(
                      hintText: 'Ask AI about market analysis...',
                      hintStyle: const TextStyle(fontSize: 12),
                      isDense: true,
                      filled: true,
                      fillColor: Colors.grey[900],
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(20),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                    ),
                  ),
                ),
                const SizedBox(width: 6),
                SizedBox(
                  width: 34,
                  height: 34,
                  child: IconButton(
                    style: IconButton.styleFrom(
                      backgroundColor: Colors.blueAccent,
                      shape: const CircleBorder(),
                    ),
                    icon: const Icon(Icons.send, color: Colors.white, size: 16),
                    padding: EdgeInsets.zero,
                    onPressed: _sendMessage,
                  ),
                ),
              ],
            ),
          )
        ],
      ),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _showHistoryDialog(BuildContext context, WidgetRef ref) {
    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => FutureBuilder<List<persisted.ChatSession>>(
          future: ref.read(chatProvider.notifier).listSessions(),
          builder: (context, snapshot) {
            final sessions = snapshot.data ?? const <persisted.ChatSession>[];
            return AlertDialog(
              backgroundColor: const Color(0xFF2D2D30),
              title: const Text('Chat history', style: TextStyle(fontSize: 15)),
              content: SizedBox(
                width: 340,
                height: 360,
                child: !snapshot.hasData
                    ? const Center(child: CircularProgressIndicator())
                    : sessions.isEmpty
                        ? Center(
                            child: Text('No past chats yet.', style: TextStyle(color: Colors.grey[500], fontSize: 13)),
                          )
                        : ListView.builder(
                            itemCount: sessions.length,
                            itemBuilder: (context, index) {
                              final s = sessions[index];
                              final t = s.updatedAt;
                              final dateLabel =
                                  '${t.day.toString().padLeft(2, '0')}/${t.month.toString().padLeft(2, '0')} '
                                  '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';
                              return ListTile(
                                dense: true,
                                leading: const Icon(Icons.chat_bubble_outline, size: 16, color: Colors.grey),
                                title: Text(
                                  s.title,
                                  style: const TextStyle(fontSize: 12.5),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                subtitle: Text(
                                  '$dateLabel · ${s.messages.length} messages · ${s.aiProvider.toUpperCase()}',
                                  style: TextStyle(fontSize: 10.5, color: Colors.grey[500]),
                                ),
                                trailing: IconButton(
                                  icon: const Icon(Icons.delete_outline, size: 16),
                                  tooltip: 'Delete',
                                  onPressed: () async {
                                    await ref.read(chatProvider.notifier).deleteSession(s.id);
                                    setDialogState(() {});
                                  },
                                ),
                                onTap: () async {
                                  await ref.read(chatProvider.notifier).loadSession(s.id);
                                  if (context.mounted) Navigator.pop(context);
                                },
                              );
                            },
                          ),
              ),
              actions: [
                TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Close')),
              ],
            );
          },
        ),
      ),
    );
  }
}
