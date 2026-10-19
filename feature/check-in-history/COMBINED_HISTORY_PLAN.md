# Combined personal history — current-main parity and next slice

## Current-main reconciliation gate — 2026-10-19

Main **28917758** is inherited parent-first through #43 -> #95 and descendants.
The Android combined calendar/timeline and all three routes remain active, but
its independent photo DTOs still ignore main's variant manifests. Before treating
the feature as complete, retain aligned original/feed/detail/thumb representations
through Shop history, Home reader adapters, calendar selection and gallery effects.
In particular, an explicitly empty manifest must not silently fetch the original
inline, and a detail URL must not erase a server-issued original. Existing account
revocation and protected app image-loader policies remain required. Profile
Activity is a separate legacy modal, not a completed full-screen migration.

## Compact visit markers and latest available photo — 2026-10-16

Days now show one dot per check-in up to three, then a compact `+N` for the
remaining visits. Dots remain visible with or without a photograph and use the
selected-background contrast color. They stay inside the existing square cell;
no extra row height is reserved. Accessibility still announces the exact visit
count once on the date, not once per decorative dot. Markers count visits, not
photographs, and follow the existing All/Shop/Home filter.

The displayed photograph belongs to the latest visit/preparation with an image
in that day's month snapshot. Later visits without images do not erase the last
available photo. Cutouts take priority over originals within the same visit,
not over newer photographed visits. Shop visit/creation and Home preparation
instants are compared as actual instants, including UTC offsets. Optional time
metadata passes through existing read models/adapters; no new HTTP request,
writer, public navigation API or iOS screen behavior was introduced. Unknown or
malformed times sort after known times without hiding the available image.

Verification: **867 selected unit executions** (both app variants and affected
domains/impl), zero failures/errors/skips; feature lint, test APK and Direct/Play
APK assemblies with packaged DEX guards pass. Shared history impl and Home read
domain compile for iOS Simulator Arm64 as a source-compatibility check only.
All **20 self-targeted history UI tests** pass on the temporary read-only emulator,
including 0/1/2/3/5 visit states in both themes, with/without images, unchanged cell
dimensions and ripple clipping. No real phone/account, backend write or consent
change was involved. Pixel 7/populated-account release gates remain separate.
The two-visit/no-photo fake calendar was visually inspected in light and dark
themes; screenshots remain local. The temporary emulator was closed without a
saved snapshot after verification.

## Calendar production-geometry correction — 2026-10-16

Restored the current-main `CheckInCalendarCard` day layout in Android history:
square slots, 1 dp gutters, 12 dp rounded selection/today borders and clipped
bounded ripple on the same cell. Removed the 76 dp minimum height, circular
number selection and the always-reserved 32 dp photo/dot row. A day photo now
occupies the square cell with the existing Fit/Crop loader contract and a small
remaining-photo count, as in main. The unused photo-stack component is retired;
previews remain beside the day component. Date/filter/MVI/account logic, the
profile Activity modal and shared/iOS paths are unchanged.

Verification: Studio MCP build and targeted file inspections pass; **43 feature
unit tests**, feature lint, Direct/Play APK assembly and both packaged DEX guards
pass. On the temporary read-only Pixel_10_Pro emulator, **four calendar UI tests
and all 15 history UI tests pass** in their final Studio runs. Pixel 7 was not
connected; these are fake-data emulator checks, not renewed real-account/device
acceptance. No real app update, account mutation or backend request was needed.
The selected-date fake screen was visually inspected in both themes after the
final run; screenshots remain local. The temporary emulator was then closed
without saving a snapshot.

Retry evidence: the first two size assertions forgot the production 1 dp gutter
on each side (54 dp visible cell within a 56 dp slot); expectations now include
that gutter. An intermediate full run passed 18/19: the decorative `+1` assertion
needed the unmerged tree because accessibility intentionally exposes one date/
count node. After correcting that finder, the complete 15-case history class
passes again; all four calendar cases had already passed. The initial manual
runner attempt found no instrumentation after IDE cleanup and executed no tests.

## Scope clarification and Pixel 7 checks — 2026-10-16

Re-fetched main: **0137ee7** remains current. Develop **82bc04c** differs in Apple
sign-in handling, not in these profile/history entry points. Main has both:

- Own profile `Чекины → Все`: `ProfileScreen` supplies `onCheckInsClick`, navigating
  to `VisitedPlaces` and the full `ProfileCheckInsScreen`. Android migration
  replaces this destination with the combined entry; it is not a retired route.
- Profile `Активность`: `UserProfileScreen` opens its Activity modal. For the own
  profile, it invokes `PersonalProfileActivityCalendar`, still using the legacy
  Shop/Home ViewModels and `CalendarCard`. Public profiles use the public activity
  calendar. Neither modal is replaced by the new full-screen history entry.

