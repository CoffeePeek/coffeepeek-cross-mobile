# Roasters — Android-first migration plan

Status: steps 2–3 done (2026-10-20): domain, data, api (ports) and impl (typed MVI) with tests; legacy screens still active, nothing activated. Child of the Coffee checkpoint
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
4. [ ] Stateless content, components in own files, colocated light/dark previews, resources; reuse design-system
   search, chips, cards, loader (bean), refresh, favorite heart; keep geometry and the discovery carousel layout.
5. [ ] App adapters in composeApp androidMain: analytics (existing `SearchTracker`, `TrackEntityDetail`,
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
