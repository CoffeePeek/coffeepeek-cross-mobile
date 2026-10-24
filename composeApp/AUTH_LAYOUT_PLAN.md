# Authentication layout alignment

Date: 2026-10-23. Source baseline: `bcba3a3c` on
`codex/migration-public-check-in-default`, plus existing local work.

## Scope and preservation

- Owner: existing shared authentication presentation in `composeApp/commonMain`.
  This is a layout correction, not a feature/module/navigation migration.
- Entry points: root `Auth`, `Register`, `ForgotPassword` destinations in
  `Navigator`; registration retains Email/Details/Success, recovery retains
  Email/EmailSent. No route or ViewModel contract changes.
- Reuse existing Auth fields/buttons/scaffold and design-system theme/tokens.
  The design-system form rail is vertical and does not implement registration
  progress. Auth progress stays local to authentication.
- Keep the existing 40 dp card side insets. Constrain progress and social buttons
  to that same content width; show all progress labels with wrapping rather than
  truncation. Keep active/completed steps and accessible selection.
- Use 16 dp between form sections, 8 dp inside the title/description group.
  Remove extra field/chip/footer margins and the unused social divider.
- Recovery uses the same mascot, title style, email icon and top reserve as login.
- Runtime screens retain lifecycle ViewModel ownership; stateless content has
  paired fake-state multiplatform previews beside its implementation, including
  registration/recovery completion and narrow/large-text progress previews.
- Preserve validation, terms and optional analytics consent, busy/error rendering,
  password handling, keyboard dismissal, cancellation, and all navigation actions.

## Analytics inventory

Existing ViewModels own password/Apple/Google sign-in analytics, deferred password
sign-up identification, and explicit analytics consent. Registration captures the
optional choice before its existing register request. UI forwards the same
callbacks; no new analytics/exposure owner, service, consent policy or event is
introduced. Recovery has no UI-owned analytics. Sandbox emits no production data.

## Verification plan

1. Build `:composeApp:assembleDirectSandbox --max-workers=6`, including isolation
   and packaged DEX runtime class checks.
2. Run existing non-UI auth/password/recovery/registration-consent unit tests.
   No automated UI, Compose instrumentation, screenshot tests or test hosts.
3. On the existing unlocked Pixel_8 emulator, explicitly target its serial;
   check APK/package/version/signature before an in-place Sandbox update.
4. Manually review login, registration email/details/success and recovery in both
   themes, with keyboard, scrolling, back/cancel and validation. Capture originals
   outside the repository and compare content edges with the previous gallery.
5. Record results, source/APK identity, device, failures/retries and limitations.

Remaining gates: iOS compilation/device, OAuth/live backend/email delivery,
TalkBack, rendered IDE previews, and additional font/window sizes. New previews
do not count as device verification. Unrelated local platform-presentation work
and its diagnostics remain outside this change.

## Results

Verification completed on 2026-10-23.

- Verification source: baseline `bcba3a3c`, then migration tip `50c32fba` plus
  the authentication patch and pre-existing packaging/run-mode edits. Packaging
  edits are separate from the authentication change.
- `assembleDirectSandbox`, `assembleDirectDebug`, Sandbox isolation and both
  packaged DEX runtime-class checks passed in one Gradle invocation with
  `--max-workers=6` (1m 24s, 1121 tasks: 70 executed, 1051 up-to-date).
- Seven existing unit suites executed: **41 tests, 0 failures, 0 errors,
  0 skipped**. PasswordSignUpAnalytics 4, AppleLoginFlow 8, SocialLoginError 16,
  PasswordRecoveryRepository 3, PasswordRecoveryViewModel 4,
  RegisterAnalyticsConsent 3, PasswordValidation 3. No test retries.
- Retained initial failures in local logs: the Sandbox unit-test task does not
  exist (use DirectDebug unit tests); the next compile rejected the unsupported
  multiplatform Preview `fontScale` parameter. Fixed it using preview-local
  density; the final build/test run above passed. No production logic failure
  was found and no tests were weakened.