Do not describe the new history calendar as a completed migration of profile
activity. Next scope review must account for both entry points, preserve main's
presentation and assess reuse of the prepared readers/calendar without embedding
the entire history screen in the modal or creating duplicate business modules.
Do not remove the active full-screen route or change public/iOS profile behavior
based on the assumption that Activity replaced all personal history.

On the agreed Pixel 7, **15 self-targeted history UI tests and 33 self-targeted
Shop UI tests pass** in their final runs. Their fake ports do not initialize the
production Application or perform real writes. The initial Shop run had three
failures: the new avatar test used an invalid fixed lazy-list index, corrected to
semantic scroll-to-author; two selection cases temporarily lost the Compose root.
All six selection cases passed independently, then all 33 Shop cases passed
together. Preserve this retry evidence; the transient failures are not proven
production bugs or permanently resolved test-host flakiness.

Inspected the fake history in both themes on the phone. In the user's already
installed **1.0.637**, opening the app, Profile → Check-ins, All/Shop/Home, previous
month, Today and clearing the date were exercised. Real history is empty; no
populated timeline/photo/backend-write acceptance is inferred. The Home form and
creation choice stay bottom-aligned; Home cancel returns to history, and the Shop
choice opens the picker and returns without submission. No real writes, logout,
consent changes, data clearing, downgrade or main-app installation occurred. The
exact source revision of that user-installed APK was not independently established.
The other connected phone was untouched. No fatal AndroidRuntime message was
observed in the inspected log window; this was a foreground-open/navigation check,
not a newly verified cold-process startup.

Studio MCP discovery and inspection of the changed avatar UI test succeeded:
zero IDE errors; one existing deprecated Compose test-rule warning. A unit-test
run requested through Studio created a run configuration, but its MCP request
ended with HTTP 404 and no completion result. Do not count it as a passing run.
The earlier 995 unit executions below remain separate, dated evidence. App-targeted
instrumentation, real populated Activity/modal parity, accessibility and safely
authorized mutation/account checks remain open. Keep #95 draft.

## Author and creation-flow code checkpoint — 2026-10-16

Shop parent fixes retain server author avatars and navigate public headers only
through canonical author.slug. The history child retains the avatar through its
own DTO/domain/image slot. Personal history in main does not wire author-profile
clicks; this screen intentionally does not invent one. The profile implementation
itself remains main's implementation, not a new extracted feature.

Creation-picker open/cancel notifications are account-scoped, deduplicated one-off
events. Application composition owns the existing CheckInTracker and hands the
same flow to the Shop picker or existing Home editor. No analytics SDK or account/
record metadata enters the feature. Injected modifiers and modal wrappers retain
main's visible-exposure, overlay, lifecycle and consent policy without copying
that infrastructure into feature/core. Home dismissal reports cancellation while
retaining edit → detail → close behavior and the pending-write dismissal guard.

New regression coverage includes canonical/hidden authors, avatar URL retention,
picker notifications, single-flow handoff, revocation, consent rejection and
Home-field/cancel behavior without writes. New UI contracts are prepared in both
themes; they must be run on the agreed Pixel 7 after code validation, not reported
as executed based on compilation alone. The former 14-test emulator run below is
dated evidence for the earlier checkpoint.

### Pixel 7 acceptance sequence (not run at this checkpoint)

1. Update Direct debug in place without clearing data; smoke-test startup and the
   new profile → history route. Keep the current account and consent unchanged.
2. Public Shop carousel/list: actual avatar, missing/failed-avatar fallback,
   canonical profile/back, no route without author address, guest-hidden rows.
3. Populated history: All/Shop/Home, month/day/reset, both cursors, retained errors,
   cutouts/originals, personal avatars, detail/photo/back and light/dark appearance.
4. Creation choice → Home fields/photo preparation → cancel; choice → Shop picker
   → back. Confirm the same flow identity with sanitized diagnostics only.
5. Real create/edit/delete and logout/account replacement need a safe test account
   and explicit coordination. Do not manufacture real records to fill an empty
   history or change analytics consent merely to make a test pass.

Next feature selection remains blocked on these acceptance gates, not on creating
more modules. iOS UI migration and broader parent analytics parity are separate.

Final combined validation: **995 selected unit-test executions**, zero failures,
errors or skips; both Direct/Play APKs and packaged DEX checks; app/Shop/history
lint; app and both feature instrumentation APK assemblies pass. The shared Native
compilation check is valid/up-to-date. New UI cases are compiled, not executed on
a device in this checkpoint. The phone/account/consent were not touched. Keep
#95 draft until Pixel 7 acceptance; no release completeness is inferred from this
engineering checkpoint.

## Latest main synchronization — 2026-10-16

