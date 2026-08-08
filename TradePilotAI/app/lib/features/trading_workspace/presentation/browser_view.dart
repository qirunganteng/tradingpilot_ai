import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import '../../../core/navigation/activity_bar.dart';

/// Default homepage used by the Home button and the very first tab.
const String kBrowserHomeUrl = 'https://www.tradingview.com/chart/';

class _QuickLink {
  final String label;
  final String url;
  final IconData icon;
  const _QuickLink(this.label, this.url, this.icon);
}

const List<_QuickLink> _kQuickLinks = [
  _QuickLink('TradingView', 'https://www.tradingview.com/chart/', Icons.show_chart),
  _QuickLink('Binance', 'https://www.binance.com/en/trade/BTC_USDT', Icons.currency_bitcoin),
  _QuickLink('CoinMarketCap', 'https://coinmarketcap.com', Icons.bar_chart),
  _QuickLink('Investing.com', 'https://www.investing.com', Icons.trending_up),
  _QuickLink('Yahoo Finance', 'https://finance.yahoo.com', Icons.attach_money),
  _QuickLink('MyFXBook', 'https://www.myfxbook.com', Icons.insights),
];

/// Mutable state for a single browser tab. Kept as a plain object (not a
/// widget) so multiple tabs can live side-by-side in an [IndexedStack] and
/// keep their WebView session alive while in the background, just like a
/// real desktop browser.
class _BrowserTab {
  final String id;
  final GlobalKey webViewKey = GlobalKey();
  InAppWebViewController? controller;

  String url;
  String title;
  bool started; // false = still showing the "New Tab" page
  bool isLoading = false;
  double progress = 0;
  bool canGoBack = false;
  bool canGoForward = false;
  bool isSecure = false;
  bool isBookmarked = false;
  int zoomPercent = 100;

  _BrowserTab({
    required this.id,
    this.url = '',
    this.title = 'New Tab',
    this.started = false,
  });
}

class _HistoryEntry {
  final String url;
  final String title;
  final DateTime visitedAt;
  const _HistoryEntry({required this.url, required this.title, required this.visitedAt});
}

class _DownloadEntry {
  final String url;
  final String filename;
  final DateTime startedAt;
  const _DownloadEntry({required this.url, required this.filename, required this.startedAt});
}

class BrowserView extends StatefulWidget {
  const BrowserView({super.key});

  @override
  State<BrowserView> createState() => _BrowserViewState();
}

class _BrowserViewState extends State<BrowserView> {
  final List<_BrowserTab> _tabs = [];
  int _activeIndex = 0;
  int _tabCounter = 0;
  bool _showBookmarksBar = true;
  late List<_QuickLink> _bookmarks = List.of(_kQuickLinks);
  final List<_HistoryEntry> _history = [];
  final List<_DownloadEntry> _downloads = [];

  final TextEditingController _addressController = TextEditingController();
  final FocusNode _addressFocusNode = FocusNode();
  final FocusNode _shortcutsFocusNode = FocusNode();

  _BrowserTab get _activeTab => _tabs[_activeIndex];

  @override
  void initState() {
    super.initState();
    // Default tab is a blank "New Tab" page, exactly like a fresh Chrome
    // window — TradingView is one click away via the bookmarks bar instead
    // of being force-loaded on startup.
    _openNewTab();
  }

  @override
  void dispose() {
    _addressController.dispose();
    _addressFocusNode.dispose();
    _shortcutsFocusNode.dispose();
    super.dispose();
  }

  String _newId() => 'tab_${_tabCounter++}';

  // ---------------------------------------------------------------------
  // Tab management
  // ---------------------------------------------------------------------

  void _openNewTab({String url = '', bool started = false, String? title}) {
    final tab = _BrowserTab(
      id: _newId(),
      url: url,
      started: started,
      title: title ?? (started ? 'Loading…' : 'New Tab'),
    );
    setState(() {
      _tabs.add(tab);
      _activeIndex = _tabs.length - 1;
      _addressController.text = url;
    });
  }

  void _closeTab(int index) {
    if (_tabs.length == 1) {
      // Never close the very last tab — reset it to a blank new tab instead,
      // matching how most browsers behave on the final tab.
      setState(() {
        _tabs[index] = _BrowserTab(id: _newId());
        if (_activeIndex == index) _addressController.text = '';
      });
      return;
    }
    setState(() {
      _tabs.removeAt(index);
      if (_activeIndex >= _tabs.length) {
        _activeIndex = _tabs.length - 1;
      } else if (_activeIndex > index) {
        _activeIndex -= 1;
      }
      _addressController.text = _activeTab.url;
    });
  }

  void _activateTab(int index) {
    setState(() {
      _activeIndex = index;
      _addressController.text = _tabs[index].url;
    });
  }

  // ---------------------------------------------------------------------
  // Navigation
  // ---------------------------------------------------------------------

  void _navigate(_BrowserTab tab, String input) {
    var url = input.trim();
    if (url.isEmpty) return;

    final looksLikeUrl = RegExp(r'^[a-zA-Z][a-zA-Z0-9+.-]*://').hasMatch(url) ||
        (url.contains('.') && !url.contains(' '));

    if (!looksLikeUrl) {
      url = 'https://www.google.com/search?q=${Uri.encodeComponent(url)}';
    } else if (!RegExp(r'^[a-zA-Z][a-zA-Z0-9+.-]*://').hasMatch(url)) {
      url = 'https://$url';
    }

    setState(() {
      tab.started = true;
      tab.url = url;
      tab.isLoading = true;
      tab.progress = 0;
    });

    if (tab.controller != null) {
      tab.controller!.loadUrl(urlRequest: URLRequest(url: WebUri(url)));
    }
    // If the controller doesn't exist yet, setting started=true above causes
    // the InAppWebView to be mounted with this url as its initial request.

    if (identical(tab, _activeTab)) {
      _addressController.text = url;
    }
    _addressFocusNode.unfocus();
  }

