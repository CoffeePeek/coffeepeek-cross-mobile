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

## Results

Implementation revision: `f637471f` (no code changes after verification).
On 2026-10-31, 48 design-system tests in 14 suites and 60 profile tests in six
suites executed with zero failures/errors/skips. Direct Sandbox and Direct Debug
assembly, both packaged DEX runtime-class checks, Sandbox isolation, and
`:core:design-system:compileKotlinIosArm64` passed. Existing preview deprecation
and expect/actual beta warnings remain; no dependency/source-set changes.
All 326 numeric tokens in the original contour data and animation constants
match the restored shared implementation. The initially overbroad text comparison
included the new KDoc and did not match; the data comparison confirms no contour
or timing change. Existing unit suites were used; no automated UI tests added.

Manual review: unlocked emulator-5554 / sdk_gphone64_x86_64, API 37, 1080x2400,
density 420, font scale 1.0. Verified package/entry/version and same signing cert,
then updated com.coffeepeek.sandbox in place from 2588 to 2591
(1.1.4.2591-sandbox / SandboxActivity). APK SHA-256:
`c1624918e7d4b32eeee1b5211b657066e92d48b0d71c4abfa0dd9371c94d60d5`.

- Reproduced sharp profile text bleeding through the old menu before installing.
  New own-profile menus blur the same content in light and dark themes; the
  underlying avatar's color remains diffuse while foreground labels stay sharp.
- Back and outside-tap dismissals passed. Checked menu over a feed photo and the
  separate public-profile destination as well; both show the captured blur.
  Sharing, blocking, logout and edit actions were not executed.
- Home pull-to-refresh in Slow mode shows the original irregular contour in both
  themes, preserves content during refresh and removes the loader on completion.
  Home search/tags/map controls and bottom navigation were visually reviewed.
- Restored Ready/Seeded, light theme and Home, with no remaining loader and the
  original fake favorite still selected. No system settings or real data changed.

Screenshots/UI dumps and build log remain local in ignored design-system build
output. Native iOS rendering/full app build, TalkBack, large fonts/windows,
forced-disabled blur and physical-device performance were not executed. The
no-source fallback was reviewed through Home controls; the disabled-blur path
uses Haze's explicit fallback tint. KLIB compilation is not an iOS device check.

Haze API evidence: [1.7.1 usage](https://github.com/chrisbanes/haze/blob/1.7.1/docs/usage.md)
and [fallback tint contract](https://github.com/chrisbanes/haze/blob/1.7.1/haze/src/commonMain/kotlin/dev/chrisbanes/haze/HazeStyle.kt).
