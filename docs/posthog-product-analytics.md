# PostHog product analytics

The app uses the official [PostHog Kotlin Multiplatform SDK](https://posthog.com/docs/libraries/kmp) for Android and iOS. It sends events to the EU project at `https://eu.i.posthog.com`. No project token is committed; without a valid public `phc_` project token, analytics stays disabled and the settings switch is unavailable.

## Configure builds

Android: set `POSTHOG_PROJECT_TOKEN` in the environment or in ignored root `local.properties`:

```properties
POSTHOG_PROJECT_TOKEN=phc_your_project_token
```

iOS: copy `iosApp/Configuration/Local.xcconfig.example` to the ignored `Local.xcconfig` and set `POSTHOG_PROJECT_TOKEN` there. CI can provide the same Xcode build setting. Use the same EU project token as the web app and backend.

The KMP SDK uses SwiftPM to link its native iOS dependency. On a Mac, after syncing Gradle, run from the repository root:

```sh
XCODEPROJ_PATH="$PWD/iosApp/iosApp.xcodeproj" ./gradlew :composeApp:integrateLinkagePackage
```

Commit the generated linkage package and updated Xcode project, then build the iOS app. This step is required before the iOS integration is complete. See [Kotlin's SwiftPM integration guide](https://kotlinlang.org/docs/multiplatform/multiplatform-spm-import.html).

## Consent, identity, and events

Analytics is opted out by default. Users can enable or withdraw consent under **Настройки → Другие настройки → Помогать улучшать CoffeePeek**. The decision is stored locally apart from the SDK state. The app calls `identify` only after consent and an active authenticated session, using the lowercase CoffeePeek user UUID. Logout and account switching reset the SDK identity. Raw search text, coordinates, names, email addresses, and tokens are never sent.

Initial explicit events:

| Event | Properties | Trigger |
| --- | --- | --- |
| `discovery_opened` | `surface`, `platform` | Shop or coffee discovery screen resumes |
| `search_submitted` | `kind`, `has_query`, `filter_keys`, `platform` | Search IME action |
| `shop_opened` | `shop_id`, `source`, `platform` | Loaded shop details are shown |
| `coffee_opened` | `coffee_slug`, `source`, `platform` | Loaded coffee details are shown |

`platform` is `android` or `ios`; `source` is currently `unspecified` until navigation origin is tracked. `coffee_slug` is the public slug at view time; the mobile model has no stable coffee ID. Automatic capture, lifecycle capture, replay, feature flag preloading, and error capture are disabled.

The backend owns the event names `favorite_added` and `check_in_published`. Do not emit them from the app: a favorite retry is not a new save, and a submitted check-in is not yet published. Production backend capture remains disabled (`PostHog__Enabled=false`) until account-level analytics consent can be checked there, so these write events are not yet available for funnels. `filter_applied` and `discovery_empty_results` remain future client events.

## Verification

1. With no token or with consent off, navigate through discovery, search, and details: no events should arrive.
2. Enable consent, submit a shop and coffee search with filters, and open one shop and one coffee detail. Verify one event for each intentional action and inspect property keys in PostHog.
3. Log in, verify the distinct ID matches the backend user UUID; log out or switch accounts and verify the next events do not use the previous user's ID.
4. Withdraw consent and repeat navigation: no new events should arrive.
5. Build both Android flavors and the iOS app after SwiftPM linkage is generated.
