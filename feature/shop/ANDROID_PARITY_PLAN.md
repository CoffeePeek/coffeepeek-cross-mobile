# Android shop migration — compatibility plan

## Final device follow-up — 2026-10-20

Publication reconciliation: concurrent parent `91dcde2c` (shared single-choice
fields) is retained via normal parent-first merges, with no force push. Resolve
the Home Gradle conflict by keeping design-system in commonMain without restoring
the removed preview dependency. Preserve the entire incoming select README
section verbatim alongside rating/Native documentation. Combined source
`68248716` passes **758** selected unit results (core 29, Shop 101, Coffee 46,
Roaster 19, app Direct 563), zero failures/errors/skips; Debug/Sandbox and
DEX/isolation plus design-system iOS Arm64 Kotlin compilation pass (54s).
The device build below predates this selector integration: it verifies the
unchanged Shop visual controls, not the selector's device/iOS behavior. No
new application Native framework/device acceptance is inferred from compilation.

Runtime source `d8289294` includes description follow-up `91b23c42` and shared
outlined-card reuse `fd929916`. **755** selected unit results (core 26, Shop 101,
Coffee 46, Roaster 19, app Direct 563), zero failures/errors/skips; Debug/Sandbox
assemblies, packaged DEX and Sandbox isolation pass (1m). Description IDE
inspection completes without errors. Paired previews remain beside their owners.

Manual **Pixel 7 / API 37**, 1080x2400: Sandbox 829 (`6e176444`) verified equal
floating glass plates/neutral-gold states, removed menu heading action, photo-tap
gallery and return, report-card shape/spacing, expanded/collapsed hours, and guest
author menu absence plus themed registration in light/dark. The later Sandbox
**843 / 1.0.843-sandbox** from `d8289294` was updated in place and its corrected
description/header visually inspected in both themes. Theme was restored to
light. App package, launcher, increasing version and matching signing certificate
were checked; no data clearing, normal CoffeePeek/Pixel 10a installation, live
account or backend mutation. Screenshots/video remain local, not tracked.

Final APK SHA-256: `c02d495f93b3780cb9b9fc7d8eb3162bd1d2f0347d62cade547511a3f10b9a1a`.
No automated UI suite/host was added or run. Hours end states and source animation
specs were checked; the short manual recording did not reliably capture the
transition, so frame timing/fluidity is not certified. Actual backend media/Haze,
large-font/window/accessibility acceptance and native iOS remain separate gates.

## Screenshot corrections — 2026-10-20

Description follow-up: restore main's outlined `surface` card (24dp corners,
zero elevation/tonal fill), the common section heading, 16dp inner inset, 8dp
heading gap and 6dp external vertical inset. Keep the complete backend text and
its existing body typography; main has no expand/collapse affordance here.
Parent Shop unit selection **101** remains green; Debug/DEX pass (56s). The later
shared-card checkpoint should reuse its existing outlined card primitive rather
than introduce another card builder. Manual description comparison follows its
new Sandbox build; earlier device captures used the pre-follow-up description.

Owner: parent #43, based on `daabcebc`. Preserve menu-photo taps, guest sign-in,
report routing, canonical public author identities and existing analytics.
Remove only the redundant menu-photo heading button. Registration uses theme
`onSurface` (black in light mode), not the primary yellow. Report information
matches main's 24dp corner, 16dp inset, 12dp gap and semibold title. Hours retain
their non-ripple header and 250ms chevron rotation, now with main's top-anchored
expand/shrink and 180/150ms fade. Updated paired previews remain with each owner.

Approved behavior difference from main: guests no longer see or emit block-author
actions, including a direct/stale ViewModel intent. Main previously offered the
sign-in prompt from this menu. Both detail and full check-in list share the same
eligibility guard; signed-in non-owner blocking and public profile navigation
remain supported. No author-block server or native iOS flow is changed.

Verification: Shop impl **101** and app Direct **446** unit results, zero
failures/errors/skips; Direct Debug assembly and packaged DEX check pass (1m12s).
IDE menu inspection timed out and is not a clean inspection result. No automated
UI tests, device installation or real account write. Manual light/dark Sandbox
comparison, popup placement and animation acceptance remain open. The contextual
header-heart glass treatment is owned by the later shared-like checkpoint and
will be verified after propagation; compact card/feed hearts must not resize.

## Develop reconciliation — 2026-10-19

Approved product baseline: main `dfb00e63` plus develop `fc85f8e5`.
The parent #43 incorporates develop before its descendants. Incoming menu-photo
priority, volume-field removal, profile spacing and cached-gallery-preview fixes
are retained. Native provider removal/plist metadata are incoming compatibility
changes, not a new iOS screen migration.

Drink-photo cutouts and MediaPipe are retired in both legacy and migrated Shop:
no extraction processor/DI/model assets, cutout fields, extra upload, draft sidecar,
choice/retry/skip UI or stale APK class requirement. Ordinary gallery uploads,
issued variant/original alignment, drink selection/label, ratings, draft ownership,
uncertain-delivery guards and author-block menu remain supported. Historical
cutout preparation/acceptance notes below describe superseded product behavior.

Verification plan: Shop domain/data/impl and app unit tests, Debug assembly with
packaged DEX checks, self-targeted Shop APK and app instrumentation compilation;
then propagate through history/Home/Sandbox/rating/Coffee and verify the tip.
Regression cases preserve original bytes and prove the retired field is absent
from a create request. Device tests use only a generic fake test application;
live backend/camera/authentication and iOS UI acceptance remain separate gates.

