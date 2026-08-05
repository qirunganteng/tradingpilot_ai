import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';

class ChatSession {
  final String id;
  final String title;
  final List<ChatMessage> messages;
  final DateTime createdAt;
  final DateTime updatedAt;
  final String aiProvider;

  ChatSession({
    required this.id,
    required this.title,
    required this.messages,
    required this.createdAt,
    required this.updatedAt,
    required this.aiProvider,
  });

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'messages': messages.map((m) => m.toJson()).toList(),
    'createdAt': createdAt.toIso8601String(),
    'updatedAt': updatedAt.toIso8601String(),
    'aiProvider': aiProvider,
  };

  factory ChatSession.fromJson(Map<String, dynamic> json) => ChatSession(
    id: json['id'] as String,
    title: json['title'] as String,
    messages: (json['messages'] as List<dynamic>)
        .map((m) => ChatMessage.fromJson(m as Map<String, dynamic>))
        .toList(),
    createdAt: DateTime.parse(json['createdAt'] as String),
    updatedAt: DateTime.parse(json['updatedAt'] as String),
    aiProvider: json['aiProvider'] as String? ?? 'gemini',
  );
}

class ChatMessage {
  final String id;
  final String role;
  final String content;
  final DateTime timestamp;
  final Map<String, dynamic>? metadata;

  ChatMessage({
    required this.id,
    required this.role,
    required this.content,
    required this.timestamp,
    this.metadata,
  });

  Map<String, dynamic> toJson() => {
    'id': id,
    'role': role,
    'content': content,
    'timestamp': timestamp.toIso8601String(),
    'metadata': metadata,
  };

  factory ChatMessage.fromJson(Map<String, dynamic> json) => ChatMessage(
    id: json['id'] as String,
    role: json['role'] as String,
    content: json['content'] as String,
    timestamp: DateTime.parse(json['timestamp'] as String),
    metadata: json['metadata'] as Map<String, dynamic>?,
  );
}

/// Service to manage chat session persistence
class ChatSessionService {
  static const String _sessionPrefix = 'chat_session_';
  static const String _sessionListKey = 'chat_sessions_list';

  final SharedPreferences _prefs;

  ChatSessionService(this._prefs);

  /// Save a chat session
  Future<void> saveSession(ChatSession session) async {
    try {
      final json = jsonEncode(session.toJson());
      await _prefs.setString('$_sessionPrefix${session.id}', json);
      
      // Update session list
      final sessionList = _prefs.getStringList(_sessionListKey) ?? [];
      if (!sessionList.contains(session.id)) {
        sessionList.add(session.id);
        await _prefs.setStringList(_sessionListKey, sessionList);
      }
    } catch (e) {
      print('Error saving session: $e');
      rethrow;
    }
  }

  /// Load a specific chat session
  Future<ChatSession?> loadSession(String sessionId) async {
    try {
      final json = _prefs.getString('$_sessionPrefix$sessionId');
      if (json == null) return null;
      
      return ChatSession.fromJson(jsonDecode(json) as Map<String, dynamic>);
    } catch (e) {
      print('Error loading session: $e');
      return null;
    }
  }

  /// Get all chat sessions
  Future<List<ChatSession>> getAllSessions() async {
    try {
      final sessionList = _prefs.getStringList(_sessionListKey) ?? [];
      final sessions = <ChatSession>[];

      for (final sessionId in sessionList) {
        final session = await loadSession(sessionId);
        if (session != null) {
          sessions.add(session);
        }
      }

      // Sort by updated date (most recent first)
      sessions.sort((a, b) => b.updatedAt.compareTo(a.updatedAt));
      return sessions;
    } catch (e) {
      print('Error getting all sessions: $e');
      return [];
    }
  }

  /// Delete a chat session
  Future<void> deleteSession(String sessionId) async {
    try {
      await _prefs.remove('$_sessionPrefix$sessionId');
      
      final sessionList = _prefs.getStringList(_sessionListKey) ?? [];
      sessionList.removeWhere((id) => id == sessionId);
      await _prefs.setStringList(_sessionListKey, sessionList);
    } catch (e) {
      print('Error deleting session: $e');
      rethrow;
    }
  }

  /// Clear all sessions
  Future<void> clearAllSessions() async {
    try {
      final sessionList = _prefs.getStringList(_sessionListKey) ?? [];
      for (final sessionId in sessionList) {
        await _prefs.remove('$_sessionPrefix$sessionId');
      }
      await _prefs.remove(_sessionListKey);
    } catch (e) {
      print('Error clearing sessions: $e');
      rethrow;
    }
  }

  /// Export session as JSON
  String exportSession(ChatSession session) {
    return jsonEncode(session.toJson());
  }

  /// Import session from JSON
  Future<ChatSession> importSession(String jsonString) async {
    try {
      final data = jsonDecode(jsonString) as Map<String, dynamic>;
      return ChatSession.fromJson(data);
    } catch (e) {
      print('Error importing session: $e');
      rethrow;
    }
  }
}
