# Discovery: legacy-first inventory

Date: 2026-10-23. Base: `feature/contributions-audit` (stack tip, main of 2026-10-23 merged in). Read-only pass: nothing is changed in code.

## API contract update — 2026-10-26

`POST /api/v1/discovery/search` has been removed, including its Gateway route, DTOs and OpenAPI schemas. There are no calls to this route in this checkout. The nearby route used here is the separate `POST /api/v1/coffee-shops/search`.

`GET /api/CoffeeShops` has been removed entirely, including its use by the check-in picker. Its list response and pagination are no longer an active contract. Both ordinary search and nearby search use only `POST /api/v1/coffee-shops/search`. Ordinary requests omit origin, radius and sort; nearby requests send the origin, 3 km radius and distance sort. See [the client contract](../../docs/PUBLIC_ADDRESSES.md#поиск-кофеен-изменение-контракта-от-26102026).

Migrated Discovery, the legacy repository used by the check-in picker/iOS, and Sandbox now use the POST page (`data.items`) for every shop search. The GET-specific request, response DTOs and Sandbox routes are removed. Every quick filter now reloads the server search, including ordinary search, so newness and pagination no longer depend on fields absent from the POST card. Consent/account gates and analytics ownership stay with their existing view models. JSON filter enums follow the backend contract (`specialty`/`coffee-bar`/`cafe`, `cheap`/`moderate`/`expensive`); the legacy fourth price band maps to `expensive` because the API has three bands. The response omits coordinates, price, kind and newness: cards use the existing requested-kind/newness fallback, while both map view models resolve coordinates through the existing details read when a suggestion is selected. Failure keeps the suggestions available for retry; a changed query cancels the selection request.

The implementation and verification records below describe the earlier contract.

### Verification — 2026-10-26

- Final runtime code: `905cc578`, based on develop `381c6cc2`, verified on Windows. Build `1.1.1.2462` / code `2462`; `directSandbox` package `com.coffeepeek.sandbox`, `directDebug` package `com.coffeepeek`.
- `assembleDirectSandbox` and `assembleDirectDebug` passed, including Sandbox isolation and both packaged DEX runtime-class checks. No APK was installed or launched; no device was used.
- Unit/contract suites: legacy network **85**, legacy data **69**, Discovery data **19**, Discovery impl **60**, selected application suites **71**: **304 passing, zero failures/errors/skips**. The application selection was `SandboxApplicationBackendTest`, `MapViewModelParityTest`, `DiscoveryAdaptersTest`, `FeedViewModelParityTest`, `FeedNearbySelectionTest`; it executes in-memory/non-UI tests through `testDirectDebugUnitTest`. Unchanged module suites were up to date in the final run after passing on the same develop-based publication branch. All Gradle runs used `--max-workers=6`, sequentially.
- Retained initial failures: requesting `testDirectSandboxUnitTest` failed task resolution (that task does not exist; no tests ran). The first application run on the migration checkout had **71 tests / one failure**: the new positive map-selection test used a details fixture without coordinates and timed out. Its fixture was corrected; the subsequent develop-based checks and final quick-filter check passed. This was a fixture correction, not a passing retry of unchanged code.
- Reviewed route removal, DTO containment, source sets and dependency boundaries; no Gradle dependency or domain implementation changes. Existing AGP/KMP, legacy preview and unrelated opt-in warnings remain; Android Studio inspections were unavailable.
- Remaining gates: device startup/manual Sandbox review (themes, sizes, accessibility, map selection and back/cancel), iOS compilation/device behavior, and read-only live API contract acceptance. Fake tests and APK assembly do not certify these gates.

Discovery is the **Поиск** tab: the shop search with quick filters and the full filter sheet, the discovery sections (mini map, nearby/found shops, roasters), the
expandable map with its own search, zones and shop carousel, and the shop card that the catalogue shows. The roaster sections it hosts are already a migrated owner
(`feature/roaster`, shared by Android and iOS); they stay a slot here.

## (1) Where it lives and who enters it

| Area (under `composeApp/src`) | Lines | Role |
|---|---|---|
| `commonMain/.../ui/screen/feed/FeedScreen.kt` | 582 | tab body: header, quick filters, discovery sections, search results, map choreography |
| `.../ui/screen/feed/FeedViewModel.kt` | 562 | search, filters, city, nearby, paging, favorites, analytics |
| `.../ui/screen/feed/ShopFiltersScreen.kt`, `ShopTagGroups.kt`, `CatalogSuggestionCard.kt` | 180 + 50 + 44 | filter sheet (price, roasters, brew methods, tags), tag groups, «Не нашли?» card |
| `.../ui/screen/map/MapScreen.kt`, `MapViewModel.kt`, `NearbyMapShops.kt` | 765 + 596 + 37 | preview and expanded map, map search, zones, carousel, bottom sheet |
| `.../ui/screen/main/DiscoveryTabContent.kt` | 106 | hosts feed + map + the roaster slots, overlay analytics |
| `.../ui/component/SearchHeader.kt`, `DesignFilterChip.kt`, `ui/model/CoffeeFocusUi.kt` | 102 + 58 + 14 | search field with shops/roasters switch, chips, coffee focus options |
| `.../feature/catalog/{ui,api}` | 625 + 33 (+36 + 12) | shop and roaster card (`ShopCardContent`, `RoasterCardContent`), brew method catalog port |
| `.../map/*` (common) | 33 + 83 + 90 + 14 | `expect CoffeeMap`, JSON for the Swift renderer, a clusterer, zone colors |
| `androidMain/.../map/*` | 895 + 412 + 6 | MapLibre canvas, bitmap markers, style override |
| `iosMain/.../map/*` | 260 + 45 | native map bridge |
| `.../location/*` | 83 | `PlatformLocation`, permission effect, distance formatting |

About 5,700 lines. Not Discovery, but near it: `ui/screen/roaster/*` (migrated owner), `feature/favorites`, the shop detail (migrated owner) that opens the map through `Navigator.openShopOnMap`.

Entry points: `MainScreen` tab `FeedTab` and iOS `PlatformRootNavigation.ios.kt` (both `DiscoveryTabContent`), `Navigator.pendingMapFocus` (a one-shot request from the shop detail «Показать на карте» that
selects the Search tab and focuses the map), the shop card of the roaster detail, favorites, the check-in shop picker and the public profile (they reuse `ShopCard`; they are not Discovery).
**`FeedViewModel` is also the view model of the legacy check-in shop picker** (`ui/screen/shop/CheckInShopPickerScreen.kt` uses search, filters, location and favorites of it), so the picker is a second consumer of the same search.

## (2) Tests and analytics that exist

Tests: `FeedFiltersUiTest` 8, `FeedNearbySelectionTest` 4, `MapSearchTest` 1, `NearbyMapShopsTest` 2, `MapPrefetchTest` 3, `MapClustererTest` 7 (the clusterer has no caller outside tests), `NativeMapStateTest` 3. Nothing covers
the view-model flows (query debounce, race of loads, favorites merge, gates), the paging, the two search paths (plain and nearby), the zone and carousel selection.

Analytics (kept as they are): `SearchTracker(Catalog.Shops)` begin/results/failed/open (list and map variant with `ResultSource.Map`), `FILTERS_OPENED`, `FILTERS_APPLIED` (apply, reset, changed keys), `MAP_INTERACTION`
(`expanded`, `collapsed`, `marker_selected`, `search_selected`, `zoomed`, `zones_toggled`, `loaded`, `load_failed`, `nearby_loaded`, `nearby_failed`), `GEOLOCATION_RESULT` (discovery/map placements), gates `visited_filter` and
`favorites_filter`, exposures `shop_catalog`, `search`, `filters`, `map`, and the overlay screen events (`discovery`, `map`, `roasters`).

## (3) Behaviour to preserve (rules)

### A. Shop search and filters (`FeedViewModel`)
- A1 Catalogs (cities, beans, equipment, roasters, brew methods, shop tags) load first; the city is the saved one or the resolved default; until the city is ready the screen stays loading so the empty state cannot flash. A failed catalog still lets the first search start.
- A2 A change of the saved city reloads the list; the query is debounced 400 ms and distinct; every reset load cancels the previous one.
- A3 Page size 20, `hasMore = currentPage < totalPages`, load more near the end (5 from the last) of the result list, one load at a time, never while loading or refreshing. Pull to refresh reloads from page 1.
- A4 Filters: city, coffee focus (type), open, new, visited, favorites, nearby, price level 1–4, min rating, roasters, beans, equipment, brew methods, tags. The badge counts everything except the city and counts each selected id.
- A5 «Open», «new», «visited» and «favorites» are also applied locally on the loaded page (`visibleShops`) and reload only while nearby is on.
- A6 Visited and favorites need a session: a guest gets the gate event and the sign-in screen, nothing changes.
- A7 The request: trimmed query, the city only when nearby is off, all ids, `origin` only with nearby. Two server paths: a nearby search (with origin) and a plain search; the nearby path marks `isNew` from the filter because the response omits it.
- A8 Favorites: membership comes from a flow of ids when available (else from a sync bus), merged into every page; a toggle is optimistic, one per shop at a time, restored on failure or cancellation unless the observed state already matches; a guest is sent to sign in.
- A9 Error: `Не удалось загрузить кофейни` with retry; a refresh error does not clear loaded shops.
- A10 `showDiscovery` = not searching, blank query, no filter except nearby. Clearing the search clears the query and the filters, keeping the city.

### B. Nearby and location
- B1 The permission effect asks once; granted refreshes the location, denied tracks `GEOLOCATION_RESULT`. A location outside latitude ±90 / longitude ±180 is ignored.
- B2 The «Рядом» chip appears only with a known location; `onUserLocationChanged(..., selectNearbyOnFirstLocation)` can select nearby on the first location, but only the legacy check-in shop picker passes `true`; an origin change clears the list while nearby is on.
- B3 Distance on a card: the server distance, else computed from the user location and the shop point.

### C. Discovery and results layout (`FeedScreen`, `DiscoveryTabContent`)
- C1 Discovery: a 240 dp mini map (the map is drawn by an overlay that follows the list item), «Кофейни» with «Показать все» (activates the search), up to 8 shop cards in a row (280 dp), «Обжарщики» with «Показать все» and the roaster section slot.
- C2 Search/filter active: quick filter row stays, the list shows shop cards with a floating-nav clearance; with a query the roaster results come first (no «Показать все» there), then «Кофейни».
- C3 Empty shops (and no error) show the shared suggestion card «Не нашли нужную кофейню?» with «Добавить кофейню» (new in main, replaces a plain message); the last list item is the same card while not loading and without an error.
- C4 Search focus activates the search (hides the discovery and the quick row stays), back or the cancel button clears focus, the keyboard, the query or filters; back also collapses the map.
- C5 The roaster list overlay (`showRoasters`) fades in over the shop search with a slide; it is dismissed by cancel (clears the search) and by a pending map focus.
- C6 Photo prefetch of the first 8 (discovery) or all (list) shop photos; none while the map is expanded.
- C7 Filter sheet: price, roasters, brew method, tags (grouped), «Сбросить» (enabled with a selection) and «Готово»; opening it loads catalogs again when they are empty.
- C8 Quick filter chips: Рядом, Открыто, Новые, Уже был, Избранное, then the coffee focus options (one selected at a time, tapping it again clears it).

### D. Map (`MapScreen`, `MapViewModel`)
- D1 Bounds change: the viewport is remembered always; a request starts only after 250 ms and not when the new viewport is inside the already loaded area at the same zoom level. The requested area is the viewport plus half a viewport (at least 3 km) on each side, clamped to ±85/±180.
- D2 Result: shops, clusters and zones from the server; shops of the previous load are not kept except the selected one and the nearby set; zones seen for the city are kept (the API returns the viewport's zones only) and reset on a city change; `isTruncated` shows «Слишком много объектов — приблизьте карту». A failed load shows «Искать в этой области».
- D3 Camera moves caused by the app (zoom buttons, shop selection, location) pause the bounds updates for 500–700 ms and load once where the camera ended.
- D4 Nearby carousel: a request in a 5 km box around the origin (zoom 22, the saved city), the nearest 10 within 5 km sorted by distance then id; the origin comes from the permitted location or from the first map center.
- D5 Selection: a marker or a carousel card selects a shop (details are loaded once and cached, a late answer for another selection is dropped); the carousel card also moves the camera to zoom 16; a zone selects a zone card with «Показать кофейни» (zoom 14.5).
- D6 Map search: 350 ms debounce, 3 suggestions, shops without coordinates are dropped, a stale answer is ignored, selecting one moves the camera to zoom 16 and selects it.
- D7 Zones can be toggled; zooming is ±1 within 3..20; «Моё местоположение» asks the platform map to find the user (zoom 15, nearby origin update).
- D8 In the preview the markers open the shop directly, in the expanded map they select; the map tab analytics exposure differs for the two.
- D9 `Navigator.pendingMapFocus` (from the shop detail): the shop is added to the map, selected and focused at zoom 16, the request is consumed, search is dismissed, the map expands.
- D10 Expansion choreography: the preview rectangle follows the mini map item while scrolling, interpolates to the full screen (420 ms), the header fades and slides away and the list slides down; the map is clipped under the separator; semantics of the covered layers are cleared.

### E. Shop card
- E1 `ShopCardContent`: cover photo, rating and a favorite control over the photo (read-only marker when the action is null), title, open status, address, distance; roaster logos, brew methods and tags only with `showCatalogDetails` (the discovery row turns it off); compact and non-compact sizes. Main (2026-10-22) tightened the control row (padding 4 dp) and made the non-compact favorite badge 48 dp.

## (4) What the legacy code depends on (to be replaced by ports)

| Dependency | Used for | Plan |
|---|---|---|
| `ShopRepository` (`searchShops`, `getMapContent`, `getCatalogs`, `getShopDetails`) | search, map, catalogs, map sheet | Discovery gets its own **read** repository (search, nearby search, map content, catalogs); the map sheet reads the shop through a narrow port over the migrated Shop owner (`ShopDetailsRepository`), not the legacy interface |
| `FavoriteRepository`, `ObserveFavoriteIdsUseCase`, `FavoriteSync`, `FavoriteInteractions` | membership, toggle | port `DiscoveryFavorites` (membership flow + toggle result) adapted by the app over the favorites owner |
| `SessionRepository` | the two gates and the favorite toggle | `DiscoveryAccount` port (account key flow, guest check) as the other owners; sign-in is a callback |
| `CityPreference` | saved city | port `DiscoveryCity` (resolve over the loaded cities, observe) adapted over the settings owner's city port |
| `Navigator` (`navigate`, `pendingMapFocus`, `consumeMapFocus`, `openShopOnMap`) | gates, opening a shop, map focus | callbacks; the map focus becomes an entry parameter plus a consumed callback, `Navigator` stays the one-shot owner |
| `PlatformLocation`, `LocationPermissionEffect`, `rememberPermittedUserLocation`, `UserDistance` | location | port `DiscoveryLocation` (permitted location, distance) and a permission-effect slot |
| `CoffeeMap` (MapLibre / native Swift), marker icons, `MapClusterer` | map canvas | **app-owned canvas slot** (the map SDK is not an owner concern); the clusterer is unused and is not moved |
| Analytics singletons (`Analytics`, `SearchTracker`, `analyticsExposure`) | all events above | analytics port with the same names and properties, adapters in the app |
| Legacy design system (`Cp*`, `GlassIconButton`, `CpSearchField`) | UI | `core/design-system` components; the glass map controls and the search field get design-system versions in the impl step |

## (5) Findings worth deciding before the code moves

1. **Two view models repeat the same work**: `FeedViewModel` and `MapViewModel` each load catalogs, resolve the city, build the same `ShopFilters` and keep near-identical filter models (`FeedFiltersUi`, `MapFiltersUi`).
2. **Navigation and session inside view models** (`Navigator.navigate(Auth)` from the gates and the favorite toggle).
3. **Hard-coded Russian strings** in every screen and view model (labels, hints, errors, content descriptions).
4. **No one-account-at-a-time rule**: favorites, visited and the favorite flags of the loaded shops belong to the account; a session change keeps old flags until the next load.
5. **`MapClusterer` is dead code** (only its test calls it): clusters come from the server.
6. **Two paths of the shop search** (plain and nearby) map to different DTOs with different fields (`isNew`, `type`, `cityName`, `priceRange`); the nearby path fills fewer fields.
7. **The local filtering of open/new/visited/favorites** (`visibleShops`) can disagree with a server-side page: the page is filtered after loading, so a page may show fewer than 20 shops.
8. **The error text is one fixed string** and a refresh error replaces nothing; the map shows no error text, only the «Искать в этой области» button.
9. **The map preview choreography** depends on list item offsets and several `onGloballyPositioned` anchors in one 580-line composable; it is hard to test.
10. **Map shop details** are fetched with the full legacy `getShopDetails` just for the bottom sheet (title, address, hours, photo).
11. The shop card is used by five legacy places outside Discovery; a design-system version is needed to stop copying it.
13. **A query typed in the first 400 ms of the view model's life is swallowed**: the search listens to `queryFlow.debounce(400).distinctUntilChanged().drop(1)`, and when nothing is typed the first debounced value is the initial blank one that `drop(1)` is meant to skip; a value typed before that window closes becomes the first one and is dropped (found by the parity tests, the tests wait for the first window). The new view model must not copy this.
12. **The check-in shop picker reuses `FeedViewModel`** (search, filters, location, favorites). Moving the search to a new owner leaves the picker on the legacy view model until the Shop owner or iOS migrates it; the legacy view model cannot be deleted with Discovery.

## (6) What changed in main since the last sync and belongs here

From main of 2026-10-22/23 (merged in the stack): the shared `CoffeePeekCatalogSuggestionCard` (design system) and the app's `CatalogSuggestionCard` binding (shop kind with «Добавить кофейню»); empty shop results show it instead of a plain message; the roaster
«Показать все» action is gone from the combined text search; the mini map follows the list item offset (`discoveryMapPreviewBounds`) and the map opacity animation was removed; horizontal paddings moved into the list items so cards scroll edge to edge;
the shop card control tightening (E1); the root navigation no longer pads the bottom inset (every page handles it: the lists here use the floating-nav clearance, a new screen must also add the system inset); the coffee list clip fix. The roaster owner now renders the
empty state with an «Добавить обжарщика» action through a composable port.

## (7) Decisions to confirm (recommendation first)

| # | Question | Recommendation |
|---|---|---|
| Q1 | Module shape | one owner `feature/discovery/{api,domain,data,impl}` for the shop search, filters, the map and the shop card; the roaster sections stay slots filled by the app |
| Q2 | View models | keep two (search, map), share one `DiscoveryFilters` model and one catalog loader/query builder in `domain` |
| Q3 | Data | Discovery owns search, nearby search, map content and the catalogs read; the app adapters of Contributions can move to it later |
| Q4 | Map canvas | an app-owned slot with a small map port (bounds/zoom/camera/selection events); no map SDK in the owner; the clusterer is not moved |
| Q5 | Map details | a port over the Shop owner's details repository for the bottom sheet |
| Q6 | Account | a guest sees the sign-in callback for visited/favorites; a change of account clears the account-bound state (favorite and visited flags, favorites-only/visited-only filters) and reloads; late answers are dropped |
| Q7 | Navigation | callbacks (`onOpenShop`, `onSignIn`, `onAddShop`); `pendingMapFocus` as an entry parameter + consumed callback |
| Q8 | Location | port `DiscoveryLocation` + permission-effect slot; distance formatting in `impl` |
| Q9 | Errors | the server message or a resource default, never a transport text; keep the fixed default for the list |
| Q10 | Shop card | extract `CoffeePeekShopCard` to the design system (with the main tightening) and use it in the new Discovery only; the legacy copies stay until iOS migrates |
| Q11 | Choreography | the expansion maths stays pure functions in `impl` (testable); composables use them |
| Q12 | Check-in shop picker | stays on the legacy `FeedViewModel` (legacy files are deleted only with iOS); a later step may give the picker the Discovery search use case through a port |
| Q13 | Slicing | inventory (this PR), Sandbox fixtures, parity tests, api+domain+data, impl search, impl map, impl tab composition, app connection + device check, audit — each ≤40 files |

## (8) Plan and PR slicing (each step on its own stacked branch)

0. Sandbox: check what the fake backend serves for the shop search (filters, paging, nearby search with origin), the map content (shops, clusters, zones, truncation), catalogs, and add the gaps with failure fixtures; unit tests like the other owners.
1. Parity tests on the legacy code: filters model and badge, the request mapping of both search paths, favorites merge and restore, the load race, the gates, bounds prefetch/contains, nearby selection, map search race, zone selection, the preview bounds maths.
2. `api + domain + data` (own models, repository, mappers, tests).
3a. `impl`: search list, quick filters, filter sheet, shop card (+ design-system card). 3b. `impl`: map (view model, overlay UI, canvas slot). 3c. `impl`: tab composition with the roaster slots and the expansion choreography.
4. App connection (`DiscoveryScreenRenderer`, `MainScreen`, ports, canvas slot over `CoffeeMap`, iOS keeps legacy) and a Pixel 7 Sandbox check.
5. Retroactive audit and the legacy map update (nothing is deleted before iOS migrates).

## (9) Not checked in this pass

Live search/map endpoints and their DTO details, the real MapLibre style and tiles, the native iOS map renderer and its Swift side, real location, large font and TalkBack, performance of the map choreography on low-end devices. They stay explicit gates for the later steps.

## Step 0 result (Sandbox fixtures)

- **Before**: the Sandbox answered the plain search with the two core shops and ignored every filter and paging, the nearby search honoured only open/new/visited/favorites without distance, radius or paging, and the map returned one marker without bounds, zones or truncation. Nothing could fail.
- **Now** (`SandboxDiscoveryBackend`, mode `sandbox_discovery`): `Seeded` keeps the old answers; `Paged` has 47 shops in two cities with all filter dimensions, paging, the nearby search by distance and radius, the map by bounds and filters and two zones; `Truncated`, `Empty`, `SearchFailure`, `MapFailure` as in `composeApp/SANDBOX.md`.
- **Tests**: one case in `SandboxApplicationBackendTest` goes through the real `ShopApiService` (paging 47/3/20/7, city, text, roaster, rating and type filters, an empty intersection, distance order and radius, paging of the nearby search, the shared favorites, map by bounds with zones, no clusters, truncation, empty, both failures and Reset). The class has 26 cases and all pass.
- **Device pass** (Pixel 7, `Paged`): the mini map draws the markers of the fixture (cup, bean and cutlery icons by kind), the discovery row shows the shop cards (core shops with their price and kind), a text search lists «Discovery Shop 1x» with the suggestion card at the end, the roaster section shows the suggestion card for an empty result. Not checked: filters, the expanded map and the zones on the device.

## Step 1 result (parity tests on the legacy view models)

- **`FeedViewModelParityTest` (19)**: the first search waits for the catalogs and uses the saved city (a removed one falls back to the first); the query is debounced, trimmed and sent once, a blank one is sent as no query; every filter reaches the request and nearby drops the city; paging appends pages, stops at the last page and ignores `loadMore` while a page loads; a newer reset load cancels an older one (a late answer is ignored); a failed refresh keeps the shops and shows `Не удалось загрузить кофейни`, a failed first load shows it without shops; a guest is sent to sign in for visited and favorites and nothing changes; the quick filters filter the loaded page locally and reload only with nearby; clearing keeps the city and drops the query; cancelling the search clears filters or the query; a change of the saved city reloads; the favorite is optimistic, one per shop and restored on failure, kept on success, a guest goes to sign in; invalid locations are ignored.
- **`MapViewModelParityTest` (13)**: the bounds load is debounced, uses the prefetch area and the city and skips pans inside it; zones seen in the city are kept and forgotten on a city change; a failed load offers «Искать в этой области» and a retry works; the nearby carousel keeps the ten nearest inside the radius (ties by id) and merges them into the map, invalid origins are ignored; a late details answer for another selection is dropped and details are cached per shop; the map search is debounced, trimmed and ignores a stale answer, drops results without coordinates and shows a failure; a search result clears the search and moves the camera to zoom 16; a carousel card and a shop focus select the shop at zoom 16; zoom moves the camera to the viewport center within 3..20; a zone card zooms to 14.5 and the zones can be hidden.
- **Not covered here** (stays for the new tests): the favorites-membership flow path (`ObserveFavoriteIdsUseCase`), the expansion choreography maths beyond the existing tests, the real clock-driven pause of the bounds updates after camera moves, anything that needs a device.
- Together with the earlier 28 legacy cases the Discovery view models have 60 pinned cases. They are time based (real debounce windows) and were run three times without a failure.

## Step 2 result (api + domain + data)

- **Modules** `feature/discovery/{api,domain,data}` registered in `Modules.kt` (`impl` comes with step 3a).
- **domain**: `ShopSummary` (the card model with the kind, the price level, the tags, the distance and the coordinates), `ShopKind`, `PriceLevel`, `Coordinates`, `DiscoveryFilters` (the same fields as the legacy filter model; `activeCount`, `clearSelections`), `ShopQuery` (text, filters, optional origin, page), `DiscoveryCatalogs`, `MapBounds`, `MapMarker`, `MapZone`, `MapContent`, `Paged`, `DiscoveryFailure` (sign-in required or other, with the server message); rules: haversine `distanceMeters`, `expandedForPrefetch` / `contains` / `center` / `nearbyBounds` / `nearestMarkers` / `clampZoom`, `visibleShops` (the local switches of the plain search), `withFavoriteMembership`, `showsDiscovery`, `searchText`; `DiscoveryRepository` (`search`, `mapContent`, `catalogs`). 11 tests.
- **data** (contract at this step; superseded by the 2026-10-26 update above): `createDiscoveryRepository(client, favoriteIds, roasters)` over `GET /api/CoffeeShops` (the plain search: text, city, kind, price, rating and the id lists; **never** open/new/visited/favorites, as in the legacy code), `POST /api/v1/coffee-shops/search` (the nearby search: origin, radius 3 km, the switches), `GET /api/Map` (shops at zoom 22, zones at the visible zoom with a fallback to the shop answer; PascalCase names accepted; the server's clusters are not read), and the six catalogs. Own DTOs and mappers (no dependency on `modules/network`); every non-2xx answer is a typed failure with the server message; the catalogs are read once and kept, and a load runs in the repository's own scope so that a caller that stops waiting does not cancel the others (the legacy load did). 17 tests with a mock engine.
- **api**: the ports `DiscoveryAccount`, `DiscoveryFavorites` (ids flow, ids now, set), `DiscoveryCity` (selected flow, resolve) and `DiscoveryLocation` (current position flow, refresh). The analytics port and the Android entries come with the impl step.
- **Decisions taken here**: the roaster list of the filters is a function the application passes (it owns that catalog and its disk cache); the favorites of the account decide the favorite flags of the plain search and a failing source counts as none; the nearby rows carry what was asked for where the server is silent (kind, new, visited) exactly as the legacy search did, which means the kind of a nearby row is the requested one or the default; a map marker without a title gets an empty title and the UI picks the text; the caller drops the city for the nearby search (the repository sends what it is given).
- **Not covered**: live contracts (the exact answers of the real server for the nearby search and the map), the roaster catalog read, the favorites flow.


## Step 3a result (impl: the shop search)

- **Module** `feature/discovery/impl` registered in `Modules.kt`; the entry is `createDiscoverySearchEntry(repository, favorites, city, location, account, analytics, host)` (`DiscoverySearchEntry` and `DiscoveryHost` live in `api/androidMain`; the analytics port `DiscoveryAnalytics` in `api/commonMain`).
- **`SearchViewModel`** (commonMain, `MviViewModel`): the legacy feed behaviour (debounced query, filters, city, nearby, paging, optimistic favorite) with three differences: one account at a time (a change of account drops visited/favorites and reloads, a guest gets the sign-in event and the gate analytics), a query typed in the first debounce window is no longer swallowed (finding 13), a failed search shows the server's message or the default text. A favorite restore yields to what the favorites of the account already say. 28 tests with fakes of the ports and virtual time.
- **UI** (androidMain): search field with the filter button and badge, quick filter chips (open, new, visited, favorites, nearby only with a position), results list with pull to refresh, paging, loader, error with retry, empty state and the «Не нашли?» card, `CoffeePeekShopCard` binding, the filter sheet (kind, price, rating, roasters, beans, equipment, brew methods, amenity tags) editing a draft that «Готово» applies with one reload. New design-system pieces: `CoffeePeekFilterChip`, `CoffeePeekShopCard`. Strings are `discovery_*` in `impl/src/androidMain/res`.
- **Checked**: domain 12 and impl 28 unit tests green; impl, api and design-system compile for Android and iOS simulator.
- **Not covered here**: the UI on a device (comes with the app connection in step 4), large font and TalkBack, previews of the sheet, the roaster slots of the text results (step 3c), the discovery content above the list (3c).

## Step 3b result (impl: the map)

- **api**: `DiscoveryShopDetails` port (the card of the selected shop: photo, rating, review count, open flag and the formatted hours; the Shop owner backs it and the application formats the hours), `MapFocus`, `MapCamera`, `MapCanvasState` / `MapCanvasCallbacks` (the contract of the app-owned canvas, no map SDK in the owner), `DiscoveryMapEntry` (androidMain) and the `mapCanvas` slot of `DiscoveryHost`. `DiscoveryAnalytics` gained `mapInteracted` (the legacy `map_interaction` actions), `mapSearchStarted` and `mapShopOpened(fromSearch)`.
- **`MapViewModel`** (commonMain): the legacy map behaviour: bounds loaded 250 ms after they settle with the prefetch margin (a pan inside the loaded area at the same zoom loads nothing), the middle of the first view is the carousel origin until a position is known, zones seen in a city are kept and forgotten on a city change, the ten nearest shops inside 5 km feed the carousel, the search suggests three shops with coordinates (350 ms, a stale answer is dropped), camera moves pause the viewport updates (700 ms, 500 ms for zoom) and load where the camera ended up, details are cached per shop and a late answer for another selection is dropped (a dropped one is asked again). 19 tests with virtual time.
- **UI** (androidMain): search field with suggestions, the «too many objects» notice, zoom control, zones switch, location button, «Искать в этой области», the looping shop carousel (selected shop first, swipe selects), the zone card with the Russian plural of the shop count. Design system gained `Minus` and `Polygon` icons.
- **Checked**: impl 47 tests green (search 28, map 19); impl, api and design-system compile for Android and iOS simulator.
- **Not covered here**: the UI on a device and the canvas slot (step 4), the preview mode of the map on the tab and the expansion choreography (step 3c), the expand/collapse analytics (3c), large font and TalkBack.

## Step 3c result (impl: the tab)

- **api**: `DiscoveryTabEntry` (androidMain) with `DiscoveryRoasterSlots` (`preview` for the roaster section, `search` for the full roaster search laid over the tab) and `DiscoveryRoasterContext` (query, selected roasters, favorites-only: what the roaster owner filters itself by); new `DiscoveryHost` slots: `retained` (native content stays mounted but paused while hidden), `locationPermission`, `photoPrefetch`, `reduceMotion`; `MapCanvasState.isPreview`; `DiscoveryOverlay` and `overlayShown` in the analytics port (the legacy `screen_shown` names `discovery`, `roasters`, `map`); exposures `MapPreview` and `MapFull`.
- **`tab/Choreography.kt`** (commonMain, own `Frame` type, no UI toolkit): `previewFrame` (the map follows the mini-map item as it scrolls and parks above the viewport once the item is recycled), `expandedFrame` (preview to the whole screen), `discoveryMapOffsetY` (the map slides away as the shop list takes over), `mapClipTop`, `mapCornerRadius`. 5 tests ported from `FeedFiltersUiTest`.
- **`DiscoveryTab`** (androidMain): the search layer and the map layer share one screen. The map is the same composable in both states (small with an expand button, full with search and carousel), clipped under the separator, the corners flatten while it expands, the header fades up and the list slides down, covered layers drop their semantics, back collapses the map or cancels the search, a map focus request cancels the search, closes the roaster overlay and expands the map. The roaster search fades in over the shop search. Discovery content: the 240 dp mini-map slot, «Кофейни» with «Показать все» (activates the search), up to 8 cards in a row (280 dp), «Обжарщики» with its slot; results with a text show the roaster section first. The `AnimatedContent` slide between discovery and results (360 ms) moved into `SearchContent`.
- **Search view model**: `LocationGranted` / `LocationDenied` actions (the screen's permission effect reports into it), the `geolocation` analytics with the `discovery` placement for every new valid position. 29 tests.
- **Checked**: domain 12, impl 53 (search 29, map 19, choreography 5) unit tests green; impl, api, design-system compile for Android and iOS simulator.
- **Not covered here**: everything on a device (the animation, the canvas, back handling, the roaster overlay), reduce-motion, large font, TalkBack; the roaster slots and the map canvas are wired in step 4.

## Step 4 result (app connection)

- **Selection**: `DiscoveryScreenRenderer` (commonMain, `ui/screen/main`) with `LegacyDiscoveryScreenRenderer` (the old `DiscoveryTabContent`) as the default; `MainScreen` and the iOS root call it through Koin (`discoveryRendererFactory` of `initKoin`). Android replaces it in `PlatformKoin.android.kt`; iOS and shared code stay legacy.
- **Android wiring** (`composeApp/src/androidMain/.../di/discovery`): `AndroidDiscoveryScreenRenderer` (wraps the entry in `CheckInTabContent`, hides the create button while the map covers the screen, maps `Navigator.pendingMapFocus` to `MapFocus` and consumes it, navigation to shop, sign-in and add shop stays here), `DiscoveryAdapters.kt` (account key like the other owners, `AppDiscoveryFavorites` over the favorites owner + `FavoriteSync`, `PreferenceDiscoveryCity` over the legacy city preference, `AppDiscoveryLocation` from `PlatformLocation` while the permission is granted, `ShopDiscoveryDetails` with the legacy hours summary, the roaster catalog for the filters), `AppDiscoveryAnalytics` (the legacy event names and properties; a search tracker for the list and one for the map, `criteria` keeps a refresh from being a new search), `DiscoveryHost.kt` (photo loader and placeholders, brew method icons, the mascot, analytics exposure, haptic, `RetainedContent`, permission effect, photo prefetch, reduce motion and the `CoffeeMap` canvas: markers and zones converted to the legacy map models).
- **Tests**: `DiscoveryAdaptersTest` (5: account key, favorite snapshot and announcement, failure not announced, ids skip a failing answer, roaster catalog).
- **Fixes found on the way (in impl)**: the roaster section above a text search pushed the list out of view (the viewport stays on the first shop): the list scrolls to the top when the section appears; the result list got its top padding and the 88 dp bottom clearance of the legacy list; the «Не нашли нужную кофейню?» copy is the application's, the brew method chips show two methods and «+N» like the card did, the catalog exposure sits on the heading only.
- **Device check (Pixel 7, Sandbox `Paged`)**: the mini map with markers, expand to the full map with zones and the controls, a marker selects and the card shows, the map search suggests three shops and picking one moves the camera to zoom 16, the filter sheet lists every catalog and applying «Кафе» turns the discovery into the filtered list with the badge, the text search shows the roaster section.
- **Finding 14 (inherited, not fixed)**: before a position is known the middle of the first viewport is the origin of the nearby carousel; on the first (world-wide) viewport of the map that is far from every shop, so the carousel stays empty and the card shows distances like `6508 км`. The legacy map does the same. A fix needs a product decision (ignore the viewport origin, or wait for the camera to settle on the city).
- **Not checked**: favorites and the guest prompts on a device, the check-in create button, back handling, TalkBack and large font, live API contracts, iOS (legacy by design), the real map tiles and location.

## Step 5 result (audit against the legacy rules A–E)

Checked rule by rule (sections A–E above) against the new code. Matches without a change: A1–A4, A6–A10 (the view model tests pin them), B1–B2, C1 (discovery content), C4–C7, D1–D10 (view model tests and the device check), E1 for the list card. Gaps found and **fixed here**:

- **C8 quick filters**: the row lacked the coffee-focus chips (Specialty / Кофейня / Кафе, one at a time, a second tap clears), the chips had no icons and «Рядом» was last. Now «Рядом» first, the icons of the legacy chips, then the three kind chips. The kind and the rating sections of the filter sheet (invented in step 3a) are gone: the legacy sheet has neither.
- **C7 filter sheet**: price as the application's slider with the hint («Любая» by default), the roasters, beans, equipment, brew methods and amenity tags as checkmark rows with the roaster avatar, the brew method icon and the tag icon (new `DiscoveryHost` slots `priceSelector`, `priceHint`, `roasterAvatar`, `tagIcon`, and a size for `brewMethodIcon`).
- **C3 empty and last item**: nothing found shows the suggestion card only (the plain message is gone); the card is the last list item whenever nothing is loading and nothing failed (it showed only after the last page).
- **E1 / B3 card**: the discovery row shows no roaster logos, brew methods or tag (`showCatalogDetails = false`); the list card's tag is the first one that is not a brew method; the location line is «адрес • N м от вас» with the server distance or, without one, the distance from the user's position (`SearchState.userLocation`); the distance wording is the legacy one (tenths of a kilometer with a comma whatever the locale); the kind labels are «Specialty», «Кофейня», «Кафе» as on the legacy card.
- **A3 load more**: five items from the end (it was three).
- **Dead code of my own removed**: `DiscoverySearchEntry`, `DiscoveryMapEntry` and their factories (the tab entry replaced them), the plain empty-state strings; `DiscoveryHost.kt` is now the name of the api file that holds the host and the roaster slots.
- Fixed in step 4 already: the roaster section pushing the list out of view, the list paddings.

Open (not fixed, decisions or gates):
- **Finding 14** (inherited): the nearby origin is the middle of the first viewport until a position is known; with the world-wide first viewport the carousel stays empty and distances read thousands of kilometers. Needs a product decision.
- The map card thumbnail uses the large placeholder label (the legacy card used a 7 sp label for the 72 dp thumbnail): visible only for a shop without a photo.
- The shop details port still reads the legacy `ShopRepository` (the Shop owner's repository is the plan), and the check-in shop picker still uses the legacy `FeedViewModel` (finding 12): legacy files stay until iOS migrates.
- Live API contracts, favorites and the guest prompts on a device, large font and TalkBack, the real map tiles and location were not checked. Device recheck of this step (Pixel 7, Sandbox `Paged`, 2026-10-23): the quick row has the icons and the Specialty / Кофейня / Кафе chips, the discovery card shows no catalog details, the filter sheet is the price slider («Любая») plus checkmark rows with the roaster avatars and the brew method icons.
- main and develop moved on after the last sync (launch and splash artwork, iOS marketing version, Android version name, auth spacing, iOS confirmations): they are taken in the planned re-sync after the last module.

## Main sync of 2026-10-23 (pull to refresh and the map preview)

`develop` (`e628de2e`) moved the pull-to-refresh translation of the list from a graphics layer to a layout offset and made the map preview follow the measured slot of the mini-map item (`discoveryMapPreviewBounds`: the measured anchor while the item is laid out, parked above the viewport once it is recycled), so the map follows the pull. The design-system `CoffeePeekPullToRefresh` came with the merge; `previewFrame` of the tab choreography now has the same rule and its test the same cases (scrolled and pulled anchors). **Reverted for Discovery (prod parity):** prod keeps the map pinned while only the shops below are pulled and the loader shows under the map, so `previewFrame` is `viewportTop + item offset` again (the item offset ignores the pull) and the loader is placed under the map with `indicatorTopOffset`. The other commits of this sync (history and favorites fixes, public check-ins by default, roaster badges, iOS sheets, version names, artwork) touch owners that were already migrated or the shared shell and need no port here.

## PR #178 composable review limit — 2026-10-25

Source: `codex/migration-pr178-review-fixes`, based on develop `5a1fe4a1`.
Verification plan: keep the one mounted map, measured preview frame, animation
durations, overlay analytics, permission callback, back/cancel and roaster/search
slots while extracting cohesive components; use existing pure choreography and
ViewModel tests, lint, Android assembly/DEX checks and manual isolated Sandbox.

DiscoveryTab is the lifecycle adapter (9 body lines): it collects state/navigation
events and supplies a map slot to DiscoveryTabContent, which has fake-state paired
light/dark previews without ViewModels/DI/network. Transient overlay/scroll/frame
state and animations have their own UI owner. Search, map, roaster overlay, landing
list, shop carousel and section title have named components in their owning files.
The native map remains mounted through the same injected retained-content host.
Existing analytics exposure/event names, initial-overlay suppression, click ownership
and permission/session boundaries are preserved; no analytics service is introduced.

Review counted nonblank/noncomment body lines including nested blocks: all extracted
composables are at most 26 lines (limit 40); ordinary helpers are below 20. The
existing pure choreography functions are unchanged; no size lint/build rule is added.
Discovery impl: 54 executed tests/4 suites, zero failures/errors/skips, including
5 existing choreography tests; lintDebug passed. Together with Contributions,
117 tests passed. DirectDebug and DirectSandbox assemblies, packaged DEX guards
and Sandbox isolation passed in the same 25s Gradle run, max-workers=6.

Manual run: read-only Pixel_5 AVD, Android 37, sdk_gphone16k_x86_64, unlocked,
1080x2340; directSandbox com.coffeepeek.sandbox 1.1.1.2453-sandbox/code 2453.
Sandbox was absent before installation; the real app/data and Studio variant were
untouched. Checked mini-map/full-map layout and back/cancel in light/dark, text
search/cancel, keyboard dismissal and tab retention. Local captures/UI dumps remain
outside tracked sources and contain synthetic data only. Sandbox map tiles are
blank by design (no INTERNET); live map/network acceptance is not claimed.
Verified Sandbox APK SHA-256:
`c42fd8e68d3beaf00a9316312bebc1166ad015d896bd41e498954ad2d7da0454`.

IDE tools/preview rendering were unavailable. Paired Android previews compile.
TalkBack, alternate font/window sizes, real map/permission/backend acceptance and
iOS runtime UI checks remain untested. No automated UI/Compose/screenshot test
was introduced or run, and no camera, production analytics or account write occurred.

## Search result priority — develop publication, 2026-10-27

At the user's request, applied the search fix independently to develop
`be7577b8`; verified code commit `7cb60f56`. Matching shops precede a confirmed
empty roaster section, and both headings use titleLarge. Adapted the Android
feature entry and the active retained iOS FeedScreen without importing the
pending migration stack or map changes. The roaster owner supplies the filtered
result status and keeps query analytics current below the lazy viewport.

Windows verification: Discovery impl 69 tests/6 suites, Roaster impl 23/3,
selected application Feed unit tests 32/4; all 124 passed with zero failures,
errors or skips, including nine new regressions. No failing-test retries.
DirectSandbox and DirectDebug assemblies, both packaged DEX runtime-class
checks and Sandbox isolation passed in 2m48s, Gradle --max-workers=6.
Builds: com.coffeepeek.sandbox 1.1.2.2478-sandbox/2478 and com.coffeepeek
1.1.2.2478/2478. Android paired previews compile; IDE rendering was unavailable.
No installation, device, automated UI, instrumentation or screenshot run took
place. Manual themes/font/window/accessibility/back/cancel, native iOS runtime
and live backend gates remain open; no production account or analytics writes.

## Home header/map gap — 2026-10-27

The Android overlay now adds LazyListLayoutInfo.beforeContentPadding to the
mini-map item offset, preserving the existing 16dp list gap below the header.
Scrolling, pinned pull-to-refresh behaviour and parking recycled map items stay
covered by ChoreographyTest; the retained iOS path already uses the measured
anchor including padding. No screen, resource, dependency or navigation redesign.

Verified on Windows: e385cc3e + this working diff on
codex/migration-home-map-header-spacing. Discovery impl 70 tests/6 suites,
zero failures/errors/skips/retries. DirectSandbox/DirectDebug assemblies, both
packaged DEX guards and Sandbox isolation passed in 2m04s, --max-workers=6;
Sandbox com.coffeepeek.sandbox 1.1.2.2479-sandbox/2479. Existing paired tab
previews compile; IDE rendering was unavailable. No device/install/manual
light-dark/font/window/accessibility/expand-back review, iOS runtime or live
backend checks were performed; no automated UI/instrumentation/screenshot tests
were created or run, and no account or production analytics writes occurred.
