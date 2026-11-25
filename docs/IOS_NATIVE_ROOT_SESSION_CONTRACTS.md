# Native iOS root session contracts

2026-11-25, continuation of the native UI exit. This is a repository-grounded
implementation map, not a completion claim or a new public feature API.

## Application lifetime

MainViewController must initialize platform DI once and return the existing UIKit
tab/navigation hierarchy. One root owner must retain the foreground notification
heartbeat, language/theme observations, analytics foreground/privacy, session
generation, pending links, notification opens, global errors/loading, collection
prompts and update/onboarding arbitration. These subscriptions must not be copied
into individual feature controllers.

RootNavigationLifecycle now owns the shared session transition rules; its native
consumer must reuse NavigatorViewModel/navigationState and its existing handoff
callbacks. PendingNotificationOpens and foregroundNotificationHeartbeat are plain
cancellable application contracts. OwnershipEntry exposes observeSessionChanges,
refreshSession and the shared profileChanges flow. These replace composition
effects rather than duplicating rules in a native root.

Token refresh for the same root navigation session preserves stacks and drafts.
Account replacement revokes private leases, cancels stale requests, clears private
navigation requests and reconstructs only the required roots. Pending login,
check-in handoff and notification open must remain generation-aware.

## Destination lifetime

Each destination owns a feature session and explicit observation handles. UIKit
appear/disappear drives visibility, refresh and analytics; final removal closes
the feature session exactly once. Temporary coverage by Photos/Camera or another
sheet must not discard a draft. Interactive pop cancellation must restore the
same session and visibility rather than construct another writer.

Existing NativeScreenController responsibilities to retain: route identity,
analytics visit, local back policy, system edge swipe, status bar, map expansion,
bottom tab clearance, detail navigation and safe-area ownership.

## Missing plain feature entry contracts

| Owner | Required adaptation |
| --- | --- |
| Discovery | SearchViewModel + MapViewModel session; criteria synchronization, permission callbacks, focus, reselection, active search/map guards, native scroll-idle/review eligibility, filters and roaster slots. |
| Community feed/post | Plain owned sessions and native routes are implemented; updated Swift link and device/modal acceptance remain pending. Existing timeline/post ViewModels retain the writers and guards. |
| Shop detail | Existing ShopDetailViewModel session with ShopBrowsingHostSession/lease explicitly supplied; protected media, favorite prompt, native overlays and ownership actions. |
| Roaster | Expose existing NativeRoasterListSession/NativeRoasterDetailSession construction and ownership; preserve criteria, favorite changes and profile-change refresh. |
| Ownership | Plain account/access observation, resume refresh and profile-change flow; native claim now has explicit lifetime, but tab/detail entry remains composition-owned. |
| Home/history and shop editor | Reuse existing native feature sessions; transfer events, dismissAllowed, access revocation and accepted-save handoff. Shelf brew now has a plain lifetime. |
| Edit profile | Reuse NativeEditProfileSession; explicit saved-event collection, native photo request and final disposal. |
| Already direct native destinations | Replace composition-local factory capture with constructor-supplied factories and dependencies; do not fork their existing sessions. |

Roaster detail and Business/MyBusiness now have direct native route factories;
their old common Android entries remain supported. The Discovery roaster slot
still needs the owned list session and plain criteria/context binding.

## Discovery extraction map

Use the existing SearchViewModel and MapViewModel constructors inside a feature
session with an owned ViewModelStore. Inputs are the same repository, favorites,
city, location, account, analytics, shop-details and optional recent-shop ports
currently assembled by FeatureDiscoveryScreenRenderer. Do not pass DiscoveryHost,
DiscoveryTabState or DiscoveryRoasterSlots into the plain native contract: they
contain composition, layout or rendering callbacks.

Preserve criteria-to-map synchronization, search/map presentation gates, focus
consumption, reselection scroll revision, back precedence, overlay analytics,
native scroll-idle/review eligibility, draft resume subscription and independent
search/map permission request counters. Picker uses SearchPurpose.CheckInPicker,
requestLocationOnMapInteraction=false and the existing lease-bound adapters and
CheckInShopPickerNavigation; do not replace accepted rows with Swift filtering.
Favorites must keep selectCollections before fallback direct writes.

Make native state/filter mapping ordinary by supplying resolved resources and
icon lookup. Existing selectors discoveryBrewArtwork/discoveryTagArtwork,
MapOpeningHours.intervalLabel and pure row/intent mapping remain authoritative.
Filter draft application must retain the latest cityId; current native close and
swipe apply the draft. Discovery publishes only roaster context; application
composition supplies the independent roaster session and nullable results flag.