Parent verification completed: **725 selected unit results**, all passed
(Shop domain 7/data 41/impl 96, app Direct 438, legacy data 47/network 77/Home 19).
Direct Debug APK/packaged DEX guard, self-targeted Shop APK and app Direct
instrumentation compilation pass (2m19s; additional contracts 11s). First build
failed on a stale nullable preview argument after removal of the cutout parameter;
corrected before the successful rerun. Pixel 7/API 37 full fake Shop suite:
**39/39**, 68.542s, no UI retries/failures. Test APK SHA-256:
`69b04263683b96f5b06236dd8922188792a781f74591492becb6e3d4cb815542`.
Manifest/self target/version 0 and signatures checked before an in-place test-only
update; ordinary CoffeePeek/Sandbox were not installed or cleared. APK contains
no retired TFLite model assets. Studio reports no errors in form/DI files; the
first ViewModel inspection timed out and is not counted as a completed IDE check.
Light/dark behavior is automated; no new manual screenshot-parity claim is made.
Logs remain local as `coffeepeek-develop-root-*2026-10-19.log`.

## Author-block action parity — 2026-10-19

Owner: Shop parent #43, based on `58edfdbd`; remote main remains `dfb00e63`.
Preserve main's block-first check-in menu and existing application-owned block
confirmation/login flow. Shop emits only a narrow public-author identity through
its API; no Koin, HTTP or author-block implementation is added to feature UI.
Application composition adapts that identity while its account/visibility lease
is current. Public author IDs are not authentication user IDs. Known own rows,
canonical own slugs, unresolved signed-in profiles, unavailable identities,
hidden guest rows and unrendered preview rows must not emit a block request.
Only visible guest rows may request the existing sign-in prompt. Existing report,
vote, album and blocked-author filtering/cancellation remain intact.

Verification: **552 selected unit results** (99 Shop impl, 11 author-blocking,
442 app Direct), zero failures/errors/skips. Direct Debug APK and packaged DEX
guard, self-targeted Shop APK and app Direct instrumentation Kotlin compilation
pass. First build: 1m43s; completed fixture build: 7s; final selector build: 2s.
Studio error inspections report none in the block icon, Shop detail ViewModel,
eligibility helper, menu and app bridge. Shared owner previews remain beside the
components; popup placement is exercised on-device, not certified by IDE rendering.

Physical **Pixel 7 / API 37**, 1080x2400/density 420: new menu/API cases **4/4**
pass (11.884s); the final full Shop suite passes **39/39** (67.221s). Tests use
only generic self-targeted Application, fake repositories and recording callbacks,
not the live root DI. The public entry cases assert exact issued identity and
localized missing-name fallback without another read or a vote/favorite write.
Package/target/version 0 and matching signatures were checked before test-only
in-place updates, without uninstall/data clear. Final test APK SHA-256:
`92708dc930ef9eac514c2735b6fb52405a1eb4789bcc0f215b31844ee37e40ff`.

Retained retry history: the first full run passed 38/39. An old parent-only
author-navigation test scrolled to index 3 in a three-item fixture (valid indices
0..2); its already-existing child fix selects the author semantically. Porting
the selector initially missed two imports (3s compile failure), then compiled
successfully. Targeted repair **1/1** passes (2.409s), followed by full **39/39**.
No product assertion or scenario was dropped. Logs stay local under
`coffeepeek-shop-block-parent-*2026-10-19.log`.

Owner runtime `6dc3f115` is propagated parent-first through #95/#99/#101/#102 to
tip runtime `46b1943a`. Final-tip verification: **650 selected unit results**
(99 Shop impl, 11 author-blocking, 540 app Direct), zero failures/errors/skips;
Direct Debug/Sandbox APKs, both packaged DEX guards, Sandbox isolation, Shop
self-targeted APK and app Direct instrumentation Kotlin compilation pass (2m03s).
The initial tip build launch was not executed because tool approval was
unavailable; the same normal approval path later succeeded without a bypass.

At that tip, the complete Pixel 7 fake Shop UI suite passes **40/40** (67.277s),
no failures/retries. The extra case is an existing descendant shared-heart
regression, not a new production capability. Test APK SHA-256:
`3f52bd107a297533179ae6094bff605d4380b5f27be78f015b0a07272573d583`.
Local final logs: `coffeepeek-shop-block-stack-final-build-2026-10-19.log` and
`coffeepeek-shop-block-stack-full-ui-2026-10-19.log`. Later tip edits are docs only.
Only test APKs were installed; ordinary CoffeePeek/Sandbox and account data are
untouched by these checks. No real block/backend write, native iOS screen change,
new Gradle module or manual pixel acceptance. Full-app Sandbox blocking transport
fixtures and live protected-media/blocking acceptance remain separate gates.

## Scoped interaction corrections — 2026-10-19

Parent #43 (`d8dff6ed` plus this patch) restores the existing main hours-header
behavior: expand/collapse remains an accessible button, without a ripple on the
whole header. Ordinary actionable buttons and selection menus retain feedback.
The static saved-drink summary now lives in the check-in card's bottom-trailing
footer alongside the helpful count; it no longer occupies a full-width row before
the note or displays a chevron suggesting nonexistent drink navigation. It uses
the shared, unplated `CoffeePeekDrinkLabel`: no background/border or asymmetric
internal padding. Monetary BYN amounts use the shared square `CoffeePeekBynIcon`
at main's 16dp menu size, not the taller price-level indicator. Cutout
image injection, localized drink-name fallback, note, album and vote behavior
remain unchanged. Paired previews stay beside the component owners.

Checks: Direct Debug APK and packaged runtime-class guard, **91 Shop impl unit
results**, **439 app Direct unit results**, Shop lint, and both self-targeted test
APKs pass. On Pixel 7 / API 37, **10 Shop detail UI tests** pass, including static
drink/footer geometry in both themes and hours expansion/collapse. The six shared
modal inset/placement tests and their harness retry are documented in
`core/design-system/INSETS.md`. Light/dark brand-font fixtures were also visually
inspected on the device; screenshots remain local. These are fake fixtures, not live account/backend,
OS camera/picker or iOS acceptance. Screenshots remain local and untracked.

