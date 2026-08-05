import 'package:hooks_riverpod/hooks_riverpod.dart';

enum AppModule {
  trading,
  aiPilot,
  social,
  education
}

class NavigationNotifier extends Notifier<AppModule> {
  @override
  AppModule build() => AppModule.trading;

  void setModule(AppModule module) {
    state = module;
  }
}

final navigationProvider = NotifierProvider<NavigationNotifier, AppModule>(NavigationNotifier.new);
