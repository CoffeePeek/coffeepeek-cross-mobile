# Full-screen photo viewer: safe area and dimming

Scope: shared application photo viewer and its Android/iOS dialog windows.
All existing gallery, profile, review and check-in callers keep their contracts.
Photo loading, access control, analytics occlusion, paging and zoom remain intact.
No Gradle dependencies, feature boundaries or data contracts change.

Verification plan: assemble `directDebug` without installing it, run the existing
`PhotoViewerImagesTest` unit suite and check packaged runtime classes. Manual UI
review uses `directSandbox` on Android; iOS requires a Mac/device build.

The shared scrim now uses 80% black and fades with the existing dismiss drag.
Each platform disables its additional dialog dimming; Android opts into
edge-to-edge and iOS disables automatic dialog insetting. The shared surface
applies `WindowInsets.safeDrawing` once, including display cutouts. Its clipping
keeps zoomed photos inside that area. The counter and close button no longer
add a second status-bar inset. Window configuration stays platform-specific.

Light/dark previews beside the component use a synthetic image and no DI,
network or database. They show the surface and controls, not native dialog
windows or actual device insets.

## Verification record — 2026-10-23

- Base: `develop` at `b685d02a`; branch: `codex/migration-photo-viewer-safe-area`.
- Android build: `directDebug`, build only; no device installation or launch.
- `assembleDirectDebug` and `verifyDirectDebugRuntimeClasses`: passed.
- `PhotoViewerImagesTest`: 7 executed, 0 failures/errors/skips, no retries.
- No automated UI/instrumentation/screenshot tests were created or run.
- Studio MCP unavailable; no IDE inspections or rendered previews claimed.
- iOS compilation is unverified on this Windows host. Native Android/iOS
  safe areas, light/dark appearance, landscape, large fonts, accessibility,
  opening from sheets, zoom, swipe and back/cancel need manual device review.
- No live account, server media, analytics delivery or release acceptance run.

## Outgoing-screen photo flash

The reported flow is navigating away from the feed/public profile: the outgoing
photo turns grey during the transition, but renders normally on return.
`CpImage` used the navigation entry's `STARTED` state both to gate requests and
to discard its displayed painter. It also dropped the selected detail image
when that entry stopped, exposing an expired or unloaded preview.

The displayed painter now survives lifecycle changes within the same resource,
session and permission scope. Selected detail quality stays composed. Transfers
still stop below `STARTED`; detail renewal also follows the existing upgrade
eligibility. The permission expiry timer continues while stopped, so only an
unexpired, currently authorized painter remains visible during the exit.
Logout, scope revocation, server denial and URL changes still remove it.

Verification: rerun image/cache/transport/session unit contracts and viewer URL
selection, assemble `directDebug` and run its packaged runtime-class guard.
Manual transition review on Android/iOS remains open; ADB reported no attached
devices on 2026-10-23. No automated UI suite is required or run.

Executed on 2026-10-23: `assembleDirectDebug` and the packaged runtime-class
guard passed; 68 image/cache/transport/session/URL unit tests passed with
0 failures/errors/skips and no retries. These contracts verify authorization
and loading policy, not the native navigation animation. No iOS build or
on-device visual acceptance is claimed.

## Public-profile section loading

Loading is scoped to individual sections in both the shared/iOS profile and
Android's migrated profile. Favorites keep their existing section loader when
no favorites have loaded. Check-ins now use the same rule: show the local
loader only while loading an empty list, keeping existing cards visible during
refresh. The header, navigation and remaining sections stay visible and usable.
The section sheets keep their existing local loading/pagination indicators.
There is no full-page transition overlay or lifecycle-dependent profile mask.

Verification: Android Debug assembly and DEX guard plus legacy public-profile
and migrated-profile unit suites. Manual Android/iOS checks of initial loading,
refresh, navigation and interactive-back cancellation remain open. No automated
UI/instrumentation/screenshot tests are created or run.

2026-10-23, same branch based on `b685d02a` with local section-loading changes:
`assembleDirectDebug` and its runtime-class guard passed. The legacy public
profile suite (15 tests) and migrated profile suite (58 tests) passed:
73 executed, 0 failures/errors/skips, no failure retries. No device launch,
rendered preview or iOS compilation is claimed.

## Hide empty profile favorites and check-ins

Successfully loaded empty favorites/check-in sections omit their headings,
links and placeholder cards in both shared/iOS and migrated Android profiles.
Section loading and retry states stay visible. Empty placeholder cards are
also removed from the profile's section sheets. Standalone Favorites/history
destinations keep their own behavior.

The own-account favorites preview owns a header slot, so it hides the heading
using its actual server-backed account lists rather than inferring emptiness
from the public profile. Its existing callers pass their headings through the
slot. Own-profile check-in navigation uses the personal check-in count, retaining
access to private history even when the public check-in preview is empty.
Fallback profiles without a public address follow the same empty-section rule.
No data, repositories or Gradle dependencies change.

Verification: Android Debug assembly/DEX guard and legacy/migrated profile plus
favorites unit contracts. Manual empty/loading/error/nonempty checks and iOS
compilation remain open; no automated UI/instrumentation/screenshot tests run.

