# Home check-ins — Android migration plan

Plan date: 2026-10-16; foundation work: 2026-10-17. Re-fetched source baseline:
`origin/main` **0137ee7**; migration parent: history **c726944**, PR #95.
Domain/data, detail/editor MVI, the Android detail entry and both renderers are
prepared, but no active Home writer, screen, endpoint or DI binding has switched.
The public editor factory and app-owned media/catalogue/picker/analytics adapters
are also prepared. Active data/DI binding and Android route integration are pending.

## Public editor entry and composition ports — 2026-10-17

Source identity: d8cf13d plus this entry/adapters diff. Main re-fetched, still
0137ee7 and already an ancestor. No new branch is needed inside the same Home
checkpoint. The public editor ABI carries only requested ID, generation, reactive
access, callbacks and modifier; it exports no draft, domain, ViewModel or DI.

- HomeCheckInEditorEntry owns its own ViewModel store while composed. Removing/
  reopening it, changing entry/ID or generation does not retain a previous form
  under the parent screen's store. Independent lease checks, runtime privacy
  masking and scoped event validity remain mandatory.
- Intentionally public impl factory ports now have real application-composition
  consumers: catalogue options, first-source processor, dates/media, platform
  picker and typed telemetry. They are not re-exported through feature API.
- Android app assembly reuses existing catalogue, extractor, image decoder,
  coffee/magnifier/method artwork, URI/JPEG/camera-permission pipeline and date/
  zone conventions. Initial instants keep millisecond precision. Historical brew
  codes display the current catalogue option without rewriting the stored code.
- Repository/catalogue/extraction adapters use the current account lease before,
  during and after work; wrapped cancellation is preserved. Catalogue transport
  messages become typed failures. Extraction and selected originals copy byte
  ownership; no independent backend/client, ML algorithm or legacy writer added.
- Photo-source sheet retains the current grouped gallery/camera choices. Each
  launcher request has its own composition scope; late work cannot be redirected
  to a new request. Delivery is bounded, one-shot and tied to the composed form/
  lease. Busy, closed, revoked, replaced or disposed owners cannot receive it.
  OS cancellation may return no result and does not prevent starting a fresh pick.
- Shared MVI emits typed telemetry categories only: no IDs, query, bean/name/note,
  dates or image bytes. The app adapts one existing consent-aware CheckInTracker,
  including global creation handoff, rather than opening a duplicate flow.
  Edits never report Created; terminal outcomes are deduplicated; revocation and
  declined-at-start consent do not replay private work. Non-cancellation telemetry
  failures cannot block editing or make an accepted create retryable.
- Android app now depends on prepared Home api/impl and packaged DEX guards
  explicitly verify their entry/VM and app adapter classes. **No active Koin,
  renderer, writer or navigation binding changed**. Data remains unwired;
  shared/iOS/profile Activity paths remain intact. Module count stays 31.

Next: provide the prepared data factory with existing gateway/upload clients and
bind it in Android composition; replace the temporary history Home bridge with
detail/editor entries; activate history/global-create/community-edit entrances
and verify injected composition, refresh/notice/back/dismiss behavior. Actual
OS picker/camera, IME/modal placement, configured protected images, Pixel 7
visual parity and explicitly authorized live CRUD/account acceptance remain gates.

### Entry/adapters verification

2026-10-17; d8cf13d plus this entry/adapters diff, main 0137ee7. The retained
read-only Pixel_10_Pro AVD was resolved explicitly as emulator-5554, API 37/
arm64/16KB, unlocked. Only the debug self-targeted fixture APK was installed;
its package/instrumentation target is com.coffeepeek.feature.homecheckin.impl.test,
generic Application, no INTERNET permission or production DI. No phone, real
gallery/camera/account operation or production APK deployment occurred.
APK SHA-256: beb772a0825bfc9622745e045b1d6c3f0722658da390c268366f69202f8b25ad.

- **7/7 new public-entry UI cases** pass on their first run, 34.966s. Then
  **36/36 complete Home UI cases** pass, 128.273s, including all previous 29
  detail/editor cases. No UI failures, weakened assertions or skipped cases in
  this slice. Earlier renderer harness failures remain recorded below.
- New entry checks cover close/reopen with the same parent store, exact edit ID/
  historical method display and unchanged submitted code, initially denied ports,
  gallery/camera source actions and cancel, bounded/one-shot delivery, callbacks
  from disposed forms and account-generation replacement during non-cooperative
  fake saving. They exercise the feature entry, not real OS URI decoding.
- **1017 selected unit executions** pass, zero failures/errors/skips: Home **112**
  (domain 14, data 23, detail 24, editor 48, picker delivery 3), legacy Home 16,
  history 54, core presentation 3 and app Direct/Play **416 each**. Valid Gradle
  up-to-date results are included. New app cases cover catalogue/alias/typed-error
  mapping, lease/cancellation and byte ownership, dates and one consent-aware
  analytics flow, including global handoff, edit, consent refusal and revocation.
  New common cases cover metrics/terminal/cancellation/SDK failure and deliveries.
- The first expanded test compilation failed on a singleton metric list's type
  inference; gave the fixture its explicit common metric type. Subsequent full
  runs passed in 48s, 23s and 26s as lease/SDK/error-mapping cases were completed.
  No failed unit assertion was hidden. One compiler warning introduced during
  development (unnecessary assertion) was removed.
