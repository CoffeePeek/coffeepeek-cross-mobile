# Shared Roasters UI and iOS activation

## Scope and ownership

Roasters keeps its existing api/domain/data/impl Gradle boundaries. Domain,
repository contracts, data and MVI already live in commonMain. Move the existing
Android Compose implementation into commonMain and activate it through shared
application composition on both Android and iOS. No new module is needed.

## Current and target locations

| Current | Target |
|---|---|
| api/src/androidMain: Compose entry interfaces, image/viewer slots and screen analytics | api/src/commonMain: the same public contracts |
| impl/src/androidMain: entry factories, screens and components | impl/src/commonMain: one Compose implementation |
| impl/src/androidMain/res: Russian strings | impl/src/commonMain/composeResources: the same strings |
| composeApp/androidMain/di/roaster: repository assembly, ports, analytics and renderer | composeApp/commonMain/di/roaster: shared application adapters |
| Android-only feature dependencies and renderer registration | commonMain dependencies and common Koin assembly |
| iOS default LegacyRoasterScreenRenderer | shared feature renderer through initKoin |

## Public API and dependencies

Keep RoasterDiscoveryEntry, RoasterDetailEntry, analytics, favorites, shop-photo and
image/viewer contracts. Make the existing Compose contracts available to iOS;
keep ViewModels, DTOs and implementations out of the feature API. The app supplies
its existing image loader, photo dialog, search header, browser, haptics,
navigation, analytics, settings-backed catalog cache and server favorites.

Domain stays Kotlin-only. Data depends on its own domain and core/network. Impl
depends on its own api/domain, core/presentation, core/design-system and shared
Compose/lifecycle/resources. Cross-feature integration remains in composeApp.

## Migration steps

1. Move API/UI and strings, enable common Compose dependencies, replace Java UUID
   with Kotlin UUID and Android resource/preview APIs with shared equivalents.
2. Move and rename reusable application adapters; keep their validation, session
   guards, cancellation, analytics properties and cache format unchanged.
3. Register one Roasters graph and renderer in common Koin. Remove duplicate
   Android registration. Keep UIKit/SwiftUI navigation and platform adapters.
4. Run Roaster domain/data/MVI and application adapter tests. Build Direct/Play
   Debug and their packaged DEX checks; compile iOS device/simulator Kotlin.
5. Record compilation results and remaining macOS/device acceptance separately.

## Risks and acceptance

- Verify iOS ViewModel ownership across native push/pop and Discovery/search;
  each destination keeps its own detail state, and Discovery/search share a list.
- Check safe areas, keyboard dismissal, both themes, back swipe, photo dialog,
  browser links, haptics, favorites authentication and analytics on iOS.
- Keep existing legacy code still used by Contributions and parity fixtures.
- Retain the current endpoints and roaster_catalog_v1 cache; no cache reset,
  account mutation, new storage format or native-screen rewrite is required.
- Kotlin compilation on Windows does not validate Xcode linking, resource
  packaging in the native app, or device behavior. These remain explicit gates.
- Do not add or run UI/instrumentation/screenshot suites for this migration.

## Verification

2026-10-21, Windows host, Kotlin 2.4.20, existing JDK/Gradle configuration:

- Direct Debug and Play Debug APKs assemble; packaged DEX/isolation checks pass.
  The final guard run also verifies RoasterModuleKt, FeatureRoasterScreenRenderer,
  RoasterEntryImplsKt and RoasterRepositoryFactoryKt definitions in both APKs.
- 618 unit results: application Direct 586, Roaster domain 1, data 12, impl 19;
  zero failures, errors or skips. Shared favorites/cache/photo/analytics adapter
  tests moved to commonTest; the Android DI/Sandbox integration test remains.
- Full composeApp Kotlin compilation passes for iosArm64 and iosSimulatorArm64,
  including the shared Roasters UI and application adapters.
- composeApp common tests compile for iosSimulatorArm64. Native tests were not
  executed on Windows.
- Roaster strings appear in the app's aggregated and processed iOS Simulator
  resources, as well as both Android APK asset inputs. Shared previews retain
  each existing fixture in explicit light/dark configurations.
- Main validation passes in 6m41s; final DEX/Native-test compilation passes in
  35s. Logs are retained locally under build/reports/roaster-ios/ (ignored).

No Xcode/Swift link, framework embedding/signing, iOS simulator/device launch,
manual screen acceptance or UI/instrumentation/screenshot test was performed.
The native-device acceptance gates above remain open.
