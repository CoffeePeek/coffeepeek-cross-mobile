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

## Responsive calendar correction — 2026-10-25

The user's device review found the fixed 4×4 month too large. Replace the default
with a 4×1 week (launcher cell sizing varies), resizable horizontally and vertically
from 180×80 dp. Responsive RemoteViews select one week below 160 dp, the previous
and current weeks from 160 dp, and the current month from 250 dp. Compact layouts
hide the brand, manual-refresh button and successful-state footer to reserve space
for photos; opening history and scheduled/return refresh remain available.

Verification plan: preserve account/cancellation and photo semantics; test complete
Monday-first weeks at month/year boundaries, leap years, visible-date photo loading,
partial errors per visible month, and zone-specific month query bounds. Compile
all three colocated light/dark previews, run history/app contracts and lint, build
Sandbox/Debug with DEX checks, then update the agreed Pixel 9 non-destructively if
connected/unlocked. No automated UI/instrumentation or screenshot tests.

All three size variants are delivered together, allowing immediate launcher resize
without network reads or clearing photos. Query preceding-month data only where
needed for the visible fortnight, and load thumbnails only for dates that can be
shown. Multiple widget instances can independently display different periods.

Responsive verification: **146 executed JVM tests** (77 history impl, 69 app
history contracts), zero failures/errors/skips; includes 11 new period/calendar
cases and 5 new multi-month loader cases. Domain code was unchanged in this step.
History lint passes with the same three pre-existing ModifierParameter warnings.
Both Direct Sandbox and Direct Debug APKs and their DEX checks pass; Sandbox
isolation passes. Final Sandbox artifact is `1.1.1.2457-sandbox` (2457), built
from `f3dee600` plus this responsive-layout change. Three light/dark previews
compile; Android Studio rendering is unavailable.

ADB initially detected the previously agreed Pixel 9, then it disconnected before
lock-state/widget-size inspection. Subsequent inventories were empty. Therefore
the responsive APK was not installed and actual launcher resize, photos, themes
and font/accessibility behavior remain manual gates. The earlier installed
month-only build is not evidence for these new layouts. No device settings were
changed, and no UI/instrumentation tests or new test hosts were introduced.

## Responsive APK installed — 2026-10-25

On the user's explicit installation request, the agreed Pixel 9 reconnected and
was unlocked. Verified the installed Sandbox 2456 APK and new 2457 APK have the
same signing certificate; updated in place with `adb install -r` successfully.
Installed package: `com.coffeepeek.sandbox`, `1.1.1.2457-sandbox`, responsive
implementation committed as `0fe7935e` (artifact built before that commit from
`f3dee600` plus its exact source changes). The regular app/data were untouched.
SandboxActivity returned `Status: ok` / HOT and the process remained running;
Android reports the widget provider and an existing widget instance. This update
confirms delivery and launch, not a manual resize/visual/accessibility pass.

## Shared calendar appearance — 2026-10-25

User confirmed that both the launcher widget and the profile's compact calendar
should match the large calendar: centered date numbers, gold today, photos replacing
numbers, no week/fortnight title, and a compact month counter such as `2/31`.

Verification plan: extract a reusable design-system day primitive and pure style
policy; apply it to the profile week, profile month and migrated history day, and
use the same policy in RemoteViews. Preserve selection, future-date muting, photo
count/visit decorations, descriptions, data loading and existing navigation/
analytics ownership. Widget progress counts distinct active dates in the current
month, not entries or only the displayed week; failures retain explicit states.
Run design-system style tests, history month/progress regressions, application
calendar/profile/history contracts, relevant lint and both APK/DEX builds. Use
paired colocated previews; manual device checks stay separate from JVM evidence.
The shared profile/month Compose adapters also have shared/iOS consumers; no
native iOS boundary or writer changes, but their shared visuals follow the same
primitive. Native visual verification remains a separate gate.

Final shared-appearance verification: **219 executed JVM tests** (49 design-system,
80 history impl, 90 application history/calendar/public-profile contracts), zero
failures/errors/skips. Eight new regressions cover shared date/photo/today/selection
style and month progress across compact modes, month boundaries and leap years.
Both lint tasks pass: design-system has no issues; history retains the same three
pre-existing ModifierParameter warnings in unchanged screen signatures. Shared
preview imports use the current multiplatform annotation; paired previews compile
but IDE rendering remains unavailable.

Direct Sandbox and Direct Debug assembly, both packaged DEX runtime-class checks
and Sandbox isolation pass. Artifact: `com.coffeepeek.sandbox`, versionCode 2459 /
`1.1.1.2459-sandbox`, built from `83a5ad3a` plus this shared-calendar change.
All Gradle commands ran sequentially with `--max-workers=6`. No new dependencies,
source sets, navigation routes, analytics events or production data flows.

ADB inventories during this change and after APK assembly found no connected
phones. This build was **not installed**; the last confirmed Pixel 9 installation
is still 2457. Manual profile/month/widget photo rendering, week/fortnight/month
resize, light/dark themes, large font/window sizes, TalkBack and navigation checks
remain pending. No automated UI/instrumentation/screenshot tests or test hosts
were created or run, and no live-account/backend actions occurred.

## Requested regular Debug installation — 2026-10-25

At the user's explicit request, assembled `directDebug` from `72fb027c`:
`com.coffeepeek`, versionCode 2460 / `1.1.1.2460`. Assembly and the packaged DEX
runtime-class check passed with `--max-workers=6`. On the agreed unlocked Pixel 9
(arm64), Package Manager confirmed that the ordinary package was absent and only
Sandbox was installed. APK signature, ABI and MainActivity were verified before
`adb install -r`, which succeeded. Post-install Package Manager confirms version
2460 and `com.coffeepeek.admin.MainActivity`; Sandbox remains installed separately.
No uninstall, data clearing or downgrade occurred. This records installation only:
the live app was not launched, no account/backend actions were performed, and the
manual visual/resize/accessibility gates above remain pending.