- Direct/Play APKs and expanded packaged DEX guards, API/impl lint, self-targeted
  UI APK and shared impl iOS Simulator Arm64 compatibility compilation pass.
  API/impl lint report no issues; no concurrent Gradle builds. Existing app
  namespace/expect-actual/deprecation warnings remain outside this task.
- Studio module inventory still contains Home domain but not api/data/impl.
  App-file diagnostics consequently report unresolved new dependency symbols;
  no clean IDE inspection/preview result is claimed. Gradle compiler/lint are
  the verified fallback; **Gradle Sync in Studio is still needed** for its model.
- New photo-source body has colocated light/dark previews. Full UI regressions
  include existing light/dark brand-font layout fixtures; new source choices
  use fake ports, not actual camera/gallery acceptance. Actual configured
  artwork/images, IME and outer history/modal parity stay open for integration.
  Screenshots/diagnostics remain local, outside tracked sources.

Local final logs: coffeepeek-home-entry-final-all-2026-10-17.log,
coffeepeek-home-entry-ui-targeted-2026-10-17.log,
coffeepeek-home-entry-ui-full-2026-10-17.log. No live backend or iOS UI acceptance
is inferred from any of these fixture/compilation checks.

## Android editor renderer — 2026-10-17

Source baseline: main 0137ee7, re-fetched and already an ancestor; parent/source
revision 393af7d plus this editor UI diff. Source geometry was checked against
HomeCheckInEditor.kt, ReviewContent.kt, CheckInVisibilityControl.kt,
PhotoAttachmentsSection.kt, DrinkPhotoPreview.kt and HomeCheckInErrorSummary.kt.
Physical-phone baseline remains unavailable under the user's continue-without-
phone instruction; source/fixture checks do not certify production pixel parity.

- Added internal HomeCheckInEditorScreen lifecycle adapter and stateless
  HomeCheckInEditorScreenContent. The public editor entry/factory is deliberately
  deferred until its real app-owned lease, media/picker and analytics adapters.
  No application route, writer, composition, profile modal or iOS UI was changed.
- Runtime collects while STARTED, checks event revision/account and uses current
  callbacks. Denied access masks old private content immediately. Back closes
  the bean picker before the form; saving blocks close/back and outer dismissal.
  As with detail, the future factory must use a separate form-owned ViewModel
  store/key and an independently current account predicate, not a recomposition-
  delayed Boolean alone. Repository and asynchronous picker guards remain required.
- Preserved header/body/footer layout, 16dp side padding, 24dp section rhythm,
  optional timeline steps, private-by-default visibility, optional rating card,
  44dp outlined controls and 80dp note minimum. Error summary and field icons/
  dark-theme contrast retain the current form presentation. Numeric duration
  inputs retain partially typed zero/empty values while reflecting external totals.
- Added own component files for date, method, bean search/custom choice, fields,
  duration, visibility, rating, original photos and cutout selection/retry/skip.
  Reused CoffeePeek fields, button, switch, form rail, loader and icon primitives.
  Each component and screen has its own colocated PreviewLightDark; no preview-
  only source file, new module, dependency or parallel design-system alias.
- Material date picker uses UTC-midnight civil days and caller-supplied local
  instant conversion. Selecting the same day never calls that conversion or
  replaces the recorded timestamp; future dates are disabled.
- Existing rating/method/empty-state artwork and original/cutout image decoding
  are required injected media ports. Layout previews/tests use fixture icons/
  boxes, not newly invented production assets or a separate network loader.
  Real app artwork and protected-image appearance must be checked on integration.
- Picking is a caller-owned request with the remaining five-photo allowance,
  explicit busy rendering and no camera dependency. Originals/cutouts have
  separate rendering roles. Edit explains that issued photos remain unchanged
  and offers no replacement picker. A cutout selection or skip is required when
  multiple objects remain unselected; the save footer reflects this and picker busy.
- All new labels, accessibility descriptions and issue mappings are Russian
  Android resources, matching this feature's actual locale baseline. Other
  Material library locales in the test APK do not prove app translation coverage.

Remaining gates: real public editor entry/factory; configured media, catalogue,
picker/extractor adapters and one existing consent-aware analytics flow; Android
history/global-create/community entrances and composition regressions; actual
keyboard/modal placement, real protected photos and Pixel 7 visual acceptance;
authorized live CRUD/account acceptance. No feature-complete claim yet.

### Editor UI verification

2026-10-17, source 393af7d plus this editor UI diff; Android debug self-targeted
test host, package and instrumentation target
com.coffeepeek.feature.homecheckin.impl.test, AndroidJUnitRunner, generic
Application, no INTERNET permission or production DI. Read-only Pixel_10_Pro AVD,
emulator-5554, API 37/arm64/16KB, 1280×2856, density 480, unlocked. Only that
fixture APK was installed/updated; no production APK, account or camera was used.
Test APK SHA-256:
64ccebcfeeced602d8ae01f54fdd8a8da63a32646a254111fd4b7781a9c4048f.

