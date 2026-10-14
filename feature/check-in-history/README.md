# Personal shop check-in history — Android-first migration

Audited 2026-10-14 on the consolidated migration tip after `c8d9346`.
This owner is **visits to coffee shops**, not home preparations or the community
feed. The existing `features/home-checkin` module and its HomeCheckIns route stay
separate; neither is renamed or duplicated here.

## Current source and contract

- Legacy `composeApp/.../ui/screen/checkins/VisitedPlacesScreen.kt`: one destination with
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
| impl | Shared typed MVI plus Android stateless screen/components, runtime adapter and entry factory | Own domain, core/presentation, resources; Android-only own api/design-system/lifecycle Compose; no data/legacy/HTTP/Koin | App-owned entry factory only. Compiling presentation without data enforces its domain-only boundary and makes account/request races independently testable. The internal ViewModel is not a cross-feature API. |
| api | Android screen entry and immutable presentation-calendar adapter contract; no models/repository/VM/DTO exports | Compose runtime only | Application routing/composition and deliberately supported cross-feature callers. Separates the screen ABI from internal MVI and Android UI implementation. |

Domain/data and the populated presentation impl are created incrementally. The
api owns a narrow screen entry/callback ABI; stateless calendar/list UI joins impl.
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
- The read/MVI/UI preparation checkpoints did not switch the active screen.
  Android destination integration is recorded below; source integration alone
  does not complete live-device/backend parity.
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
At that checkpoint intents reduced state only (`Nothing` events). The screen
preparation below adds actual caller navigation effects, not speculative classes.

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

## Android screen preparation boundaries

The current design-system has an Android target only. Keep the existing shared
ViewModel/state and their Native targets in impl/commonMain; prepare Android UI,
entry factory and runtime adapter in androidMain. This is a real dependency/platform
boundary, not moving UI just for previews. Android components use PreviewLightDark
in their own files. Do not add iOS targets to the entire design-system or drop
the existing shared presentation's Native compilation in this slice.

API exposes only Content callbacks/session-refresh inputs and an immutable date
snapshot port (local today/zone, month query instants and timestamp-to-local-date).
The factory receives domain repository, date provider and image-rendering callback
from composition. Protected image loading stays with the existing app-owned
loader; the feature must not create its own unauthenticated image client. Previews
and fake UI scenarios render local placeholders, never request private URLs.

Resource audit: composeApp and migrated Shop currently have values/strings.xml
only, with Russian default text, not two aligned localization directories. This
slice owns Russian strings, count forms and month/weekday arrays explicitly; it does
not invent or claim a second translation. The current Russian catalog uses its
actual count forms even on an English device, preserving the legacy grammar.
Thumbnail fallback artwork is copied unchanged from composeApp:
`maskot_with_photo.png` → `history_maskot_with_photo.png`, identical SHA-256
`b8b8062e57c14da276413120188c30d5f4e55c3f5a7d2004844558ef498b1feb`.

## Android screen verification — 2026-10-14

- Prepared calendar/list screen content, independent components, Russian resources
  and colocated PreviewLightDark previews. Runtime entry uses lifecycle-owned
  presentation and caller callbacks, without feature Koin or HTTP/image clients.
- Navigation events validate visible entries and carry session generations. The
  runtime immediately masks obsolete account state and drops stale effects.
  Initial content-generation baselines avoid a duplicate first request.
- All 48 history unit tests pass: 6 domain, 7 data and 35 presentation tests.
  Android API/impl lint and shared impl iOS Simulator Arm64 compilation pass.
- All 11 isolated UI tests pass on both Pixel 7 and the temporary read-only
  emulator, using the final test APK targeting SDK 36. Coverage includes calendar
  selection, guest privacy, retained-content and paging retries, photo/shop
  routing, drink-photo Fit, large fonts and actual entry callback/load contracts.
- Direct/Play debug assembly and both packaged DEX runtime-class checks pass.
  Library conventions explicitly target the configured SDK for test APKs; AGP's
  library-test default otherwise uses minSdk and obscures modern inset behavior.
