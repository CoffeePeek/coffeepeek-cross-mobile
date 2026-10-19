# Android refactoring — current roadmap

Audit date: **2026-10-19**. Current main: **dfb00e63**; application-code baseline
remains **28917758** (the last commit changes iOS CI/signing tests only).
The current inventory and ordered work below supersede dated 0137ee7/4976e53
checklists retained later for provenance. Inspect actual entry points, not just
screen filenames, before extracting another capability.

## Current status after parent-first main synchronization

Main was integrated starting at #43, then propagated through #95 -> #99 ->
#101 -> #102 -> #104. Local main is fast-forwarded to the same baseline. See
[MAIN_SYNC_AUDIT.md](MAIN_SYNC_AUDIT.md) for reconciliation and verification.
This synchronization does not activate Coffee or migrate new profile/auth UI.
The final CI-only dfb00e63 update was also propagated parent-first; its two
signing-profile unit tests pass. Android/KMP source did not change after the
verification below.

There are **36 registered application/library Gradle modules** at the Coffee tip:
one composeApp, five core modules, 24 modules in six api/domain/data/impl feature
families, four legacy modules and the two existing single-module features
`features/home-checkin` / `features/author-blocking`. Included build-logic projects
are separate from this count. The existing plural features remain compatibility
paths, not templates for new migrations.

| Owner | Implemented / active on Android | Still outstanding |
|---|---|---|
| Core and build-logic | Coroutines, network, database, typed MVI, design-system; Modules catalog/conventions; shared fractional ratings, gold hearts, slot card and expandable text | Scoped reuse; remove legacy equivalents only after consumers migrate, not a new core/base/common project |
| Favorites | Four boundaries; active Android entry for server-backed shops/roasters/drinks, account guards and app-owned DI | Live account/persistence/photo/accessibility acceptance and remaining analytics parity; never restore the local server-favorite writer |
| Shop Report | Four boundaries; active Android reason selection/submission entry and grouped accessible choices | Safe live submit/error/uncertain-delivery acceptance; not the separate check-in abuse-report flow |
| Shop | Four boundaries; active detail, menu gallery, public check-in list and creation sheet; shared controls/prod geometry; block/report menus delegate through revocable app adapters | Full-app Sandbox blocking fixtures, protected-photo visual acceptance, media/permissions/extraction, guest/owner/return and authorized writes |
| Combined history | Four boundaries; three Android aliases use combined Shop/Home calendar/timeline, paging, filters, visit markers and current-main feed/thumb/detail/original photo contracts | Populated live/protected-photo/accessibility checks. Profile Activity modal remains a separate legacy consumer |
| Home check-ins | Four boundaries; history/global creation use new data/entries, media/analytics bridges; distinct photo variants reach calendar/detail/gallery, 41/41 isolated owner UI cases pass | Real media/upload/account/CRUD acceptance. Profile Activity and shared/iOS consumers remain legacy |
| Coffee catalogue | Four boundaries; repository/picker, typed list/detail MVI, Android entries/content/components/resources/previews and isolated tests prepared | **Not activated**: tab/detail routes/Koin/picker still use composeApp implementation. Finish analytics adapters, Android DI/routes/picker and full Sandbox checks before switching |
| Sandbox / shared UI support | Separate offline app with actual app graph, RAM fake backend and explicit run modes; common rating/heart/card changes also affect normal consumers | New-main author-blocking scenarios and protected-photo lifecycle fixtures need explicit fake coverage; Sandbox does not certify live APIs/OAuth/camera/storage |
| Discovery, roasters, profile/account, auth, community/content reporting, settings, contributions | Current-main product behavior retained; new four-boundary extraction not started | Extract by business owner after Coffee, auditing current routes and dependencies first |
| Author blocking | Incoming main implementation in features/author-blocking plus shared app UI; Shop adapters honor visibility and forward eligible author-block requests | Full-app Sandbox blocking transport fixtures; not a completed four-boundary migration, preserve behavior while later extracting UI/ownership |

Five feature families have active Android entries; Coffee is the sixth prepared
family, not a sixth completed integration. No label closes every release gate.
iOS UI migration remains deferred; Native compilation is only compatibility.

