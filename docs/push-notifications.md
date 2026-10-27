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

After a signed-in user successfully creates a shop or home check-in, Android 13+
asks for POST_NOTIFICATIONS once per installation. Opening the editor, a failed
submission or editing an existing check-in never triggers the prompt. A denial is
respected; subsequent check-ins do not open system settings or repeat the prompt.
Android 8–12 already allow notifications without a runtime permission prompt.
The notification panel also has one push switch. Explicit enabling requests permission
on first use or opens Android notification settings after a previous denial. Disabling
is saved in the account and unregisters this device; later check-ins do not undo it.
Foreground messages are shown by CoffeePeekMessagingService; background messages
use FCM's system display. The channel is “Уведомления CoffeePeek”. Android taps resolve
the owned notification and open its destination after the current session is ready.

The channel explicitly uses the default notification sound with notification audio
attributes; foreground notifications also specify DEFAULT_SOUND. Android keeps the
existing channel ID, including any sound/mute choice the user has made. Recreating
a channel cannot override its saved sound or importance. Silent mode, Do Not Disturb,
notification volume and user-muted channels still govern whether a sound is heard.
See [Android notification channels](https://developer.android.com/develop/ui/views/notifications/channels).

Both foreground and FCM background notifications use `ic_stat_coffee_bean`: a white
24 dp vector of the standing mascot holding a coffee bean, with transparent eyes,
seams and a thin outline around the held bean. The editable SVG and Figma previews
are in `design/notifications/README.md`.

## iOS activation through CI

The Xcode project links FirebaseCore/FirebaseMessaging 12.19.2 and the
GoogleService-Info.plist resource, with -ObjC in both configurations. CI restores
the release plist from its secret without rewriting the project. Local Xcode
builds also need that ignored resource at iosApp/iosApp/GoogleService-Info.plist.
Unsigned PR verification copies a synthetic plist and needs no secrets; the
release configuration validator rejects this fixture. Debug uses the
development APNs entitlement; Release/TestFlight uses production.
Firebase is pinned to 12.19.2 for compatibility with Google Sign-In 10: Firebase
13 requires App Check 12, while the released Google Sign-In 10 requires App Check 11.
Upgrade Firebase when a compatible Google Sign-In release is available.

Repository secrets required for push-enabled releases:
- FIREBASE_GOOGLE_SERVICES_JSON: raw composeApp/google-services.json.
- FIREBASE_GOOGLE_SERVICE_INFO_PLIST: raw GoogleService-Info.plist for com.coffeepeek.
- IOS_PROVISIONING_PROFILE_BASE64: App Store profile with Push Notifications,
  Sign in with Apple and Associated Domains. CI validates all three capabilities.

Release workflows fail when Firebase configuration is absent. Relevant PRs to
main/develop automatically compile Kotlin and the native Firebase delegate and
run the non-UI ViewModel compatibility tests on the macOS runner. Manual
verification on a feature branch remains available. A local Mac is unnecessary.

In Firebase Console, project coffeepeek -> Project settings -> Cloud Messaging ->
Apple app configuration, upload the APNs .p8 with Key ID and Team ID. TestFlight
needs a production key. Keep private keys outside the repository and client apps.
The optional iosXtool host is not used by these workflows and still requires its
Firebase package/resource wiring before being used for push.

The delegate registers APNs, exchanges it for an FCM registration token, presents
foreground banners and refreshes system permission on app activation. It never uses
the APNs token as the backend device address.
The same successful check-in callback requests alert/sound/badge authorization only
while authorization is not determined; denied users are not redirected to Settings.
Explicit enabling in the notification panel opens iOS Settings when authorization was
previously denied. Returning to the app refreshes permission and the displayed switch.
For audible iOS delivery, the sender must also include `apns.payload.aps.sound =
"default"`. Foreground `.sound` presentation alone does not supply a missing sound
in the APNs payload. See [FCM Apple message handling](https://firebase.google.com/docs/cloud-messaging/ios/receive-messages).

## Sessions, preferences and activity

The shared notification panel shows a support code while native iOS registration is
pending or failed: RESET_PENDING, APNS_PENDING, FCM_PENDING, or RESET_/APNS_/FCM_
followed by the NSError numeric code. FCM_EMPTY means the provider returned neither
a token nor an error. These codes contain no tokens, provider response bodies or
credentials. A successful token clears the code; account switching clears it and
ignores late callbacks belonging to the previous account. Registration and token
revocation rules are unchanged. This diagnostic requires a new iOS build; it does
not diagnose an already-installed older TestFlight build remotely.

`feature/notifications` (api, domain, data, impl) owns shared coordination. The app supplies authenticated Ktor,
SessionRepository and NativePushBridge. A random installation UUID is kept in platform
storage. Tokens are registered only for the current signed-in account with OS permission.
Registration renews on token changes and foreground entry; activity reports on entry and
every 15 foreground minutes, including when push permission is denied.

The feed bell opens notification history and one master push switch. The saved account
setting is loaded from GET /api/v1/me/notifications/push and changed with PUT and an
explicit {"enabled":true|false} body. Login and foreground entry preserve the setting
and existing category preferences. OS permission remains independent; the switch is
shown active when both the account setting and device authorization allow push.

GET /api/v1/me/notifications returns items, nextCursor and unreadCount. Pass before
with the previous nextCursor to load another page. PUT /{id}/read marks one owned row;
PUT /read with {"throughId":"newest-visible-id"} marks the displayed snapshot and older
messages, leaving later arrivals unread. Foreground receipts refresh the open panel
and badge. Errors preserve the previous list and expose retry actions.

Types include approval/rejection for shops and roasters, check-in moderation,
correction moderation, newly published shops, general messages and existing inactivity
reminders. Newly published shops are announced to all active CoffeePeek accounts;
push delivery still respects opt-out. History remains available with push disabled.
The backend retains the existing policy of purging rows 30 days after push expiry.

Before explicit logout the app attempts device removal with a three-second bound while
the old access token is usable. Native FCM auto-init stays disabled. Token revocation
also runs for session invalidation/account switching and cold starts; failed revocation
is retried before another token is registered. Offline logout cannot recall an already
accepted system notification. Foreground handling ignores another account's userId.
Tokens are never logged. On Android, inbox rows with supported entities open their
destination even when already read. NewShop opens the current published shop slug;
correction moderation reads the owned change and opens its shop card. Approved shop
submissions open their published shop when available; unpublished submissions open
own contributions. Roaster decisions open own roaster submissions, check-in decisions
open check-in history, and general messages retain read-only inbox behavior.

GET /api/v1/me/notifications/{id}/destination returns {kind,value}, 204 for no destination,
and 404 for a foreign/missing notification or unavailable published shop. The additive
Account endpoint must be deployed before this Android client. It preserves existing
rows without a migration. Resolution does not mark read; the client marks successful
opens separately. Foreground notifications have distinct PendingIntent identities;
background/terminated FCM clicks use the same notificationId/userId handoff. A failed
lookup shows an error and allows retry. iOS navigation remains unchanged.

iOS uses the public `Messaging.deleteData(completion:)` reset to revoke Messaging
registrations and check-in data. With auto-init disabled, Firebase does not request
a replacement token during this reset. It also handles an installation without
check-in credentials; token-only deletion in Firebase 12.19.2 returns Messaging
error 6 in that state and would block first registration (RESET_6).
The existing reset gate still retries real failures before APNs/FCM registration
can continue. Firebase installation IDs and CoffeePeek sessions/history are retained.
The Debug callback asserts that the SDK kept automatic registration disabled.
See the pinned [public reset contract](https://github.com/firebase/firebase-ios-sdk/blob/12.19.2/FirebaseMessaging/Sources/Public/FirebaseMessaging/FIRMessaging.h)
and [fresh-install reset implementation](https://github.com/firebase/firebase-ios-sdk/blob/12.19.2/FirebaseMessaging/Sources/Token/FIRMessagingTokenManager.m).

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

On 2026-10-20, Linux verification passed Direct debug APK assembly, 5 notification
tests and 353 application unit tests. Coverage includes defaults/account switching,
failed-default retry, permission request timing, current-account token filtering/logout,
permission revocation/activity throttling and shop/home check-in success/failure/edit
callbacks. The SVG and Android vector paths match. The Figma board was visually
checked at 16, 20, 24 and 32 px on light/dark backgrounds. No Android device was connected
for audible-delivery verification during this change. On macOS also build the Xcode app.

On real devices verify: foreground/background/terminated notification, permission denial
and re-enable, token renewal, logout/account switch, all categories enabled, moderation
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

## Inbox presentation follow-up — 2026-10-21

Original verification source: migration tip 813f0ab8 (feature/appupdate-audit)
plus this UI/lint patch. The develop integration is recorded below.
Scope: the shared feed bell and inbox UI/resources in composeApp, the existing
bell icon exposed through core/design-system, and the failing profile/settings
Scaffold padding. No Gradle boundaries or presenter/repository contracts change.
The app's shared resource set currently contains only default Russian strings.

Preserve generation-based sheet close, one accessible push toggle, permission/save
guards, read-one/read-all snapshots, retry and cursor pagination. The existing
platform modal and AnalyticsOverlay keep consent/session behavior; no analytics
events are added. A sibling badge avoids the circular button clip, retains the
full accessible count and visually caps at 99+. Non-filling list weight fits short
content; the existing 88% maximum and scrolling bound long lists. Empty history
omits the inactive read-all toolbar. Existing design-system components are reused.

CI diagnosis: Android CI run 39612753340 and Android Release run 39612738657 both
stopped at profile/impl's two unused Scaffold-padding errors. Applying its padding
to both root containers fixed them; the next full local pass exposed the same
error in settings/impl SettingsContent, also fixed at its root. Zero Scaffold
insets and explicit status-bar handling are preserved. No suppressions are added.

Verification on 2026-10-21:
- Final full pass: BUILD SUCCESSFUL in 5m 4s, --max-workers=6 --continue.
- assembleDirectSandbox, assembleDirectDebug, assemblePlayDebug passed, including
  all three packaged DEX runtime-class checks and Sandbox manifest isolation.
- testDebugUnitTest plus composeApp's Direct/Play unit tasks: 2,133 cases across
  405 suites; zero failures, errors or skips. The application has 646 cases per
  delivery variant. Notifications' 12 cases executed in the first attempt and
  were UP-TO-DATE in the final pass; no failed test cases or test retries.
- lintDebug and composeApp's lintDirectDebug/lintPlayDebug passed. Existing
  ProfileCard ModifierParameter and common Preview/compiler warnings remain.
- APK identity: 1.0.1010 (Sandbox: 1.0.1010-sandbox), code 1010; packages
  com.coffeepeek for Direct/Play Debug and com.coffeepeek.sandbox for Sandbox.
- Earlier local compilation failures (required loader accessibility description,
  cross-module nullable smart cast and unsupported common Preview fontScale)
  were corrected before the successful full pass. The third pass preserved
  evidence of the settings lint failure before the final successful retry.
- git diff --check passed. New/substantively changed UI functions meet the
  40-line composable / 20-line ordinary-function review limits.

Colocated light/dark previews compile with fake state for empty/short/long lists,
read/unread cards, push settings and counts 0/1/12/99+, including 2x button text.
They were not rendered: Android Studio tools were unavailable. No device was
selected or deployed to, and no automated UI/instrumented/screenshot test ran.
Manual modal sizing, light/dark rendering, font/window sizes, accessibility and
back/cancel checks on Sandbox remain open. Live push/backend acceptance and iOS
compilation/device checks were not performed. These results describe local checks.

### Develop integration — 2026-10-21

Only this 11-file patch was cherry-picked onto develop 7830b970 as e96a089d;
the remaining migration stack was excluded. The version 1.1 configuration and
the existing settings update behavior on develop are preserved.

The same full Gradle command was repeated against e96a089d with --max-workers=6
and --continue: BUILD SUCCESSFUL in 10m 14s. DirectSandbox, DirectDebug and
PlayDebug assemblies, their packaged DEX checks, Sandbox isolation, all module
and application unit tasks, lintDebug and both application Debug lint tasks
passed. This base has 2,099 cases in 401 suites, with zero failures/errors/skips.
The tested APKs identify as 1.1.1016 (Sandbox: 1.1.1016-sandbox), code 1016.
Only this verification note changed after that successful code validation.
The manual device/iOS gates above remain open.
