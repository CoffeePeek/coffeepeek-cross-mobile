# Shop feature — incremental migration

2026-10-14: detail's public entry and application-effect contracts are wired locally
through createShopDetailEntry. Detail/list share session guards and explicit
post-write refresh signals; caller-owned date/distance/local-clock inputs do not
leak platform APIs. Android ShopDetail/ShopReviews routes select the new detail/list
renderers; default shared/iOS composition retains legacy screens. A navigation-
owned Android host retains feature VMs across configuration changes, clears them
on account replacement and guards repository calls/effects with revocable leases.
Both Android variants, lint, unit tests and isolated device UI scenarios now pass
(2026-10-14). Live authenticated writes and individual IDE previews remain
unverified; see the compatibility plan's latest record rather than earlier slices.

Incoming main now supplies drinkPhotoUrl. Prepared Shop cards decode/render it
separately from the gallery with Fit and a neutral fallback. The Android form
now has foreground extraction via an app-owned ML Kit adapter, selection/retry/
opt-out, draft retention and separate PNG upload/payload. It does not classify
cups. Changed-first-photo work is cancelled and late replies are rejected;
selected PNG upload failures stop creation. The incoming source had no extraction
provider despite its documentation. Sources compile, regression tests and native
PNG encoding checks pass. Actual cup segmentation/model availability, picker/
camera and authenticated backend submission still require QA.
See [ANDROID_PARITY_PLAN.md](ANDROID_PARITY_PLAN.md) for the next ordered work.

`feature/shop` owns shop browsing. Android source wiring now selects its gallery,
detail, public check-in list and creation entries. Legacy detail/list code remains
for iOS until a separately agreed migration; it is not deleted by this switch.
The current compatibility checklist is [ANDROID_PARITY_PLAN.md](ANDROID_PARITY_PLAN.md).
It supersedes the earlier review-first integration order: the active Android
shop UI now renders public check-ins and creates visits, not legacy reviews.
Preparation slices add a read-only `ShopDetails` snapshot (overview, menu,
locally displayed weekly schedule, coffee catalog, contacts, features, reviews
and public/personal check-ins), plus stateless components with colocated light/dark
previews, selected through app-owned Android platform renderers.
`ShopDetailScreenContent` composes these blocks and emits typed actions with
fake-state previews. Its runtime adapter and typed `MviViewModel` are wired through
the public entry and Android detail renderer, not through feature Koin lookups.
The ViewModel observes and mutates local favorites through the supported pure
`feature/favorites/domain` contract. Its `FavoriteChanged` event is for the
application bridge to notify remaining legacy consumers through FavoriteSync;
that bridge is wired. A failed membership read leaves the favorite control disabled rather
than guessing from the server's `isFavorite` field.
Sharing, suggest-change, and route controls emit platform-agnostic events;
the application maps them to its existing share helper, navigation and
maps launcher through the Android renderer. Suggest-change asks
guests to sign in first. The prepared bottom bar exposes route and check-in
actions; the form/result bridge is connected and emits a post-write refresh signal.
The existing shared/iOS route and ViewModel remain untouched.

| Module | Responsibility | Allowed dependencies and consumers |
|---|---|---|
| `api` | Minimal gallery/detail/list/check-in modal entries, identifiers and caller effects | Compose runtime; application composition |
| `domain` | Gallery/details, review and check-in models, validation and `Result` repository contracts | Pure Kotlin; shop data/impl |
| `data` | Shop HTTP requests, narrow DTOs, mappers, photo uploads and repository factories | Domain, core/network, Ktor, serialization; application composition |
| `impl` | MVI gallery screen, resources, previews and API adapter | API/domain, core presentation/design-system; application composition |