## Current visual scope — 2026-10-15

The latest controls checkpoint additionally groups photo-source actions, adds
form top clearance and restores selected drink checks plus the None divider.
It preserves saved/custom draft choices, callback capacities and modal anchors.
Shop Report's separate README records its grouped single-choice reason list.
Current parent checks: Direct/Play APK/DEX, app/Shop/Shop Report lint, 518 selected
unit executions and 17 isolated UI tests pass. Light/dark fake photo-source,
drink-menu and report-list fixtures were visually inspected; no phone/account,
OS camera/picker, live submission or iOS integration is certified by these checks.

The screenshot-reported detail regressions are corrected in parent #43 and
inherited by history child #95. Root main/develop are not changed by publishing
this work. See README.md's current visual correction for exact geometry and the
user-approved exceptions: yellow information-card icons and larger 14/12sp badge
text are retained, not replaced with legacy white icons or 11sp labels.
This does not close live authenticated, picker/camera or individual IDE gates.
The dated source/preparation sections below are historical evidence, not current
claims that the Android renderer remains unbuilt.

Audit baseline: 2026-10-08, consolidated PR #43, branch
`feature/core-design-system-controls`, target `develop`.
Continue this single PR as requested; do not recreate the former PR stack.

## Source of truth and boundaries

Preserve the behavior currently connected in `composeApp/ui/Navigator.kt`,
`ui/screen/shop`, `modules/network` and `modules/data`, not assumptions from
earlier migration fixtures. The shop-details and full-list screens now display
check-ins even though the legacy list route is still named `ShopReviews`.
Prepared review editors remain unused until an explicit product decision.

New business code belongs to `feature/shop/{domain,data,impl}`; root Koin,
platform callbacks and temporary adapters belong to Android app composition.
Use pure domain contracts, constructor DI, Kotlin Result and cancellation
propagation. Do not switch shared/iOS routes, delete their legacy consumers,
replace root navigation or introduce more Gradle modules for this work.

## 1. Correct active transport contracts

- [x] Shop issue report sends `shop`, not `shopId`; test actual serialized body
  for every existing category. This fixes the already-active Android report.
- [x] Gallery and details decode the flat shop object inside `data`, not
  `data.shopDto`. Reject unsuccessful responses or an absent shop slug.
- [x] Preserve shop and roaster address slugs/canonical paths, `beans`,
  `checkInCount`, and menu-photo ordering/variant selection.
- [x] Preserve public `checkIns` independently of personal `userCheckIns`.
- [x] Map current check-in author, text, timestamps, drinks, ratings,
  visibility/moderation/revision, and helpful counts. Resolve issued photo URLs,
  preserve ordering, and never invent a URL from a check-in storage key.

Tests must use the current wire shape and verify fields, not only success.
Device/server verification remains separate from MockEngine coverage.

## 2. Align check-in creation

- [x] Use `/api/v1/check-ins` with `coffeeShopSlug`, `text`, `rating`,
  `visibility`, `visitedAt`, drink selection and uploaded attachments.
- [x] Remove obsolete public-title/2000-character rules. Both visibility modes
  require trimmed text of 1–1000 characters, ratings of 1–5, a valid nonfuture
  visit timestamp, optional catalog/custom drink, and no more than five photos.
- [x] Keep `/api/Photos/check-in` and attachment `size`; no signed review tag.
- [x] Do not confirm creation from an empty or malformed reply, rejected HTTP
  status/envelope, or missing visit ID. Expose unconfirmed creation separately
  from a known rejection; preserve server rejection and cancellation.
- [x] Add creation MVI and UI handling for success, known rejection and
  unconfirmed delivery (check history before repeating the write).

## 3. Finish data and MVI interaction parity

- [x] Enrich menu labels/categories/order from `/api/menu/drinks`, preserving
  usable details if the optional catalog request fails.
- [x] Observe favorites continuously, not only on initial detail read. Keep
  the single Android favorites writer and stored representation compatible.
- [x] Add check-in helpful PUT/DELETE contracts and owner/duplicate guards.
- [x] Prepare full-list/report events and public check-in cards with guest
  preview/sign-in/register and owner fallback via personal visit IDs.
- [x] Connect full-list/report events to the app-owned destinations (2026-10-14 source wiring).
- [ ] Verify guest/owner behavior and the new renderer on device before release.
- [x] Handle return/session changes in detail MVI and its runtime adapter;
  detail no longer requests legacy review eligibility.
- [x] Connect the app's session generation and refresh after completed writes.
  Host/session scenarios are authored; execution and live QA remain below.

## 4. Create the check-in form without changing behavior

- [x] Typed MviViewModel, State/Action/Event, one action entry point and
  conflicting-operation guards.
- [x] Stateless form content and separate components; paired light/dark
  previews beside each component; text/accessibility in resources.
  Compiled and covered by MVI tests; device/IDE rendering is a separate gate.
- [x] Three ratings (default 4), required note, visit date, optional drink or
  custom drink, five-photo limit and Public/Private toggle (default Private).
- [x] Prepare drinks loading/error/retry and the date field/Android formatter
  with the same meaning as the current date picker.
- [x] Wire application-owned camera/gallery conversion and the date formatter
  through the active Android form entry.
- [ ] Verify actual picker/camera behavior on device, including permission,
  cancellation, results and recreation; adapter/host tests do not prove this.
- [x] Preserve existing draft lifetime: one shop draft for the app process,
  retain on dismiss/failure, replace on another shop, clear on success/logout.
  Do not silently add durable persistence or retain drafts across accounts.
  One Android adapter is registered around the existing store. Bound leases
  preserve internal ID versus public slug and reject stale session/account writes;
  active Android wiring and adapter/host tests are in place. Safe authenticated
  end-to-end scenarios remain part of the completion gate.
