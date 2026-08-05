import 'package:just_audio/just_audio.dart';

class AudioPlayerService {
  final AudioPlayer _player = AudioPlayer();

  AudioPlayer get player => _player;

  Future<void> init() async {
    // URL publik untuk stream radio lo-fi (sebagai contoh)
    // Dalam production, ambil dari konfigurasi atau backend.
    const streamUrl = 'https://play.streamafrica.net/lofi';
    
    try {
      await _player.setAudioSource(AudioSource.uri(Uri.parse(streamUrl)));
    } catch (e) {
      print("Error loading audio source: \$e");
    }
  }

  Future<void> play() async => await _player.play();
  Future<void> pause() async => await _player.pause();
  Future<void> stop() async => await _player.stop();
  Future<void> setVolume(double volume) async => await _player.setVolume(volume);

  void dispose() {
    _player.dispose();
  }
}
