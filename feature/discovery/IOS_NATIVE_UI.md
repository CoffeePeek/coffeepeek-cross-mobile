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

Base includes integration `9861b209d` and the ONNX build recovery from PR #396.
Before publishing, Windows Gradle compiled
`:feature:discovery:impl:compileCommonMainKotlinMetadata` and
`:feature:discovery:impl:compileIosMainKotlinMetadata`. The repository function
body check reported only the two pre-existing ownership/public-profile
violations, with no new Discovery violations. Existing duplicate metadata and
preview deprecation warnings remain outside this checkpoint.

Added non-UI native contracts cover accepted picker rows, pending recents,
stale-query pagination, current-result opening, filter/cancel actions, distinct
map select/open actions, and login-generation/account/authentication keys.
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