- [x] Prepare the draft adapter contract and retain an uncertainty guard across
  form recreation. Require explicit history-check acknowledgement before retry.
- [x] Prepare dismissal/repeated-submit guards and retain input on errors.
  MVI guards and isolated modal/Android host UI tests pass; real authenticated
  submission and picker recreation remain pending.
- [x] Prepare submission/result UI with distinct Public/Private confirmation
  and caller-owned feed/history events; success does not auto-dismiss.
- [x] Connect those callbacks to the existing Community/Visited Places routes.
- [ ] Verify real navigation/return refresh after a safe authenticated write.

## 5. Finish detail UI and app composition

The first UI slice below was prepared locally on 2026-10-10. Android platform
wiring was added locally on 2026-10-14; it remains unbuilt/unverified. It restores title/type/address below the
photo hero, rating/count/full-list intent, injected distance, the existing BYN
price tiers and explanation sheet, section order, report intent, separate menu
thumbnail/viewer and gallery actions, and the normal/picker bottom bars. Personal
check-ins remain ownership input, not an extra section absent from the old screen.

- [ ] Match current section order/actions and bottom-bar check-in behavior.
- [ ] Restore distance without extra permission prompts, shop type, price
  explanation sheet, check-in count/full list, report action and photo viewer.
- [x] Assemble one set of repositories/ViewModels through existing app Koin;
  pass the existing authenticated client and separate upload client.
- [x] Adapt sharing using canonicalPath, suggest-change/report/auth, map,
  external route/contact/copy, gallery and roaster navigation in the app.
- [x] Preserve `CreateCheckIn` shop picker and `ShopDetail(forCheckIn=true)` in source wiring.
  Leave feed/map migration outside this slice; use existing destinations.
- [x] Add public detail entry and source wiring in the isolated migration branch. Switch
  Android through a platform renderer; leave iOS on its current implementation.

## 6. Completion gate

- [x] Run affected domain/data/impl tests plus both Android app builds,
  application unit-test variants and lint after integration changes.
- [x] Compile changed KMP domain/data for iOS Simulator as a regression guard,
  without implementing or switching an iOS screen.
- [ ] Inspect previews and Android guest/signed-in flows on safe QA data:
  loading/retry, heart synchronization, photos, reports, submission outcomes,
  drafts, back/picker mode and return refresh.
- [ ] Review dependencies, DTO containment, Result/cancellation and public API.
- [ ] Record remaining QA limits; remove only proven-unused Android glue, never
  shared paths still consumed by iOS.

Do not mark UI parity or live-server correctness complete from compilation or
unit tests alone. Android source wiring is local in the isolated migration
branch; do not merge/release the switch before this gate passes.

## Current execution order — 2026-10-14

Per the user's explicit request, defer builds, tests, device checks and preview
rendering until the Android feature implementation is ready. Colocated light/dark
previews are still authored with each component; their presence is not rendering
verification. The existing verification records below apply only to earlier
slices, not to these new detail UI edits.

1. Finish detail component parity: schedule → features → menu → coffee/contact
   → complete loading/guest/picker states. Schedule, five-row features with
   icons, menu stack/BYN/date presentation and separated coffee/contact groups
   are now prepared locally. They remain unbuilt/unrendered; Android wiring is present.
2. Coordinate detail refresh/vote/session work and post-write refresh signals.
   Prepared: exclusive load/vote operations, coalesced post-write refresh,
   monotonic generations, account-state reset and session-tagged effects.
   The full public-list entry/MVI/content is now wired through the Android renderer;
   align its behavior with detail instead of preparing another duplicate list.
3. ShopDetailEntry/ShopDetailEffect/ShopDetailContext and full-list refresh input
   are wired. Android supplies session/refresh generations, local clock/date/
   distance and application-owned actions; legacy iOS remains the default renderer.
4. Drink-photo creation source is prepared: Android processor port/adapter,
   selection/retry/opt-out, first-photo lifetime, separate PNG upload/payload
   and draft guards. Reading drinkPhotoUrl and Fit rendering are prepared too.
   The incoming main tree has no extraction implementation despite its document;
   the new Android implementation and all related regression scenarios still
   require compilation, model/device QA and backend deployment verification.
5. Android detail/list renderer source wiring is present, preserving current
   picker/forms and legacy iOS. Account leases and a navigation-owned feature
   store guard session replacement even while a child route covers the screen.
6. Run the complete verification gate above; fix regressions and record real QA
   limits before treating Shop as finished or starting another business owner.

The current cross-project audit, candidate owners and detailed ordered work are
in [../ANDROID_REFACTORING_PLAN.md](../ANDROID_REFACTORING_PLAN.md). Existing
verification records below apply only to the earlier slices identified there.

The preparation was initially unbuilt; the 2026-10-14 verification record at the
end now supersedes that status for compilation, automated tests and selected
device rendering. Main is included locally through merge commit 6a0e49f;
validated work is being published to consolidated PR #43. The pending live QA
gates remain explicit even as independent next-owner preparation starts.

## Verification record — transport and creation foundation

2026-10-08: wire-contract regression tests first reproduced the gallery envelope
and report field mismatches. After the fixes, the complete Android CI command
passed locally: Direct/Play debug assembly, all library debug unit tests, both
application unit-test variants, and library/application lint. Changed shop
domain/data and shop-report data also compiled for iOS Simulator Arm64, without
changing native iOS screens or routes.

Coverage includes the flat details response, canonical addresses, public/personal
check-ins, issued photo URLs and ordering, report serialization, the v1 creation
payload, validation, HTTP/server rejection, unconfirmed delivery and cancellation.
This verifies the first foundation slice, not the unchecked integration gates
above. Device, authenticated live-server and preview verification remain pending;
the active legacy detail/check-in screens are still the runtime implementation.