2026-10-23, same branch based on `b685d02a` with local empty-section changes:
Android Debug assembly and runtime-class guard passed. Executed 17 legacy
profile/favorites tests and 58 migrated-profile tests: 75 passed, 0 failures,
errors or skips, no failure retries. No device launch or iOS build was performed.

## iOS map control above the Discovery header

The supplied screenshot shows the location control above the filter row while
Discovery displays its lists. The map surface already uses UIKit underlay
interop and the preview viewport already clips to its bounds. Its location/zone
buttons instead used the generic native-glass overlay above the entire Compose
canvas, allowing them to escape that viewport and cover the header.

Only `PlatformMapControlButton` now uses `placedAsOverlay = false`. The native
UIButton factory, glass configuration, accessibility, selected colors and tap
callbacks are shared unchanged with other glass buttons through a private helper.
Other native-glass controls retain their existing overlay policy. No location
permission, tracking, camera or map-state behavior changes.

Verification on 2026-10-23, same branch/base with local iOS control changes:
Android Debug assembly/DEX guard and existing map unit contracts pass. Pure
Discovery preview/viewport/expansion geometry tests are included separately;
their `FeedFiltersUiTest` name does not imply UI instrumentation. No automated
UI or screenshot tests are created or run. Native iOS compilation is unavailable
on this Windows host, so the UIKit change is source-reviewed only. Verify light/
dark Discovery scrolling, recycled map previews, expand/collapse, location/zone
actions, taps and accessibility on iOS before treating the visual fix as accepted.

Executed map/Discovery unit contracts: 21 tests, 0 failures/errors/skips, no
failure retries. The passing Android build does not compile the iOS helper.

## iOS safe-area follow-up — 2026-10-24

Device screenshots from build 2417 identify two additional layout issues: a
fixed lower band on the private check-in history, and dimmed status/home-indicator
areas outside the full-screen point picker. This follow-up starts at `47a25b9c`
on `codex/fix-ios-native-presentation` and changes existing presentation only.

The user chose to retain centered system confirmations after discussing the
system-controlled width. Alert presentation and its width are unchanged here.

Verification plan: compile iOS arm64 KLIBs, assemble Android DirectDebug with the
packaged DEX guard, and run existing calendar, profile, photo-viewer, action and
native-map non-UI contracts. Native packaging and manual device checks follow.
Do not perform logout, block/unblock, photo upload or other live data mutations.

The existing edge-to-edge photo modal is shared as `PlatformFullScreenDialog`;
its platform implementations are renamed without changing photo behavior. The
point picker now reuses it. On iOS, disabling platform-inset clipping and the
scrim lets the picker paint its surface under both system areas, while existing
status/navigation inset padding keeps its header and confirmation controls safe.
Android retains the same edge-to-edge implementation and content-owned scrim.
Native modal/window rendering cannot be certified by Compose previews.

Only the native private-history routes (`VisitedPlaces`, `HomeCheckIns`) gain
bottom-edge extension. Their host consumes the top inset; Scaffold retains the
bottom safe inset for floating-button placement. The list's bottom clearance is
inside content padding instead of reducing its viewport. A small stateless
`CheckInHistoryScaffold` retains the same header/create callback and has paired
light/dark previews. Calendar state, filtering, paging, photos, navigation and
create-attempt analytics remain with their existing owners. No Gradle, DI,
repository, source-set or feature-boundary changes are introduced.

Local checks passed on 2026-10-24: iOS KLIB compilation, Android DirectDebug
assembly/DEX guard and the selected existing unit suites, in one invocation with
`--max-workers=6` (1m04s). Seven suites executed 47 tests with zero failures,
errors, skips or retries. No automated UI, instrumentation or screenshot tests
were added or run. Device visual acceptance, keyboard, both themes, accessibility,
back/cancel/reopen and photo-viewer regression remain manual gates.

Native package verification: [run 40086070708](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/40086070708)
passed in 18m32s for implementation commit `71a705a2`, including the strict Kotlin
device-framework link and native Xcode Debug build. The first dispatch,
`40086012865`, stopped at checkout because its abbreviated SHA was interpreted as
a branch/tag; it never compiled the app. Re-dispatching the full SHA succeeded
without source changes. Both outcomes are retained.

Local IPA verification confirmed exact source, checksum, iPhone arm64 Mach-O,
version 1.1.1/build 2419, 63 Compose resources, launch assets and app configuration.
A separate CoffeePeek Dev copy is prepared for the existing development bundle;
IPA files and sanitized verification reports stay in ignored build output.
Device installation and visual acceptance are pending connection at this stage.

Following the user's connection/unlock confirmation, CoffeePeek Dev was updated
in place from 2417 to 2419 on iPhone15,4/iOS 27.0.1. xtool completed installation
and verification; a separate installed-app metadata read confirms build 2419.
The original TestFlight app remains at 1961. Sanitized installation evidence is
stored beside the ignored IPA. Manual startup and visual review remain pending.
