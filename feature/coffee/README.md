# Coffee catalogue

Reviewed against `develop` on 2026-11-27. This is the current feature contract;
superseded migration checkpoints remain available in Git history.

## Ownership and entries

`api`, `domain`, `data` and `impl` are populated Gradle modules. `CoffeeListEntry`,
`CoffeeDetailEntry` and `CoffeePicker` expose browsing, detail and selection
without exporting ViewModels or transport objects. Android uses the shared Compose
presentation. iOS coffee detail uses `CoffeeDetailScreen.swift` with the same
Kotlin ViewModel and an iOS session; the shared catalogue/picker contracts remain.
Application composition owns navigation, authenticated clients, images, sharing
and analytics adapters. Domain has no platform, Compose, Ktor or DI dependency.

## Contracts to preserve

- Data uses `/api/v1/coffees/search`, `/api/v1/coffees/{slug}` and
  `/api/v1/catalogs/coffee-filter-values`. DTOs and backend implementations stay
  inside data. A selected coffee is returned through the narrow picker contract.
- List presentation owns debounced queries, filter drafts, apply/cancel/reset,
  pagination and stale-response guards. A detail failure and a similar-coffee
  failure have independent retry paths. Cancellation is propagated.
- Seller/contact links are validated before external navigation. Images and the
  fullscreen viewer use the application's configured media ports; the feature
  does not create another HTTP/image client.
- In-app detail pushes return to their caller. App links to `/coffees/{slug}` are
  routed by the application root and return to the selected tab root.
- Detail visits and section exposure use the existing analytics adapter and
  consent/session guards. Exposure requires 10% visibility for 500 ms while the
  same visit is visible and unobscured; recomposition is not another visit.

## Source and verification

Start with [public API](api/src/commonMain), [transport](data/src/commonMain),
[presentation](impl/src/commonMain) and [iOS detail bridge](impl/src/iosMain).
Regression suites live in each module's `commonTest` and impl's `iosTest`.
Check the actual Native test selectors in
[ios-verify](../../.github/workflows/ios-verify.yml) when adding a suite; compiling
a test class does not mean it ran.

For code changes, execute affected unit/contract suites, compile the affected
platforms and assemble the selected Android variant with runtime-class checks.
Manual acceptance still needs the tested revision/device: both themes and
languages, Dynamic Type/TalkBack/VoiceOver, filters and empty/error recovery,
picker cancellation, seller/share actions, related navigation, fullscreen photos
and completed/cancelled back swipes. Use a test analytics sink and the
[Sandbox](../../composeApp/SANDBOX.md), not production events or account writes.
This documentation review ran no app build, Native test or device check.
