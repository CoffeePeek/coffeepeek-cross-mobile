# Native iOS root and session contracts

Current implementation map, reviewed against `develop` on 2026-11-27. The old
extraction todo lists are superseded: the active root directly owns plain Kotlin
sessions and native SwiftUI/UIKit controllers. Shared business/data/ViewModels
remain KMP. This document is an ownership contract, not device acceptance.

## Application lifetime

`MainViewController` initializes platform Koin once and constructs `IosNativeRoot`.
`IosNativeRootControllers` selects the registered Swift `CoffeePeekRootController`.
Neither is a Compose renderer. Both launchers register their native screen,
map, glass, loader and editor factories before constructing this boundary:
`iosApp/iosApp/CoffeePeekApp.swift` and the launcher under `iosXtool`.

The Kotlin root owns cancellable observations for session/environment, foreground
notification heartbeat, analytics/privacy, navigation/deep links, root readiness,
reselection and modal arbitration. Swift owns UIKit containment and transition
completion. Do not copy root collectors or initialize DI in each destination.
`RootNavigationLifecycle` and `Navigator` remain authoritative shared routing policy.

Token refresh within the same root session preserves stacks/drafts. Account
replacement revokes private leases, cancels stale work, clears private pending
requests and rebuilds the required roots. Commands and callbacks carry generation
identity; stale login, check-in, notification and collection-return completions
must not affect a replacement root. `IosRootModals` is the single owner of global
prompt/picker arbitration; a native sheet must not introduce another writer.

## Destination lifetime

Each destination retains its feature session and explicit observation handles.
Appear/disappear drives feature visibility, refresh and analytics. Final removal
closes the session exactly once. Temporary coverage by photo library/camera or
another sheet must not destroy a draft. Interactive pop cancellation restores the
same session and visibility instead of constructing a new writer.

Native route hosts preserve route identity, local back protection, edge swipe,
status bar, map expansion, safe areas and bottom-bar clearance. UIKit transitions
finish before another navigation command or queued modal is applied. Failure/
retry remains usable even if a feature's account observation terminates.

## Feature adapters

| Owner | Current contract to preserve |
| --- | --- |
| Discovery | Owned Search/Map VM store, criteria/presentation/focus/reselection, permission and scroll-idle effects; native map session retained across snapshots |
| Check-in shop picker | CheckInPicker purpose, owned history/browsing lease and one-shot accepted selection; no Swift-side row filtering or duplicate permission policy |
| Community feed/post | Owned VM sessions, cursor/epoch/visibility guards, stable cards under overlays, guest teaser, vote rollback and explicit refresh triggers |
| Shop detail/check-ins | ShopBrowsingHostSession/lease, generation-filtered events, account AND author-visibility rebind, protected media and exactly-once linked-check-in handoff |
| Roaster/coffee | Owned list/detail sessions and shared criteria/favorites; native navigation is presentation only |
| Profile/collections | Owned profile/editor/workspace sessions; cancel preserves membership draft; accepted save returns to Collections through a once-consumed Navigator intent |
| Home/shelf/ownership | Explicit editor, photo-import and modal lifetime; dismissAllowed/draft protection, accepted-save handoff and account revocation |
| Notifications/settings | Existing shared session/consent/language/theme contracts with native list/form rendering |

Composition resolves localized resources and vector artwork through ordinary
resource lifetimes after applying language. `CoffeePeekVectorArtwork` preserves
vector transforms, clipping, fill/stroke/trim. Do not create a hidden composition
or use internal Painter constructors to prepare native artwork. Review prompting
continues through the existing platform eligibility port, not a Swift reimplementation.

## Verification boundary

Use non-UI feature/composition contracts for close/cancellation, account replacement,
same-account token refresh, late completions, terminal failure/retry and one-shot
handoff. Native tests must actually execute their new cases; compilation or a green
workflow with an excluding filter is insufficient. Android integration needs app
assembly with DEX checks; native changes need the complete framework and Swift link.

Device acceptance covers themes/larger text, back/swipe cancellation, keyboard,
photo modal coverage, foreground/background and privacy. Read-only navigation
checks do not certify OAuth, push, real submissions or persistence. No new build,
device or release verification ran for this documentation-only update. See
[remaining exit gates](IOS_NATIVE_EXIT_PLAN.md) and [setup](IOS_SETUP.md).