Main **0137ee7** is inherited through parent #43, including #96's server-backed
favorites/photo contracts and #97–98's personal/public profile redesign. Profile
UI is preserved from main; its feature extraction has not started. All three
migrated Android history routes remain connected, and shared/iOS fallback remains.

History calendar photos now come from month snapshots rather than the selected
timeline day/page. The app-owned Home reader fetches the month pages, preserves
issued photo URLs, prefers transparent drink cutouts, deduplicates entries and
propagates failures/cancellation. Photo rendering uses the existing injected image
boundary and main's cutout outline. Main's Home photo-preparation factory and
select/retry/skip actions pass through the existing editor bridge, without a new
writer or cross-feature implementation dependency. Direct/Play APKs and packaged
DEX guards, app/history lint, both instrumentation APKs and **859 selected unit
cases** pass with zero failures/errors/skips. Shared application Kotlin also
compiles for iOS Simulator Arm64; this is not an iOS screen/device certification.
The first run caught a missing mandatory timestamp in a newly added mock response;
the corrected contract fixture passes without weakening production parsing.
No live write or new phone verification is implied.

The app's incoming profile UI-automation scenarios have not been certified here:
two emulator attempts timed out while a visible System UI ANR dialog obscured the
fixture. Profile code is unchanged from main; no product workaround is introduced
for this test-host failure. The source/build/unit checks above do not replace that
visual/device gate. History's self-targeted Compose checks are recorded separately.

All **14 self-targeted history UI checks** pass on the isolated offline emulator,
including both-theme month-photo injection with an empty selected timeline,
guest privacy, typed navigation/creation and large-font guest controls. Calendar
cutouts keep transparent backgrounds; ordinary photos/placeholders retain their
surface. The taller month calendar exposed a test that attempted to click an
uncomposed lazy-list row; it now scrolls through the list's semantics before
asserting the same destination callback. Tests use fake data, no production DI,
backend requests, account changes or writes. Individual IDE preview rendering
and populated physical-device visual parity remain unchecked.

The author and history-creation follow-up is implemented in the code checkpoint
above. Remaining follow-up includes populated-device/photo/date-edge journeys, safe
mutations and account/accessibility checks. Do not start a new feature checkpoint
or call this migration release-complete while these gates remain.

## Historical physical-device smoke / then-new-main gate — 2026-10-16

Installed Direct debug **1.0.617**, source **ef41fd8**, over the existing debug
CoffeePeek on the agreed Pixel 7 using an in-place update. No app-data clearing,
logout/account replacement or check-in create/update/delete was performed; the
other connected phone was untouched. Fresh Direct assembly and packaged DEX
checks pass. The normal app cold start and Profile → Check-ins route work.

The real session's Shop mine page/month and Home page/calendar reads returned
HTTP 200; the displayed history is empty. Checked All/Shop/Home filter rendering,
previous month, Today restoring the current month/date, date reset, creation
choice, the actual Home form and populated brew-method dropdown. The form stays
bottom-aligned, its Save action is visible, and closing it without saving returns
to history. The Shop choice opens the existing shop picker; no submission occurs.
This is a real-device empty-history/read/navigation smoke check, not populated
timeline/photo/date-edge, persisted edit/delete or account-switch certification.
The earlier fake-data tests remain the evidence for those authored scenarios.
Screenshots and UI dumps stay local; no account identity, credentials or photos
are added to Git. iOS and the user's app settings remain unchanged.

A fresh fetch during this check discovered **origin/main f2b184b**, PR #96,
after the previously integrated 4976e53. Its 240-file change includes public
profiles, server-backed favorites, analytics, calendar photo previews, Home UI
and check-in time/cutout behavior. It was **not yet integrated at that smoke checkpoint** into #43/#95;
ef41fd8's smoke result must not certify parity with this newer main.
Next: sync the parent and history child, audit the changed product contracts
(especially favorites ownership/server semantics and history photos/time), then
rebuild and repeat affected tests before a new feature or release. Do not bring
the deleted local-favorites implementation back merely to resolve a merge.

## Current status: Android route activated — 2026-10-16

User authorized switching the three existing Android history routes. VisitedPlaces,
HomeCheckIns and EditHomeCheckIn(id) now render the migrated combined entry, keeping
the same route IDs and initial edit-by-ID. Platform renderer selection preserves
the shared/iOS ProfileCheckInsScreen. Application composition binds the existing
Home editor/detail/delete workflow to the history account lease and child store,
without migrating its writer or adding feature-to-feature implementation imports.
Only successful writes invalidate the combined reader; editor construction does
not issue duplicate legacy history/calendar loads. Account replacement closes
editor/photos and rejects late callbacks; token renewal retains the account.
Pending writes lock dismissal/replacement, failures keep the draft without retries.
The destination consumes initial edit IDs once, not once per account. Typed
Result/cancellation contracts are preserved. This is a compatibility bridge,
not a complete migration of the Home editor/data/writer.