- **29/29 self-targeted UI tests pass**, including **17 new editor cases** and
  all 12 detail regressions; complete final run 103.715s. Coverage includes both
  themes, all fields, actual typed actions, selected/changed/cancelled civil date,
  method choice, bean/custom selection and retry/back intents, partial duration,
  localized error semantics, edit-photo retention, remaining picker allowance,
  cutout selection/skip, busy/closed/revoked rendering, 320×520dp/font-scale 1.6
  reachability, guarded saving/dismissal, latest callbacks and lease revocation.
- Initial complete 28-case run had **five failures**, all traced to fixture/
  synchronization assumptions: a controlled input did not echo its new value;
  candidate scrolling targeted a horizontal ancestor instead of the vertical
  body; an empty editable value was compared with its visible placeholder;
  callback publication and a current lease were incorrectly delayed until
  recomposition. Kept the strict assertions and corrected the fixture/state,
  scroll and ordering setup, not the production outcome requirements.
- Targeted expanded six-case rerun passed those five; its additional changed-day
  check failed a visible-number selector. A diagnostic single-case reproduction
  confirmed Material exposes the full localized date as semantic text. Replaced
  that selector with the locale's full date, removed diagnostic output, reran it
  **1/1**, then reran the **entire 29-case suite** successfully. No failing case
  was skipped or assertion weakened.
- **987 selected unit executions pass**, zero failures/errors/skips: Home 104,
  legacy Home 16, history 54, core presentation 3 and app Direct/Play 405 each,
  including valid Gradle up-to-date results. Both APK variants and packaged DEX
  runtime-class guards, impl lint, UI APK assembly and impl iOS Simulator Arm64
  compatibility compilation pass. Latest source checks passed in 11s after the
  full selection passed in 13s; no concurrent Gradle builds.
- Brand-font light/dark layout screenshots were inspected locally; header,
  outlines, rail, rating, fields and save footer render without visible overflow.
  Screenshots/diagnostics remain outside tracked sources. Fixture artwork is
  explicitly not the final mascot/photo appearance. No individual IDE preview
  rendering, actual IME/modal-container parity, protected-photo decoding, Pixel 7
  or live backend/write acceptance is claimed.
- Studio file diagnostics still report these sources outside imported content
  roots; compiler/lint are the fallback. Lint reports no issues. Boundary checks
  find no new app/data/legacy/other-feature implementation imports. Common MVI
  stays platform-independent; UI stays Android-only. New module count: **zero**.

Local logs: coffeepeek-home-editor-ui-final-2026-10-17.log,
coffeepeek-home-editor-ui-suite-final-2026-10-17.log. Layout captures:
coffeepeek-home-editor-{light,dark}-393af7d-ui-2026-10-17.png. All are local,
outside the repository; screenshots contain fixture data only.

## Editor MVI — 2026-10-17

- Common HomeCheckInEditorViewModel now owns typed State/Action/Event, form
  loading/editing/validation/save and account/revision-scoped outcomes. Constructor
  values come from composition's current instant/civil-date conversion; the
  future factory must retain millisecond precision rather than nine fractional
  digits. No Android/time-zone dependency is added to common code.
- Create starts private at taste score 5. Recipe fields remain optional. Editing
  loads only its requested preparation, preserves historical product identity,
  nullable rating/visibility, custom drink and issued photos; its upload list stays
  empty. Failed/mismatched/blank edit IDs never fall through to POST creation.
- The same selected civil day keeps its existing instant; another day supplies
  both explicit fields. Domain validation runs before requests. Dose comma input,
  existing text limits and toggled optional rating follow the current form.
  Only changed fields lose/revalidate prior errors; unrelated server errors remain.
- Catalogue/method and first-photo processing ports are internal presentation
  contracts, consumed by the new VM/tests. The future entry/composition will
  adapt the existing catalogue/extractor; no backend/extraction algorithm or
  feature-to-feature implementation dependency is created here. Bean search uses
  the existing 300ms debounce, query limit and stale/cancellation checks.
- A small form-owned photo preparation component uses the VM lifecycle scope.
  It preserves five-original ordering, extracts only the first source, auto-selects
  one candidate and requires selection/skip for multiple candidates. Source change,
  skip, close or revocation invalidates late work; repeated active retries coalesce.
  Source/draft/cutout state changes are atomic even for immediate Flow collectors.
  Original/candidate bytes are copied at their ownership boundaries. Edit ignores
  replacement-photo intents. Processor failures retain originals and permit saving
  without a cutout, as before. Fixtures validate port metadata, not image decoding.
- Save blocks conflicting edits/close/duplicate writes and never retries itself.
  Result failures keep the draft and typed field/operation issues; cancellation
  is rethrown, including when returned inside Result. One accepted response closes
  the form and emits only saved ID plus visibility-unconfirmed flag. A moderation
  Pending response is not mistaken for a visibility failure or publication approval;
  a mismatch never starts a second create. Private form/photo state is then erased.
- Account revocation/owner clearing cancels work and erases state; late non-
  cooperative results cannot reopen a form or emit Saved. HTTP 401/403 alone is
  not an account-generation change: when the same account lease remains current,
  retain its draft and permit explicit retry after token renewal. Composition must
  still guard the repository and invalidate the lease on actual account replacement.
- This is prepared logic, not an activated editor. API/module count stays **31**;
  Android editor content, components/resources/previews, lifecycle factory, picker/
  extractor adapters and the single existing consent-aware analytics flow still
  follow. No Compose renderer or old application/iOS binding changed in this slice.

