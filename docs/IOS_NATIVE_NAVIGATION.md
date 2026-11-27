# Native iOS navigation

Current source contract, reviewed 2026-11-27. Application composition owns shared
routing policy; UIKit owns native tabs, pushed controllers, gestures and modal
transitions. The active boundary is `MainViewController` → `IosNativeRoot` →
`CoffeePeekRootController` in `NativeRootController.swift`, not the retired
Compose AppContent/NativeScreenController host.

## Root and routes

The native tab controller has five slots, including the create action, and
retains its navigation controllers. `Navigator` publishes routes, selected-tab
requests and map focus. `IosRootState`/generation-aware commands describe the
requested state; UIKit acknowledges completed transitions before Kotlin advances
routing. Direct selection/reselection and programmatic routing retain their
explicit shared policies; do not invent a second Swift navigation history.

Route controllers retain feature session ownership and visibility. Native back,
interactive swipe cancellation and local draft protection must agree with shared
navigation state. A detail can hide the tab bar; map expansion has its own bar
policy. Safe-area/clearance is supplied by the native host and must not be added
again using Android floating-bar padding.

`IosRootModals` and `NativeRootModalPresenter` arbitrate global UI and wait for an
attached, stable presenter. Busy forms retain dismiss guards. Close/cancel must
close only their own presentation and dispose the correct session once. Account
replacement invalidates stale navigation and private modal callbacks; token
refresh for the same account does not discard every stack/draft.

## Appearance

On supported systems native controls/tab chrome use system Liquid Glass; older
supported iOS versions use the configured material/filled fallback. The app's
minimum is iOS 17 (`Shared.xcconfig`), not the old iOS 15 Compose fallback.
Native root/feature backgrounds use CoffeePeek's theme, and Swift applies the
selected appearance to the owning window/controller. Native glass is presentation,
not permission to duplicate actions or accessibility elements.

Do not infer all scroll-edge/minimization effects from the existence of a native
tab controller. Review the actual Swift host and each scroll container. Feature
content, keyboard and full-bleed hero safe areas need their own layout checks.

## Verification

Use the existing navigation/root/session contracts plus the iOS workflow's full
Kotlin/Swift link. Check device tab retention, show-on-map focus, cold/warm links,
back/swipe cancellation, create/editor return and account transitions in both
themes/larger text. A local source/contract audit does not certify native touch
routing or real sign-in. No device or build ran for this documentation update.
See [root/session contracts](IOS_NATIVE_ROOT_SESSION_CONTRACTS.md),
[exit gates](IOS_NATIVE_EXIT_PLAN.md) and [setup](IOS_SETUP.md).
