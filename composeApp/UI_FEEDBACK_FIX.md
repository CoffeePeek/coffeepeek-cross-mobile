# UI feedback corrections — 2026-11-01

Baseline: main `803c0970`, branch `codex/migration-ui-feedback`.

Scope: remove the Home city shortcut (Settings remains the owner); stabilize iOS
filter measurement; restore catalog contribution cards; reuse map glyphs for
venue filters; unify photo rating/favorite plates and use bright gold likes;
make phone-copy and check-in overflow triggers plain icons.

Preserve existing navigation, authentication, favorite request/loading guards,
analytics, filter state, native menu actions and 48dp touch targets. No feature
migration, data changes or dependency additions are planned.

Verification plan: affected unit/contract suites, design-system iOS compilation,
Android directSandbox and directDebug assembly including packaged DEX checks.
Manually review fake Sandbox data in light/dark, normal/large text and compact
width; open/cancel contribution forms, filters and menus without live writes.
Physical iOS rendering, scrolling and Dynamic Type remain device gates; Windows
cannot establish these by Kotlin compilation alone. Record actual results below.


## Implementation and verification result

- Home no longer owns a city-settings callback or a city chip; the existing
  Settings route and city-based queries are retained.
- Native controls are configured before their first measurement. The installed
  Compose 1.12.1 `UIKitInteropRemeasureRequester` requests a Compose layout pass
  after UIKit updates, including asynchronously rasterized filter icons. This
  preserves intrinsic sizing and viewport/touch clipping rather than fixing widths.
  See [JetBrains' API contract](https://github.com/JetBrains/compose-multiplatform-core/blob/jb-main/compose/ui/ui/src/iosMain/kotlin/androidx/compose/ui/viewinterop/UIKitInteropRemeasureRequester.ios.kt).
- Venue chips reuse byte-identical map PNG glyphs, exposed from design-system;
  native chips rasterize both vector and bitmap painters through the same path.
- Shop empty/search results use the existing illustrated contribution card;
  the full roaster list also retains it after successful nonempty results.
- Photo favorites and ratings share the same 68% black plate. Floating navigation
  controls retain glass. Selected hearts use brand gold; light plain surfaces
  get a contrasting heart outline without darkening the fill.
- Copy-phone and check-in overflow triggers use the shared plain 48dp icon
  control. Menus, callbacks, accessibility labels and authentication remain owned
  by their existing callers. No analytics ownership or event changes.

2026-11-01, Windows, local working tree based on the baseline above:

- `:core:design-system:compileKotlinIosArm64` passed, including the installed
  remeasurement API. This is Kotlin/Native compilation, not a complete iOS app build.
- Executed 364 unit tests: design-system 54, shop 108, discovery 139, roaster 23,
  community 40; zero failures/errors/skips. Existing palette contracts now verify
  bright-gold fill and contrasting outlines on light surfaces.
- `assembleDirectSandbox`, `assembleDirectDebug`, Sandbox isolation and both
  packaged DEX runtime-class checks passed. All Gradle runs used `--max-workers=6`.
- Two earlier build attempts failed on duplicate imports/declarations introduced
  during the edits. Those failures remain in local logs; the corrected third run
  passed. Existing Preview/KMP deprecation warnings remain; no IDE inspection was
  available through the tool inventory.
- Manual Android review: `CoffeePeek_API_37`, `emulator-5554`,
  `sdk_gphone64_x86_64`, unlocked. Updated Sandbox in place from 2622 to 2630 after
  checking matching signatures. No uninstall, data clearing or live Debug launch.
- Checked light/dark Home, map glyphs and scrolling tags, matching photo plates,
  bright selected hearts, filter opening/back, restored shop/roaster contribution
  actions and opening/cancelling both forms. City settings still open from Settings.
- Checked plain overflow triggers and menu opening/back in the feed; menu icons
  remain present. Checked Home with app-only 320x740dp bounds and font scale 1.3
  in both themes. Restored ordinary dimensions, font scale and light theme.
- The stock fake shop has no phone contact, so phone-copy appearance is supported
  by the shared component/call-site review and colocated previews, not a claimed
  device interaction. Physical iOS chips/scrolling/Dynamic Type, native menus and
  copy-phone still need a device pass. No automated UI tests or live-account
  writes were used; form opening does not certify backend persistence.

Verified APK: `com.coffeepeek.sandbox`, version `1.1.4.2630-sandbox`,
launcher `com.coffeepeek.admin.sandbox.SandboxActivity`, no INTERNET permission.
SHA-256: `92D817E4E6C33881756A59D6FDA8A5CDD2549A31AD5D2433EF7394F9D041063C`.
Screenshots, UI dumps and logs remain ignored under `build/ui-feedback-validation/`.


## Add action list — 2026-11-01

Follow-up scope: replace the global plus picker with direct icon/label actions:
«Чекин в кофейне», «Чекин дома», a logical separator, «Пачку на полку».
Remove radio selection and Continue; retain the existing native iOS sheet host,
Android modal host, session gates, handoff callbacks and analytics. The history
picker uses the same direct rows for its two existing check-in destinations;
contextual shop/shelf add buttons keep their existing destinations.

Reuse the design-system menu row in a grouped action list, without adding a
second plate around each item. Labels remain in the existing Russian resources;
light/dark previews render without production dependencies. Grouping follows
[Apple HIG menus](https://developer.apple.com/design/human-interface-guidelines/menus).

Verification plan: existing global flow/session/analytics unit contracts,
check-in history and design-system unit suites, iOS Kotlin compilation for the
affected shared libraries, directSandbox/directDebug assemblies and packaged DEX
checks. Manually open/cancel all global actions in Sandbox, inspect both themes
and larger font/narrow bounds. Physical iOS interaction remains an untested gate.

Verification result (2026-11-01, working tree on `70036f89`):

- 131 executed unit tests passed: design-system 54, check-in-history 54, global
  flow 16, global flow lifecycle/store 4, creation analytics 3. No failures,
  errors or skips. No automated UI/instrumentation tests were added or run.
- Design-system and check-in-history implementation `compileKotlinIosArm64`
  passed; this is library compilation, not an iOS app/device run.
- `assembleDirectSandbox`, `assembleDirectDebug`, Sandbox isolation and both
  packaged DEX runtime-class checks passed with `--max-workers=6`.
- Setup failures retained locally: sandboxed Gradle could not access its wrapper
  cache, and the first unrestricted command selected a nonexistent
  `testDirectSandboxUnitTest`. The corrected `testDirectDebugUnitTest` command
  passed. Existing Preview/dependency warnings remain; no Studio MCP available.
- Manual device: `CoffeePeek_API_37`, `emulator-5554`,
  `sdk_gphone64_x86_64`, unlocked. Checked APK package, launcher and matching
  signing certificate before updating Sandbox in place from 2630 to 2631.
- Verified all three global labels/icons, ordering and separator in light/dark;
  each row opens its existing destination immediately. Opened/cancelled shop
  selection, Home form and shelf scan introduction without saving, downloading
  a model, taking photos or accessing a live backend. Also inspected the
  two-action history variant and dismissal. Returned to Home, normal app bounds,
  normal font and light theme.
- Attempted Sandbox-only 320x740dp/font 1.3 review. Those overrides constrain the
  app content but do not propagate into the separate modal window, so this does
  not certify narrow-modal/large-font behavior. Large-font device review and
  physical iOS/native sheet review remain untested; previews are provided.

APK: `com.coffeepeek.sandbox`, `1.1.4.2631-sandbox`, launcher
`com.coffeepeek.admin.sandbox.SandboxActivity`, no INTERNET permission.
SHA-256: `FDC1FD553B1B558E1B1537CB6C8DB16DD4D2060E837365FB356FAD8B301D5B8E`.
Logs/screenshots/dumps are local and ignored in `build/plus-menu-validation/`.