Installed Compose resources 1.12.1 supplies plain getSystemResourceEnvironment,
suspend getString/getDrawableResourceBytes and decodeToImageVector. Resolve after
applyPlatformAppLanguage, then refresh on language changes. ResourceEnvironment
has no public constructor. VectorPainter has no public plain constructor; do not
invoke its internal renderer or create an empty composition. Use a plain artwork
converter or native assets from the same source, preserving fill/stroke/clip/trim
and group transforms (Aeropress includes negative scaleY).

Native map/media sessions already have ordinary APIs; retain each map session
across snapshots for camera/viewport restoration. Location permission and review
prompt need plain platform owners. Reuse AppReviewPlatform.promptWhenEligible
for cooldown, delay, active-window checks and claiming; Swift must not duplicate
eligibility. Private source identity and late-image cancellation stay in KMP.

Discovery continuation now has NativeDiscoverySession (owned Search/Map VM
store, events, criteria/presentation/focus/reselection/permission/idle effects),
plain native row/intent contracts in commonMain, and a StateFlow filter draft.
Native strings/filter/state mapping uses an ordinary resource lifetime;
CoffeePeekVectorArtwork renders the same vector assets without Painter or a
composition, including transforms/clip/fill/stroke/trim. NativeDiscoveryIosSession
provides ordinary presentation/filter flows; DiscoveryIosPresentation no longer
contains a composable roaster slot. The old adapter's slot stays separate until
the root and picker select the plain binding.

The binding/filter integration passed all 204 Discovery tests / 25 suites. Main
Discovery and check-in selection now select plain owners directly. The picker
owns its browsing host, history lease, permissions and one-shot handoff. Its
native error/retry state replaces a failed generation and keeps back/close usable.
Kotlin/Native application compilation, the real framework and Swift 6 complete
strict ARM64 executable link passed. Standard Android directDebug/DEX checks and
the function gate 128/0 passed. This is not a device visual check or release archive.

## Feed and shop owners

Community needs owned CommunityViewModel/PostViewModel stores using the existing
repository/account/analytics/drinks/author-visibility/saved ports. Do not pass the
composable CommunityHost. Reuse IosCommunityFeed/Card/CheckInDetail transports;
extract localized presentation/date ticker and snackbar queue. Preserve guest
two-card teaser, account/epoch/visibility guards, card keys during overlays,
cursor guards, vote rollback and edit/report/media exclusion. Post reloads on
resume; feed refreshes only for its existing explicit/save-return triggers.
There are no current comments API/VM/UI consumers to migrate.

Shop detail must follow IosShopCheckInList's host binding: a navigation-owned
ShopBrowsingHostViewModel supplies ShopBrowsingHostSession, its lease and its
ViewModelStore. Create ShopDetailViewModel in that store with lease-bound
details/votes/favorites/currentViewer, existing clocks/generations and collection
selection. Rebind on account AND AuthorVisibility revision, revoking before
clearing the old store. Do not replace the lease with a login Boolean. Retain
session-generation event envelopes, monotonic refresh, the two-minute resume
policy, profile-change retry and linked-check-in exactly-once opening. Reuse
IosShopDetail and pure intent/contact/photo bounds/schedule/menu/check-in helpers;
resolve their resources/artwork through ordinary preparation. Favorite membership,
check-in handoffs, verified share/copy/links and media remain app adapters to the
same feature contracts. No repository or permission policy moves to Swift.

Main Discovery artwork uses its plain resource/vector preparation lifetime and
the nested roaster source shares an owned list VM across preview/full-screen.
The transitional Discovery/picker adapter and feature platform-content hooks
were removed with the picker consumer. The common Android screens remain.

Edit profile now also selects its ordinary owned session and direct native
controller. Saved events are collected only while visible; account failure clears
private state without cancelling the UI observer. Native photo imports have a
separate child lifetime, so revocation cancels conversion and rejects late callbacks.

Native Discovery now shares one failure-safe account observation across both
models. Terminal status is explicit, and events are filtered at delivery after
revocation. The picker consumes terminal failure independently of the browsing
host. Main Discovery still needs its terminal retry presentation wired with the
remaining root/session work. Profile/picker changes require a fresh full Swift
link; the earlier linked executable is not evidence for those later sources.

## Verification boundary

Add non-UI contracts for close, observer cancellation, account replacement,
token refresh, stale completion and exactly-once handoff. Keep existing feature
state-machine tests. Do not introduce instrumented UI/screenshot infrastructure.
Compile Android including runtime-class checks and link the native application.
Native device acceptance must cover back/swipe cancellation, keyboard, temporary
modal coverage, foreground/background, themes and larger text. Read-only device
checks do not certify real submissions, OAuth, push or persistence.

Remove the final Compose root only after every active destination and global
host has an explicit owner. Do not replace it with a hidden/transparent Compose
controller, a custom headless renderer or an unverified fallback. Existing common
Android Compose implementations remain supported.
