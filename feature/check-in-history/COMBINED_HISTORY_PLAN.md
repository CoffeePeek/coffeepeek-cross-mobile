# Combined personal history — current-main parity and next slice

## Combined UI replacement — verified preparation, 2026-10-16

Replaced the inactive Shop-only Calendar/List entry with the prepared combined
coordinator. Preserved main's All/Shop/Home control, dot calendar, Today/date reset,
day headings, compact cards and timeline rail. Shop details/photos stay modal;
Home details and creation are required caller callbacks, not a second writer.
Retired the obsolete prepared mode-specific VM/UI and updated its consumers.
Keep the active root route and shared/iOS screens unchanged in this checkpoint.
The Android prepared renderer overload requires all Home/create callbacks; its
legacy no-argument interface continues the current combined screen until the
application-owned Home bridge is ready. No no-op user actions are introduced.
Verification of the final source: Direct/Play APK assembly and packaged DEX
checks (now also requiring CombinedHistoryViewModel), app/history lint and **642
selected unit cases** pass: 247 app per variant, three Home domain, eight history
domain, seven history data, 38 history impl (34 combined MVI + four presentation),
87 Shop and five Shop Report. Gradle reuses valid cached/up-to-date tasks. The
obsolete prepared VM's tests are retired with it, not counted as current coverage.
Shared history impl compiles for iOS Simulator Arm64; iOS UI remains unchanged.

All **18 final isolated Android UI tests** pass on the owned read-only emulator:
13 feature-content/entry cases and five app-boundary cases. They exercise guest
privacy, mixed rows, filter/date semantics, independent retries without automatic
paging loops, complete photo-album indices, Home/create callbacks, token renewal,
account replacement clearing rows/photos and cancellation while the route is hidden.
The first retry-test run failed because off-screen LazyColumn items were not yet
composed; explicit scroll-to-node fixes the harness, and the final full run passes.
Additional MVI regression coverage prevents a refreshed-away record's detail modal
from reopening when that record later returns. Detail text is complete, as in
main's fullVersion card; shop navigation uses its explicit button, not the whole
detail card.

Light/dark mixed fixtures and the bottom-aligned creation sheet were visually
inspected. Each new/changed independent component owns paired previews; individual
IDE renders are not certified, and modal previews show their content. App-boundary
tests inject fake auth/readers/images; the app process still performs normal
Application initialization. No physical phone, account clearing, backend write or
active route switch is part of this preparation. Fresh main remains 4976e53 and
is already included in both migration branches; no empty sync merge is needed.

Next: app-owned Home detail/editor/delete/create bridge and return invalidation,
initial EditHomeCheckIn(id), existing Shop picker, then all three Android route
contracts and safe live owner/Home-date-boundary parity. Do not mark the business
feature or backend semantics complete based on these fake-data checks.

## Combined MVI preparation — 2026-10-16

Implemented in this checkpoint: a commonMain coordinator for one
All/Shop/Home filter, month/date selection, source-specific calendars and page
cursors. It consumes only this feature's Shop repository and Home reader, plus
the existing caller-supplied zone-aware calendar snapshot. No new Gradle module,
cross-feature import, backend writer, iOS binding or root route switch is needed.

Preserve main's read semantics: no selected date uses both paged timelines;
selected Shop visits come from the complete month snapshot, while Home pages
use the existing same-civil-day range. Failures/retries stay source-specific.
Retain content on same-query refresh failure, clear mismatched date/month/account
content, reject canceled/stale responses and guard overlapping pagination.

The new combined coordinator temporarily coexists with the earlier prepared,
inactive Shop-only Calendar/List entry/VM so its existing consumers still build.
The next production-shaped UI/entry slice must replace that prepared path and
remove its obsolete mode-specific coordinator, not activate two history screens.
This read-only MVI slice uses Nothing events; navigation effects join when the
actual combined entry/UI consume them. Source preparation is not UI parity.

Verification: all 29 new combined-MVI scenarios pass (64 impl cases including
the earlier prepared coordinator/calendar tests). They cover partial failures,
source-only retry, date/filter/month replacement, independent pagination,
deduplication, coalesced refresh, clock/time-zone changes, guest/account reset,
same-generation renewal, cancellation, late responses and lifecycle cleanup.
A regression test first reproduced an extra initial Resume request after account
replacement; resetting that generation's first-resume flag makes it pass.

