import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

class BrowserView extends StatefulWidget {
  const BrowserView({super.key});

  @override
  State<BrowserView> createState() => _BrowserViewState();
}

class _BrowserViewState extends State<BrowserView> {
  final GlobalKey webViewKey = GlobalKey();
  InAppWebViewController? webViewController;
  final TextEditingController _urlController = TextEditingController(text: 'https://www.tradingview.com/chart/');
  
  double progress = 0;
  bool canGoBack = false;
  bool canGoForward = false;

  void _loadUrl(String url) {
    if (webViewController != null) {
      var uri = Uri.tryParse(url);
      if (uri != null && uri.scheme.isEmpty) {
        uri = Uri.parse('https://\$url');
      }
      if (uri != null) {
        webViewController!.loadUrl(urlRequest: URLRequest(url: WebUri.uri(uri)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Address Bar
        Container(
          color: const Color(0xFF252526),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
          child: Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back),
                onPressed: canGoBack ? () => webViewController?.goBack() : null,
              ),
              IconButton(
                icon: const Icon(Icons.arrow_forward),
                onPressed: canGoForward ? () => webViewController?.goForward() : null,
              ),
              IconButton(
                icon: const Icon(Icons.refresh),
                onPressed: () => webViewController?.reload(),
              ),
              Expanded(
                child: Container(
                  height: 36,
                  margin: const EdgeInsets.symmetric(horizontal: 8),
                  decoration: BoxDecoration(
                    color: const Color(0xFF3C3C3C),
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: TextField(
                    controller: _urlController,
                    decoration: const InputDecoration(
                      border: InputBorder.none,
                      contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                      hintText: 'Search or enter website name',
                      isDense: true,
                    ),
                    onSubmitted: (value) => _loadUrl(value),
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.home),
                onPressed: () => _loadUrl('https://www.tradingview.com/chart/'),
              ),
            ],
          ),
        ),
        if (progress < 1.0)
          LinearProgressIndicator(value: progress, backgroundColor: Colors.transparent),
        
        // Web View
        Expanded(
          child: InAppWebView(
            key: webViewKey,
            initialUrlRequest: URLRequest(
              url: WebUri('https://www.tradingview.com/chart/'),
            ),
            initialSettings: InAppWebViewSettings(
              isInspectable: true,
              mediaPlaybackRequiresUserGesture: false,
              javaScriptEnabled: true,
              transparentBackground: true,
            ),
            onWebViewCreated: (controller) {
              webViewController = controller;
            },
            onLoadStart: (controller, url) {
              setState(() {
                _urlController.text = url.toString();
              });
            },
            onLoadStop: (controller, url) async {
              final back = await controller.canGoBack();
              final forward = await controller.canGoForward();
              setState(() {
                canGoBack = back;
                canGoForward = forward;
              });
            },
            onProgressChanged: (controller, p) {
              setState(() {
                progress = p / 100;
              });
            },
          ),
        ),
      ],
    );
  }
}
