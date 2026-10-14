# Personal shop check-in history — Android-first migration

Audited 2026-10-14 on the consolidated migration tip after `c8d9346`.
This owner is **visits to coffee shops**, not home preparations or the community
feed. The existing `features/home-checkin` module and its HomeCheckIns route stay
separate; neither is renamed or duplicated here.

## Current source and contract

- `composeApp/.../ui/screen/checkins/VisitedPlacesScreen.kt`: one destination with
  calendar/list modes, Monday-first calendar, selected-day cards, three stacked
  thumbnails, month visit/unique-shop counts, shop navigation and fullscreen photos.
- `VisitedPlacesViewModel.kt`: legacy BaseViewModel, calendar requests and list
  pagination. Its calendar month/today are captured at construction. It has no
  account-change observer or generation guard for list responses, and concurrent
  reset/list requests can overwrite newer state.
- `CheckInCalendar.kt`: month arithmetic and within-month streaks. Request bounds
  are UTC midnights, but rendering groups dates in the device's local zone. Those
  definitions disagree at month boundaries outside UTC.
- Legacy CheckInRepository/CheckInApiService read `/api/v1/check-ins/mine` using
  `pageNumber`, `pageSize`, optional `from`/`to`, and an envelope containing
  `items`/`totalCount`. The existing range repository already fetches all pages;
  preserve that behavior, rather than reintroducing a 100-item month limit.
- Shop/author relations are nullable public addresses. No GUID fallback for a
  removed relation. Photos and optional drinkPhotoUrl use server-issued URLs,
  never guessed storage-key paths. Rating is optional; privacy/moderation states
  and rejection text must survive mapping for the owner's history.
- The current destination only reads entries. Editing/visibility/reporting are
  community capabilities today; no writer is silently activated in this slice.

## Boundary / module-creation gate

| Module | Responsibility / hidden details | Dependencies | Consumers / reason for Gradle isolation |
|---|---|---|---|
| domain | Personal-history read models, calendar rules and repository contract; no DTOs/HTTP/UI | Kotlin only | Future history impl and data, app-owned adapters. Independent business rules must compile/test without the legacy layers, Compose or transport. |
| data | Existing mine endpoint, private DTOs, mapping and complete range pagination | Own domain, core/network, Ktor/serialization/coroutines | App composition through one repository factory. A package in composeApp would let UI access HTTP/DTOs and retain feature transport in application composition. |
| impl | Typed MVI state/actions, independent calendar/list loading and presentation date port; later stateless screen components | Own domain, core/presentation; no data/legacy/HTTP/Koin | Future app-owned entry factory only. Compiling presentation without data enforces its domain-only boundary and makes account/request races independently testable. The internal ViewModel is not a cross-feature API. |

Domain/data and the populated presentation impl are created incrementally. Future
api owns a narrow screen entry/callback ABI; stateless calendar/list UI will join impl.
There are no placeholder modules/packages, feature DI module, new root navigator,
or history storage/cache. Data receives the existing authenticated HttpClient;
the caller owns that client's lifetime. Domain/data use manual DI and Result,
rethrowing cancellation. There is no dependency on Shop impl/data, home-checkin,
legacy domain/network/data, or any application module.

## Ordered implementation

1. Independent read foundation: calendar rules without a fake UTC month;
   paged owner reads, complete bounded period reads, DTO containment and mapping.
2. Presentation: MviViewModel, immutable actions/state/events, separate calendar
   and list load state, retain-content failures and guarded paging/refresh.
   Deduplicate IDs, reject stale account/month/request results, cancel obsolete
   work and clear private rendering immediately on session replacement.
3. Screen API and UI: one entry, caller-owned shop/photo callbacks, Screen runtime
   adapter and ScreenContent. One component/file, explicit resources and paired
   previews beside components; preserve current calendar/list meaning, not a UI
   redesign. Audit supported locale directories before adding translations.
4. Android composition: lifecycle-owned feature store, account lease, guest state,
   local today/date grouping and **zone-aware month start/next-month start** from
   app platform code (including DST); do not assume a fixed offset all month.
   Confirm the v1 backend boundary interpretation before switching. Wire only
   Android VisitedPlaces; HomeCheckIns and shared/iOS consumers stay unchanged.