  void _goHome(_BrowserTab tab) => _navigate(tab, kBrowserHomeUrl);

  void _reload(_BrowserTab tab) {
    if (tab.isLoading) {
      tab.controller?.stopLoading();
    } else {
      tab.controller?.reload();
    }
  }

  void _applyZoom(_BrowserTab tab, int delta) {
    final newZoom = (tab.zoomPercent + delta).clamp(50, 200);
    setState(() => tab.zoomPercent = newZoom);
    tab.controller?.evaluateJavascript(
      source: "document.body.style.zoom='$newZoom%';",
    );
  }

  Future<void> _findInPage(_BrowserTab tab) async {
    final controller = TextEditingController();
    final query = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: const Text('Find in page', style: TextStyle(fontSize: 15)),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(hintText: 'Search text on this page…'),
          onSubmitted: (v) => Navigator.pop(ctx, v),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, controller.text), child: const Text('Find')),
        ],
      ),
    );
    if (query != null && query.trim().isNotEmpty) {
      try {
        await tab.controller?.findAllAsync(find: query.trim());
      } catch (_) {
        // findAllAsync isn't implemented on every platform (e.g. web) yet.
      }
    }
  }

  Future<void> _cutAddressBar() async {
    await Clipboard.setData(ClipboardData(text: _addressController.text));
    _addressController.clear();
  }

  Future<void> _copyAddressBar() async {
    await Clipboard.setData(ClipboardData(text: _addressController.text));
  }

  Future<void> _pasteAddressBar(_BrowserTab tab) async {
    final data = await Clipboard.getData(Clipboard.kTextPlain);
    if (data?.text != null) {
      setState(() => _addressController.text = data!.text!);
      _addressFocusNode.requestFocus();
    }
  }

  /// Opens the current page through Google Translate's page-proxy in a new
  /// tab — the same trick Chrome's own "Translate this page" ultimately
  /// relies on, without needing a translation API key of our own.
  void _translatePage(_BrowserTab tab) {
    if (!tab.started || tab.url.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Open a page first, then translate it.')),
      );
      return;
    }
    final targetLang = Localizations.localeOf(context).languageCode.isNotEmpty
        ? Localizations.localeOf(context).languageCode
        : 'id';
    final translated =
        'https://translate.google.com/translate?sl=auto&tl=$targetLang&u=${Uri.encodeComponent(tab.url)}';
    _openNewTab(url: translated, started: true, title: 'Translated · ${tab.title}');
  }

  // ---------------------------------------------------------------------
  // UI
  // ---------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    final tab = _activeTab;

    return Focus(
      focusNode: _shortcutsFocusNode,
      autofocus: true,
      onKeyEvent: (node, event) {
        if (event is! KeyDownEvent) return KeyEventResult.ignored;
        final ctrl = HardwareKeyboard.instance.isControlPressed ||
            HardwareKeyboard.instance.isMetaPressed;
        if (!ctrl) return KeyEventResult.ignored;

        if (event.logicalKey == LogicalKeyboardKey.keyT) {
          _openNewTab();
          return KeyEventResult.handled;
        }
        if (event.logicalKey == LogicalKeyboardKey.keyW) {
          _closeTab(_activeIndex);
          return KeyEventResult.handled;
        }
        if (event.logicalKey == LogicalKeyboardKey.keyL) {
          _addressFocusNode.requestFocus();
          return KeyEventResult.handled;
        }
        if (event.logicalKey == LogicalKeyboardKey.keyR) {
          _reload(_activeTab);
          return KeyEventResult.handled;
        }
        return KeyEventResult.ignored;
      },
      child: Consumer(
        builder: (context, ref, _) {
          final isFullscreen = ref.watch(browserMaximizedProvider);
          return Column(
            children: [
              if (!isFullscreen) _buildTabStrip(),
              if (!isFullscreen) _buildToolbar(tab),
              if (!isFullscreen && _showBookmarksBar) _buildBookmarksBar(tab),
              if (tab.isLoading)
                LinearProgressIndicator(
                  value: tab.progress <= 0 ? null : tab.progress,
                  minHeight: 2,
                  backgroundColor: Colors.transparent,
                  color: Colors.blueAccent,
                )
              else
                const SizedBox(height: 2),
              Expanded(
                child: Stack(
                  children: [
                    IndexedStack(
                      index: _activeIndex,
                      children: [for (final t in _tabs) _buildTabBody(t)],
                    ),
                    // A real browser still lets you get back out of
                    // fullscreen without hunting for a keyboard -- a
                    // small pill in the corner, same idea as Chrome's
                    // "Press Esc to exit full screen" toast.
                    if (isFullscreen)
                      Positioned(
                        top: 8,
                        right: 8,
                        child: Material(
                          color: Colors.black.withValues(alpha: 0.55),
                          borderRadius: BorderRadius.circular(16),
                          child: InkWell(
                            borderRadius: BorderRadius.circular(16),
                            onTap: () => ref.read(browserMaximizedProvider.notifier).toggle(),
                            child: const Padding(
                              padding: EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.fullscreen_exit, size: 14, color: Colors.white70),
                                  SizedBox(width: 4),
                                  Text('Exit fullscreen (Esc)', style: TextStyle(fontSize: 11, color: Colors.white70)),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildTabStrip() {
    // Chrome-style layout: tabs get a comfortable width when there's room,
    // shrinking down (never below a usable minimum) as more are added, and
    // scrolling internally once even the minimum doesn't fit. Each tab's
    // *rendered* width always matches the *reserved* width exactly, so a
    // tab's close (X) button can never end up clipped outside its
    // allotted space -- which is what was cutting off the close button on
    // every tab except the first one before.
    const double maxTabWidth = 200;
    const double minTabWidth = 96;
    const double addButtonWidth = 32;
    const double windowControlsWidth = 62;

    return Container(
      height: 32,
      color: const Color(0xFF1E1E1E),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final reserved = addButtonWidth + windowControlsWidth;
          final availableForTabs = (constraints.maxWidth - reserved).clamp(0.0, double.infinity);
          final naturalWidth = _tabs.length * maxTabWidth;
          final perTabWidth = (naturalWidth <= availableForTabs || _tabs.isEmpty)
              ? maxTabWidth
              : (availableForTabs / _tabs.length).clamp(minTabWidth, maxTabWidth);
          final tabsWidth = (perTabWidth * _tabs.length).clamp(0.0, availableForTabs);

          return Row(
            children: [
              SizedBox(
                width: tabsWidth,
                child: ListView.builder(
                  scrollDirection: Axis.horizontal,
                  itemCount: _tabs.length,
                  itemBuilder: (context, index) {
                final t = _tabs[index];
                final isActive = index == _activeIndex;
                return GestureDetector(
                  onTap: () => _activateTab(index),
                  child: Container(
                    width: perTabWidth,
                    margin: const EdgeInsets.only(top: 6, right: 2),
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                    decoration: BoxDecoration(
                      color: isActive ? const Color(0xFF252526) : Colors.transparent,
                      borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
                      border: isActive
                          ? Border.all(color: Colors.grey[800]!, width: 0.5)
                          : null,
                    ),
                    child: Row(
                      children: [
                        Icon(
                          t.isLoading
                              ? Icons.autorenew
                              : (t.isSecure ? Icons.lock_outline : Icons.public),
                          size: 13,
                          color: isActive ? Colors.grey[300] : Colors.grey[600],
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            t.title,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: 12,
                              color: isActive ? Colors.white : Colors.grey[500],
                            ),
                          ),
                        ),
                        const SizedBox(width: 4),
                        InkWell(
                          onTap: () => _closeTab(index),
                          borderRadius: BorderRadius.circular(10),
                          child: Icon(Icons.close, size: 13, color: Colors.grey[500]),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
          IconButton(
            icon: const Icon(Icons.add, size: 18),
            tooltip: 'New tab (Ctrl+T)',
            onPressed: () => _openNewTab(),
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
          ),
          // Empty draggable space between "+" and the window controls --
          // without this, the minimize/maximize buttons sat right next to
          // "+" instead of staying pinned to the far right edge.
          const Expanded(child: SizedBox()),
          Consumer(
            builder: (context, ref, _) {
              final isMaximized = ref.watch(browserMaximizedProvider);
              return Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.minimize, size: 15),
                    tooltip: 'Minimize (restore panels)',
                    onPressed: isMaximized ? () => ref.read(browserMaximizedProvider.notifier).toggle() : null,
                    padding: EdgeInsets.zero,
                    constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
                  ),
                  IconButton(
                    icon: Icon(isMaximized ? Icons.filter_none : Icons.crop_square, size: 13),
                    tooltip: isMaximized ? 'Browser is maximized' : 'Maximize (hide side panels)',
                    onPressed: isMaximized ? null : () => ref.read(browserMaximizedProvider.notifier).toggle(),
                    padding: EdgeInsets.zero,
                    constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
                  ),
                ],
              );
            },
          ),
              const SizedBox(width: 4),
            ],
          );
        },
      ),
    );
  }

  Widget _buildToolbar(_BrowserTab tab) {
    return Container(
      color: const Color(0xFF252526),
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.arrow_back, size: 18),
            tooltip: 'Back',
            onPressed: tab.canGoBack ? () => tab.controller?.goBack() : null,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 30, minHeight: 30),
          ),
          IconButton(
            icon: const Icon(Icons.arrow_forward, size: 18),
            tooltip: 'Forward',
            onPressed: tab.canGoForward ? () => tab.controller?.goForward() : null,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 30, minHeight: 30),
          ),
          IconButton(
            icon: Icon(tab.isLoading ? Icons.close : Icons.refresh, size: 18),
            tooltip: tab.isLoading ? 'Stop' : 'Reload (Ctrl+R)',
            onPressed: () => _reload(tab),
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 30, minHeight: 30),
          ),
          IconButton(
            icon: const Icon(Icons.home_outlined, size: 18),
            tooltip: 'Home',
            onPressed: () => _goHome(tab),
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 30, minHeight: 30),
          ),
          Expanded(
            child: Container(
              height: 28,
              margin: const EdgeInsets.symmetric(horizontal: 6),
              padding: const EdgeInsets.symmetric(horizontal: 10),
              decoration: BoxDecoration(
                color: const Color(0xFF3C3C3C),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Row(
                children: [
                  Icon(
                    tab.isSecure ? Icons.lock : Icons.info_outline,
                    size: 13,
                    color: tab.isSecure ? Colors.greenAccent[400] : Colors.grey[400],
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: TextField(
                      controller: _addressController,
                      focusNode: _addressFocusNode,
                      style: const TextStyle(fontSize: 13),
                      decoration: const InputDecoration(
                        border: InputBorder.none,
                        isDense: true,
                        hintText: 'Search or type a URL (Ctrl+L)',
                      ),
                      onSubmitted: (value) => _navigate(tab, value),
                    ),
                  ),
                  InkWell(
                    onTap: () => setState(() => tab.isBookmarked = !tab.isBookmarked),
                    child: Icon(
                      tab.isBookmarked ? Icons.star : Icons.star_border,
                      size: 15,
                      color: tab.isBookmarked ? Colors.amberAccent : Colors.grey[400],
                    ),
                  ),
                ],
              ),
            ),
          ),
          Consumer(
            builder: (context, ref, _) => PopupMenuButton<String>(
            tooltip: 'Menu',
            icon: const Icon(Icons.more_vert, size: 18),
            color: const Color(0xFF2D2D30),
            constraints: const BoxConstraints(minWidth: 230, maxWidth: 260),
            onSelected: (value) {
              switch (value) {
                case 'new_tab':
                  _openNewTab();
                  break;
                case 'new_window':
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('New window isn\'t available inside the app browser.')),
                  );
                  break;
                case 'new_incognito':
                  _openNewTab(title: 'Incognito');
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Opened as a plain new tab — private browsing isn\'t modeled here yet.')),
                  );
                  break;
                case 'history':
                  _showHistoryDialog(context);
                  break;
                case 'downloads':
                  _showDownloadsDialog(context);
                  break;
                case 'bookmarks':
                  _showBookmarksDialog(context);
                  break;
                case 'print':
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Print isn\'t available in the embedded browser yet.')),
                  );
                  break;
                case 'cast':
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Cast isn\'t available in the embedded browser yet.')),
                  );
                  break;
                case 'find':
                  _findInPage(tab);
                  break;
                case 'translate':
                  _translatePage(tab);
                  break;
                case 'more_tools':
                  _showStubDialog(context, 'More tools', 'Extensions and developer tools aren\'t available here yet.', Icons.build_outlined);
                  break;
                case 'settings':
                  showDialog(context: context, builder: (_) => const _BrowserSettingsDialog());
                  break;
                case 'help':
                  _showStubDialog(context, 'Help', 'TradePilot Browser — built on flutter_inappwebview.', Icons.help_outline);
                  break;
              }
            },
            itemBuilder: (context) => [
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'new_tab', child: _MenuRow(label: 'New tab', shortcut: 'Ctrl+T')),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'new_window', child: _MenuRow(label: 'New window', shortcut: 'Ctrl+N')),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'new_incognito', child: _MenuRow(label: 'New Incognito tab', shortcut: 'Ctrl+Shift+N')),
              const PopupMenuDivider(height: 8),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'history', child: _MenuRow(label: 'History', trailingArrow: true)),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'downloads', child: _MenuRow(label: 'Downloads', shortcut: 'Ctrl+J')),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'bookmarks', child: _MenuRow(label: 'Bookmarks', trailingArrow: true)),
              const PopupMenuDivider(height: 8),
              PopupMenuItem(
                enabled: false,
                height: 30,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                child: _ZoomRow(
                  zoomPercent: tab.zoomPercent,
                  onZoomOut: () => _applyZoom(tab, -10),
                  onZoomIn: () => _applyZoom(tab, 10),
                  onZoomReset: () {
                    setState(() => tab.zoomPercent = 100);
                    tab.controller?.evaluateJavascript(source: "document.body.style.zoom='100%';");
                  },
                  isFullscreen: ref.watch(browserMaximizedProvider),
                  onToggleFullscreen: () => ref.read(browserMaximizedProvider.notifier).toggle(),
                ),
              ),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'print', child: _MenuRow(label: 'Print…', shortcut: 'Ctrl+P')),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'cast', child: _MenuRow(label: 'Cast…')),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'find', child: _MenuRow(label: 'Find in page', shortcut: 'Ctrl+F')),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'translate', child: _MenuRow(label: 'Translate this page')),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'more_tools', child: _MenuRow(label: 'More tools', trailingArrow: true)),
              const PopupMenuDivider(height: 8),
              PopupMenuItem(
                enabled: false,
                height: 30,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                child: _EditRow(
                  onCut: () => _cutAddressBar(),
                  onCopy: () => _copyAddressBar(),
                  onPaste: () => _pasteAddressBar(tab),
                ),
              ),
              const PopupMenuDivider(height: 8),
              PopupMenuItem(
                height: 30,
                padding: const EdgeInsets.symmetric(horizontal: 12),
                value: 'toggle_bookmarks',
                child: _MenuRow(label: _showBookmarksBar ? 'Hide bookmarks bar' : 'Show bookmarks bar'),
              ),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'settings', child: _MenuRow(label: 'Settings')),
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'help', child: _MenuRow(label: 'Help', trailingArrow: true)),
              const PopupMenuDivider(height: 8),
              const PopupMenuItem(
                enabled: false,
                height: 26,
                padding: EdgeInsets.symmetric(horizontal: 12),
                child: Text('TradePilot Browser · v1.0', style: TextStyle(fontSize: 10.5, color: Colors.grey)),
              ),
            ],
          ),
          ),
        ],
      ),
    );
  }

  void _showBookmarksDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF2D2D30),
          title: const Text('Bookmarks', style: TextStyle(fontSize: 15)),
          content: SizedBox(
            width: 340,
            height: 320,
            child: _bookmarks.isEmpty
                ? Center(
                    child: Text('Star a page to bookmark it.', style: TextStyle(color: Colors.grey[500], fontSize: 13)),
                  )
                : ListView.builder(
                    itemCount: _bookmarks.length,
                    itemBuilder: (context, index) {
                      final link = _bookmarks[index];
                      return ListTile(
                        dense: true,
                        leading: Icon(link.icon, size: 16, color: Colors.grey[400]),
                        title: Text(link.label, style: const TextStyle(fontSize: 13)),
                        subtitle: Text(link.url, style: TextStyle(fontSize: 10.5, color: Colors.grey[600]), maxLines: 1, overflow: TextOverflow.ellipsis),
                        trailing: IconButton(
                          icon: const Icon(Icons.delete_outline, size: 16),
                          onPressed: () {
                            setState(() => _bookmarks.removeAt(index));
                            setDialogState(() {});
                          },
                        ),
                        onTap: () {
                          Navigator.pop(ctx);
                          _navigate(_activeTab, link.url);
                        },
                      );
                    },
                  ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Close')),
          ],
        ),
      ),
    );
  }

  void _showHistoryDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF2D2D30),
          title: Row(
            children: [
              const Expanded(child: Text('History', style: TextStyle(fontSize: 15))),
              if (_history.isNotEmpty)
                TextButton(
                  onPressed: () {
                    setState(() => _history.clear());
                    setDialogState(() {});
                  },
                  child: const Text('Clear all', style: TextStyle(fontSize: 12)),
                ),
            ],
          ),
          content: SizedBox(
            width: 380,
            height: 360,
            child: _history.isEmpty
                ? Center(
                    child: Text('No browsing history yet.', style: TextStyle(color: Colors.grey[500], fontSize: 13)),
                  )
                : ListView.builder(
                    itemCount: _history.length,
                    itemBuilder: (context, index) {
                      final entry = _history[index];
                      final t = entry.visitedAt;
                      final timeLabel = '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';
                      return ListTile(
                        dense: true,
                        leading: Text(timeLabel, style: TextStyle(fontSize: 11, color: Colors.grey[500])),
                        title: Text(entry.title, style: const TextStyle(fontSize: 13), maxLines: 1, overflow: TextOverflow.ellipsis),
                        subtitle: Text(entry.url, style: TextStyle(fontSize: 10.5, color: Colors.grey[600]), maxLines: 1, overflow: TextOverflow.ellipsis),
                        trailing: IconButton(
                          icon: const Icon(Icons.close, size: 15),
                          onPressed: () {
                            setState(() => _history.removeAt(index));
                            setDialogState(() {});
                          },
                        ),
                        onTap: () {
                          Navigator.pop(ctx);
                          _navigate(_activeTab, entry.url);
                        },
                      );
                    },
                  ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Close')),
          ],
        ),
      ),
    );
  }

  void _showDownloadsDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF2D2D30),
          title: const Text('Downloads', style: TextStyle(fontSize: 15)),
          content: SizedBox(
            width: 360,
            height: 320,
            child: _downloads.isEmpty
                ? Center(
                    child: Text('No downloads yet.', style: TextStyle(color: Colors.grey[500], fontSize: 13)),
                  )
                : ListView.builder(
                    itemCount: _downloads.length,
                    itemBuilder: (context, index) {
                      final d = _downloads[index];
                      return ListTile(
                        dense: true,
                        leading: Icon(Icons.insert_drive_file_outlined, size: 18, color: Colors.grey[400]),
                        title: Text(d.filename, style: const TextStyle(fontSize: 13), maxLines: 1, overflow: TextOverflow.ellipsis),
                        subtitle: Text(d.url, style: TextStyle(fontSize: 10.5, color: Colors.grey[600]), maxLines: 1, overflow: TextOverflow.ellipsis),
                        trailing: IconButton(
                          icon: const Icon(Icons.open_in_new, size: 15),
                          tooltip: 'Open again',
                          onPressed: () {
                            Navigator.pop(ctx);
                            _openNewTab(url: d.url, started: true, title: d.filename);
                          },
                        ),
                      );
                    },
                  ),
          ),
          actions: [
            if (_downloads.isNotEmpty)
              TextButton(
                onPressed: () {
                  setState(() => _downloads.clear());
                  setDialogState(() {});
                },
                child: const Text('Clear all'),
              ),
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Close')),
          ],
        ),
      ),
    );
  }

  void _showStubDialog(BuildContext context, String title, String message, IconData icon) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: Text(title, style: const TextStyle(fontSize: 15)),
        content: SizedBox(
          height: 100,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: 32, color: Colors.grey[600]),
              const SizedBox(height: 10),
              Text(message, style: TextStyle(color: Colors.grey[400], fontSize: 13)),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Close')),
        ],
      ),
    );
  }

  Widget _buildBookmarksBar(_BrowserTab tab) {
    return Container(
      height: 26,
      color: const Color(0xFF232324),
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: _bookmarks.isEmpty
          ? Center(
              child: Text(
                'Star a page to add it here, right-click a shortcut to remove it',
                style: TextStyle(fontSize: 11, color: Colors.grey[600]),
              ),
            )
          : ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: _bookmarks.length,
        separatorBuilder: (_, __) => const SizedBox(width: 4),
        itemBuilder: (context, index) {
          final link = _bookmarks[index];
          return GestureDetector(
            onSecondaryTapDown: (details) => _showBookmarkContextMenu(details.globalPosition, index),
            onLongPress: () => _showBookmarkContextMenu(null, index),
            child: InkWell(
              borderRadius: BorderRadius.circular(4),
              onTap: () => _navigate(tab, link.url),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                child: Row(
                  children: [
                    Icon(link.icon, size: 13, color: Colors.grey[400]),
                    const SizedBox(width: 5),
                    Text(link.label, style: TextStyle(fontSize: 11.5, color: Colors.grey[300])),
                  ],
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  /// Right-click (desktop/web) or long-press (touch) context menu for a
  /// single bookmark shortcut, letting it be removed from the bar --
  /// exactly like Chrome's "Remove" option on its own bookmarks bar.
  Future<void> _showBookmarkContextMenu(Offset? globalPosition, int index) async {
    final RenderBox overlay = Overlay.of(context).context.findRenderObject() as RenderBox;
    final position = globalPosition ?? overlay.localToGlobal(Offset.zero);
    final selected = await showMenu<String>(
      context: context,
      position: RelativeRect.fromRect(
        position & const Size(1, 1),
        Offset.zero & overlay.size,
      ),
      color: const Color(0xFF2D2D30),
      items: [
        PopupMenuItem(
          value: 'remove',
          height: 32,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Text('Remove "${_bookmarks[index].label}"', style: const TextStyle(fontSize: 12.5)),
        ),
      ],
    );
    if (selected == 'remove') {
      setState(() => _bookmarks.removeAt(index));
    }
  }

  Widget _buildTabBody(_BrowserTab tab) {
    if (!tab.started) {
      return _NewTabPage(onGo: (input) => _navigate(tab, input));
    }
    return InAppWebView(
      key: tab.webViewKey,
      initialUrlRequest: URLRequest(url: WebUri(tab.url)),
      initialSettings: InAppWebViewSettings(
        isInspectable: true,
        mediaPlaybackRequiresUserGesture: false,
        javaScriptEnabled: true,
        transparentBackground: true,
        supportZoom: true,
        useOnDownloadStart: true,
        javaScriptCanOpenWindowsAutomatically: true,
        supportMultipleWindows: true,
      ),
      onWebViewCreated: (controller) {
        tab.controller = controller;
      },
      // Links with target="_blank" / window.open() now actually open as a
      // new tab in this browser, instead of doing nothing or navigating
      // the current tab away -- a real "new window" feature, just modeled
      // as a tab since this is an embedded browser without OS windows.
      onCreateWindow: (controller, createWindowAction) async {
        final req = createWindowAction.request;
        _openNewTab(
          url: req.url.toString(),
          started: true,
          title: 'Loading\u2026',
        );
        return true;
      },
      // Real download capture: the webview can't write arbitrary files to
      // disk across every platform without extra native plugins, but we
      // *can* reliably intercept the request, record it, and hand the URL
      // back to the OS/browser download pipeline via an external tab --
      // which is what actually completes the download.
      onDownloadStartRequest: (controller, request) async {
        final filename = request.suggestedFilename ??
            (request.url.pathSegments.isNotEmpty ? request.url.pathSegments.last : 'download');
        setState(() {
          _downloads.insert(
            0,
            _DownloadEntry(url: request.url.toString(), filename: filename, startedAt: DateTime.now()),
          );
        });
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Downloading $filename\u2026'), duration: const Duration(seconds: 2)),
          );
        }
        _openNewTab(url: request.url.toString(), started: true, title: filename);
      },
      onLoadStart: (controller, url) {
        setState(() {
          tab.isLoading = true;
          tab.progress = 0;
          tab.url = url.toString();
          tab.isSecure = tab.url.startsWith('https://');
          if (identical(tab, _activeTab)) _addressController.text = tab.url;
        });
      },
      onTitleChanged: (controller, title) {
        if (title == null || title.isEmpty) return;
        setState(() => tab.title = title);
      },
      onLoadStop: (controller, url) async {
        final back = await controller.canGoBack();
        final forward = await controller.canGoForward();
        final pageTitle = await controller.getTitle();
        setState(() {
          tab.isLoading = false;
          tab.progress = 1;
          tab.canGoBack = back;
          tab.canGoForward = forward;
          if (pageTitle != null && pageTitle.isNotEmpty) tab.title = pageTitle;
          if (url != null) {
            tab.url = url.toString();
            tab.isSecure = tab.url.startsWith('https://');
          }
          if (identical(tab, _activeTab)) _addressController.text = tab.url;
          // Record real browsing history — skip if this exact URL is still
          // the most recent entry (e.g. a reload) to avoid spamming dupes.
          if (tab.url.isNotEmpty && (_history.isEmpty || _history.first.url != tab.url)) {
            _history.insert(0, _HistoryEntry(url: tab.url, title: tab.title, visitedAt: DateTime.now()));
          }
        });
      },
      onProgressChanged: (controller, p) {
        setState(() {
          tab.progress = p / 100;
          tab.isLoading = p < 100;
        });
      },
      onReceivedError: (controller, request, error) {
        setState(() {
          tab.isLoading = false;
          tab.title = 'Unable to load page';
        });
      },
    );
  }
}

/// Chrome-style "New Tab" landing page with a search box and quick links,
/// shown until the tab actually navigates somewhere.
class _NewTabPage extends StatefulWidget {
  final ValueChanged<String> onGo;
  const _NewTabPage({required this.onGo});

  @override
  State<_NewTabPage> createState() => _NewTabPageState();
}

class _NewTabPageState extends State<_NewTabPage> {
  final TextEditingController _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF1E1E1E),
      alignment: Alignment.center,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 480),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.public, size: 48, color: Colors.grey[700]),
            const SizedBox(height: 16),
            Container(
              height: 44,
              padding: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(
                color: const Color(0xFF2D2D30),
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: Colors.grey[800]!),
              ),
              child: Row(
                children: [
                  Icon(Icons.search, size: 18, color: Colors.grey[500]),
                  const SizedBox(width: 10),
                  Expanded(
                    child: TextField(
                      controller: _controller,
                      autofocus: true,
                      decoration: const InputDecoration(
                        border: InputBorder.none,
                        isDense: true,
                        hintText: 'Search Google or type a URL',
                      ),
                      onSubmitted: widget.onGo,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              alignment: WrapAlignment.center,
              children: _kQuickLinks
                  .map(
                    (l) => InkWell(
                      borderRadius: BorderRadius.circular(10),
                      onTap: () => widget.onGo(l.url),
                      child: Container(
                        width: 96,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        decoration: BoxDecoration(
                          color: const Color(0xFF2A2A2E),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Column(
                          children: [
                            Icon(l.icon, color: Colors.blueAccent[100], size: 20),
                            const SizedBox(height: 6),
                            Text(
                              l.label,
                              textAlign: TextAlign.center,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(fontSize: 11, color: Colors.grey[400]),
                            ),
                          ],
                        ),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ],
        ),
      ),
    );
  }
}

/// Chrome-style Settings dialog: a left rail of categories + a right content
/// pane, mirroring a real browser's settings page. Most sections are
/// informational placeholders since this is an embedded app browser rather
/// than a full standalone browser profile.
class _SettingsCategory {
  final String label;
  final IconData icon;
  const _SettingsCategory(this.label, this.icon);
}

const List<_SettingsCategory> _kSettingsCategories = [
  _SettingsCategory('Privacy & security', Icons.privacy_tip_outlined),
  _SettingsCategory('Appearance', Icons.palette_outlined),
  _SettingsCategory('Search engine', Icons.search),
  _SettingsCategory('Downloads', Icons.download_outlined),
  _SettingsCategory('Extensions', Icons.extension_outlined),
  _SettingsCategory('About', Icons.info_outline),
];

class _BrowserSettingsDialog extends StatefulWidget {
  const _BrowserSettingsDialog();

  @override
  State<_BrowserSettingsDialog> createState() => _BrowserSettingsDialogState();
}

class _BrowserSettingsDialogState extends State<_BrowserSettingsDialog> {
  int _selected = 0;
  bool _blockThirdPartyCookies = true;
  bool _doNotTrack = false;
  bool _safeBrowsing = true;

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: const Color(0xFF2D2D30),
      child: SizedBox(
        width: 560,
        height: 420,
        child: Row(
          children: [
            Container(
              width: 190,
              decoration: const BoxDecoration(
                border: Border(right: BorderSide(color: Color(0xFF1E1E1E))),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Padding(
                    padding: EdgeInsets.fromLTRB(16, 16, 16, 8),
                    child: Text('Settings', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  ),
                  for (int i = 0; i < _kSettingsCategories.length; i++)
                    InkWell(
                      onTap: () => setState(() => _selected = i),
                      child: Container(
                        color: i == _selected ? const Color(0xFF3C3C3C) : Colors.transparent,
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                        child: Row(
                          children: [
                            Icon(_kSettingsCategories[i].icon, size: 16, color: Colors.grey[300]),
                            const SizedBox(width: 10),
                            Text(_kSettingsCategories[i].label, style: const TextStyle(fontSize: 12.5)),
                          ],
                        ),
                      ),
                    ),
                ],
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: _buildSettingsContent(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSettingsContent() {
    switch (_selected) {
      case 0:
        return _settingsSection('Privacy & security', [
          SwitchListTile(
            value: _blockThirdPartyCookies,
            onChanged: (v) => setState(() => _blockThirdPartyCookies = v),
            title: const Text('Block third-party cookies', style: TextStyle(fontSize: 13)),
            activeColor: Colors.blueAccent,
            dense: true,
          ),
          SwitchListTile(
            value: _safeBrowsing,
            onChanged: (v) => setState(() => _safeBrowsing = v),
            title: const Text('Safe Browsing protection', style: TextStyle(fontSize: 13)),
            activeColor: Colors.blueAccent,
            dense: true,
          ),
          SwitchListTile(
            value: _doNotTrack,
            onChanged: (v) => setState(() => _doNotTrack = v),
            title: const Text('Send "Do Not Track" requests', style: TextStyle(fontSize: 13)),
            activeColor: Colors.blueAccent,
            dense: true,
          ),
        ]);
      case 1:
        return _settingsSection('Appearance', const [
          ListTile(
            dense: true,
            contentPadding: EdgeInsets.zero,
            title: Text('Theme', style: TextStyle(fontSize: 13)),
            subtitle: Text('Dark (matches TradePilot workspace)', style: TextStyle(fontSize: 12, color: Colors.grey)),
          ),
        ]);
      case 2:
        return _settingsSection('Search engine', const [
          ListTile(
            dense: true,
            contentPadding: EdgeInsets.zero,
            title: Text('Search engine used in the address bar', style: TextStyle(fontSize: 13)),
            subtitle: Text('Google', style: TextStyle(fontSize: 12, color: Colors.grey)),
          ),
        ]);
      case 3:
        return _settingsSection('Downloads', const [
          ListTile(
            dense: true,
            contentPadding: EdgeInsets.zero,
            title: Text('No downloads yet', style: TextStyle(fontSize: 13)),
          ),
        ]);
      case 4:
        return _settingsSection('Extensions', const [
          ListTile(
            dense: true,
            contentPadding: EdgeInsets.zero,
            title: Text('No extensions installed', style: TextStyle(fontSize: 13)),
          ),
        ]);
      default:
        return _settingsSection('About', const [
          ListTile(
            dense: true,
            contentPadding: EdgeInsets.zero,
            title: Text('TradePilot Browser', style: TextStyle(fontSize: 13)),
            subtitle: Text('Powered by flutter_inappwebview', style: TextStyle(fontSize: 12, color: Colors.grey)),
          ),
        ]);
    }
  }

  Widget _settingsSection(String title, List<Widget> children) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold)),
        const SizedBox(height: 12),
        Expanded(child: ListView(children: children)),
        Align(
          alignment: Alignment.centerRight,
          child: TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Close'),
          ),
        ),
      ],
    );
  }
}

/// A single Chrome-style menu row: label on the left, optional shortcut text
/// or submenu arrow on the right.
class _MenuRow extends StatelessWidget {
  final String label;
  final String? shortcut;
  final bool trailingArrow;

  const _MenuRow({required this.label, this.shortcut, this.trailingArrow = false});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(child: Text(label, style: const TextStyle(fontSize: 13))),
        if (shortcut != null)
          Text(shortcut!, style: TextStyle(fontSize: 11.5, color: Colors.grey[500])),
        if (trailingArrow)
          Icon(Icons.arrow_right, size: 18, color: Colors.grey[500]),
      ],
    );
  }
}

