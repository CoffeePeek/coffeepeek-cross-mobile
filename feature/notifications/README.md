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

The bell on the community feed opens a light/dark bottom sheet with one push switch,
date groups, read/unread rows, pagination and snapshot-bounded “read all”. The history
works with push disabled or OS authorization denied. Supported types are ShopApproved,
ShopRejected, RoasterApproved, RoasterRejected, CheckInModeration, CorrectionModeration,
NewShop, Inactivity and General; unknown future types display as General.

Android rows with supported entity metadata remain actionable after reading. The API
resolves an owned notification through GET /api/v1/me/notifications/{id}/destination;
transport DTOs and entity identifiers stay inside the feature. Its public destination
contract distinguishes published shop slugs from correction/submission IDs. Application
composition resolves contributions and owns cross-feature routing. Native push taps
wait for root/session readiness and discard another account's or generation's result.
Errors retain the sheet for retry. Deploy the additive Account endpoint before this client.