Verification: **2026-10-17**, base **e77821e** plus this editor diff. Main was
re-fetched and remains **0137ee7**, already an ancestor; no new merge was needed.
All **43 editor cases** pass: create/edit mapping, optionality/date/error correction,
catalogue debounce/retry/late results, write/cancellation/lease guards, visibility
notice, photo selection/skip/source changes and ownership/atomic-state invariants.
Home now has **104 cases**: domain 14, data 23, detail 24, editor 43. The complete
selected set passes **987 unit executions**, zero failures/errors/skips: Home 104,
legacy Home 16, history 54, core presentation 3 and app Direct/Play 405 each,
including valid Gradle up-to-date results. Both app APKs/packaged DEX checks, impl
lint, self-targeted UI test APK compilation/assembly and impl iOS Simulator Arm64
compilation pass. Initial 36-case logic run passed in 15s; after additional cases
and review corrections all expanded selections passed, with the final complete
run taking 7s. No failing tests, weakened assertions or hidden retries occurred.

All ports/repositories and byte payloads in these tests are fixtures; no physical
phone, real account, network writer, camera or iOS UI was used. Existing detail/
calendar screenshots and UI counts below belong to their prior runs: UI tests
were **compiled, not executed again** in this non-renderer slice, and there is no
new editor preview/device/visual acceptance. Studio still reports the new source
outside imported content roots; compiler/lint are the verified fallback, not an
IDE inspection result. No forbidden app/data/other-feature implementation imports,
new Gradle dependency, module or active DI binding was added. Local final log:
coffeepeek-home-editor-mvi-atomic-final-2026-10-17.log, outside tracked sources.

## Android detail renderer — 2026-10-17

- Populated Android API exposes a detail body, not a second destination/sheet.
  App owns its lifecycle owner, account generation, date/method formatting,
  navigation, outer-sheet dismissal and configured photo loader/viewer.
- Runtime entry obtains the lifecycle ViewModel, collects only while STARTED,
  uses current callbacks and checks account/revision before delivering effects.
  Denied access immediately hides private content and prevents an initial request.
  Confirmed deletion reports dismissal availability to the outer sheet; back and
  conflicting actions do not cancel it silently.
  During integration the app must give each overlay its own lifecycle/store and
  clear it on close, rather than retaining a closed detail VM in history's store.
- Stateless detail content and feature-owned heading, recipe, rating, original
  photo strip, transparent cutout, actions, status and confirmation have colocated
  PreviewLightDark previews. Padding, radii, typography and 1/2/3-photo geometry
  follow the current main source; gallery and cutout remain separate albums.
- Text/accessibility strings are Android resources. This new renderer currently
  has Russian default resources only; no additional localization is claimed.
- UI/API live in androidMain because core/design-system currently has no Native
  target. Shared ViewModel/domain/data stay commonMain. A build convention enables
  Compose only for Android while preserving the existing Native VM targets;
  no iOS UI or artificial Compose runtime dependency is added to common code.
- API is now populated and registered: **31 modules**. API depends only on the
  minimal Compose contract; impl's Android source set exports API and consumes
  design-system, never data, legacy or another feature's implementation.
- Prepared fixture UI/entry tests are self-targeted and use fake repositories,
  image ports and local screenshots, not production Application/Koin/network.
  Verification results are recorded below. Physical Pixel 7,
  real images, keyboard/editor and live-write acceptance remain open.

Build preparation: the first configuration used a Compose UI catalog group
instead of the artifact; corrected before any tests ran. The next run exposed
Compose compilation being applied to Native despite Android-only UI. After that
known failure its remaining packaging work was cancelled, then the new convention
limited the compiler to Android. Native compilation, Android test compilation and
all 24 MVI unit cases subsequently passed in 48s. These setup failures are not
reported as successful test executions. Existing unrelated AGP/Compose preview
deprecations and legacy repository warnings were not modified.

Verification: 2026-10-17, base **04c746e** plus this renderer diff, main still
**0137ee7**. The full selected set passes **944 unit executions**, zero failures,
errors or skips (Home domain/data/MVI 14/23/24, legacy Home 16, history 8/46,
core presentation 3, app Direct/Play 405 each; valid up-to-date results reused).
Direct/Play APKs and packaged DEX guards, API/impl lint, test APK and shared
impl iOS Simulator Arm64 compilation pass. Full validation took 4m; repeated
final validation after test synchronization took 3s with configuration/cache reuse.

The self-targeted debug host (`com.coffeepeek.feature.homecheckin.impl.test`,
targetSdk 36, generic Application, no INTERNET permission) ran on read-only AVD
Pixel_10_Pro, emulator-5554, API 37 / arm64 / 1280×2856 / density 480, unlocked.
No physical phone or production APK/account was touched. First UI run: **12 cases,
2 failures** in dialog visibility checks. Parent recomposition did not guarantee
that the separate dialog window had finished layout. Added bounded waiting for
the real visible semantic target, preserving the display/action assertions.
Targeted delete retry: **1/1 pass**; the complete suite then passed **12/12** in
53.531s, with no skipped cases or remaining failures. It covers both themes,
moderation/unknown visibility, separate image ports/actions, confirmation/cancel,
busy actions, denied initial load, current callbacks, account generations, immediate
revocation, small window/large font and brand-font rendering.

