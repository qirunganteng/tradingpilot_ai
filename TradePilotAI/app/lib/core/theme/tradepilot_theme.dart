import 'package:flutter/material.dart';

/// TradePilot AI Color Scheme - Modern Dark Trading Theme
class TradePilotColors {
  // Primary Brand Colors
  static const Color primaryBlue = Color(0xFF2196F3);
  static const Color primaryAccent = Color(0xFF1E88E5);
  static const Color primaryDark = Color(0xFF1565C0);

  // Dark Background
  static const Color bgDarkest = Color(0xFF0A0E27);
  static const Color bgDarker = Color(0xFF121212);
  static const Color bgDark = Color(0xFF1E1E1E);
  static const Color bgMedium = Color(0xFF252526);
  static const Color bgLight = Color(0xFF2D2D30);

  // UI Elements
  static const Color surfaceOverlay = Color(0xFF1F1F1F);
  static const Color dividerColor = Color(0xFF3F3F46);
  static const Color borderColor = Color(0xFF444746);

  // Status Colors (Trading Specific)
  static const Color bullish = Color(0xFF26C485);
  static const Color bearish = Color(0xFFEF5350);
  static const Color neutral = Color(0xFF78909C);
  static const Color warning = Color(0xFFFFA726);
  static const Color success = Color(0xFF66BB6A);
  static const Color error = Color(0xFFEF5350);

  // Text Colors
  static const Color textPrimary = Color(0xFFFFFFFF);
  static const Color textSecondary = Color(0xFFB0B0B0);
  static const Color textTertiary = Color(0xFF808080);
  static const Color textDisabled = Color(0xFF505050);

  // Chart Colors
  static const Color chartGreen = Color(0xFF26C485);
  static const Color chartRed = Color(0xFFEF5350);
  static const Color gridColor = Color(0xFF2A2A3E);
}

/// Dark Theme Data for TradePilot
ThemeData createTradePilotDarkTheme() {
  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    colorScheme: ColorScheme.dark(
      primary: TradePilotColors.primaryBlue,
      primaryContainer: TradePilotColors.primaryAccent,
      secondary: TradePilotColors.warning,
      surface: TradePilotColors.bgDark,
      surfaceContainer: TradePilotColors.bgMedium,
      error: TradePilotColors.error,
    ),
    scaffoldBackgroundColor: TradePilotColors.bgDarkest,
    appBarTheme: AppBarTheme(
      backgroundColor: TradePilotColors.bgDark,
      elevation: 0,
      centerTitle: true,
      titleTextStyle: const TextStyle(
        color: TradePilotColors.textPrimary,
        fontSize: 18,
        fontWeight: FontWeight.w600,
      ),
    ),
    cardTheme: const CardThemeData(
      color: TradePilotColors.bgMedium,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(8)),
        side: BorderSide(
          color: TradePilotColors.borderColor,
          width: 0.5,
        ),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: TradePilotColors.bgMedium,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: TradePilotColors.borderColor),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: TradePilotColors.borderColor),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: TradePilotColors.primaryBlue, width: 2),
      ),
      hintStyle: const TextStyle(color: TradePilotColors.textTertiary),
      labelStyle: const TextStyle(color: TradePilotColors.textSecondary),
    ),
    textTheme: const TextTheme(
      displayLarge: TextStyle(
        color: TradePilotColors.textPrimary,
        fontSize: 32,
        fontWeight: FontWeight.bold,
      ),
      displayMedium: TextStyle(
        color: TradePilotColors.textPrimary,
        fontSize: 28,
        fontWeight: FontWeight.bold,
      ),
      titleLarge: TextStyle(
        color: TradePilotColors.textPrimary,
        fontSize: 20,
        fontWeight: FontWeight.w600,
      ),
      titleMedium: TextStyle(
        color: TradePilotColors.textPrimary,
        fontSize: 16,
        fontWeight: FontWeight.w500,
      ),
      bodyLarge: TextStyle(
        color: TradePilotColors.textPrimary,
        fontSize: 16,
      ),
      bodyMedium: TextStyle(
        color: TradePilotColors.textSecondary,
        fontSize: 14,
      ),
      bodySmall: TextStyle(
        color: TradePilotColors.textTertiary,
        fontSize: 12,
      ),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: TradePilotColors.primaryBlue,
        foregroundColor: TradePilotColors.textPrimary,
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: TradePilotColors.primaryBlue,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      ),
    ),
  );
}

/// Light Theme Data for TradePilot (Future use)
ThemeData createTradePilotLightTheme() {
  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.light,
    colorScheme: ColorScheme.light(
      primary: TradePilotColors.primaryBlue,
      primaryContainer: TradePilotColors.primaryAccent,
      secondary: TradePilotColors.warning,
      surface: Colors.white,
      error: TradePilotColors.error,
    ),
    scaffoldBackgroundColor: Colors.white,
    // Additional light theme customization
  );
}