## Current ordered work

1. [x] Reconcile main 28917758 in #43 and propagate through all open children.
   Preserve Android entries, atomic sessions, incoming links/web-only password
   recovery, and injected fake Sandbox image transport.
2. [ ] Close identified **current-main parity verification** before another screen switch:

   - [x] History/Home feed/thumb/detail variants, explicit originals, album indices
     and calendar thumbs in #95/#99, propagated to children. Recorded final-tip
     checks: 704 unit results and 41 Home + 21 history isolated UI cases pass.
   - [x] Shop block/report menu integration in parent #43 (`6dc3f115`), propagated
     through the stack. Parent checks: 552 selected unit results and 39/39 isolated
     Pixel 7 UI cases pass after the documented existing-selector correction.
     Final tip `46b1943a`: 650 unit results, Debug/Sandbox APK/DEX/isolation and
     repeated **40/40** isolated Shop UI cases pass, no failures/retries.
   - [ ] Full-app Sandbox block/unblock/failure/login/visibility fixtures and
     protected-photo lifecycle scenarios. The fake backend currently lacks
     `/api/v1/me/blocks`; self-targeted menu tests do not certify that app graph.

   Preserve protected permissions/cache/session invalidation and blocked-author
   filtering. Live media/account/write acceptance remains a separate authorized
   release gate. Keep fixes with their owner; no feature UI in composeApp.
3. [ ] Finish Coffee app-owned analytics: normalized search/filter fingerprint,
   consent/session invalidation, result/open position and detail/section actions.
   Features receive narrow ports, never an analytics SDK.
4. [ ] Wire Coffee Android factories, Koin, tab/detail/restored routes and picker.
   Keep existing root navigation/iOS fallback. Do not invent a Coffee app-link:
   current-main public links cover shops/roasters/users.
5. [ ] Verify integrated Coffee in full Sandbox, then authorized live-read/
   platform checks: themes, text scaling, gallery/share/browser return and cancel.
   Keep unit, self-targeted UI, app composition and real backend evidence separate.
6. [ ] Close the Coffee engineering checkpoint with acceptance gates documented.
   Start the next owner in a child branch/PR using the 100–150-file guide. Roasters
   are a candidate because Coffee links to them, not work authorized by this audit.
7. [ ] Later extract discovery, profile/account, auth, community/reporting,
   settings, contributions and author-blocking UI. New screens/ViewModels/business
   code belong in feature; composeApp retains startup/root DI/navigation and thin
   platform/legacy adapters only.
8. [ ] Remove old code only after all platform consumers migrate. Do not delete
   shared/iOS paths or revive retired review/journal screens to fill a diagram.

Main declares **41 serialized route contracts**: five graphs, five tabs and 31
other contracts, not 41 unique screens/features. ResetPassword is a restoration
redirect after web-only recovery. Three history routes share one screen; profile
Activity is a separate modal, Shop creation is a sheet, and ReportReview remains
compatibility code rather than a new editor requirement.

## Current stack

| PR | Branch | Base |
|---|---|---|
| #43 | feature/core-design-system-controls | develop |
| #95 | feature/check-in-history-refactoring | feature/core-design-system-controls |
| #99 | feature/home-check-in-refactoring | feature/check-in-history-refactoring |
| #101 | feature/sandbox-application | feature/home-check-in-refactoring |
| #102 | feature/shared-rating-component | feature/sandbox-application |
| #104 | feature/coffee-catalog-refactoring (current working tip) | feature/shared-rating-component |

No PR is merged into main/develop by synchronization; published history is not
rewritten. Root Navigation 3 and native iOS presentation remain separate decisions.

## Fresh verification — 2026-10-19

Shop author-menu follow-up: owner runtime `6dc3f115`, propagated to Coffee-tip
runtime `46b1943a`. **650/650** selected unit results (Shop impl 99, author-blocking
11, app Direct 540), Direct Debug/Sandbox APK/DEX/isolation, Shop test APK and app
Direct instrumentation compilation pass (2m03s). Pixel 7 / API 37 complete Shop
suite repeats **40/40** (67.277s) with fake ports, zero failures/retries at the tip.
Parent 39-case run and its repaired selector failure remain in the owning Shop
plan. Only self-targeted test APKs were deployed; no ordinary app update, real
block/write, iOS UI or manual pixel acceptance. Full-app fake blocking transport
and protected-image lifecycle scenarios remain the next parity gate.