Light/dark fixture screenshots were inspected locally: recipe/heading/rating,
notes and actions render without obvious clipping. This is not production pixel
parity, real protected-photo, modal-anchor, keyboard/editor or Pixel 7 acceptance.
Prepared previews were compiled, not all individually rendered in Studio; Studio
still reports the new source outside imported content roots. No IDE-clean claim.
Local sanitized logs/screenshots remain in /private/tmp, not tracked sources;
final logs: coffeepeek-home-detail-renderer-verified-2026-10-17.log and
coffeepeek-home-detail-ui-final-2026-10-17.log. The UI APK SHA-256 is recorded in
the local verification output; no production deployment was performed.

## Detail MVI slice — 2026-10-17

- The populated impl now owns a typed detail/delete ViewModel based on
  core/presentation, with separate State/Action/Event files. No Compose renderer,
  public API placeholder or application dependency is introduced in this slice.
- It loads detail only, never history/calendar/statistics just to open a card.
  Reloads coalesce; invalid/mismatched IDs do not become another record's content.
  Offline refresh retains the current snapshot, while Unauthorized/Forbidden/
  NotFound removes it. Unknown exceptions become typed generic issues, not UI text.
- Delete requires explicit confirmation, blocks conflicting actions and emits
  exactly one confirmed Deleted outcome for app-owned invalidation/dismissal.
  Failed/ambiguous delivery retains the record where access permits and requires
  new user confirmation to retry. There is no automatic retry or optimistic delete.
- A VM belongs to one preparation and one account lease. Composition must guard
  the repository too; the presentation predicate is not server authentication.
  Revocation/owner clearing erases private state and rejects non-cooperative late
  responses. A new account needs a new lifecycle owner/VM; token renewal must not
  be treated as account revocation by the future adapter.
- Photo/edit/close/deleted effects carry account generation and operation revision.
  The future runtime adapter must check canDeliver before invoking callbacks and
  hide content immediately when its account lease ceases to be current.
- Fresh main is still 0137ee7. Source parity anchors remain the unchanged Home
  detail/editor files, global creation and community edit callers. Pixel 7 is
  not attached in this turn, so running-app/visual baseline is still unchecked.
  This slice changes no renderer; screenshot/keyboard/modal parity is not claimed.
- User chose to continue without the phone. Source/fixture-based renderer work
  may continue, but the physical-device visual/keyboard gate must remain open;
  do not repeatedly block code preparation on device availability.
- Impl currently uses the shared-library convention for common MVI. When adding
  Compose, preserve its existing Native targets with an appropriate convention;
  do not move the ViewModel to Android merely for preview tooling.

Verification for this slice: base **9c9d4f2** plus the detail-MVI diff, current
main **0137ee7**, 2026-10-17. All **24 detail MVI cases** pass; Home now has 61
cases across domain/data/impl. The full selected set reports **944 executions**,
zero failures/errors/skips: Home 61, legacy Home 16, history 54, core presentation
3 and app 405 per Direct/Play variant (valid up-to-date results may be reused).
Both APK/DEX checks, impl lint and impl iOS Simulator Arm64 compilation pass.
The initial full run took 10m14s, actively processing DEX rather than waiting on
a network connection; read-only process diagnostics stayed outside the repo.
After adding two final event-validity cases, the entire selected set passed
again in 4s. No test failed, assertion was weakened or run was hidden. Local
final log: `coffeepeek-home-detail-mvi-validation-final-2026-10-17.log`.
Studio cannot inspect the new impl until its module is imported, so no clean
IDE inspection is claimed; Gradle compiler/lint are the verified fallback.
No renderer changed, so no UI/preview/device or live-write acceptance is claimed.

### Source baseline for the upcoming renderer (not device acceptance)

HomeCheckInDetail.kt and HomeCheckInEditor.kt have no diff versus main 0137ee7.
Keep these audited properties when replacing legacy components with CoffeePeek
primitives; neither missing device coverage nor migration is a redesign mandate.

| Area | Existing source contract |
|---|---|
| Detail body | LazyColumn: start/end 20dp, top 4dp, bottom 24dp; 20dp item gaps. Heading block gap 10dp, bean title headlineSmall/Bold. |
| Recipe card | Radius 20dp; padding/gaps 16dp; icon tile 44dp/radius 14dp, icon 24dp. Method value titleMedium/SemiBold; metric gaps 24dp. |
| Detail photos | Original strip uses 16:10 for a single photo. Cutout container 160dp high/radius 20dp/padding 12dp and preserves transparent image presentation. |
| Detail actions | Primary Edit, destructive text Delete; a separate destructive confirmation at the overlay boundary. Ordinary photo and cutout open separate supplied albums. |
| Editor rhythm | headlineSmall/Bold create/edit title, keyboard-aware weighted body, error summary above scrolling fields, existing CpDimens spacing4/spacing6 rhythm. |
| Editor fields | Visibility, optional taste rating, then date/method/bean/dose/duration/note/drink/photos steps. Bean search is an internal picker; edit keeps server photos without exposing replacement uploads. |

