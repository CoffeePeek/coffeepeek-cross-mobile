# Legacy-first retroactive parity audit — 2026-10-20

Method: the rule in [ANDROID_REFACTORING_PLAN.md](ANDROID_REFACTORING_PLAN.md#legacy-first-parity-gate-mandatory-for-every-owner).
Four read-only code audits compared legacy `composeApp` with the migrated Android path for the six active
owners (Shop, Home check-in, combined history, Favorites, Coffee, Roasters) plus Shop Report.
Nothing was run on a device; "spot-checked" means the lead re-read the cited code after the audit.
Items already recorded elsewhere are not repeated (see each owner plan, `ANALYTICS_PARITY_AUDIT.md`).
Status of every row: **open** unless marked. Fixes belong to the owning branch/PR, not to this file.

## Intentional differences

A difference is not a defect when the UI was deliberately improved. Such rows are closed as
**intentional** with a reason and the owner's confirmation; they stay in the table so the decision is
visible. Analytics, data, session/guard and behaviour rows cannot be closed this way without an explicit
product decision.

Owner decisions (2026-10-20): the like colour, larger/more readable fonts, moved and improved icons are
intentional product changes; **A2** (sign-in block for guests instead of an empty history) is intentional.
Prod is the reference: where prod behaved differently and the change was not deliberate, the prod behaviour
is restored (see "Fixes applied"). R2 is kept as is: the roaster heart needs the public slug (identity of a
favorite), not an address; a roaster without a slug cannot be addressed, so the heart stays hidden.

Rows still **awaiting the owner's confirmation**: A3, A4, A7, B1, G5, G10.
Already documented as approved elsewhere: guest block-author action hidden, menu-photos heading button
removed, yellow info-card icons and badge sizes, glass back button, generic detail error text.

Legend: sev H/M/L. Verification: **S** = spot-checked in code by the lead, **R** = reported by the audit only.

## Home check-in
| id | sev | gap | where to fix | ver. |
|---|---|---|---|---|
| H1 | H | Form state (draft, photos, picker) is lost on Activity recreation (rotation, theme/font/locale change, camera return). The editor creates its own `ViewModelStoreOwner` and clears it on dispose and ignores `LocalViewModelStoreOwner` from `HistoryHomeOverlay`; the global flow does the same. Legacy kept the VM in the host store. | `HomeCheckInEditorApiImpl.kt:46`, `AndroidCheckInCreateFlowRenderer.kt:34` | S |
| H2 | M | Consequence of H1: recreation emits a false `check_in_cancelled` and starts a new flow id. | after H1; skip Cancelled on config change | R |
| H3 | L-M | Analytics semantics changed: `step_viewed(bean)` on open, `retry_clicked(bean)` only on a real retry (legacy fired retry on every open). Tell analytics owner. | none | R |
| H4 | L | Bean-search failure loses `status_code`; other 4xx become `unknown`. | `AndroidHomeCheckInPorts` | R |
| H5 | L | Save-failure texts (401/403, photo upload, validation plural) differ from legacy. | impl `strings.xml`, `IssueText` | R |
| H6 | L | Editor re-fetches the record (extra loader/retry before the form). Accepted. | — | R |
| H7 | M (Sandbox) | No Home rows in the Community feed, no Approved/Rejected/Unknown moderation, no photo variants on Home rows, no 400/401/403/404/409/429 payloads. | `SandboxBackend`, `SandboxApplicationBackend` | R |
| H8 | — | Double refresh on Community resume — **fixed** with the removal of the resume auto-refresh (`7056de2f`). | done | S |
| H9 | L | `consumeInitialHomeId` marks the id consumed before the lease check, so a Community owner-edit open can be dropped silently. | `AndroidCheckInHistoryScreenRenderer.kt:62` | R |
Not a regression: legacy `HomeCheckInScreen` (History/Calendar/Statistics) is unrouted on `main` too.

## Shop
| id | sev | gap | where to fix | ver. |
|---|---|---|---|---|
| G1 | H | Guest taps the heart → Auth → favorite is **not** applied after login (legacy: 10-min deferred intent via `TrackedFavoriteInteractions`, `resumed_after_auth`). Shop adapters have no `FavoriteInteractions` path; whether `FavoriteInteractions.current` exists on Android is unverified. | route Shop favorite writes through `FavoriteInteractions.change` or add a deferred-intent port | S (no usage in shop adapters) |
| G2 | M | Heart state uses `observe()` (fetches the details of every favorite, emits empty first) instead of ids only: N requests per shop open, flicker, heart disabled until hydrated. | `ShopFavoritesAdapter.kt` → `observeIds()`; override in `AndroidShopBrowsingSession` | S |
| G3 | M | `shop_action(check_in)` + `CHECK_IN_START_ATTEMPTED` are always emitted; legacy took the global-create handoff first and emitted only without it → double count in the global flow. No test for it. | `AndroidShopAnalytics.checkInStarted` | S (always emits) |
| G4 | L | Exposure markers wider than legacy (directions when disabled, check_in in picker mode). | `ShopDetailBottomBar.kt` | R |
| G5 | L | Guest can open an author profile from the detail card. | `ShopPublicCheckInsSection.kt:132` | R |
| G6 | L | Photo viewer gets full URLs only (no preview→full progression). | `ShopOverviewHero.kt`, `ShopDetailScreen.kt` | R |
| G7 | L | Image prefetch (`PrefetchListPhotos`) lost in full check-in list/menu gallery. | image slot / renderer | R |
| G8 | L | Detail reloads on every `ON_RESUME` after the first (plan says return refresh; extra network). Decide whether to keep. | `ShopDetailScreen.kt:115` | R |
| G9 | L (Sandbox) | Fixture shop: invalid `priceRange`, no contacts/features/tags/menu timestamps, identical schedules, no 404 slug. | `SandboxApplicationBackend.kt:225` | R |
| G10 | L | Error text and missing-slug check-in feedback differ. | optional | R |
Also: `OpenMenuGallery`/`ShopMenuGalleryScreen` has no UI entry on Android (approved removal of the heading button) — confirm or delete later.

## Combined history
| id | sev | gap | ver. |
|---|---|---|---|
| A1 | M | Card photos show only the feed variant (no thumb→detail sharpening), viewer previews are uncached detail URLs. Fix: `HistoryPhoto`/`imageContent` slot with `highQualityData`. | R |
| A2 | L | FAB hidden for guests (legacy fired the attempt event) — likely intentional. | R |
| A3 | L | Calendar load error no longer shown in the month summary line. | R |
| A4–A7 | L | Title `maxLines`, `showCreate` not `rememberSaveable`, fallback `CheckInSource`, SegmentTick haptic added on Android. | R |
| A8 | M (Sandbox) | No paging (>20), no previous month, no day with ≥4 visits, own Shop row without variants, no single-source failure. | R |

## Favorites
| id | sev | gap | ver. |
|---|---|---|---|
| B1 | M | Saved-shop card differs from legacy `ShopCard(showCatalogDetails=false)`: logos/brew chips shown, price raw `$$` instead of BYN beans, no type label (`FavoriteShop` has no `type`), 2-line title. | R |
| B2 | M | Saved-drink heart has no `favoriteExposure` and is not sized to 48dp (shop/roaster cards have both). | S |
| B3 | L | Roaster card lost the description (up to 3 lines). | R |
| B4–B6 | L | Remove error is an inline block (not snackbar); no auth gate/resume for guest remove; no `syncIdentityNow` before write. | R |
Unverified: whether the legacy Profile favorites preview refreshes after a remove on the new screen (`FavoriteSync.notifyChanged` is no longer called).

## Coffee
| id | sev | gap | ver. |
|---|---|---|---|
| C1 | M | List load error is plain text; legacy showed `LoadErrorContent` with the mascot. Add a `loadError` slot like Roasters. | R |
| C2 | L | Search field lost IME-safe synced text state (check Cyrillic autocorrect/fast typing on device). | R |
| C3 | L | Blank photo URL not filtered in the mapper. | R |
| C4 | L (Sandbox) | Fixture uses non-wire values (kind/form/availability), no brew group, one page, no photo-less/price-less coffee. | R |
Unchanged from legacy: shared `/coffees/{slug}` links are not mapped by `AppLinks`.

## Roasters
| id | sev | gap | ver. |
|---|---|---|---|
| R1 | M | Detail hero should be edge-to-edge (legacy ignores Scaffold padding); new content gets Scaffold padding **and** `navigationBarsPadding` (inset applied twice). Fix: `contentWindowInsets=0` / no padding on the details branch; check visually. | S (code) |
| R2 | L | Heart hidden (not disabled) for a roaster without slug. | R |
| R3 | L | Detail heart exposure placement fixed to `roaster_card`. | R |
| R4 | L (Sandbox) | No unaddressed/blank-name roaster; manual checklist still open. | R |

## Shop Report
| id | sev | gap | ver. |
|---|---|---|---|
| S1 | M | Every exception (including connect/DNS/timeout before send) becomes `ShopReportDeliveryUnconfirmed`, which blocks resubmission; offline users cannot resend. Legacy: error + retry. Fix: pre-send failures → ordinary failure. | S |
| S2 | L | Server message replaced by a fixed string. | R |
| S3 | L | Category order differs (Address/Hours vs Schedule/Address). | R |
| S4 | L | `take(500)` can split a surrogate pair. | R |
| S5 | L (Sandbox) | Endpoint always 200; no rejected/5xx/unconfirmed scenario. | R |

## Fixes applied — 2026-10-20 (branch `feature/roaster-refactoring`)

Unit tests pass; Android instrumented test APKs and the iOS composeApp compile build. Device check: see "Pixel 7 Sandbox check" below.

| id | change |
|---|---|
| H1 (+H2) | Editor no longer owns a private ViewModel store; the caller provides one. The global flow uses an Activity-retained store (`RetainedViewModelStore`) released only when the flow really ends, so rotation/theme/font/camera return keep the draft and no false `check_in_cancelled` is sent. History already used its destination store. |
| G1 | The first attempt (intent kept in the shop ViewModel) **failed on the device**: signing in resets navigation to the root and destroys the detail screen. Now the guest's heart emits `FavoriteRequiresSignIn`; the app-level `ShopFavoriteIntent` keeps one 10-minute in-memory intent and applies it once for the next signed-in session (fetches details, saves, notifies `FavoriteSync`). Tests added. The `resumed_after_auth` analytics flag is not reproduced: the server-favorite events do not carry it on this path; the shop VM still reports the guest attempt/gate. |
| G2 | Shop favorites use `observeIds()` (no per-favorite detail fetches); session wrapper overrides it. |
| G3 | `shop_action(check_in)` / `CHECK_IN_START_ATTEMPTED` are skipped while a fresh global-create handoff exists (test added). |
| G6 | Hero and menu pass detail previews as viewer images and full-screen URLs as originals. Missing photos elsewhere were reported by the owner without locations: **needs concrete screens**. |
| S1 | Connection/DNS failures before sending are an ordinary failure with retry; unconfirmed delivery remains for failures after the write (test added). |
| R1 | Roaster detail Scaffold uses zero insets; hero is edge-to-edge and pads its own controls; no doubled bottom inset. |
| C1 | Coffee list takes an optional `loadError` slot; the app passes the mascot `LoadErrorContent`. |
| B3 | `FavoriteRoaster.description` restored (3-line text on the card). |
| B4 | Remove failures show a snackbar ("Не удалось изменить избранное"); the inline block stays for load failures. |

Still open from the table: G1 analytics flag, G4, G5, G7–G9, H3–H7, H9, A1, A3–A8, B1, B2, B5, B6, C2–C4,
R2 (decided: keep), R3, R4, S2–S5 and all Sandbox fixture rows.

## Analytics parity fixes — 2026-10-20 (second pass)

Unit tests pass (full `composeApp` Direct suite plus shop, favorites, roaster, coffee and home-check-in modules); instrumented
test APKs and iOS composeApp compile build. Not re-verified on a device; Sandbox has analytics disabled, so event delivery
is verified only by unit tests against the recording transport.

| id | change |
|---|---|
| G4 | Directions exposure only when the route button is enabled; check-in exposure not applied to the picker-mode "Continue" button (as in prod). |
| B2 | Saved-drink heart now has the favorites exposure and the 48dp size. |
| R3 | Roaster detail heart exposure uses the same page-driven placement rule as the card (as in prod). |
| H4 | `HomePreparationFailure.statusCode` carries the HTTP status; bean-search failures report `status_code` and category `validation` for 4xx. Test added. |
| G1 | `favorite_changed.resumed_after_auth` is `true` for the write applied after sign-in, and no second `favorite_attempted` is sent for it (the guest tap already reported it). Tests added. |

Decisions: **A2** stays intentional (guest sees a sign-in block, no `check_in_start_attempted` from a guest); **H3** (bean picker
`step_viewed` on open, `retry_clicked` only on a real retry) is treated as an improvement; analytics owners must be told so
dashboards are not misread. Still open: B5/B6 (guest remove gate, `syncIdentityNow`).

### Analytics parity harness

`composeApp/src/androidUnitTest/.../analytics/parity/` (`AnalyticsParityHarness.kt`): a scenario is one user journey, run once
against the **legacy reference** and once against the **migrated adapter**, each on a fresh consent-aware service with a recording
transport; the wire events (names and all properties, except random ids and timings) must be identical. The harness fails on an
empty reference, and `AnalyticsParityHarnessTest` proves it fails on drift.

| Owner | Legacy side | Migrated side | Scenarios |
|---|---|---|---|
| Favorites | `TrackedFavoriteInteractions` | `trackFavoriteMutation` | shop/roaster add, remove, write failure (6) |
| Coffee list | legacy `CoffeeListViewModel` (real VM, fake repository) | `AndroidCoffeeAnalytics.list()` | load, typed query, filter apply/reset, open, failure, filters-opened marker (4) |
| Roaster list | legacy `RoasterListViewModel` (real VM, fake repository/favorites) | `AndroidRoasterAnalytics.list()` | search, tag filter, open, catalogue failure (3) |

Limits, stated plainly: the migrated side is driven through the adapter ports with the call sequence the migrated ViewModel makes
(pinned separately by the ViewModel tests in each feature), not through the migrated ViewModel itself, because feature ViewModels
are module-internal. Legacy analytics that live inside Composables (coffee detail, shop/roaster detail exposures and content
events, shop actions, history, home editor) cannot be replayed without UI and are **not** covered by the harness; they stay
covered by the adapter contract tests. To add an owner: write its two journeys and call `ParityHarness.assertParity`.

## Remaining regressions — fixes and decisions, 2026-10-20 (third pass)

Owner decisions: A1, G7, S3, S4, C2, C3, H5, H9, B6, G8 return to the production behaviour; **B1 closed** (owner: production and
migrated cards do not differ); **S2 intentional** (users see a clear fixed text, never the server's message); **Sandbox fixtures
(G9, H7, A8, C4, R4, S5) are postponed on request**. Unit tests, instrumented test APKs and the iOS compile pass; not verified on
a device.

| id | change |
|---|---|
| A1 | History card photos start with the thumbnail and upgrade to the detail variant (`highQualityImage` slot, `CpImage(highQualityData=)`); the viewer opens on the thumbnail already shown. Test added. |
| G7 | Shop check-in list warms the next cards' thumbnails (`photoPrefetch` slot wired to the app's `PrefetchListPhotos`). The menu gallery has no Android entry, so it is unchanged. |
| S3 | Report reasons use the production order: menu, closed, schedule, photos, address, other. |
| S4 | Report description is cut without splitting an emoji (surrogate pair). Test added. |
| C2 | `CoffeePeekSearchField` (core) keeps cursor and IME composition local (`SyncedTextFieldState`), so autocorrect and fast typing are not rewound; test added. Applies to every screen using the field. Cyrillic IME still to be checked on a device. |
| C3 | Coffee mapper drops blank photo URLs. |
| H5 | Editor save errors use the production texts for 401/403 and a failed photo upload; the validation summary has Russian plural forms. |
| H9 | Community owner-edit request is consumed only when the editor can open (lease current, signed in), so it is no longer dropped silently. |
| B6 | Favorite writes sync the signed-in identity before the write, as in production. |
| G8 | Shop detail reloads on return only after 2 minutes away (share, maps, phone calls no longer refetch). Production reloaded only on session/visibility change; explicit refresh after a check-in still works. Test added. |

Not done: **B5** (guest/expired-session remove opening sign-in and resuming) — a guest has no saved rows, so only an expired session
mid-use is affected; closed 2026-10-20 without code: signed-out users already get the guest login block, and an expired token mid-use shows the remove-failure snackbar. **R2** stays as is. **S2/B1** need no change.

## Pixel 7 Sandbox check — 2026-10-20

Pixel 7, `directSandbox` built from this branch, Sandbox package only (data cleared at install; ordinary CoffeePeek untouched).
Auto-rotation and the Sandbox scenario were restored afterwards.

| Check | Result |
|---|---|
| H1: create-home form across rotation (portrait → landscape → portrait) | **Pass**: sheet stayed open, rating 3 and dose 18 kept, scroll position kept; reopening after close starts at a clean type picker. |
| G1: guest heart → real sign-in screen (local Sandbox login) → app returns to root | **Pass** on the second build: the second shop's heart became filled after sign-in. The first build failed this check (see G1). A scenario/"local account" switch from the Sandbox handle also resets navigation to the root. |
| C1: coffee tab with "no network" | **Pass**: mascot error with retry button. |
| R1: roaster detail | **Pass**: hero edge-to-edge under the status bar, no extra top gap, normal bottom spacing; photos 1/2, links, shops list. |
| B4: favorites remove failure ("write error" scenario) | **Pass**: snackbar «Не удалось изменить избранное», no inline block. |
| S1: report without network | **Not reproducible on the device**: changing the Sandbox scenario resets navigation, so the report screen cannot be left open while going offline. Covered by the unit test only. |
| Large font, dark theme, retry after offline, B3 description on a roaster with a description | Not checked in this pass (Sandbox roaster has a short description; description shows only on the roaster card in favorites when the list data has one). |

Found while checking (not fixed):
- Shop detail, roaster row: the missing-logo placeholder text («Фото обжарщика отсутствует») overflows its circle.
- Sandbox: every shop slug returns the same details, so the favorite saved for the second shop is titled "Sandbox Cafe" (fixture limitation, G9).
- Sandbox: the Google Password Manager sheet appears after a local login (system UI, dismissed without saving).

### Pixel 7 Sandbox check of the third pass — 2026-10-20

Pixel 7, `directSandbox` from this branch, Sandbox package only (data cleared at install).

| Check | Result |
|---|---|
| S3: report reasons order | **Pass**: menu, closed, schedule, photos, address, other. |
| C2: Cyrillic search (Gboard, Russian layout) | **Pass**: fast typing kept every letter and the cursor, tapping an autocorrect suggestion replaced the word without rewinding, the clear button emptied the field and the list returned. A real hardware-keyboard/long-composition session was not tried. |
| G4: picker-mode shop detail | Seen: «Выбрать другую» / «Продолжить» bar, no extra check-in button. |
| A1: history photo upgrade and viewer | **Not verifiable in Sandbox**: the own shop check-in row has no photos and a check-in created with a synthetic photo does not get a photo in the history row (upload bytes stay in memory). Needs the postponed history fixtures (A8) or a real account. |
| G7: list prefetch, S4, H5, H9, B6, G8 | Not checked on the device (no visible effect in Sandbox or needs fixtures). |

## Suggested order of fixes (needs owner decision)
1. H1 (+H2), G1, S1, R1, G2, G3 — behavioral regressions.
2. A1, B1, B2, C1 — visible differences.
3. Sandbox fixtures (H7, G9, A8, C4, R4, S5) so the above can be exercised.
4. Low items as decided per owner.

## Not verified by this audit
Device behaviour (IME, insets, large font, TalkBack, process death), live analytics delivery, iOS, server contracts.


## Follow-up — 2026-10-20

- Shop detail roaster row: the logo placeholder was text overflowing the 64dp circle; it is now the coffee icon with the same accessibility label. Compiles; not re-checked on device (Sandbox roasters have no logo fixture gap issue beyond this).
