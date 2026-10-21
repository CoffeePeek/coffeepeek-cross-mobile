# Accepted develop adoption — 2026-10-19

Current baseline is main `dfb00e63` plus approved develop `fc85f8e5`.
Develop's five incoming commits retire drink cutouts/MediaPipe, prioritize menu
photos/remove volume fields, retain cached photo previews during original loads,
adjust profile layout and add native encryption metadata. These product changes
must also reach prepared migration code rather than surviving only in legacy UI.
Synchronize #43 first and propagate normally through its children; do not modify
main/develop or rewrite published branch history. Historical cutout acceptance
below does not override this retirement.

# Migration stack versus main — current and historical audits

## Target-base conflict resolved — 2026-10-19

User approved adopting develop now. Parent #43 at `bb718dbb` merges develop
`fc85f8e5`, reconciles migrated Shop/legacy compatibility and is MERGEABLE into
develop. History `29ef7ea8`, Home `c165d8fc`, full Sandbox `e3ddf72d` and
rating `03272578` inherit it normally; Coffee #104 is the final propagation gate.
No main/develop push/merge, force-push or new iOS screen migration is performed.
Ordinary photos remain; drink cutouts/MediaPipe do not. Dated pre-adoption
main-only results below are historical. Current verification is in the Shop,
history, Home and Sandbox plans; final-tip results are recorded separately.

## Shop author-block menu reconciliation — 2026-10-19

Parent #43 runtime fix `6dc3f115` is inherited through #95 -> #99 -> #101 -> #102
to Coffee-tip runtime source `46b1943a`. Detail preview and full-list menus now
request the existing block/login confirmation through a narrow Shop API identity
and account/visibility-bound app adapter. No block backend/coordinator/UI is
copied into Shop or new feature UI added to composeApp. Own/unresolved/hidden
identities are guarded; the auth user ID is never substituted for public author ID.

Parent checks: 552 selected unit results and final 39/39 isolated Pixel 7 Shop UI
cases pass; prior selector failure and compile retry are preserved in
`shop/ANDROID_PARITY_PLAN.md`. Final-tip selected unit results are 650/650;
Direct Debug/Sandbox APKs, DEX/isolation, Shop test APK and app Direct test
compilation pass (2m03s). The repeated final-tip Pixel 7 Shop suite also passes
40/40 (67.277s), zero failures/retries. Its extra existing shared-heart case is
inherited from the descendants. APK identity/logs are in the owning Shop plan.
Full-app Sandbox still lacks the block transport contract/fixtures; safe menu
fixtures alone do not close that graph, live protected media, real account/write
or release gates. Coffee remains prepared/inactive; no iOS UI migration.

## Photo contract reconciliation — 2026-10-19

The Home/history DTO/calendar/gallery gaps identified in the audit below are now
implemented in their owners: history `abc5876f`, Home `fe80c7e0`, propagated
parent-first to the Coffee tip. Separate feed/thumb/detail/original URLs preserve
album indices; missing explicit previews never fetch originals inline. Main's
active-gallery-page fallback remains application-owned. Upload/edit ownership,
account leases, protected loader and iOS consumers are preserved.

History passed 550 selected unit results and 21/21 isolated Pixel 7 UI cases;
Home's owner checkpoint passed 695 selected unit results, Direct Debug APK/DEX,
test APK and app instrumentation compilation. Home's owner APK also passes
41/41 isolated Pixel 7 UI cases (73.492s), with no failures or retries. Whole-tip
`28c64bff` passes 704 selected unit results (33 Home data, 76 Home impl, 11 history
data, 47 history impl, 537 app Direct), both Direct Debug/Sandbox APKs and DEX/
Sandbox isolation guards, both feature test APKs and app Direct instrumentation
compilation (2m21s). Final-tip Pixel 7 UI repetition also passes: Home 41/41
(73.350s), history 21/21 (38.937s), no failures/retries. APK identities and logs
are in the owner plans. Normal CoffeePeek/Sandbox device packages are unchanged.
This closes the identified code contract gap, not live protected media, actual
camera/upload/account/release acceptance. Shop menu integration is addressed
by the subsequent owner slice above, with its own separate evidence.
See the owning history/Home plans for source identities and exact evidence.

