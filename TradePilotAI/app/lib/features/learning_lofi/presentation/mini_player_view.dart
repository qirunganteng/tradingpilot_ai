import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:just_audio/just_audio.dart';
import '../providers/audio_provider.dart';

class MiniPlayerView extends ConsumerWidget {
  const MiniPlayerView({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final isPlaying = ref.watch(isPlayingProvider).value ?? false;
    final processingState = ref.watch(processingStateProvider).value ?? ProcessingState.idle;
    final volume = ref.watch(volumeProvider);
    final audioService = ref.read(audioServiceProvider);

    final isBuffering = processingState == ProcessingState.loading || processingState == ProcessingState.buffering;

    return Container(
      height: 60,
      color: const Color(0xFF181818),
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: Colors.blueAccent.withOpacity(0.2),
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Icon(Icons.radio, color: Colors.blueAccent),
          ),
          const SizedBox(width: 12),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'Lofi Trading Radio',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                ),
                Text(
                  'Live Stream',
                  style: TextStyle(color: Colors.grey, fontSize: 12),
                ),
              ],
            ),
          ),
          if (isBuffering)
            const SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(strokeWidth: 2),
            )
          else
            IconButton(
              icon: Icon(isPlaying ? Icons.pause_circle_filled : Icons.play_circle_fill),
              iconSize: 36,
              color: Colors.white,
              onPressed: () {
                if (isPlaying) {
                  audioService.pause();
                } else {
                  audioService.play();
                }
              },
            ),
          const SizedBox(width: 16),
          const Icon(Icons.volume_up, size: 20, color: Colors.grey),
          SizedBox(
            width: 100,
            child: Slider(
              value: volume,
              min: 0.0,
              max: 1.0,
              activeColor: Colors.blueAccent,
              onChanged: (val) {
                ref.read(volumeProvider.notifier).setVolume(val);
                audioService.setVolume(val);
              },
            ),
          )
        ],
      ),
    );
  }
}
