# Shop feature — gallery and detail preparation

`feature/shop` owns shop browsing. The first Android slice migrates only the
menu-photo gallery; the much larger shop-details screen stays in legacy code.
Preparation slices add a read-only `ShopDetails` snapshot (overview, menu,
locally displayed weekly schedule, coffee catalog, contacts, features, reviews
and user check-ins), plus stateless components with colocated light/dark
previews, without switching that screen yet.
`ShopDetailScreenContent` composes these blocks and emits typed actions with
fake-state previews. Its runtime adapter and typed `MviViewModel` are prepared,
but no application entry or Android detail route uses them yet.
The ViewModel reads and mutates local favorites through the supported pure
`feature/favorites/domain` contract. Its `FavoriteChanged` event is for the
application bridge to notify remaining legacy consumers; that bridge is not
wired yet. A failed membership read leaves the favorite control disabled rather
than guessing from the server's `isFavorite` field.
Sharing, suggest-change, and route controls now emit platform-agnostic events;
the application must map them to its existing share helper, navigation and
maps launcher when the Android detail route is switched. Suggest-change asks
guests to sign in first. The temporary bottom bar exposes route and review
actions until the check-in flow is ready; it is not the final detail UI.
The existing shared/iOS route and ViewModel remain untouched.

| Module | Responsibility | Allowed dependencies and consumers |
|---|---|---|
| `api` | Minimal composable gallery entry and caller callbacks | Compose runtime; application composition |
| `domain` | Gallery and read-only details models with repository contracts returning `Result` | Pure Kotlin; shop data/impl |
| `data` | Shared shop-details HTTP request, narrow DTOs, mappers and factories | Domain, core/network, Ktor, serialization; application composition |
| `impl` | MVI gallery screen, resources, previews and API adapter | API/domain, core presentation/design-system; application composition |

Packages alone cannot enforce the domain/transport/UI boundaries or keep HTTP
types out of the public feature entry. The application passes its already
configured authenticated `HttpClient` to the data factory. No second client or
feature Koin module is created. Gallery and details data are read from the
same `GET /api/CoffeeShops/{id}` endpoint as legacy; DTOs decode only the
fields these slices need. Both use one `ShopDetailsBackend`, not a parallel
HTTP endpoint. The details repository returns a complete read-only snapshot from
one response; it accepts the current UTC offset from composition, so shared
data does not depend on Android time APIs. This preserves the legacy
current-offset rule, but cannot be DST-stable without a shop IANA time-zone ID.
The menu mapper keeps the legacy preference for
`urls.fullscreen`/`urls.detail`, falls back to `fullUrl`, drops missing URLs and
sorts by `sortIndex`. A top-level menu is used when `shopDto.menu` is absent.
Coffee, contact and engagement fields come from that same snapshot. The file
origin is supplied by application composition so review/check-in storage keys
can be resolved without depending on legacy data code. A separate narrow vote
repository implements the existing idempotent helpful PUT/DELETE operation.
Review creation/edit eligibility has a separate authenticated read contract
for `GET /api/CoffeeShopReviews/can-create`; it is not inferred from published
reviews. The ViewModel routes the review action to create/edit events from this
response and fails closed when eligibility is unavailable. The new forms and
application event bridge are not wired yet.
The review editor has a stateless text/rating/photo body and modal shell with
paired previews and domain-owned length validation; no route uses it yet.
The photo source sheet, existing-photo strip, five-photo selection limit and
restored-draft notice mirror the current UI. Application composition must
provide Android gallery/camera picking and convert its selected images to the
feature's pure photo model. Draft storage, write requests and success/failure
handling remain for the next slices. The current backend update command has no
`photos` field: the legacy edit UI shows new-photo selection but the repository
silently drops those photos. Preserve the UI during migration, but do not claim
edited photos were saved until server support exists.
The nested photo-source modal may require Interactive/Run Preview in the IDE;
compilation does not verify actual modal rendering or Android picker behavior.
The write repository now prepares review creation through the existing
`/api/Photos/shop` presigned upload flow and `POST /api/ModerationReviews`, and
editing through `PUT /api/ModerationReviews/{reviewId}` with text/ratings only.
The upload client is separate from the authenticated API client; public upload
URLs are validated before any image bytes are sent. This repository is not yet
wired into a review editor ViewModel or application route.
Contact link formatting belongs to presentation; opening links and copying phone numbers remain caller
callbacks, not feature-owned platform calls.

Android composition renders the new entry and adapts its photo-open callback
to the existing full-screen viewer, preserving swipe/zoom behaviour without
adding a feature-to-app dependency or copying that shared component. The
root back stack remains application-owned. Feature UI does not see DTOs, Ktor,
legacy repositories or Koin.

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

The Android detail route must not switch until the remaining legacy interactions
are represented: application bridges for sharing, suggest-change and route,
review
creation/editing with moderation eligibility, check-in draft and
submission with uploads, and the associated bottom sheets. Viewer/session and
device-time providers and favorite change notifications must be bridged from
application composition. Verify guest
review visibility, own-review restrictions, photos, auth redirects and failures
on device before removing the legacy Android renderer. iOS stays on its current
path until a separate migration.

Potential risks: backend DTO shape varies between nested and top-level menu;
images may be missing or URLs may expire; iOS still relies on legacy source.
The feature has only default-language resources until translations are audited.