Direct/Play APK assembly and packaged DEX checks, app/history lint and **668
selected unit-test cases** pass: 247 app per variant, three Home domain, 79
history (eight domain, seven data, 64 impl), 87 Shop MVI and five Shop Report MVI.
Valid cached/up-to-date tasks are reused. Shared history impl compiles for iOS
Simulator Arm64 without an iOS UI/binding change. No new device UI run, live read/
write or active route switch is claimed. Fresh main remains 4976e53 and is already
in the stack. The next checkpoint step is combined ScreenContent/entry replacement,
then Android integration and live boundary/parity gates, not another feature.

## Home read foundation — 2026-10-16

Prepared in this same history checkpoint, without activating a root destination:

- Populated pure `feature/home-check-in/domain` read models, civil-date range,
  page/calendar contract. The module gate and legacy compatibility path are in
  `../home-check-in/README.md`. No new Home api/data/impl or writer exists.
- History owns distinct Home read models/port, with no dependency on the Home
  module. Android app composition maps legacy reads into Home domain, then the
  history port; manual constructor DI and existing HTTP transport are retained.
- The existing revocable history session binds Home page/calendar reads, rejects
  guests, cancels account replacements and preserves same-account token renewal.
- Preserve recipe values, IDs/issued photo URLs, unknown visibility/moderation
  and preparedOn separately from timestamps. Equal day bounds and calendar
  first/last dates follow the existing client verbatim; backend inclusivity is
  still not independently verified, so no route-switch parity is claimed.

Combined MVI, compact timeline, delegated editor actions and route activation
remain the next steps. Source preparation is not a completed Home feature.

Verification: Direct/Play APK assembly with expanded packaged DEX guards, app
Direct/Home-domain/history lint and **639 selected unit-test cases** pass: 247
app cases per variant, three Home domain, 50 history (eight domain, seven data,
35 MVI), 87 Shop MVI and five Shop Report MVI. This includes 14 new app scenarios
per variant and five new domain scenarios. Gradle reuses valid cached tasks.
Both changed domain modules compile for iOS Simulator Arm64; no iOS UI/binding
or writer is changed. Mock HTTP tests preserve the current Home request shape,
not independently verified live date-boundary semantics. No new physical-device
UI run, account modification, photo download or backend write is claimed.

## Latest parent controls checkpoint — 2026-10-15

Parent `59e9c93` is inherited through a normal merge: grouped photo-source and
Shop Report actions, selected drink checks/divider and form top clearance.
No history implementation, route activation or Home writer changed. The child
still contains the entire 71-file history checkpoint, rather than a second copy
of the parent's UI fixes. Its next implementation step remains the account-bound
Home read foundation described below, followed by combined MVI/UI/integration.

Fresh fetch confirms main remains `4976e53`; local main and both migration tips
already include it. No force-push or merge of either PR into develop/main occurs.
The parent's 518 selected unit executions and 17 isolated controls UI tests pass;
light/dark fake fixtures were inspected. Those tests do not certify a combined
history entry, authenticated writes or an iOS screen migration.

After the merge, Direct/Play APK/DEX, app/Shop/Shop Report/history lint and
**606 selected unit-test cases** pass: 233 app cases per variant, 87 Shop MVI,
five Shop Report MVI and 48 history (six domain, seven data, 35 MVI). Gradle
reuses valid cached/up-to-date tasks where applicable. No new combined-history
UI/device run or root route switch is claimed.

Source audit: 2026-10-15, main 4976e53, preserved implementation checkpoint 9e5a533.
This supersedes the standalone-shop destination switch, not the validated shop
history read/session foundation. Android first; preserve the shared/iOS paths.

## Ownership and checked status

