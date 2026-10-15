# Android refactoring — current roadmap

Audit date: 2026-10-15. Product baseline: `origin/main` **4976e53**.
This is the current checklist. Older feature READMEs contain historical checkpoint
notes; they do not override this source audit or certify later changes.

## Branch ownership

- `feature/core-design-system-controls`, PR #43 → develop: core, favorites,
  Shop Report and Shop migration. New history code is excluded from its final diff.
- `feature/check-in-history-refactoring` → the branch above: the **entire**
  prepared history migration, including four modules, resources, app wiring and
  tests. Continue that capability only there; do not split it into a third PR.
- The base registers 23 modules; the history child registers 27. Existing
  `features/home-checkin` remains registered in both. No new Home module exists.
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

## Current feature inventory versus main

| Owner / current product | Migration state | What still blocks completion |
|---|---|---|
| Favorites: saved shops **and roasters** | In progress; Android entry now renders both kinds | The missing roaster section is restored with main's counts/logo fallback/public route/removal behavior and account guards. Automated UI/DI/Result/cancellation checks pass. Real saved data/photos, account journey, process restoration, physical-device/accessibility/locale QA still block full completion. |
| Shop Report: incorrect shop data | In progress; Android entry active | Real safe submit/error/uncertain-delivery flow and paired preview inspection; retain shared/iOS fallback. |
| Shop: detail, menu, public check-ins and creation sheet | In progress; Android entries active | Physical-device visual/guest/owner/return parity; picker/camera permissions and recreation, ML Kit quality/model availability and safely authorized submissions. Prepared review editors are not a product requirement. |
| Personal history: one combined Shop/Home calendar and timeline | In progress; all prepared migration code in history child, **not active** | The prepared Shop-only Calendar/List UI is not parity with main. Complete combined readers, MVI, production-shaped UI, home callbacks and Android integration first. |
| Home preparations: create/detail/edit/delete, recipes and photos | Not started as a new feature migration | Existing main editor/detail remain active through combined history. Extract only the needed read boundary first; do not recreate unused history/statistics tabs from the standalone Home screen. |
| Discovery: shop list/search/filter/map and embedded roasters | Not started | Audit actual list/map/filter ownership and current platform map; no independent module for every mode. |
| Coffee catalogue: Coffee tab/list/detail | Not started | Preserve current catalogue/search/detail and links to roasters; catalog pickers are existing shared app boundaries, not invented screens. |
| Roasters: list/detail and saved membership | Not started | Preserve current entry points, contacts, catalogue links and favorites. Favorites parity is an earlier gate, not permission for a wholesale roaster rewrite. |
| Profile/account: statistics, edit, pending deletion | Not started | Preserve current routes and delegates to history/favorites/auth; don't duplicate their business ownership. |
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

- Main has **38 serialized route declarations**: five graph containers, five tabs
  and 28 other destinations. There are 33 routes without graphs, not 33 unique
  screens. Excluding Main and the legacy ReportReview contract leaves 31 content
  route contracts; aliases/modal flows still share screens.
- Tabs are discovery/Feed, Coffee, Community, Profile and Settings.
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
2. [ ] In the history child, prepare account-bound Home reads and history-owned
   read models/ports. Confirm date-boundary/calendar/page semantics and preserve
   unknown visibility/moderation, protected photos and recipe data.
   Application composition adapts contracts; no cross-feature domain/data/impl
   import. Create Home domain only when the module gate has real consumers;
   no empty api/impl/DI modules or forwarding-only use cases.
3. [ ] Combined typed MVI: one filter/month/date, two source cursors, retained
   content/partial failures, duplicate/conflicting-action guards, cancellation,
   stale-response rejection and account replacement versus token renewal.
4. [ ] Production-shaped stateless history content: compact Shop/Home rows,
   one calendar/filter, loading/empty/retry, photos/details and creation callbacks.
   Each independent component owns its file and paired fake-state previews.
   No second global Calendar/List mode or full-card redesign.
5. [ ] Extend the entry and Android renderer without losing any of the three
   existing history route contracts. Delegate Home edit/create/delete and the
   existing Shop picker; keep active legacy history until parity is verified.
6. [ ] Verify both source cursors, filters/date reset/month limits, partial-source
   failures/retry, guest/account/token changes, back/photos/editor return and
   initial edit ID. Build Direct/Play/DEX/unit/lint and isolated UI tests; then
   safely verify physical-device/live-read parity. Live writes require a safe
   environment/account and separate authorization.
7. [ ] Select the next owner only after these gates, repeating the audit against
   the latest main before coding. Later ordering is provisional, not authority
   to restore removed behavior or scaffold all future modules.

## Verification limits

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
