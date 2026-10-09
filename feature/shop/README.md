# Shop feature — incremental migration

`feature/shop` owns shop browsing. The first Android slice migrates only the
menu-photo gallery; the much larger shop-details screen stays in legacy code.
The current compatibility checklist is [ANDROID_PARITY_PLAN.md](ANDROID_PARITY_PLAN.md).
It supersedes the earlier review-first integration order: the active Android
shop UI now renders public check-ins and creates visits, not legacy reviews.
Preparation slices add a read-only `ShopDetails` snapshot (overview, menu,
locally displayed weekly schedule, coffee catalog, contacts, features, reviews
and public/personal check-ins), plus stateless components with colocated light/dark
previews, without switching that screen yet.
`ShopDetailScreenContent` composes these blocks and emits typed actions with
fake-state previews. Its runtime adapter and typed `MviViewModel` are prepared,
but no application entry or Android detail route uses them yet.
The ViewModel observes and mutates local favorites through the supported pure
`feature/favorites/domain` contract. Its `FavoriteChanged` event is for the
application bridge to notify remaining legacy consumers; that bridge is not
wired yet. A failed membership read leaves the favorite control disabled rather
than guessing from the server's `isFavorite` field.
Sharing, suggest-change, and route controls now emit platform-agnostic events;
the application must map them to its existing share helper, navigation and
maps launcher when the Android detail route is switched. Suggest-change asks
guests to sign in first. The prepared bottom bar exposes route and check-in
actions; the form/result bridge is not yet connected.
The existing shared/iOS route and ViewModel remain untouched.

| Module | Responsibility | Allowed dependencies and consumers |
|---|---|---|
| `api` | Minimal composable gallery entry and caller callbacks | Compose runtime; application composition |
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
authenticated API client plus a separate public-upload client. This is not yet
an active screen: Android photo picking and the navigation/composition bridge
remain pending. The date formatter and process-lifetime draft adapter are now
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

Keep a single `AndroidShopCheckInDraftStore` instance when wiring composition:
it wraps the existing process-only draft and keeps the uncertainty guard across
sheet recreation. Logout/clear/replacement invalidates that guard; stale form
state cannot overwrite or clear a replacement draft. The date formatter retains
the existing local-calendar-day to UTC conversion. Neither adapter is registered
or connected to active routes yet. The feature's API remains gallery-only until
detail/form behavior parity and device verification are complete.
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

The Android detail route must not switch until the remaining current interactions
are represented: check-in cards and full list, helpful/report ownership rules,
guest visibility, check-in draft/submission/results, report/suggest/share/map
bridges, distance/type/price information, and live favorite observation.
Keep the existing picker route and its `forCheckIn` mode working. Do not connect
legacy review forms merely because they were prepared before the backend/UI
change. Viewer/session and device-time providers stay application-owned. Verify
the active flows on device before replacing the legacy Android renderer. iOS
stays on its current path until a separate migration.

Potential risks: future server contract changes must be reflected in shared fixtures;
images may be missing or URLs may expire; iOS still relies on legacy source.
The feature has only default-language resources until translations are audited.
