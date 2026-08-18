import 'dart:collection';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:multi_split_view/multi_split_view.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../../core/navigation/activity_bar.dart';
import '../services/password_vault.dart';
import '../services/session_manager.dart';
import '../services/screenshot_service.dart';
import '../services/workspace_manager.dart' as ws;
import '../services/history_manager.dart';
import '../services/download_manager.dart';
import '../services/permission_manager.dart';
import '../../../core/security/https_enforcer.dart';
import '../../../core/security/safe_browsing_service.dart';
import '../../../core/security/tracking_protection_lists.dart';
import '../../../core/security/fingerprint_protection_script.dart';

/// Default homepage used by the Home button. Google, matching what every
/// mainstream browser defaults to -- a genuinely fresh install's first tab
/// is still a blank "New Tab" search page (see _loadWorkspaceTabs), not
/// this URL; this only fires when the Home button is tapped, or when
/// there's no saved session at all to restore (see _initWorkspacesAndRestore).
const String kBrowserHomeUrl = 'https://www.google.com';
const String _kVerticalTabsPrefKey = 'tradepilot_browser_vertical_tabs';

class _QuickLink {
  final String label;
  final String url;
  final IconData icon;
  const _QuickLink(this.label, this.url, this.icon);
}

// PRD 3.3.2 Tracking Protection content blockers now live in
// core/security/tracking_protection_lists.dart (buildTrackerContentBlockers()),
// and PRD 3.3.3's injected script in core/security/fingerprint_protection_script.dart
// (kFingerprintProtectionScript) -- both shared with anything else that
// embeds a WebView, not duplicated per call site.
final List<ContentBlocker> _kTrackerBlockList = buildTrackerContentBlockers();

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
  // Fase PRD 3.3.1 / 2.2.11 -- Incognito Mode: no history recording, no
  // persistent cookie/storage (via InAppWebViewSettings.incognito), and a
  // distinct visual so it's obvious which tabs are private.
  final bool isIncognito;
  // PRD 2.2.3 "Tab Groups" -- null when the tab isn't in any group.
  String? groupId;

  _BrowserTab({
    required this.id,
    this.url = '',
    this.title = 'New Tab',
    this.started = false,
    this.isIncognito = false,
  });
}

/// PRD 2.2.3 "Tab Groups" -- a named, colored bucket that tabs can be
/// assigned to (via the tab's right-click / long-press context menu),
/// with bulk close and collapse/expand so a cluster of related tabs
/// ("Forex Analysis", "Portfolio", "News"...) can be tucked away as one
/// small pill without actually closing them.
class _TabGroup {
  final String id;
  String name;
  Color color;
  bool isExpanded = true;

  _TabGroup({
    required this.id,
    required this.name,
    required this.color,
  });
}