## Latest main dfb00e63 — CI-only follow-up, 2026-10-19

During final publication main advanced from 28917758 to dfb00e63. Only the iOS
TestFlight signing-profile validation and its test file changed. This commit was
also inherited #43 -> #95 -> #99 -> #101 -> #102 -> #104; local main matches it.
Both signing-profile unit tests pass. Application/KMP source is unchanged from
the Android/Native verification recorded below; no signing/upload/release or
device operation was executed. Current module/feature inventory is unchanged.

## Main 28917758 — parent reconciliation, 2026-10-19

Synchronization starts at PR #43, not the Coffee tip. Main photo delivery,
author blocking, protected painter/cache invalidation, public entity app links
and web-only password recovery are retained. ResetPassword remains a restored
route compatibility contract, not a mobile editor to recreate. Incoming shared
and iOS files are upstream compatibility changes, not a new iOS migration.

Conflict resolutions keep the utilities-owned Modules catalog, migrated Android
renderers and app-owned DI. No new feature screen is implemented in composeApp.
The existing shared Shop ViewModel remains transitional: reconciliation combines
upstream author visibility/request invalidation with existing favorite membership
and cancellation guards. API/photo clients share one refresh rotation while
retaining atomic expected-session persistence; a rejected write never returns
another account's rotated credentials.

Migrated Shop DTOs now retain author IDs and feed/detail/drink-thumb variants.
Absent and explicitly empty manifests remain distinct, album order is aligned,
and gallery effects retain the server-issued originals separately. Android
composition injects the protected application image loader, filters blocked
public authors and revokes feature stores/callbacks/open gallery composition on
visibility revision changes. Domain/UI do not depend on the blocking implementation.
Direct check-in blocking-menu parity, Home/history photo adapters and live protected
photo acceptance still need auditing; this merge is not their completion claim.

Parent verification on 495d4189 plus the two test corrections: **707 selected
unit results** (77 network, 48 legacy data, 11 author blocking, 41 Shop data,
91 Shop impl, 439 app Direct), zero failures/errors/skips; valid up-to-date
results included. Direct Debug APK/packaged DEX checks and Shop instrumentation
APK assembly pass. No device test, installation, account write or iOS acceptance
is claimed. Parent-first propagation and whole-tip verification follow separately.

Retry evidence: the initial new gallery test needed event-envelope access and
explicit sealed-base assertion typing. The host observation test needed scheduler
synchronization after combining session/visibility flows; its state assertions
were retained. Upstream Apple cancellation test now asserts a thrown cancellation,
not a failed Result, matching the existing cancellation-preserving request policy.
The entire selection was rerun successfully; no product assertion was weakened.

## Full-stack verification and inventory — 2026-10-19

Runtime/test source is Coffee tip **32cc4d57** (audit/plan edits after it are
documentation only). Local main equals origin/main 28917758, which is an ancestor
of #43 and each child. The chain remains #43 -> #95 -> #99 -> #101 -> #102 -> #104;
no rebase, force push, PR merge into main/develop or application switch is part
of this synchronization. Coffee UI remains prepared/inactive.

All Android library unit suites and both app delivery suites pass: **1704 results**,
zero failures/errors/skips (535 app Direct + 535 app Play; 634 library cases).
Direct/Play Debug APKs and packaged DEX checks, Direct Sandbox APK/isolation/DEX,
both app instrumentation APKs, Coffee/Shop impl lint and Direct app lint pass.
Full composeApp Kotlin compiles for iOS Simulator Arm64 (42s), and four build-logic
DEX-check tests pass. Native compilation is compatibility only, not Swift/UI or
iOS device acceptance. Android Studio reports zero errors in the inspected refresh coordinator, Shop
photo mapper and detail ViewModel; this is scoped inspection, not every preview.

