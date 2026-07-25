# TradePilot AI - Proguard rules (Fase 9 akan melengkapi)
# Jangan obfuscate model yang di-parse Moshi (data-ai) agar JSON mapping tetap benar.
-keepclassmembers class com.tradepilot.domain.model.** { *; }
-keep class com.tradepilot.core.database.entity.** { *; }