## Verification record — interaction and form MVI preparation

The interaction slice passed both Android debug builds, all debug unit tests,
both application unit-test variants, library/application lint and shop
domain/data iOS Simulator compilation. Additional form MVI tests also passed.

Menu tests cover successful caching, parsed-field preservation, stable unknown
item order, failure fallback/retry and cancellation. Detail tests cover live
favorites, observation recovery/cleanup, vote/report ownership, duplicate votes,
return refresh, session load replacement and stale-session vote replies. Guest
cards beyond the first compose placeholders, not hidden real text/images.

Form tests cover restoration, photo limits, catalog retry, validation, guarded
writes, known rejection, uncertainty across recreation, explicit history checks,
cleanup failures and cancellation. The draft store is an app-owned adapter
contract, not a second durable store. Form UI, the Android draft/result bridge,
device verification and final route replacement remain unfinished.

The subsequent startup incident and APK-definition guard are documented in
`build-logic/ANDROID_RUNTIME_PACKAGING.md`. Build success alone did not detect
stale intermediate class-directory packaging; packaged runtime checks and a
Pixel 7 startup smoke test now supplement compilation.

2026-10-09: the full Android completion command passed again, including the
packaged DEX checks, application tests/lint and shop domain/data iOS Simulator
compilation. Build-logic DEX parser tests passed independently. A real race in
legacy feed/detail favorite membership was fixed by committing membership and
loaded content under one mutex; tests now cover both ordered and concurrent
initialization. This retains the existing shared/iOS behavior rather than
switching their feature entry points.

Local setup now uses the existing public API address `https://api.coffeepeek.by/`
instead of the retired Railway application. Public shop/city reads returned
HTTP 200 with data; generated Android configuration and device GET request URLs
were checked. No live writes or authenticated migration scenarios were exercised.
The Android detail/form integration checklist above remains open.

## Form UI and Android bridge — verified foundation, not connected

The prepared `ShopCheckInCreateScreen` observes its injected lifecycle ViewModel
and renders stateless `ShopCheckInCreateScreenContent` in the design-system sheet.
Ratings, note, date, drink and visibility each have their own component file and
paired light/dark previews. Review and check-in editors share visual rating/photo
primitives; their business models and upload purposes remain separate.

The form includes pending/success states, Public/Private messages, draft and
validation errors, catalog retry, a five-photo limit, history acknowledgement
before retrying an unconfirmed write and caller-owned feed/history events.
Success does not dismiss the confirmation automatically. Camera/gallery remain
caller callbacks, not platform code inside the feature.

`AndroidShopCheckInDraftStore` wraps the existing process-only store. Keep one
adapter instance in app composition across form recreation. Legacy clear/logout
or draft replacement invalidates its delivery guard; stale writes/cleanup cannot
overwrite a replacement draft. The Android date formatter delegates to the
existing local-date/UTC conversion. No new persistent storage or iOS screen was
introduced. Android DI now registers these adapters; live routes remain unchanged.

Adapter tests cover field/photo preservation, defaults, timezone round trips,
guard lifetime, stale saves/cleanup and cancellation. A design-system device
test covers blocked handle/back/drag dismissal and unlocking. Form MVI tests
cover the explicit success destinations. Form UI tests cover editing/catalog
retry, localized note-error semantics, uncertainty acknowledgement, pending
writes and Public/Private confirmation destinations using fake state only.

2026-10-09: form MVI (8 tests), Android draft/date adapter (6 tests) and the
complete Android build/unit/lint command passed, including the debug DEX guard
and shop domain/data iOS Simulator compilation. DEX parser unit tests pass.
Both isolated UI-test APKs compile; CI now compiles them too. The prior command
approval `403` no longer prevents execution.

The initial Pixel 10a run was asleep/locked and found no visible Compose
hierarchy. A later unlocked Pixel 7 run passed 5/6 overlay and 4/5 form tests:
it exposed an unconditional Material handle click action on a locked sheet
and an invalid test selector looking for SetText on an intentionally disabled
field. Neither failure was dismissed as a connection problem.

After correction, all 7 overlay and 5 form tests pass on the temporary
Pixel_10_Pro emulator (read-only AVD, no snapshot saved). The additional overlay
test verifies physical handle taps dismiss exactly once. Locked-sheet coverage
checks disabled/no-click semantics, physical tap, long drag, outside tap, Back,
then unlocking. Disabled-note coverage uses EditableText, asserts disabled and
the absence of SetText, and verifies explicit history acknowledgement actions.
The full Direct/Play build, DEX guard, debug unit tests and lint pass again.

The Android menu-gallery route was registered but had no reachable caller.
An Android-only app-composition button now opens it from the legacy detail
menu without replacing the existing thumbnail-to-viewer action or iOS UI.
The emulator loaded the real "1801 кофе" gallery image; opening the viewer and
returning through gallery/detail were checked. This read-only check is not
authenticated-write QA or final detail/form migration approval.

The corrected tests are installed on Pixel 7, but that phone is currently
locked; rerunning the fixes there remains pending. IDE modal/paired-preview
inspection, authenticated QA and Android photo-picker integration are still
open. CoffeePeek user data was not cleared, and no backend writes were made.

## Android form composition — 2026-10-10

`shopCheckInModule` registers the repository with existing authenticated API and
separate upload clients, one date/draft adapter, the existing Android photo-picker
pipeline and the new public `ShopCheckInCreateEntry`. There is no feature DI
Gradle module and no Koin in feature UI/domain/data. API exposes only identifiers,
shop name and caller callbacks, not ViewModels, transport or business models.

Internal shop IDs and public address slugs are distinct. Bound draft ports preserve
legacy ID keys but restore/save domain inputs with the public slug. Per-binding
leases reject writes and clears from an old screen after account/draft replacement,
including when a new screen has already opened the replacement. The single
process store and delivery guard remain intact across modal recreation.