The initial broad build ran out of Java heap in D8 while packing Debug/Sandbox;
all unit tests had passed. Repeated packaging/lint sequentially with max-workers=6
and a temporary 5 GiB heap succeeds (2m19s), without repository memory-setting
changes or suppressed checks. Preserve the failed first run (6m1s) as environment
evidence, not a passing build. No physical-device/UI test, deployment, backend
write, live protected-photo acceptance or iOS screen migration is claimed.

The initial source audit found Home/history photo representation gaps, addressed
by the later owner slices recorded above. Shop public
reads now honor blocking, but its migrated action-menu parity remains open.
Sandbox's image client is MockEngine-backed, while the new blocking scenarios
still need explicit fake fixtures. These are tracked ahead of Coffee activation,
not hidden behind compilation success. Main password reset is web-only; do not
recreate its retired mobile form. Current-main routes total 41 contracts, not
41 independent screens. See ANDROID_REFACTORING_PLAN.md for the full inventory.

## Historical main 0137ee7 — 2026-10-16

Earlier in the Coffee UI slice, main 18142f80 was observed but not merged.
That pending-merge note is superseded by the 28917758 reconciliation above.
The older unit/device evidence below retains its stated source baseline; it is
not fresh production photo acceptance. Main's later gallery uses detail variants
with aligned explicit-original fallback, rather than the earlier manual-original
button. The later Home/history slices above reconcile that contract; live
protected-photo acceptance remains outstanding.

## Latest profile update (#97–98)

Main advanced again from f2b184b to 0137ee7. The new personal/public profile,
profile activity calendar and favorites previews are retained from main, not
rewritten in feature modules. The personal ProfileScreen and public-profile
implementation match main; the existing logout cleanup error handling remains
in ProfileViewModel. Root session arguments and public-profile user dependencies
are carried through without replacing migrated screen entries.

Main removed mobile change-filter jobs. Android CI now keeps its unconditional
single build job, with migration-required Java 21 and extra runtime/test-APK
checks; no stale filter-dependent gate or duplicate job remains. Main's shared
sheet, photo-memory and Apple-login updates are incoming changes, not new iOS
refactoring in this stack. Verification below describes the earlier baseline;
fresh 0137ee7 validation is recorded separately below.

### Fresh parent verification at 0137ee7

Direct/Play debug APKs, both packaged DEX guards, both app unit-test variants,
Direct lint and the app instrumentation APK pass. Added regression cases preserve
the new guest/authenticated profile-tab selection and stable Settings identity.
ProfileScreen and the public-profile implementation have no diff against main.
No physical phone, production account or backend write is part of these checks.

This audit supersedes the 4976e53 inventory where incoming product behavior changed.
Keep parent #43 → develop and history #95 → #43; do not create a second history PR.
Main added 240 files/changes through #96. That synchronization preserves its public
profiles, analytics consent/privacy, submission feedback and iOS implementation.
No root Navigation migration, dependency upgrade or extra feature scaffolding is
part of this synchronization. Shared Native compilation is not an iOS device test.

## Parent integration

| Incoming contract | Migration reconciliation |
|---|---|
| Server favorites for shops, roasters and drinks | Android composition now uses the same server repository as existing consumers. Own feature ports expose shop/drink snapshots; app adapters hide the legacy/server representations. The existing local storage implementation is not the production writer. No local rows are automatically uploaded or erased. Logout clears memory only, not server favorites. |
| Server membership and failed requests | Membership observation reads public shop slugs without hydrating every card. Errors remain Result failures; addressless rows stay visible without invented navigation. Account replacement clears old rows and rejects late detail/write results. Roaster errors are no longer dropped at the bridge. |
| Saved drinks | The migrated screen renders a separate section with remove/pending/error behavior. Drinks with no contracted slug cannot be removed; no drink-detail route is invented. UI owns no HTTP or legacy model. |
| Offline vessel extraction | The migrated Android processor now adapts main's bundled EfficientDet/MagicTouch extractor, replacing its separate Play-services ML Kit pipeline. Original bytes, candidate order and cancellation survive mapping. Main's models/assets/licenses and original/iOS provider remain intact. |
| Preserve visit time | Legacy draft visitMillis is an instant, not UTC picker midnight. The new bridge round-trips that instant; changing the calendar date preserves local time, including an unchanged repeated DST hour, and caps future choices like main. |
| Analytics / profiles / navigation | Main's root analytics installation, consent, public profile routes and common platform definitions are retained. Existing feed/legacy-detail favorite interactions delegate to the tracked action boundary while retaining mutation guards. Analytics coverage of every migrated feature callback remains a separate audit, not implied by a clean merge. |
| Build/runtime safety | MediaPipe's older transitive error-prone annotations conflicted with Espresso under Android consistent resolution. Both resolve 2.30.0 through the catalog. Debug DEX guards also require the server adapters and migrated photo processor. No secrets or actual account fixtures belong in Git. |

