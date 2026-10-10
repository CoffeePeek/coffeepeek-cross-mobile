# Favorites: incremental Android integration

For the remaining Android-only verification and cleanup, follow
[ANDROID_COMPLETION_PLAN.md](ANDROID_COMPLETION_PLAN.md). iOS integration and the
application-wide root Navigation 3 migration remain separate decisions.

Android application composition now binds the new repository and a legacy-contract
adapter as one writer. Its Favorites destination renders the new feature screen
through a platform adapter, while the shared root remains on Navigation 2. The
old screen, route, repository implementation file, JSON key and database schema
remain; the old repository binding is omitted on Android only. iOS keeps its
existing screen, binding and runtime behaviour. These changes are stacked on
#49 → favorites foundation → UI/DI → compatibility bridge → Android integration.

## Boundaries and module gate

| Module | Owns / public surface | Dependencies | Consumers / why a module |
|---|---|---|---|
| api | Serializable FavoritesRoute: NavKey and minimal Composable FavoritesEntry interface | Navigation 3 runtime, Compose runtime, serialization | Root/feature navigation; small screen-construction ABI without VM/data |
| domain | FavoriteShop snapshot, FavoritesRepository Result/Flow contracts, ObserveFavoriteIdsUseCase | Kotlin + coroutines only | Own UI/data, explicitly supported cross-feature membership consumers; independent business ABI/tests |
| data | Internal StoredFavorite/mapping/repository; public storage construction port and factory | domain + serialization | Application composition only; UI cannot import DTOs or repository implementation |
| impl | Internal MVI ViewModel/state/actions/events/screen/cards; API implementation and feature Navigation 3 registration | api/domain, design-system, core presentation contract, lifecycle, Kamel | Application composition; public API consumers do not acquire screen or VM implementation |

```text
Android composition → data → domain ← impl/ui
                   └────→ impl API adapter → api
                   bridge → existing SettingRepository
                   adapter → existing FavoriteRepository contract
Android root Navigation 2 destination → FavoritesEntry.Content (active)
future Navigation 3 root entryProvider → favoritesEntry (prepared)
```

Domain/data are manually constructed; no Koin annotations, contexts or service
lookups. Data requires an injected dispatcher for storage and parsing; production
DI supplies the core IO dispatcher. Android application composition declares the
favorites Koin bindings and starts Koin. The temporary Room and legacy bridges
live in composeApp/androidMain under di/favorites, not in a separate Gradle
module or generic core DI aggregator. Factories hide implementations.
Feature UI depends on neither data nor application DI; Gradle enforces this restriction.
Other business/UI features may consume favorites domain deliberately; api remains
the route/entry boundary. No generic BaseViewModel or Navigator singleton.

## Current vs target and behaviour

Legacy FavoritesViewModel/Screen, FavoriteRepositoryImpl and LocalFavoriteShopDto
stay in their existing folders. New UI uses a favorites-owned saved snapshot,
not the large CoffeeShopDetails aggregate or feed.ShopCard implementation.
The original feature has no favorites HTTP service; no artificial backend layer
or endpoint is introduced. Future HTTP DTOs/services belong in this data module.

The package layout now follows ownership: domain model/repository/usecase;
data local/model, mapper and repository; impl API adapter, feature navigation,
and ui/compose with component/model folders. `FavoritesAction` enters the
ViewModel, `FavoritesUiState` describes durable rendering, and `FavoritesEvent`
carries one-off shop/back navigation to the runtime screen. The stateless
`FavoritesScreenContent` and components stay previewable without DI. No
presentation-only `ui/data`, backend or formatter package is needed here.
The ViewModel inherits the typed core presentation base. Core owns its private
state flow and event channel; favorites owns the actions, repository observation,
Result failures and cancellation handling. `updateState` performs atomic
`MutableStateFlow.update` without duplicating that extension.

The storage key remains local_favorite_shops. Internal JSON field names/defaults
match the existing saved format, including single roasterPhotoUrl fallback and
plural roasterPhotoUrls. Duplicate saved IDs retain their first (newest) snapshot.
A save replaces by ID and moves it to the front. Removing
the last row deletes the key, and clear deletes only this key. Corrupt data is an
explicit Result failure, not silently interpreted as empty and overwritten.
Clear is an explicit destructive user/session operation and can delete corrupt
data; it is never triggered by a read failure. Unknown fields are ignored like
the legacy serializer; their preservation on future writes is not promised.

One repository instance serializes its read-modify-write operations. Observation
comes from storage, covering external writes without FavoriteSync. Android now
shares one Koin instance across the new contract and legacy adapter. Independent
repository instances or direct old writers are not protected by its mutex; do not
reintroduce a parallel writer and assume shared JSON prevents races. iOS retains
its one legacy writer until its own migration.

For the transition, createLegacyFavoritesRepositoryBridge(newRepository) adapts
the existing repository contract for feed/detail/session consumers. It maps
CoffeeShop snapshots to FavoriteShop and saved snapshots back to the existing
CoffeeShopDetails shape without putting legacy models in domain/data. Both the
adapter and new screen MUST receive the same new repository singleton. The
factory itself does not register a Koin binding. Android composition now opts out
of dataModule's legacy binding and loads favoritesRoomModule plus
legacyFavoritesConsumersModule, which resolves the same new repository singleton.
iOS keeps dataModule's default legacy binding. Do not load both writers.
The old getFavoriteIds/isFavorite methods return no Result and previously treated
malformed rows as empty; the temporary adapter returns an empty membership set
on ordinary read failures so old feed/detail callers do not crash. Its new
repository and getFavorites still report Result.failure; add/remove fail without
overwriting corrupt rows. clearAll propagates failure. Cancellation is never
converted to a Result or an empty set. This legacy fallback must be removed when
those consumers migrate to Result/Flow observation with explicit error UI.
The adapter itself does not emit FavoriteSync events. Android feed and shop
details now observe ObserveFavoriteIdsUseCase from feature domain; failures retain
the last displayed membership, and newly loaded rows are reconciled against the
latest known ID set. On iOS the optional observer is absent and feed keeps its
old FavoriteSync subscription. Existing feed/details still notify FavoriteSync
after their own writes for the legacy iOS favorites screen. Android no longer
renders that screen; remove the event path only after its remaining consumers
are audited and switched.