The modal owns a fresh lifecycle ViewModel for each opening. Disposal cancels
in-flight work; a possibly accepted write remains guarded on reopen. The Android
photo port converts already-prepared images, limits delivery to remaining slots
and disables submission while images are being prepared. Real picker permission,
camera cancellation/result and recreation QA are not inferred from this coverage.

Verified: all library debug unit tests, both Android APK variants, application
unit tests and lint, plus 9 isolated form/entry UI tests on the temporary
read-only Pixel_10_Pro AVD. Android adapter tests cover ID/slug separation, stale
leases, date/draft behavior and photo conversion limits; DI resolution preserves
the draft and does not read credentials. UI tests cover processing-disabled
submission, successful callback delivery without auto-dismiss, fresh ViewModel
on reopen, input retention and lifecycle cancellation without automatic retry.
The application startup smoke check stayed alive without crashes. Packaged DEX
checks now also require the new DI/API/domain and data/impl entry factories.
No user data was cleared and no live writes were sent. No physical phone was
connected for this run. iOS screen/binding paths were not switched.

This composition checkpoint is followed by the active Android connection below.
Keep the full detail renderer disconnected until its remaining parity gates pass.

This checkpoint exceeds the usual 50-file PR guideline because it completes
previously accumulated work inside the single consolidated PR #43 explicitly
requested by the user. Do not recreate the superseded PR stack for these fixes.

## Active Android form connection — 2026-10-10

The existing detail and `forCheckIn` shop-picker mode now select the new form
through `ShopCheckInFormRenderer`. Its default remains the legacy/iOS form and
writer. Android opening skips the legacy catalog/draft preparation; the feature
entry owns the only form catalog request and write. Guest opening retains Auth;
missing public slugs show a resource-owned error without opening a broken form.

Successful creation refreshes the legacy detail via a temporary app-owned bridge,
without hiding the confirmation or clearing the feature's draft a second time.
The root still owns ordered feed/history navigation. The bridge rejects a late
refresh response if its account has changed and rethrows coroutine cancellation.

Android composition binds each modal and its requests to the active account.
Logout/account replacement cancels outstanding requests, closes the modal and
clears only that binding's draft; renewed tokens for the same account are allowed.
Opening under a different account also drops a closed modal's old process draft.
Old bindings cannot reopen, save into, or clear another account's replacement.
There is no new token store, DI module, feature-to-legacy dependency or iOS switch.

Host UI tests use fake auth and transport: the Android renderer does not execute
legacy content, loads drinks once, sends the public slug once, retains confirmation
and forwards callbacks. Account replacement cancels a suspended fake write and
does not expose the old note on reopening. Default rendering still selects legacy.
No production check-in was submitted during verification.

Verification for this slice: both Direct/Play APK assemblies and packaged DEX
checks, both application unit-test variants, all library debug unit tests and
Direct/Play lint passed. The targeted suites include 13 legacy/detail bridge,
11 Android draft-adapter and 6 session-lease tests. Three Android host UI tests
passed on the temporary read-only emulator with fake auth/transport; they are
not a substitute for authenticated backend or real picker/camera QA.

### Next ordered refactoring slices

1. Close Android form QA: real gallery/camera permission, cancellation and result,
   recreation, keyboard/Back, light/dark previews, and known/uncertain write
   outcomes on approved QA data. Do not infer these from fake-host coverage.
2. Finish detail content parity in `feature/shop/impl`: section/action order,
   distance/type/price explanation, photo viewer and bottom-bar picker mode.
   Keep each component and its paired previews together.
3. Prepare the public check-in full list currently named `ShopReviewsScreen`,
   helpful/owner/guest/report behavior and its app-owned destinations. It is not
   already migrated merely because detail events and vote transport exist.
4. Publish a minimal detail entry, assemble its repositories/session/favorites
   ports, wire platform sharing/map/contact/roaster callbacks, and switch only
   Android after the preceding parity gates and integration tests pass.
5. Audit and migrate personal visit history (`VisitedPlaces`): calendar/list,
   month boundaries, paging, account invalidation, retry and return refresh.
   Decide its feature boundary from actual consumers before creating modules.
6. Next prioritize profile/account editing and auth/session, then browsing/feed,
   coffee/roasters and community; map/contributions/edit submissions require
   their own parity audit. Root navigation migration and native iOS are separate.

The checklist's earlier unchecked device/QA items remain unchecked until the
real scenarios pass. Gallery and Android check-in form are connected; complete
detail/full-list migration and authenticated behavior are not claimed complete.

## Prepared detail presentation continuation — 2026-10-11

Local, unverified source changes only; the active Android detail route still
uses its legacy renderer. No iOS entry, writer or shared legacy route is switched.
Design-system APIs/files now use `CoffeePeek…` names and existing consumers have
been updated without old-name forwarding aliases.

Prepared detail now uses screen-owned Haze/glass controls over the photo hero,
with persistent Back, edit/favorite/share actions and branded favorite progress.
Horizontal safe-drawing and status/bottom inset ownership are explicit. Loading,
initial error/retry and background refresh feedback have separate previewable
components. A failed refresh retains the loaded details; session replacement
still clears private content before any new request and never restores it on failure.

Public check-in presentation now includes the loaded-sample rating breakdown,
author/date header, report menu, stars, saved-drink summary, expandable note,
one/two/three-tile photo collage with the complete album callback, and helpful
selection/progress semantics. Cards use the current outlined surface style.
Carousel widths adapt to their actual parent, retaining the guest next-card peek.
Hidden guest cards remain neutral placeholders with no real hidden text/photos
in the composed tree, rather than relying on platform-dependent blur. The empty
state and sign-in/register card preserve the existing visual families.

