# Android feature refactoring — active execution plan

Source audit: updated 2026-10-14 after merging main at e5722f9. Continue the existing migration branch and consolidated
PR #43 targeting develop. This plan supersedes the earlier checkpoint ordering;
closed stack PRs are historical, not separate current workspaces.

## Scope and counting

The audited layout before history preparation had 23 Gradle modules: composeApp,
four legacy modules, five core modules and twelve feature modules (four each
for favorites, shop-report and shop), plus the existing main module
features/home-checkin. Keep that transitional plural-directory module registered
through module.feature.homeCheckIn; do not create a duplicate or rename it in
the Shop slice. Build-logic is a separate included build.
History preparation adds populated api/domain/data/impl modules (27 registered
modules in total). Shared MVI stays in commonMain; Android screen/entry code uses
androidMain because the current design-system has only an Android target.
There is no new application module or generic DI module to create.

Excluding five graph containers, the shared navigator declares 30 destinations:
25 root destinations including Main and the new HomeCheckIns, plus five tabs.
Excluding Main and the dormant ReportReview route leaves 28 content destinations. This is not a count
of independent business features: map and roaster-list content can be embedded
without a separate route; sheets, form steps, tabs and success states do not
each require four Gradle modules.

- ShopReviews currently renders public check-ins, not the prepared review editor.
- CreateCheckIn is the shop picker; the actual creation UI is a bottom sheet.
- Visited Places calendar/list are modes of the same destination.
- My Contributions has multiple content modes, not three mandatory features.
- Registration steps and Add Shop success are internal states.
- ReportReview is not a reason to activate an obsolete review creation flow.

Recount navigation before choosing each later owner. The figures describe this
source snapshot, not guaranteed runtime coverage or future product scope.

## Current integration status

| Area | Android active path | Prepared, not yet a completed migration |
|---|---|---|
| Favorites | New feature entry and app-owned storage bridge | Remaining visual/live QA; iOS stays legacy |
| Shop issue report | New feature entry | Completion QA alongside Shop flows |
| Shop menu gallery | New gallery entry, now reachable from the new detail adapter | Visual/navigation QA |
| Shop check-in creation | New Android form and app-owned draft/session/photo adapters | Authenticated submission, camera/picker and recreation QA |
| Shop detail | New Android renderer/feature entry built and smoke-tested | Full behavior/live QA pending |
| Public shop check-in list | New Android entry built; isolated presentation tests pass | Live guest/owner/navigation QA pending |
| Personal check-in history | Incoming main's combined ProfileCheckInsScreen on VisitedPlaces/HomeCheckIns; home editing retained | New shop-history entry/account/photo/DI prepared, not active; audit combined shop/home ownership and parity before integration |
| Home check-ins | Existing main module and HomeCheckIns route retained | Audit ownership with personal history before its later migration |
| Core | Infrastructure, presentation and design-system available; selected consumers use them | Legacy HTTP/Room are not globally replaced |

Prepared source, authored tests and authored previews are different from verified
behavior. Earlier successful builds do not certify subsequent unbuilt UI edits.
No new iOS screen, binding, writer or native navigation is in this execution plan.

## Ordered work before leaving Shop

### 1. Finish detail presentation parity

Compare against the current connected detail screen, preserving its meaning:

- Schedule: closing time, next opening including split intervals/week rollover,
  day off, expansion and Monday-first display. Inject the application's local
  weekday and minute; never invent midnight when the clock is unavailable.
- Features: first five rows, expansion count, existing tag/brew artwork and
  collapse when the feature list changes. Business icon selection stays in Shop.
- Menu: present items only, Espresso/Filter ordering and stable other categories,
  decimal comma/two-decimal prices, BYN artwork, photo stack, separate full-viewer
  and gallery actions, captured/updated dates. Inject the existing local date
  formatter; the preview fallback does not convert UTC to the local date.
- Coffee: separate roasters/beans/equipment groups, full-row roaster action and
  no action for an absent slug. Contacts: existing icon/order, external targets,
  copy-phone action, and empty-data omission.
- Recheck hero/header/stats, description, public check-in cards, loading/retry,
  retained-content error, guest and picker-mode states. Preserve normal/picker
  bottom actions and avoid adding a personal-history section to this screen.
- Keep every independent component in its own file, reusable section semantics
  in the feature, resources explicit and light/dark previews colocated.

