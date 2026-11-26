# Notifications

Layout: `api` (feature interface, immutable inbox state, `PushDevicePlatform`, `NotificationAccount` port), `domain`
(models and repository interface), `data` (Ktor transport, DTOs stay internal), `impl` (the presenter and
`createNotificationFeature`). The composition root adapts its session storage to `NotificationAccount`
(`composeApp/.../di/notifications/SessionNotificationAccount.kt`); the feature has no dependency on the legacy modules.

Owns notification history, unread state, the account push switch, device registration,
post-check-in permission coordination and foreground activity reporting.
The application composition consumes `api` and supplies the authenticated HTTP client,
session repository and native device port. Domain has no platform or transport imports;
DTOs and HTTP endpoints stay internal to data. Shared presentation coordinates session
changes; platform SDKs remain in Android/iOS application boundaries. Application DI
wires successful shop/home creation callbacks to the feature API. Automatic permission
requests respect a previous denial. Explicit enabling from the notification panel
requests authorization or opens OS settings after a denial. Foreground push receipts
refresh the history, including when a previous request is still in flight.
No other feature may depend on internals. A separate module follows the existing
author-blocking feature boundary and prevents native Firebase dependencies leaking into
the shared business code. The legacy modules have no dependency on this feature.

The public API exposes immutable presentation state and actions. Domain models,
repositories, transport DTOs and the presenter implementation remain internal.
Android's Compose renderer lives in notifications/impl and uses core/design-system;
composeApp owns its root navigation adapter and native intent handoff. The remaining
legacy shared renderer serves iOS. Android/shared and native iOS root composition pass
the bell into the community header through a Composable slot; the community feature
does not import notification implementation or require its DI binding in isolation.
Account/session changes clear the inbox and fence
late responses; login and foreground entry never overwrite saved push preferences.

The bell on the community feed opens a light/dark bottom sheet with date groups,
read/unread rows and pagination; the push preference lives in Settings. Android's
migrated panel and iOS's transitional panel mark a row read after it has been mostly
visible for two continuous seconds while the app is resumed. Leaving the viewport
or backgrounding the app resets the timer, and several visible rows are written in
sequence. The history works with push disabled or OS authorization denied. Supported
types are ShopApproved,
ShopRejected, RoasterApproved, RoasterRejected, CheckInModeration, CorrectionModeration,
NewShop, Inactivity and General; unknown future types display as General.

Android rows with supported entity metadata remain actionable after reading. The API
resolves an owned notification through GET /api/v1/me/notifications/{id}/destination;
transport DTOs and entity identifiers stay inside the feature. Its public destination
contract distinguishes published shop slugs from correction/submission IDs. Application
composition resolves contributions and owns cross-feature routing. Native push taps
wait for root/session readiness and discard another account's or generation's result.
Errors retain the sheet for retry. Deploy the additive Account endpoint before this client.

## Visible-read panel verification — 2026-11-02

- Source: `codex/migration-notifications-visible-read-develop` based on `origin/develop` at `4ff207f40`. Android variant: `directSandbox` (`com.coffeepeek.sandbox`). No device was selected, and no APK was installed.
- `:feature:notifications:impl:testDebugUnitTest`: 34 tests passed, 0 failed, no retries. This includes multiple simultaneous read receipts and an account change during a pending write.
- `:composeApp:assembleDirectSandbox`: passed with `--max-workers=6`. Sandbox isolation and packaged DEX runtime-class checks passed.
- Paired light/dark previews remain beside both row and sheet implementations; the row previews use a long message. IDE preview rendering and manual light/dark, large text, accessibility, scroll, back and modal checks were not run. iOS compilation and live backend acceptance remain unverified.

## Selected city audience — issue #412

`NotificationCity` is a narrow application-supplied port over the existing current city.
It emits resolved selections only: absence of an initial emission means loading, while
`NotificationCitySelection(null)` means a genuinely empty resolved selection. The shared
`CityPreference` publishes this replay after its existing manual/automatic resolution;
Settings and Discovery keep the same selection rules and persistence. The composition
adapter does not load a catalog, request coordinates or ask for location permission.
Both Android and native iOS receive the same common Kotlin synchronization.

The signed-in account sends `PUT /api/v1/me/notifications/city` with the required nullable
`citySlug` field. Explicit null is serialized; only HTTP 204 is accepted. An unavailable
endpoint or a rejected city remains a failure and is retried on foreground entry or the
next selection. City synchronization works with push disabled, no OS permission or an
unavailable provider, and does not change the saved push/category preference.

A collector belongs to the notification account generation and is canceled on account
change or logout. Pending city changes conflate to the latest value. A separate city-write
mutex serializes requests across generations: if an old transport ignores cancellation,
it finishes before the current generation writes its latest city. The current account is
checked after taking that mutex. Inbox loads, device registration and login do not wait
for it. Existing analytics events, exposure ownership and native Firebase transport stay
unchanged; no new telemetry or provider requests are introduced.

Backend companion: [coffeepeek-backend#381](https://github.com/CoffeePeek/coffeepeek-backend/pull/381).
Apply its Account migration and deploy the Account city contract/consumers before updated
Shops producers and this client. The server owns recipient filtering and queued-delivery
checks; a foreground-only client filter cannot enforce background notification delivery.
Older device/push contracts are preserved.

Verification plan: execute notification data/presenter unit contracts, passive composition
adapter and Sandbox backend regressions; build DirectSandbox including DEX/runtime-class
and manifest isolation checks; run `verifyFunctionBodyLimits`. Cover loading versus empty,
automatic/manual changes, HTTP 404/503, cancellation, A→B→A with a noncancellable old write,
latest-city completion order, and unchanged push controls. No UI/instrumented tests,
phone deployment, real accounts, production analytics or live FCM/APNs calls are planned.

### City sync verification — 2026-11-26

Source: `codex/migration-412-selected-city-sync`, fast-forwarded from the initial worktree
base `43dfcc43f` to the published reconciliation parent `a030f9ac39c3b74f22086345ad196e198f116eb3`
before verification. No root checkout files were changed.

- Notification data: 7 executed tests in 2 suites; notification impl: 42 in 5 suites.
- Application DirectDebug non-UI unit tests: 744 executed tests in 168 suites, including
  the passive CityPreference adapter and Sandbox city transport. Total: 793 cases in
  175 suites, zero failures/errors/skips. Data tests executed in the first attempt and
  remained up-to-date in the successful retry; no failed test case was retried.
- The first new presenter-test compilation failed because expected nullable pairs needed
  explicit generic types. This fixture-only error was corrected before the passing run.
- `verifyFunctionBodyLimits` passed with 125 recorded entries and zero new violations.
- `assembleDirectSandbox`, Sandbox manifest isolation and packaged DEX runtime-class checks
  passed. Built APK: `com.coffeepeek.sandbox`, version `1.2.2.3387-sandbox`, code 3387.
  No device was selected and no APK was installed or launched.
- Existing Compose Preview deprecation, experimental-test opt-in and native symbol stripping
  warnings remain. No new UI/resources or Gradle dependencies were introduced.

Shared tests prove loading does not clear a server city, resolved null serializes explicitly,
failed/404 requests are retried, and NonCancellable old minsk completion precedes the current
brest write after A→B→A. The city mutex is separate from device synchronization, while account
and notification-generation guards remain in force. This change adds no analytics events.

Native macOS/iOS compilation, manual UI review, real backend persistence and FCM/APNs delivery
were not exercised locally. Sandbox and in-memory contracts do not certify those gates.
No UI/instrumented/screenshot test, real-account write, live push, merge or deployment ran.
