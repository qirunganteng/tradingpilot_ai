// Web implementation: there is no native OS window to minimize/maximize
// from inside the page, so "maximize" toggles the browser tab's Fullscreen
// API instead — the closest web equivalent, and genuinely useful for a
// kiosk-style trading terminal running in a tab.
import 'dart:html' as html;

Future<void> toggleBrowserFullscreen() async {
  if (html.document.fullscreenElement != null) {
    html.document.exitFullscreen();
  } else {
    await html.document.documentElement?.requestFullscreen();
  }
}

bool get isBrowserFullscreen => html.document.fullscreenElement != null;