Photo contract follow-up: history `abc5876f`, Home `fe80c7e0`, propagated to tip
`28c64bff`. Owner history 21/21 and Home 41/41 isolated Pixel 7 UI cases pass,
no failures/retries. Whole-tip selected unit regression **704/704**, Direct Debug
and Direct Sandbox APK/DEX/isolation, both feature test APKs and app Direct
instrumentation compilation pass (2m21s). Final-tip device repetitions also pass:
Home **41/41** (73.350s), history **21/21** (38.937s), no failures/retries. APK
identities and logs are recorded in the owner plans. These fake-port suites do not close protected live
photo, account/write, media, analytics, iOS or Shop blocking-menu acceptance.

Source 32cc4d57 plus documentation-only audit edits: **1704 Android unit results**
pass with zero failures/errors/skips; Direct/Play Debug APKs/DEX guards, Direct
Sandbox APK/isolation/DEX, both app test APKs, Coffee/Shop impl lint and Direct
app lint pass. The first broad packaging run hit D8 Java-heap exhaustion; the
sequential retry with a temporary 5 GiB heap passes without changing project
settings. Four build-logic tests and full app Kotlin compilation for iOS Simulator
Arm64 also pass (compatibility only). No physical-device test or installation was performed;
previous Pixel 7 evidence below is historical, not renewed live acceptance.

## Historical baseline notes (0137ee7)

Latest parent synchronization includes main #97–98's personal/public profile
redesign unchanged. Profile migration remains **not started**. Main #96 is
reconciled with #43, including actual
server-backed shop/roaster/drink favorites, offline photo extraction and exact
visit times. See [MAIN_SYNC_AUDIT.md](MAIN_SYNC_AUDIT.md) for changed contracts,
verification and history-child follow-up. Older dated 4976e53 notes below are
historical; they do not override the current server contract or latest route audit.

## Historical branch/checkpoint notes

- `feature/coffee-catalog-refactoring` -> rating PR #102: Android-first Coffee
  catalogue migration. Domain/data, picker, shared MVI and Android list/detail UI are prepared
  with contract tests; active screens/picker/DI still use the existing implementation.
  No iOS screen migration or catalogue redesign. See [Coffee plan](coffee/MIGRATION_PLAN.md).
  Includes the user's explicit shared-heart UI correction: one affinity renderer/
  toggle, selected gold and neutral outline, with [verification](../core/design-system/LIKE_PLAN.md).

- `feature/shared-rating-component` / PR #102 -> Sandbox PR #101: one design-system rating
  renderer/input with fractional tenths and size variants, reused by migrated
  feature UI and shared legacy feed/profile paths. Business averages/actions and
  next-feature migration remain unchanged. Android checkpoint verified with 1144
  unit executions, 169/169 final self-targeted UI cases and scoped Sandbox visuals
  on Pixel 7; Native compatibility compilation is not iOS UI acceptance. Evidence is in
  [rating checkpoint](../core/design-system/RATING_PLAN.md).

- `feature/sandbox-application` -> Home PR #99: application-wide offline test
  composition, actual navigation and local transport/media/session fixtures.
  Scoped Pixel 7 app-graph/local CRUD/photo/error/theme/guest walkthrough passed
  on 2026-10-18 after repairing the redesigned-profile fixture; 450 app unit cases
  pass per delivery variant. Unsupported contracts/live release gates remain.
  This is verification infrastructure, not a completed migration of Discovery,
  Coffee, Profile or Community. Track device/contract gates in
  [Sandbox plan](../composeApp/SANDBOX_PLAN.md).

- `feature/core-design-system-controls`, PR #43 → develop: core, favorites,
  Shop Report and Shop migration. New history code is excluded from its final diff.
- `feature/check-in-history-refactoring` → the branch above: the **entire**
  prepared history migration, including four modules, resources, app wiring and
  tests. Continue that capability only there; do not split it into a third PR.
