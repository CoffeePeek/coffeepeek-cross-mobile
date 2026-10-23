# Discovery: legacy-first inventory

Date: 2026-10-23. Base: `feature/contributions-audit` (stack tip, main of 2026-10-23 merged in). Read-only pass: nothing is changed in code.

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
- **Not yet**: a device pass with the fixtures (the phone was locked), the details of the generated shops, the parity tests of the legacy view models (step 1).

