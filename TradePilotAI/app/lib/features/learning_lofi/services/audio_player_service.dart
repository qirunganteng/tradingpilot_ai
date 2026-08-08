import 'package:media_kit/media_kit.dart';

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

/// Audio playback backed by media_kit (libmpv) instead of just_audio.
///
/// This mirrors the reference pattern from Spotube (spotube-master), which
/// specifically chose media_kit over just_audio for reliable Windows
/// Desktop playback -- just_audio's desktop support is comparatively
/// thin/less battle-tested. See CONSTITUTION.md's cross-platform-first
/// package selection rule ("Selalu prioritaskan package yang memiliki
/// dukungan Cross-Platform (Desktop + Mobile) yang stabil").
class AudioPlayerService {
  final Player _player = Player();

  final List<AudioSourceInfo> _playlist = [];
  int _currentIndex = 0;

  Player get player => _player;
  List<AudioSourceInfo> get playlist => _playlist;
  int get currentIndex => _currentIndex;

  /// Initialize audio player with lofi radio stream
  Future<void> init({String? initialStream}) async {
    const defaultStream = 'https://play.streamafrica.net/lofi';
    final streamUrl = initialStream ?? defaultStream;

    try {
      await _player.open(Media(streamUrl), play: false);
    } catch (e) {
      _logError('Error loading audio source', e);
    }
  }

  /// Load a single audio source
  Future<void> loadAudio(AudioSourceInfo source) async {
    try {
      if (source.type == AudioSourceType.url) {
        await _player.open(Media(source.source));
      }
    } catch (e) {
      _logError('Error loading audio', e);
      rethrow;
    }
  }

  /// Load multiple audio sources as a playlist
  Future<void> loadPlaylist(List<AudioSourceInfo> sources) async {
    try {
      _playlist
        ..clear()
        ..addAll(sources);
      _currentIndex = 0;

      if (sources.isNotEmpty) {
        final playlist = Playlist(sources.map((s) => Media(s.source)).toList());
        await _player.open(playlist);
      }
    } catch (e) {
      _logError('Error loading playlist', e);
      rethrow;
    }
  }

  Future<void> play() async {
    try {
      await _player.play();
    } catch (e) {
      _logError('Error playing audio', e);
    }
  }

  Future<void> pause() async {
    try {
      await _player.pause();
    } catch (e) {
      _logError('Error pausing audio', e);
    }
  }

  Future<void> resume() async {
    try {
      if (_player.state.playing) {
        await pause();
      } else {
        await play();
      }
    } catch (e) {
      _logError('Error toggling playback', e);
    }
  }

  Future<void> stop() async {
    try {
      await _player.stop();
    } catch (e) {
      _logError('Error stopping audio', e);
    }
  }

  /// Set volume. Accepts the same 0.0-1.0 range the rest of the app uses
  /// and rescales to media_kit's native 0-100 range internally.
  Future<void> setVolume(double volume) async {
    try {
      await _player.setVolume(volume.clamp(0.0, 1.0) * 100);
    } catch (e) {
      _logError('Error setting volume', e);
    }
  }

  Future<void> skipNext() async {
    try {
      if (_currentIndex < _playlist.length - 1) {
        _currentIndex++;
        await _player.next();
      }
    } catch (e) {
      _logError('Error skipping next', e);
    }
  }

  Future<void> skipPrevious() async {
    try {
      if (_currentIndex > 0) {
        _currentIndex--;
        await _player.previous();
      }
    } catch (e) {
      _logError('Error skipping previous', e);
    }
  }

  Future<void> seek(Duration position) async {
    try {
      await _player.seek(position);
    } catch (e) {
      _logError('Error seeking', e);
    }
  }

  PlayerState getCurrentState() => _player.state;

  Stream<Duration> get positionStream => _player.stream.position;
  Stream<Duration> get durationStream => _player.stream.duration;
  Stream<bool> get playingStream => _player.stream.playing;
  Stream<bool> get bufferingStream => _player.stream.buffering;

  Future<void> setSpeed(double speed) async {
    try {
      await _player.setRate(speed);
    } catch (e) {
      _logError('Error setting speed', e);
    }
  }

  Future<void> dispose() async {
    try {
      await _player.dispose();
    } catch (e) {
      _logError('Error disposing player', e);
    }
  }

  void _logError(String context, Object e) {
    assert(() {
      // ignore: avoid_print
      print('[AudioPlayerService] $context: $e');
      return true;
    }());
  }
}