- Installed only `directSandbox` on unlocked `emulator-5554`, AVD `Pixel_8`,
  model `sdk_gphone16k_x86_64`; 1080×2400, 420 dpi, font scale 1.0.
  Package `com.coffeepeek.sandbox`, Activity
  `com.coffeepeek.admin.sandbox.SandboxActivity`, version
  `1.1.1.1232-sandbox` / 1232. The matching debug certificate allowed an
  in-place update of 1230. No uninstall, downgrade, data clearing, permission
  changes or real-account operations.
- APK SHA-256:
  `1D8ADB0EAAD8310687D983DFD935D84C7C5ADE73D94DAB01A1B48B97ADF02244`.
- Manual review: login, registration Email/Details/Success and recovery Email in
  both themes; 20 curated screenshots plus intermediate evidence, kept outside
  tracked sources in the chat's local `auth-after` artifact directory.
  Original before screenshots were preserved in `auth-review`.
- Registration progress occupies x=147…933 px, matching fields/buttons. Its
  three columns are 147…409, 409…671 and 671…933 px; all labels visible.
  Login/recovery headings now both begin at y=695 px (recovery was y=411).
  Recovery's long title wraps normally instead of being truncated.
- Verified local edit-email → continue preserves the typed name, local
  registration reaches Success, and empty recovery submission shows validation
  and hides the keyboard. Tested back from recovery and keyboard dismissal.
  With the keyboard open, login's CTA is below the initial viewport and is
  reachable by scrolling; a separate scrolled capture proves access.
  Long registration also requires scrolling. No fixed bottom CTA was introduced.
- Fake data only: `auth-review@example.invalid`, Sandbox, dummy password;
  optional analytics remained off. Original dark theme/local Sandbox session
  restored after capture.
- Paired previews compile, including narrow 208 dp content and 130% text
  previews for progress; their IDE rendering was not available. Configured IDE
  access does not expose this project. Existing unrelated Gradle/KMP/AGP and
  deprecation warnings remain. Changed functions fit review body limits; larger
  unchanged legacy background/social-button bodies were not rewritten.
- No automated UI/instrumentation/screenshot tests or test hosts were created
  or run. No Gradle/source-set/dependency/DI or business contract changes.
- Remaining: iOS/device compilation, OAuth/live backend/mail delivery, recovery
  EmailSent rendered on a device, TalkBack, extra window sizes and device font
  scales. Recovery EmailSent has fake-state previews only.
- The comparison HTML and original PNGs remain local, outside tracked sources.
  Manual Android observations are the visual verification evidence.

## Develop integration

On 2026-10-23 the auth-only patch was applied cleanly to `origin/develop`
(base `b889b73e`). Auth source/resources are identical to the Sandbox-reviewed
patch; unrelated local APK packaging edits are excluded.

- `assembleDirectDebug`, packaged DEX runtime-class checks and the same seven
  unit suites passed: 41 tests, zero failures/errors/skips, no test retries.
- Final integration invocation: `--max-workers=6`, 1m 52s, 1093 tasks:
  567 executed and 526 from cache. The unit test task executed.
- This integration APK was assembled only; device evidence remains the earlier
  Sandbox run documented above. No UI/device tests were added or run here.
- Existing multiplatform preview annotations compile with deprecation warnings;
  this remains a tooling warning, not a rendering verification.

## iOS presentation follow-up — 2026-10-24

Baseline: `35d71005` on develop; implementation branch
`codex/fix-ios-native-presentation`. Ownership remains existing authentication,
profile/photo and shop presentation plus UIKit platform adapters. No feature,
DI, navigation destination, dependency or business-rule migration is included.

The authentication host extends to the bottom edge and consumes only the top
system inset. Home-indicator clearance and vertical spacing now belong to the
scrolling form content, eliminating the fixed blank band below the viewport.
Registration/recovery reuse the same correction. Existing validation, keyboard
handling, navigation, analytics ownership and consent behavior remain intact.

