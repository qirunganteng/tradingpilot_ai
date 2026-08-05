import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum ThemeMode {
  dark,
  light,
  system,
}

class ThemeNotifier extends Notifier<ThemeMode> {
  static const String _themeKey = 'theme_mode';

  @override
  ThemeMode build() {
    // Load saved theme from SharedPreferences
    _loadTheme();
    return ThemeMode.dark; // Default
  }

  Future<void> _loadTheme() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final savedTheme = prefs.getString(_themeKey);
      if (savedTheme != null) {
        state = ThemeMode.values.firstWhere(
          (e) => e.toString() == 'ThemeMode.$savedTheme',
          orElse: () => ThemeMode.dark,
        );
      }
    } catch (e) {
      print('Error loading theme preference: $e');
    }
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    state = mode;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString(_themeKey, mode.name);
    } catch (e) {
      print('Error saving theme preference: $e');
    }
  }

  void toggleTheme() {
    final newMode = state == ThemeMode.dark ? ThemeMode.light : ThemeMode.dark;
    setThemeMode(newMode);
  }
}

final themeProvider = NotifierProvider<ThemeNotifier, ThemeMode>(ThemeNotifier.new);