- The base registers 23 modules; the history child registers 28, including the
  populated Home read domain. Existing `features/home-checkin` remains registered
  in both; Home api/data/impl are not part of those parent branches. The next child
  prepares Home data as described below.
- The independent modal fix #92 reached develop and main and is already in the
  migration ancestry. Do not cherry-pick it again.
- Preserve published history; no force-push or merging either PR to main/develop
  is part of this organization step. New checkpoints use the agreed 100–150-file
  guide and may group one to three related features. Fixes to #43 stay there,
  then update its history child. The existing 71-file history extraction stays
  coherent; do not recreate the old small-PR stack.

## Status meanings

**Done** below means the named engineering task is implemented and verified,
not that every consuming feature is production-ready.
**In progress** includes prepared-but-inactive code and integrated code with parity
or release gates outstanding. **Not started** means the new migration has not
started; the corresponding product capability may already work in main.
A compilation/test pass is not evidence of pixel parity or a live write.

## Home activation checkpoint — 2026-10-18 (historical evidence)

History/calendar checkpoint **c726944** is published in #95. Home work continues
in its child `feature/home-check-in-refactoring` / PR #99, not in a new feature.
The detailed source audit, historical checks and remaining gates are in
[home-check-in/MIGRATION_PLAN.md](home-check-in/MIGRATION_PLAN.md).

As of 2026-10-18, the four Home boundaries contain real domain/data and shared
detail/editor MVI plus Android entries/components, resources and paired previews.
App-owned catalogue/media/picker/extraction/date/analytics adapters reuse current
implementations. Module count remains 31; main **0137ee7** is already an ancestor.

Android history now binds new Home data and detail/editor entries. All three
existing history routes, including Community owner edit by ID, use them.
Confirmed create/update/delete refresh history once; detail/form owners and
callbacks are scoped to the current destination/account. Calendar photo
enrichment/counts and unconfirmed-visibility notices are retained.
Global CheckInCreateFlow in tab/feed now delegates to the new Home entry/data
without constructing the legacy VM before the choice. Its single tracker, Shop
handoff, once-only refresh/close and revocable lifetime are app-owned. Home data
Koin definitions serve both entrances. Profile Activity/shared iOS remain legacy.
The unused Android compatibility reader is removed; adapter/lease fixtures use
the new Home reader directly. Shared legacy code remains for profile Activity/iOS.

Current selection: **1054 passing unit executions**, Direct/Play APKs and expanded
DEX guards, both app instrumentation compilations and common/iOS Native
compatibility compilation. The 14 new global navigation/analytics/lease cases
pass in each app variant. On 2026-10-18, physical Pixel 7 Home self-targeted UI
passed **40/40**, including four new runtime photo/failure/lifecycle cases
(targeted 4/4, full 40/40, first pass in that slice). The preceding locale-selector
repair remains documented (initial 35/36, targeted 1/1, then complete 36/36).
Direct Debug was updated in place; startup, empty history,
Home create/cancel, date picker and numeric IME/footer were checked read-only.
Real OS gallery open/cancel/relaunch passed twice without reading/selecting photos.
App-targeted UI is compiled only. Failure history/build identities are in the Home plan.
Populated Pixel 7 protected-photo/detail/edit, broader IME/recreation parity,
actual image selection/decoding/extraction/camera/permission
and authorized live CRUD/account acceptance remain open. These checks do not
establish release completion or an iOS UI migration.

## Completed engineering tasks

- [x] Separate offline `directSandbox`/`playSandbox` APK for real Home/history
  entries, app-owned fake HTTP/media and failure controls. Pixel 7 local fake
  create/edit/delete/photo/error checks pass; ordinary app data is untouched.
  This closes the safe local write-test setup, not the live server/account gates.
  See [Sandbox guide](../composeApp/SANDBOX.md).

- [x] Named Modules catalog and Gradle conventions; concise feature build files.
- [x] Independent core coroutines, network, database, presentation and design-system
  boundaries; no core-to-feature dependency.
- [x] Typed lifecycle-owned MVI state/actions/events with atomic updates and
  explicit feature-owned Result/cancellation/conflicting-action handling.
