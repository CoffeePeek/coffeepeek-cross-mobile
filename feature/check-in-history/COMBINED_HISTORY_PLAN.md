# Combined personal history — current-main parity and next slice

Source audit: 2026-10-15, main 4976e53, preserved implementation checkpoint 9e5a533.
This supersedes the standalone-shop destination switch, not the validated shop
history read/session foundation. Android first; preserve the shared/iOS paths.

## Ownership and checked status

All prepared history code is now in `feature/check-in-history-refactoring`, based
on `feature/core-design-system-controls` (PR #43). The parent excludes the four
new modules and their application adapters/tests; the child restores them as one
cohesive capability. Its 71-file checkpoint fits the current 100–150-file guide;
do not split the same feature again just to create smaller PRs. Git ancestry is
preserved; no force-push or route switch.

- [x] Prepared Shop history domain/data, shared MVI, Android API/UI and revocable
  account/calendar/image composition, with fake-data tests.
- [x] Entire implementation preserved unchanged during branch extraction.
- [x] Main's combined ProfileCheckInsScreen, Home editor and all three route
  contracts remain active and unchanged. This is not the prepared entry.
- [ ] Home read boundary/adapter: not started; no new Home module exists yet.
- [ ] Combined filter/calendar/paging MVI: not started; prepared MVI is Shop-only.
- [ ] Production-shaped combined UI: not started; prepared Calendar/List UI is
  not an approved replacement for the current timeline.
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
- The next independent foundation is a populated `feature/home-check-in/domain`
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