5. Complete-feature gate: Direct/Play build/unit/lint/DEX, paired UI tests and
   safe device parity for paging, calendar boundaries, photos/shop navigation,
   resume/return refresh and logout/account replacement. No live writes or user
   data clearing. Remove only Android glue proven unused, not shared iOS paths.

## Scope / risks

- Calendar dates are local civil dates; query periods carry explicit caller-issued
  instants. The pure month model deliberately has no `fromUtc`/`toUtc` shorthand.
- The endpoint's page-number pagination is not snapshot isolation. Duplicate IDs
  can occur while entries change. Complete range reads deduplicate and fail on a
  missing expected page instead of silently certifying a partial calendar.
- A repository instance has no user identity; account cancellation/revocable
  leases belong to Android composition, as in Shop. No global singleton cache
  may share private history across accounts.
- Prepared modules do not switch the active screen or complete history migration.
- Real camera/ML Kit/authenticated submission QA remains a Shop release gate,
  independently of this next-owner preparation.

## Read-foundation verification — 2026-10-14

The two new modules compile for Android and iOS Simulator Arm64; their debug unit
tests and lint pass (13 tests, zero skips/failures). Coverage includes Gregorian
dates/year boundaries/overflow, count-based pagination, exact caller-issued
period bounds and three-page reads, duplicate IDs, nullable relations/ratings,
privacy/moderation/drink-photo mapping, no storage-key URL fallback, rejected or
malformed responses, missing later pages and cancellation propagation.

This is isolated foundation validation, not screen/device/backend parity. No app
dependency, DI registration, route switch or iOS UI implementation was added.
The later MVI slice is described below; screen API/UI and Android adapters remain
the next implementation steps.

## Shared presentation preparation — 2026-10-14

Impl now contains an internal CheckInHistoryViewModel on core/presentation's
typed MviViewModel, immutable action/state models, and an injected presentation
calendar snapshot port. It depends only on own domain and core/presentation.
The current intents reduce state only (`Nothing` events); navigation effects
will be introduced with real screen callbacks, not speculative event classes.

- Calendar and list have independent load/error state and guarded lifecycle jobs.
  Mode switching reuses fresh content and loads an invalidated mode on demand.
- Refresh cancels older list paging; late replies cannot append to a reset list.
  Content-change generations coalesce one follow-up read; obsolete snapshots are
  not published. Pagination failure preserves the current page and requires an
  explicit retry instead of repeatedly fetching while the end of the list is visible.
- Monotonic session replacement clears both modes before reloading. Duplicate or
  older session/content notifications are ignored; canceled or non-cooperative
  late replies cannot restore the previous account/month or clear newer flags.
- Same-session refresh failure retains data. State exposes error flags, never
  raw transport messages. Cancellation is rethrown without becoming a UI error
  or executing a queued automatic retry.
- Calendar groups deduplicated entries with the same immutable zone snapshot used
  for request bounds, validates local dates and filters out-of-month entries.
  Explicit date selection survives load/refresh; default selection follows the
  existing today/latest/empty-month behavior. Nullable shop relations do not
  invent navigation IDs or increase unique-shop counts.
- Today/zone are rechecked on resume/refresh/retry. A changed zone invalidates
  grouping; a moved-back clock clamps a now-future month. If the platform snapshot
  fails, no query is issued with old-zone bounds. The initial injected snapshot
  must contain a valid local date; there is no fabricated epoch fallback.
- First Resume acknowledges startup without another request, including an already
  failed startup read. Later returns invalidate and refresh the visible mode.

Android compilation/lint and iOS Simulator Arm64 compilation pass for impl.
All 24 fake-repository MVI tests pass, including non-cooperative responses and
immediate coroutine completion. The 13 domain/data regression tests also pass
(37 history tests total, no skips/failures); no production DI, device/account mutation or live
HTTP is used. There is no Compose screen/component in this slice, so no preview
is claimed. Paired previews will be added beside real screen/component code.

Generation guards do not bind a repository to an account by themselves. Android
composition must still provide a revocable account lease/lifecycle store and
real zone-aware adapters before the active route is switched. iOS renderers,
home preparations, and the legacy VisitedPlaces screen remain unchanged.