All prepared history code is now in `feature/check-in-history-refactoring`, based
on `feature/core-design-system-controls` (PR #43). The parent excludes the four
new modules and their application adapters/tests; the child restores them as one
cohesive capability. Its initial 71-file extraction is extended here by the Home
read foundation, still under the current 100–150-file review guide. Do not split
the same feature again just to create smaller PRs. Git ancestry is
preserved; no force-push or route switch.

- [x] Prepared Shop history domain/data, shared MVI, Android API/UI and revocable
  account/calendar/image composition, with fake-data tests.
- [x] Entire implementation preserved unchanged during branch extraction.
- [x] Main's combined ProfileCheckInsScreen, Home editor and all three route
  contracts remain active and unchanged. This is not the prepared entry.
- [x] Home read boundary/adapter: implemented and automatically verified; live
  backend date-boundary verification remains a route-activation gate.
- [x] Combined filter/calendar/paging MVI prepared and automatically verified;
  runtime entry remains inactive until combined UI replaces the old prepared path.
- [x] Prepared production-shaped combined UI/entry: All/Shop/Home, one dot calendar,
  compact cards/rail, Shop detail/photo modal and required Home/create callbacks.
  The obsolete prepared Calendar/List code is retired, not active beside it.
- [ ] Activate the combined Android route and finish parity/release checks.

The authoritative whole-project status is ../ANDROID_REFACTORING_PLAN.md. It
records the saved-roasters parity fix in parent #43, inherited by this branch.
Its guarded account-bound removal and fake-data checks pass; remaining live QA
still prevents labelling that unrelated base feature production-complete.
Current main's newer commits add CI changes, not a new history product shape.
Standalone HomeCheckInScreen/history-statistics and VisitedPlacesScreen have no
call sites in the audited main source. Do not restore them as separate screens;
preserve the active combined timeline and its Home editor/detail callbacks.

## Actual owner and incompatibility

`composeApp/.../ui/screen/checkins/ProfileCheckInsScreen.kt` is the active owner
for VisitedPlaces, HomeCheckIns and EditHomeCheckIn(id). It combines shop visits
and home preparations. It currently coordinates VisitedPlacesViewModel and
HomeCheckInViewModel directly in Compose. There is no ProfileCheckInsViewModel.

The prepared history entry renders Calendar/List modes for shop visits only.
Activating it as-is would remove home records, the type filters, creation and
editing flows. Do not switch the root route merely because that entry compiles.

| Main behavior | Prepared migration gap | Required preservation |
|---|---|---|
| All / Shop / Home filter above one calendar and timeline | Shop-only Calendar/List mode selector | Keep the production type filters and combined layout; do not add a second global mode selector |
| One month/date selection; home and shop calendar counts | Independent shop month/selection and shop statistics | One authoritative selection/month, combined counts, next-month guard and clear-date/today actions |
| Timeline merges both sources; day headings, time, rail and stable kind-prefixed keys | Full shop cards in a separate calendar/list view | Keep compact Shop/Home timeline cards; details and photos remain separate modal actions |
| Show more loads whichever sources have more data | Only shop page state | Separate source cursors/loading/errors, deduplication and filtering; never infer one server cursor for both endpoints |
| Shop modal opens shop/photos; home modal opens detail/editor/delete confirmation | Shop/photo callbacks only | Preserve home detail, edit-by-ID entry, delete confirmation and safe caller-owned navigation |
| Create button opens the choice between shop and home | No combined creation entry | Delegate to the current shop picker and home editor during migration; no new writer required for history integration |
| Home date uses preparedOn; shop date uses local visitedAt/createdAt | Shop-only zone-aware instant grouping | Do not reinterpret the home civil date as a UTC instant or apply shop exclusive-to bounds to home date queries |

This table records source behavior, not a passing live-account/device audit.
The current client sends the same date as home `from`/`to` for day filtering;
confirm that backend's date-boundary contract before replacing the adapter.

## Boundaries

- History owns filter/calendar/timeline read coordination and presentation, not
  home creation, recipe validation, editing, deletion, cup extraction or uploads.
- Home owns its preparation models, business rules and read/write contracts.
  Its existing `features/home-checkin` artifact mixes domain and Ktor data, so
  do not add it as a dependency of the new pure history domain.
- The newly prepared independent foundation is a populated `feature/home-check-in/domain`
  boundary: real preparation/read models, calendar summaries and narrow reader
  contracts required by combined history. Keep legacy models/writers available
  to existing consumers until their migration; no placeholder api/impl modules.
- History owns its own home-history read models and reader port. Application
  composition adapts Home contracts to that port; history does not import Home
  domain/data/impl or the legacy module. Cross-feature dependencies use only
  intentionally public API; history UI/domain stays free of Koin, HTTP and
  application classes.
- Application composition supplies account-bound readers and temporary legacy
  adapters, image/date ports and callbacks to existing home actions. Data and
  domain retain manual constructor DI and cancellation-safe Kotlin Result.
- Do not invent a generic DI module, universal activity DTO or core history
  manager. Shared calendar/filter coordination belongs to this business owner.

### Next-module gate: Home domain

Responsibility: independent preparation/read models and reader contracts, with
meaningful domain rules added only when required. Dependencies: Kotlin only.
Consumers: Home data/presentation and application adapters, not other features'
domain/presentation. Hidden details: DTOs, transport,
photo reservations/uploads and repository implementations remain outside domain.
A package in the existing monolithic artifact does not enforce this independent
dependency boundary: consuming it also brings the feature's infrastructure.
Create the boundary with actual consumers and tested models, not empty scaffolding.

## Ordered implementation and stop points

1. Finish the migrated core modal correction and Shop source/visual parity.
   Retain pending-write locks and independently run placement/overlay/form tests.
   PR #92 is the separate main-based production fix, now merged into develop and
   received through merge 54d2acf. Do not cherry-pick it again when main is updated.
2. Prepare Home's pure read foundation, history-owned reader port and app-owned
   compatibility adapter between them. Do not introduce a history-to-Home-domain
   dependency or publish business models in API just to share them.
   Audit calendar/day/query semantics, nullable visibility/moderation, photos,
   page counts and missing fields. Preserve preparedOn versus preparedAtUtc.
   Test mappings, Result failures/cancellation and account replacement with fake
   repositories; no live writes or UI route switch in this slice.
3. Coordinate the combined history through typed MVI. One filter/month/date;
   source-specific list/calendar/paging states; retained-content failures;
   cancellation, stale-response rejection and same-account token renewal.
   Only create a domain use case for genuine shared rules/coordination, not a
   forwarding wrapper around each reader call.
4. Prepare production-shaped ScreenContent and component-per-file UI: filter,
   calendar, day group, timeline rail, Shop/Home cards, retry/empty/loading and
   create action. Strings/images are explicit resources; previews stay beside
   each component, with fake state in both themes. Current resource directories
   expose only default values/strings.xml, not a verified second translation.
5. Extend the narrow entry/caller callbacks and Android renderer without dropping
   VisitedPlaces/HomeCheckIns/EditHomeCheckIn(id). Keep current home writers and
   editor/detail flows delegated at the app boundary until their own migration.
   A revoked account closes photos/details and invalidates callbacks immediately.
6. Verify the whole combined Android route before switching it: mixed timeline,
   filters/month/today/date reset, both cursors, partial-source errors and retry,
   guest/account changes, photos/back/return and initial edit ID. Build Direct/
   Play, DEX/unit/lint and isolated UI scenarios. Live owner/backend parity is a
   separate gate; never remove shared/iOS legacy paths on fake-test evidence.

After this route reaches parity, select the next owner from the current audit.
Do not move to profile/auth/community as a substitute for finishing combined
history, or rewrite the full home editor as a prerequisite for read integration.

## Extraction verification — 2026-10-15

- The implementation and binary asset match 9e5a533 exactly; only roadmap and
  ownership/verification documentation changed. The move adds no product behavior.
- The parent extraction 415a69b passes independent Direct/Play assembly and
  packaged DEX checks, both app unit variants, Direct lint and app UI APK assembly.
- The restored child passes Direct/Play assembly/DEX, 222 app tests per variant
  and 48 history tests (6 domain, 7 data, 35 MVI): 492 executions, zero failures.
  App/history lint and app/history instrumentation APK assembly pass as well.
- ComposeApp and history data/shared MVI compile for iOS Simulator Arm64.
  No iOS screen, native navigation or writer was migrated.
- No device UI run is claimed for this extraction. Earlier UI results are dated
  evidence; the interrupted design-system rerun is not a passing result. No phone
  update, account clearing or backend write occurred in this organization step.

## Parent synchronization verification — 2026-10-15

- Parent #43's saved-roaster correction (`ea61b19`, `621d81c`) is inherited by
  normal merges. The history modules, application adapters, tests and assets
  still match 9e5a533; the child's diff against its parent remains 71 files.
- Both Android debug APKs and packaged DEX checks pass. Application tests run
  233 cases per variant, plus 48 history tests: 514 executions, zero failures,
  errors or skips. App/history lint and both instrumentation APK assemblies pass.
- The parent independently passes 449 selected unit executions. Its 12 isolated
  favorites UI tests and three app route tests pass on the temporary emulator;
  light/dark fake roaster fixtures were inspected. These are not a fresh combined
  history UI/device pass. No physical phone, live account or backend write was
  involved. The temporary emulator was closed after verification.
- Shared Native compilation is recorded in the earlier verified checkpoints;
  this final sync adds no Native-specific code or iOS screen/binding changes.
- Live parity/release gates remain open. Local success does not certify GitHub
  CI, and publishing the branches does not merge either PR.

Subsequent parent visual synchronization (`f7e22a0`): Shop detail corrections are
inherited without changing history implementation. Direct/Play app/DEX, app/Shop/
core lint and 601 selected unit executions pass (233 app per variant, 87 Shop MVI,
48 history). The parent's 13 isolated detail UI tests and inspected light/dark fake
fixtures cover that Shop correction, not a combined-history route switch. User-
approved yellow information icons and 14/12sp badge text are intentional prod
exceptions. No physical phone, account data, backend writes or iOS screen changed.
