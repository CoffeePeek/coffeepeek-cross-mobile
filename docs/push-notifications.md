# Push notifications (FCM)

Firebase project: `coffeepeek`; Android package/iOS bundle ID: `com.coffeepeek`.
The backend owns the queue, moderation events, reminders (7 days, repeat weekly),
templates and category preferences. No Firebase Auth or Firestore is introduced.

## Android

Supply `composeApp/google-services.json` from the Firebase Android app. It is ignored
by Git and must be supplied securely in CI for builds with push support. The Google
Services Gradle plugin is applied only when this file exists, so builds without it
still compile and show push as unavailable. Both Play and Direct use the same package.
Firebase BoM 35.0.2 and Google Services plugin 4.5.0 are pinned in the app build.

Sign in, open Settings → Уведомления → Разрешить уведомления. Android 13+ asks for
POST_NOTIFICATIONS; after a denial, this row can open system notification settings.
Older Android versions use the app/channel system settings. Foreground messages are
shown by CoffeePeekMessagingService; background messages use FCM's system display.
The channel is “Уведомления CoffeePeek”. Tapping opens the app's normal entry screen.

## iOS activation through CI

The existing macOS CI runner installs FirebaseCore/FirebaseMessaging using the
vendored Firebase plugin Swift setup package in tools/ios/xcode_spm_setup. It links
the plist resource and preserves -ObjC without raw pbxproj edits. Debug uses the
development APNs entitlement; Release/TestFlight uses production.
Firebase 12.19.2 is the latest compatible release with Google Sign-In 10: Firebase
13 requires App Check 12, while the released Google Sign-In 10 requires App Check 11.
Upgrade Firebase when a compatible Google Sign-In release is available.

Repository secrets required for push-enabled releases:
- FIREBASE_GOOGLE_SERVICES_JSON: raw composeApp/google-services.json.
- FIREBASE_GOOGLE_SERVICE_INFO_PLIST: raw GoogleService-Info.plist for com.coffeepeek.
- IOS_PROVISIONING_PROFILE_BASE64: App Store profile with Push Notifications,
  Sign in with Apple and Associated Domains. CI validates all three capabilities.

Release workflows fail when Firebase configuration is absent. Dispatch iOS build
verification on a feature branch to compile Kotlin and native Firebase delegate on
the macOS runner before merging. A local Mac is unnecessary.

In Firebase Console, project coffeepeek -> Project settings -> Cloud Messaging ->
Apple app configuration, upload the APNs .p8 with Key ID and Team ID. TestFlight
needs a production key. Keep private keys outside the repository and client apps.
The optional iosXtool host is not used by these workflows and still requires its
Firebase package/resource wiring before being used for push.

The delegate registers APNs, exchanges it for an FCM registration token, presents
foreground banners and refreshes system permission on app activation. It never uses
the APNs token as the backend device address.

## Sessions, preferences and activity

`features/notifications` owns shared coordination. The app supplies authenticated Ktor,
SessionRepository and NativePushBridge. A random installation UUID is kept in platform
storage. Tokens are registered only for the current signed-in account with OS permission.
Registration renews on token changes and foreground entry; activity reports on entry and
every 15 foreground minutes, including when push permission is denied.

Settings expose moderation, inactivity and custom switches for every installation of
the account. Moderation/custom default on; inactivity defaults off and needs explicit
opt-in. Saving uses server state and reports failures. Account changes clear preferences.

Before explicit logout the app attempts device removal with a three-second bound while
the old access token is usable. Native FCM auto-init stays disabled. Token revocation
also runs for session invalidation/account switching and cold starts; failed revocation
is retried before another token is registered. Offline logout cannot recall an already
accepted system notification. Foreground handling ignores another account's userId.
Tokens are never logged. There is no notification inbox or per-submission deep link.

## Backend credential

Firebase CLI login and the app configuration files do not authenticate the backend.
An operator must supply server Application Default Credentials and enable the FCM
HTTP v1 API. A service account needs the Firebase Cloud Messaging API Admin role
(`roles/firebasecloudmessaging.admin`) or a narrower equivalent permission to send.

For a Docker server, install its JSON outside the checkout at
`/etc/coffeepeek/firebase/service-account.json`; the backend compose mounts the
directory read-only and sets GOOGLE_APPLICATION_CREDENTIALS. Enable
`PushNotifications__Enabled=true` and `PushNotifications__ProjectId=coffeepeek` only
after applying Account migrations. Never put a server key in either client app.
CoffeePeek admin email/password is unrelated to this Google service identity.

The service-account download is in
[Firebase project settings](https://console.firebase.google.com/project/coffeepeek/settings/serviceaccounts/adminsdk).
Use an existing server identity when available. A downloaded private key belongs in
secret storage, never in Git or chat. See the backend's docs/api/push-notifications.md.

## Verification

On Windows, verified Direct debug APK assembly, Play Kotlin compilation and three
shared tests: current-account token filtering/logout, failed preference save/account
reset, permission revocation/activity throttling. On macOS also build the Xcode app.

On real devices verify: foreground/background/terminated notification, permission denial
and re-enable, token renewal, logout/account switch, custom category off, moderation
decision and weekly reminder. Use a designated test account and the admin Notifications
page. A 202 response only means queued; inspect provider acceptance and the device.
On 2026-10-20, real FCM delivery was verified on a USB-connected Pixel 9 (API 37).
The existing installed com.coffeepeek had a different signing certificate; an isolated
com.coffeepeek.pushdebug Firebase Android app was registered and installed alongside it.
The device registered with the local Account service, and two custom messages sent
from the local admin were accepted by FCM on their first attempt and posted as Android
notifications with the app open and in the background. The owner confirmed seeing the
first notification. APNs delivery and an Xcode build remain unverified.


## Local Android device test

The normal local.properties API_BASE_URL still targets production. A debug-only test
can override it without editing that file:

```powershell
.\gradlew.bat :composeApp:assembleDirectDebug -PapiBaseUrl=http://127.0.0.1:8080/
adb reverse tcp:8080 tcp:8080
adb install -r composeApp/build/outputs/apk/direct/debug/composeApp-direct-debug.apk
```

Start the local Account/Gateway with dev/dev.local.env and Account migrations applied.
The debug manifest permits HTTP for this loopback test; release builds do not add that
permission. Check the installed app signing certificate before updating: never uninstall
an existing app to resolve a signing mismatch without the owner's approval. The test
uses a local database/account, independent of production credentials and sessions.

If the installed app uses another signing key, use `-PpushTestApp=true` for an
isolated debug package `com.coffeepeek.pushdebug`. Supply the matching Firebase Android
client configuration (project coffeepeek, display name CoffeePeek Push Test) in the local
google-services.json alongside the normal com.coffeepeek client. Install/launch this
package separately; it does not replace the existing app or use its local data.