The removed LocalRoasterFavorites implementation and its tests are not restored:
main removed that writer. Their former code remains recoverable in Git. Isolated
Room-foundation tests may remain, but they must not be mistaken for active Android
server wiring. A dedicated instrumentation check resolves the actual production
bindings; compilation of the isolated graph alone cannot prove correct selection.

## History-child follow-up

### Public check-in author correction

Shop data now retains server-issued authorAvatarUrl and a separate nullable
authorSlug. Public detail-carousel and full-list headers show the avatar with an
initial fallback. Their typed MVI effects pass slug/nickname/avatar to the existing
application UserProfile route. Avatar drawing is supplied by application factories
through a private feature UI local, using the same configured CpImage loader as
main (including authenticated URLs/cache); the header creates no parallel client.
Neither user IDs nor guessed paths become profile
addresses. Guest-hidden carousel/list rows cannot issue author effects, and
session-generation envelopes plus application account leases retain revocation.
Domain/data remain independent of navigation, Koin and profile implementation.
The personal-history header has no author navigation in main; its child must
retain that behavior rather than inventing a new link.

New unit regressions cover transport URL/address mapping and visible/missing/
hidden authors; a paired-theme UI contract checks the author action separately
from photos. Phone validation is deferred until the current code checkpoint is
ready, as requested; compilation of that UI test is not an executed device test.

- Inherit this checked parent normally, preserving all three migrated history routes.
- Audit main's month-wide calendar photo previews (not just selected-day/page photos)
  and transparent drink-photo display. Preserve Home civil dates versus Shop instants.
- Pass main's Home photo-preparation factory/actions through the existing app-owned
  editor bridge; do not fall back to an unavailable processor in the new route.
- Recheck author-avatar/public-profile contracts in migrated Shop/history cards;
  never construct a public user path from a GUID.
- Rebuild and rerun affected unit/UI/DEX checks after the child merge. Previous
  ef41fd8/Pixel 7 smoke is dated evidence, not verification of this new baseline.

Child #95 now inherits 0137ee7 through the parent. Month-wide photos and Home
photo-preparation callbacks are implemented and covered by regression tests.
Direct/Play APKs/DEX, app/history lint, instrumentation APK assembly and 859
selected unit cases pass; shared Kotlin compiles for iOS Simulator Arm64.
The subsequent code checkpoint restores public Shop author callbacks and personal
history avatars, using the configured application image loaders. History creation
now hands off the existing single analytics flow and preserves consent, exposure
and overlay policy via app-owned integration. Broader parent-feature analytics
and physical-device acceptance remain open; see the current child plan.
The combined author/creation checkpoint passes 995 selected unit executions,
Direct/Play APK/DEX checks, app/Shop/history lint and instrumentation APK assembly.
The subsequent Pixel 7 run passes 15 self-targeted history and 33 self-targeted
Shop UI cases after a semantic-scroll harness correction and a full-suite retry.
Read-only app navigation on the user's installed 1.0.637 reaches an empty history
and opens/cancels Home/Shop creation without submission. Its exact source revision
is not independently established; populated and write acceptance remain open.
See the child's COMBINED_HISTORY_PLAN.md for current status and UI-check evidence.

