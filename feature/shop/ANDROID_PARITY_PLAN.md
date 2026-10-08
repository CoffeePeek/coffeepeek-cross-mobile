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
- [ ] Add creation MVI and UI handling for success, known rejection and
  unconfirmed delivery (check history before repeating the write).

## 3. Finish data and MVI interaction parity

- [ ] Enrich menu labels/categories/order from `/api/menu/drinks`, preserving
  usable details if the optional catalog request fails.
- [ ] Observe favorites continuously, not only on initial detail read. Keep
  the single Android favorites writer and stored representation compatible.
- [ ] Add check-in helpful PUT/DELETE contracts and owner/duplicate guards;
  wire check-in reports to the existing app-owned destination.
- [ ] Preserve full-list entry, guest preview/sign-in/register, own-check-in
  restrictions and current-user identity fallback via personal visit IDs.
- [ ] Handle return/session changes and refresh after completed mutations.
  The detail screen must not depend on review eligibility to create a check-in.

## 4. Create the check-in form without changing behavior

- [ ] Typed MviViewModel, State/Action/Event, one action entry point and
  conflicting-operation guards.
- [ ] Stateless form content and separate components; paired light/dark
  previews beside each component; text/accessibility in resources.
- [ ] Three ratings (default 4), required note, visit date, optional drink or
  custom drink, five-photo limit and Public/Private toggle (default Private).
- [ ] Drinks loading/error/retry; application-owned camera/gallery conversion
  and date/time conversion with the same meaning as the current date picker.
- [ ] Preserve existing draft lifetime: one shop draft for the app process,
  retain on dismiss/failure, replace on another shop, clear on success/logout.
  Do not silently add durable persistence or retain drafts across accounts.
- [ ] Prevent dismissal/repeated submit during a write; retain input on errors.
- [ ] Preserve submission/result UI and transitions to Community or Visited
  Places, including distinct Public/Private confirmation.

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
