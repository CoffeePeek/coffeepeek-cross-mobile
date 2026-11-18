# Native iOS contribution forms — 2026-11-18

Checkpoint: codex/ios-native-contribution-forms, PR base codex/ios-native.

Current locations: feature/contributions/impl owns AddShopViewModel, AddRoasterViewModel, SuggestChangeViewModel and ChangeEditorViewModel. Their Compose screens are the Android implementation and were the iOS fallback. MyContributions and ShopChangeRequestDetail already use native iOS screens.

Target: switch the actual AddShop, AddRoaster, SuggestShopChange and ShopChangeEditor destinations to SwiftUI. Preserve existing Kotlin repository writes, field validation, account reset, wizard steps, selection/order, menu revision, schedule preparation and submission outcomes. The application retains native navigation, platform permissions, media preparation and the shareable map provider. No new Gradle boundary is needed.

NativeContributionFormSession is a destination-owned presentation adapter over the existing models. It exposes immutable form snapshots and guarded user actions. The application serializes presentation values for the Swift Codable models; backend DTOs and upload keys stay behind the feature. Local photo reads validate account version and opaque photo identity. A one-shot attachment request uses the existing ContributionPhotoDelivery so cancelled/repeated/previous-account callbacks cannot add photos.

Map selection keeps the existing ContributionMapPickerState geocoding, debounce, validation and confirmation. Its observation is now StateFlow, consumed by both Compose and native adapters, so a native screen does not require a running Compose snapshot manager. The existing MapLibre provider renders the map; SwiftUI owns its pin, controls and confirmation. CoreLocation permission requests stay native. The form confirms coordinates through the existing AddShopAction.MapPicked.

Plan and acceptance:
1. Inventory actual routes, all four models, media limits, public entry contracts and existing native contributions registration.
2. Add native state/actions and preserve shared validation/mutations. Native sessions close with their UIKit owner.
3. Add SwiftUI fields, five-step shop flow, roaster address validation, change section choice, menu/photo editing, schedule and submission states.
4. Use native gallery/camera launchers, keep selection order, bound source requests to account/revision and retain the draft after picker dismissal.
5. Switch root route selection, register source/resources for Xcode and xtool, retire only the last iOS-specific fallbacks; shared Compose remains for Android.
6. Compile metadata, run iOS unit/contract and Xcode verification, review merge diff, then merge through the integration owner.

Analytics inventory: the four existing forms have no separate feature analytics service or event calls. Their same Navigator.Screen identities retain the root NativeScreenController page/exposure tracking. Native success/error feedback adapts the same app haptic function.

Verification evidence:
- 2026-11-18, Windows source metadata: commonMain compiled; first iOSMain run caught a NativeContributionField name collision with the existing read-only request presentation. Renamed the new input type to NativeContributionInput; retry passed.
- Native contract coverage adds guest guards, shared step validation/back, one-shot photo copy and old-account rejection, section-scoped submission, map cancellation/confirmation, one-time GPS camera target and closed-form callbacks. Existing common map tests also cover the new native camera-target action.
- SwiftUI previews use injected fake snapshots for shop/roaster/suggestion/editor, schedule and map controls, in light and dark. They have not been rendered on this Windows host.
- Xcode and executed Kotlin/Native results are pending the iOS-only PR verification.
- Remaining manual iPhone gates: Photos permissions/cancel/iCloud load; camera capture/cancel and repeated plus; initial/denied/manual location and map panning; keyboard/Dynamic Type/light/dark; back and restored SwiftUI graph; safe-account create/edit submission and server outcomes. No production account writes or real camera/device tests were performed here.

