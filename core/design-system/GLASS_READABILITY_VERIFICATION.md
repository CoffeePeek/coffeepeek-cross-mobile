# Android glass readability and original loader

2026-10-31. Base: develop `83e0ab6b0ba768bc75149729aa74cdd14337f57b`.
Branch: `codex/migration-glass-readability-loader`.

User requests: restore the original irregular loader and stop text behind Android
glass menus from competing with foreground text. Keep brand colors and native iOS
controls. These are presentation fixes, with no business, data, navigation or
analytics behavior changes.

## Scope and verification plan

- Restore the existing five loader contours and 1600/2800/5000 ms spin/fill/morph
  timing in the shared design system. Android platform loader and retained legacy
  Compose loader reuse it. Keep the migrated iOS UIKit indicator. Forward caller
  stroke width and preserve indeterminate accessibility semantics.
- Supply screen content to Android popup menus. Reuse the existing tab backdrop;
  full-screen public profile provides its own. A separate menu-only local prevents
  in-window controls from recursively sampling their parent. Native iOS menus keep
  their UIKit presentation. Use dense, theme-colored fallback when no blur source
  exists or Haze disables blur, preserving explicit control tint colors.
- No new dependencies/modules. Existing Haze 1.7.1 supports dialog sources and
  `HazeStyle.fallbackTint`; no library upgrade is included.
- Run design-system and profile unit tests, assemble Direct Sandbox/Debug including
  packaged DEX checks, and compile the shared iOS design-system KLIB.
- Manual review only in Direct Sandbox on emulator-5554: profile menu light/dark,
  dismissal by Back/outside tap, and loader during Slow requests. Check surrounding
  Home controls after the fallback change. No real account writes or automated UI
  tests. Native iOS visuals, TalkBack, large fonts/windows and performance on a
  physical device remain separate gates. Studio MCP is unavailable this session.

Implementation evidence: popup menus explicitly passed `hazeState = null` while
the fallback used only the normal translucent tint. The screenshot's underlying
profile text therefore remained sharp and visible through menu items.
