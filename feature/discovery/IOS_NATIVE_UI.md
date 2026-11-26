# Discovery native iOS checkpoint

## Scope and actual entries

The root Discovery tab still enters `DiscoveryTabEntry.Content` through
`FeatureDiscoveryScreenRenderer`. Its iOS platform adapter now hosts
`DiscoveryNativeScreen` instead of `DiscoveryTabContent`. Android keeps its
existing renderer. The `Navigator.Screen.CreateCheckIn` route still enters
`CheckInShopPickerEntry.Content`; the same iOS adapter renders the shop picker.
The global plus/type-choice sheet is a different route and is unchanged here.

The iOS adapter has a required startup presenter, configured by
`configureNativeDiscovery` in `CoffeePeekApp`; there is no optional Compose
Discovery screen fallback on iOS. The common Compose implementations remain
because Android still consumes them.

## Ownership and preserved behavior

* `SearchViewModel` and `MapViewModel` retain query debounce, accepted picker
  snapshots/recents, criteria, location permission outcomes, account changes,
  favorite writes, catalog recovery, pagination, events and analytics.
* Feature-owned `DiscoveryIosState` contains localized display values;
  `DiscoveryIosIntents` maps native intents to those existing shared actions.
  The picker cannot paginate while its displayed criteria are stale.
* Application composition supplies authorized media, UIKit containment,
  account-generation revocation and the existing MapLibre provider.
  Native image requests use `IosCatalogueMedia.observeImage`; shared Swift
  `CoffeePeekCardImage` renders them with the existing CFPK placeholder.
* A single native map remains mounted through preview expansion and collapse.
  `NativeMapSession` and `nativeMapStateJson` remain authoritative for map
  markers, clusters, zones, heatmap, viewport callbacks and camera moves.
  Map controls, suggestions, zone cards and selection carousel render in SwiftUI.
* List opening and map selection/opening remain different Kotlin intents.
  Root callbacks still own shop routes, sign-in, add-shop, draft continuation,
  tab map expansion, picker back and picker cancellation.
* Search/list/map presentation changes follow the root lifecycle and actual
  visibility. Native geometry reports exposure visibility to the existing
  consent/session/lifecycle guarded analytics ledger. There is no second
  analytics service. Overlay/map/search/favorite events remain in shared logic.

## Explicit remaining owner slots

Discovery still calls `DiscoveryRoasterSlots.preview`, `.search`, `.hasResults`
and `.onAdd`, assembled by `discoveryRoasterSlots(RoasterScreenRenderer)`.
`IosDiscovery.roastersController(expanded)` hosts these owner entries without
copying their ViewModels or business logic. Their composition locals and
analytics visibility follow the current root/overlay state. Native previews
reserve 360 points, or 520 with accessibility text sizes.

The next Roaster checkpoint owns `RoasterScreenRenderer.DiscoverySection`,
`DiscoverySearch` and `hasDiscoveryResults`, including the owner cards, tags,
favorites and empty/add-roaster recovery. The existing contract is unchanged,
so replacing the Roaster entry renderer also replaces these embedded slots.
No claim that all embedded Roaster content is already native is made here.

## Verification (2026-11-18)

Base includes integration `8d18a6395`, the native photo-viewer checkpoint #397,
and the ONNX build recovery from PR #396.
Final pre-push Windows Gradle compiled
`:feature:discovery:impl:compileCommonMainKotlinMetadata` and
`:feature:discovery:impl:compileIosMainKotlinMetadata`. The repository function
body check reported only the two pre-existing ownership/public-profile
violations, with no new Discovery violations. Existing duplicate metadata and
preview deprecation warnings remain outside this checkpoint.

Added fourteen non-UI native contracts cover accepted picker rows, pending recents,
stale-query pagination, current-result opening, filter/cancel actions, distinct
map select/open actions, restored viewport camera, zero-key versus explicit
location requests, empty-roaster suggestion boundaries and login-generation/account/authentication keys.
The iOS verification workflow includes these suites together with existing
search/filter/session/media contracts. Native tests and the Xcode app build
must pass on macOS before merge; metadata compilation does not execute them.

Paired light/dark SwiftUI previews are colocated with the screen, shop card,
picker row and map controls. They use fake state and no network. No automated
UI, screenshot or instrumentation tests were added or run. No device app was
installed and no live account, camera or persistence writes were performed.

Remaining manual gates on an iPhone or simulator: light/dark and accessibility
sizes; preview-map clipping/expansion with retained list position; search and
filter cancellation; viewport/cluster/zone/heatmap/camera interactions; protected
images; guest favorite gates; account replacement; picker recents/pagination,
selection, back and close; root draft continuation; and embedded Roaster slots.

