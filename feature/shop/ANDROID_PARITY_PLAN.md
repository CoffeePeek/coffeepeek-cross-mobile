# Android shop migration — compatibility plan

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
- [ ] Connect full-list/report events to the app-owned destinations and verify
  guest/owner behavior on device before replacing the current renderer.
- [x] Handle return/session changes in detail MVI and its runtime adapter;
  detail no longer requests legacy review eligibility.
- [ ] Connect the app's session generation and refresh after completed writes.

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
- [ ] Wire application-owned camera/gallery conversion and the date formatter
  through the active form entry, and verify picker behavior on device.
- [ ] Preserve existing draft lifetime: one shop draft for the app process,
  retain on dismiss/failure, replace on another shop, clear on success/logout.
  Do not silently add durable persistence or retain drafts across accounts.
  Android adapter is prepared around the existing store; single-instance
  composition/route wiring remain pending; adapter unit tests pass.
- [x] Prepare the draft adapter contract and retain an uncertainty guard across
  form recreation. Require explicit history-check acknowledgement before retry.
- [x] Prepare dismissal/repeated-submit guards and retain input on errors.
  MVI guards are tested; the modal is compiled, but device execution and active
  route wiring remain pending.
- [x] Prepare submission/result UI with distinct Public/Private confirmation
  and caller-owned feed/history events; success does not auto-dismiss.
- [ ] Connect those events to Community/Visited Places and verify return refresh.

## 5. Finish detail UI and app composition

- [ ] Match current section order/actions and bottom-bar check-in behavior.
- [ ] Restore distance without extra permission prompts, shop type, price
  explanation sheet, check-in count/full list, report action and photo viewer.
- [ ] Assemble one set of repositories/ViewModels through existing app Koin;
  pass the existing authenticated client and separate upload client.
- [ ] Adapt sharing using canonicalPath, suggest-change/report/auth, map,
  external route/contact/copy, gallery and roaster navigation in the app.
- [ ] Preserve `CreateCheckIn` shop picker and `ShopDetail(forCheckIn=true)`.
  Leave feed/map migration outside this slice; use existing destinations.
- [ ] Add public detail entry only when the full behavior is ready. Switch
  Android through a platform renderer; leave iOS on its current implementation.

## 6. Completion gate

- [ ] Run affected domain/data/impl tests plus both Android app builds,
  application unit-test variants and lint after integration changes.
- [ ] Compile changed KMP domain/data for iOS Simulator as a regression guard,
  without implementing or switching an iOS screen.
- [ ] Inspect previews and Android guest/signed-in flows on safe QA data:
  loading/retry, heart synchronization, photos, reports, submission outcomes,
  drafts, back/picker mode and return refresh.
- [ ] Review dependencies, DTO containment, Result/cancellation and public API.
- [ ] Record remaining QA limits; remove only proven-unused Android glue, never
  shared paths still consumed by iOS.

Do not mark UI parity or live-server correctness complete from compilation or
unit tests alone. The new detail renderer stays disconnected until this gate.

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
introduced. These adapters are not yet registered or connected to live routes.

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

Next: finish isolated UI verification, then connect Android date/photo/draft
adapters through app-owned entry composition (one draft adapter instance).
Pass the existing API/upload clients and map success/feed/history callbacks
without replacing root navigation. Keep active legacy detail/form routes until
the remaining behavior-parity and device gates pass.

This checkpoint exceeds the usual 50-file PR guideline because it completes
previously accumulated work inside the single consolidated PR #43 explicitly
requested by the user. Do not recreate the superseded PR stack for these fixes.