The membership use case projects/deduplicates ID sets for catalog/detail/session
consumers without exposing saved-card presentation or metadata-only updates.
No one-call load/remove use-case wrappers are introduced. Cancellation is rethrown;
ordinary persistence failures are Result failures. ViewModel scopes are lifecycle
owned. Removal waits for confirmed storage observation; failure keeps the card
and shows a safe localized error, never raw storage exception text.

## Navigation / UI / platform boundaries

Navigation 3 1.2.0 is used by the prepared feature route and its isolated UI
tests. Its AAR requires compileSdk 37, so api/impl and the Android app compile
against 37; target/min SDK remain unchanged. The shared root still owns a
Navigation 2 NavHost and ShopDetail route. Android's Favorites destination calls
FavoritesEntry.Content and translates onOpenShop(id)/onBack into the existing
root navigation events. iOS still renders the legacy screen from the same root
route. This temporary platform difference is isolated to the composition module;
feature UI itself has no Navigator dependency. A separate root migration will
own Navigation 3 NavDisplay, back stack and entry decorators. Do not nest a
second NavDisplay inside the Navigation 2 destination. The API does not publish
a shop route for another business owner.

The new screen covers loading/empty/error/list, retry, separate remove action and
pending-removal disablement. It uses brand primitives and feature-owned cards.
Distance is a host-injected label from saved coordinates: the feature never
reads device location or permissions. Android composition reuses the app's
existing distance calculation, reads last known location only if permission
was already granted, and shows no distance otherwise. No permission prompt or
location service is copied into the feature. Kamel renders URLs in UI; it is
not a favorites HTTP service. The feature-owned card now presents the saved
rating, remove action, roaster logos, open status, brew methods, address,
distance, price label and tag in the legacy card's visual hierarchy. It does
not import feed UI or legacy CoffeeShop. Exact parity is impossible from the
saved format: it contains neither `isNew` nor shop type; the old screen also
cannot reconstruct those values from saved rows. The BYN price symbols and
photo placeholder art still differ. All saved fields remain in data/domain.
Feature-owned common Compose resources now hold the Russian UI and accessibility
strings. A second source locale has not been confirmed; translation, RTL/large-font
and real-photo UI QA remain gates.

Domain/data declare and compile iOS simulator variants with no Android APIs,
Koin or native iOS implementation. Android api/impl plus composeApp are the tested UI/composition
boundary at this stage. For native SwiftUI, share domain/data through a future
framework/bridge, adapt suspend/Flow/Result at that boundary and supply native
storage/lifecycle/navigation; do not expose Compose VM/Android NavDisplay to Swift.
Compose iOS UI is a separate choice requiring native variants of its dependencies
(including design-system), testing and platform integration. No speculative
expect/actual declarations, Swift code or framework export are added now.

## Tests, previews, remaining integration

Contract tests cover legacy JSON/default/logo compatibility, all saved fields,
ordering/deduplication, last removal, corruption, concurrent writes, observation,
Result/cancellation, membership projection, VM lifecycle/pending/failure/retry,
route serialization, and an isolated Koin/setting bridge graph.
Legacy-adapter tests cover shared reads/writes, full snapshot/location mapping,
old singular-logo rows, corruption, failure and cancellation propagation.
Android fixtures exercise screen states/callbacks and a real NavDisplay/entryProvider
with saveable and VM-store decorators. They use fake data and no network/real DB.
App-level tests exercise feed/detail membership changes both before and after
their initial shop responses, including reconciliation of late-loaded rows.
Android instrumented app tests also verify that the new screen adapter sends
shop-open and Back actions to the existing root Navigator. Feature UI tests
verify coordinate forwarding and missing-coordinate handling; app unit tests
verify formatting and absent/invalid location. An app-level DI instrumented test writes
legacy JSON to the actual Room settings table, closes and reopens the database,
then verifies the new repository and legacy adapter share that persisted row
and one writer. It uses a uniquely named test database, not user data. This
does not validate a full signed-in user journey or historic Room migrations
from schema versions 1/2. FavoritesScreen.kt now keeps paired multiplatform
light/dark previews for content, closed, loading, empty and error beside its
stateless ScreenContent; each extracted component has previews in its own file,
without DI, network or actual photo URLs. UI tests render light and dark; IDE
preview pixels have not been manually inspected in this environment.

Android DI supplies the adapter from the same singleton to existing consumers,
including ShopRepositoryImpl and UserSessionCleaner. Feed/details observe
membership directly. The Android screen is now wired for shop navigation,
Back and an optional distance label without a new permission prompt. The card
uses all presentation fields actually persisted by the old format, but real
photos/logos, RTL, large fonts and visual details need device review. Validate
a signed-in journey with pre-existing favorites, transitions, Back, process
restoration and IME before merging this integration.
Root Navigation 3 migration, final FavoriteSync removal and legacy UI deletion
remain separate follow-up steps. Native iOS UI remains later.

Reference: [Navigation 3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3).