- [x] CoffeePeek-prefixed reusable components, brand font/assets/tokens and
  colocated paired previews; rendering every IDE preview is a separate QA gate.
- [x] Android api/domain/data/impl entries and app-owned DI/legacy bridges for
  favorites, Shop Report, Shop detail/menu/public check-ins/check-in creation.
  These are implementation milestones, not completion of those whole features.
- [x] Core and legacy fractional-sheet anchor correction; themed switch primitive.
- [x] Source fixes/tests for cache credentials, field error semantics, upload
  purpose, session/cancellation races and account-scoped favorites.
- [x] Fresh main merged at 9e5a533; CI conflict preserves incoming caching/change
  filters and the migration's Java 21/extra Android test APK checks.
- [x] The PR #43 base builds independently without new history modules:
  Direct/Play debug APKs and DEX checks, both app unit variants, Direct lint and
  the application instrumentation APK pass at extraction checkpoint 415a69b.
- [x] Restore the saved-roaster section in Android favorites, including account
  replacement, owner-checked removal and cancellation guards. This closes the
  missing implementation, not the real-account release checks below.
- [x] Correct Shop detail geometry, branded image fallbacks and floating actions.
  Preserve the explicit approved exceptions: all information-card icons yellow,
  14sp values / 12sp captions and readable 12sp type labels.
- [x] Replace photo-source text actions with grouped outlined rows and a separate
  cancel action; add check-in top clearance. Drink selection shows a check and a
  divider after None without replacing an existing saved/custom choice.
- [x] Group Shop Report reasons inside one outlined block with dividers and a
  selected check. Keep accessible single-choice semantics, no initial reason,
  existing validation and typed MVI actions; disable changes while submitting.

## Historical synchronization checkpoint — 2026-10-15

At that checkpoint `origin/main` was **4976e53**. Local main was identical;
both #43 and #95 already contain that commit. Do not create an empty merge or
merge someone else's develop-to-main PR to manufacture an update. Publish
parent-owned UI fixes in #43, then merge that parent normally into #95. The
independent, already merged modal-fix worktree is not an active migration tip.

Next work is ordered, not parallel feature scaffolding:

1. Finish the current visual controls checkpoint and its isolated Android tests.
2. Keep the remaining parent release gates explicit: real saved data/account
   transitions, image loading, guest/owner/return flows, OS picker/camera and safe
   submissions. Fake fixtures do not close these gates.
3. Continue **combined Shop/Home history** in #95: account-bound Home reads first,
   then combined MVI, production-shaped content, route integration and verification.
   Keep legacy combined history active until the replacement reaches parity.
4. Choose another business feature only after that checkpoint and a fresh main
   audit. iOS UI/integration remains deliberately deferred.

## Historical feature inventory versus main 0137ee7

