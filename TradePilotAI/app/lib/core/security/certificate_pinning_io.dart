import 'dart:convert';
import 'dart:io';
import 'package:crypto/crypto.dart';
import 'package:dio/dio.dart';
import 'package:dio/io.dart';
import 'certificate_pinning.dart';

/// Wires [CertificatePinningConfig] into the shared Dio client's
/// certificate validation (desktop/mobile only -- see
/// certificate_pinning_web.dart for the web no-op, since
/// `IOHttpClientAdapter`/`dart:io` don't exist on web).
///
/// A no-op today because [CertificatePinningConfig.isPinningConfigured] is
/// false until real pins are populated at deploy time -- see that file for
/// why they ship empty. Once configured, every leaf certificate presented
/// for a pinned host is SHA-256 hashed (of the full DER-encoded
/// certificate -- a simpler, slightly stricter variant of the SPKI-only
/// pinning described in RFC 7469 §2.5: it also survives across a
/// like-for-like cert renewal on the *same* key, and only needs
/// invalidating when the backend actually rotates its private key) and
/// compared against the configured pins; the connection is rejected if
/// none match.
void configureCertificatePinning(Dio dio) {
  if (!CertificatePinningConfig.isPinningConfigured) return;

  (dio.httpClientAdapter as IOHttpClientAdapter).createHttpClient = () {
    final client = HttpClient();
    client.badCertificateCallback = (X509Certificate cert, String host, int port) {
      final pins = CertificatePinningConfig.kPinnedPublicKeyHashes[host];
      if (pins == null) {
        // Not a pinned host -- this callback only fires for certs the
        // platform already rejected, so preserve default-deny for
        // anything we don't explicitly pin.
        return false;
      }
      final actual = base64.encode(sha256.convert(cert.der).bytes);
      return pins.contains(actual);
    };
    return client;
  };
}