2026-10-11: schedule/features/menu/coffee/contact source parity is prepared locally.
Pure formatter/clock/expansion tests and UI contract scenarios are authored.
This is not a checked completion gate: compilation, preview rendering and device
comparison are deferred. Android wiring was subsequently added on 2026-10-14;
the shared/iOS renderer still selects the legacy implementation.

### 2. Coordinate MVI refresh, writes and sessions

- Prevent a detail refresh snapshot overwriting a concurrently completed helpful
  vote; coalesce deferred refreshes and guard duplicate/conflicting actions.
- Preserve first-load versus resume behavior without duplicate initial requests.
  Refresh after relevant writes and return from forms/list/report destinations.
- Cancel old-session work and reject its late replies/events; clear account-owned
  state on session changes without clearing another account's draft or favorites.
- Verify owner detection using both current user and personal check-in IDs;
  guests must not compose private/hidden card text or photos.
- Preserve Result failures, cancellation, retained-content errors and uncertainty
  after an unconfirmed write. Do not retry writes automatically.

### 3. Finalize narrow feature entry contracts

- Expose only the detail entry and required context/callbacks; keep repositories,
  DTOs, upload transport and feature ViewModels out of the public screen API.
- Keep internal shop ID versus public slug/canonicalPath distinct for forms,
  sharing, reporting, gallery, list and edit actions.
- Supply session generation, refresh signals, distance/local clock/date formatting
  and existing form context from Android composition.
- Use the prepared full-list entry. Do not create a second personal history or
  wire unused review editors as check-in forms.

2026-10-13: these entry contracts and shared presentation coordination are
prepared locally. ShopDetailEntry delegates application actions through
ShopDetailEffect; forms/report/edit receive caller identity and public slug
separately in ShopDetailContext. The current details wire response exposes
address.slug, not a separate numeric/GUID shop ID. Sharing requires canonicalPath.
The caller supplies local clock, distance and menu/check-in date formatting.

Detail and full-list accept monotonic sessionGeneration and refreshGeneration.
Initial generations avoid duplicate first-composition loads. Post-write signals
during GET/vote coalesce into one later GET. Repeated/older signals are ignored.
Session replacement cancels pending work, resets account state, immediately
hides previous rendering state and drops buffered old-session effects. Favorite
cleanup is tied to its operation identity. Localized favorite/vote snackbar
errors stay with the feature. Regression scenarios are authored, not executed.

### 3a. Preserve incoming main's drink-photo behavior

- Reading is prepared: optional server-issued drinkPhotoUrl is separate from
  the full gallery; drink badges use Fit and a neutral icon for old/missing/
  failed images. Synthetic drink pictures are removed to match main.
- 2026-10-13: the new Android form has a narrow processor port and app-owned
  ML Kit subject-segmentation adapter. Despite the incoming documentation, the
  source tree at main 6f298e7 has no extraction provider to reuse. This detects
  foreground objects, not the cup class; inappropriate results can be skipped.
- First-photo replacement/removal cancels processing and rejects late replies.
  One object is selected automatically; multiple objects require selection.
  Retry and opt-out preserve other fields; submission waits for processing or
  selection. The same-account process-lifetime draft retains selection/opt-out.
- Feature input, header/size validation and transport now support separate PNG
  drinkPhoto, outside the five full-photo limit. PNG bytes are not recompressed;
  reservation/upload HTTP failures stop creation. These sources are unverified.
- Keep iOS providers/writers unchanged. See docs/CHECK_IN_DRINK_PHOTO.md for the
  server contract and deployment prerequisites. Regression tests and paired
  previews are authored, not run/rendered. Model availability, object quality,
  transparency, Android picker/reopen and real backend acceptance remain QA
  gates, not completed parity claims.

### 4. Wire Android only

- Assemble repositories/ViewModels through current app Koin and narrow factories,
  using existing authenticated transport plus the separate upload client.
- Delegate photo viewer/gallery, form, auth/report/edit, map/route, share/copy,
  contacts and roaster navigation to existing application destinations/adapters.
- Preserve the CreateCheckIn picker and ShopDetail(forCheckIn=true) behavior.
- Replace only the Android renderer; shared/iOS consumers keep their legacy path.
  No root Navigation 3 migration, global transport/database replacement or legacy
  deletion is bundled into this switch.