| Owner / current product | Migration state | What still blocks completion |
|---|---|---|
| Favorites: server-backed shops, roasters **and drinks** | In progress; Android entry adapts the existing server source | Preserve main's three kinds, canonical addresses, explicit read/write errors, account replacement and cache-only logout. New drink cards have no invented detail route. Real saved data/photos, login-retry/account journey, process restoration, physical-device/accessibility/locale QA and migrated-feature analytics parity remain release gates. |
| Shop Report: incorrect shop data | In progress; Android entry active | Real safe submit/error/uncertain-delivery flow and paired preview inspection; retain shared/iOS fallback. |
| Shop: detail, menu, public check-ins and creation sheet | In progress; Android entries active | Physical-device visual/guest/owner/return parity; picker/camera permissions and recreation, bundled extraction quality and safely authorized submissions. Prepared review editors are not a product requirement. |
| Personal history: one combined Shop/Home calendar and timeline | In progress; Android code checkpoint prepared in #95 | Combined reads/MVI/content, month photos, Home photo preparation and creation-flow analytics are integrated. Personal author avatars match main; public author navigation belongs to Shop. Pixel 7 populated-history/photo/date/account/accessibility and safely authorized mutation checks remain. |
| Home preparations: create/detail/edit/delete, recipes and photos | In progress; new data/entries active, unused Android read bridge removed | History, Community owner edit and global tab/feed creation use the new Home implementation. Pixel 7 self-targeted UI 40/40 and real-app create/cancel/date/numeric-IME/gallery-cancel-relaunch checks pass. Actual selected/protected-photo/camera, broader IME/recreation and authorized live acceptance remain; profile Activity/iOS stay legacy. See home-check-in/MIGRATION_PLAN.md. |
| Discovery: shop list/search/filter/map and embedded roasters | Not started | Audit actual list/map/filter ownership and current platform map; no independent module for every mode. |
| Coffee catalogue: Coffee tab/list/detail | In progress — four boundaries, shared MVI/picker and Android ScreenContent/entries/components/resources/previews prepared; active screens not switched | Preserve current catalogue/search/detail and links to roasters; app-owned analytics and navigation integration remain required. |
| Roasters: list/detail and saved membership | Not started | Preserve current entry points, contacts, catalogue links and favorites. Favorites parity is an earlier gate, not permission for a wholesale roaster rewrite. |
| Profile/account: personal/public profile, activity calendar, favorites preview, connections, edit, pending deletion | Not started | Main #97–98 redesign is retained, not extracted. Preserve its public-profile dependency, session-aware tabs, existing history/favorites/auth delegates and cached-account fallback; don't duplicate their business ownership. |
| Auth: sign-in, registration, forgot/reset password | Not started as feature extraction | Existing session fixes are not a completed auth migration. Keep recovery/deep-link and Google/platform behavior. |
| Community: current feed/check-in actions | Not started | Current refresh/filter/paging/votes/privacy and owner guards; no resurrection of legacy reviews. |
| Settings: city/theme/legal/share/version/update actions | Not started | Audit preferences and existing app-update integration; no speculative settings/storage/update module split. |
| Contributions: add shop/roaster, suggest/edit changes, own submissions/detail | Not started | Preserve Changes/Shops/Roasters modes and moderation states; don't count each tab/form step as a new business feature. |

No migrated business feature is marked fully closed while its production parity
or release gates above remain open. iOS feature migrations are deferred, not done;
shared Native compilation is only a compatibility check.

## Historical product-shape audit at 0137ee7

Audit anchors are main's `ui/Navigator.kt`, `ui/screen/main/MainScreen.kt`,
`ui/screen/checkins/ProfileCheckInsScreen.kt`, `ui/screen/profile/ProfileScreen.kt`,
`ui/screen/profile/SettingsScreen.kt`, `ui/screen/favorites/FavoritesScreen.kt`,
`ui/screen/shop/ShopReviewsScreen.kt` and contributions' ViewModel.
These findings come from registered renderers and their call sites, not filenames.

- Main has **40 serialized route declarations**: five graph containers, five tabs
  and 30 other destinations. There are 35 routes without graphs, not 35 unique
  screens. Excluding Main and the legacy ReportReview contract leaves 33 content
  route contracts; aliases/modal flows still share screens.
- Authenticated tabs are discovery/Feed, Coffee, Community, Profile and Settings.
  Preserve main's session-aware guest tab selection rather than hard-coding all five.
- VisitedPlaces, HomeCheckIns and EditHomeCheckIn(id) all render the same
  ProfileCheckInsScreen; the last passes an initial home ID for editing.
- History has All/Shop/Home filters, one calendar/date selection, a combined
  compact timeline, per-source pagination, details/photos and creation choice.
  Home uses preparedOn (a civil date), not the Shop UTC query boundaries.
- CreateCheckIn opens the shop picker; shop creation is a modal, not a new
  standalone form route. Home creation/edit/delete stays delegated for now.
- Standalone VisitedPlacesScreen and HomeCheckInScreen (including the latter's
  history/statistics tabs) have declarations but no call sites in main's audited
  composeApp/iosApp/iosXtool source. The root renders ProfileCheckInsScreen instead.
  Do not rebuild those old screens as separate product features; keep compatibility
  code until platform consumers are checked, without inventing new entry points.
- ShopReviews actually displays **check-ins**. ReportCheckIn opens the report
  screen in check-in mode. ReportReview remains registered but has no
  shared Kotlin navigation call site in this audit: preserve compatibility until
  all entry points are audited; do not revive a review editor or delete the route
  just because it looks old.