Reuse existing CoffeePeekButton and CoffeePeekFormStep where equivalent. Missing
photo/picker-specific composition must remain injected/feature-owned; never import
legacy app components or Shop impl to avoid implementing its own small adapter.

## Current checkpoint — prepared foundation

- Existing domain now also owns preparation input/upload types, read/write
  repository contracts and field validation. Errors are typed reasons, not
  localized text or raw backend payloads. The future impl must map every reason
  to feature resources while preserving current explanations.
- New populated data module owns backend/DTOs, mappers, private repository and
  narrow factory. Clients and account lifetime remain application-owned. The
  old `features/home-checkin` repository and all application bindings are unchanged.
- Paging rejects invalid arguments rather than silently clamping the existing
  read-domain contract. Calendar bounds are forwarded as inclusive civil dates.
  Signed uploads use a separate client and preflight the complete reservation
  with the existing public-upload URL guard before transmitting any image.
- No statistics or protected-photo download interface is added without a new
  consumer: legacy statistics stay intact, and configured image/viewer ports
  remain the intended presentation boundary.
- No UI has changed in this slice. Live-form baseline comparison stays open
  before the editor/detail migration, not a claimed result of foundation tests.

## Goal and stack

Extract existing Home preparation details and create/edit/delete without changing
their product behavior or visual design. Combined history already has a pure Home
read boundary, but editor/detail still delegates to application-owned legacy code.
Do not move unrelated profile, catalogue or community business logic here.

