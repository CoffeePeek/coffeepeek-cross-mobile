# Coffee shops

Reviewed against `develop` on 2026-11-27. Android-first preparation and native
activation ledgers are superseded by this current contract; history is in Git.

## Ownership and runtime

The populated `api/domain/data/impl` modules own shop detail, menu gallery,
public check-in lists and shop check-in creation. Android renders feature Compose.
iOS detail, menu gallery, check-in list and form use native screens/sessions over
the same Kotlin ViewModels. The full check-in list retains the compatibility
`ShopReviews` route name; it is not a restored legacy review editor.

`ShopDetailEntry`, `ShopMenuGalleryEntry`, `ShopCheckInListEntry` and
`ShopCheckInCreateEntry` expose rendering/effects. `PublicShopDetails` and
`PublicShopPreviews` supply narrow cross-feature reads. Composition supplies
repositories, account/blocked-author leases, media, platform actions, ownership
and analytics. Business ownership integrates through its public API. Feature UI
does not resolve Koin or import another feature's domain/data implementation.

## Data and interaction contracts

- Detail and gallery read the existing `/api/CoffeeShops/{slug}` snapshot through
  one backend owner. Data decodes flat detail fields and enriches drink labels,
  categories and order from `/api/menu/drinks`; successful catalogue reads are
  cached for the repository lifetime and failed enrichment can retry. A gallery
  read does not fetch the drink catalogue.
- Shop data owns DTOs, photo upload and check-in request/error mapping. Preserve
  the server's address slug/canonical path, bean, check-in and personal-check-in
  contracts. Photos use server-issued URLs and sort order; never invent an
  `/api/file` URL from a check-in storage key. Preview/detail/original arrays
  remain aligned for the shared fullscreen viewer.
- The form keeps drink/custom-drink, ratings, note, visit date and ordinary
  ordered photos, account-owned drafts and uncertain-delivery guards. Retired
  drink-photo cutout/ML processing and its extra upload/selection flow stay
  removed. Known failures retain correction/retry; uncertain delivery blocks
  blind repeated writes. Saved options absent from a refreshed menu stay visible.
- Favorites use the authoritative server source adapted into Shop's own port.
  A failed initial membership read leaves the control disabled rather than
  guessing from detail's `isFavorite`. Collection selection and synchronization
  are caller effects; guests use the existing sign-in prompt.
- Report/block/helpful-vote/author actions preserve account and author visibility
  checks. Public author IDs are not authentication IDs. Guests receive one
  visible preview card and a bounded hidden count; signed-in detail previews
  retain the existing limit. Hidden author/photo data is not exported to Swift.

## Native and platform presentation

Native detail uses `PlatformShopDetailContent` with the required iOS presenter;
the active native factory does not silently fall back to common detail UI.
`ShopBrowsingSession` and its ViewModel store remain authoritative across account
and blocked-author changes. Native sessions revoke subscriptions/images/rows and
dismiss presented photos on disposal; stale actions cannot use a revoked lease.

Share, contacts/copy, maps/route choice, contributions, ownership, full list,
menu gallery and form navigation remain application ports. Schedule text uses
the supplied current UTC offset; DST-stable shop-local schedules require a shop
IANA zone contract and cannot be inferred from that offset alone. UI preserves
the dedicated hours, guest account card, rating and protected-photo presentation.
The native list uses the root UIKit navigation bar and system Back/interactive
pop; it does not nest another runtime navigation stack. Keyboard-visible forms
hide publication controls and retain the existing numeric/date/note accessories.

The existing consent/session/lifecycle-aware analytics adapter owns shop actions
and exposures. Native geometry feeds its section exposure ledger; recomposition
or controller bridges must not create a second analytics service or visit owner.

## Source and verification

Start with [API](api/src/commonMain), [domain](domain/src/commonMain),
[transport](data/src/commonMain), [MVI/Compose/native sessions](impl/src/commonMain)
and [iOS adapters](impl/src/iosMain). Application assembly is under
`composeApp/.../di/shop`; native Swift views are under
`iosXtool/CoffeePeek/Sources/CoffeePeek/Shop*`.

Existing unit/contract suites cover detail/list/create MVI, schedules/menu/contact
mapping, author permissions, browsing leases and native sessions/gallery media.
For a code change execute affected suites, Android assembly with packaged DEX
checks and the relevant Native CI selectors. Paired fake-state previews remain
beside components; compilation is not evidence of rendering or manual acceptance.

Manual acceptance needs a recorded revision/device: hero/safe areas, both themes,
large text/accessibility, menu/photos/price overlays, list/back-swipe cancellation,
guest/own/blocked authors, form keyboard/date/picker/back/cancel, write outcomes
and refresh, ownership actions and session replacement during media work.
Use [Sandbox](../../composeApp/SANDBOX.md) or an explicitly authorized test setup;
this docs review ran no device, live write, build or analytics check.
