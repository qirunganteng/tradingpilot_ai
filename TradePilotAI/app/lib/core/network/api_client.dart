import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'app_config.dart';
import '../security/certificate_pinning_export.dart';

/// Single, shared Dio instance for the whole app.
final dioClientProvider = Provider<Dio>((ref) {
  final config = ref.watch(appConfigProvider);

  final dio = Dio(
    BaseOptions(
      baseUrl: config.gatewayUrl,
      connectTimeout: const Duration(seconds: 30),
      receiveTimeout: const Duration(minutes: 5),
      contentType: 'application/json',
    ),
  );

  if (kDebugMode) {
    dio.interceptors.add(
      LogInterceptor(
        requestBody: false,
        responseBody: false,
        logPrint: (obj) => debugPrint('[Dio] $obj'),
      ),
    );
  }

  // PRD 3.2.4 "Certificate Pinning" -- no-op until real pins are
  // configured (see core/security/certificate_pinning.dart).
  configureCertificatePinning(dio);

  return dio;
});