- Light calendar and dark list were visually inspected in the isolated host.
  The test host and fake fixtures live only in the self-targeted test APK. The
  temporary emulator was stopped without saving a snapshot. No CoffeePeek user
  data, account or backend records were changed.

This is prepared UI, not a completed active-screen migration. Individual IDE
preview rendering and real-account/backend parity are not certified by the fake
host. Next provide Android revocable account leases and lifecycle stores, real
zone-aware calendar/date adapters and the existing protected-image renderer;
assemble them in app composition before switching VisitedPlaces. Keep home
preparations and iOS renderers/writers on their existing paths.

## Android composition preparation — 2026-10-14

Application-owned code now lives in `composeApp/androidMain/di/checkinhistory`:

- A permanently revocable account lease binds the existing authenticated remote
  repository. Guests/expired sessions/missing account IDs never issue owner
  reads. Token renewal for the same account preserves pending work; logout,
  replacement or failed observation cancels it. Non-cooperative late replies
  cannot restore old history. Result failures remain failures and cancellation
  is rethrown, including a mistakenly Result-wrapped cancellation.
- A navigation-owned host observes account changes even while history is not
  composed. It revokes the old lease and clears its child ViewModelStore on
  replacement/disposal. Observation failure hides data behind localized retry
  and Back actions; retry creates a new store rather than reusing old content.
- The calendar captures today and ZoneId together. Each month boundary uses
  `atStartOfDay` with that zone's rules independently, supporting DST and missing
  local midnight. Response grouping uses the same immutable zone. New snapshots
  see device-zone/clock changes. Parsing preserves the application's existing
  UTC fallback for server timestamps without a suffix, without fabricating a
  valid date on malformed input.
- The prepared renderer assembles the feature entry with the bound repository,
  date snapshot and existing `CpImage` protected-URL loader. It guards caller
  callbacks again at the application boundary. The eventual destination must
  scope any full-screen photo state to the account and clear it on replacement;
  a callback guard does not revoke an already-open external viewer.
- Android Koin registers one lazy repository/calendar/renderer graph. Resolution
  does not read credentials, open leases or make HTTP requests. Shared/iOS DI
  and legacy screens remain unchanged; no new Gradle module is necessary.

At this preparation checkpoint the active VisitedPlaces route was unchanged. No local
OpenAPI/backend implementation establishes whether `from`/`to` mean instant
bounds or local date boundaries, and whether `to` is exclusive. Mock transport
tests prove serialization, not server filtering. Confirm that contract (or
verify it with safe authenticated boundary fixtures) before routing users to
the new calendar. Remaining destination work: account-scoped photo overlay,
existing date-label/navigation adapters, route selection and live read-only
calendar/list/return parity. Do not remove shared legacy paths used by iOS.

Verification of this composition checkpoint: all 180 application unit tests pass
in each of Direct and Play, including 17 new calendar/lease/host/DI tests; the
48 history regression tests pass. Both debug APK assemblies, expanded packaged
DEX checks and application lint pass. The DI test uses fake sessions and an
unreachable fixture client without credential reads or requests. No APK was
installed or live account/backend scenario exercised in this checkpoint; the
11 physical/emulator UI tests above cover the prior isolated feature screen,
not the new application composition or its future destination integration.

## Android destination integration — 2026-10-14

The local destination experiment initially selected the new Android entry; it
was not committed or published. Incoming main e5722f9 subsequently replaced the
old separate histories with ProfileCheckInsScreen, including home edits. The main
merge at 29cff24 preserves that combined route on both platforms. The standalone
shop entry, renderer and revocable viewer remain prepared but are not active.
Do not reapply the old standalone root switch: audit the combined shop/home
ownership and integrate without discarding home records or editing capability.

The previous backend-contract gate was resolved by read-only source inspection
of coffeepeek-backend commit `3216c3603636a019ec08d65b40b45e4f685e4a1b`:

