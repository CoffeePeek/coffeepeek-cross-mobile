# Platform design migration

## Scope and ownership

The shared design system owns appearance and interaction primitives. Android uses
a Compose approximation of Liquid Glass; iOS uses UIKit Liquid Glass for controls and navigation,
with native blur/filled controls before iOS 26. Content surfaces, photos, lists and
forms retain the CoffeePeek identity and readable, opaque backgrounds.

Current: Material 3 and Haze controls in `core/design-system`, duplicated legacy
controls in `composeApp`, and UIKit navigation/presentation in `composeApp/iosMain`.
Target: platform rendering behind the existing design-system contracts; legacy
controls delegate to those contracts. Domain, data, routes and analytics stay in
their existing owners. No new Gradle modules or feature dependencies are needed.

## Visual direction

Apple HIG is the visual reference on both platforms: native system controls where
iOS supports them, Compose glass emulation on Android. Follow
[Materials](https://developer.apple.com/design/human-interface-guidelines/materials)
and [Adopting Liquid Glass](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass):
glass belongs to controls/navigation above content, with one material layer per
control. Preserve the original content palette rather than restyling cards/forms.

Use the existing brand roles: primary `#EAB308`, warm accent `#D4A84B`, light
background `#FAFAF9`, light surface `#FFFFFF`, dark background `#1A1412` and dark
surface `#2D241F`. Keep the existing semantic colors and
Manrope typography, content alignment and screen structure. Android controls use
24dp backdrop blur where a screen-owned Haze source is available, a translucent
surface fallback, a subtle lit rim and soft shadows. Navigation is a compact
64dp floating capsule with equally sized, accessible tab targets. Buttons keep
their shapes and use a small press response; the loader uses fading spokes.
iOS controls use native glass above content, native menus and native
presentation. Avoid glass inside content cards and nested glass surfaces.

## Migration

1. Apply the shared brand theme through both existing theme entry points.
2. Render shared action buttons and icon buttons with Compose glass backgrounds on
   Android and native UIKit glass backgrounds on iOS, preserving Compose slots,
   callbacks, enabled/loading states and accessibility.
3. Route shared switches, segmented controls, menus and sheets to platform
   implementations; retain the public contracts used by features.
4. Render Android's floating navigation with a weighted Row, glass capsule and
   selection highlight. Keep iOS's existing native tab controller.
5. Route existing feature/application Material button imports through the shared
   controls. Keep business logic, validation, events and navigation unchanged.

## Verification plan

Preserve click/selection/dismiss callbacks, disabled and loading guards, selection
semantics, large-font layout, light/dark contrast and modal/back behavior. Compile
the design system, run its existing unit tests, then assemble `directSandbox`
with the packaged runtime-class checks (`--max-workers=6`, sequential builds).
Do not create UI instrumentation or screenshot tests.

The design-system UIKit sources can be cross-compiled to an iOS KLIB on Windows.
The application includes ONNX Runtime through SwiftPM; its complete iOS target,
framework linking and native glass visual checks require macOS/Xcode.
Manual review should cover Android Sandbox and iOS 26 plus the older-iOS fallback,
light/dark themes, increased font size, VoiceOver/TalkBack, reduced motion and
reduced transparency. A Windows Android build does not certify iOS rendering.

## Risks

Native controls must stay behind Compose foreground slots and must not introduce
a second tap handler or accessibility element. Native modals must inherit the
parent composition locals and dismiss only their own controller. Material's
internal APIs must not be accessed through suppressed visibility checks.
Android approximates glass; it does not provide Apple's native refraction effect.
Backdrops cannot sample native map content, and controls without a Haze source
use their translucent fallback. Popup menus use a readable translucent surface.
The existing stable Material 3 baseline (`1.9.0`) remains underneath shared content
and form primitives. Expressive preview APIs and the alpha dependency have been
removed after the user rejected the Android Expressive appearance.

## Initial Expressive verification (superseded appearance)

- Base revision: `25ac83d0`; branch: `codex/migration-platform-design`.
- Date: 2026-10-31. Environment: Windows; build-only Android checks. ADB inventory
  reported no connected devices. No application was installed or launched, and no
  live-account operations were performed.
- `:core:design-system:compileDebugKotlinAndroid` and
  `:core:design-system:compileKotlinIosArm64`: passed for the compatible Material
  dependency; the iOS design-system compiler task executed.
- `:core:design-system:testDebugUnitTest`: 45 tests in 13 suites, zero failures.
- `:composeApp:assembleDirectSandbox`: passed, including Sandbox isolation and
  packaged DEX runtime-class verification.
- `:composeApp:assembleDirectDebug`: passed, including packaged DEX runtime-class
  verification.
- Selected `:composeApp:testDirectDebugUnitTest`: five tests in three suites
  (`ThemeManagerTest`, `SwitchPaletteTest`, `NativeSheetDetentHeightTest`), zero
  failures. Sandbox has no separate unit-test task; the initial attempt to select
  that nonexistent task was corrected to the filtered Debug task.
- `:composeApp:compileKotlinIosArm64`: requested; the app compiler task was SKIPPED
  on Windows because the shelf data module's `swiftPMImport` cinterop disables
  cross-compilation. This is not a successful full iOS application compile.
- Optional `:composeApp:compileIosMainKotlinMetadata` check stopped in the untouched
  `modules/room` generated metadata: `CoffeePeekDatabaseConstructor` expect/actual
  declarations are included in the same metadata module. Room configuration was
  not changed as part of this UI migration; this check does not certify the app.
- Final verification after review: PASS. One sequential Gradle invocation executed
  the design-system iOS KLIB compiler, all 45 design-system unit tests, both Android
  APK assemblies and the five selected application tests. Both APK runtime-class
  checks passed; Sandbox isolation passed. No failures or skipped unit tests.
- Paired light/dark previews are colocated with changed reusable components.
  Previews and device visuals were not rendered in this environment. Remaining
  gates: manual Android review, complete Xcode build/link, iOS 26 and older-iOS
  fallback review, large fonts, VoiceOver/TalkBack, reduced motion/transparency.

Most changed feature files only redirect existing Material button imports to the
shared platform renderer. The migration spans the application to keep a single
control contract; it exceeds the 100–150 file review guide for that reason.
Business models, repositories, DTOs, navigation and analytics handlers are untouched.

## Android glass follow-up — 2026-10-31

- User direction: approximate Liquid Glass on Android as well as keeping native
  Liquid Glass on iOS. Public component contracts and screen/tab callbacks stay
  in their existing owners; no modules, domain/data boundaries or SDK levels change.
- Initial emulator launch: Pixel 8 AVD `CoffeePeek_API_37`, serial `emulator-5554`,
  API 37, 1080x2400 at 420dpi. Installed `directSandbox`, package
  `com.coffeepeek.sandbox`, version `1.1.4.2573-sandbox` (2573), from the working
  tree on `codex/migration-platform-design`, base `25ac83d0`.
- The first visual review exposed a ShortNavigationBar measurement regression:
  each direct item received the bar's full width. Only Search was visible.
  A weighted Row correction was built and installed without clearing data; all
  five tabs were then visible in both light and dark captures. Eleven existing
  tab/navigation unit tests passed. The final glass implementation uses that
  explicit width ownership without the experimental navigation component.
- Android glass builds passed for `directSandbox` and `directDebug`, including
  packaged runtime-class checks and Sandbox isolation. The design-system iOS
  KLIB compiler executed successfully. Existing unit tests passed: 45 in 13
  design-system suites and 16 in 6 application suites, no failures or skips.
- Manual Sandbox review on the emulator covered the glass navigation in light
  and dark themes, opening Settings and Coffee with the correct selected tab,
  and selecting a theme and returning with Back. The app remains open for review;
  no full accessibility, form/persistence or live-backend acceptance is claimed.
- Subsequent photo-header feedback exposed inconsistent backdrop propagation:
  ownership's Edit action lacked the source used by Back/Favorite/Share, and
  FloatingGlass favorites used a 44dp plate beside 48dp glass controls. The header
  now provides its shared backdrop to all nested actions. Floating control plates
  share `glassControlSize` (48dp), including the existing size contract test.
  Blurred and fallback plates use the same neutral tint opacity (0.64 dark,
  0.72 light). Photo-dependent color variation remains inherent to transparency.
- The material-consistency follow-up passed both Android assemblies and their
  runtime checks, Sandbox isolation, the executed iOS design-system KLIB compiler,
  all 45 design-system tests and 16 selected application tests. The checked APK
  was updated in place on the emulator after checking version and signing identity.
  The subsequent device snapshot was in the filter form; a dedicated post-update
  photo-header visual comparison is not claimed. Screenshots and build diagnostics
  remain in ignored `core/design-system/build`, outside tracked sources.
- Full iOS application build/link, API 31–36 device review, large fonts,
  TalkBack/VoiceOver and reduced-motion/transparency checks remain separate gates.

## Review corrections — 2026-10-31

- Restored the original ColorScheme assignments, including primary/secondary
  containers and the original surface-role defaults. Removed the two introduced
  dark-container colors. The shared theme entry point and glass renderers remain.
- A native sheet now restores all locals supplied by its own Compose owner after
  inheriting application locals. Focus, keyboard, window, lifecycle and UIKit
  interop therefore belong to the sheet, while application/theme dependencies
  remain available. Screen insets and the cross-controller Haze source reset.
- The map expand control has one platform material background, with no Haze source
  for native map content. The photo viewer's close icon inherits the material's
  foreground color instead of forcing white in light mode.
- Button backgrounds now use the caller's enabled/disabled container colors for
  all styles. Android preserves explicit tint opacity. Neutral glass defaults
  stay neutral; native iOS controls still own their system rendering/state effects.
  Colocated outlined-button previews include custom enabled/disabled colors.
- Verification: both Android APK assemblies and packaged runtime-class checks,
  Sandbox isolation, the executed design-system iOS KLIB compiler and existing
  unit tests passed (45 design-system + 16 application tests, zero failures/skips).
  Final build completed in 43 seconds; local log: `android-glass-review-final.log`.
- An additional runtime-only composition test was attempted. Its initial compile
  error was corrected; execution then failed because the JVM Android stub does
  not implement `android.os.Trace.beginSection`. The attempted test was removed
  instead of introducing a new test framework or changing module-wide mock behavior.
  Both initial diagnostics remain in ignored build logs. No native keyboard/focus
  runtime acceptance is claimed; this still requires macOS/iOS.
- Updated `directSandbox` in place on the same unlocked API 37 emulator after
  verifying package/version and matching signing identity. No live Debug install,
  uninstall or data clearing was performed.
- Manual review after that update: Search/map and all five tabs in light mode;
  the shop header's Back/Edit/Favorite/Share controls; opening and closing the
  hero-photo gallery; the theme screen in dark mode, then restoring the original
  Light setting and returning to Search. The hero gallery is the application's
  `FullScreenImageDialog`, not the design-system attachment viewer. The latter's
  contrast correction has code/compile coverage and colocated previews only;
  its dedicated device review and custom disabled-tint preview rendering remain
  unverified. No form submissions or persistence operations were performed.
