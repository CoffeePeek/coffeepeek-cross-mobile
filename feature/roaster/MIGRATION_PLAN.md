# Roasters — Android-first migration plan

Status: steps 2–5 done in code (2026-10-20): domain, data, api (ports + entries) and impl (typed MVI + stateless UI + entries); **activated on Android** (Discovery roasters and the roaster detail route); iOS/shared keep the legacy screens. Step 4 isolated UI suite passed on Pixel 7 before step 5; device/Sandbox acceptance of the activation is still open (step 6). Child of the Coffee checkpoint
(`feature/coffee-catalog-refactoring`, PR #104); the branch/PR is created only after this plan is accepted.
Same rules as Coffee: preserve the current product, requests and visuals; Android first; shared/iOS keep the
legacy composeApp UI; no new feature in composeApp; modules only with real content.

## What exists today (read from the legacy code, not assumed)

| Piece | Where | Behaviour to preserve |
|---|---|---|
| List | `ui/screen/roaster/RoasterListScreen.kt` (+ ViewModel), used by the Discovery tab's roaster section (`DiscoveryTabContent`) and its own "show all" list | Name search, tag filter chips (from loaded items), favorites-only, selected-roaster filter from Discovery, lazy per-item details load (4 concurrent, per-slug lock), favorite toggle with `FavoriteInteractions` and error message, refresh, `SearchTracker(Catalog.Roasters)` with `results/failed/open(position)`, exposure `roaster_catalog` (catalog and discovery placements) |
| Detail | `RoasterDetailScreen.kt` / `RoasterDetailViewModel` (route `RoasterDetail(roasterId)` = slug) | Load by slug, favorite toggle (id = public slug or id), shop photos enriched through `ShopRepository.getShopDetails` for shops without photos, photo preview dialog, shop tap → `ShopDetail`, Instagram/website links (normalized URLs), `TrackEntityDetail`, interactions `photos/open`, `shops/select` (+ `DetailEntryMemory`), `contacts/instagram|website`, exposure `roaster_contacts` |
| Data | legacy `RoasterRepository` (`modules/domain`, impl in `modules/data`) — **read and write in one interface**: `getRoasters`, `getRoaster`, `submitRoaster`, `getMyRoasterSubmissions`, … | Reads belong to this feature; submissions belong to Contributions (not migrated here) |
| Entry points | `Navigator` routes `RoasterDetail`, `AddRoaster`; `AppLinks` (public roaster links); Favorites roaster cards; Shop detail roaster row; Coffee detail roaster row (already routes via app navigation); My contributions | Routes and links unchanged; `AddRoaster` stays out of scope (Contributions) |
| Favorites | `RoasterFavorites` port in composeApp `feature/favorites/api` + server-backed favorites module | Never restore a local favorites writer; keep account/session guards |
| Sandbox | fake backend already serves `/api/roasters` and detail, one roaster | Needs more fixtures (tags, several roasters, shops without photos, contacts) |

Not part of this migration: roaster creation/edit forms and moderation (Contributions), a roaster app-link
redesign, favorites storage, iOS screens.

## Target boundaries (create only with real content)

| Module | Responsibility | Depends on |
|---|---|---|
| `feature/roaster/domain` | Summary/details/tag/contact models, read-only `RoasterRepository` contract (list, detail) | Kotlin only |
| `feature/roaster/data` | Existing endpoints, DTOs and mappers behind a factory; keep wire names and cache policy | domain, core/network |
| `feature/roaster/api` | Public list/detail/discovery-section entries, analytics + screen-analytics ports, `RoasterFavoritesPort` (narrow), shop-photo lookup port | Kotlin; Compose only in androidMain |
| `feature/roaster/impl` | Typed list/detail MVI, stateless content, components, resources, previews | api, domain, core/presentation, design-system |

Cross-feature rules: favorites and shop photo enrichment come in through **ports supplied by composeApp**
(adapters over the existing favorites repository and Shop repository); navigation, browser, share and the
image viewer are app-owned callbacks (same pattern as Coffee entries and `CoffeeScreenRenderer`).
The legacy `RoasterRepository` keeps the write methods for Contributions; the new read contract is an
adapter-free factory over the same endpoints, so reads and legacy writes cannot diverge on wire format.

## Ordered work (each step independently verifiable)

1. [ ] Child branch/PR off #104; read the legacy list/detail/add-roaster, data impl and tests; inventory
   analytics events exactly (done above) and the data layer's request shapes and error handling.
2. [x] (2026-10-20, `feature:roaster:domain` + `feature:roaster:data`, 1 + 12 unit tests, Android + iOS device/simulator compile) Extract `domain` + `data` with parity tests (paths/verbs, tag ordering, address/canonical path,
   missing contact fields, cancellation). Legacy screens stay active.
3. [x] (2026-10-20, 19 impl unit tests; see "Step 3 record") `api` ports (analytics, favorites, shop-photo lookup) + `impl` typed MVI for list and detail:
   debounce/query bounds if any, lazy detail loading with the concurrency limit and per-slug de-duplication,
   favorite optimistic state and error message, stale-result/cancellation guards, retry.
4. [x] (2026-10-20; isolated UI suite 36/36 on Pixel 7; see "Step 4 record") Stateless content, components in own files, colocated light/dark previews, resources; reuse design-system
   search, chips, cards, loader (bean), refresh, favorite heart; keep geometry and the discovery carousel layout.
5. [x] (2026-10-20, code + unit/composition tests; no device run yet; see "Step 5 record") App adapters in composeApp androidMain: analytics (existing `SearchTracker`, `TrackEntityDetail`,
   exposures, `DetailEntryMemory`), favorites and shop-photo ports, `RoasterScreenRenderer` (Legacy default),
   Koin module, Discovery section and detail route switch for Android only.
6. [ ] Richer Sandbox fixtures (several roasters, tags, shops with/without photos, contacts, favorites) and
   unit tests; isolated UI tests; Sandbox walkthrough in both themes, error mode and large font where the
   device allows without changing system settings.
7. [ ] Document gates; keep live API/media, iOS and Contributions acceptance explicit.

## Risks and checks

- Discovery shares `RoasterListViewModel` state (selected roaster ids from filters): the migrated section must
  accept those inputs through its entry, not by reading Discovery's ViewModel.
- Favorites identity: ids are `slug` or backend id depending on screen; keep the current rule
  (`publicAddress.slug ?: id`) and test it.
- Per-item detail loading can fan out: preserve the 4-request limit and cancellation on scroll/leave.
- Shop-photo enrichment calls Shop details; failures must not hide the roaster page.
- Analytics: the same event names/props as the legacy screens (parity test like Coffee's adapter test).
- Do not delete legacy roaster screens until iOS and Contributions no longer use them.

## Verification plan

Unit (domain/data/impl/adapters), composeApp composition test, Direct/Play/Sandbox builds with isolation
checks, instrumentation compilation and iOS Simulator compilation, isolated feature UI tests on Pixel 7 (test
APKs only), then a Sandbox walkthrough. Record dates, variants, counts and what was not covered.

## Step 2 record — 2026-10-20

- `domain`: `RoasterRepository` (read-only: `getRoasters`, `getRoaster(slug)`), summary/details/address/tag/
  location/contact/photo/shop models. Its own `RoasterAddress` replaces the legacy `PublicAddress`; the summary
  exposes `slug` (null without a public address).
- `data`: private DTOs with the legacy wire names, `RoasterBackend` (`GET /api/roasters`,
  `GET /api/roasters/{slug}` with an encoded path segment), mappers and `createRoasterRepository(client,
  fileBaseUrl, store, nowMs)`. Preserved from the legacy repository: photo priority (card variant, logo URL,
  cover full URL, storage key), tags sorted with slug fallback names, flexible numeric counters, unaddressed cards
  kept and duplicate slugs dropped, detail location only with a non-blank address, blank contact links dropped,
  photos sorted by `sortIndex` and resolved through the file host, shop thumbnails from the cover full URL, the
  single-flight in-memory catalog (a loaded session stays stable) and the persisted 24 h catalog with
  stale-on-error fallback, cache write/read failures ignored, cancellation rethrown.
- Persistence goes through the app-supplied `RoasterCatalogStore`; with the legacy
  `roaster_catalog_v1:<host>` key the saved JSON is read by both implementations (tested with a legacy-shaped
  payload). The in-memory cache is per repository, so while both exist the app may issue one extra catalog
  request per process; the legacy Shop filter catalog (`ShopRepositoryImpl`) still uses the legacy repository.
- Differences from legacy, deliberately small: `getRoaster` encodes the slug as a path segment and rejects blank
  or over-long slugs; the domain model drops upload-only fields (`fileName`, `storageKey`).
- Not done: api/impl, app adapters, Koin, renderer, Sandbox fixtures, any activation.

## Step 3 record — 2026-10-20

New modules `feature:roaster:api` and `feature:roaster:impl` (logic-only for now: shared-library convention; the
Compose convention and entries arrive with step 4). No composeApp dependency yet.

- **Ports (api, commonMain, Kotlin only):** `RoasterAnalytics` -> `RoasterListAnalytics` (`resultsShown(fingerprint,
  query, filterKeys, resultCount, failed)`, `opened(position)`) and `RoasterDetailAnalytics` (`contentInteracted`
  with Photos/Shops/Contacts and Open/Select/Instagram/Website, `shopOpened`); `RoasterFavoritesPort`
  (`observeKeys`, `observeErrors`, fire-and-forget `change(target, favorite, onFailed)` — auth gating, de-duplication,
  account guards and analytics stay in the app adapter around `TrackedFavoriteInteractions`/`RoasterFavorites`);
  `RoasterShopPhotos` (`firstPhoto(shopSlug)`, null on failure). All have silent `None` defaults.
- **List MVI:** state holds items (+ lazily loaded details), loading/failed, query, selected tags, Discovery's
  selected roasters, favorites-only, favorite keys and a typed message. Filtering reproduces the legacy rules
  (trimmed case-insensitive name, all selected tags, selected roaster slugs, favorites-only). Initialize loads
  catalog + favorites + store errors as independent children, idempotently; a newer refresh supersedes an older one
  even if the old result is non-cancellable; per-slug detail locks, at most 4 concurrent detail reads, failures
  ignored and retryable; favorite toggle builds the target with the legacy photo fallback; results are reported once
  settled with a canonical fingerprint (the adapter's tracker ignores unchanged criteria) and `opened` is a 1-based
  position among visible items.
- **Detail MVI:** load by slug (blank or >200 chars fails without a request), favorite state derived from the slug
  key, shop-photo enrichment only for shops without photos and only if the same load is still current, photo preview
  as state, validated photo/shop actions, Instagram/website events carry already normalized URLs (link rules copied
  1:1 into `impl/ui/data/RoasterLinks.kt` with tests), back as an event.
- **Verification:** 10 list + 9 detail unit tests (19), domain/data suites unchanged (13); api/impl compile for
  Android, iOS device and simulator; the app still compiles. No device or Sandbox run (no UI yet).
- **Deliberate parity notes:** the catalog reload drops loaded details exactly like the legacy list; the reporting
  quirk that a retry after a failed first search is not re-reported (same fingerprint) is preserved in the adapter
  contract, not hidden. Strings are typed messages here; localized resources arrive with the UI.

## Step 4 record — 2026-10-20

`api`/`impl` now use the shared Android Compose convention; impl has `resourcePrefix = "roaster_"` and Russian-only
strings (`values/strings.xml`). Besides the logic, step 3 changed one behaviour to match the legacy screen: the list
now reports results 350 ms after criteria/loading settle (typing is not reported per keystroke); tests cover it.

- **Ports added:** `RoasterScreenAnalytics` (androidMain: `TrackDetail(loaded)`, `exposure(modifier, RoasterExposure)`;
  `RoasterExposure` = Catalog, Discovery, Photos, Contacts, Shops).
- **Application slots (typealiases in impl):** `RoasterImage` (url, description, modifier, scale, placeholder label
  size — the app keeps its loader/placeholder), `RoasterLoadError` (the app's mascot error content with retry, so the
  artwork is not duplicated), `RoasterPhotoViewer` (full-screen dialog), the search `header` of the list, and
  `onFavoriteFeedback` (haptics). Search header, tab/route logic and Discovery's criteria stay with the app.
- **Components (one file each, with light/dark previews):** `RoasterCard` (legacy custom layout: footer measured
  first so stats survive an exact carousel height), `RoasterStat`, `RoasterFavoriteHeart` (design-system
  `CoffeePeekLikeButton`, 48dp), `RoasterTagFilters`, `RoasterListItemCard` (requests details once settled),
  loading/empty rows, `RoasterSection`, `RoasterContactPill`, `RoasterShopCard`, `RoasterDetailHero` (pager, counter,
  glass back button, glass heart), `RoasterDetailError`.
- **Screens:** `RoasterListScreen(Content)` (pull to refresh, tag chips, snackbar for typed messages, exposure
  `Catalog`), `RoasterDiscoverySection(Content)` (carousel: card width = 88% clamped to 280–360 dp, at most 8 cards,
  exposure `Discovery`), `RoasterDetailScreen(Content)` (hero, about, address, links, shops, empty-shops note,
  failure with retry/back, photo viewer slot, `TrackDetail`, exposures Photos/Contacts/Shops).
- **Not wired yet (step 5):** public entries/factories in api, app adapters (analytics, favorites, shop photos,
  image/error/viewer slots, haptics), renderer, Koin, route switch.
- **Verification so far:** impl unit tests (19), Android + iOS compile, Android lint for impl (zero issues; no
  failing run), app still compiles. Isolated UI suite `RoasterContentTest` (9 cases x 4 theme/font-scale configs:
  states, filters/actions, carousel limit, exact-height footer, detail sections/actions, empty detail, photo viewer,
  loading/failure) compiles; the phone was locked, so it has **not** been executed.
- **Known gaps against legacy, to verify visually in step 6:** the detail back button is now the design-system glass
  button (legacy: app `CpCircularBackButton`); the favorite heart's analytics exposure and haptic are handled by the
  app wrapper in step 5; the detail error text is generic instead of the server message.

### Step 4 device run — 2026-10-20

Pixel 7 / API 37, `:feature:roaster:impl:connectedDebugAndroidTest`, self-targeted test APK only (no application
package installed or updated). `RoasterContentTest`: 9 cases x 4 configurations (light/dark x font 1.0/1.5) =
**36/36 pass** (1m10s) after one repair round.

First run: 24/36 — three cases failed in all four configurations, all test-harness issues, no product change except
one test tag:
- photo-viewer case: the fake viewer was composed beneath the screen's own surfaces and never received the click;
  the app's viewer is a dialog window, so the fake now uses a `Dialog`;
- favorite-message case: the snackbar reports dismissal only after its short duration; the test now waits for
  `DismissMessage` instead of asserting immediately;
- carousel case: two lazy rows match `hasScrollToIndexAction` (tag chips and cards), so the scroll hit the wrong
  one; the carousel row got a `roaster-carousel` test tag (same practice as Coffee's `coffee-list`).
No assertion was weakened. The full suite was then rerun once: 36/36, no retries. Covered: loading/empty/failed list,
card actions and tag chips, card without a public address (inert), typed message snackbar, carousel limit of eight,
exact-height card keeping its stats footer, detail sections and actions, empty detail, application photo viewer slot,
detail loading/failure with retry/back. Not covered: real images/loader/error artwork (app slots), Haze blur for the
glass buttons, accessibility services, Sandbox/real-app behaviour (step 6).

## Step 5 record — 2026-10-20

Roasters are now **active on Android** in code; shared/iOS keep the legacy composeApp screens (renderer default).

- **api:** public `RoasterDiscoveryEntry` (`Section` carousel + `Search` overlay, which must be called in the same
  ViewModel-store owner so they share one list state like the legacy Discovery) and `RoasterDetailEntry`; the slot
  typealiases (`RoasterImage`, `RoasterLoadError`, `RoasterPhotoViewer`) became public; `RoasterFavoriteTarget` now
  carries the public address parts (the favorites store persists the full address); `RoasterExposure` gained
  `FavoriteCard`/`FavoriteDetail` (the legacy heart's exposure placement). `impl` exposes `createRoasterDiscoveryEntry`
  and `createRoasterDetailEntry`; the Discovery section also pushes query/favorites-only into the shared state.
- **composeApp (commonMain):** `RoasterScreenRenderer` (Legacy default: `RoasterPreview`, `RoasterListScreen`,
  `RoasterDetailScreen`) with a Koin factory parameter; `DiscoveryTabContent` and both `RoasterDetail` route hosts in
  `Navigator` call the renderer; the Discovery tab no longer builds the legacy list ViewModel itself.
- **composeApp (androidMain, `di/roaster`):** `roasterModule()` (repository over the authenticated client, catalog
  store over `SettingRepository` under the legacy `roaster_catalog_v1:<host>` key, favorites port, shop-photo port),
  `AndroidRoasterFavorites` (wraps the existing `RoasterFavorites` + `FavoriteInteractions`, so auth gating,
  de-duplication, account guards and favorite analytics are unchanged), `AndroidRoasterShopPhotos`,
  `AndroidRoasterAnalytics`/`AndroidRoasterScreenAnalytics` (existing `SearchTracker`, `TrackEntityDetail`, exposures,
  `DetailEntryMemory`, legacy names), and `AndroidRoasterScreenRenderer` (search header, navigation, browser, viewer,
  `CoffeeShopImage`, `LoadErrorContent`, haptics supplied by the app).
- **Tests:** 9 new app unit/composition tests per variant (ports 4, analytics 4, module 1); app Direct+Play suites
  **1106 results, 0 failures**; roaster impl unit 19, domain 1, data 12. Builds: Direct/Play Debug, Direct Sandbox,
  Direct instrumentation compilation, iOS Simulator Arm64 compilation, roaster impl lint (0 issues).
- **Not verified:** any device or Sandbox behaviour of the activated screens (the phone was locked), the step 4 UI
  suite after the entry/typealias refactor, haptic feedback, glass back button vs legacy back button, Haze, real images.
- **Known differences from legacy:** the detail back button is the design-system glass button; the detail load error
  text is generic instead of the server message; the legacy list's `isFavoriteLoading` was never set and is dropped.
- Next (step 6): richer Sandbox fixtures, Sandbox walkthrough in both themes/error mode, rerun the isolated UI suite.
