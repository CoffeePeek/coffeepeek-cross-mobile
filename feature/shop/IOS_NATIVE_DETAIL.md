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

## Shared native ownership

The toolbar uses the shared Business factory
`iosOwnershipActionsController(ownership, language, kind, id, slug, allowed,
onSuggest, onOwnerEdit)` with the same `BusinessOwnershipEntry` and
`OwnershipAccess`. Its SwiftUI menu and native claim sheet reuse the existing
Kotlin claim lifecycle and mutation rules. Native SwiftUI continues to own
favorite/share. All owner actions retain the current Shop browsing lease guard.
A missing ownership provider produces no owner controller. The controller is
replaced only when its Shop/ownership identity changes; observation and claim
cleanup remain with its existing owner. The obsolete Shop-specific Compose
owner host was removed in the same checkpoint.

Business #404 is a declared integration dependency; its source was merged for
combined validation before both PRs land in `codex/ios-native`.

## Verification, 2026-11-18

Base was refreshed to integration `1d75fd56b` and Business #404 for the final combined macOS build. On Windows,
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

The first macOS run (`44270540581`, head `d93cf335e`) compiled Kotlin and
executed the application native contracts, authentication and Shop native test
steps successfully. Its Xcode step failed because feature DTOs were not exported
and a nested photo callback expected `KotlinUnit`. The app-owned display values
and callback signature fix these concrete diagnostics. The earlier run is not
a passing Xcode check; the corrected head still requires both CI jobs to pass.
The Xcode project was checked with the full OpenStep parser after merging the
latest integration; it has no new missing or duplicate object references.

The shared `ShopCheckInListCard`/ratings are used by both the full Shop check-in
list and this new Shop detail preview. Their artwork now reuses the existing
`CoffeePeekPhotoPlaceholder` for photographs, retaining avatar symbols.
Selected likes and rating stars use the existing gold accent; counts retain
the normal foreground. Paired light/dark previews beside the shared card
compose its actual rating, helpful and artwork primitives with fake data and
no image requests. Device and preview rendering remain unverified on Windows.

The corrected Shop Xcode build (`44276663555`, head `4c43d6d6f`) passed,
confirming the exported display types and nested photo callback fix. The first
run's XML recorded all six native Shop bridge contracts and 37 existing
ShopDetailViewModel tests passing, with zero failures, errors or skips. The
price-disposal assertion, native ownership adoption and shared card visuals
require verification on the final head. The latest Windows metadata check
passed commonMain/iOSMain; body limits only reported the pre-#404 ownership
factory from that earlier base, with no new Shop violations.

## Hours and check-in header parity, 2026-11-25

Source `dce8989e5` plus `codex/migration-ios-shop-filter-parity` working changes.
Schedule expansion drives a 250 ms layout animation on the detail's content stack,
so the panel, arrow and following sections move together. The day list fades as one
group. Reduce Motion disables the animation. The check-in header uses a 48-point
glass chevron button with the existing localized accessibility label and navigation
action. As on Android, the button appears when preview rows are available.

Colocated light/dark previews cover the check-in header with and without the button,
large text and interactive weekly schedule expansion. Reduce Motion requires device review. Swift 6.4.2
syntax parsing passed. Android `directDebug` (`com.coffeepeek.debug`,
`1.2.0.3221-debug`) assembled with `--max-workers=6`, passed the packaged DEX check
and introduced zero body-limit violations. Shop's 112 unit tests in 11 suites
executed successfully, with zero failures, errors or skips. Existing Gradle and
preview deprecation warnings remain. Full native compilation and manual iPhone
animation, VoiceOver and navigation acceptance remain open.

## Guest check-ins and shared account prompt, 2026-11-25

The detail preview and full check-in list now compose the existing
`CoffeePeekGuestAccountCard`, also used by the guest Profile, using their existing
localized labels and auth actions. Hidden rows use one feature-owned
`ShopCheckInGuestPlaceholder`: blurred neutral skeleton shapes, clipped to the
existing card surface. Hidden photos/text are never supplied to this view; the
placeholder is non-interactive and hidden from accessibility. The previous
CoffeePeek image/lock placeholder is removed from these guest rows.

Paired light/dark fake previews cover the placeholder and guest list. The final
Debug 3224 compiled and linked all changed Swift, was signed with validated runtime
entitlements, and installed/launched on the agreed iPhone. Shop's 112 unit results
were UP-TO-DATE in that final check; Android assembly/DEX passed. Guest detail/list
visual acceptance, auth-card back/cancel, dark mode and accessibility remain manual
gates. No account changes or automated UI tests were performed.

## Protected photos and native rating parity — 2026-11-25, Debug 3225

The full check-in list previously called the raw one-shot image decoder for
protected photos. That path has no permission scope; `grantOnlinePhoto` requires
one and fails closed, so the decoder returns nil. The full list now uses the same
`IosCatalogueMedia.observeImage` stream as shop detail, including permission scope,
expiry/session invalidation and cancellation. Cancellation still clears the frame
and rethrows CancellationException; failures clear the placeholder. The same raw
call was found in Shelf archive covers and changed to its existing owner-scoped
stream, preserving the current-photo check. Public-only one-shot callers remain.
A blank thumbnail now falls back to the original photo, like a missing thumbnail.

The existing KMP average is exported through the app-owned row value, so the native
shop card can render five stars and the formatted score like Android. Shop detail,
full list, feed, profile and check-in detail share the native rating renderer.
Ratings round to tenths as in CoffeePeekRating. Full stars bypass masking entirely;
fractional masks use the actual resizable symbol bounds, avoiding intrinsic-glyph
clipping. Accessibility retains each feature's existing localized description.

Non-UI iOS contracts now cover the exposed average, blank thumbnail fallback and
authorized immutable-variant decoding alongside original/bag photos. Native test
execution and real photo rendering must be recorded separately from compilation.
No protected URL/token or photo bytes are added to diagnostics or tracked fixtures.

Final 3225 native compilation/linking and cached signing passed, including the
new KMP average export and both protected-image consumers. The shared execution
plan records the exact artifact/source hashes. The first install attempt could
not find the agreed USB phone; photo rendering, five/full/fractional stars and
native editor appearance have not yet been observed on this build. JVM/cache and
native-test limitations above remain explicit.

The agreed iPhone accepted the 3225 in-place update after USB reconnection; its OS
is now 27.2.1. Developer-image setup/transport interrupted automated startup
observation, as recorded in the shared execution plan. The user's subsequent
photo-viewer lock-in report is addressed by the native-only 3226 follow-up: an
always available close control, native dismissal independent of a loading callback,
and immediate selected-original loading while retaining the valid preview. Owner,
authorization expiry and cancellation policies remain in the existing KMP ports.
