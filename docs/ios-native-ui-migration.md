# Incremental native iOS UI migration

## Responsibility and current boundary

The application composition module owns route dispatch, UIKit controller lifetime,
tab selection, and platform chrome. Existing feature UI owns state and actions;
domain and data contracts remain shared. SwiftUI screens will live in `iosApp`.
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

## Remaining migration sequence

1. SwiftUI settings pilot: expose feature state/actions and a cancellable state
   subscription; ensure main-thread delivery and screen-owned lifetime. Keep
   repositories and DTOs behind the feature boundary. Move settings ownership
   incrementally before adding a SwiftUI consumer.
2. Profile and forms: SwiftUI Form/List/TextEditor, focus management, validation,
   and keyboard interaction. Preserve credential content types and reuse the
   existing native photo pickers.
3. Search/feed/shop/check-in screen content, native refresh, and photo viewer.
   Preserve CoffeePeek branding and the existing MapLibre engine.

## Risks and verification

Retain iOS 15 compatibility. Do not introduce a second navigation stack for a
single destination, duplicate safe-area padding, or bypass feature APIs from Swift.
Check custom back actions, authentication resets, deep links, and pending map/tab
selection after each navigation change.

Compilation on Windows verifies Kotlin/Native API signatures, not UIKit runtime
layout or gesture arbitration. Required device checks: tab state retention;
cancelled swipe-back; editor back; password recovery email request and web reset;
open shop on map from another tab; keyboard dismissal and search cancellation;
large accessibility text; dark/light theme; iPad rotation; iOS 15 and iOS 26 bars.

References:
- https://developer.apple.com/documentation/uikit/uitabbarcontroller
- https://developer.apple.com/documentation/uikit/uinavigationcontroller
- https://kotlinlang.org/docs/multiplatform/ios-liquid-glass.html