Seven existing app images were copied without transformation into feature-owned
resources (rating mascots, empty-state mascot and current placeholder drink art).
They are not exported through design-system or accessed through app Res. The
legacy originals remain for their current consumers. Colocated light/dark source
previews and regression fixtures are authored; none were built, rendered or run
in this continuation, following the user's deferred-verification instruction.

Next: prepare the full public check-in list; then publish narrow detail/list
entries and Android composition/navigation/session wiring. Composition must
supply the existing localized relative-date formatter (the disconnected content
currently has an ISO-date fallback), along with platform photo/share/map actions.
Real photo/viewer, permission, back/inset, large-type and authenticated backend QA
remain completion gates. Do not mark the earlier integration/verification boxes
complete based on these prepared components.

## Prepared public check-in list — 2026-10-11

The full-list MVI implementation and narrow `ShopCheckInListEntry` are prepared
inside the existing shop api/impl modules. It reuses the details snapshot and
existing check-in vote domain contracts rather than inventing pagination/API
behavior. Cards show complete notes, ratings, author/date, photos, helpful marks
and report actions. Guest prompt/neutral placeholders and ownership restrictions
are preserved without composing hidden content. Guest state retains only the
first card and aggregate/count information. Back remains usable during load.

Initial error/retry, same-account refresh failure, return refresh without an
extra first request, duplicate votes, refresh/vote serialization, cancellation,
session invalidation and stale results have authored regression scenarios.
The runtime suppresses old-session rendering before the asynchronous reset and
drops photo/report effects from older generations. No builds, tests, device
checks or previews have run for this slice; these are completion-gate inputs,
not proof that Android integration is ready.

Historical next step at the time of this preparation: publish the detail entry and assemble the Android detail/list composition
ports, including account-bound transports, existing localized relative dates,
photo viewer, auth, report, sharing, route/map/contact and roaster destinations.
Preserve the existing check-in form/confirmation and picker mode, then verify the
complete Android slice before switching/removing legacy paths. iOS stays legacy.

## Android composition source wiring — 2026-10-14

ShopDetail and ShopReviews now resolve platform screen renderers. Android selects
the feature entries; default common/iOS composition keeps the existing screens.
App-owned Koin creates one details repository and one check-in vote repository
from the existing authenticated client. The same favorites repository/writer is
reused. No new Gradle module, root navigation migration or legacy deletion.

Android adapters route gallery/viewer, report/auth/register/suggest, maps/route,
roasters, external contacts and canonical-path sharing to the current app.
Map focus includes the shop title, not an empty placeholder. Check-in creation
keeps its confirmation modal and signals detail refresh without another write;
picker mode retains the existing choose-another navigation. Relative check-in
dates reuse current app formatting and lifecycle-bound minute updates; menu
dates retain the local dd.MM.yyyy format and device UTC-offset convention.

A navigation-owned host retains the feature store through configuration changes,
observes session replacement behind child routes, revokes old account leases and
clears feature VMs before publishing a new generation. Token renewal for the same
account preserves the store. Session observation errors hide data and offer an
explicit retry. Guest favorite reads do not access account-owned storage. Date,
viewer identity and distance/platform services stay in app composition, not domain.

Host disposal/replacement, observation failure/retry, guest guards, token renewal,
non-cooperative late votes and Result failure scenarios are authored. The existing
DI graph test now also resolves the browsing repositories without requests or
credential reads. No new build/test/device/preview execution is claimed here.
The next completion gate is compilation, both Android variants, affected tests,
DEX startup guards, preview rendering and real navigation/picker/session/backend
QA on safe data. Keep this switch out of release/main until those gates pass.

## Verification record — integrated Android source, 2026-10-14

- Full Android CI-equivalent task set passes locally with two workers/4 GiB heap:
  Direct/Play debug assembly and DEX guards, all debug library unit tests, both
  application unit-test variants, library/application lint, design-system and
  Shop UI-test APK assembly. 326 unit tests pass without skips or failures; three
  build-logic verifier tests pass separately. No lint errors; existing warnings
  are not silently removed by this migration.
- Common Shop domain/data and shop-report data compile for iOS Simulator Arm64.
  No iOS application implementation or navigation was changed.
- Pixel 7 passes 20 isolated Shop UI tests and 50 design-system UI tests. Six
  selected application tests cover native PNG encoding and fake-transport form
  outcomes. No real-account storage tests, login/logout, helpful/favorite writes
  or live check-in submissions were run on the user's device.
- CoffeePeek 1.0.547 starts after update installation without clearing user data;
  Happy Coffee detail renders actual backend address/distance/schedule/menu images.
  No fresh AndroidRuntime crash observed. Native PNG checks verify transparency,
  visible pixels, size bounds and unchanged source, not segmentation quality.
- The generation-reset bug reproduced by tests is fixed in detail/list: capture
  the new generation outside the State receiver. Coordinate smart casts and test
  assumptions about Ktor's outgoing body/types were corrected and rerun.

Still pending: real cup extraction/model availability, camera/gallery/recreation,
authenticated backend writes and PNG access, live guest/owner/session/picker/return
parity and individual IDE preview inspection. These limits prevent declaring the
entire Shop feature release-ready or removing shared legacy/iOS consumers.

## Production visual-parity correction — 2026-10-14

User reference images show a concrete regression in the migrated check-in form;
the first implementation replaced production's branded rating cards and connected
form sections with plain stars/default Material fields. Main e5722f9 was merged
into the migration branch at 29cff24; merge conflicts and restored-WIP conflicts
were resolved without deleting the new combined shop/home history or password
recovery flows. Both main and the working branch contain the fetched main tip.

Corrections in local source:

- Check-in header/place-pin and original title/copy; ratings heading/hint, three
  56dp mascot illustrations, bordered cards and 44dp star targets. Reuse the
  existing feature PNGs; the original app assets remain unchanged.
