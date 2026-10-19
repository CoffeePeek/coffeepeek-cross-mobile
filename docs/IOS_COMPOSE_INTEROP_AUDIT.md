# UIKit / Compose rendering audit

Audit date: 2026-10-14. Scope: every explicit UIKitView, UIKitViewController and
ComposeUIViewController in `composeApp/src/iosMain`, both SwiftUI launchers, and
standalone native presentations. Changes stay in presentation/application code;
no feature, data, domain, DI or Gradle dependency boundary changes are needed.

## Cause and rendering rules

The default UIKit interop placement is below Compose's Metal canvas, with a
transparent cutout. A transparent native view in that cutout can expose the
opaque controller background as a black/white rectangle. Giving only the inner
control rounded corners does not fix its surrounding rectangular cutout.

- Shared design-system fields, search, switches and form scrolling now render
  through Compose. The old native field/search/scroll hooks return false.
- Transparent native controls explicitly use `placedAsOverlay = true`.
  Set their background and appearance deliberately and refresh theme-dependent
  properties in `update`, not only in `factory`.
- Opaque, full-rectangle hosts (map and root controller) keep
  `placedAsOverlay = false`, allowing Compose controls to appear above them.
  Do not enable overlay placement globally: native overlays can cover Compose
  content. Check clipping, scrolling and overlapping controls on a device.
- Every Compose controller must paint its entire content background. AppContent
  supplies the theme and a default content color, including for text outside a
  Material Surface. UIKit containers use the same palette.
- Controller theme overrides use ThemeMode: System resets to Unspecified.
  Otherwise a forced UIKit trait can feed back into Compose's system-theme
  detection and prevent returning to the real system appearance.
- Each edge has one inset owner. NativeScreenController consumes bottom system
  insets because its child ends at the safe-area bottom, and top system insets
  when UIKit owns a navigation header. IME insets remain available to forms.

These placement rules follow the `UIKitInteropProperties` implementation and
documentation in the pinned Compose Multiplatform 1.12.1 dependency.

## Inventory and decisions

Paths below are relative to `composeApp/src/iosMain/kotlin/com/coffeepeek/admin/`.

| Integration | Decision / correction |
| --- | --- |
| `ui/component/NativeSearchField.ios.kt` | Shared CpSearchField owns fill, shape and actions. |
| `ui/component/NativeFormControls.ios.kt` | Shared auth fields; retain native time-picker row and numeric keyboard Done accessory. |
| `ui/component/PlatformTextInput.ios.kt` | Preserve explicit auth content types and Compose password visibility behavior. |
| `ui/component/PlatformScrollableColumn.ios.kt` | Shared scrolling with IME padding; remove nested native scroll/controller rendering boundary. |
| `ui/component/PlatformPresentation.ios.kt`: switch | Shared switch palette and geometry; retain selection haptic. |
| Same file: segmented control | Transparent overlay, explicit theme/text colors, native accessibility. |
| Same file: date/time picker | Transparent overlay, themed native wheels; scrollable body below the action row. |
| Same file: modal sheet | Full-size Surface, themed controller, app environment, system detents. |
| Same file: alerts/action sheets | Apply app appearance and tint; preserve native presentation/dismissal. |
| `ui/component/PlatformActivityIndicator.ios.kt` | Clear overlay, theme-derived color, no touch interception. |
| `ui/component/PlatformScrollToTop.ios.kt` | Clear 1 dp overlay; preserve status-bar event delegate and existing interaction behavior. |
| `ui/component/PlatformGlassControls.ios.kt` | Existing overlays retained; synchronize foreground, selection and appearance with theme, enable native accessibility. |
| `auth/AppleAuth.ios.kt` | Overlay, app field radius, theme, enabled state and current click callback. |
| `map/CoffeeMap.ios.kt` | Opaque underlay with themed loading background; preserve map gesture handling. |
| `ui/PlatformRootNavigation.ios.kt` | Opaque controller host, themed tab/container colors and native accessibility. |
| `ui/NativeScreenController.kt` | Paint background, consume UIKit-owned insets, theme detail headers; clear disposed header state. |
| `MainViewController.kt` / common `App.kt` | Root Compose paints background; every AppContent provides theme/default text color. |
| `utils/ImagePicker.ios.kt` / `utils/ShareHelper.ios.kt` | Photo/camera/share controllers follow the selected app theme. |
| `ui/component/KeyboardDismiss.ios.kt` | Gesture-only bridge; no rendering surface added, existing exclusions retained. |
| SwiftUI launchers in `iosApp` and `iosXtool` | Both delegate container/keyboard safe areas to the KMP host. No extra padding added. |
| Swift social sign-in and MapLibre providers | Native providers remain; their visual hosts are configured at the Kotlin boundary. |

Shared root headings in CommunityScreen, ProfileScreen and SettingsScreen remain
left-aligned. Detail CpTopBar still publishes a native header. Password recovery
explicitly uses themed heading/body colors, including its confirmation state.

## Verification

Kotlin compilation for Android and iOS is available on Windows. It checks shared
and native API signatures, but does not run UIKit, link the Swift app or validate
pixels. Current verification commands:

```text
.\gradlew.bat :composeApp:compileKotlinIosArm64 :composeApp:compilePlayDebugKotlinAndroid --offline --console=plain
.\gradlew.bat :composeApp:testPlayDebugUnitTest --offline --console=plain
```

Results: Android and iOS Kotlin compilation passed. All 124 application unit
tests passed (40 suites, zero failures/errors/skips). `git diff --check` passed.
Two pagination tests previously waited for the raw exception text `Offline`,
which the existing error-handling changes no longer expose. They now wait for
the failed page/error state and still verify retry, ordering and deduplication;
the production loading logic was not changed for this audit.

Required iOS runtime acceptance checks before release:

1. Dark, light and system themes, including switching explicit theme back to
   system while the app is open. Check search, auth, reset confirmation, loaders,
   filters, map loading, tab bar, native headers and native presentations.
2. Login/register: rounded fields, eye spacing, first edit after visibility
   toggle, selection, Next/Done and Passwords AutoFill. Password recovery: email
   request, confirmation, browser reset from the email and return to login.
3. Filters/date/time sheets at medium and large detents: full switch geometry,
   no rectangular backgrounds, scrolling with the keyboard and large text.
4. Feed/profile/settings heading alignment and first-item spacing; detail push,
   back and cancelled swipe-back; no duplicated top/bottom/keyboard clearance.
5. Scroll transparent native controls past clipping boundaries; open overlapping
   menus/sheets; check map buttons, Apple sign-in, VoiceOver, Reduce Motion and
   status-bar scroll-to-top on the active list.

Device/simulator rendering has not been verified by this Windows audit. Earlier
simulator or emulator results in the migration documents are historical.