/// A small fixed palette so groups get visibly distinct colors without a
/// full color-picker UI, cycling once more than this many groups exist.
const List<Color> _kTabGroupColors = [
  Colors.blueAccent,
  Colors.deepPurpleAccent,
  Colors.tealAccent,
  Colors.orangeAccent,
  Colors.pinkAccent,
  Colors.greenAccent,
];

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
  // PRD 2.2.8 History -- persisted via HistoryManager (see initState);
  // this in-memory list is just the current session's working copy that
  // the UI renders from.
  List<HistoryEntry> _history = [];
  // PRD 2.2.9 Downloads -- real transfers with live progress, tracked and
  // persisted by DownloadManager rather than an in-memory placeholder.
  final DownloadManager _downloadManager = DownloadManager();
  // PRD 2.2.2 Vertical Tabs -- persisted layout preference (left sidebar
  // vs the classic Chrome-style top strip).
  bool _verticalTabs = false;
  // Fase PRD Keyboard Shortcuts: Ctrl+Shift+T reopens the last closed tab --
  // small LIFO stack of what a closed tab's url/title/incognito state was.
  final List<({String url, String title, bool isIncognito})> _recentlyClosed = [];

  // PRD 2.2.3 "Tab Groups" -- every known group; a tab belongs to one via
  // _BrowserTab.groupId.
  final List<_TabGroup> _tabGroups = [];

  // PRD 2.2.5 "Workspace" -- named, switchable sets of tabs.
  List<ws.Workspace> _workspaces = [];
  String _activeWorkspaceId = 'default';

  // PRD 2.2.4 "Split View" -- when enabled, the content area shows two
  // panes side-by-side (or stacked) instead of a single active tab, each
  // pane independently choosing which open tab it displays.
  bool _splitViewEnabled = false;
  Axis _splitAxis = Axis.horizontal;
  List<int> _splitPaneTabIndex = [0, 0];
  final MultiSplitViewController _splitController = MultiSplitViewController();

  final TextEditingController _addressController = TextEditingController();
  final FocusNode _addressFocusNode = FocusNode();
  final FocusNode _shortcutsFocusNode = FocusNode();

  _BrowserTab get _activeTab => _tabs[_activeIndex];

  ws.Workspace get _activeWorkspace => _workspaces.firstWhere(
        (w) => w.id == _activeWorkspaceId,
        orElse: () => _workspaces.isNotEmpty ? _workspaces.first : ws.Workspace(id: 'default', name: 'Default'),
      );

  _TabGroup? _groupFor(String? groupId) {
    if (groupId == null) return null;
    for (final g in _tabGroups) {
      if (g.id == groupId) return g;
    }
    return null;
  }

  @override
  void initState() {
    super.initState();
    // A blank seed tab so `_tabs` is never empty on the very first build --
    // _initWorkspacesAndRestore() below loads the real tabs from disk
    // *asynchronously*, but Flutter calls build() synchronously right
    // after initState(), before that finishes. Without this seed tab,
    // `_activeTab`'s `_tabs[_activeIndex]` throws a RangeError on that
    // first frame -- which in a release build renders as a blank gray box
    // with no error text (Flutter's default release ErrorWidget), not the
    // red debug error screen, so it's easy to miss outside a release build.
    // _initWorkspacesAndRestore() replaces this seed tab moments later once
    // the real, persisted tabs are loaded.
    _tabs.add(_BrowserTab(id: _newId()));
    // PRD 2.2.6 Session Manager + PRD 2.2.5 Workspace: restore whichever
    // workspace was last active, with whatever tabs were open in it --
    // falls back to one blank "New Tab" in a fresh "Default" workspace on
    // first run.
    _initWorkspacesAndRestore();
    _loadPersistedHistory();
    _downloadManager.loadPersisted();
    _loadVerticalTabsPref();
  }

  Future<void> _loadPersistedHistory() async {
    final loaded = await HistoryManager.loadAll();
    if (!mounted) return;
    setState(() => _history = loaded);
  }

  Future<void> _loadVerticalTabsPref() async {
    final prefs = await SharedPreferences.getInstance();
    final saved = prefs.getBool(_kVerticalTabsPrefKey);
    if (saved != null && mounted) setState(() => _verticalTabs = saved);
  }

  Future<void> _toggleVerticalTabs() async {
    setState(() => _verticalTabs = !_verticalTabs);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_kVerticalTabsPrefKey, _verticalTabs);
  }

  Future<void> _initWorkspacesAndRestore() async {
    var loaded = await ws.WorkspaceManager.loadAll();
    var activeId = await ws.WorkspaceManager.loadActiveId();

    if (loaded.isEmpty) {
      // First launch with Workspaces: migrate whatever the old
      // single-session storage had so nobody's open tabs disappear when
      // this feature ships.
      final legacy = await SessionManager.restoreSession();
      final defaultWorkspace = ws.Workspace(
        id: 'default',
        name: 'Default',
        icon: '🧭',
        tabs: legacy.map((t) => ws.SavedWorkspaceTab(url: t.url, title: t.title)).toList(),
      );
      loaded = [defaultWorkspace];
      activeId = defaultWorkspace.id;
      await ws.WorkspaceManager.saveAll(loaded);
      await ws.WorkspaceManager.saveActiveId(activeId);
    }

    if (activeId == null || !loaded.any((w) => w.id == activeId)) {
      activeId = loaded.first.id;
    }

    if (!mounted) return;
    setState(() {
      _workspaces = loaded;
      _activeWorkspaceId = activeId!;
    });
    _loadWorkspaceTabs(_activeWorkspace);
  }

  /// Replaces whatever tabs/groups are currently open with the ones saved
  /// for [workspace] -- used both on first launch and whenever the user
  /// switches workspaces.
  void _loadWorkspaceTabs(ws.Workspace workspace) {
    setState(() {
      _tabs.clear();
      _tabGroups.clear();
      _splitViewEnabled = false;
      if (workspace.tabs.isEmpty) {
        _tabs.add(_BrowserTab(id: _newId()));
      } else {
        for (final t in workspace.tabs) {
          _tabs.add(_BrowserTab(id: _newId(), url: t.url, started: true, title: t.title));
        }
      }
      _activeIndex = 0;
      _addressController.text = _tabs.first.url;
    });
  }

  /// Debounced session save -- called after any tab open/close/navigate so
  /// the *next* app launch (or workspace switch) resumes here. Incognito
  /// tabs are deliberately excluded (PRD 3.3.1).
  void _persistSession() {
    final tabsToSave = _tabs
        .where((t) => !t.isIncognito && t.started && t.url.isNotEmpty)
        .map((t) => SavedTab(url: t.url, title: t.title))
        .toList();
    SessionManager.saveSession(tabsToSave);
    if (_workspaces.isEmpty) return;
    final idx = _workspaces.indexWhere((w) => w.id == _activeWorkspaceId);
    if (idx == -1) return;
    _workspaces[idx].tabs = tabsToSave.map((t) => ws.SavedWorkspaceTab(url: t.url, title: t.title)).toList();
    ws.WorkspaceManager.saveAll(_workspaces);
  }

  // ---------------------------------------------------------------------
  // Workspace management (PRD 2.2.5)
  // ---------------------------------------------------------------------

  Future<void> _switchWorkspace(String id) async {
    if (id == _activeWorkspaceId || !_workspaces.any((w) => w.id == id)) return;
    _persistSession(); // save current tabs into the workspace being left
    setState(() => _activeWorkspaceId = id);
    await ws.WorkspaceManager.saveActiveId(id);
    _loadWorkspaceTabs(_activeWorkspace);
  }

  Future<void> _createWorkspace() async {
    final nameController = TextEditingController();
    final name = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: const Text('New workspace', style: TextStyle(fontSize: 15)),
        content: TextField(
          controller: nameController,
          autofocus: true,
          decoration: const InputDecoration(hintText: 'e.g. Forex Analysis, Crypto, Stocks'),
          onSubmitted: (v) => Navigator.pop(ctx, v),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, nameController.text), child: const Text('Create')),
        ],
      ),
    );
    if (name == null || name.trim().isEmpty) return;
    _persistSession(); // flush current workspace before creating/switching
    final newWorkspace = ws.Workspace(id: 'ws_${DateTime.now().millisecondsSinceEpoch}', name: name.trim());
    setState(() {
      _workspaces.add(newWorkspace);
      _activeWorkspaceId = newWorkspace.id;
    });
    await ws.WorkspaceManager.saveAll(_workspaces);
    await ws.WorkspaceManager.saveActiveId(newWorkspace.id);
    _loadWorkspaceTabs(newWorkspace);
  }

  Future<void> _renameWorkspace(ws.Workspace workspace) async {
    final nameController = TextEditingController(text: workspace.name);
    final name = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: const Text('Rename workspace', style: TextStyle(fontSize: 15)),
        content: TextField(controller: nameController, autofocus: true, onSubmitted: (v) => Navigator.pop(ctx, v)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, nameController.text), child: const Text('Save')),
        ],
      ),
    );
    if (name == null || name.trim().isEmpty) return;
    setState(() => workspace.name = name.trim());
    await ws.WorkspaceManager.saveAll(_workspaces);
  }

  Future<void> _deleteWorkspace(ws.Workspace workspace) async {
    if (_workspaces.length <= 1) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('At least one workspace must remain.')),
      );
      return;
    }
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: const Text('Delete workspace?', style: TextStyle(fontSize: 15)),
        content: Text('This closes and forgets all tabs saved in "${workspace.name}".', style: const TextStyle(fontSize: 13)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Delete')),
        ],
      ),
    );
    if (confirmed != true) return;
    final wasActive = workspace.id == _activeWorkspaceId;
    setState(() => _workspaces.removeWhere((w) => w.id == workspace.id));
    await ws.WorkspaceManager.saveAll(_workspaces);
    if (wasActive) {
      final next = _workspaces.first;
      setState(() => _activeWorkspaceId = next.id);
      await ws.WorkspaceManager.saveActiveId(next.id);
      _loadWorkspaceTabs(next);
    }
  }

  @override
  void dispose() {
    _addressController.dispose();
    _addressFocusNode.dispose();
    _shortcutsFocusNode.dispose();
    _splitController.dispose();
    _downloadManager.dispose();
    super.dispose();
  }

  String _newId() => 'tab_${_tabCounter++}';

  // ---------------------------------------------------------------------
  // Tab management
  // ---------------------------------------------------------------------

  void _openNewTab({String url = '', bool started = false, String? title, bool isIncognito = false}) {
    final tab = _BrowserTab(
      id: _newId(),
      url: url,
      started: started,
      title: title ?? (started ? 'Loading…' : (isIncognito ? 'New Incognito tab' : 'New Tab')),
      isIncognito: isIncognito,
    );
    setState(() {
      _tabs.add(tab);
      _activeIndex = _tabs.length - 1;
      _addressController.text = url;
    });
    _persistSession();
  }

  void _closeTab(int index) {
    final closed = _tabs[index];
    if (closed.started && closed.url.isNotEmpty) {
      _recentlyClosed.add((url: closed.url, title: closed.title, isIncognito: closed.isIncognito));
      if (_recentlyClosed.length > 10) _recentlyClosed.removeAt(0);
    }

    if (_tabs.length == 1) {
      // Never close the very last tab — reset it to a blank new tab instead,
      // matching how most browsers behave on the final tab.
      setState(() {
        _tabs[index] = _BrowserTab(id: _newId());
        if (_activeIndex == index) _addressController.text = '';
        _splitPaneTabIndex = [0, 0];
      });
      _pruneEmptyGroups();
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
      // Keep each split pane pointing at a valid, sensible tab after the
      // index shift above.
      for (var i = 0; i < _splitPaneTabIndex.length; i++) {
        if (_splitPaneTabIndex[i] >= _tabs.length) {
          _splitPaneTabIndex[i] = _tabs.length - 1;
        } else if (_splitPaneTabIndex[i] > index) {
          _splitPaneTabIndex[i] -= 1;
        }
      }
    });
    _pruneEmptyGroups();
    _persistSession();
  }

  /// Removes any [_TabGroup] that no tab currently belongs to — e.g. after
  /// its last member tab was closed.
  void _pruneEmptyGroups() {
    final activeGroupIds = _tabs.map((t) => t.groupId).whereType<String>().toSet();
    final before = _tabGroups.length;
    _tabGroups.removeWhere((g) => !activeGroupIds.contains(g.id));
    if (_tabGroups.length != before && mounted) setState(() {});
  }

  // ---------------------------------------------------------------------
  // Tab Groups (PRD 2.2.3)
  // ---------------------------------------------------------------------

  Future<void> _showTabContextMenu(Offset? globalPosition, int index) async {
    final tab = _tabs[index];
    final RenderBox overlay = Overlay.of(context).context.findRenderObject() as RenderBox;
    final position = globalPosition ?? overlay.localToGlobal(Offset.zero);
    final selected = await showMenu<String>(
      context: context,
      position: RelativeRect.fromRect(position & const Size(1, 1), Offset.zero & overlay.size),
      color: const Color(0xFF2D2D30),
      items: [
        const PopupMenuItem(
          value: 'new_group',
          height: 32,
          padding: EdgeInsets.symmetric(horizontal: 12),
          child: Text('Add tab to new group', style: TextStyle(fontSize: 12.5)),
        ),
        for (final g in _tabGroups)
          PopupMenuItem(
            value: 'group_${g.id}',
            height: 32,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: [
                Container(width: 8, height: 8, decoration: BoxDecoration(color: g.color, shape: BoxShape.circle)),
                const SizedBox(width: 8),
                Text('Add to "${g.name}"', style: const TextStyle(fontSize: 12.5)),
              ],
            ),
          ),
        if (tab.groupId != null)
          const PopupMenuItem(
            value: 'ungroup',
            height: 32,
            padding: EdgeInsets.symmetric(horizontal: 12),
            child: Text('Remove from group', style: TextStyle(fontSize: 12.5)),
          ),
        const PopupMenuDivider(height: 8),
        PopupMenuItem(
          value: 'split',
          height: 32,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Text(_splitViewEnabled ? 'Show this tab in split view' : 'Open in split view', style: const TextStyle(fontSize: 12.5)),
        ),
        const PopupMenuDivider(height: 8),
        const PopupMenuItem(
          value: 'close',
          height: 32,
          padding: EdgeInsets.symmetric(horizontal: 12),
          child: Text('Close tab', style: TextStyle(fontSize: 12.5)),
        ),
      ],
    );
    if (selected == null) return;
    if (selected == 'new_group') {
      await _createGroupWithTab(index);
    } else if (selected == 'ungroup') {
      setState(() => tab.groupId = null);
      _pruneEmptyGroups();
    } else if (selected == 'close') {
      _closeTab(index);
    } else if (selected == 'split') {
      _openTabInSplit(index);
    } else if (selected.startsWith('group_')) {
      setState(() => tab.groupId = selected.substring('group_'.length));
    }
  }

  Future<void> _createGroupWithTab(int index) async {
    final nameController = TextEditingController(text: 'New group');
    final name = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: const Text('New tab group', style: TextStyle(fontSize: 15)),
        content: TextField(
          controller: nameController,
          autofocus: true,
          decoration: const InputDecoration(hintText: 'e.g. Forex Analysis'),
          onSubmitted: (v) => Navigator.pop(ctx, v),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, nameController.text), child: const Text('Create')),
        ],
      ),
    );
    if (name == null || name.trim().isEmpty) return;
    final color = _kTabGroupColors[_tabGroups.length % _kTabGroupColors.length];
    final group = _TabGroup(id: 'grp_${DateTime.now().millisecondsSinceEpoch}', name: name.trim(), color: color);
    setState(() {
      _tabGroups.add(group);
      _tabs[index].groupId = group.id;
    });
  }

  void _toggleGroupExpanded(_TabGroup group) {
    setState(() => group.isExpanded = !group.isExpanded);
  }

  /// Bulk-close every tab in [group] (PRD "Close all tabs in group").
  void _closeGroupTabs(_TabGroup group) {
    final indices = <int>[
      for (var i = 0; i < _tabs.length; i++)
        if (_tabs[i].groupId == group.id) i,
    ];
    // Close from the highest index down so earlier indices stay valid.
    for (final i in indices.reversed) {
      _closeTab(i);
    }
  }

  void _ungroupAll(_TabGroup group) {
    setState(() {
      for (final t in _tabs) {
        if (t.groupId == group.id) t.groupId = null;
      }
      _tabGroups.removeWhere((g) => g.id == group.id);
    });
  }

  /// The "Tab groups" overview — rename, recolor, collapse/expand, bulk
  /// close, or ungroup, without needing to right-click each tab.
  void _showTabGroupsDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF2D2D30),
          title: const Text('Tab groups', style: TextStyle(fontSize: 15)),
          content: SizedBox(
            width: 360,
            height: 320,
            child: _tabGroups.isEmpty
                ? Center(
                    child: Text(
                      'No groups yet. Right-click a tab to create one.',
                      style: TextStyle(color: Colors.grey[500], fontSize: 13),
                    ),
                  )
                : ListView.builder(
                    itemCount: _tabGroups.length,
                    itemBuilder: (context, i) {
                      final g = _tabGroups[i];
                      final memberCount = _tabs.where((t) => t.groupId == g.id).length;
                      return ListTile(
                        dense: true,
                        leading: Container(width: 10, height: 10, decoration: BoxDecoration(color: g.color, shape: BoxShape.circle)),
                        title: Text(g.name, style: const TextStyle(fontSize: 13)),
                        subtitle: Text('$memberCount tab${memberCount == 1 ? '' : 's'}', style: TextStyle(fontSize: 11, color: Colors.grey[500])),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            IconButton(
                              icon: Icon(g.isExpanded ? Icons.unfold_less : Icons.unfold_more, size: 16),
                              tooltip: g.isExpanded ? 'Collapse' : 'Expand',
                              onPressed: () {
                                _toggleGroupExpanded(g);
                                setDialogState(() {});
                              },
                            ),
                            IconButton(
                              icon: const Icon(Icons.edit_outlined, size: 16),
                              tooltip: 'Rename',
                              onPressed: () async {
                                await _renameGroup(g);
                                setDialogState(() {});
                              },
                            ),
                            IconButton(
                              icon: const Icon(Icons.close, size: 16),
                              tooltip: 'Close all tabs in group',
                              onPressed: () {
                                _closeGroupTabs(g);
                                setDialogState(() {});
                              },
                            ),
                          ],
                        ),
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

  Future<void> _renameGroup(_TabGroup group) async {
    final nameController = TextEditingController(text: group.name);
    final name = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: const Text('Rename group', style: TextStyle(fontSize: 15)),
        content: TextField(controller: nameController, autofocus: true, onSubmitted: (v) => Navigator.pop(ctx, v)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          TextButton(onPressed: () => Navigator.pop(ctx, nameController.text), child: const Text('Save')),
        ],
      ),
    );
    if (name == null || name.trim().isEmpty) return;
    setState(() => group.name = name.trim());
  }

  // ---------------------------------------------------------------------
  // Split View (PRD 2.2.4)
  // ---------------------------------------------------------------------

  void _toggleSplitView() {
    setState(() {
      _splitViewEnabled = !_splitViewEnabled;
      if (_splitViewEnabled) {
        final left = _activeIndex;
        // Pick a sensible second tab: the next one over, or open a fresh
        // blank tab if this is the only one open.
        int right;
        if (_tabs.length > 1) {
          right = (left + 1) % _tabs.length;
        } else {
          _tabs.add(_BrowserTab(id: _newId()));
          right = _tabs.length - 1;
        }
        _splitPaneTabIndex = [left, right];
        _splitController.areas = [Area(data: 0), Area(data: 1)];
      }
    });
  }

  void _openTabInSplit(int tabIndex) {
    setState(() {
      _splitViewEnabled = true;
      final other = tabIndex == 0 && _tabs.length > 1 ? 1 : 0;
      _splitPaneTabIndex = [other == tabIndex ? tabIndex : other, tabIndex];
      _splitController.areas = [Area(data: 0), Area(data: 1)];
    });
  }

  void _setSplitPane(int pane, int tabIndex) {
    setState(() => _splitPaneTabIndex[pane] = tabIndex);
  }

  void _toggleSplitAxis() {
    setState(() => _splitAxis = _splitAxis == Axis.horizontal ? Axis.vertical : Axis.horizontal);
  }

  /// Closes one pane of the split view -- the remaining pane's tab becomes
  /// the single active tab again, exactly like Chrome's "close the other
  /// half" behavior when you drop a split.
  void _closeSplitPane(int pane) {
    final remainingPane = pane == 0 ? 1 : 0;
    final remainingTab = _splitPaneTabIndex[remainingPane].clamp(0, _tabs.length - 1);
    setState(() {
      _splitViewEnabled = false;
      _activeIndex = remainingTab;
      _addressController.text = _tabs[_activeIndex].url;
    });
  }

  /// Ctrl+Shift+T — reopens the most recently closed tab, exactly where it
  /// left off.
  void _reopenClosedTab() {
    if (_recentlyClosed.isEmpty) return;
    final last = _recentlyClosed.removeLast();
    _openNewTab(url: last.url, started: true, title: last.title, isIncognito: last.isIncognito);
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
    } else {
      // PRD 3.2.1 "HTTPS Only": auto-upgrade any explicit http:// entry to
      // https:// before it ever reaches the WebView. If the site genuinely
      // has no HTTPS, the load will fail visibly rather than silently
      // downgrading security.
      url = HttpsEnforcer.enforceHttps(url);
    }

    // PRD 3.2.5 Safe Browsing -- block known-bad destinations outright
    // instead of loading them, with a clear reason instead of a silent
    // failure.
    final host = Uri.tryParse(url)?.host ?? '';
    final safeBrowsingVerdict = SafeBrowsingService.check(host);
    if (safeBrowsingVerdict.isBlocked) {
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: const Color(0xFF2D2D30),
          title: const Row(
            children: [
              Icon(Icons.gpp_bad, color: Colors.redAccent, size: 20),
              SizedBox(width: 8),
              Text('Dangerous site blocked', style: TextStyle(fontSize: 15)),
            ],
          ),
          content: Text(
            'TradePilot Safe Browsing blocked "$host" because it matches a known phishing/malware pattern.',
            style: const TextStyle(fontSize: 13),
          ),
          actions: [TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('OK'))],
        ),
      );
      return;
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

  /// PRD 2.2.16 "Screenshot" -- captures the visible page and saves it via
  /// [saveScreenshot] (desktop/mobile: Downloads folder; web: browser
  /// download prompt).
  Future<void> _captureScreenshot(_BrowserTab tab) async {
    final messenger = ScaffoldMessenger.of(context);
    try {
      final Uint8List? bytes = await tab.controller?.takeScreenshot();
      if (bytes == null) {
        messenger.showSnackBar(const SnackBar(content: Text('Could not capture this page.')));
        return;
      }
      final safeName = tab.title.replaceAll(RegExp(r'[^a-zA-Z0-9]+'), '_').toLowerCase();
      final filename = 'tradepilot_${safeName.isEmpty ? 'screenshot' : safeName}_${DateTime.now().millisecondsSinceEpoch}.png';
      final savedPath = await saveScreenshot(bytes, filename);
      messenger.showSnackBar(SnackBar(content: Text('Screenshot saved: $savedPath')));
    } catch (e) {
      messenger.showSnackBar(SnackBar(content: Text('Screenshot failed: $e')));
    }
  }

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
        final shift = HardwareKeyboard.instance.isShiftPressed;
        final key = event.logicalKey;

        // F5 / F11 don't need Ctrl -- check these first.
        if (key == LogicalKeyboardKey.f5) {
          _reload(_activeTab);
          return KeyEventResult.handled;
        }
        if (key == LogicalKeyboardKey.f11) {
          ProviderScope.containerOf(context, listen: false).read(browserMaximizedProvider.notifier).toggle();
          return KeyEventResult.handled;
        }
        if (!ctrl) return KeyEventResult.ignored;

        if (shift && key == LogicalKeyboardKey.keyT) {
          _reopenClosedTab();
          return KeyEventResult.handled;
        }
        if (shift && key == LogicalKeyboardKey.keyN) {
          _openNewTab(isIncognito: true);
          return KeyEventResult.handled;
        }
        if (shift && key == LogicalKeyboardKey.delete) {
          setState(() => _history.clear());
          HistoryManager.clear();
          _downloadManager.clearAll();
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Browsing history and downloads cleared.'), duration: Duration(seconds: 2)),
          );
          return KeyEventResult.handled;
        }
        if (key == LogicalKeyboardKey.tab) {
          setState(() {
            final delta = shift ? -1 : 1;
            _activeIndex = (_activeIndex + delta) % _tabs.length;
            if (_activeIndex < 0) _activeIndex += _tabs.length;
            _addressController.text = _activeTab.url;
          });
          return KeyEventResult.handled;
        }
        if (key == LogicalKeyboardKey.keyT) {
          _openNewTab();
          return KeyEventResult.handled;
        }
        if (key == LogicalKeyboardKey.keyW) {
          _closeTab(_activeIndex);
          return KeyEventResult.handled;
        }
        if (key == LogicalKeyboardKey.keyL) {
          _addressFocusNode.requestFocus();
          return KeyEventResult.handled;
        }
        if (key == LogicalKeyboardKey.keyR) {
          _reload(_activeTab);
          return KeyEventResult.handled;
        }
        if (key == LogicalKeyboardKey.keyF) {
          _findInPage(_activeTab);
          return KeyEventResult.handled;
        }
        if (key == LogicalKeyboardKey.keyJ) {
          _showDownloadsDialog(context);
          return KeyEventResult.handled;
        }
        // Ctrl+1..8 jump to that tab, Ctrl+9 always jumps to the last tab --
        // identical semantics to Chrome/Firefox.
        const digitKeys = [
          LogicalKeyboardKey.digit1,
          LogicalKeyboardKey.digit2,
          LogicalKeyboardKey.digit3,
          LogicalKeyboardKey.digit4,
          LogicalKeyboardKey.digit5,
          LogicalKeyboardKey.digit6,
          LogicalKeyboardKey.digit7,
          LogicalKeyboardKey.digit8,
        ];
        final digitIndex = digitKeys.indexOf(key);
        if (digitIndex != -1) {
          if (digitIndex < _tabs.length) _activateTab(digitIndex);
          return KeyEventResult.handled;
        }
        if (key == LogicalKeyboardKey.digit9) {
          _activateTab(_tabs.length - 1);
          return KeyEventResult.handled;
        }
        return KeyEventResult.ignored;
      },
      child: Consumer(
        builder: (context, ref, _) {
          final isFullscreen = ref.watch(browserMaximizedProvider);
          final content = Column(
            children: [
              if (!isFullscreen && !_verticalTabs) _buildTabStrip(),
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
                    // PRD 2.2.4 Split View: swap the single-pane IndexedStack
                    // for a resizable multi-pane layout. Flutter reparents
                    // each tab's GlobalKey-ed InAppWebView in place rather
                    // than disposing it, so the underlying page/session
                    // survives the switch either way.
                    if (_splitViewEnabled) _buildSplitView() else
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

          // PRD 2.2.2 Vertical Tabs -- a left sidebar showing every tab
          // stacked vertically instead of the classic top strip, toggled
          // from the toolbar and persisted across restarts.
          if (!isFullscreen && _verticalTabs) {
            return Row(
              children: [
                _buildVerticalTabBar(),
                Expanded(child: content),
              ],
            );
          }
          return content;
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

          // PRD 2.2.3 Tab Groups: collapsed groups collapse their member
          // tabs down into a single small pill, in the position of the
          // first member tab encountered.
          final displayItems = <Widget>[];
          final renderedCollapsedGroups = <String>{};
          for (var index = 0; index < _tabs.length; index++) {
            final t = _tabs[index];
            final group = _groupFor(t.groupId);
            if (group != null && !group.isExpanded) {
              if (renderedCollapsedGroups.contains(group.id)) continue;
              renderedCollapsedGroups.add(group.id);
              displayItems.add(_buildGroupPill(group));
              continue;
            }
            displayItems.add(_buildTabChip(index, perTabWidth, group));
          }

          return Row(
            children: [
              // PRD 2.2.5 Workspace switcher.
              _buildWorkspaceSwitcher(),
              SizedBox(
                width: tabsWidth,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  children: displayItems,
                ),
              ),
          IconButton(
            icon: const Icon(Icons.add, size: 18),
            tooltip: 'New tab (Ctrl+T)',
            onPressed: () => _openNewTab(),
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
          ),
          IconButton(
            icon: Badge(
              isLabelVisible: _tabGroups.isNotEmpty,
              label: Text('${_tabGroups.length}', style: const TextStyle(fontSize: 9)),
              child: const Icon(Icons.folder_outlined, size: 16),
            ),
            tooltip: 'Tab groups (PRD 2.2.3)',
            onPressed: () => _showTabGroupsDialog(context),
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 30, minHeight: 32),
          ),
          // Empty draggable space between the controls above and the
          // window controls -- without this, the minimize/maximize buttons
          // sat right next to them instead of staying pinned to the far
          // right edge.
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

  /// PRD 2.2.2 "Vertical Tabs" -- left sidebar rendering of every open
  /// tab (plus the workspace switcher, new-tab, and tab-groups controls
  /// that normally live in the horizontal strip), for traders who prefer
  /// seeing more tab titles at once over the classic Chrome-style top
  /// strip. Collapsible via the toolbar toggle; the choice is persisted
  /// (see [_toggleVerticalTabs]).
  Widget _buildVerticalTabBar() {
    return Container(
      width: 240,
      color: const Color(0xFF1E1E1E),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            height: 32,
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: Row(
              children: [
                Expanded(child: _buildWorkspaceSwitcher()),
                IconButton(
                  icon: const Icon(Icons.add, size: 16),
                  tooltip: 'New tab (Ctrl+T)',
                  onPressed: () => _openNewTab(),
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
                ),
                IconButton(
                  icon: Badge(
                    isLabelVisible: _tabGroups.isNotEmpty,
                    label: Text('${_tabGroups.length}', style: const TextStyle(fontSize: 9)),
                    child: const Icon(Icons.folder_outlined, size: 14),
                  ),
                  tooltip: 'Tab groups (PRD 2.2.3)',
                  onPressed: () => _showTabGroupsDialog(context),
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(minWidth: 28, minHeight: 28),
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: Color(0xFF2D2D30)),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(vertical: 4),
              itemCount: _tabs.length,
              itemBuilder: (context, index) => _buildVerticalTabRow(index),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildVerticalTabRow(int index) {
    final t = _tabs[index];
    final isActive = index == _activeIndex;
    final group = _groupFor(t.groupId);
    return GestureDetector(
      onTap: () => _activateTab(index),
      onSecondaryTapDown: (details) => _showTabContextMenu(details.globalPosition, index),
      onLongPress: () => _showTabContextMenu(null, index),
      child: Container(
        height: 32,
        margin: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
        padding: const EdgeInsets.symmetric(horizontal: 8),
        decoration: BoxDecoration(
          color: t.isIncognito
              ? (isActive ? const Color(0xFF2B2440) : Colors.transparent)
              : (isActive ? const Color(0xFF37373D) : Colors.transparent),
          borderRadius: BorderRadius.circular(6),
          border: group != null ? Border(left: BorderSide(color: group.color, width: 3)) : null,
        ),
        child: Row(
          children: [
            Icon(
              t.isIncognito
                  ? Icons.visibility_off_outlined
                  : (t.isLoading ? Icons.autorenew : (t.isSecure ? Icons.lock_outline : Icons.public)),
              size: 13,
              color: t.isIncognito ? Colors.deepPurple[200] : (isActive ? Colors.grey[300] : Colors.grey[600]),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                t.title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontSize: 12, color: isActive ? Colors.white : Colors.grey[400]),
              ),
            ),
            InkWell(
              onTap: () => _closeTab(index),
              borderRadius: BorderRadius.circular(10),
              child: Icon(Icons.close, size: 13, color: Colors.grey[500]),
            ),
          ],
        ),
      ),
    );
  }

  /// A single tab's rendered chip in the tab strip -- extracted out of
  /// [_buildTabStrip] so grouped tabs can share the same look while also
  /// getting a colored top indicator and a right-click/long-press menu for
  /// group actions.
  Widget _buildTabChip(int index, double width, _TabGroup? group) {
    final t = _tabs[index];
    final isActive = index == _activeIndex;
    return GestureDetector(
      onTap: () => _activateTab(index),
      onSecondaryTapDown: (details) => _showTabContextMenu(details.globalPosition, index),
      onLongPress: () => _showTabContextMenu(null, index),
      child: Container(
        width: width,
        margin: const EdgeInsets.only(top: 6, right: 2),
        padding: const EdgeInsets.symmetric(horizontal: 10),
        decoration: BoxDecoration(
          color: t.isIncognito
              ? (isActive ? const Color(0xFF2B2440) : const Color(0xFF211D30))
              : (isActive ? const Color(0xFF252526) : Colors.transparent),
          borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
          border: isActive
              ? Border.all(color: t.isIncognito ? Colors.deepPurple[300]! : Colors.grey[800]!, width: 0.5)
              : null,
          // PRD 2.2.3 Tab Groups "color coding": a thin colored bar along
          // the top edge marks which group this tab belongs to.
          boxShadow: group != null
              ? [BoxShadow(color: group.color, offset: const Offset(0, -2), blurRadius: 0, spreadRadius: -1)]
              : null,
        ),
        child: Row(
          children: [
            if (group != null) ...[
              Container(width: 6, height: 6, decoration: BoxDecoration(color: group.color, shape: BoxShape.circle)),
              const SizedBox(width: 5),
            ],
            Icon(
              t.isIncognito
                  ? Icons.visibility_off_outlined
                  : (t.isLoading
                      ? Icons.autorenew
                      : (t.isSecure ? Icons.lock_outline : Icons.public)),
              size: 13,
              color: t.isIncognito
                  ? Colors.deepPurple[200]
                  : (isActive ? Colors.grey[300] : Colors.grey[600]),
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
  }

  /// A collapsed group's stand-in in the tab strip -- tap to expand,
  /// right-click/long-press for rename / close-all / ungroup.
  Widget _buildGroupPill(_TabGroup group) {
    final memberCount = _tabs.where((t) => t.groupId == group.id).length;
    return GestureDetector(
      onTap: () => _toggleGroupExpanded(group),
      onSecondaryTapDown: (details) => _showGroupContextMenu(details.globalPosition, group),
      onLongPress: () => _showGroupContextMenu(null, group),
      child: Container(
        width: 52,
        margin: const EdgeInsets.only(top: 6, right: 2),
        padding: const EdgeInsets.symmetric(horizontal: 8),
        decoration: BoxDecoration(
          color: group.color.withValues(alpha: 0.22),
          borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
          border: Border.all(color: group.color, width: 1),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.folder, size: 12, color: group.color),
            const SizedBox(width: 4),
            Text('$memberCount', style: TextStyle(fontSize: 11.5, color: group.color, fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }

  Future<void> _showGroupContextMenu(Offset? globalPosition, _TabGroup group) async {
    final RenderBox overlay = Overlay.of(context).context.findRenderObject() as RenderBox;
    final position = globalPosition ?? overlay.localToGlobal(Offset.zero);
    final selected = await showMenu<String>(
      context: context,
      position: RelativeRect.fromRect(position & const Size(1, 1), Offset.zero & overlay.size),
      color: const Color(0xFF2D2D30),
      items: [
        PopupMenuItem(
          value: 'toggle',
          height: 32,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Text(group.isExpanded ? 'Collapse group' : 'Expand group', style: const TextStyle(fontSize: 12.5)),
        ),
        const PopupMenuItem(value: 'rename', height: 32, padding: EdgeInsets.symmetric(horizontal: 12), child: Text('Rename group', style: TextStyle(fontSize: 12.5))),
        const PopupMenuItem(value: 'close_all', height: 32, padding: EdgeInsets.symmetric(horizontal: 12), child: Text('Close tabs in group', style: TextStyle(fontSize: 12.5))),
        const PopupMenuItem(value: 'ungroup', height: 32, padding: EdgeInsets.symmetric(horizontal: 12), child: Text('Ungroup', style: TextStyle(fontSize: 12.5))),
      ],
    );
    switch (selected) {
      case 'toggle':
        _toggleGroupExpanded(group);
        break;
      case 'rename':
        await _renameGroup(group);
        break;
      case 'close_all':
        _closeGroupTabs(group);
        break;
      case 'ungroup':
        _ungroupAll(group);
        break;
    }
  }

  /// PRD 2.2.5 Workspace switcher -- sits at the start of the tab strip,
  /// showing the active workspace's icon/name and letting the user switch,
  /// rename, delete, or create workspaces without leaving the tab strip.
  Widget _buildWorkspaceSwitcher() {
    if (_workspaces.isEmpty) return const SizedBox(width: 4);
    final active = _activeWorkspace;
    return PopupMenuButton<String>(
      tooltip: 'Workspaces (PRD 2.2.5)',
      color: const Color(0xFF2D2D30),
      constraints: const BoxConstraints(minWidth: 220, maxWidth: 260),
      onSelected: (value) {
        if (value == '__new__') {
          _createWorkspace();
        } else if (value.startsWith('rename_')) {
          final id = value.substring('rename_'.length);
          final w = _workspaces.firstWhere((w) => w.id == id, orElse: () => active);
          _renameWorkspace(w);
        } else if (value.startsWith('delete_')) {
          final id = value.substring('delete_'.length);
          final w = _workspaces.firstWhere((w) => w.id == id, orElse: () => active);
          _deleteWorkspace(w);
        } else {
          _switchWorkspace(value);
        }
      },
      itemBuilder: (context) => [
        for (final w in _workspaces)
          PopupMenuItem(
            value: w.id,
            height: 34,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: [
                Text(w.icon, style: const TextStyle(fontSize: 13)),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    w.name,
                    style: TextStyle(fontSize: 12.5, fontWeight: w.id == _activeWorkspaceId ? FontWeight.bold : FontWeight.normal),
                  ),
                ),
                if (w.id == _activeWorkspaceId) const Icon(Icons.check, size: 14, color: Colors.blueAccent),
                InkWell(
                  onTap: () => Navigator.pop(context, 'rename_${w.id}'),
                  child: const Padding(padding: EdgeInsets.all(2), child: Icon(Icons.edit_outlined, size: 13, color: Colors.grey)),
                ),
                InkWell(
                  onTap: () => Navigator.pop(context, 'delete_${w.id}'),
                  child: const Padding(padding: EdgeInsets.all(2), child: Icon(Icons.delete_outline, size: 13, color: Colors.grey)),
                ),
              ],
            ),
          ),
        const PopupMenuDivider(height: 8),
        const PopupMenuItem(
          value: '__new__',
          height: 32,
          padding: EdgeInsets.symmetric(horizontal: 12),
          child: Row(children: [Icon(Icons.add, size: 14), SizedBox(width: 8), Text('New workspace', style: TextStyle(fontSize: 12.5))]),
        ),
      ],
      child: Container(
        height: 32,
        constraints: const BoxConstraints(maxWidth: 130),
        padding: const EdgeInsets.symmetric(horizontal: 8),
        alignment: Alignment.center,
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(active.icon, style: const TextStyle(fontSize: 13)),
            const SizedBox(width: 5),
            Flexible(child: Text(active.name, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: Colors.white))),
            const SizedBox(width: 2),
            Icon(Icons.arrow_drop_down, size: 16, color: Colors.grey[500]),
          ],
        ),
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
          IconButton(
            icon: const Icon(Icons.photo_camera_outlined, size: 17),
            tooltip: 'Screenshot page (PRD 2.2.16)',
            onPressed: tab.started ? () => _captureScreenshot(tab) : null,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 30, minHeight: 30),
          ),
          IconButton(
            icon: Icon(
              Icons.view_sidebar_outlined,
              size: 17,
              color: _verticalTabs ? Colors.blueAccent : null,
            ),
            tooltip: _verticalTabs ? 'Switch to top tab strip' : 'Vertical tabs (PRD 2.2.2)',
            onPressed: _toggleVerticalTabs,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 30, minHeight: 30),
          ),
          IconButton(
            icon: Icon(
              _splitAxis == Axis.horizontal ? Icons.vertical_split : Icons.horizontal_split,
              size: 17,
              color: _splitViewEnabled ? Colors.blueAccent : null,
            ),
            tooltip: _splitViewEnabled ? 'Exit split view' : 'Split view (PRD 2.2.4)',
            onPressed: _toggleSplitView,
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(minWidth: 30, minHeight: 30),
          ),
          if (_splitViewEnabled)
            IconButton(
              icon: const Icon(Icons.swap_horiz, size: 17),
              tooltip: 'Toggle split direction',
              onPressed: _toggleSplitAxis,
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
                  _openNewTab(isIncognito: true);
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
                case 'passwords':
                  _showPasswordVaultDialog(context);
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
              const PopupMenuItem(height: 30, padding: EdgeInsets.symmetric(horizontal: 12), value: 'passwords', child: _MenuRow(label: 'Passwords and autofill')),
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
                    HistoryManager.clear();
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
                            HistoryManager.saveAll(_history);
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
      builder: (ctx) => ValueListenableBuilder<List<DownloadRecord>>(
        valueListenable: _downloadManager.records,
        builder: (context, downloads, _) => AlertDialog(
          backgroundColor: const Color(0xFF2D2D30),
          title: const Text('Downloads', style: TextStyle(fontSize: 15)),
          content: SizedBox(
            width: 380,
            height: 340,
            child: downloads.isEmpty
                ? Center(
                    child: Text('No downloads yet.', style: TextStyle(color: Colors.grey[500], fontSize: 13)),
                  )
                : ListView.builder(
                    itemCount: downloads.length,
                    itemBuilder: (context, index) {
                      final d = downloads[index];
                      final progress = d.totalBytes > 0 ? d.downloadedBytes / d.totalBytes : null;
                      return ListTile(
                        dense: true,
                        leading: Icon(
                          d.status == DownloadStatus.completed
                              ? Icons.check_circle_outline
                              : d.status == DownloadStatus.failed
                                  ? Icons.error_outline
                                  : Icons.downloading_outlined,
                          size: 18,
                          color: d.status == DownloadStatus.completed
                              ? Colors.greenAccent
                              : d.status == DownloadStatus.failed
                                  ? Colors.redAccent
                                  : Colors.grey[400],
                        ),
                        title: Text(d.filename, style: const TextStyle(fontSize: 13), maxLines: 1, overflow: TextOverflow.ellipsis),
                        subtitle: d.status == DownloadStatus.inProgress
                            ? LinearProgressIndicator(value: progress, minHeight: 3)
                            : Text(
                                d.status == DownloadStatus.completed ? (d.path ?? 'Saved') : 'Failed',
                                style: TextStyle(fontSize: 10.5, color: Colors.grey[600]),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                        trailing: IconButton(
                          icon: const Icon(Icons.refresh, size: 15),
                          tooltip: 'Download again',
                          onPressed: () => _downloadManager.start(d.url, suggestedFilename: d.filename),
                        ),
                      );
                    },
                  ),
          ),
          actions: [
            if (downloads.isNotEmpty)
              TextButton(
                onPressed: () => _downloadManager.clearAll(),
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

  /// PRD 2.2.12 "Password Manager" -- view, add, reveal, and delete saved
  /// logins from [PasswordVault]. Manual (no auto-detect on form submit
  /// yet -- see the note on [PasswordVault] itself), but a real, working,
  /// encrypted-at-rest vault rather than a placeholder screen.
  void _showPasswordVaultDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) {
          return FutureBuilder<List<SavedCredential>>(
            future: PasswordVault.loadAll(),
            builder: (context, snapshot) {
              final items = snapshot.data ?? const <SavedCredential>[];
              final revealed = <String>{};
              return StatefulBuilder(
                builder: (context, setInnerState) => AlertDialog(
                  backgroundColor: const Color(0xFF2D2D30),
                  title: Row(
                    children: [
                      const Expanded(child: Text('Passwords and autofill', style: TextStyle(fontSize: 15))),
                      IconButton(
                        icon: const Icon(Icons.add, size: 18),
                        tooltip: 'Add saved login',
                        onPressed: () async {
                          final added = await _showAddCredentialDialog(context);
                          if (added) setDialogState(() {});
                        },
                      ),
                    ],
                  ),
                  content: SizedBox(
                    width: 380,
                    height: 340,
                    child: !snapshot.hasData
                        ? const Center(child: CircularProgressIndicator())
                        : items.isEmpty
                            ? Center(
                                child: Text(
                                  'No saved logins yet. Tap "+" to add one.',
                                  style: TextStyle(color: Colors.grey[500], fontSize: 13),
                                ),
                              )
                            : ListView.builder(
                                itemCount: items.length,
                                itemBuilder: (context, index) {
                                  final c = items[index];
                                  final isRevealed = revealed.contains(c.id);
                                  return ListTile(
                                    dense: true,
                                    leading: const Icon(Icons.lock_outline, size: 18, color: Colors.grey),
                                    title: Text(c.site, style: const TextStyle(fontSize: 13), maxLines: 1, overflow: TextOverflow.ellipsis),
                                    subtitle: Text(
                                      '${c.username} · ${isRevealed ? c.password : '•' * c.password.length.clamp(6, 12)}',
                                      style: TextStyle(fontSize: 11, color: Colors.grey[500]),
                                    ),
                                    trailing: Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        IconButton(
                                          icon: Icon(isRevealed ? Icons.visibility_off : Icons.visibility, size: 16),
                                          tooltip: isRevealed ? 'Hide password' : 'Reveal password',
                                          onPressed: () => setInnerState(() {
                                            if (isRevealed) {
                                              revealed.remove(c.id);
                                            } else {
                                              revealed.add(c.id);
                                            }
                                          }),
                                        ),
                                        IconButton(
                                          icon: const Icon(Icons.copy, size: 16),
                                          tooltip: 'Copy password',
                                          onPressed: () async {
                                            await Clipboard.setData(ClipboardData(text: c.password));
                                            if (context.mounted) {
                                              ScaffoldMessenger.of(context).showSnackBar(
                                                const SnackBar(content: Text('Password copied.'), duration: Duration(seconds: 2)),
                                              );
                                            }
                                          },
                                        ),
                                        IconButton(
                                          icon: const Icon(Icons.delete_outline, size: 16),
                                          tooltip: 'Delete',
                                          onPressed: () async {
                                            await PasswordVault.remove(c.id);
                                            setDialogState(() {});
                                          },
                                        ),
                                      ],
                                    ),
                                  );
                                },
                              ),
                  ),
                  actions: [
                    TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Close')),
                  ],
                ),
              );
            },
          );
        },
      ),
    );
  }

  Future<bool> _showAddCredentialDialog(BuildContext context) async {
    final siteController = TextEditingController(text: _activeTab.started ? (Uri.tryParse(_activeTab.url)?.host ?? '') : '');
    final userController = TextEditingController();
    final passController = TextEditingController();
    final saved = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D30),
        title: const Text('Add saved login', style: TextStyle(fontSize: 15)),
        content: SizedBox(
          width: 320,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: siteController, decoration: const InputDecoration(labelText: 'Site')),
              TextField(controller: userController, decoration: const InputDecoration(labelText: 'Username / email')),
              TextField(controller: passController, obscureText: true, decoration: const InputDecoration(labelText: 'Password')),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
          TextButton(
            onPressed: () async {
              if (siteController.text.trim().isEmpty || passController.text.isEmpty) return;
              await PasswordVault.add(SavedCredential(
                id: 'cred_${DateTime.now().millisecondsSinceEpoch}',
                site: siteController.text.trim(),
                username: userController.text.trim(),
                password: passController.text,
                savedAt: DateTime.now(),
              ));
              if (ctx.mounted) Navigator.pop(ctx, true);
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
    return saved ?? false;
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
        // PRD 2.2.11 / 3.3.1 Incognito Mode: no persistent cookies/local
        // storage/cache for this tab's session (Android/iOS support this
        // natively; other platforms fall back gracefully to a normal
        // session since flutter_inappwebview doesn't expose true profile
        // isolation there yet -- history recording is still skipped below
        // regardless of platform, which is the privacy guarantee that
        // matters most).
        incognito: tab.isIncognito,
        cacheEnabled: !tab.isIncognito,
        contentBlockers: _kTrackerBlockList,
      ),
      initialUserScripts: UnmodifiableListView([
        UserScript(
          source: kFingerprintProtectionScript,
          injectionTime: UserScriptInjectionTime.AT_DOCUMENT_START,
        ),
      ]),
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
      // Real download capture -- routed through DownloadManager so bytes
      // actually land on disk (desktop/mobile) or the browser's own
      // download pipeline (web), with live progress in the Downloads
      // dialog, instead of the old placeholder that just reopened the URL
      // in a new tab and called that a "download".
      onDownloadStartRequest: (controller, request) async {
        final filename = request.suggestedFilename ??
            (request.url.pathSegments.isNotEmpty ? request.url.pathSegments.last : 'download');
        _downloadManager.start(request.url.toString(), suggestedFilename: filename);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Downloading $filename\u2026'), duration: const Duration(seconds: 2)),
          );
        }
      },
      // PRD 3.2.6 / CONSTITUTION.md "Permission Manager" -- camera/mic/
      // location access must be explicit per-site, never a blanket allow.
      // Checks PermissionManager for a remembered decision first; only
      // prompts when this exact (origin, resource) pair has never been
      // decided, then remembers the answer either way.
      onPermissionRequest: (controller, request) async {
        final origin = request.origin.toString();
        final resourceLabels = request.resources.map((r) => r.toString()).toList();
        final resourceKey = resourceLabels.join(',');

        final remembered = await PermissionManager.decisionFor(origin, resourceKey);
        if (remembered == PermissionDecision.allow) {
          return PermissionResponse(resources: request.resources, action: PermissionResponseAction.GRANT);
        }
        if (remembered == PermissionDecision.block) {
          return PermissionResponse(resources: request.resources, action: PermissionResponseAction.DENY);
        }

        if (!mounted) {
          return PermissionResponse(resources: request.resources, action: PermissionResponseAction.DENY);
        }
        var remember = true;
        final allowed = await showDialog<bool>(
          context: context,
          builder: (ctx) => StatefulBuilder(
            builder: (ctx, setDialogState) => AlertDialog(
              backgroundColor: const Color(0xFF2D2D30),
              title: const Row(
                children: [
                  Icon(Icons.privacy_tip_outlined, color: Colors.amberAccent, size: 20),
                  SizedBox(width: 8),
                  Text('Permission request', style: TextStyle(fontSize: 15)),
                ],
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('"$origin" wants to use:', style: const TextStyle(fontSize: 13)),
                  const SizedBox(height: 6),
                  Text('• $resourceKey', style: TextStyle(fontSize: 12.5, color: Colors.grey[300])),
                  const SizedBox(height: 12),
                  StatefulBuilder(
                    builder: (ctx, setInner) => CheckboxListTile(
                      value: remember,
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      controlAffinity: ListTileControlAffinity.leading,
                      title: const Text('Remember for this site', style: TextStyle(fontSize: 12.5)),
                      onChanged: (v) => setInner(() => remember = v ?? true),
                    ),
                  ),
                ],
              ),
              actions: [
                TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Block')),
                TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Allow')),
              ],
            ),
          ),
        );

        final decision = allowed == true ? PermissionDecision.allow : PermissionDecision.block;
        if (remember) {
          await PermissionManager.remember(origin, resourceKey, decision);
        }
        return PermissionResponse(
          resources: request.resources,
          action: allowed == true ? PermissionResponseAction.GRANT : PermissionResponseAction.DENY,
        );
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
          // Record real browsing history — skip entirely for incognito tabs
          // (PRD 3.3.1), and skip if this exact URL is still the most
          // recent entry (e.g. a reload) to avoid spamming dupes. Persisted
          // immediately via HistoryManager so it survives an app restart.
          if (!tab.isIncognito && tab.url.isNotEmpty && (_history.isEmpty || _history.first.url != tab.url)) {
            _history.insert(0, HistoryEntry(url: tab.url, title: tab.title, visitedAt: DateTime.now()));
            HistoryManager.saveAll(_history);
          }
        });
        _persistSession();
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

  /// PRD 2.2.4 "Split View" -- two resizable panes, each independently
  /// showing one of the currently open tabs, so a chart and a news feed
  /// (for example) can be watched side-by-side.
  Widget _buildSplitView() {
    // multi_split_view 3.x drives its layout from a controller + a
    // per-area builder rather than a fixed `children` list -- `area.data`
    // carries which pane slot (0 or 1) this area represents.
    return MultiSplitView(
      axis: _splitAxis,
      resizable: true,
      controller: _splitController,
      dividerBuilder: (axis, index, resizable, dragging, highlighted, themeData) {
        return Container(
          color: dragging || highlighted ? Colors.blueAccent.withValues(alpha: 0.6) : const Color(0xFF1E1E1E),
          child: Center(
            child: Icon(
              axis == Axis.horizontal ? Icons.drag_indicator : Icons.drag_handle,
              size: 14,
              color: Colors.grey[600],
            ),
          ),
        );
      },
      builder: (context, area) => _buildSplitPane(area.data as int),
    );
  }

  Widget _buildSplitPane(int pane) {
    final tabIndex = _splitPaneTabIndex[pane].clamp(0, _tabs.length - 1);
    final tab = _tabs[tabIndex];
    return Column(
      children: [
        Container(
          height: 30,
          color: const Color(0xFF232324),
          padding: const EdgeInsets.symmetric(horizontal: 6),
          child: Row(
            children: [
              Icon(tab.isSecure ? Icons.lock_outline : Icons.public, size: 12, color: Colors.grey[500]),
              const SizedBox(width: 6),
              Expanded(
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<int>(
                    value: tabIndex,
                    isDense: true,
                    isExpanded: true,
                    dropdownColor: const Color(0xFF2D2D30),
                    style: const TextStyle(fontSize: 12, color: Colors.white),
                    items: [
                      for (var i = 0; i < _tabs.length; i++)
                        DropdownMenuItem(value: i, child: Text(_tabs[i].title, overflow: TextOverflow.ellipsis)),
                    ],
                    onChanged: (i) {
                      if (i != null) _setSplitPane(pane, i);
                    },
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.close, size: 14),
                tooltip: 'Close this pane',
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(minWidth: 24, minHeight: 24),
                onPressed: () => _closeSplitPane(pane),
              ),
            ],
          ),
        ),
        Expanded(child: _buildTabBody(tab)),
      ],
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
          const Divider(height: 24, color: Color(0xFF1E1E1E)),
          ListTile(
            dense: true,
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.cookie_outlined, size: 18, color: Colors.grey),
            title: const Text('Cookies and site data', style: TextStyle(fontSize: 13)),
            subtitle: const Text('Clears cookies for every tab in this app', style: TextStyle(fontSize: 11.5, color: Colors.grey)),
            trailing: TextButton(
              onPressed: () async {
                final navigator = Navigator.of(context);
                final messenger = ScaffoldMessenger.of(context);
                await CookieManager.instance().deleteAllCookies();
                if (!mounted) return;
                messenger.showSnackBar(
                  const SnackBar(content: Text('All cookies cleared.'), duration: Duration(seconds: 2)),
                );
                navigator.pop();
              },
              child: const Text('Clear cookies'),
            ),
          ),
          const Divider(height: 24, color: Color(0xFF1E1E1E)),
          ListTile(
            dense: true,
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.restart_alt, size: 18, color: Colors.grey),
            title: const Text('Reset browsing data', style: TextStyle(fontSize: 13)),
            subtitle: const Text(
              'Clears saved tabs, workspaces, and history -- next launch starts fresh with a blank New Tab',
              style: TextStyle(fontSize: 11.5, color: Colors.grey),
            ),
            trailing: TextButton(
              onPressed: () async {
                final navigator = Navigator.of(context);
                final messenger = ScaffoldMessenger.of(context);
                final confirmed = await showDialog<bool>(
                  context: context,
                  builder: (ctx) => AlertDialog(
                    backgroundColor: const Color(0xFF2D2D30),
                    title: const Text('Reset browsing data?', style: TextStyle(fontSize: 15)),
                    content: const Text(
                      'This clears every saved workspace, tab, and browsing session. Bookmarks, passwords, and downloads are kept. Restart TradePilot afterward to see a blank New Tab.',
                      style: TextStyle(fontSize: 13),
                    ),
                    actions: [
                      TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancel')),
                      TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Reset')),
                    ],
                  ),
                );
                if (confirmed != true) return;
                await ws.WorkspaceManager.clearAll();
                await SessionManager.clearSession();
                await HistoryManager.clear();
                if (!mounted) return;
                messenger.showSnackBar(
                  const SnackBar(content: Text('Browsing data reset. Restart TradePilot to see a fresh start.'), duration: Duration(seconds: 3)),
                );
                navigator.pop();
              },
              child: const Text('Reset'),
            ),
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
