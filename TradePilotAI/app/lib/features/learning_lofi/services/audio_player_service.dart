import 'package:just_audio/just_audio.dart';
import 'package:audio_service/audio_service.dart';

enum AudioSourceType {
  url,
  file,
  playlist,
}

class AudioSourceInfo {
  final String id;
  final String title;
  final String artist;
  final String? imageUrl;
  final String source;
  final AudioSourceType type;

  AudioSourceInfo({
    required this.id,
    required this.title,
    required this.artist,
    this.imageUrl,
    required this.source,
    required this.type,
  });
}

/// Enhanced Audio Player Service with background playback support
class AudioPlayerService {
  final AudioPlayer _player = AudioPlayer();
  
  // Playlist management
  final List<AudioSourceInfo> _playlist = [];
  int _currentIndex = 0;

  AudioPlayer get player => _player;
  List<AudioSourceInfo> get playlist => _playlist;
  int get currentIndex => _currentIndex;

  /// Initialize audio player with lofi radio stream
  Future<void> init({String? initialStream}) async {
    const defaultStream = 'https://play.streamafrica.net/lofi';
    final streamUrl = initialStream ?? defaultStream;

    try {
      await _player.setAudioSource(
        AudioSource.uri(Uri.parse(streamUrl)),
        preload: true,
      );
    } catch (e) {
      print("Error loading audio source: $e");
    }
  }

  /// Load a single audio source
  Future<void> loadAudio(AudioSourceInfo source) async {
    try {
      if (source.type == AudioSourceType.url) {
        await _player.setAudioSource(
          AudioSource.uri(Uri.parse(source.source)),
          preload: true,
        );
      }
    } catch (e) {
      print("Error loading audio: $e");
      rethrow;
    }
  }

  /// Load multiple audio sources as playlist
  Future<void> loadPlaylist(List<AudioSourceInfo> sources) async {
    try {
      _playlist.clear();
      _playlist.addAll(sources);
      
      if (sources.isNotEmpty) {
        final audioSources = sources
            .map((s) => AudioSource.uri(Uri.parse(s.source)))
            .toList();
        
        await _player.setAudioSource(
          ConcatenatingAudioSource(children: audioSources),
          preload: true,
        );
      }
    } catch (e) {
      print("Error loading playlist: $e");
      rethrow;
    }
  }

  /// Play current audio
  Future<void> play() async {
    try {
      await _player.play();
    } catch (e) {
      print("Error playing audio: $e");
    }
  }

  /// Pause playback
  Future<void> pause() async {
    try {
      await _player.pause();
    } catch (e) {
      print("Error pausing audio: $e");
    }
  }

  /// Resume playback
  Future<void> resume() async {
    try {
      if (_player.playerState.playing) {
        await pause();
      } else {
        await play();
      }
    } catch (e) {
      print("Error toggling playback: $e");
    }
  }

  /// Stop playback
  Future<void> stop() async {
    try {
      await _player.stop();
    } catch (e) {
      print("Error stopping audio: $e");
    }
  }

  /// Set volume (0.0 - 1.0)
  Future<void> setVolume(double volume) async {
    try {
      await _player.setVolume(volume.clamp(0.0, 1.0));
    } catch (e) {
      print("Error setting volume: $e");
    }
  }

  /// Skip to next track
  Future<void> skipNext() async {
    try {
      if (_currentIndex < _playlist.length - 1) {
        _currentIndex++;
        await _player.seek(Duration.zero, index: _currentIndex);
      }
    } catch (e) {
      print("Error skipping next: $e");
    }
  }

  /// Skip to previous track
  Future<void> skipPrevious() async {
    try {
      if (_currentIndex > 0) {
        _currentIndex--;
        await _player.seek(Duration.zero, index: _currentIndex);
      }
    } catch (e) {
      print("Error skipping previous: $e");
    }
  }

  /// Seek to position
  Future<void> seek(Duration position) async {
    try {
      await _player.seek(position);
    } catch (e) {
      print("Error seeking: $e");
    }
  }

  /// Get current playback state
  PlayerState? getCurrentState() => _player.playerState;

  /// Get current position stream
  Stream<Duration> get positionStream => _player.positionStream;

  /// Get duration stream
  Stream<Duration?> get durationStream => _player.durationStream;

  /// Get player state stream
  Stream<PlayerState> get playerStateStream => _player.playerStateStream;

  /// Get playback speed stream
  Stream<double> get speedStream => _player.speedStream;

  /// Set playback speed
  Future<void> setSpeed(double speed) async {
    try {
      await _player.setSpeed(speed);
    } catch (e) {
      print("Error setting speed: $e");
    }
  }

  /// Dispose resources
  Future<void> dispose() async {
    try {
      await _player.dispose();
    } catch (e) {
      print("Error disposing player: $e");
    }
  }
}