- Work in `feature/home-check-in-refactoring`, created from the published history
  tip, not main. Its PR targets `feature/check-in-history-refactoring` (#95).
- Keep one child PR for this feature, normally within the 100–150-file review
  guide. This planning commit starts that checkpoint, not another task-sized PR
  to close before implementation.
- History's code checkpoint is published. Its populated Pixel 7/account/write/
  accessibility gates remain open in `check-in-history/COMBINED_HISTORY_PLAN.md`;
  Home planning does not certify them or make #95 ready for release.
- Re-fetch main before implementation and recheck actual entry points. Inherit
  parent updates normally; retarget a child after its parent reaches main. No
  force-push or merge to main/develop is part of this plan.

## Current ownership and entry-point audit

| Existing capability | Actual source / consumer | Migration decision |
|---|---|---|
| Home history/calendar reads | `feature/home-check-in/domain`, Android `LegacyHomePreparationReader`, `HomeHistoryReaderAdapter` | Keep read contracts and civil dates; extend this domain, not a second Home domain. |
| Domain, validation, HTTP and uploads in one artifact | `features/home-checkin` | Reproduce active contracts in the new boundaries. Keep the old artifact for shared/iOS and unmigrated consumers. |
| Detail/create/edit | `composeApp/.../feature/homecheckin/ui/HomeCheckInDetail.kt`, `HomeCheckInEditor.kt`, `HomeCheckInViewModel.kt` | Extract incrementally into Home impl; retain legacy UI for iOS. |
| Combined-history overlay | Android `HistoryHomeOverlay`, `AndroidHistoryHomeBridge`, `HistoryHomeEditorFactory` | Replace legacy-editor delegation in application composition, never inside history impl. Retain account leases and invalidation. |
| Global creation | `CheckInCreateFlow` from `CheckInTabContent` and `CommunityScreen` | Audit and switch these Android entrances too; replacing the history overlay alone does not migrate creation everywhere. |
| Community edit | `Screen.EditHomeCheckIn(id)` | Preserve the history destination and initial edit ID. No new root route is required. |
| Own-profile Activity calendar | `PersonalProfileActivityCalendar` with legacy Home/Shop VMs | Separate profile read/modal consumer; preserve its UI and bindings. Not a Home editor or an already migrated history renderer. |
| Standalone Home screen/statistics | `HomeCheckInScreen`, `beanStats` / `methodStats` | No current root navigation call site found for this standalone screen. Do not rebuild its history/calendar/statistics UI; retain legacy contracts while consumers remain. |

Fresh-main `Navigator.kt` maps VisitedPlaces/HomeCheckIns/EditHomeCheckIn to the
combined `ProfileCheckInsScreen`; Android migration uses `CheckInHistoryDestination`.
Legacy repository/editor/detail match main. The stack's VM additionally rejects
stale history/calendar/detail responses: do not lose those protections by copying
the less guarded main VM verbatim. This is a source audit, not a new physical-
device Home acceptance run.

## Target boundaries and module gates

There are **28** registered modules in the history parent: base 23, four history
modules and one Home read domain. This child now registers **31**, adding populated
Home data, detail impl and Android API while legacy remains. Register
modules only with real code through `Modules.feature.homePreparation`. Keep
`Modules.feature.homeCheckIn` for the old artifact's remaining consumers.

| Boundary | Responsibility and hidden implementation | Dependencies, consumers and reason for a module |
|---|---|---|
| `api` — detail populated | Minimal Android detail body/caller contract; editor contract follows its real implementation. No ViewModel, screen implementation, DTO or Koin. | Compose runtime/UI only. App and genuine cross-feature callers consume it without access to Home implementation. |
| `domain` — existing, extended | Preparation/input/photo/visibility models, validation and read/write capabilities in model/repository/usecase. | Pure Kotlin and explicitly justified domain dependencies. Own data/impl and composition consume it; packages in the mixed legacy artifact cannot enforce this independence. |
| `data` — prepared, inactive | HTTP APIs/DTOs, envelope/error mapping, signed uploads, mappers and repository implementation; only narrow construction factories public. | Own domain, required core/network, Ktor/serialization. App constructs it; UI never consumes it. Separate artifact prevents transport exposure through domain. No new database/cache is needed. |
| `impl` — detail/editor logic prepared, inactive | Common detail/delete and editor/save/catalogue/photo MVI; Android detail HomeCheckInApiImpl, runtime/stateless content, components/resources/previews. Editor UI/factory follows. | Common: own domain/core presentation. Android: own API, design-system and lifecycle/Compose. No data/legacy/app imports; artifact enforces presentation-versus-transport boundaries. |

Use constructor DI in domain/data. Root Koin and platform/legacy adapters remain
in `composeApp/androidMain/di/homecheckin`; no feature DI module, root Navigation 3
swap or generic core module. History keeps its own ports and app-owned adapters,
not a dependency on Home domain/data/impl.

Shared models/data/ViewModels belong in commonMain. Android entries/platform ports
and Android-only UI belong in androidMain; shared Compose UI may stay commonMain
when platform-independent. Preview tooling alone is not a reason to move it.
iOS keeps its current UI/writer/DI path.

Impl layout: `HomeCheckInApiImpl.kt`, `navigation/`, `ui/<Name>ViewModel.kt`,
`ui/compose/<Name>Screen.kt`, `compose/component/`, `compose/model/`.
Detail and editor are two presentation surfaces; create/edit share one editor.
Bean selection and date/photo sheets are subordinate controls, not new features.

## Behavior contract to preserve

- New drafts start private with taste score 5. Method, bean, dose, duration,
  rating, note, drink and photos remain optional. Preserve validation bounds,
  field errors, input limits and correction of previously invalid fields.
- Keep `preparedOn` a civil date and `preparedAtUtc` an instant. Reselecting the
  same date preserves recorded time; retain future-date limits, explicit offsets
  and the server's timestamp-precision limit.
- Edit retains historical product identity, custom drink, returned visibility,
  unknown moderation and existing photos. Current PUT does not upload/replace
  photos; do not invent that capability during extraction.
- Preserve `/api/v1/check-ins/home` CRUD/calendar and inclusive Home day bounds.
  Photo reservations use `/api/Photos/check-in`; signed PUT retains
  `x-amz-tagging: is_permanent=False`. External upload uses the separate upload
  client, never an authenticated gateway redirected to a signed URL.
- Keep original gallery photos and drink cutout separate, first-source extraction,
  candidate selection/retry/skip, readiness before submit and five-original limit.
  Reuse the existing processor; no second extraction pipeline.
- Public choice is not moderation approval. Retain Pending/Rejected/Unknown and
  an explicit notice when the saved response does not confirm chosen visibility.
  Retain the saved ID; never automatically repeat creation to repair visibility.
- Keep `Result` and coroutine cancellation. Retain drafts on failure, safe error
  text, conflicting-write guards, stale-result rejection and account revocation.
  Ambiguous delivery must not trigger an automatic duplicate write.
- Preserve one analytics flow across type choice/editor/save and current consent,
  exposure and cancel policy. App adapts a narrow port; no application analytics
  imports or personal field data in feature telemetry.
- Inject configured image loader/photo viewer/picker/camera/extraction ports.
  Existing coffee picker and brew-method catalogue stay behind app adapters until
  their features migrate. Home cannot import composeApp or Shop/catalogue impl.

## Ordered work

1. [x] Publish history/calendar checkpoint; inspect Home sources, actual callers,
   Gradle registration, bridges and existing tests against fresh main.
2. [x] Isolate this plan in a child branch; identify module responsibilities.
3. [ ] Before coding, compare current Home detail/editor/bean/photo controls in
   the app and record baseline geometry, type/color and keyboard/modal behavior.
   Recheck main and all Android creation/edit callers first.
4. [x] Extend existing domain with independent input/write/detail contracts and
   validation. Reuse read concepts where semantics match; no DTO/legacy aliases
   or forwarding-only use cases. Port field/date/visibility tests first.
5. [x] Populate data/backend, mapper and repository using existing clients through
   a narrow factory. Port fake-HTTP envelope/error/upload contracts; cover reads
   needed to replace the compatibility reader too. Keep legacy bindings active.
6. [x] Prepare minimal API and MviViewModel-based detail/editor: immutable State/Action
   and actual one-off Event. No old BaseViewModel or history/statistics load just
   to open a form. Guard save/delete/search/extraction races and late results.
   - [x] Prepare account-owned detail/delete MVI and effect validity checks.
   - [x] Add the real Android detail entry and its lifecycle/runtime adapter.
   - [x] Prepare editor/create/save/search/photo MVI and internal presentation ports.
   - [x] Prepare public editor entry and app-owned catalogue/media/picker/
     extraction/date/analytics adapters. Active binding belongs to step 8.
7. [ ] Extract independent components into own files, reuse CoffeePeek primitives,
   feature resources and same-file paired previews. Preserve keyboard/scroll,
   outline/spacing/type and bounded ripple. Add shared design-system primitives
   only for an actual shared need, not a parallel redesign.
   - [x] Prepare Android detail content/components, resources and paired previews.
   - [x] Prepare editor content/components/resources, paired previews and internal
     lifecycle adapter using fake ports.
   - [ ] Verify actual keyboard/modal/visual parity with configured app ports.
8. [ ] Switch Android app delegation through Home entry/factories: history,
   global creation and community edit. Preserve leases, photo/catalogue ports
   and single analytics flow. Refresh history once after confirmed mutations;
   close after delete, retain notices and correct back/cancel behavior. New Home
   reads use new data; unrelated legacy/iOS DI stays unchanged.
9. [ ] Remove only obsolete Android bridge paths/dependencies after all migrated
   entrances work. Keep shared Home/profile-modal/iOS callers; do not delete the
   old artifact globally while any remain.
10. [ ] Complete verification, document limits and review the child diff against
    #95. Choose another business owner only after this checkpoint.

First coding slice: **domain validation/write contracts and data contract tests**.
UI and active binding switches follow within the same feature checkpoint.
The subsequent detail/delete/editor MVI, Android renderers/internal runtime,
public editor factory and app-owned port/analytics adaptation are prepared.
Next is Android data/DI and route activation. Keep physical-device gates
open while proceeding without a phone, as requested.

## Verification and completion gates

### Foundation verification — 2026-10-17

Source/build identity: `feature/home-check-in-refactoring`, parent c726944 plus
this Home foundation diff, re-fetched main 0137ee7. No device deployment or
production-account request occurred. Android UI, DI and iOS rendering are unchanged.

- **37 Home unit cases pass**: 14 domain (including three existing read-model
  cases) and 23 new data/MockEngine cases. Total selected results: **917 executions**,
  zero failures/errors/skips: Home 37, legacy Home 16, history domain/impl 54 and
  app 405 per Direct/Play variant. Valid up-to-date results may be reused by Gradle.
- Domain/data lint, both app debug APKs and both packaged DEX guards pass. Both
  new Home domain/data compile for iOS Simulator Arm64; no iOS UI was implemented
  or device-tested. No UI suite is claimed for this non-UI change.
- The first full validation passed in 2m26s; after adding the last metadata guard,
  the full relevant set passed again in 9s. Final local log is
  `coffeepeek-home-foundation-validation-final-2026-10-17.log` outside the repo.
- An initial CLI attempt was stopped without a test result when an IDE build was
  found still active after its request timed out. Subsequent Gradle checks ran
  sequentially after IDLE was confirmed; no failed test was hidden or weakened.
- Studio's existing-domain file inspection returned no issues. Data-file
  inspections are unavailable until the new Gradle module is imported; no clean
  IDE-inspection result is claimed for those files. Compiler and Gradle lint pass.
  Existing AGP/Compose deprecations in unrelated modules remain out of scope.
- Boundary audit: domain imports Kotlin only; data uses own domain/core network,
  has internal DTOs/backend/mappers and a private implementation. No app/legacy/
  other-feature imports, UI dependency or Koin wiring were introduced.

At the foundation checkpoint the remaining work was current-app baseline, public
entry/presentation, resource mapping, Android switching and device/release checks.
The newer detail-MVI slice above completes only the detail/delete logic portion.

Calendar evidence is separate: 867 selected unit executions, 20 self-targeted history UI
cases, Direct/Play APKs/DEX, history lint and shared Native compilation passed
before c726944 publication. Fake light/dark calendar QA is not populated physical-
device/account acceptance.

- Domain: optionality/bounds, field correction, visibility/unknown states, civil
  dates versus instants, timestamp precision and meaningful coordination.
- Data/MockEngine: POST/PUT payloads, product ID/slug, missing/malformed/failed
  envelopes, owner detail/delete, protected photos, reservation/tagging, no
  credential forwarding, cancellation and no silent write retries.
- MVI: create/detail/edit, debounce/out-of-order search, late detail/method retry,
  duplicate writes, extraction selection/retry/skip, unconfirmed saved visibility,
  retained failed draft and account revocation.
- Self-targeted UI/fake ports: light/dark, small windows/large fonts, keyboard,
  focus/scroll, errors/accessibility, bean/date/photo controls, sheet placement,
  dismissal and detail/edit/delete confirmation. No production Application,
  network, real camera or account data in that host.
- Composition: every Android entry, factory/lease lifetime, confirmed mutation
  invalidation, initial edit ID, photo callbacks and one consent-aware analytics
  flow. Rerun affected history and parent regressions.
- Both app APK variants/DEX guards, affected unit suites/lint/instrumentation
  APKs and source-set/import/cycle checks. One Gradle build at a time with
  `--max-workers=6`. Shared Native compilation is compatibility, not iOS acceptance.
- Pixel 7: resolve serial/version/lock, compare baseline/new form/detail and
  keyboard/photos read-only. No data loss or silent downgrade. Real CRUD/account
  changes require a safe test account and explicit permission.
- Record date/source/variant/device, executed versus compiled tests, counts,
  failures/retries, visuals and untested gates here. Keep private diagnostics
  outside tracked files. APK assembly alone does not close the feature.

Excluded: iOS UI migration, profile/Activity extraction, separate Home history or
statistics screens, new server capabilities, photo editing, root navigation
replacement and wholesale removal of legacy code.
