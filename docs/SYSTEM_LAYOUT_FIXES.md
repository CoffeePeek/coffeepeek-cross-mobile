# System layout and iOS startup fixes

## User correction — 2026-10-22

The first implementation did not establish device acceptance. The user reports
that system panels still overlap the shop photo and lower content. The photo's
existing crop/hero size must stay unchanged. Horizontal shop/roaster carousels
and filter tags must scroll to the screen edges; retain initial/final content
padding inside their scroll containers rather than narrowing the viewport.

Follow-up scope and checks:

- Reserve top/bottom at the outer SwiftUI iOS boundary, keeping horizontal
  edges open, and avoid duplicate/zero nested UIKit safe-area assumptions.
  Verify Android's Compose root reserves only the vertical system insets.
- Resolve iOS System appearance from the window scene, outside controller/window
  overrides; clear forced appearance on returning to System. Preserve saved
  theme settings and test Light/Dark/System resolution.
- Add a review link to settings for App Store and Play builds only. The user
  supplied App Store ID `6821851267`. Direct APK builds expose no review action;
  use application-owned URLs and the existing narrow external-actions port.
- Set Android's shared minSdk to 31 (Android 12, revised by the user from 14), the Xcode application target
  to iOS 18 and the xtool application package to iOS 18. Dependency libraries may
  retain lower supported targets; the application defines install eligibility.
- Compile Direct/Play/Sandbox APKs and Kotlin iOS; run theme, review-link/settings,
  existing map geometry and minimum-version/configuration contracts. Verify
  merged/packaged manifests. No automated UI/instrumented tests or app installs.

These follow-up results will be recorded below; the earlier results describe
the first implementation, not device confirmation of the reported issues.

Scope: application composition (`composeApp`), the two iOS launchers and their
resources. Preserve navigation, local back actions, analytics, account data and
Android's fixed-size map animation. No feature/module migration is required.

Verification plan (2026-10-22):

- Reserve Android system bars/cutouts once around Compose navigation; let UIKit
  bound each iOS screen to its safe area and consume those same insets in Compose.
- Render iOS headers through the existing Compose bar, as Add Shop does; preserve
  native edge-back restrictions for local editor/detail state.
- Keep the discovery map mounted during scrolling, derive its position from the
  lazy list and bound the iOS native renderer to its visible viewport.
- Reuse the Android splash artwork for light/dark iOS launch and first-frame
  splash screens, in both the Xcode and xtool launchers.
