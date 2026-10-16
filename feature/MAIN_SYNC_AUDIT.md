# Migration stack versus main 0137ee7 — 2026-10-16

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
Author-avatar/public-profile callback and migrated analytics audits remain open.
See the child's COMBINED_HISTORY_PLAN.md for current status and UI-check evidence.

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