2026-10-14: source wiring is now present. Existing ShopDetail/ShopReviews routes
resolve platform renderers; default composition keeps legacy iOS screens, while
Android builds narrow entries from the existing authenticated client, the same
favorites writer and app-owned viewer/time/distance adapters. Gallery, photos,
report/auth/edit, share/copy, contacts, roasters, map/route and picker actions are
mapped to their existing application destinations/services. The check-in modal
retains its success screen and signals explicit detail refresh after submission.

A navigation-owned Android host observes session changes even behind child
routes. It revokes old leases and clears its feature ViewModel store on account
replacement, not same-account token renewal; configuration changes retain the
host/store. Account observation failure hides content and allows explicit retry.
Repository wrappers check the account before/after work and cancel on replacement;
guests cannot read the previous account's favorites or mutate votes/favorites.
Photos/forms and buffered effects are bounded by that lease. Form context and
refresh generation are saveable; a recreated in-flight form still uses the
existing uncertainty guard, never automatic write retry. These new host/session
test scenarios and the Android-only error preview are authored, not run/rendered.
Source wiring is not a verified release gate; no changes have reached main.

### 5. Verify the complete Android feature

2026-10-14 completion-gate progress: both Direct/Play debug APKs and packaged
DEX startup checks pass, with all library unit tests and both application unit
test variants (326 tests, no skips/failures). Lint passes with no error entries;
existing framework/deprecation warnings remain. Build-logic verifier's three
tests pass. Shop domain/data and shop-report data compile for iOS Simulator Arm64
as a regression check only; iOS screens/bindings/writers are unchanged.

On Pixel 7, 20 isolated Shop UI tests, 50 design-system UI tests, and six selected
application tests pass. The latter cover native PNG encoding/invalid input and
the form using fake sessions/transport; no test submits to the live backend or
changes the real account/database. APK 1.0.547 was installed as an update without
clearing data. Startup and Happy Coffee's new detail were visually inspected:
address, distance, opening hours and menu images render from the existing backend;
no fresh AndroidRuntime crash was observed. This is not authenticated-write QA.

The first run reproduced a public-property smart-cast compilation error and
an actual detail/list state-generation bug: the State receiver shadowed the
ViewModel's generation during reset. Immutable coordinate locals and an explicit
next-generation value fix these. Tests were corrected to inspect Ktor's actual
OutgoingContent and current fixture types; all reruns passed. Concurrent DEX
packaging exhausted the default local heap; the passing complete run used
`--max-workers=2 -Dorg.gradle.jvmargs="-Xmx4096M -Dfile.encoding=UTF-8"` without
changing project-wide Gradle settings.

Remaining gates: real camera/gallery and recreation, ML Kit model download and
foreground quality on actual cup photographs, server deployment/transparent PNG
access, a safe authenticated submission, and live guest/owner/session/picker
navigation parity. Automated light/dark content tests are not an IDE rendering
record for every preview. Keep these explicitly pending before release;
do not infer completion from successful builds alone. On 2026-10-14 the user
authorized publishing the validated checkpoint and proceeding to the next owner.
Next-owner preparation may proceed independently; it does not waive these Shop
release gates or authorize live test writes/removal of shared legacy consumers.

Per the user's explicit instruction, execute this gate once implementation and
Android wiring are ready, not between the current preparation steps:

- Affected domain/data/impl tests, Android app Direct/Play unit-test variants,
  debug assemblies, lint and packaged DEX runtime-definition guards.
- Render paired previews including narrow width, large font and empty/error states.
- Smoke-test startup without clearing user data; compare guest/signed-in detail,
  list, heart synchronization, owner actions, photos, drafts, form outcomes,
  session changes, back/picker and return refresh on safe QA data.
- Regression-compile changed shared domain/data for iOS if relevant, without
  implementing or switching any native iOS feature.
- Review dependencies/public API, record real QA limits, then remove only proven
  unused Android glue. Compilation alone does not complete this gate.

## Candidate owners after Shop

2026-10-14: check-in-history/domain and data now contain an independent read
foundation. Its 13 tests, Android lint and Android/iOS Simulator compilation pass.
Typed MVI/API/UI and app-owned session/time-zone/image/DI composition are prepared,
but the incoming main's combined ProfileCheckInsScreen is retained. The backend controller/query confirm
UTC instant bounds with exclusive `to`. Destination/overlay behavior and live
read-only parity are the remaining checks. Details and
module-creation rationale are in check-in-history/README.md. Home check-ins remain
their existing separate capability, not a duplicate migration target.

