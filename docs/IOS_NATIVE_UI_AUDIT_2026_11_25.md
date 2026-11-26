# Remaining non-native iOS UI — 2026-11-25

Current continuation: both launchers now enter an ordinary Kotlin root owner and
SwiftUI/UIKit navigation/modal controllers. A second read-only reachability audit
found no active Compose UI body, missing native destination or Compose root from
that boundary. `Main` web links return to the selected tab root; retired password
destinations normalize to native login. The historical findings below document
what was replaced. Final integration builds, device behavior and TestFlight
delivery remain separate gates; see `IOS_NATIVE_EXIT_PLAN.md` for current evidence.

Source: `codex/migration-ios-shop-filter-parity`, base `dce8989e58b1` plus the
current working changes. A read-only subagent traced all root destinations,
feature iOS entry implementations, modal variants, registered Swift factories,
and embedded controller chains in both application entry points. No files,
accounts, device settings or backend records were changed by that audit.

Native here means visible SwiftUI/UIKit rendering. Shared Kotlin state, domain,
repositories, ViewModels, analytics and coroutine ownership are not Compose UI.
Design/HIG parity is a separate question: an already native screen can still have
visual defects. Findings below are static reachability evidence, not comprehensive
device acceptance. The primary agent separately installed and observed startup of
Debug 1.2.2 (3226) on the agreed iPhone15,4 / iOS 27.2.1.

## Native presentation follow-up — 2026-11-25

The findings below preserve the original audit. The current follow-up replaces
all three reachable visible Compose bodies identified there:

- Collection membership and quick creation now use SwiftUI page sheets through
  `PlatformCollectionsPicker` and the existing workspace factory. Shared request
  leases, decorated repositories, favorite synchronization and ViewModel writes
  remain in place. The workspace session and nine contracts are platform-neutral.
- UIKit operation cards and native controls now mount the SwiftUI contour through
  `configureNativeLoader`; CoffeePeekNativeLoader no longer creates a Compose
  controller inside the native control.
- Home full-screen presentation bypasses Compose Dialog. Shelf brew owns a plain
  native flow/editor/photo lifetime. Representative claims own a plain feature
  session and directly return the native controller. Community edit/report choose
  the native modal before the Android sheet and no longer use Compose Surface or
  height measurement. Feed/header/message placement uses native controllers and
  constraints; SnackbarHost no longer renders or positions messages.

The root/route compatibility compositions, generic full-screen UIKit interop and
some shared feature presentation adapters **remain active**. This is not a claim
that Compose runtime/dependencies or every container have been removed. Their
remaining owners and migration prerequisites are documented in
[the root session contracts](IOS_NATIVE_ROOT_SESSION_CONTRACTS.md) and
[the native exit plan](IOS_NATIVE_EXIT_PLAN.md). No hidden/headless replacement
root has been introduced.

Evidence: 223 executed Kotlin contracts in 22 suites, no failures; 40 final
tooling contracts passed, including Swift 6 strict photo-model compilation.
Android directDebug assembly and packaged DEX checks passed. Function-body gate:
128 recorded, zero new. The first full iOS 3228 compile/link passed; the final
build includes subsequent lifetime fixes and is pending. Native device visuals,
keyboard, gestures, accessibility and live persistence remain separate gates.

## Reachable visible Compose UI

| Priority | User flow | Remaining content |
| --- | --- | --- |
| P1 | Heart on a shop/roaster → Save to collection | Membership picker toolbar, search, list rows, privacy sections, covers, loading/error/empty/retry states and layout are Compose. UIKit presents the sheet; some controls adapt to UIButton. |
| P1 | `+` inside that picker → New collection | Quick-create title, field, hint, status and layout remain Compose. This is separate from the already native full collection editor. |
| P2 | Block/unblock author → operation status | The status card and label are UIKit. Its animated contour is `CoffeePeekLoader` mounted through a small ComposeUIViewController. |

The membership chain is active on iOS:

- [PlatformKoin.ios.kt](../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/di/PlatformKoin.ios.kt#L46)
  enables picker favorite tracking and the collection-favorite adapter.
- [App.kt](../composeApp/src/commonMain/kotlin/com/coffeepeek/admin/App.kt#L43)
  mounts the permanent picker host.
- [CollectionsPickerHost.kt](../composeApp/src/commonMain/kotlin/com/coffeepeek/admin/di/collections/CollectionsPickerHost.kt#L34)
  calls the public `CollectionsEntry.Picker` contract.
- [CollectionsEntryImpl.kt](../feature/collections/impl/src/commonMain/kotlin/com/coffeepeek/feature/collections/impl/CollectionsEntryImpl.kt#L53)
  and [SavedCollectionsScreen.kt](../feature/collections/impl/src/commonMain/kotlin/com/coffeepeek/feature/collections/impl/ui/compose/SavedCollectionsScreen.kt#L47)
  select picker mode.
- [SavedCollectionsPicker.kt](../feature/collections/impl/src/commonMain/kotlin/com/coffeepeek/feature/collections/impl/ui/compose/SavedCollectionsPicker.kt#L30)
  implements the sheet; `SavedQuickCreate` starts at line 99 and its content at 108.
- Favorite triggers are supplied by
  [DiscoveryAdapters.kt](../composeApp/src/commonMain/kotlin/com/coffeepeek/admin/di/discovery/DiscoveryAdapters.kt#L36),
  [FeatureShopDetailScreenRenderer.kt](../composeApp/src/commonMain/kotlin/com/coffeepeek/admin/di/shop/FeatureShopDetailScreenRenderer.kt#L160)
  and [RoasterPorts.kt](../composeApp/src/commonMain/kotlin/com/coffeepeek/admin/di/roaster/RoasterPorts.kt#L44).

The operation contour is reached through
[AuthorBlockingHost.kt](../feature/author-blocking/impl/src/commonMain/kotlin/com/coffeepeek/feature/authorblocking/impl/ui/compose/AuthorBlockingHost.kt#L44),
[NativeOperationStatus.ios.kt](../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/ui/component/NativeOperationStatus.ios.kt#L26)
and [CoffeePeekNativeLoader.kt](../core/design-system/src/iosMain/kotlin/com/coffeepeek/core/designsystem/component/CoffeePeekNativeLoader.kt#L39).
The native blocked-author list is a different destination.

## Active Compose containers around native content

| Container/flow | Evidence and boundary |
| --- | --- |
| Home check-in create/edit | [Create flow](../composeApp/src/commonMain/kotlin/com/coffeepeek/admin/di/homecheckin/FeatureCheckInCreateFlowRenderer.kt#L90) and [history edit](../composeApp/src/commonMain/kotlin/com/coffeepeek/admin/di/checkinhistory/HomeCheckInDestinationContent.kt#L42) still use [Compose Dialog](../core/design-system/src/iosMain/kotlin/com/coffeepeek/core/designsystem/component/CoffeePeekFullScreenDialog.ios.kt#L19). The form is SwiftUI through `PlatformHomeCheckInEditor.ios.kt`. |
| Brew from a shelf bag | [IosShelfScreen.makeBrew](../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/di/shelf/IosShelfScreen.kt#L131) creates a Compose root inside the SwiftUI cover, then reaches the same native Home editor. |
| Community check-in edit/report sheets | [PostSheets.kt](../feature/community/impl/src/commonMain/kotlin/com/coffeepeek/feature/community/impl/ui/PostSheets.kt#L108) reaches [native sheet rendering](../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/di/community/NativeCommunitySheetsRenderer.ios.kt#L20). UIKit owns presentation, SwiftUI owns form/report content, but [Compose root](../core/design-system/src/iosMain/kotlin/com/coffeepeek/core/designsystem/component/CoffeePeekNativeModalSheet.ios.kt#L40) and [Surface/measurement](../core/design-system/src/iosMain/kotlin/com/coffeepeek/core/designsystem/component/CoffeePeekNativeModalSheet.ios.kt#L86) remain. |
| Shop/roaster representative claim | [IosOwnershipActions.makeClaim](../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/di/ownership/IosOwnershipActions.kt#L38) creates a Compose root. [PlatformOwnershipContent](../feature/business-ownership/impl/src/iosMain/kotlin/com/coffeepeek/feature/ownership/impl/PlatformOwnershipContent.ios.kt#L25) renders the actual SwiftUI ownership form. |
| Feed layout and snackbar | [NativeCommunityFeedRenderer](../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/di/community/NativeCommunityFeedRenderer.ios.kt#L33) retains Compose placement. [CommunitySnackbar](../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/di/community/CommunitySnackbar.ios.kt#L34) retains SnackbarHost queue/timing/transitions; the message card and buttons are SwiftUI. |
| Root and route hosts | [MainViewController](../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/MainViewController.kt#L159) and [NativeScreenController](../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/ui/NativeScreenController.kt#L165) retain Compose lifecycle/interop for several routes. Visible discovery, feed, business, shop/roaster/check-in content reaches native factories. |

These containers mean complete Compose removal has not happened. They do not mean
that the embedded form, screen or card is still a Compose implementation.

## Routes checked as native

The selection table is
[PlatformRootNavigation.ios.kt](../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/ui/PlatformRootNavigation.ios.kt#L358).
Both `iosApp/iosApp/CoffeePeekApp.swift` and
`iosXtool/CoffeePeek/Sources/CoffeePeek/CoffeePeekApp.swift` install the same native
factory set.

| Area | Actual visible implementation |
| --- | --- |
| Tabs, bottom navigation, back stack | UITabBarController / UINavigationController. |
| Discovery, shop picker, roaster discovery, filters | SwiftUI; the map is MapLibre UIKit. The iOS feature entry returns native-handled, with no active Compose discovery fallback. |
| Feed, check-in cards and details | SwiftUI, with the hosting distinctions above. |
| Guest, own and public profiles; public check-ins; connections | SwiftUI. Collection/shelf slots create native controllers. |
| Collection library/editor/public/featured/preferences/poster | SwiftUI. Membership picker and quick-create are the exceptions above. |
| Settings, city, theme, language, policy and deletion waiting | Native controllers and SwiftUI pages. |
| Login, register, recovery and shop-check-in sign-in | SwiftUI/UIKit, including progress/error states. |
| Startup landing and app-version prompt | Native SwiftUI root modals. |
| `+` check-in type picker | SwiftUI sheet. |
| Shop/Home check-in forms and supplementary fields | SwiftUI. Dates, photo library and camera use native UI. Home presentation still retains the Compose container above. |
| Shop/roaster/coffee detail; shop check-ins; menu/report | SwiftUI native controllers; owner controls resolve to native content. |
| Add shop/roaster, suggestion/editor, contribution list/detail | SwiftUI. |
| Business/MyBusiness, claims, editor and QR | SwiftUI through the platform ownership entry; QR sheet is native. |
| Shelf/archive/bag/add/manual form/scan/camera/finish | SwiftUI; AVFoundation viewfinder is UIKit. Scanner execution capability is separate from UI implementation. |
| Notification bell and inbox | Native UIHostingController, SwiftUI sheet/list. |
| Photo viewer in current contexts | Shared SwiftUI viewer and native modal factories. |
| Global errors/loading, permission prompts | SwiftUI global dialog; UIKit alerts/action sheets and system Photos/Camera/CoreLocation UI. |

## Inactive iOS fallback/legacy variants

- `ComposeMainScreen` and old bottom navigation remain in code, but the live iOS
  root exits through `PlatformRootNavigation(...)=true` first.
- Old Compose auth inputs, native-time-row bridge and Compose date-picker chrome
  are bypassed by the current native auth/Home/Shop/contribution/shelf screens.
- The Home editor's Compose fallback and old bean/date/photo-source UI are supplied
  as callbacks but not invoked by the current iOS actual implementation.
- The shop-editor fallback requires a missing native factory; both current entry
  applications install the factory with `ProvidePlatformCheckInEditors`.
- Shared discovery/shop/roaster/ownership/settings/profile/collection Compose UI is
  retained for Android. Its existence in commonMain is not an active iOS route.
- The old notification list is bypassed by the current native bell/inbox.
- Community leaders are disabled (`communityHost.leaders = null`).
- Shared Compose ErrorDialog/LoadingDialog bodies are bypassed by mandatory iOS
  platform implementations returning handled.

Do not delete shared Android implementations based on their iOS unreachability.
Any iOS-only cleanup must prove its last consumer was removed.

## Next migration slices and acceptance

1. Membership picker and quick-create, retaining the same repository, account,
   favorite synchronization and membership commit contracts.
2. Native presentation of Home create/edit/Brew without the Compose Dialog/root.
3. Community sheets without Compose Surface/measurement; preserve draft, cancel,
   keyboard, gestures, session guards and analytics.
4. Native operation contour; remove only then-unused iOS bridges.
5. Retire remaining root/route composition interop incrementally after each last
   consumer is verified.

Local xcross 3226 disables scanner linkage; Apple/Google availability depends on
the test signing/configuration. Those limitations do not make their UI Compose.
This audit does not certify OAuth, push, scanning, real submissions, HIG, gestures,
accessibility or network/permission transitions. The subsequent 3227 visual fixes
do not migrate the two collection sheets or these remaining Compose containers.