Packages alone cannot enforce the domain/transport/UI boundaries or keep HTTP
types out of the public feature entry. The application passes its already
configured authenticated `HttpClient` to the data factory. No second client or
feature Koin module is created. Gallery and details data are read from the
same `GET /api/CoffeeShops/{slug}` endpoint as legacy; DTOs decode only the
fields these slices need. Both use one `ShopDetailsBackend`, not a parallel
HTTP endpoint. The response is flat inside `data`, with `address.slug` and
`address.canonicalPath`, `beans`, `checkInCount`, `checkIns`, and `userCheckIns`.
The obsolete `data.shopDto` envelope is no longer used. Details optionally enrich
menu labels/categories/order from `/api/menu/drinks`. Successful catalog reads
are cached for the repository lifetime; failures retain parsed data and can
retry on the next details read. Gallery loads do not request this catalog.
The repository accepts the current UTC offset from composition, so shared
data does not depend on Android time APIs. This preserves the legacy
current-offset rule, but cannot be DST-stable without a shop IANA time-zone ID.
The menu mapper keeps the legacy preference for
`urls.fullscreen`/`urls.detail`, falls back to `fullUrl`, drops missing URLs and
sorts by `sortIndex`; the menu comes from `data.menu`.
Coffee, contact and engagement fields come from that same snapshot. The file
origin is supplied by application composition. Check-in photos use the
server-issued `url` (including relative API paths) sorted by `sortIndex`, with
no fabricated `/api/file` URL from a check-in storage key. Check-in models retain
author/shop slugs, drink names, visibility, moderation, revision, and helpful votes.
Legacy review file keys and review vote repositories remain prepared but are not
a replacement for the active check-in flow. Prepared detail MVI/content now use
public check-in cards, the v1 helpful PUT/DELETE contract, full-list/report events
and creation actions. Own visits are detected by author identity or personal
visit IDs, with duplicate-vote guards. The app destination bridge remains pending.
Detail no longer requests review eligibility; the independent prepared legacy
review access/forms remain unused. The runtime adapter refreshes on return and
accepts app-owned session generation. Session replacement hides private details,
cancels stale loads and ignores vote replies from the previous session.
The review editor has a stateless text/rating/photo body and modal shell with
paired previews and domain-owned length validation; no route uses it yet.
The photo source sheet, existing-photo strip, five-photo selection limit and
restored-draft notice mirror the current UI. Application composition must
provide Android gallery/camera picking and convert its selected images to the
feature's pure photo model for creation. The current backend update command has
no `photos` field: the legacy edit UI shows new-photo selection but the repository
silently drops those photos. The new edit form shows existing photos but disables
adding new ones, with an explicit explanation, until server support exists.
The nested photo-source modal may require Interactive/Run Preview in the IDE;
compilation does not verify actual modal rendering or Android picker behavior.
The write repository now prepares review creation through the existing
`/api/Photos/review` presigned upload flow (including the signed
`x-amz-tagging: is_permanent=False` upload header) and `POST /api/ModerationReviews`, and
editing through `PUT /api/ModerationReviews/{reviewId}` with text/ratings only.
The upload client is separate from the authenticated API client; public upload
URLs are validated before any image bytes are sent. This repository is not yet
wired into the application route. A new create-review MVI ViewModel now uses the
write contract and a presentation-only draft-store port. Its runtime sheet
adapter accepts application-owned Android gallery/camera callbacks. An Android
adapter for the existing keyed draft store is prepared in `composeApp`
(text/ratings persisted, photo bytes memory-only). Composition still needs to
create the ViewModel, supply clients and callbacks, and handle success events;
no active Android route uses this sheet yet. Edit-review loading and
submission now also have a prepared MVI ViewModel and runtime sheet. The new
user-review repository searches the authenticated user's published reviews via
the same paged endpoint as legacy, retaining the moderation ID for PUT. It may
request later pages rather than silently treating reviews beyond the first 100
as missing. The edit draft adapter uses the existing per-published-review key;
the current app route and iOS implementation are unchanged. Application
composition must still construct both form ViewModels, supply authenticated
clients and Android callbacks, route success, and verify the flows on device.
The check-in foundation provides a pure input/validation contract and a
`Result`-returning repository. It reads drink choices from the existing
`GET /api/catalogs/drinks` endpoint and submits to `POST /api/v1/check-ins` with
`coffeeShopSlug`, `text`, `rating`, `visibility`, `visitedAt`, optional drink
fields, and uploaded photos. Both visibility modes require text of 1–1000
trimmed characters and all three ratings of 1–5. The parsed visit timestamp
must be positive and not in the future; the clock is injectable for validation
tests. No review title or public-only text rule is imposed on check-ins.
Creation requires an HTTP-success envelope with a nonblank created visit ID;
blank/malformed replies and connection loss never confirm creation.
`ShopCheckInCreationUnconfirmed` tells future UI to check history before retrying
a possibly accepted request. Coroutine cancellation is rethrown.
Reviews and check-ins now share the feature-data
photo upload transport, while keeping separate domain photo/input types and
requesting check-in URLs from `/api/Photos/check-in` without the review tag.
The repository validates before uploading, and uses the caller-supplied
authenticated API client plus a separate public-upload client. The Android
detail/picker route now opens the new check-in entry through an app-owned
platform renderer; the larger detail screen and the iOS form remain legacy.
The date formatter and process-lifetime draft adapter are now
prepared in Android app composition and unit-tested against the existing store.
The new check-in MVI owns typed state/actions/events, catalog loading/retry,
validation, guarded writes and visibility-specific success. Its draft port saves
an uncertainty guard before writes so recreation cannot silently repeat a possibly
accepted request. Known rejection restores retry eligibility; uncertain delivery
requires explicit history checking. Cleanup failure never converts success into
another submission. The prepared runtime sheet now renders stateless
`ShopCheckInCreateScreenContent`, with separate rating/note/date/drink/visibility
components, resource-owned messages and colocated paired light/dark previews.
Photo/rating visual primitives are shared with the unused review editor without
sharing its business models. Pending writes disable dismissal; successful writes
retain their Public/Private confirmation until the caller receives a feed/history
event. Device UI tests are provided separately from unit coverage.