Earlier presentation gate: 24 MVI tests and 13 domain/data regressions passed (37 total),
along with impl Android lint/compilation and shared iOS Simulator compilation.
That validated fake-repository coordination only, not a device screen or real
account-bound transport; no application route/DI or iOS UI was switched then.

Earlier composition gate: 17 new Android calendar/lease/store/DI unit tests passed
inside all 180 app tests in each Direct/Play variant; 48 history regression tests,
both debug APK assemblies/DEX checks and app lint pass. The lazy Android DI graph
was registered, but VisitedPlaces still selected the legacy screen then. Prior 11
Pixel/emulator UI tests cover the isolated feature screen, not this new host.

The earlier destination experiment is local/unpublished and superseded by incoming
main's combined shop/home screen; the standalone entry and account-owned viewer
stay prepared. Before
the test-host fix, Direct/Play builds, DEX/lint, 183 app unit tests per variant and
composeApp iOS Simulator compilation passed. Four composition UI tests have no
passing result yet. MainActivity splash gating was found in the fake harness;
the debug-only plain Compose host correction has now compiled; device rerun remains.
Earlier command-approval usage-limit/403 errors prevented device verification.
See check-in-history/README.md for exact evidence; do not leave history for the
next business feature until the affected checks and live parity gates are settled.

Immediate UI parity correction (2026-10-14): user supplied production references
for the Shop check-in form and reported themed icon/text regressions. Main e5722f9
was merged locally at 29cff24; pending history work was restored without replacing
the incoming combined profile routes. The form now restores rating mascot cards,
required/optional rails, header/labels/field shapes, photo tile and visibility copy.
Empty notes match the current optional-note rule; the 1000-character cap remains.
Shop header/glass controls own their theme tint; public check-ins reuse the prod
headline section style and the check-in bottom action uses its surface treatment.
Direct/Play APK and DEX checks, 217 app unit tests per variant, 96 Shop domain/impl
unit tests and app/Shop lint pass. The final label/theme/form/history test sources
also compile in the publication build. Device execution and visual parity remain
unverified; earlier fixture installation was rejected by command approval with 403.
Command access recovered for the publication attempt. Publish accumulated work in
the existing PR #43 targeting develop; do not merge it or claim release readiness.
The reported top-anchored fractional modal is a known release blocker. An independent
fix is prepared from main for a separate PR into develop, as explicitly requested;
the migrated core sheet must receive the equivalent correction before release.
Inspect both themes against the production references and rerun device UI tests.

These twelve areas are an ownership map, not instructions to create 48 empty
modules. Complexity includes parity, session/platform integration and testing.

| Candidate | Complexity | Next action / dependencies |
|---|---|---|
| Favorites | Low remaining | Keep current Android bridge; finish completion QA |
| Shop report | Low remaining | Verify integrated report alongside detail flows |
| Shop | High, Android integrated | Finish remaining release QA; do not remove shared legacy consumers |
| Personal check-ins/history | High, next owner | Independent shop-visit calendar/list read foundation; home preparations remain separate |
| Community | High | Feed refresh/pagination, votes/privacy/owner guards; agree check-in ownership first |
| Discovery/search/map | Very high | Filters, location, map/picker and list consistency; separate platform map from domain |
| Coffee catalogue | Medium | Audit list/details/filter ownership before module creation |
| Roasters | Medium–high | Catalogue/detail navigation, contacts and favorites consumers |
| Profile | High | Session, statistics/history and editing; avoid importing another feature's implementation |
| Auth/registration | High | Session lifecycle, registration steps and Google platform integration |
| Settings | Medium | Preferences/session/logout, legal/external actions and platform settings |
| Contributions/add-shop/edit | Very high | Multiple contribution kinds, media, validation and moderation; split only by actual ownership |

Suggested later sequence: personal history (including auditing the incoming home
check-in capability) → profile/auth session boundaries →
catalogue/roasters → community/discovery → contributions. Reassess against current
product changes before each capability; do not treat this sequence as permission
to switch incomplete features or remove legacy iOS paths.

See shop/ANDROID_PARITY_PLAN.md for transport/form verification records and the
Shop-specific compatibility checklist. This document owns the current execution
order; those historical records do not mark new presentation work verified.