- [Mine controller](https://github.com/CoffeePeek/coffeepeek-backend/blob/3216c3603636a019ec08d65b40b45e4f685e4a1b/CoffeePeek.ShopsService/Controllers/UnifiedCheckInsController.cs)
  accepts DateTimeOffset bounds, rejects `from >= to` and passes their UTC instants
  to the query. The request is authorized and keeps pageNumber/pageSize.
- [Persistence query](https://github.com/CoffeePeek/coffeepeek-backend/blob/3216c3603636a019ec08d65b40b45e4f685e4a1b/CoffeePeek.Shops.Persistance/Queries/CheckInQueries.cs)
  filters owner shop visits by `VisitedAt >= from` and `VisitedAt < to`, excluding
  deleted records. This matches the local-zone adapter's instant bounds. It is
  source evidence, not certification of the live deployment or boundary fixtures.
- The public Swagger probe returned 404; no token was extracted or account query
  used to resolve this contract. The backend repository was not modified.

Photo preview lives in the account-owned host session, not SavedState or a global
store. Opening copies the full album and validates the selected index and current
authenticated lease. Logout/replacement/disposal clears an already-open viewer;
old callbacks cannot reopen it. Token renewal preserves it. The renderer reuses
the existing app photo dialog and protected loader, with injectable render ports
for fake-image composition tests.

Back, sign-in and shop actions map to the existing root routes; no feature owns
Navigator. Shop detail/list and history reuse one application date-label adapter,
moved from the Shop-only package without changing formatting/minute refresh.
History's existing lifecycle Resume and pull-to-refresh behavior handle return
reads. Do not subscribe to the old ReviewSync bus: this source tree has no active
producer, so it cannot establish post-write freshness while history is visible.

Legacy shared screen/ViewModel/calendar and DI registration remain for iOS.
Completion still requires safe live owner/guest, month boundaries, list paging,
photos/shop/Back/return and account replacement parity. A fake fixture UI test
does not prove authenticated backend behavior or render every IDE preview.

### Destination verification status

Before the final test-host correction, both Direct/Play APK assemblies and DEX
checks, app lint, all 183 app unit tests in each variant, and full composeApp
iOS Simulator Arm64 compilation passed. The latter is a shared-route regression
check, not a new iOS screen implementation.

Four application-composition UI tests were authored with fake sessions,
repositories, thumbnail rendering and photo-viewer callbacks. They are **not
verified as passing**: the first Pixel run preceded completion of the main APK
update and used the old target; the later run showed a blank splash-gated host.
Source inspection found that replacing MainActivity's App before its onReady
effect runs leaves its isAppReady gate false. The temporary emulator additionally
showed a System UI ANR and was stopped without a saved snapshot; it supplies no
passing evidence for this slice. Pixel's main APK update completed without data
clearing, but neither that installation nor the failed harness certifies startup
or live history parity.

The test now launches plain ComponentActivity through debug-only ui-test-manifest,
following [Android Compose testing setup](https://developer.android.com/develop/ui/compose/testing).
This avoids changing production MainActivity/splash behavior or starting its root
UI in the fake scenario. CoffeePeekApplication still initializes normally in app
instrumentation; do not describe the entire process as having no live background
activity just because the tested renderer's dependencies are fake.

The final manifest dependency/test-host source now passes both Android builds,
unit regressions, app lint and application test APK assembly. The four UI tests
still require device execution; compilation does not certify their behavior.
Earlier command-approval usage-limit/403 failures prevented device execution;
command access recovered for the requested publication into PR #43. Do not mark
the migration finished or the Pixel/UI/real-account gate passed on build evidence.

Main-sync/UI-parity follow-up: the current local root keeps ProfileCheckInsScreen
for VisitedPlaces, HomeCheckIns and EditHomeCheckIn in both Android/shared route
selection. The previous incomplete history work is retained and backed up in
stash 40d3fe81295a6a95d05678eb206297fd0abed065, not discarded. Current builds and
217 app unit tests per variant pass after main synchronization and Shop parity
changes, but the pending history composition UI tests still lack a passing device
result. A later command-approval 403 also prevented installing the Shop fixture;
this is not a backend/GitHub authorization failure or new verification evidence.
