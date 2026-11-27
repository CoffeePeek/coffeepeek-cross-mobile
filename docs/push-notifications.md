# Push notifications

Reviewed against mobile source and workflows on 2026-11-27. Firebase provides Messaging; this integration does not add Firebase Auth or Firestore. Shared inbox/account/device coordination belongs to [feature/notifications](../feature/notifications/README.md). The backend owns sending, categories and moderation/reminder policies; deployment settings are verified separately by its operator.

## Android configuration

Production package is `com.coffeepeek`; live Debug is `com.coffeepeek.debug`; isolated Sandbox is `com.coffeepeek.sandbox`. Direct/Play are delivery flavors, not separate package identities within the same environment.

Google Services plugin application checks for ignored `composeApp/google-services.json`, `src/debug/google-services.json` or `src/release/google-services.json`. A configured build needs the matching registered Firebase client. Main release CI restores distinct validated Debug/Release JSON; develop distribution uses the Debug client. See [Firebase distribution](FIREBASE_CD.md).

Pinned dependencies are declared in `composeApp/build.gradle.kts` (Firebase BoM 35.0.2, Google Services plugin 4.5.0 at review). Builds without config can compile with push unavailable; release workflows require their configured resources. Do not commit configuration/credentials or rename a production client's package to imitate Debug.

Android 13+ asks notification permission following a successful new check-in, not form opening, edit or failed save. Explicit user enabling can request permission or open system settings after denial. Account push preference and OS authorization are independent. Foreground delivery uses CoffeePeekMessagingService; background display uses FCM/system behavior. Existing channel/user sound choices remain authoritative.

Foreground and background messages use `ic_stat_coffee_bean`; its editable source and previews are in [design/notifications](../design/notifications/README.md). A payload cannot override a channel the user muted.

## iOS configuration

`iosApp/iosApp.xcodeproj` links FirebaseCore/FirebaseMessaging (12.19.2 at review). Supply ignored `iosApp/iosApp/GoogleService-Info.plist` for the production bundle. CI restores it from `FIREBASE_GOOGLE_SERVICE_INFO_PLIST`; unsigned PR verification uses a synthetic fixture that must not ship in Release.

Release provisioning must include Push Notifications, Sign in with Apple and Associated Domains. Debug uses development APNs entitlement; Release/TestFlight uses production. Register the APNs key in the Firebase Apple-app configuration through the normal operator process; keep the private key out of both clients and Git.

Native delegate/APNs registration exchanges the APNs token for an FCM registration token. The APNs token itself is not the backend device address. Native auto-init remains disabled so account/session guards own registration. Foreground authorization changes refresh the permission state. Explicit enabling after denial can open Settings; automatic successful-check-in handling does not repeatedly redirect a denied user.

The pinned native reset path uses public `Messaging.deleteData(completion:)` before registration, including fresh installs without check-in credentials. Failed revocation is retried before a new token is registered; Firebase installation ID and CoffeePeek session/history are retained. See the [pinned public API](https://github.com/firebase/firebase-ios-sdk/blob/12.19.2/FirebaseMessaging/Sources/Public/FirebaseMessaging/FIRMessaging.h). For audible delivery the server payload must include `aps.sound`; foreground sound presentation alone is insufficient.

## Account and navigation safety

Registration requires the current account, OS authorization and account preference. Tokens are not logged. Logout attempts bounded device removal while old authorization is usable; failure/offline handling cannot recall an already accepted system notification. Stale account deliveries and responses are rejected.

Notification destinations are resolved through the owned server notification contract; marking read is separate from resolution. Shared destination/inbox contracts and platform navigation are documented with the [feature](../feature/notifications/README.md) and [native navigation](IOS_NATIVE_NAVIGATION.md), rather than an old Android-only route snapshot.

Server credentials use service-account/ADC configuration outside the client repository. Release secrets, signing and Firebase resources are described in [FIREBASE_CD.md](FIREBASE_CD.md) and [IOS_SETUP.md](IOS_SETUP.md). Do not add a server key or credential path specific to an operator's machine to a client build.

## Verification gates

Use unit/contract tests for preference, token/session generations, cancellation, inbox paging/read and destination failure. Sandbox fixtures never establish FCM/APNs delivery. Android/iOS compile and unsigned native tests do not certify provisioning, permission prompts, terminated-app taps or audible delivery.

On an explicitly agreed device/account, verify foreground/background/cold-start delivery, Allow/Deny/Settings return, toggle/account switching, separate notification taps, stale-account rejection and sound settings. Inspect installed package/version/signature before a non-destructive update; no uninstall/data clearing to bypass incompatibility. Test messages and real account writes require authorization. Historical delivery logs are in Git history; this documentation refresh did not send notifications or exercise a device.