- Lock launcher activities and both iOS manifests to upright portrait.
  The current Android target is API 36; its large-display compatibility opt-out
  preserves this contract there too. When upgrading to target API 37, Android
  removes that opt-out for displays wider than 600dp; desktop/user/OEM overrides
  can also supersede application orientation restrictions. See
  [Android orientation rules](https://developer.android.com/develop/adaptive-apps/guides/app-orientation-aspect-ratio-resizability).
- Run map geometry unit tests, iOS configuration/resource contract tests and
  Android Sandbox/Debug assembly, including packaged DEX runtime-class checks.

Remaining device gates: manual light/dark checks at normal/large text sizes;
notch/status bar and home/gesture/three-button navigation clearance; map scroll
to bottom/back, expand/collapse, search/cancel, tab changes and edge-back; local
editor back; cold/warm startup and rotation. No automated UI tests. No live data
writes. This Windows host cannot build Xcode or run iPhone UI checks.

Xcode compiles the light/dark launch asset catalog. The xtool preparation copies
the same raw images; its first-frame splash uses the system background when no
compiled named-color catalog is bundled. The native launch screen follows
[Apple's UILaunchScreen configuration](https://developer.apple.com/documentation/bundleresources/information-property-list/uilaunchscreen).

Verification results (2026-10-22):

- Source: `codex/migration-system-insets-ios`, based on `4742a16d`, plus this
  uncommitted working diff. Android variants: `directDebug` (`com.coffeepeek`)
  and `directSandbox` (`com.coffeepeek.sandbox`). Both assembled; packaged DEX
  runtime-class checks passed and Sandbox isolation passed. Merged manifests
  confirm portrait launchers and the API 36 compatibility property.
- `:composeApp:testDirectDebugUnitTest --tests
  com.coffeepeek.admin.ui.screen.feed.FeedFiltersUiTest`: 8 executed, 0 failures,
  0 errors, 0 skipped. Includes preview scroll/recycling/return geometry plus
  expansion and list-transition regressions. These are unit tests, not UI tests.
- `python -m unittest discover -s tools/ios -p 'test_*.py'`: 5 executed, all
  passed (3 presentation/configuration contracts, 2 existing Firebase contracts).
- `:composeApp:compileKotlinIosArm64`: passed as a cross-compiled KLIB on
  Windows. The first attempt failed on missing UIKit category-extension imports;
  those imports were restored and compilation passed on retry. This does not
  compile Swift, link an Apple framework or verify Xcode's asset compiler.
- `git diff --check`: passed. IDE diagnostics were unavailable for this project.
  Existing duplicate Android namespace, deprecated UI APIs in legacy code and
  Kotlin expect/actual beta warnings remain outside this fix.
- Device/manual visual checks: not run; ADB initially could not start inside
  the sandbox, then the host ADB query succeeded with no connected devices.
  No app was installed, launched or cleared. No native iPhone/Simulator/Xcode
  environment was available. Light/dark previews were added beside the shared
  header and native splash, but were not rendered in an IDE.

The device gates above remain unverified; fake/unit checks do not certify live
backend, OAuth, camera, analytics delivery or native UI acceptance.

## Follow-up verification — 2026-10-22

Source remains `codex/migration-system-insets-ios` at `4742a16d` plus the working
diff. The final requested application minimums are **Android 12 / API 31** and
**iOS 18**. The earlier Android 14 request was superseded before the final build.

- `assembleDirectSandbox`, `assembleDirectDebug` and `assemblePlayDebug` passed
  with `--max-workers=6`. All three packaged DEX guards and Sandbox isolation
  passed. `aapt2 dump badging` on each finished APK confirms minSdk `31` and the
  intended package (`com.coffeepeek.sandbox` or `com.coffeepeek`). BuildConfig
  confirms Direct enables the APK channel and Play enables the store channel.
- Executed 33 JVM unit tests across six suites: Android settings ports 12, theme
  persistence 2, discovery/map geometry 8, review URLs/channel exclusion 2,
  settings ViewModel 6 and theme ViewModel 3. No failures, errors or skipped
  tests. The final API 31 rebuild repeated these checks successfully.
- Executed 6 Python configuration/resource contracts, all passed, including the
  Android/iOS minimums, upright portrait launchers and light/dark launch assets.
- `compileKotlinIosArm64` and `compileTestKotlinIosArm64` passed. The two new
  native appearance-mapping tests were **compiled, not executed**. Swift/Xcode,
  Apple framework linking, native launch asset compilation and iPhone runtime
  appearance changes remain unverified on this Windows host.
- The first follow-up compile failed after a ColumnScope header was wrapped in
  a Box; it now retains a ColumnScope. The next attempt found a new string whose
  accessor had been generated before that resource edit; generation and
  compilation passed once rerun against the final sources. These were build
  failures, not failing/retried test scenarios. Diagnostic logs remain local.
- Shop hero crop/height and callbacks are preserved. The full-screen photo
  dialog now reserves its own system bars because dialog windows do not inherit
  the page's inset consumption. Shop/roaster carousels and filter rows keep
  their initial/final padding inside full-width scrolling viewports. Roaster
  card dimensions retain the original content-width calculation.
- The review action opens App Store ID `6821851267` with `action=write-review`,
  or CoffeePeek's Google Play page. Direct APK ports expose no review row and
  opening that action directly is a no-op. New labels use the existing default
  resource locale. Existing analytics actions and account writes are unchanged.
- `git diff --check` passed. Existing build/deprecation warnings remain; no
  dependency or module graph changes were introduced. No automated UI tests,
  app installation, native device run, live account action or publication ran.

Manual acceptance still needs the final builds on devices: iOS Light/Dark →
System for both OS appearances; shop photo and bottom actions under system
panels; photo viewer; carousel/tag scrolling to either edge; large text, tab
transitions, back/cancel and light/dark startup. New previews were not rendered
in the unavailable IDE. Kotlin/JVM and configuration checks do not certify
these visuals or the App Store/Play applications launching on a device.

## Liquid Glass tab underlay — 2026-10-22

Scope/verification plan: preserve the safe top edge, detail/photo screens,
navigation and analytics; extend only native tab roots under the bottom bar.
Compile the iOS app/test KLIBs, assemble Android Direct Debug with its DEX guard
and run the existing map geometry and native tab routing unit contracts.
Manual iPhone checks must cover light/dark scrolling behind the tab bar, the
last item above the bar, check-in/map controls, expanded map, keyboard and back.

The previous layout ended the Compose child at the bottom safe-area guide and
disabled extended layout. Consequently, the native tab bar sampled the solid
controller background. Tab roots now extend to the bottom edge and provide the
measured UIKit bottom safe inset as `LocalFloatingNavClearance`. Lists keep
their padding inside the scrollable content; floating controls clear the bar.
Both SwiftUI launchers let UIKit own the bottom system region. Top constraints
and pushed detail screens still use the safe-area guide. This follows Apple's
[tab-bar underlay guidance](https://developer.apple.com/documentation/uikit/uitabbar/istranslucent).

The map now consumes the host's actual clearance. The Android host provides
zero while its bottom bar is hidden for the expanded map; the native iOS host
provides the measured clearance while its bar remains visible. No bar metrics,
custom material, dependency, navigation path or account behavior was added.

Verification: `4742a16d` plus this working diff, Windows, 2026-10-22.
`compileKotlinIosArm64` and `compileTestKotlinIosArm64` passed (KLIBs only;
native tests were not executed). Android `assembleDirectDebug` passed its
packaged DEX runtime-class check. Executed 13 JVM contracts: map geometry 8
and native tab routing 5, with zero failures/errors/skips. All 6 existing Python
configuration/resource checks passed. `git diff --check` passed. The first
compile failed on a missing `dp` import; the import was added and the complete
verification command passed on the next run. No test failures were retried.

No device was used or application installed. Swift/Xcode compilation, Apple
framework linking and the manual Liquid Glass checks above remain unverified
on this host. IDE rendering/diagnostics were unavailable for this project;
the change reuses existing screen content/previews and adds no new component.
Existing preview deprecation and expect/actual warnings remain outside scope.

## Apple sign-in artwork — 2026-10-22

Scope/verification plan: replace the shared Apple button artwork used by login
and registration, retain the 18 dp icon frame, theme/disabled tint, labels and
authentication callbacks. Compile Kotlin iOS and assemble Android Direct Debug
with the packaged DEX guard. No authentication attempt or account write is needed.

The Phosphor fill icon's own path has a flat bottom. The replacement uses the
original logo-only path from the official
[Sign in with Apple artwork](https://developer.apple.com/design/resources/),
downloaded from `Logo-Sign-in-with-Apple.dmg`. The resource keeps that contour
unchanged, centers it by translation and leaves space inside all viewport edges.
The supplied white button rectangle is not part of the icon. Material Icon tint
continues to follow the button's enabled state and light/dark content color.
Paired multiplatform previews sit beside the button and use no auth provider/DI.

An assert-based check compared the resource path against the original SVG and
confirmed the translated contour fits the viewport. Device acceptance still
requires login/registration checks on iPhone in light/dark mode and at large
text sizes; the previews have not been rendered on this Windows host.

Verification at `4742a16d` plus this working diff: `compileKotlinIosArm64` and
`assembleDirectDebug` passed, including the packaged DEX runtime-class guard
(1m 17s, no failed attempts). `git diff --check` passed. No unit test was added
for this artwork-only substitution; authentication logic is unchanged. No UI
test, sign-in attempt, device install or publication ran. Swift/Xcode linking
and visual confirmation on iPhone remain unverified.

## Shop photo at the status bar and solid bottom panel — 2026-10-22

The user clarified the screenshot requirement: the shop hero must continue
behind the clock/system icons. This supersedes the earlier top-bounded shop
photo layout. Horizontal scrolling/insets and Liquid Glass tab underlays stay.

Scope/verification plan: cover both the migrated Android detail entry and the
legacy iOS detail, including the shop-picker variant. Keep navigation/actions,
photo crop mode, labels and analytics. Assemble Direct Sandbox/Debug with DEX
guards, compile iOS app/test KLIBs, run map/tab routing and shop detail ViewModel
unit contracts. No UI automation, live data writes or installation is involved.

Android root navigation now leaves the top inset available on shop-detail
routes, while the retained main screen keeps its own vertical safe area. The
root paints system regions and clips content above the bottom system area.
Shop scaffolds reserve only bottom insets; top buttons still apply status-bar
padding. Hero height includes the status-bar height in addition to its previous
300 dp, preserving the visible photo space. Android requests light status icons
over the photo and retains theme-based navigation icons.

SwiftUI now lets UIKit manage all container edges. Native shop detail controllers
extend only at the top, keep their bottom safe-area boundary, and request light
status icons. Other native screens retain their safe-area layout. Both detail
implementations clip their lists and paint the action panel with the theme's
opaque background so menu photos cannot show through its gaps or below it.

Manual gates: photo under the clock/notch, readable system icons, safe top
controls, an opaque gesture region/footer while scrolling, large text, gallery,
back transitions and the shop picker. Native tab Liquid Glass and the retained
map also need a regression check on devices. Existing feature light/dark
previews remain beside the hero/footer/screen; previews cannot certify system
insets and have not been rendered in an IDE on this host.

Verification: `4742a16d` plus the working diff, Windows, 2026-10-22. The complete
command passed in 3m 48s with no failed attempts: `assembleDirectSandbox`,
`assembleDirectDebug`, both packaged DEX guards and Sandbox isolation;
`compileKotlinIosArm64` and `compileTestKotlinIosArm64` (KLIBs only). Executed
48 JVM tests with zero failures/errors/skips: shop detail ViewModel 35,
native tab routing 5 and discovery/map geometry 8. No native test was executed.
`git diff --check` passed. The screenshot and diagnostics remain outside tracked
sources. No app was installed, no device/IDE visual check or Xcode build ran,
and no changes were committed, pushed or published. The device gates above
remain required for these local changes.

## Grouped legal links — 2026-10-22

Scope: combine Terms and Privacy into one outlined card with a divider in both
the migrated Android settings page and legacy iOS page. Each full-width row
keeps its own button semantics and original document callback/URL. The analytics
consent card, labels and consent persistence remain unchanged. Its inner padding
was moved to the row when the legacy card's padding moved to its link rows.

Verification plan: compile Kotlin iOS, assemble Android Direct Debug with the
DEX guard and run the existing UsagePolicyViewModel unit contracts. Android's
existing paired screen previews reflect the grouping; new light/dark common
previews show the iOS group with injected no-op callbacks and no production DI.
No automated UI test was added for this layout-only change. Device checks still
need the combined outline/divider, large text, independent link taps and back.

Verification at `4742a16d` plus the working diff: all requested tasks passed in
1m 17s, including the Android packaged DEX guard and Kotlin iOS KLIB compilation.
The existing UsagePolicyViewModel suite executed 3 tests with zero failures,
errors or skips. `git diff --check` passed. No app was installed, no consent or
account setting was changed, and no live legal link was opened. Native Xcode
linking, IDE rendering and device visual/accessibility checks remain unverified.

## Profile header and personal activity week — 2026-10-22

Scope: own Profile tab on Android and iOS, including cached accounts without a
public slug. Statistics move beside the avatar; only check-ins, shops and
followers remain. The existing public-profile header shares this layout. Bio
moves below the avatar row. Backend roaster counts and contribution actions stay
intact. Own activity moves above Favorites into a compact Monday–Sunday preview
with check-in photos, active-day dots and an additional-photo count. Tapping it
opens the existing full calendar (or personal history for the no-slug fallback).

The existing application-owned personal calendar slot still joins shop/home
reads. A narrow additional preview slot keeps these private reads out of public
profiles and migrated presentation. Separate account/week keyed model instances
read a whole week directly, including adjacent months. Full-month navigation,
photo pagination, error/retry and calendar photo URL selection stay in the
existing history/home readers. No new module/dependency, writers or analytics
service; existing favorites events and author-visibility guards are preserved.

Verification plan: execute profile unit contracts, existing calendar/photo/home
regressions plus week date-range and DST/year-boundary checks; assemble Direct
Sandbox and Direct Debug with packaged DEX checks; compile iOS Kotlin KLIB. Pure
light/dark previews accompany the header and weekly content. No UI automation
or device launch; remaining gates are native iOS linking and manual Sandbox/iOS
checks for photos, week boundaries, large text, back and modal placement.

Verification at `4742a16d` plus the working diff: first check passed in 1m56s;
final verification after splitting the weekly adapter into its own file passed
in 1m35s. No failed build/test retries. All 100 JVM tests executed successfully
(Profile impl 48, application profile/calendar 52), with zero failures, errors
or skips. New regressions cover complete home-photo pagination across the year
boundary and local week bounds across DST/month changes. Existing date/photo
merging, profile and monthly calendar tests still pass.

Both `assembleDirectSandbox` (`com.coffeepeek.sandbox`) and `assembleDirectDebug`
(`com.coffeepeek`) pass packaged DEX runtime-class verification; Sandbox isolation
also passes. App iOS device KLIB and test KLIB compile; no native test executable,
Swift/Xcode link or device visual review was run. XML parses with unique default
resource names, and `git diff --check` passes. Existing unrelated preview,
BackHandler, KMP/interop and test-source warnings remain. No app was installed,
no real account/data/consent was changed, and no analytics events were emitted.
Preview rendering and manual light/dark, large-font, VoiceOver/TalkBack and
calendar modal checks remain device gates.

## iOS update version parsing — 2026-10-22

Scope: application platform configuration only. TestFlight CI writes
`CFBundleVersion = GITHUB_RUN_NUMBER.GITHUB_RUN_ATTEMPT` (for example `48.1`).
The old `toLongOrNull()` returned null, preventing automatic update-policy reads
and making the manual check fail. Parse the validated CI run component as the
numeric update-policy code (`48.1` and `48.2` both map to `48`). Whole-number
local/older build values remain supported; malformed or overflowing run numbers
still produce null. Upload attempts do not create a separate public update
version. Neither marketing versions nor Android version codes are changed.

Server contract: iOS `versionCode` and `minVersionCode` use those integer run
numbers. A read-only unauthenticated request to the configured public endpoint
`https://api.coffeepeek.by/api/v1/app-version?platform=ios` returned HTTP 404 with
`App version is not configured for this platform.` No policy was created or
changed: a release policy must point to a build actually published in App Store,
not merely uploaded to TestFlight, to avoid an unavailable mandatory update.

Verification plan: parser cases for retries, integer compatibility, malformed
input and Long overflow; update-state regressions proving automatic/manual policy
reads and required/optional/current-version behavior from a dotted iOS build.
Run the existing update unit contracts, assemble Android Debug with packaged DEX
checks, and compile iOS device/simulator Kotlin and iOS test sources. No app
installation, release upload or automated UI test. Native execution and real
App Store policy/install verification remain device/server gates.

Verification at `4742a16d` plus the working diff: all 12 JVM update tests pass
(parser 3, update state 7, response/policy 2), with zero failures, errors or
skips. Android `assembleDirectDebug` and packaged DEX runtime-class validation
pass. Device and simulator iOS KLIB targets pass, and iOS test KLIB compiles;
no Native test executable or Swift/Xcode application was run. `git diff --check`
passes. There are no new dependencies, public API changes or UI changes.

The first combined platform build failed after 2m27s because Kotlin's Android
compilation could not delete its temporary `kotlin-classes/directDebug/com`
directory. A subsequent invocation stopped at task selection because PowerShell
split an unquoted dotted `-P` argument. Both original logs remain outside tracked
sources. The Android rerun passed in 3m01s with the correctly quoted, command-only
`kotlin.compiler.execution.strategy=in-process`; no persistent Gradle setting or
application source change was needed for that retry. The separate iOS check
passed in 15s. No test assertion failed during these attempts. Existing platform,
preview and unrelated source warnings remain.

No application was installed or uploaded and the public iOS update policy was
not modified. An App Store release plus its correctly configured server policy
are still required to verify actual update prompts on an iPhone.

## iOS shop bottom safe-area seam — 2026-10-22

The follow-up screenshot shows the hero correctly under the status bar, but a
hard horizontal cutoff through the bottom action shadows above the home
indicator. The shop's Compose child was still anchored to UIKit's bottom safe
area and clipped there; the full-height native parent painted the remaining
strip. The parent also marked the bottom inset as already consumed.

Scope: iOS NativeScreenController for ShopDetail only. Extend that controller
and its Compose child through the physical bottom edge as well as the top;
leave system insets unconsumed for the shop so its existing navigationBarsPadding
keeps buttons in the same safe position. The action bar background and shadows
can now render continuously to the bottom of the screen. Keep tab-root behavior,
other destinations, the header/hero, scroll clipping and opaque footer unchanged.
No UI/data/DI/API change, new dependency, added automated UI test or screenshot
fixture. This is a reversible bounds fix, so there is no new source-matching
layout test.

Verification plan: compile iOS device/simulator and test KLIBs; execute existing
NativeTabRoutes unit regressions; check Android Direct Debug assembly and its DEX
validation. Manual iPhone checks still need the bottom shadow/home-indicator
continuity in light/dark themes, unchanged action hit targets, scrolling, and
push/back/tab transitions. Windows KLIB compilation cannot certify rendered
UIKit bounds or native Swift linking.

Verification at `4742a16d` plus the working diff passed on the first attempt in
46s. Android Direct Debug assembly and packaged DEX checks pass. All 5 existing
NativeTabRoutes JVM tests pass with zero failures, errors or skips. iOS device
and simulator KLIBs and iOS test KLIB compile successfully. No Native tests,
Swift/Xcode linking or device visual checks were run, and no application was
installed. Existing interop/KMP/preview warnings remain. `git diff --check`
passes; the follow-up changes only NativeScreenController and this record.
The screenshot's visual result still needs confirmation on an iPhone.

## Coffee weight selector overlaps search — 2026-10-22

The iOS screenshot shows an otherwise clipped card with its native weight
selector floating over the catalogue search field. Traced both catalogue entry
points: Android uses feature/coffee/impl; iOS uses the legacy common screen.
The native segment bridge is also used by contributions and personal history.
Inspected the installed Compose 1.12.1 source: placedAsOverlay puts UIKit views
above the Metal canvas and disables clipping of their interop wrapper. Thus
adding zIndex or changing only the header would not fix the native draw order.

Use ordinary UIKit interop placement for NativeSegmentedControl, preserving its
native appearance, selection, haptic, touch arbitration and accessibility. The
bridge now follows Compose draw order and clipping for every embedded segment.
Other native glass/menu overlays and the tab bar keep their existing placement.
Clip the coffee refresh viewport after Scaffold/IME padding in both catalogue
renderers, outside the translated scrolling layer, so card content cannot cross
into the header while scrolling or refreshing. Preserve price/weight state,
filter/search/pagination/navigation and the current analytics owners/events.
No data, dependency, resources, module, writing or account changes.

Verification plan: coffee impl unit contracts and application coffee catalogue/
filter analytics tests; Android Direct Debug assembly with packaged DEX checks;
iOS device/simulator and test KLIB compilation. Existing Android paired list
previews reflect viewport clipping; new colocated light/dark native segment
previews use only fake labels/state. They require a UIKit host to render and
were not used as device evidence. No automated UI/instrumented/screenshot test
is added for this reversible presentation fix. Manual iPhone gates: fling a
partially visible weight selector past search, pull/release refresh, select each
weight and verify price, search/filter interactions, keyboard, light/dark,
VoiceOver and contribution/history segments. No live account/data operation.

Initial verification: all 46 Coffee impl JVM tests pass (zero failures/errors/
skips). The application check failed in 59s while Gradle tried to hash an
already-missing generated AndroidCheckInHistorySession coroutine class in its
Kotlin output directory; no Kotlin source diagnostic or failed test assertion
was reported. The original log is retained outside tracked sources. A full
non-incremental rerun uses command-only Kotlin compiler flags; build scripts
and application code were not changed to work around that build-output error.

The native underlay now has an opaque Material surface fill matching its
Compose card, so the Metal cutout does not reveal the controller background
through transparent corners. Selection tint, labels, callbacks and the 44dp
native control height remain unchanged.

Final verification at `4742a16d` plus the working diff: Android Direct Debug
assembly and packaged DEX validation pass after the full compile (5m23s). The
compiler execution/incremental flags were command-only. All 51 relevant JVM
tests pass with zero failures/errors/skips: Coffee impl 46 and app catalogue/
filter analytics 5. Tests use MockEngine and in-memory analytics transport; no
production events or account operations. The separate iOS device/simulator and
test KLIB check passes in 2m10s, including the native previews. No Native test
executable or Swift/Xcode application was linked/run. `git diff --check` passes;
existing unrelated compiler/platform/preview warnings remain. No app was
installed, no UI test was created/run, and native preview rendering and manual
iPhone clipping/touch/VoiceOver checks remain unverified.

## Profile background and username alignment — 2026-10-22

The Android feature profile and the shared/iOS profile both paint a warm gold
gradient over the standard page background. Remove that overlay and retain the
existing Scaffold background token, matching the other pages in both themes.
Both own-profile and public-profile entry points use these wrappers; the
no-slug fallback already uses the standard background. Their shared headers
will align the first statistic's number and label to the username's leading
edge, without adding a fixed username offset. Preserve the remaining columns,
dividers, follower hit target, avatar, bio and weekly activity.

Verification plan: existing profile feature and application profile JVM tests,
Android Direct Debug assembly and packaged DEX checks, then iOS device/simulator
and test KLIB compilation. Existing colocated fake-state light/dark header
previews will use the actual page background. No new logic or automated UI
tests are needed for these modifier changes. Manual phone checks of theme
consistency, long names, larger font sizes and alignment remain a device gate.

Verification at `4742a16d` plus the working diff passed without retries. Android
Direct Debug assembly and packaged DEX runtime-class validation pass in 2m31s.
All 82 selected JVM tests pass with zero failures, errors or skips: profile impl
48 and application profile ViewModels 34, using fake/in-memory dependencies.
The separate iOS device/simulator and test KLIB compilation passes in 1m01s.
Compiler execution/incremental options were command-only; no build settings or
dependencies changed. `git diff --check` passes. Existing unrelated compiler
and preview warnings remain. No app was installed, Native tests executed or
Swift/Xcode application linked. Device rendering of the updated previews,
light/dark backgrounds, larger fonts and username alignment is unverified.

## Android bottom edge follow-up — 2026-10-22

The root Compose navigation still pads and clips the retained main page and
all pushed pages above the bottom system inset. This leaves an application
background strip regardless of individual screen/footer fixes. Move bottom
inset ownership out of that root viewport: keep its top protection, let the
shop hero retain its existing top edge-to-edge behavior, and let each page's
Scaffold/controls handle its own bottom inset. Tab clearance must include the
system navigation inset, including the expanded map when the tab bar is hidden.
Avoid counting it again in the own-profile scroll content.

For the Android shop page, extend the scrolling viewport to the physical bottom
and include the system inset in scrollable end padding; keep its opaque footer
and inset-aware buttons. Disable Android's three-button navigation contrast
scrim in the existing system-bar owner, retaining theme-aware icon colors.
This matches Android's documented transparent system-bar configuration:
https://developer.android.com/develop/ui/compose/system/system-bars
No business logic, analytics, navigation routes, data, dependencies or API changes.

Verification plan: existing root/tab navigation regressions, shop detail and
profile unit contracts; Android Direct Debug and Direct Sandbox assemblies,
packaged DEX checks and Sandbox isolation; iOS device/simulator KLIB compilation
for the shared navigation source. No automated UI/instrumented/screenshot tests.
Manual Sandbox gates are gesture/three-button navigation in both themes, tab
and map expansion, full bottom scroll, shop actions and push/back transitions.
IDE diagnostics are unavailable because the configured IDE has a different
project open. No live account operations or Release test run is authorized.

Verification at `4742a16d` plus the working diff passed on the first build
attempt (1m33s): Direct Debug and Direct Sandbox APKs, both packaged DEX checks
and Sandbox isolation. All 93 JVM tests pass with zero failures/errors/skips:
shop detail ViewModel 35, profile impl 48, root/tab navigation and nearby
selection 10. Tests use fake/in-memory dependencies. The shared iOS device and
simulator KLIB check passes in 29s; no Swift/Xcode app or Native test executable
was linked or run. Existing preview/platform warnings remain and the changed
system-bar function reports no new compiler warning. `git diff --check` passes.
Compiler options were command-only; no persistent Gradle configuration changed.

ADB initially could not start its daemon in the restricted shell. The permitted
read-only device inventory then succeeded and returned no connected devices.
Thus no APK was installed, no app was launched, no UI test was created/run and
the manual Sandbox gates above remain unverified. In particular, successful
assembly and unit tests do not establish the rendered bottom edge or system
button contrast on a physical Android phone. Native iOS bounds/configuration
and its legacy shop screen were not changed in this Android follow-up.

## All profile statistics align left — 2026-10-22

The user requests the same leading alignment for every statistic, rather than
only check-ins. Set both the number/label column alignment and label text
alignment to Start in the Android and shared/iOS headers. All own/public and
fallback profile callers reuse these headers. Column widths, dividers, values,
username alignment and follower click/accessibility stay unchanged. Existing
colocated fake-state light/dark previews reflect the update. Verification plan:
profile impl unit tests, Direct Debug assembly with DEX validation, and iOS
device/simulator KLIB compilation. No new test is needed for constant alignment
modifiers; manual rendering remains unverified without a connected phone.

Verification at `4742a16d` plus the working diff passes: Android Direct Debug
assembly and packaged DEX validation (1m17s), 48 profile impl JVM tests with no
failures/errors/skips, and iOS device/simulator KLIB compilation (24s). No build
retry, app installation, Native execution or UI test. Existing warnings remain;
`git diff --check` passes. Physical light/dark and larger-font rendering remain
unverified. The change removes four alignment conditions across two renderers.

## Remove profile statistic separators — 2026-10-22

Remove the two vertical rules between the three statistics from both Android
and shared/iOS profile headers, retaining equal-width columns and left-aligned
numbers/labels. Own/public/fallback callers and existing colocated light/dark
fake-state previews all reuse these headers. Remove the unused Android layout
imports; no logic, values, hit targets, accessibility or resources change.
Verification plan: profile impl unit tests, Android Direct Debug assembly/DEX
validation and iOS device/simulator KLIB compilation. No new test is needed for
removing decorative elements; device rendering remains a manual gate.

Verification at `4742a16d` plus the working diff passes without retries:
Direct Debug assembly and packaged DEX validation (56s), 48 profile impl JVM
tests with zero failures/errors/skips, and iOS device/simulator KLIB compilation
(22s). Existing warnings remain; `git diff --check` passes. No app installation,
UI test, native linking/execution or device visual check was performed.

## Shop footer glass actions — 2026-10-22

Remove the opaque full-width background behind the shop's route/check-in
actions. Reuse core/design-system's existing glass icon button and liquid-glass
modifier in the Android feature and shared/iOS footer, passing the screen's
existing Haze source to both controls. Do not create another blur source or
platform bridge. Keep the buttons above the system navigation inset while
allowing scrolling content underneath the transparent footer; extend the legacy
iOS scroll viewport through its bottom inset and add that inset to scrollable
end padding, matching the Android page. Preserve directions/check-in exposure
ownership, callbacks, disabled/loading states and the alternate shop-picker
actions. Extract the legacy footer into its own component file with resource
labels and colocated fake light/dark previews; Android already has those previews.

Verification plan: shop detail ViewModel, route URL and Android shop analytics
unit contracts, Android Direct Debug/Sandbox assemblies with DEX/isolation
checks, and iOS device/simulator/test KLIB compilation. No automated UI test is
added for this presentation change. Manual Sandbox/device gates: scroll photos
under both buttons, light/dark blur/contrast, safe tap targets, no-coordinate
route, loading/check-in double taps, shop-picker variant, accessibility and
push/back. No connected Android device was available at the preceding check;
Windows cannot certify rendered UIKit/Haze or native app linking.

Verification at `4742a16d` plus the working diff passes on the first attempt:
Android Direct Debug/Sandbox assemblies, both packaged DEX checks and Sandbox
isolation (1m51s). All 43 selected JVM tests pass with zero failures/errors/skips:
shop detail ViewModel 35 and application route/analytics 8, using fake/in-memory
dependencies without production events. iOS device/simulator and test KLIBs
compile successfully (29s). Existing platform/preview warnings remain; the new
footer compiles without a diagnostic. `git diff --check` passes. No dependency,
persistent compiler setting, native platform-control API or data contract changed.

This uses CoffeePeek's existing Compose/Haze glass material on both renderers.
New shared previews use a synthetic two-color backdrop, including normal,
shop-picker and loading states in light/dark. Android footer and full-screen
previews reuse the updated component. They were compiled, not rendered or used
as physical-device evidence. No app was installed, no automated UI test created
or run, no Native test executable/Swift app linked and no device visual/touch
review performed. Actual blur, scrolling behind the home indicator, contrast
over photos, text scaling and accessibility remain the manual gates above.

## Add-shop/add-roaster keyboard strip — 2026-10-22

The screenshots show a fixed blank band above the Android keyboard and a form
header that moves when focusing lower inputs. Both forms apply Scaffold's
padding without consuming it before PlatformScrollableColumn applies imePadding.
The roaster form additionally repeats navigationBarsPadding. Both also place
their vertical form margin outside the scrolling/IME-aware viewport.

Consume Scaffold padding once, remove the duplicate roaster navigation padding,
and move vertical edge spacing into scrollable content while preserving initial
form spacing. Reuse PlatformScrollableColumn unchanged; no new inset helper or
configuration API is needed. Set adjustResize on the main and Sandbox Android
activities so IME resizing is explicit. Keep field text/selection, validation,
step changes, draft submission, numeric-keyboard accessories and iOS insets intact.
Reference for padding consumption:
https://developer.android.com/develop/ui/compose/system/insets-ui

Verification plan: existing submission and text-input unit regressions, Android
Direct Debug/Sandbox assemblies with DEX/isolation checks, inspect both packaged
Activity IME modes, and iOS device/simulator/test KLIB compilation. No automated
UI/instrumented/screenshot test is added. The affected entry points remain the
existing runtime forms and their colocated field previews; the component API and
preview fixtures do not change. Manual Sandbox gates: each input/keyboard type,
scrolling to the bottom, opening/dismissing IME, fixed header, numeric Done,
back/cancel, both themes and larger fonts. Unit/compile checks cannot verify the
keyboard gap. No live-account write or device data reset is part of this task.

Verification at `4742a16d` plus the working diff passes without retries:
Direct Debug/Sandbox assemblies, both packaged DEX checks and Sandbox isolation
(1m36s). All 19 JVM regressions pass with zero failures/errors/skips: input
state/selection/composition 9, iOS input defaults 5, and submission/draft/retry
contracts 5. Tests use in-memory/fake repositories, not production writes.
The iOS device/simulator and test KLIB compilation passes (31s). Existing
warnings remain; `git diff --check` passes. No persistent build option changed.

Inspected each built APK's binary AndroidManifest.xml with Android SDK aapt2:
MainActivity in Direct Debug and SandboxActivity in Direct Sandbox both contain
windowSoftInputMode 0x00000010 (adjustResize). This setup follows:
https://developer.android.com/develop/ui/compose/system/setup-e2e
No APK was installed, app/IME manually opened, UI test created/run or Native
application linked/executed. The fixed header, absence of the screenshot's
strip, keyboard animations and focus visibility still require a physical-device
check; the Activity-wide resize mode also needs manual auth/edit-form review.

## Search empty results and roaster contribution — 2026-10-22

Remove the redundant plain no-results messages and the roaster Show All action
from the combined text-search results shown in the screenshot. Keep the ordinary
Discovery carousel's Show All navigation. Use the existing shop suggestion card
as the visual pattern for a roaster empty state with an Add Roaster button.
Extract its presentational shell to core/design-system instead of duplicating
the layout; the application binds the existing branded illustration, resource
text and creation destinations. The migrated roaster feature owns when an empty
result is rendered and receives a narrow composable content port through its
existing composition factory, alongside its image/error ports. No feature UI
imports app/legacy implementations or another feature. Both platforms use that
same migrated roaster entry. Cover the carousel/combined search and full roaster
list, including query/tag/favorites empty results; never replace loading/error
with a contribution prompt. Existing creation/session policy remains in place.

Verification plan: roaster list ViewModel contracts, app feed/filter/roaster
analytics and submission regressions; Direct Debug/Sandbox assemblies, packaged
DEX/isolation checks, and iOS device/simulator/test KLIB compilation. Colocated
fake light/dark previews cover the shared shell and both application bindings.
No automated UI/instrumented/screenshot tests or live-account writes. Manual
gates: zero matches, cleared search, empty catalog, selected tags/favorites,
load failure/retry, CTA opening/back, guest/authenticated creation, both themes,
large fonts and scrolling behind the floating navigation. The illustration and
new copy retain the currently supported Russian base resources.

Initial verification: Android Direct Debug/Sandbox, DEX/isolation checks and all
39 selected JVM tests pass (2m11s): roaster list ViewModel 10 and app contracts
29. The first iOS check fails in 9s because the new design-system component used
the AndroidX Preview import, unavailable in this module's Native dependency set.
Replace only that import with the existing module's multiplatform Preview; no
dependency change is needed. Original failure diagnostics remain outside tracked
sources. Repeat native compilation and Android verification after this repair.

Final verification at `4742a16d` plus the working diff passes: Direct Debug and
Direct Sandbox assemblies, packaged DEX runtime-class checks and Sandbox
isolation (1m44s). All 39 selected JVM tests pass with zero failures/errors/skips:
roaster list ViewModel 10 and application contracts 29. Tests use fake
repositories/in-memory analytics; no production events or data writes.
iOS device/simulator and test KLIB compilation passes (46s) after the Preview
import repair. The module's existing multiplatform Preview is deprecated but
supported; no dependency/build option was changed. `git diff --check` passes;
existing unrelated warnings remain.

No APK was installed and no Native application was linked or executed. Preview
source compilation does not confirm rendering. Card layout, CTA opening/back,
authentication handling and the manual visual gates above still need device
review; no automated UI/instrumented/screenshot tests were created or run.

## Shared profile overview and roaster SVG photos — 2026-10-22

Base: `a7ff5894`, branch `codex/migration-profile-layout-roaster-photos`.
The own/public entry points already share their header and overview screen on
each platform, but the public page still uses the old bottom activity card.
Reuse one stateless week component below the bio for both own and public pages,
retaining the owner-only private/home data sources, public-only check-in photos,
follow/block/share, followers, favorites, errors/retry and month-sheet navigation.
Keep the Android feature independent through its existing application UI slots;
update the iOS legacy binding without moving the Android-first feature.

Roaster catalog and card/original logo GETs succeed (HTTP 200, image/svg+xml).
Kamel determines the decoder from the resource's string representation; the
CustomUrl data-class string hides the extension, selecting a bitmap decoder.
Android additionally attempts to resize SVG bytes with BitmapFactory. Preserve
the URL string for format selection and pass SVG through unchanged to the
installed SVG decoder. Keep permission-scoped data equality and protected-photo
authorization/caches intact. Reach: shared URL type, Android fetcher, their unit
regressions; no backend/media write, new dependency or new loader.

Verification plan: fake profile/privacy/session regressions, week-date/count
mapping tests, SVG format/transport unit regressions, Android Debug/Sandbox APKs
with DEX/isolation checks, and iOS device/simulator/test KLIB compilation.
Colocated fake light/dark week previews; no automated UI/instrumented/screenshot
tests. Device rendering, follow/back/month switching, protected-image behavior,
large fonts and actual native iOS execution remain manual gates.

Results: first complete check passed in 5m55s; final verification after preserving
all Kamel-supported non-bitmap inputs (SVG/XML/GIF) passed in 2m34s. No failed
build/test retry was needed. The final result set has 757 JVM tests with zero
failures/errors/skips: application Direct Debug 678, Profile impl 48, Roaster
data 12 and Roaster impl 19. The unchanged roaster suites were up-to-date; the
application's seven added regressions executed (format/scope 2, Android raw
non-bitmap transport/error 2, public week timezone/month/count mapping 3).
No new dependency, source-set or server configuration change.

Both Direct Debug and Direct Sandbox APKs assembled; packaged DEX runtime-class
checks and Sandbox isolation passed. iOS device/simulator KLIBs and iOS test
sources compile; Native tests, executable linking and Swift/Xcode application
execution were not performed. Colocated light/dark previews compile without
production DI or image requests; no IDE rendering is claimed. Existing KMP,
preview and unrelated test warnings remain. `git diff --check` passes.

The public week keeps activity snapshots for its visible month(s) while the
full month sheet browses other periods, and uses only that author's loaded
public feed for thumbnails. Photos from not-yet-loaded public feed pages are
not fetched independently; days without a matching month snapshot use loaded
public check-ins as the count fallback. The owner's existing full personal
week loaders remain independent. Manual device review, native SVG rendering,
month boundaries/large public feeds, navigation and accessibility remain gates.
No device was installed/launched and no production data or analytics was written.
