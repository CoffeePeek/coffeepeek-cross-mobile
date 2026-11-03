# iOS glass consistency follow-up — 2026-10-31

Scope: the supplied TestFlight screenshots show missing menu glyphs, a clipped
notification switch, solid quick filters, and inconsistent control/sheet materials.
The CI action update is retained separately in parent checkpoint `9570c6d3`.

## Verification plan and preserved behaviour

- Keep application colours, favorite state, menu actions, filter state, haptics,
  accessibility ownership, analytics and native sheet dismissal/height policies.
- Own platform rendering in `core/design-system`; application controls reuse it.
  Feature changes only connect existing actions to those controls.
- Compile the design-system iOS KLIB; execute its unit tests, including pure
  viewport geometry regressions; assemble Android Sandbox and Debug with packaged
  DEX checks. No automated UI or screenshot tests are added.
- Windows cannot verify an iPhone's glass rendering or compile the full native
  application with MapLibre. These remain device/macOS gates, not passed checks.

## Changes

- Native menu actions render the supplied vector icons instead of passing a null
  image. Custom trigger content is retained, and disabled feed menus retain their
  disabled state.
- Native switches use UIKit's fitting width instead of the old 51-point width.
  A decorative switch forwards touch ownership to its toggleable parent row.
- Glass icon buttons and segments render above Compose, so their native materials
  can sample the actual content. An ancestor-viewport mask clips partially scrolled
  controls; the native container also restricts hit testing to that viewport.
  Fully hidden controls cannot receive taps. Intrinsic sizing is forwarded to the
  actual UIKit control.
- Home quick filters reuse the design-system chip: native glass on iOS and the
  existing glass approximation on Android. Selected filters retain the primary
  colour, and unselected filter glyphs retain their accent colour.
- Sheet content is transparent above a native glass effect (regular blur on older
  iOS), replacing the opaque Compose surface. Content cards retain their existing
  content palette. The shop check-in overflow also uses the shared action menu.
- Following the additional request, replace the wide shop-detail check-in action
  and Android Home's inline creation button with the shared yellow circular `+`.
  iOS uses a prominent native glass button; Android reuses the existing glass FAB.
  Keep shop/global creation callbacks, auth, exposure analytics and loading guards.
  Home places it above the navigation bar and hides it on the expanded map/filters;
  shop selection's Continue/Choose another actions and form submission stay intact.

The placement requirement is documented by
[Compose Multiplatform](https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html#overlay-placement-for-interop-views).
The custom sheet material uses Apple's
[UIGlassEffect](https://developer.apple.com/documentation/uikit/uiglasseffect).
UIKit [hit testing](https://developer.apple.com/documentation/uikit/uiview/hittest(_:with:))
does not use the drawn content, so the viewport mask also needs a touch boundary.

## Results and device acceptance

- Verification date: 2026-10-31. Branch `codex/migration-ios-glass-consistency`,
  parent `9570c6d3` plus the UI changes in this checkpoint. These local checks
  preceded publication and do not establish TestFlight delivery.
- Initial iOS compile attempts failed on UIKit enum imports, the experimental
  Compose configuration, sheet factory/frame bindings and containment imports.
  These were corrected; the original failed runs remain local diagnostics.
- Passed `:core:design-system:compileKotlinIosArm64`, including the final native
  viewport hit-testing container and explicit NSData interop opt-in.
- Executed unit tests: design-system 54, shop impl 108, discovery impl 139
  (301 total, no failures or errors). The four new tests are pure viewport geometry
  regressions, not automated UI tests.
- Passed `:composeApp:assembleDirectSandbox` and `:composeApp:assembleDirectDebug`,
  packaged DEX runtime-class checks for both, and Sandbox isolation verification.
  Existing deprecated Preview/API and Gradle warnings remain outside this fix.
- Manually reviewed only `directSandbox` on unlocked `emulator-5554`, AVD
  `CoffeePeek_API_37`, model `sdk_gphone64_x86_64`. Updated in place with matching
  signing certificate; version `1.1.4.2622-sandbox` / code `2622`.
  APK SHA-256: `CC526F0F55771C3A415ED6577EEB720D16EE5433C1CB527ACDF516F7291D5F44`.
  This APK includes the final Android changes; later edits only affect iOS.
- Android manual checks passed: light/dark Home, selected Open filter and its gold
  glyph, compact 320 x 740 dp content at font scale 1.3, circular `+` above tabs,
  global creation choice, shop-specific creation form for Sandbox Cafe, modal
  cancellation, hiding `+` on the expanded map and restoring it after collapse.
  No check-in was submitted and no live backend was used. Default window/font
  size and the original light theme were restored after review.
- Immediate launch after APK install initially reached Android's package-update
  activity; the later explicit cold launch reached SandboxActivity successfully.
  One shop tap during modal dismissal was ignored; a tap after dismissal opened
  the shop. Neither initial attempt is counted as a successful check.
- Screenshots and sanitized UI dumps remain in ignored local build output.
  No Studio MCP was available. iPhone visuals/touches, full iOS application build,
  native accessibility and release acceptance remain unverified gates.
- On iPhone, check both themes, increased text size and Reduce Transparency;
  overflow icons/actions, filters, selected favorites, switch on/off and taps on
  its row; open/dismiss/resize the notification sheet; scroll photo controls and
  weight segments beneath headers and off the edges of horizontal carousels.
  Preview fixtures do not certify these native behaviours.
