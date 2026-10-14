# Combined personal history — current-main parity and next slice

Source audit: 2026-10-14, main e5722f9, migration checkpoint b70b1b0.
This supersedes the standalone-shop destination switch, not the validated shop
history read/session foundation. Android first; preserve the shared/iOS paths.

## Actual owner and incompatibility

`composeApp/.../ui/screen/checkins/ProfileCheckInsScreen.kt` is the active owner
for VisitedPlaces, HomeCheckIns and EditHomeCheckIn(id). It combines shop visits
and home preparations. It currently coordinates VisitedPlacesViewModel and
HomeCheckInViewModel directly in Compose. There is no ProfileCheckInsViewModel.

The prepared history entry renders Calendar/List modes for shop visits only.
Activating it as-is would remove home records, the type filters, creation and
editing flows. Do not switch the root route merely because that entry compiles.

| Main behavior | Prepared migration gap | Required preservation |
|---|---|---|
| All / Shop / Home filter above one calendar and timeline | Shop-only Calendar/List mode selector | Keep the production type filters and combined layout; do not add a second global mode selector |
| One month/date selection; home and shop calendar counts | Independent shop month/selection and shop statistics | One authoritative selection/month, combined counts, next-month guard and clear-date/today actions |
| Timeline merges both sources; day headings, time, rail and stable kind-prefixed keys | Full shop cards in a separate calendar/list view | Keep compact Shop/Home timeline cards; details and photos remain separate modal actions |
| Show more loads whichever sources have more data | Only shop page state | Separate source cursors/loading/errors, deduplication and filtering; never infer one server cursor for both endpoints |
| Shop modal opens shop/photos; home modal opens detail/editor/delete confirmation | Shop/photo callbacks only | Preserve home detail, edit-by-ID entry, delete confirmation and safe caller-owned navigation |
| Create button opens the choice between shop and home | No combined creation entry | Delegate to the current shop picker and home editor during migration; no new writer required for history integration |
| Home date uses preparedOn; shop date uses local visitedAt/createdAt | Shop-only zone-aware instant grouping | Do not reinterpret the home civil date as a UTC instant or apply shop exclusive-to bounds to home date queries |

This table records source behavior, not a passing live-account/device audit.
The current client sends the same date as home `from`/`to` for day filtering;
confirm that backend's date-boundary contract before replacing the adapter.

## Boundaries

- History owns filter/calendar/timeline read coordination and presentation, not
  home creation, recipe validation, editing, deletion, cup extraction or uploads.
- Home owns its preparation models, business rules and read/write contracts.
  Its existing `features/home-checkin` artifact mixes domain and Ktor data, so
  do not add it as a dependency of the new pure history domain.
- The next independent foundation is a populated `feature/home-check-in/domain`
  boundary: real preparation/read models, calendar summaries and narrow reader
  contracts required by combined history. Keep legacy models/writers available
  to existing consumers until their migration; no placeholder api/impl modules.
- Only deliberately supported pure Home domain contracts may be consumed by
  history. No history UI/domain dependency on home data/impl, the legacy module,
  Koin, HTTP, or application classes.
- Application composition supplies account-bound readers and temporary legacy
  adapters, image/date ports and callbacks to existing home actions. Data and
  domain retain manual constructor DI and cancellation-safe Kotlin Result.
- Do not invent a generic DI module, universal activity DTO or core history
  manager. Shared calendar/filter coordination belongs to this business owner.

### Next-module gate: Home domain

Responsibility: independent preparation/read models and reader contracts, with
meaningful domain rules added only when required. Dependencies: Kotlin only.
Consumers: Home data/presentation, combined-history domain/presentation through
supported contracts, and application adapters. Hidden details: DTOs, transport,
photo reservations/uploads and repository implementations remain outside domain.
A package in the existing monolithic artifact does not enforce this independent
dependency boundary: consuming it also brings the feature's infrastructure.
Create the boundary with actual consumers and tested models, not empty scaffolding.

## Ordered implementation and stop points

1. Finish the migrated core modal correction and Shop source/visual parity.
   Retain pending-write locks and independently run placement/overlay/form tests.
   PR #92 remains the separate main-based production fix into develop; do not
   cherry-pick it into the migration branch or duplicate legacy-file edits here.
2. Prepare Home's pure read foundation and app-owned compatibility adapter.
   Audit calendar/day/query semantics, nullable visibility/moderation, photos,
   page counts and missing fields. Preserve preparedOn versus preparedAtUtc.
   Test mappings, Result failures/cancellation and account replacement with fake
   repositories; no live writes or UI route switch in this slice.
3. Coordinate the combined history through typed MVI. One filter/month/date;
   source-specific list/calendar/paging states; retained-content failures;
   cancellation, stale-response rejection and same-account token renewal.
   Only create a domain use case for genuine shared rules/coordination, not a
   forwarding wrapper around each reader call.
4. Prepare production-shaped ScreenContent and component-per-file UI: filter,
   calendar, day group, timeline rail, Shop/Home cards, retry/empty/loading and
   create action. Strings/images are explicit resources; previews stay beside
   each component, with fake state in both themes. Current resource directories
   expose only default values/strings.xml, not a verified second translation.
5. Extend the narrow entry/caller callbacks and Android renderer without dropping
   VisitedPlaces/HomeCheckIns/EditHomeCheckIn(id). Keep current home writers and
   editor/detail flows delegated at the app boundary until their own migration.
   A revoked account closes photos/details and invalidates callbacks immediately.
6. Verify the whole combined Android route before switching it: mixed timeline,
   filters/month/today/date reset, both cursors, partial-source errors and retry,
   guest/account changes, photos/back/return and initial edit ID. Build Direct/
   Play, DEX/unit/lint and isolated UI scenarios. Live owner/backend parity is a
   separate gate; never remove shared/iOS legacy paths on fake-test evidence.

After this route reaches parity, select the next owner from the current audit.
Do not move to profile/auth/community as a substitute for finishing combined
history, or rewrite the full home editor as a prerequisite for read integration.