UIKit `UIAlertController` now owns alerts and action sheets, including destructive
logout confirmation and camera/gallery selection. Shared action sequencing still
waits for native dismissal before opening another resource, ignores repeated taps
and preserves disabled actions. The existing four action-selection contracts cover
those semantics. Camera availability, current callbacks and iPad popover anchoring
remain supported. Native controller rendering requires manual iOS review;
Compose previews cannot render system alerts or exercise UIKit presentation.

Routes offer Apple, Google and Yandex Maps again using the existing URL builders;
the share sheet is removed. The location picker's header close button uses native
overlay placement, while map-viewport controls retain clipping-aware underlay
placement. See JetBrains' [interop placement explanation](https://youtrack.jetbrains.com/issue/CMP-8588):
underlays cut a rectangle through Compose, which exposed the black background
around the transparent close control. The marker uses SF Symbols `cup.and.saucer`
on iOS, rasterized into Compose above the native map; Android keeps its existing
coffee icon. Paired light/dark previews accompany the marker and platform icon.

Local verification of this implementation on 2026-10-24:

- `compileKotlinIosArm64`, `assembleDirectDebug`, packaged DEX runtime-class
  verification, six selected app unit suites and design-system unit tests passed
  in one invocation with `--max-workers=6` (53 seconds).
- 25 application tests plus 36 design-system tests: 61 passed, no failures,
  errors or skips. Tests executed; iOS KLIBs compiled, not linked/executed.
- The first combined invocation rejected the unsupported replacement Preview
  import. Restoring the repository's multiplatform annotation resolved it;
  original/final logs remain local. No test failures or weakened assertions.
- No automated UI/instrumentation/screenshot tests, real-account actions,
  installation or iPhone launch were performed by these local checks.
- Native `.ipa` linking/packaging, updated-device visuals in both themes, keyboard,
  scrolling, sheet cancel/reopen, map handoff and accessibility remain separate
  device gates. Build/source identity is retained with the resulting artifact.

### Native package and device update

[Device build 40083882771](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/40083882771)
passed in 18m30s using implementation commit `b2342f73`. Kotlin's strict iOS
framework link and the native Xcode Debug build succeeded. Local verification
confirmed the artifact's SHA-256, exact source revision, arm64 physical-device
Mach-O, version 1.1.1/build 2417, 63 Compose resource files (including the new
map chooser strings), launch assets and configured application resources.

With the user-confirmed connected/unlocked iPhone15,4 on iOS 27.0.1, xtool
successfully signed, installed and verified the in-place CoffeePeek Dev update
from 2416 to 2417. Installed application metadata independently confirms 2417;
the original TestFlight application remains at 1961. IPA files, hashes and
sanitized installation/verification reports stay in ignored build output.

Device startup and visual acceptance of the four reported issues are awaiting
the user's manual check. No account logout, photo capture/upload or live data
mutation was performed. Compiled previews and installation alone do not confirm
light/dark rendering, keyboard/scroll behavior, map-app handoff or accessibility.

## Authentication top safe area — 2026-10-24

Baseline: `9052226b` on `codex/fix-ios-native-presentation`. The user reported
missing top safe-area clearance on login after installing device build 2419.
This correction belongs to the existing UIKit screen host and shared auth
scaffold's existing inset handling; it introduces no new feature or abstraction.

Authentication now extends its Compose child to the controller's top edge and
leaves the top system inset available to Compose. `AuthScreenScaffold` already
uses `statusBarsPadding()` for its close button and scroll viewport, so it owns
top clearance explicitly instead of depending on a nested UIKit safe-area guide
while the same inset is marked consumed. Bottom clearance stays inside scroll
content. Registration/recovery reuse the same scaffold. Photo-header status-bar
styling, private-history layout, validation, navigation and analytics retain
their existing owners and callbacks.

Verification plan: compile iOS arm64 KLIBs, assemble Android DirectDebug including
the packaged DEX guard, and execute the existing auth-validation/ViewModel and
native-tab routing unit suites with `--max-workers=6`. Native packaging follows
the local checks. Existing paired auth previews compile but cannot reproduce the
UIKit host/status-area boundary; light/dark device review, scrolling, keyboard
and back/close remain manual gates. No automated UI or screenshot tests, account
logout or real authentication requests are required for this layout correction.

