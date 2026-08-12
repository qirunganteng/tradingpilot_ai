import 'package:dio/dio.dart';

/// Web builds run inside the browser's own network stack (fetch/XHR under
/// the hood), which never exposes raw certificate bytes to page/app code --
/// so there is nothing for Dio to pin here. The browser's own certificate
/// validation (and, on a phished/MITM'd network, its own warning UI) is the
/// applicable protection on this platform.
void configureCertificatePinning(Dio dio) {}