- Shared design-system CoffeePeekFormStep renders the production solid/dashed
  rail and localized optional-field semantics; no feature models/policy in core.
- Date leading calendar/trailing chevron, multiline note shape/placeholder,
  outlined camera tile/overhanging remove badge, left-aligned drink selector and
  visibility title/hint. Keep cancellation, draft, upload and delivery guards.
- Optional notes now follow current main's rule (empty permitted, trimmed length
  at most 1000); domain tests and the invalid-input MVI scenario are aligned.
- Modal sizing reproduced production's outer fractional-height modifier. The user
  subsequently reported that this produces a top-anchored sheet with a gap below;
  it is a known blocker, not validated parity. Keep the anchor viewport unrestricted
  and bound inner content instead. The independent main-based fix is a separate
  PR into develop; its equivalent is still needed in the migrated core consumer.
- Shop public check-in heading reuses headlineSmall SectionTitle, not titleMedium.
  Base Manrope weights/sizes and palette match the legacy sources; they were not
  replaced globally to compensate for incorrect component styles.
- Glass/header controls explicitly use the themed onSurface tint, browsing
  composition provides the correct baseline LocalContentColor, and the bottom
  check-in action restores production's surface/outline rather than a yellow
  primary action. Yellow route controls and photo-overlay white-on-black contrast
  retain their intentional treatment.

Current executed checks: Direct/Play debug assembly and expanded packaged DEX
checks, 217 application unit tests per variant, 9 Shop domain and 87 Shop impl
unit tests, app Direct lint and Shop impl lint all pass. The publication build also
assembles the final Shop, design-system and application test APKs, including the
newest label/rating, theme ownership and history composition sources. Compilation
is not device execution; those new UI scenarios still need a passing run.

Not verified: actual visual equality on Pixel 7, modal/IDE previews in both themes,
the added theme/optional-rail tests and production-label/rating test. The fixture
installation was rejected with 403 by the command-approval service (not GitHub
or CoffeePeek API). No fixture was opened, new form screenshot captured, live
chekin submitted, or main APK updated on the phone in this correction slice.
Command access recovered for the requested publication into existing PR #43.
The new independent modal fix is not included in this refactoring checkpoint.
Use the test-only ShopFormTestActivity parity_preview/parity_dark fixture with
Memories, empty note and 4/4/4 ratings; it makes no DI/HTTP/picker/write calls.
Compare top and scrolled sections against the user's production references and
rerun form/core/host UI scenarios before merging or declaring visual parity.

## Migrated core modal and switch verification — 2026-10-14

The migration now has its own equivalent positioning correction: the core sheet's
fractional inner handle/content column leaves Material's anchor viewport intact.
The Android check-in entry and no-network visual fixture use maxSheetHeightFraction,
not an outer fillMaxHeight fraction. Null/wrap-content and pending-write handle,
drag, Back and outside-tap behavior are preserved by the overlay tests.

The legacy production file is deliberately unchanged here. PR #92 targets develop
from main; its eventual main merge will bring that legacy fix through the normal
history. No cherry-pick or duplicate legacy-file patch is part of this checkpoint.

Visual fixture inspection additionally found default Material switch colors and
thumb sizing. CoffeePeekSwitch reuses the current production tokens for both
themes/states/disabled modes; the Shop visibility field and core settings row
share the primitive. Row semantics remain the sole toggle action. Colocated
paired previews and palette/actual-pixel/touch tests cover the reusable component.

Final Direct/Play APKs and packaged DEX guards, app/core/Shop lint and 537 selected
unit-test executions pass (16 core, 87 Shop impl, 217 app per variant). On the
owned temporary emulator, all 56 design-system and 21 Shop UI tests pass. Final
light/dark form and scrolled-bottom fixture screenshots were inspected: no gap
below the sheet, both thumb colors visible, and the submit button reachable.
These are fake Memories/rating/date fixtures, not a real backend form submission.
The emulator ran read-only and stopped without a snapshot; Pixel 7/10a were not
updated or cleared. Live picker/camera/ML Kit/writes, physical-device parity and
actual IDE rendering remain separate gates.

## Vote recomposition guard — 2026-10-19

Device UI run — 2026-10-19, runtime `96b4162d` (docs-only after): Pixel 7 (serial 2C121FDH2003NC) / API 37, test APKs only (`com.coffeepeek.feature.shop.impl.test`, `com.coffeepeek.core.designsystem.test`); no app package installed or updated.
Shop impl **41/41** (1m32s) and core design-system **84/84** (2m59s) self-targeted UI cases pass, zero
failures/retries/skips. Shop's 41 include the new `votingOneVisitDoesNotRecomposeItsNeighbour`
(counts a neighbouring card's recompositions while another vote is pending). Negative check: with the
pre-fix `ShopPublicCheckInsSection` (`9a96d756^`) the same test fails (`expected:<1> but was:<2>`);
with the fix it passes, so it guards the regression. The restored bean `CoffeePeekLoader` is covered by
core's `PresentationContractTest` (semantics only, not its look; the look was checked on the Sandbox build).

Change: per-card item scope in `ShopPublicCheckInsSection`, memoized own-visit ids, and the public/full-list
cards no longer null every card's vote callback while one vote is pending (both view models still reject
concurrent votes). Visible effect: other like buttons stay tappable during a pending vote; a tap is ignored.

## Retroactive parity audit — 2026-10-20

Open gaps from the legacy-first audit: G1–G10 (high: G1 favorite after login; medium: G2, G3). Full table and verification status: [LEGACY_PARITY_AUDIT.md](../LEGACY_PARITY_AUDIT.md).

Fixes applied 2026-10-20 and owner decisions on intentional differences: see the "Fixes applied" and "Intentional differences" sections of [LEGACY_PARITY_AUDIT.md](../LEGACY_PARITY_AUDIT.md). Device verification is still open.