First macOS run `44264283844` stopped both jobs while compiling application iOS
sources: `IosDiscoveryMap` lacked UIKit's category-extension import for
`endEditing`. Added `platform.UIKit.endEditing`, matching existing native
keyboard-dismiss code. No native contract tests had executed in that failed
run; the follow-up workflow must supply actual execution evidence.

Static parity review also preserves cached native exposure visibility on return
from child routes and explicitly suppresses Discovery exposures while the
full Roaster owner slot is open. Permission granted after the map is mounted
updates its location puck without recentering the viewed map; initial already-
authorized positioning and each explicit location request still run once.
The add-roaster suggestion remains limited to confirmed empty text results.

The second macOS run `44267989848` passed application Kotlin compilation and
its first app contract step, then Swift compilation found that feature-owned
native payload types were not exported from the app framework. Added app-owned
`IosDiscoveryState`/row wrappers, following the existing filter boundary, rather
than exporting feature implementation modules. The final workflow must compile
Swift against these app-owned types and execute the full updated suites.

Run `44267989848` completed its Kotlin Native job successfully: the new account
suite executed 2 tests and native feature-intent suite executed 7 tests, with
zero failures, errors or skips in their XML reports. The updated wrappers and
five additional parity regressions require the next workflow run; the failed
Swift build is retained as earlier evidence rather than counted as a pass.

## Filter row parity, 2026-11-25

Source `dce8989e5` plus `codex/migration-ios-shop-filter-parity` working changes.
The native Features filter uses the same divided selection rows as the other
catalog groups. Selected rows show a checkmark. The existing first-five plus
selected-item rule, draft, reset and apply callbacks remain the owners of selection.
Colocated light/dark previews cover normal and accessibility text sizes, including
a selected feature after the first five entries. Swift 6.4.2 syntax parsing passed.
Android `directDebug` (`com.coffeepeek.debug`, `1.2.0.3221-debug`) assembled with
`--max-workers=6` and passed the packaged DEX check. The body-limit check reported
130 recorded violations and zero new ones. Discovery's 179 unit tests in 21 suites
executed successfully, with zero failures, errors or skips.
Manual iPhone rendering, VoiceOver, filter selection/cancellation and full native
compilation remain acceptance gates.

## Check-in shop picker motion and state audit, 2026-11-25

Source `dce8989e5` plus `codex/migration-ios-shop-filter-parity` working changes.
The audited entry is `Navigator.Screen.CreateCheckIn` through
`CheckInShopPickerEntry`, not the preceding type-choice sheet. Verification plan:
preserve accepted rows during search debounce, recents, Nearby permission and denial,
map/list switching, selection/back/close, pagination, account guards and cancellation.
Kotlin still owns search, requests and navigation intents; Swift owns view motion.

The audit found and corrected these native differences:

- Raw text changes scrolled the list to its beginning before accepting any results.
  A small shared presentation value now exposes the normalized accepted query and
  Nearby flag. Swift resets the scroll position only when these accepted values
  change; whitespace, further pages and location-distance updates keep the position.
- Nearby from the map neither collapsed the map nor preserved an already active
  Nearby filter. Both renderers now use the same intent policy. The native chip
  also reflects a pending permission/location request and is unselected on the map.
- Empty results during debounce could show empty feedback instead of pending
  progress. Native loading includes the same pending-criteria rule as Android.
  Failed requests continue to show recovery instead of indefinite progress.
- Picker refresh inserted a loader above retained rows and shifted them. As on
  Android, retained rows remain in place while a new snapshot is requested.
- Row changes had no placement/fade animation, and recents/results used separate
  identity scopes. One stable shop-ID sequence now preserves row/image identity
  across those sections and animates insertion/reordering over 180 ms. Chip selection
  and accepted-result scroll changes animate too. Reduce Motion disables this motion.
- The native pagination sentinel now rechecks eligibility when returning from the
  map or becoming active, while retaining existing pending-query and session guards.

No business use case, repository, API endpoint, analytics event or consent/session
guard was replaced. Picker selection still uses the shared open intent; list/map
back and close remain separate root callbacks. Existing analytics/exposure and media
ownership is unchanged, and no new Task, DispatchQueue or isolation escape was added.

