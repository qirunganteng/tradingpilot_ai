import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// PRD 2.2.12 "Password Manager".
///
/// This is a real, working credential vault -- entries are encrypted at
/// rest via flutter_secure_storage (Keychain on macOS/iOS, Credential
/// Manager on Windows, libsecret on Linux, EncryptedSharedPreferences on
/// Android, IndexedDB+WebCrypto on web) -- but it's *manual* (add/view/
/// delete a saved login yourself) rather than auto-detecting login forms
/// inside the embedded WebView and offering to save on submit. Auto-detect
/// needs JS injection into every page to hook form submissions, which is a
/// meaningfully bigger and more security-sensitive follow-up; a manual
/// vault is the honest, safe subset to ship first.
class SavedCredential {
  final String id;
  final String site;
  final String username;
  final String password;
  final DateTime savedAt;

  const SavedCredential({
    required this.id,
    required this.site,
    required this.username,
    required this.password,
    required this.savedAt,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'site': site,
        'username': username,
        'password': password,
        'savedAt': savedAt.toIso8601String(),
      };

  factory SavedCredential.fromJson(Map<String, dynamic> json) => SavedCredential(
        id: json['id'] as String,
        site: json['site'] as String,
        username: json['username'] as String,
        password: json['password'] as String,
        savedAt: DateTime.parse(json['savedAt'] as String),
      );
}

class PasswordVault {
  static const _storage = FlutterSecureStorage();
  static const _key = 'tradepilot_saved_credentials';

  static Future<List<SavedCredential>> loadAll() async {
    try {
      final raw = await _storage.read(key: _key);
      if (raw == null || raw.isEmpty) return [];
      final list = jsonDecode(raw) as List<dynamic>;
      return list.map((e) => SavedCredential.fromJson(e as Map<String, dynamic>)).toList();
    } catch (_) {
      return [];
    }
  }

  static Future<void> _saveAll(List<SavedCredential> items) async {
    final raw = jsonEncode(items.map((e) => e.toJson()).toList());
    await _storage.write(key: _key, value: raw);
  }

  static Future<void> add(SavedCredential credential) async {
    final all = await loadAll();
    all.add(credential);
    await _saveAll(all);
  }

  static Future<void> remove(String id) async {
    final all = await loadAll();
    all.removeWhere((e) => e.id == id);
    await _saveAll(all);
  }

  static Future<void> clearAll() async {
    await _storage.delete(key: _key);
  }
}
