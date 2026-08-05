import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';

enum AiProvider {
  gemini,
  claude,
  openai,
  deepseek,
  qwen,
}

class AiStreamService {
  final Dio _dio;
  final String _backendUrl;
  final String? _apiKey;

  AiStreamService({
    String backendUrl = 'http://localhost:8787',
    String? apiKey,
  })  : _backendUrl = backendUrl,
        _apiKey = apiKey,
        _dio = Dio(
          BaseOptions(
            connectTimeout: const Duration(seconds: 30),
            receiveTimeout: const Duration(minutes: 5),
            contentType: 'application/json',
          ),
        );

  /// Stream real chat response dari Cloudflare AI Gateway
  /// Supports SSE (Server-Sent Events) format
  Stream<String> streamChat({
    required String prompt,
    AiProvider provider = AiProvider.gemini,
    Map<String, dynamic>? systemContext,
    bool useMock = false,
  }) async* {
    if (useMock) {
      yield* streamChatMock(prompt);
      return;
    }

    try {
      final headers = {
        'Accept': 'text/event-stream',
        if (_apiKey != null) 'Authorization': 'Bearer $_apiKey',
      };

      final response = await _dio.post<ResponseBody>(
        '$_backendUrl/api/v1/chat/stream',
        data: {
          'prompt': prompt,
          'provider': provider.name,
          'system_context': systemContext,
        },
        options: Options(
          responseType: ResponseType.stream,
          headers: headers,
        ),
      );

      final stream = response.data?.stream;
      if (stream != null) {
        StringBuffer buffer = StringBuffer();
        await for (final chunk in stream) {
          final decoded = utf8.decode(chunk);
          buffer.write(decoded);

          // Parse SSE format: "data: {...}"
          final lines = buffer.toString().split('\n');
          for (int i = 0; i < lines.length - 1; i++) {
            final line = lines[i].trim();
            if (line.startsWith('data: ')) {
              final data = line.substring(6).trim();
              if (data.isNotEmpty && data != '[DONE]') {
                try {
                  final json = jsonDecode(data);
                  if (json['text'] != null) {
                    yield json['text'] as String;
                  }
                } catch (e) {
                  yield data;
                }
              }
            }
          }
          // Keep last incomplete line in buffer
          buffer.clear();
          if (lines.isNotEmpty) {
            buffer.write(lines.last);
          }
        }
      }
    } on DioException catch (e) {
      yield 'Error (${e.type}): ${e.message}';
    } catch (e) {
      yield 'Error: $e';
    }
  }

  /// Mock streaming untuk testing UI tanpa backend
  Stream<String> streamChatMock(String prompt) async* {
    final words = [
      'Analisis chart menunjukkan ',
      'pola bullish yang kuat ',
      'dengan support di level 64000 ',
      'dan resistance di 66000.\n\n',
      'Indikator:\n',
      '- RSI: 65 (overbought territory)\n',
      '- MACD: Bullish crossover\n',
      '- Moving Average: 50MA > 200MA\n\n',
      '**Rekomendasi:**\n\n',
      '| Level | Action | Risk/Reward |\n',
      '|-------|--------|-------------|\n',
      '| 64000 | BUY | 1:2.5 |\n',
      '| 66000 | SELL | Safe TP |\n',
      '| 63500 | STOPLOSS | Risk Limit |',
    ];

    for (var word in words) {
      await Future.delayed(const Duration(milliseconds: 100));
      yield word;
    }
  }

  /// Request analysis dengan image upload
  Future<Map<String, dynamic>> analyzeChart({
    required List<int> imageBytes,
    required String imageMimeType,
    AiProvider provider = AiProvider.gemini,
    List<String> methods = const ['RSI', 'MACD', 'MovingAverage'],
  }) async {
    try {
      final base64Image = base64Encode(imageBytes);

      final response = await _dio.post(
        '$_backendUrl/api/v1/analyze',
        data: {
          'imageBase64': base64Image,
          'mimeType': imageMimeType,
          'methods': methods,
          'provider': provider.name,
          if (_apiKey != null) 'apiKey': _apiKey,
        },
      );

      return response.data as Map<String, dynamic>;
    } on DioException catch (e) {
      throw Exception('Analysis failed: ${e.message}');
    }
  }
}
