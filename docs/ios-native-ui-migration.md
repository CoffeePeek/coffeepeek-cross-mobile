# Incremental native iOS UI migration

## Integration branch

Start native SwiftUI migration branches from `codex/ios-native` and open their
pull requests against it. For a stacked pull request, target its open parent
first and retarget to `codex/ios-native` after that parent lands. GitHub Actions
runs iOS build verification for migration pull requests into this branch.
Android CI continues to run for pull requests into `main` and `develop`.

## Responsibility and current boundary

The application composition module owns route dispatch, UIKit controller lifetime,
tab selection, and platform chrome. Existing feature UI owns state and actions;
domain and data contracts remain shared. The native app builds SwiftUI sources
from `iosXtool/CoffeePeek/Sources/CoffeePeek` in Xcode and xtool.
No new Gradle module is needed for this stage: the existing platform source set
is the appropriate boundary for UIKit implementations of shared UI contracts.

Before this stage, one UINavigationController hosted a Compose main screen with
Compose navigation between tabs. Top bars and search fields were Compose views.

## Stage 1: UIKit shell, headers, and search

- `PlatformRootNavigation.ios.kt` hosts a UITabBarController with five retained
  UINavigationControllers. Each tab keeps its own content and navigation stack.
  Push/pop uses UIKit; reduced motion disables programmatic transitions.
- Detail controllers hide the tab bar, matching the previous detail presentation.
  The discovery root keeps the native tab bar available, including expanded map
  mode. The shared Android renderer retains its existing bar-hiding behaviour.
- `NativeScreenController` hosts one Compose controller and lets UIKit reserve
  space for navigation/tab bars and the home indicator. Shared floating-tab
  clearance is zero inside these controllers.
- `CpTopBar` publishes title, back behaviour, and an optional header action to
  the enclosing native controller. Plain back uses UIKit's back item. Local
  editor back actions are explicit and disable stack-pop gestures while active.
  Standalone sheets fall back to their own header instead of changing a screen
  underneath. Custom hero headers remain part of their screen for now.
- `CpSearchField` now uses the shared Compose field on both platforms. Its
  background, clipping, search/clear actions and focus belong to one renderer;
  `NativeSearchField` returns false. See the
  [interop audit](IOS_COMPOSE_INTEROP_AUDIT.md) for the correction to this stage.
- Discovery content is shared between the Android navigator and native iOS root;
  it is not copied into a second feature implementation.

Public surface added: `NavigationBarAction`, because the shared public CpTopBar
accepts it. Native controllers, tab-route mapping, and platform hooks are internal.
There are no new data, domain, DI, Gradle, or Swift framework dependencies.

## Stage 2 — Theme settings

The `ThemeSettings` route now hosts a SwiftUI list through `UIHostingController`.
The existing UIKit container owns push/pop, safe areas, stack restoration and
the `settings` screen impression. The native list inherits the app appearance
from the UIKit shell and uses the current app language for Russian and Belarusian
strings. The Android destination continues to render the Compose screen.

The Swift-facing `IosThemeSettings` adapter reads and writes through `AppThemePort`.
Both renderers use the same `ThemeManager` and persisted `theme_mode` setting.
The subscription delivers Kotlin updates on the iOS main thread and SwiftUI
cancels it when the screen disappears. There are no additional packages or
Gradle modules.

### Verification on 2026-11-16

- Source `8b559dd86` plus the `codex/swiftui-theme` working changes. Android
  `directDebug` assembled on Windows with JDK 17. The packaged DEX check passed
  for `com.coffeepeek.debug`, version `1.2.0.3035-debug` (3035). Settings ran
  42 tests in six suites, with zero failures, skips or retries. No APK was
  installed or launched.
- Static checks verified both iOS entry points, Xcode and xtool resource wiring,
  and seven matching localization keys in each language. The SwiftUI previews
  and manual checks require Xcode.
- Gradle skipped application Kotlin/Native compilation and native test
  compilation on Windows because the app graph includes the Shelf SwiftPM
  cinterop. CI is configured to run the two Theme bridge contract tests.
- On macOS, build `iosApp` and run its iOS contract tests. On the simulator, check
  System, Light and Dark, persistence after relaunch, an iOS appearance change
  in System mode, both languages, Dynamic Type, VoiceOver, Back, and completed
  and cancelled edge swipes. Check the surrounding Compose screen after each
  theme change and verify the existing screen impression with a test sink.

## Remaining migration sequence

1. Move a read-only destination with loading, error and retry states to confirm
   that the Kotlin bridge handles asynchronous screen work.
2. Migrate profile and forms with SwiftUI Form/List/TextEditor. Preserve
   validation, credential content types and the native photo pickers.
3. Migrate search, feed, shop and check-in content, including modals and photo
   viewing. Preserve CoffeePeek branding and the existing MapLibre engine.

## Risks and verification

Retain the current iOS deployment target. Do not introduce a second navigation stack for a
single destination, duplicate safe-area padding, or bypass feature APIs from Swift.
Check custom back actions, authentication resets, deep links, and pending map/tab
selection after each navigation change.

Windows can cross-compile Kotlin libraries whose dependencies support it.
Xcode verifies the Swift interface. Required device checks: tab state retention;
cancelled swipe-back; editor back; password recovery email request and web reset;
open shop on map from another tab; keyboard dismissal and search cancellation;
large accessibility text; dark/light theme; iPad rotation; and current iOS bars.

References:
- https://developer.apple.com/documentation/uikit/uitabbarcontroller
- https://developer.apple.com/documentation/uikit/uinavigationcontroller
- https://kotlinlang.org/docs/multiplatform/ios-liquid-glass.html


## Edge-back restoration — 2026-10-28

The root navigation delegate restores the system interactive-pop recognizer's
existing delegate and enabled state after each shown controller. This keeps
edge-back available with hidden UIKit bars, including repeated detail visits.
Root controllers disable the recognizer; transition, modal and local-back guards
remain in shouldBegin. UIKit owns gesture progress, completion and cancellation;
the screen's accessible Back button remains available, following
[Apple HIG](https://developer.apple.com/design/human-interface-guidelines/gestures).
No feature/data/DI dependencies or analytics events change.

Verification target: develop `97b98ba5` plus this fix. Two non-UI Native contract
tests cover recognizer restoration and root/stack notifications without a window,
Compose content, account or services; the existing iOS CI filter includes them.
Verification on Windows passed in 8m01s, Gradle --max-workers=6: iOS Arm64 and
Simulator production compilation, Native test compilation, and DirectDebug APK
assembly with the packaged DEX runtime-class guard. App unit tests executed:
830 tests / 166 suites, 0 failures/errors/skips/retries. Built package
com.coffeepeek, version 1.1.2.2484/2484, source 97b98ba5 plus this uncommitted fix.
Existing preview/expect-actual/test opt-in warnings remain; changed Kotlin files
have no compiler warnings. No app was installed and no account/backend/analytics
operations were performed; IDE inspections and full Xcode build were not run.
Manual iPhone completion/cancellation, repeated entry, previous-screen state,
modal/editor guards, light/dark and VoiceOver remain device gates. Windows Native
test compilation does not execute UIKit tests or confirm gesture arbitration.
