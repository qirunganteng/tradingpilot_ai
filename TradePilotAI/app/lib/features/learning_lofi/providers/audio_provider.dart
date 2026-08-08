import 'package:hooks_riverpod/hooks_riverpod.dart';
import '../services/audio_player_service.dart';

final audioServiceProvider = Provider<AudioPlayerService>((ref) {
  final service = AudioPlayerService();
  service.init();
  ref.onDispose(() => service.dispose());
  return service;
});

final isPlayingProvider = StreamProvider<bool>((ref) {
  final service = ref.watch(audioServiceProvider);
  return service.playingStream;
});

/// media_kit doesn't expose the same ProcessingState enum just_audio had --
/// this maps its plain `buffering` bool stream onto a small stand-in enum
/// so callers (e.g. the Lofi mini-bar's spinner) don't need to change.
enum AudioProcessingState { idle, buffering, ready }

final processingStateProvider = StreamProvider<AudioProcessingState>((ref) {
  final service = ref.watch(audioServiceProvider);
  return service.bufferingStream.map(
    (buffering) => buffering ? AudioProcessingState.buffering : AudioProcessingState.ready,
  );
});

class VolumeNotifier extends Notifier<double> {
  @override
  double build() => 1.0;

  void setVolume(double val) {
    state = val;
  }
}

final volumeProvider = NotifierProvider<VolumeNotifier, double>(VolumeNotifier.new);
