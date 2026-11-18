# Native iOS ShopDetail checkpoint

## Runtime boundary

The actual iOS route remains `iosShopDetailRenderer` ->
`FeatureShopDetailScreenRenderer` -> `ShopBrowsingHost` -> `ShopDetailEntry`.
`ShopDetailScreen` keeps the existing `ShopDetailViewModel`, event envelopes,
session/refresh generations, resume policy, deep-linked check-in handling and
analytics. `PlatformShopDetailContent` selects a required SwiftUI presenter on
iOS; Android retains the existing screen content. There is no iOS fallback to
the common detail layout once the native factory is configured at startup.

The feature owns localized native display values and intent validation. Native
UI renders hero photos and identity, favorite/share/edit controls, review,
distance and price summaries, description, coffee/roasters/equipment, hours,
menu photos and prices, contacts, features, check-in previews, report recovery,
route and check-in footer, price information and transient messages.

## Shared logic and existing integrations

The application still assembles lease-wrapped details/vote/favorite repositories
and its existing account port. `ShopBrowsingSession` remains authoritative for
account and blocked-author revision changes; its ViewModelStore still owns the
shared detail model. No repository, business or session policy moved to Swift.
Native actions are accepted only while the same browsing lease is current.
Guest snapshots export one check-in card and a bounded hidden-card count, rather
than passing hidden authors or photos into Swift. Signed-in previews keep the
existing three-card limit. Existing `ShopCheckInListRow` and
`ShopCheckInListCard` render the cards and retain their own/report/block/vote
permissions. Original detail events still handle author profiles, block/report,
sign-in, collection selection, favorite synchronization and contributions.

Native media uses `IosCatalogueMedia.observeImage`, the authorized shared image
transport and existing CFPK placeholders. The detail photo effect now presents
the existing `IosPhotoViewer`/SwiftUI viewer. Disposal revokes image callbacks,
clears all exported rows/dates and dismisses a presented photo controller. Menu
and check-in photo intents retain the existing preview/original ordering.

Phone copying, external contacts, share, native route choice, map focus,
check-in form, menu-gallery and full check-in routes still call their existing
application ports. Their feature ownership and navigation contracts are
unchanged. Menu amount/category grouping, validated contacts, schedule text,
price tiers, date formatting and author permissions reuse Kotlin presentation.
No camera, account write or background sign-in flow was added.

Native footer and retry actions reuse `CoffeePeekNativeActionButton`; its new
optional symbol preserves the existing route/check-in icons and defaults to nil
for all other consumers.

The existing consent/session/lifecycle-aware shop analytics adapter remains the
only analytics service. Shared action/content metrics are preserved; native
geometry reports actual visible sections to the existing exposure ledger.
Price/photo overlays retain their overlay ownership.

## Ownership integration dependency

The Business checkpoint #404 publishes the shared
`iosOwnershipActionsController(ownership, language, kind, id, slug, allowed,
onSuggest, onOwnerEdit)` factory for both Shop and Roaster. This branch keeps
`shopOwnershipController` as a narrow temporary host of the existing
`BusinessOwnershipEntry.ProfileActions`, with empty favorite/share slots and
lease-guarded edit/representative actions. Native SwiftUI owns favorite/share.
After #404 reaches integration, replace this temporary helper with the shared
native factory before declaring the ownership toolbar fully migrated. It must
not create another OwnershipViewModel or copy access/claim decisions into Shop.

## Verification, 2026-11-18

Base was refreshed to integration `e67fe3d5d` before publishing. On Windows,
the changed Shop commonMain and iOSMain metadata compiled. The first
metadata attempt found forbidden access to internal design-system resource
symbols; corrected by feature-owned ru/be resources and the application's
existing snackbar/back resources. The subsequent metadata run passed. The
function-body check found no new Shop violations; its two failures on the
original base belong to the existing ownership/public-profile factories.
No baseline entries were extended.

Six new non-UI native contracts cover guest data minimization, signed-in preview
ownership, original/preview photo sources and bounds, hidden/stale row rejection,
validated contacts and disposal clearing. They are included in `ios-verify`
alongside the existing ShopDetailViewModel and browsing lease/media contracts.
Metadata compilation does not execute these tests. Xcode app compilation and
Kotlin Native execution remain macOS CI gates before merge.

Paired light/dark SwiftUI previews are colocated with the detail, information,
inline session error and transient-message views. They use fake data, no DI or
network. No automated UI, screenshot or instrumentation tests were added or run.
No live-account write, camera use or device installation was performed.

Manual gates remain: full-bleed hero/status bar, narrow/large-text layouts,
light/dark contrast, photo/price modal back and swipe cancellation, share,
copy/contacts, route choice and map focus, guest favorite/check-in gates, own
and blocked authors, account replacement during media work, menu/check-in
photographs, form cancellation/submission refresh and ownership claims/edit.

Discovery's macOS build exposed a framework boundary issue before this Shop
checkpoint's first build: feature implementation payload types are intentionally
not exported directly to Swift. Added app-owned `IosShopDetailData`/detail values
and pure forwarding mappings, matching the existing native app boundaries. The
shared feature state, business logic and module exports are unchanged; Swift
now depends only on app-owned types. App values use `summary` for optional
business descriptions to avoid NSObject member-name collisions.