/// The inline "-  100%  +  [fullscreen]" row shown inside the menu, matching
/// Chrome's Zoom control. Lives inside a `PopupMenuItem(enabled: false, ...)`
/// so tapping its own buttons doesn't close the whole menu.
class _ZoomRow extends StatelessWidget {
  final int zoomPercent;
  final VoidCallback onZoomOut;
  final VoidCallback onZoomIn;
  final VoidCallback onZoomReset;
  final bool isFullscreen;
  final VoidCallback onToggleFullscreen;

  const _ZoomRow({
    required this.zoomPercent,
    required this.onZoomOut,
    required this.onZoomIn,
    required this.onZoomReset,
    required this.isFullscreen,
    required this.onToggleFullscreen,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Expanded(child: Text('Zoom', style: TextStyle(fontSize: 13))),
        InkWell(
          onTap: onZoomOut,
          borderRadius: BorderRadius.circular(4),
          child: const Padding(
            padding: EdgeInsets.symmetric(horizontal: 6, vertical: 4),
            child: Icon(Icons.remove, size: 16),
          ),
        ),
        InkWell(
          onTap: onZoomReset,
          borderRadius: BorderRadius.circular(4),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: Text('$zoomPercent%', style: const TextStyle(fontSize: 12.5)),
          ),
        ),
        InkWell(
          onTap: onZoomIn,
          borderRadius: BorderRadius.circular(4),
          child: const Padding(
            padding: EdgeInsets.symmetric(horizontal: 6, vertical: 4),
            child: Icon(Icons.add, size: 16),
          ),
        ),
        const SizedBox(width: 4),
        InkWell(
          onTap: onToggleFullscreen,
          borderRadius: BorderRadius.circular(4),
          child: Padding(
            padding: const EdgeInsets.all(4),
            child: Icon(isFullscreen ? Icons.fullscreen_exit : Icons.fullscreen, size: 18),
          ),
        ),
      ],
    );
  }
}

/// The inline "Cut / Copy / Paste" row, matching Chrome's Edit control.
/// These act on the address bar's current text/selection.
class _EditRow extends StatelessWidget {
  final VoidCallback onCut;
  final VoidCallback onCopy;
  final VoidCallback onPaste;

  const _EditRow({required this.onCut, required this.onCopy, required this.onPaste});

  @override
  Widget build(BuildContext context) {
    Widget button(String label, VoidCallback onTap) {
      return Expanded(
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(4),
          child: Container(
            margin: const EdgeInsets.symmetric(horizontal: 2),
            padding: const EdgeInsets.symmetric(vertical: 6),
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey[800]!),
              borderRadius: BorderRadius.circular(4),
            ),
            alignment: Alignment.center,
            child: Text(label, style: const TextStyle(fontSize: 11.5)),
          ),
        ),
      );
    }

    return Row(
      children: [
        Text('Edit', style: TextStyle(fontSize: 11.5, color: Colors.grey[500])),
        const SizedBox(width: 8),
        button('Cut', onCut),
        button('Copy', onCopy),
        button('Paste', onPaste),
      ],
    );
  }
}
