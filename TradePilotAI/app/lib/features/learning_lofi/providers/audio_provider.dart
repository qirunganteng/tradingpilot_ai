import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import '../services/audio_player_service.dart';

final audioServiceProvider = Provider<AudioPlayerService>((ref) {
  final service = AudioPlayerService();
  service.init();
  ref.onDispose(() => service.dispose());
  return service;
});

final isPlayingProvider = StreamProvider<bool>((ref) {
  final service = ref.watch(audioServiceProvider);
  return service.player.playingStream;
});

final processingStateProvider = StreamProvider<ProcessingState>((ref) {
  final service = ref.watch(audioServiceProvider);
  return service.player.processingStateStream;
});

class VolumeNotifier extends Notifier<double> {
  @override
  double build() => 1.0;

  void setVolume(double val) {
    state = val;
  }
}

final volumeProvider = NotifierProvider<VolumeNotifier, double>(VolumeNotifier.new);
