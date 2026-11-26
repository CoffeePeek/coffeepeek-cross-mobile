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



### Retained destination observation

The UIKit hosting controller retains the Swift model while map, camera or gallery
covers temporarily hide the form. Its one state observation continues throughout
those covers and is cancelled only in model deinit; the controller closes the
Kotlin session on route disposal. Map address/canConfirm/camera updates, dismissal,
photo delivery and account replacement therefore reach the same active model.
A non-UI contract verifies one subscription receives map confirmation, photo and
account transitions until session close. SwiftUI cover presentation/dismissal and
real permission transitions still require the documented manual iPhone gate.


### Back during submission

The native scaffold omits its explicit Back action while sending, matching the
existing shared SubmissionStatusScreen. Native session consumeBack also consumes
that state for shop, roaster and change submissions, preventing stale button
callbacks from popping and disposing an in-flight destination. The UIKit local
back guard remains active. A non-UI contract holds all three fake submissions
pending and verifies back is consumed without changing their state or drafts.


## Native form parity — 2026-11-25, Debug 3226

AddShop and AddRoaster share a native navigation header and Liquid Glass back
control. Both omit the mandatory-field explanation and duplicate City caption;
the picker retains its localized placeholder, selected city, accessibility label,
validation and single-city restriction. Only existing fields are changed; no
price/schedule fields are added to roasters.

Shop price uses discovery's discrete native slider: unspecified plus three existing
domain option IDs. Feature choices use the same selectable row as filters. Groups
start collapsed, show selected names, animate with a system smooth transition and
respect Reduce Motion. Logos retain the scoped loader; no alternate catalog or
cross-feature dependency is introduced. The map uses a standard SF Symbols marker;
My Location sits above the address card. Schedule presets are removed; manual times,
per-day editing and working-day actions are preserved.

Submission presentation follows Android: 88-point sending/error artwork, localized
title/description, full-width retry/return actions with the retained draft,
160-point existing success mascot, 48-point accent confirmation and shop summary.
Success omits the form header. Existing submission, feedback, account and media
owners remain in KMP; no new Task, queue or isolation escape was introduced.
Editors retain their request/status actions through the same scaffold.

Verification plan: compile the full native app, run contribution domain/data/impl
unit contracts and Android directDebug assembly/DEX checks. Colocated fake previews
cover fields, collapsed features, schedule, map, success and failure in both themes.
They are not rendered on Windows. iPhone gates: keyboard/Dynamic Type, selection,
slider values, map controls, back/cancel and safe-account submission outcomes.
No real form is submitted and no automated UI/screenshot tests are added.


2026-11-25 validation: final native Debug 1.2.2 (3226) compiled and signed. Domain
17 tests/2 suites and data 35/2 executed; impl 82/9 results came FROM-CACHE, all with
zero failures/errors/skips. Android directDebug assembly/packaged DEX and function
limits passed. The packaged success mascot path was verified. Native previews have
not been rendered and submission outcomes have not been exercised against a live
account. Device layout/back/keyboard/map gates remain open pending USB access.


After the user reconnected USB, InstallationProxy verified the in-place 3226
installation on the agreed iPhone15,4 / iOS 27.2.1; the main application version
was unchanged. The same native PID remained running across the 12-second startup
observation. Local screenshots showed an active form transition and later the
new success artwork/layout while the user was interacting. The agent sent no taps,
text, form submission or account mutations; the inspection transport was closed
to leave control to the user. These observations do not certify the remaining
photo-failure exit, selection/map, dark mode, accessibility or complete submission
contracts on a live backend. Captures remain local, outside tracked sources.
