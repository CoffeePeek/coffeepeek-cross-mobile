# Roasters — Android-first migration plan

Status: **plan only** (2026-10-20). No module, code or route change yet. Child of the Coffee checkpoint
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
2. [ ] Extract `domain` + `data` with parity tests (paths/verbs, tag ordering, address/canonical path,
   missing contact fields, cancellation). Legacy screens stay active.
3. [ ] `api` ports (analytics, favorites, shop-photo lookup) + `impl` typed MVI for list and detail:
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