- Main favorites includes shops and a separate roaster section. Roaster logos
  attached to a shop card do **not** replace saved-roaster functionality.
- ForgotPassword/ResetPassword, account deletion and app-update actions already
  exist. They are not new product features to invent.
- Do not migrate old journal/bean-bag screens merely because unused classes or
  database tables remain. Require a current renderer/entry point first.
- Keep the existing root navigation and native/shared platform fallbacks. A root
  Navigation 3 swap or new iOS UI is not part of completing history.

## Historical history-extraction gates — superseded above

1. [ ] Finish the base's remaining release/visual gates before treating PR #43
   as ready. Favorites' missing roaster section is fixed and automatically tested;
   full account/device QA and the existing Shop/report gates remain. Preserve main
   behavior, not a new design.
2. [x] In the history child, prepare account-bound Home reads and history-owned
   read models/ports. Confirm date-boundary/calendar/page semantics and preserve
   unknown visibility/moderation, protected photos and recipe data.
   Application composition adapts contracts; no cross-feature domain/data/impl
   import. Create Home domain only when the module gate has real consumers;
   no empty api/impl/DI modules or forwarding-only use cases.
3. [x] Combined typed MVI: one filter/month/date, two source cursors, retained
   content/partial failures, duplicate/conflicting-action guards, cancellation,
   stale-response rejection and account replacement versus token renewal.
4. [x] Production-shaped stateless history content: compact Shop/Home rows,
   one calendar/filter, loading/empty/retry, photos/details and creation callbacks.
   Each independent component owns its file and paired fake-state previews.
   No second global Calendar/List mode or full-card redesign.
5. [x] Extend the entry and Android renderer without losing any of the three
   existing history route contracts. Delegate Home edit/create/delete and the
   existing Shop picker. Android uses the new entry; shared/iOS fallback remains.
6. [ ] Verify both source cursors, filters/date reset/month limits, partial-source
   failures/retry, guest/account/token changes, back/photos/editor return and
   initial edit ID. Build Direct/Play/DEX/unit/lint and isolated UI tests; then
   safely verify physical-device/live-read parity. Live writes require a safe
   environment/account and separate authorization.
7. [x] Reconcile main's author avatar and canonical public-author links in Shop,
   and preserve personal-history headers without inventing a new profile link.
   Restore the existing creation flow/exposure policy in app-owned composition,
   with no analytics SDK in feature modules and no user text/URLs in payloads.
8. [ ] Select the next owner only after these gates, repeating the audit against
   the latest main before coding. Later ordering is provisional, not authority
   to restore removed behavior or scaffold all future modules.

## Historical verification limits (not fresh-main acceptance)

Latest controls checkpoint (2026-10-15), parent #43: both Direct/Play debug APKs
and packaged DEX guards, app Direct/Shop/Shop Report lint, and **518 selected
unit-test executions** pass (213 app per variant, 87 Shop MVI, five Shop Report
MVI). **17 isolated UI tests** pass on an owned read-only emulator: 13 form/drink/
photo-source scenarios and four grouped-report scenarios, with both-theme cases.
The earlier detail checkpoint additionally passed 13 detail UI tests; it is not
a claim that all device suites were rerun for this controls-only change.
No physical phone, account data, backend write or iOS renderer was changed.
Light/dark photo-source, selected-drink and report-list fixtures were visually
inspected with the real theme/fonts. These are local fake screens, not live
account parity or confirmation that every IDE modal preview renders.

The pre-extraction combined code passed 794 selected unit-test executions across
libraries and both app variants, Android APK/DEX/lint, 13 selected app-device
tests on a temporary read-only emulator, and shared iOS Simulator compilation.
Those are dated checkpoint results, not certification of every future edit.
A later full design-system run was interrupted; it is **not** a fresh passing
56-test result. No new physical-phone update, authenticated write, data clearing
or native iOS feature switch is claimed for this organization/audit step.
The child must be checked again after restoring the complete feature. Record its
actual result in the history plan; never infer a passing device run from a
successfully assembled instrumentation APK.