Executed verification: all 187 Discovery unit tests in 22 suites passed, including
8 new accepted-result/pending/Nearby regressions; zero failures, errors, skips or
retries. `:feature:discovery:impl:compileIosMainKotlinMetadata` passed. Android
`directDebug` (`com.coffeepeek.debug`, `1.2.0.3221-debug`, code 3221) assembled and
passed the packaged DEX runtime-class check with `--max-workers=6`. Function limits
reported 130 recorded and zero new violations. Swift 6.4.2 syntax parsing with
strict-concurrency flags and `git diff --check` passed. Existing duplicate metadata
and preview deprecation warnings remain outside this change.

Colocated light/dark interactive fake-state previews cover Nearby/list/map changes,
insertion/reordering and large text, with no network or production DI.
Reduce Motion requires a device/system-setting check; its EnvironmentValues entry is read-only.
They were not rendered on this Windows host. No device was selected or app installed;
no account/data mutations occurred. Full application Kotlin/Swift compilation and
iOS-only intent tests remain unverified; the earlier application metadata attempt
failed in unchanged Room constructor generation (see the report feature record).
Manual iPhone/simulator acceptance still needs real layout and animation checks,
keyboard/search, rapid Nearby/map changes, permission denial, recents, scrolling and
pagination, selection/back/close, VoiceOver, both themes and accessibility text sizes.

## Search empty-state visual parity, 2026-11-25

Source `dce8989e5` plus `codex/migration-ios-shop-filter-parity` working changes.
Native empty search now follows Android's SearchEmptyContent and
SearchContributionActions. Separate shop/roaster suggestion cards use the same
design-system `maskot_with_magnifying_glass.png` resource, 132-point illustration,
20-point corners, surface-variant background at 55% opacity, 16/24-point card
padding, 22-point bold heading, 17-point description and plus action. No new image,
package or resource-copy path was introduced. The native shared palette gained
the existing design-system surfaceVariant token.

Kotlin forwards `SearchState.hasNoSearchMatches` to the app-owned rendering state.
As on Android, confirmed globally empty results show the suggestion cards without
generic recovery copy; other empty shop/catalog/filter states keep their heading,
description and appropriate edit-query/reset-filter actions. Empty search no longer
inserts the pagination sentinel between recovery and suggestions. Picker empty-state
behavior, existing suggestion eligibility, root navigation and analytics/session
ownership remain unchanged. RU/BE copy comes from existing Discovery resources.

Verification plan: preserve the distinct query/filter/catalog recovery cases, wait
for both search owners before claiming no global matches, retain failures/pending
work and query-preserving filter reset. All 187 Discovery unit tests in 22 suites
executed successfully, including the 4 existing empty-state contracts; zero failures,
errors, skips or retries. Discovery iOS metadata, Android directDebug assembly/DEX
(`com.coffeepeek.debug`, `1.2.0.3221-debug`, code 3221), body limits (130 recorded,
zero new) and Swift 6.4.2 syntax parsing passed. Gradle used `--max-workers=6`.
Existing metadata/preview warnings remain. No Swift task or isolation escape was added.

Colocated fake-state light/dark previews cover global empty search, empty shop
results, filter recovery and empty catalog, plus a small window with large text.
These previews were not rendered on Windows. Full application native compilation
and manual iPhone layout/asset, keyboard/scroll, VoiceOver, both languages/themes
and recovery/action checks remain open after the previously recorded Room metadata
failure. No device was selected, app installed or account data changed; no automated
UI, screenshot or instrumentation tests were added or run.

## Map restoration after search — 2026-11-25

The map preview and its anchor now live in an eager outer VStack; result rows stay
inside their existing LazyVStack. The previous outer lazy container could discard
the zero-height preview/anchor in search mode, leaving no measured slot when the
landing returned. Keeping the structural slot mounted preserves the map canvas
and measurement owner through search/cancel. Search intents, filters, pagination,
map data, analytics and request/session guards are unchanged. The existing 187
Discovery results and Android assembly/DEX remain passing; the final combined
build records them as UP-TO-DATE. Manual acceptance must repeat search/cancel,
clear/back, map expand/collapse, scrolling and keyboard changes on the iPhone.


## Filter dismissal — 2026-11-25, Debug 3226

Removed Done from both native header layouts. Cross and swipe commit the existing
draft through the same apply action; otherwise removing Done would discard choices.
Reset, filtering and loading/error owners remain unchanged. The native price slider
and selectable row are shared with contribution forms. Discovery 187 unit results
were UP-TO-DATE; Android directDebug assembly/packaged DEX and function limits
(130 recorded, zero new) passed for dismissal. Manual cross/swipe/reset and subsequent
map/search review remain open.