Android `shopCheckInModule` now registers one repository, date formatter,
photo-picker adapter, `AndroidShopCheckInDraftStore` and `ShopCheckInCreateEntry`.
Resolving the graph does not open a draft or read credentials/send requests.
The modal API takes the internal shop ID separately from the public address slug:
`forShop(id, slug)` retains existing draft keys while domain/transport use the slug.
Each binding holds its own draft lease; a stale form cannot overwrite a replacement
even if a new form has already opened it. The entry owns a fresh ViewModelStore
per modal lifetime and clears it on disposal. Closing/reopening restores input
without reusing a completed ViewModel; cancelling a pending write retains the
uncertainty guard. Feature UI still performs no Koin lookup.

Keep a single `AndroidShopCheckInDraftStore` instance in composition:
it wraps the existing process-only draft and keeps the uncertainty guard across
sheet recreation. Logout/clear/replacement invalidates that guard; stale form
state cannot overwrite or clear a replacement draft. The date formatter retains
the existing local-calendar-day to UTC conversion. The Android photo port reuses
the existing URI/JPEG/camera-permission pipeline, caps delivered photos at the
remaining five-photo allowance, and disables submission during processing.
The Android form is active from both normal detail and `forCheckIn` mode.
Guests retain the existing Auth route. Opening the new form does not load the
legacy drink catalog or open a second legacy draft. Accepted submission refreshes
detail without hiding confirmation; feed/history callbacks use the existing
Community/Visited Places destinations. A per-modal session lease cancels pending
requests on logout/account change and suppresses stale results, while same-user
token renewal remains valid. Account-bound drafts cannot move to another user,
including when a previous modal was already closed. Default iOS composition
continues to render its old form and writer.
Android host UI tests use fake transport/auth and verify the platform switch,
single catalog/write, confirmation callbacks, cancellation and draft clearing.
Real picker/camera, preview inspection and authenticated-write QA remain separate
gates; this connection does not complete the full detail migration.
Contact link formatting belongs to presentation; opening links and copying phone numbers remain caller
callbacks, not feature-owned platform calls.

