# Activity calendar Android home-screen widget

## Scope and verification plan — 2026-10-25

Branch: `codex/migration-activity-calendar-widget`, from migration tip `105b8994`
(the checkout's origin/develop). New Android launcher surface for the current
month, using the existing combined Shop/Home history and calendar photo variants.
No new Gradle module: domain coordination belongs to check-in-history/domain,
RemoteViews presentation to check-in-history/impl, and Android lifecycle,
account binding, jobs, image transport and root navigation to composeApp.

Preserve local-zone Shop instant bounds, Home inclusive civil dates, calendar
photo precedence, authentication/cancellation semantics and existing analytics.
The widget is read-only, owns no analytics emission, and opens the existing
VisitedPlaces destination. No existing screen or iOS path is replaced.

Use the platform AppWidget/RemoteViews and JobScheduler APIs, with no added
library dependency. Widget rendering cannot use Compose primitives; it reuses
the calendar grid and CoffeePeek design-system color tokens. Russian is the
existing history catalog's only supported locale. Do not claim other translations.

Verification: domain month aggregation, partial failures, photo selection and
cancellation tests; existing history/calendar contracts; Android Sandbox assembly
with packaged DEX checks. Add colocated light/dark content previews. Manual
launcher checks remain separate: widget picker/resize, real photos, light/dark,
large font, TalkBack, click navigation, refresh, month/timezone rollover, offline,
process restart, logout and account replacement. No automated UI tests.

Photos are deliberately visible on the user's launcher after adding the widget.
No widget photo files or account data are persisted by this feature. Full updates
replace launcher bitmaps; account changes clear the view before another read.
Background jobs are inexact and may be delayed by Android battery restrictions.

Implementation and verification results will be recorded below.

## Implemented behavior

- Resizable 4×4 home-screen widget for the current local month, Monday-first,
  one latest available calendar photo per active day; cutouts retain their full
  silhouette. Days without an available image keep an activity marker.
- Shared month-grid calculation and CoffeePeek text/accent tokens; launcher
  night-mode-aware text and light/night rounded backgrounds. The colocated
  `PreviewLightDark` uses synthetic bitmap data, no DI or network.
- Bounded 72×72 image payloads (at most 31 bitmaps, about 628 KiB), four concurrent
  downloads, six-second per-photo timeout and sixty-second job timeout. Images
  use the existing authenticated transport and never bypass the Sandbox gateway.
- Explicit loading, signed-out, empty, failed and partially loaded states.
  Missing pictures fall back to activity markers. No background writes.
- Hourly platform updates, manual refresh, size/timezone/clock/locale updates and
  refresh after leaving an Activity. Jobs own the process lifetime during reads.
  Android may delay these updates; a launcher snapshot can remain visible until
  the next update after remote-only changes. Local account changes clear it.
- Captured account lease and publication generation prevent older successful
  reads from restoring photos after sign-out/replacement or a newer refresh.
- Heading/day taps open the existing combined history through the app's root
  navigation, including cold/warm launches and the Sandbox entry Activity.
- No new Gradle dependency, module, analytics event, iOS integration or UI test.

## Verification — 2026-10-25

Source: migration tip `105b8994` plus this widget checkpoint on
`codex/migration-activity-calendar-widget`. Build identity: versionCode 2455,
Sandbox `1.1.1.2455-sandbox` / `com.coffeepeek.sandbox`; Debug `com.coffeepeek`.
All Gradle runs used `--max-workers=6` and ran sequentially.

- Executed: domain 8 tests; history impl 66 tests, including 12 new month/widget
  aggregation regressions; application `*di.checkinhistory.*` 64 contract/DI/
  calendar/account tests. Total **138**, zero failures/errors/skips.
- `:feature:check-in-history:impl:lintDebug`: zero errors; three existing
  ModifierParameter warnings in unchanged CheckInHistoryScreen code.
- `:composeApp:assembleDirectSandbox` and `:composeApp:assembleDirectDebug`
  succeeded, including both packaged DEX runtime-class checks and Sandbox
  manifest isolation. Merged Sandbox manifest contains the widget receiver/job,
  keeps SandboxActivity as launcher and has no INTERNET permission.
- Initial verification command requested nonexistent
  `:composeApp:testDirectSandboxUnitTest` and failed before running tests.
  Corrected to the existing `testDirectDebugUnitTest` task with only the isolated
  history contracts selected; this is JVM execution, not live-app testing.
- Initial lint found a missing explicit orientation in the dynamic weekday row.
  It was corrected; final lint/build rerun passed. New hard-coded-label and
  redundant SDK-check warnings were also corrected.
- Android Studio MCP was unavailable. Previews compile but were not rendered in
  the IDE. ADB inventory found **no connected devices**. No APK was installed,
  no UI/instrumentation tests ran, and no account/backend writes occurred.
- Pending manual/release gates: launcher picker/resize and visual checks with
  photos, TalkBack/large fonts, warm/cold navigation, clock/month rollover,
  background restrictions, offline behavior and local account changes. Real
  media/backend and iOS acceptance are unverified. The PR remains draft for
  this device review.

Android platform behavior reference:
[Widget updates and background execution](https://developer.android.com/develop/ui/views/appwidgets/advanced).

## Device installation and startup — 2026-10-25

User-requested retry detected an unlocked Pixel 9 (Android API 37, arm64-v8a).
Installed `directSandbox`, package `com.coffeepeek.sandbox`, versionCode 2456 /
`1.1.1.2456-sandbox`, built from `889d1805`. The prior no-device result above
remains the original verification record. No existing Sandbox package was found;
APK signature/ABI/entry were checked before non-destructive `adb install -r`.
The regular CoffeePeek package and its data were not modified.

Cold launch of SandboxActivity returned `Status: ok` (1019 ms); the process
remained running with no entries in its scoped crash buffer. Android registered
ActivityCalendarWidgetProvider. This confirms installation/startup/provider
registration only: adding the widget, photo rendering, resizing, theme/font/
accessibility checks and widget navigation/account scenarios remain unverified.
No automated UI/instrumentation test or live-backend action was performed.