Entry-point audit against re-fetched main 0137ee7: own-profile `Чекины → Все`
still opens the full history destination; `Активность` separately opens a calendar
modal. Our new Android history route does not replace that modal, which still
uses PersonalProfileActivityCalendar with the legacy Shop/Home ViewModels. Treat
profile activity as an explicit remaining integration/parity check, not proof
that the full-screen history was retired or that profile migration is complete.

Physical-device populated history/photos/date-edge, safe authenticated writes,
account journeys and accessibility remain release checks. The stack remains draft
until those gates are explicitly checked; a merge alone is not product parity.

## Parent verification

Both Android APKs and expanded DEX guards, Direct lint and instrumentation APK
assembly pass. **851 selected unit cases** pass with zero failures/errors/skips:
both app variants plus favorites domain/impl, Shop impl and legacy network/data
contract suites (valid cached tasks may be reused). Complete composeApp Kotlin
compiles for iOS Simulator Arm64; Swift/native-device behavior is not certified.

All **14 isolated favorites UI tests** pass, including drink-only content and
addressless shop navigation. The app instrumentation run reports **13 cases**:
real server DI wiring, three processor-adapter cases and nine main extractor
cases. One private cup-in-hand fixture is intentionally skipped without an
externally supplied image; the other 12 pass with the emulator offline. No private
image/account is used. The first adapter run exposed a non-void expression-body
JUnit signature; explicit Unit fixes the harness, not production cancellation.
Existing fake-data test hosts use no production DI/network for their UI; app
instrumentation still performs normal Application initialization. Phones unchanged.


## Coffee activation checkpoint — 2026-10-19

Not a main synchronization; recorded here because it changes what the stack ships on Android.
Runtime commits on `feature/coffee-catalog-refactoring`: `9a96d756` (core bean loader restored; Shop
check-in votes no longer recompose neighbouring cards), `4129a80c` (full-app Sandbox author-blocking and
protected-photo fixtures), `d72a81c5` (Coffee analytics ports and Android adapters with legacy event
names), `96b4162d` (Android activation via `CoffeeScreenRenderer`, `coffeeModule`, migrated `CoffeePicker`
in Home/history), `6493c444` (walkthrough record).

- Legacy composeApp catalogue screens/ViewModels/repository stay for iOS and as the renderer default; no
  root navigation, route or iOS screen change.
- Verification: unit/composition/adapter suites (1088 app + Coffee results, 0 failures), Direct/Play/
  Sandbox builds and isolation, instrumentation and iOS Simulator compilation. Pixel 7 Sandbox smoke:
  Coffee list/search/filters/detail/gallery/roaster/both themes; share is blocked by Sandbox's external-app
  guard. No crash in the run.
- Not verified: populated/slow/error fixtures for Coffee, large font, pagination, restored-route and Home
  picker return on device, block UI walkthrough in Sandbox,
  live API/media/share targets, iOS.
- Behaviour note: other check-in cards' like buttons stay tappable while one vote is pending; view models
  already ignore concurrent votes, so no second request is sent.

Device UI run — 2026-10-19, runtime `96b4162d` (docs-only after): Pixel 7 (serial 2C121FDH2003NC) / API 37, test APKs only (`com.coffeepeek.feature.shop.impl.test`, `com.coffeepeek.core.designsystem.test`); no app package installed or updated.
Shop impl **41/41** (1m32s) and core design-system **84/84** (2m59s) self-targeted UI cases pass, zero
failures/retries/skips. Shop's 41 include the new `votingOneVisitDoesNotRecomposeItsNeighbour`
(counts a neighbouring card's recompositions while another vote is pending). Negative check: with the
pre-fix `ShopPublicCheckInsSection` (`9a96d756^`) the same test fails (`expected:<1> but was:<2>`);
with the fix it passes, so it guards the regression. The restored bean `CoffeePeekLoader` is covered by
core's `PresentationContractTest` (semantics only, not its look; the look was checked on the Sandbox build).