Read-only backend source audit at 6144903272381e7dd4cc2b48790860bc34480682:
HomeCheckInsController and CheckInQueries.Home use inclusive PreparedOn civil dates
for both page and calendar; equal day bounds are valid. UnifiedCheckInsController
mine uses GetUnifiedByUserAsync, which explicitly filters Kind.Shop and uses
VisitedAt >= from and < to. The two readers therefore do not duplicate Home rows.
This establishes source compatibility, not the deployed backend's live behavior.

Source anchors: [Home controller](https://github.com/CoffeePeek/coffeepeek-backend/blob/6144903272381e7dd4cc2b48790860bc34480682/CoffeePeek.ShopsService/Controllers/HomeCheckInsController.cs),
[Home queries](https://github.com/CoffeePeek/coffeepeek-backend/blob/6144903272381e7dd4cc2b48790860bc34480682/CoffeePeek.Shops.Persistance/Queries/CheckInQueries.Home.cs),
[Shop queries](https://github.com/CoffeePeek/coffeepeek-backend/blob/6144903272381e7dd4cc2b48790860bc34480682/CoffeePeek.Shops.Persistance/Queries/CheckInQueries.cs).

Direct/Play APK assembly and packaged DEX checks, app/history lint and **662 selected
unit cases** pass: 257 app per variant, three Home domain, eight history domain,
seven history data, 38 history impl, 87 Shop and five Shop Report. Valid cached/
up-to-date tasks may be reused. The complete composeApp shared graph compiles for
iOS Simulator Arm64; its selected legacy screen/writer is not migrated.

All **26 isolated Android UI cases** pass on the owned read-only emulator: eight
actual destination/DI/Home workflow cases, five existing app-boundary cases and
13 feature-content/entry cases. These include initial edit-by-ID, default
Back/Auth/Shop-picker events, create/edit/delete confirmation and cancellation,
successful-write refresh without duplicate legacy history reads, pending-write
locking and logout without replaying an old initial ID into a new account.
The first harness run incorrectly tried scrolling a fixed Save button; it now
asserts/clicks the displayed action. A later fixture had a future preparation
time correctly rejected by existing validation; the fixture now uses a past
time. Production validation was not weakened. The final full suites pass.
Production editor ownership uses ViewModelProvider after lint rejected direct
ViewModelStore.put; the corrected source passes lint without suppression.

Light/dark fake Home forms, detail and delete confirmation were visually inspected:
bottom-aligned bounded sheet, themed content and reachable fixed Save action.
Modal IDE previews render their content; individual IDE preview renders are not
certified. Application instrumentation injects fake auth/repositories/catalogs/
images at the tested boundary while normal Application initialization remains.
No physical phone update, user-data clearing or live write is part of this change.
Live deployed-backend/date-edge, owner/device and accessibility QA remain release
gates. Earlier dated sections below are historical and do not override this status.

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
preserved without force-push. Android activation is recorded above.

- [x] Prepared Shop history domain/data, shared MVI, Android API/UI and revocable
  account/calendar/image composition, with fake-data tests.
- [x] Entire implementation preserved unchanged during branch extraction.
- [x] All three root route contracts are preserved. Android now selects the
  combined feature entry; shared/iOS still selects ProfileCheckInsScreen.
  Existing Home editor/writer is reused through the account-bound app bridge.
- [x] Home read boundary/adapter: implemented and automatically verified; live
  backend source confirms date bounds; deployed-backend verification remains open.
- [x] Combined filter/calendar/paging MVI prepared and automatically verified;
  runtime entry is now active on Android only.
- [x] Prepared production-shaped combined UI/entry: All/Shop/Home, one dot calendar,
  compact cards/rail, Shop detail/photo modal and required Home/create callbacks.
  The obsolete prepared Calendar/List code is retired, not active beside it.
- [x] Activate the combined Android route, retain initial edit-by-ID and delegate
  Home creation/detail/edit/delete and the existing Shop picker.
- [ ] Finish deployed-backend/physical-device/owner and accessibility release checks.

The authoritative whole-project status is ../ANDROID_REFACTORING_PLAN.md. It
records the saved-roasters parity fix in parent #43, inherited by this branch.
Its guarded account-bound removal and fake-data checks pass; remaining live QA
still prevents labelling that unrelated base feature production-complete.
Current main's newer commits add CI changes, not a new history product shape.
Standalone HomeCheckInScreen/history-statistics and VisitedPlacesScreen have no
call sites in the audited main source. Do not restore them as separate screens;
preserve the active combined timeline and its Home editor/detail callbacks.

## Original main audit / incompatibility (before the replacement above)

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
