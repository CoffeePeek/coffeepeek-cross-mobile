# Android refactoring — current roadmap

Audit date: 2026-10-16. Product baseline: `origin/main` **0137ee7**.
This is the current checklist. Older feature READMEs contain historical checkpoint
notes; they do not override this source audit or certify later changes.

Latest parent synchronization includes main #97–98's personal/public profile
redesign unchanged. Profile migration remains **not started**. Main #96 is
reconciled with #43, including actual
server-backed shop/roaster/drink favorites, offline photo extraction and exact
visit times. See [MAIN_SYNC_AUDIT.md](MAIN_SYNC_AUDIT.md) for changed contracts,
verification and history-child follow-up. Older dated 4976e53 notes below are
historical; they do not override the current server contract or latest route audit.

## Branch ownership

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

## Current checkpoint — Home preparations (Android entrances activated)

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
passed **36/36** after one locale-selector repair (initial 35/36, targeted 1/1,
then complete 36/36). Direct Debug was updated in place; startup, empty history,
Home create/cancel, date picker and numeric IME/footer were checked read-only.
App-targeted UI is compiled only. Failure history/build identities are in the Home plan.
Populated Pixel 7 protected-photo/detail/edit, broader IME/recreation parity, OS gallery/camera
and authorized live CRUD/account acceptance remain open. These checks do not
establish release completion or an iOS UI migration.

## Completed engineering tasks

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

## Current feature inventory versus main

| Owner / current product | Migration state | What still blocks completion |
|---|---|---|
| Favorites: server-backed shops, roasters **and drinks** | In progress; Android entry adapts the existing server source | Preserve main's three kinds, canonical addresses, explicit read/write errors, account replacement and cache-only logout. New drink cards have no invented detail route. Real saved data/photos, login-retry/account journey, process restoration, physical-device/accessibility/locale QA and migrated-feature analytics parity remain release gates. |
| Shop Report: incorrect shop data | In progress; Android entry active | Real safe submit/error/uncertain-delivery flow and paired preview inspection; retain shared/iOS fallback. |
| Shop: detail, menu, public check-ins and creation sheet | In progress; Android entries active | Physical-device visual/guest/owner/return parity; picker/camera permissions and recreation, bundled extraction quality and safely authorized submissions. Prepared review editors are not a product requirement. |
| Personal history: one combined Shop/Home calendar and timeline | In progress; Android code checkpoint prepared in #95 | Combined reads/MVI/content, month photos, Home photo preparation and creation-flow analytics are integrated. Personal author avatars match main; public author navigation belongs to Shop. Pixel 7 populated-history/photo/date/account/accessibility and safely authorized mutation checks remain. |
| Home preparations: create/detail/edit/delete, recipes and photos | In progress; new data/entries active, unused Android read bridge removed | History, Community owner edit and global tab/feed creation use the new Home implementation. Pixel 7 self-targeted UI 36/36 and real-app create/cancel/date/numeric-IME checks pass. Populated/protected-photo, broader IME/recreation/OS-picker and authorized live acceptance remain; profile Activity/iOS stay legacy. See home-check-in/MIGRATION_PLAN.md. |
| Discovery: shop list/search/filter/map and embedded roasters | Not started | Audit actual list/map/filter ownership and current platform map; no independent module for every mode. |
| Coffee catalogue: Coffee tab/list/detail | Not started | Preserve current catalogue/search/detail and links to roasters; catalog pickers are existing shared app boundaries, not invented screens. |
| Roasters: list/detail and saved membership | Not started | Preserve current entry points, contacts, catalogue links and favorites. Favorites parity is an earlier gate, not permission for a wholesale roaster rewrite. |
| Profile/account: personal/public profile, activity calendar, favorites preview, connections, edit, pending deletion | Not started | Main #97–98 redesign is retained, not extracted. Preserve its public-profile dependency, session-aware tabs, existing history/favorites/auth delegates and cached-account fallback; don't duplicate their business ownership. |
| Auth: sign-in, registration, forgot/reset password | Not started as feature extraction | Existing session fixes are not a completed auth migration. Keep recovery/deep-link and Google/platform behavior. |
| Community: current feed/check-in actions | Not started | Current refresh/filter/paging/votes/privacy and owner guards; no resurrection of legacy reviews. |
| Settings: city/theme/legal/share/version/update actions | Not started | Audit preferences and existing app-update integration; no speculative settings/storage/update module split. |
| Contributions: add shop/roaster, suggest/edit changes, own submissions/detail | Not started | Preserve Changes/Shops/Roasters modes and moderation states; don't count each tab/form step as a new business feature. |

No migrated business feature is marked fully closed while its production parity
or release gates above remain open. iOS feature migrations are deferred, not done;
shared Native compilation is only a compatibility check.

## Verified product shape — do not redesign during extraction

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

## Remaining work — ordered gates

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

## Verification limits

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