Local checks passed in one invocation (1m09s): iOS arm64 KLIB compilation,
Android DirectDebug assembly/DEX guard and 16 executed unit tests across the
three selected suites, with zero failures/errors/skips and no retries. No device
launch or visual acceptance is inferred from these local checks.

### Internal-screen bottom inset scope

Before packaging, the user clarified that build 2419 still has the lower band
on internal screens; main tabs are unaffected. The auth-only packaging run
`40087935217` was cancelled to include this correction in the same device update.

The application owns UIKit screen bounds; the design system owns Scaffold and
scroll-content inset distribution. Extend every internal native screen to its
bottom edge, expose the actual bottom clearance through one design-system
composition local, and let the shared Scaffold keep FAB/bottom-bar protection
while omitting an unused bottom inset from the scroll viewport. Move that
clearance into vertical list/form content. Main-tab composition and Android use
the default inactive policy. Existing photo/header and auth top-inset ownership
remain explicit. No Gradle module, DI, domain, navigation or product migration
is required. Compile both platforms, run inset-geometry and existing screen/DI
contracts, then package one native update and manually review internal lists,
forms, bottom actions, back/cancel, keyboard and both themes.

Local verification on 2026-10-24 passed with the final inset changes in one
Gradle invocation (1m07s, max-workers=6): iOS arm64 KLIB compilation, Android
DirectDebug assembly and packaged DEX runtime-class guard, 798 application unit
tests and 41 design-system unit tests. All 839 tests completed with zero
failures/errors/skips. Five new non-UI geometry cases cover inactive-host
compatibility, header/RTL padding, measured bottom-bar clearance, preservation
of last-item spacing and zero safe-area behavior. Earlier checks also passed;
the final rerun followed the Android viewport-compatibility adjustment.

Fixed bottom actions keep their measured height plus the native home-indicator
clearance; scrolling lists/forms own that clearance inside their content. The
existing shop-photo and other-profile scroll padding is retained rather than
duplicated. No Gradle dependencies, feature contracts, DI or analytics ownership
changed. Paired design-system previews are colocated. No automated UI,
instrumentation or screenshot tests ran. Native framework linking, device
installation and manual visual acceptance remain separate gates.

### Combined iPhone update 2423

Native device workflow
[40089562002](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/40089562002)
built source `8cc5436d4eda85a773ad4cc0554e69cd0de36a3c` successfully in
21m42s. The configuration suite executed 11 tests successfully; physical-iPhone
arm64 Kotlin framework linking, Swift compilation and Debug packaging passed.
Existing Swift capture/deprecated messaging API warnings remain in unchanged
application code; no unrelated warning cleanup was included.

The downloaded IPA reports 1.1.1 (2423), iPhoneOS Debug, the exact source SHA,
63 Compose resource files and SHA-256
`88bfdf8843923cd161b691eb0ccd148faafb8a84ed4dd9fae091dfb95b02d336`.
Checksum, Mach-O device platform, executable permissions, launch catalog,
configured resources and development entitlements passed local verification.
The separately prepared `CoffeePeek Dev` copy retains the original artifact.
Artifacts and sanitized verification records remain in ignored
`iosApp/build/device/8cc5436d/`; private diagnostics stay outside tracked sources.

After the user confirmed USB connection/unlocked state, the agreed iPhone15,4
on iOS 27.0.1 was checked again. In-place installation of
`XTL-93Y4D5FM2W.com.coffeepeek` advanced Dev 2419 to 2423, reached installer
verification 100% and succeeded. Installed app metadata confirms 1.1.1 (2423)
and the same Dev signing identity. Original TestFlight app metadata remains
unchanged at build 1961. No uninstall, data clearing or account writes occurred.
Manual launch and visual review of the login top inset and internal-screen
bottom edge remain pending the user's device check.