Android composition renders the new entry and adapts its photo-open callback
to the existing full-screen viewer, preserving swipe/zoom behaviour without
adding a feature-to-app dependency or copying that shared component. The
root back stack remains application-owned. Feature UI does not see DTOs, Ktor,
legacy repositories or Koin.
The Android detail menu now offers a separate resource-backed "Фотографии меню"
button when photos exist. It opens the migrated gallery through the existing
platform renderer. Tapping a menu thumbnail still opens the viewer directly;
the default legacy/iOS renderer adds no button and retains its current behavior.

## Migration sequence and risks

1. Add domain/data/API/UI contracts and cover result, mapper and MVI behaviour.
2. Switch only the Android gallery route via a platform renderer; keep the
   legacy shared/iOS renderer as the default.
3. Compile the app and inspect light/dark previews and gallery behaviour using
   a non-production endpoint. A successful build does not prove server data or
   image loading at runtime.
4. Add stateless detail UI and its MVI coordination in independent slices,
   including favorites, check-ins, reviews and navigation, before switching the
   Android detail route. Do not show a reduced read-only replacement meanwhile.
5. Later migrate the rest of shop browsing under this same owner. Retire legacy
   DTOs, repositories and screen paths only after all platform consumers move.

The Android detail source switch represents the current interactions:
check-in cards and full list, helpful/report ownership rules,
guest visibility, check-in draft/submission/results, report/suggest/share/map
bridges, distance/type/price information, and live favorite observation.
Keep the existing picker route and its `forCheckIn` mode working. Do not connect
legacy review forms merely because they were prepared before the backend/UI
change. Viewer/session and device-time providers stay application-owned. Verify
the active flows on device before releasing this renderer switch. iOS
stays on its current path until a separate migration.

Potential risks: future server contract changes must be reflected in shared fixtures;
images may be missing or URLs may expire; iOS still relies on legacy source.
The feature has only default-language resources until translations are audited.

## Android public check-in list (wired locally, not verified)

`ShopCheckInListEntry` and its impl factory prepare the screen currently named
`ShopReviewsScreen` in legacy code; this destination contains check-ins, not
review editor data. The existing `ShopDetailsRepository.getDetails` supplies the
whole available snapshot. There is no additional list endpoint, pagination or
new Gradle module. Domain/data contracts and their manual DI remain unchanged.

The caller supplies a navigation-owned ViewModelStoreOwner, shop ID, monotonically
changing session generation, localized date formatter and back/auth/register/
report/photo callbacks. The lifecycle-owned ViewModel is keyed by shop ID. Root
navigation and the existing photo viewer remain application-owned. On first
resume the initial load is reused; subsequent returns refresh the snapshot.
Failed same-session refresh keeps the existing cards. Initial failure has retry.
Votes and GET refreshes cannot overlap; queued refreshes are coalesced after the
vote. Result cancellation is rethrown, not shown as a transport error.

Guest rendering state contains only the first public card plus aggregate ratings
and the hidden-card count, not hidden authors/notes/photos or personal visits.
Owner detection uses current user ID and personal-history IDs. Owner votes and
reports, unknown/hidden card actions and out-of-range photo indices are guarded
in the ViewModel as well as the UI. Other guest actions request sign-in. Account
changes clear content and cancel work; stale results and buffered photo/report
effects cannot cross generations. Android composition additionally binds transports
to a revocable account/session lease to prevent a write reaching a different
account during a token switch; presentation guards are not backend authorization.

This is local source wiring only. The existing Android full-list route now uses
the feature entry and iOS retains its legacy renderer. Colocated previews, unit scenarios and Android UI scenarios are
authored and now compiled/run on Pixel 7 (20 isolated Shop UI tests). Full backend
write/session/picker parity and individual IDE preview inspection remain pending.
