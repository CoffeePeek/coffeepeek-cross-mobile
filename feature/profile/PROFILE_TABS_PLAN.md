# Profile tabs and controls — 2026-11-05

## Profile check-in likes — 2026-11-19

The shared profile check-in card displayed a reaction count through a read-only
footer. It supplied no click action. The profile now sends like and unlike
actions through a narrow injected port backed by the existing Community writer.
The server's confirmed count and selection update the row and its profile
snapshot. Returning to the profile refreshes the check-in page. A pending vote
blocks another vote on that row, and a page reload waits for pending votes.
Account replacement cancels pending work and prevents late responses from
updating another session.

Public read-only checks found nonzero counts in both the general and author
feed on 2026-11-19. No live reaction was submitted. The new regression checks
cover reading nonzero counts, like/unlike, failed and duplicate votes, cached
route recreation, refresh during a vote, return to the profile and session
replacement. Android app assembly, the shared iOS module compilation, RU/BE
resource parity and the function body check are part of verification.
Manual Sandbox interaction and full iOS application/device checks remain open.

The profile uses the existing request analytics and signals the Community feed
to refresh after a confirmed vote. It adds no analytics service or feed-event
emission from a profile card. Preview states use fake data with three likes.

Verified on Windows from `b13879713` with this patch. Executed JVM tests
passed with no failures, errors or skips. Profile impl passed 83, Profile data
43, Community data 24, Community impl 56, design system 81 and Direct Debug app
676, for 963 total. These include 11 new reaction regressions with fake ports
and HTTP responses. The final timeout/retry regression also passed.

Both Direct Debug and Sandbox assembled with their packaged runtime-class
checks, and Sandbox isolation passed. The builds are `1.2.1.3158-debug` and
`1.2.1.3158-sandbox`, each with 36 DEX files. Profile impl and its tests compiled
for iOS ARM64. RU/BE resource keys match. The function body baseline was
regenerated with only reductions and passed verification. A combined baseline
generation/verification invocation first failed Gradle's implicit output
dependency validation. Running them sequentially passed. Existing Preview
deprecation warnings remain.

No app was installed or launched. Previews compiled but were not rendered in
an IDE. Full Xcode linking and manual checks on Android/iOS remain open.

Source: `codex/migration-shelf-light-timber`, based on `origin/develop`
`43c9969ce`, continuing the uncommitted shelf reference changes. The user
requested parallel subagents for this follow-up and confirmed the profile
menu belongs in the upper-right corner.

## Scope and ownership

- Shelf presentation: remove Add, align the existing archive button with the
  Coffee shelf title, and retain the summary/drag hint below. Keep the wooden
  board, texture, ordering, archive permission and existing callbacks.
- Profile presentation: show underlined tabs ordered Check-ins, Collections,
  Shelf below the profile and weekly activity, with selection retained for the
  current profile. Render existing content/slots only for the selected section.
  Keep owner/private, public/featured and blocked-profile visibility rules.
- Keep the weekly activity preview; tapping it invokes the same full-screen
  history callback as All check-ins. Remove the activity-sheet entry point and
  the weekly card's outer border in loaded/loading states.
- Move profile editing to the existing upper-right three-dot menu. Preserve
  other actions and destructive confirmation. Avatar taps open the photo.
  Apply the same controls to the owner's fallback without a public address;
  never fetch a shelf for a blank public slug.

Existing migrated profile entries are wired by Android and iOS composition;
legacy screen files remain present but are not the active root entry. Features
remain independent: Profile uses its application-supplied collections/shelf
slots and does not import another feature's domain/data/impl. No new module,
dependency, database or network contract is planned. Any reusable underlined
control belongs to the existing design-system, with feature labels in Profile.

The existing collections callback owns `favorites_opened {entry_point: profile}`;
preserve it. Root routes own screen analytics; the weekly preview and menu reuse
their existing navigation callbacks. No new analytics service/events or live
analytics requests are introduced. Session/account, logout and privacy guards
remain owned by the existing models/composition.

## Verification plan

Before acceptance, compile the Android application in `directSandbox`, require
packaged DEX class and isolation checks, and execute existing Profile, Shelf,
design-system and relevant application profile/cache/DI unit regressions.
Compile changed shared presentation for iOS Arm64. Check RU/BE resource parity,
imports/dependency boundaries, function review limits and paired colocated
light/dark previews. No automated UI/instrumented/screenshot tests or test hosts
will be created or run.

Studio MCP is unavailable. No device or app launch was selected. IDE rendering,
manual light/dark, font/window sizes, underline/scroll placement, accessibility,
tab/Back/cancel and photo/menu review remain separate gates. No live account,
favorites, consent, logout or persistence writes are authorized for testing.

## Results

Verified on 2026-11-05 from `43c9969ce` plus the current uncommitted patch:

| Executed JVM unit results | Suites | Tests | Failures / errors / skips |
|---|---:|---:|---:|
| Design-system `testDebugUnitTest` | 16 | 57 | 0 / 0 / 0 |
| Profile impl `testDebugUnitTest` | 6 | 66 | 0 / 0 / 0 |
| Shelf impl `testDebugUnitTest` | 11 | 85 | 0 / 0 / 0 |
| App profile/cache/DI `testDirectDebugUnitTest` | 11 | 58 | 0 / 0 / 0 |
| Total | 44 | 266 | 0 / 0 / 0 |

The selected app suites cover profile account/session ports, sign-out fakes,
public profile loading/visibility/history, weekly calculations and personal-week
cache invalidation. These are local unit tests with fake collaborators, not
live sign-out or real account acceptance. No test failure or test retry occurred.

The first invocation failed during task selection because
`:composeApp:testDirectSandboxUnitTest` does not exist. No source compilation or
tests ran in that attempt. After checking the available tasks, the corrected
single invocation used the existing Direct Debug JVM test task and assembled
only `directSandbox`; it passed in 2m 3s with six workers. No Debug app was
installed or launched. The original failed task-selection attempt is retained
here rather than counted as a passing test retry.

- Android application assembly, `verifyDirectSandboxRuntimeClasses` and
  `verifyDirectSandboxIsolation` passed. Sandbox package/version:
  `com.coffeepeek.sandbox`, `1.1.7.2721-sandbox` / 2721.
- APK SHA-256:
  `25ddb29ae8bf18f4c70ff84aac60b4c78bc7d1b06bbd4fd317068a38e75451f4`.
- Design-system, Profile impl and Shelf impl shared Kotlin compiled for iOS
  Arm64. Native Swift/iOS linking, executable tests and device behavior remain
  unverified on this Windows host.
- RU/BE resource key parity, dependency/import boundaries and `git diff --check`
  passed. No new cross-feature implementation dependency or public API change.
  New/changed functions were reviewed against the 40/20 body-line guidance;
  paired previews compile with fake state and without DI/network access.
- Existing Gradle/KMP and Preview annotation deprecation warnings remain. No
  IDE diagnostics or preview rendering ran. Three subagents implemented the
  independent slices, and a read-only integration review found no concrete
  callback/session/privacy issue in the owner fallback and menu wiring.

No device was selected, installed or launched. Manual light/dark, large fonts,
narrow windows, tab/underline placement, accessibility, profile variants,
activity/full-history, photo/menu, back/cancel and native iOS acceptance remain
open. No automated UI/instrumented/screenshot tests were created or run.

Before the user-approved develop/main publication, the profile Shelf tab was
extended to all returned bags in rows of three. The final follow-up executed
the existing 85 Shelf tests again and passed Android/iOS shared compilation,
Sandbox assembly and DEX/isolation checks. See `../shelf/LIGHT_TIMBER_PLAN.md`
for the final artifact identity; the 266-result profile integration above was
completed before that scoped shelf layout correction.

## Editable owner shelf — 2026-11-10

The personal navigation now opens the editable shelf in the owner profile.
The shelf retains scanning, drag and drop, archive and undo. Saving a bag
opens the Shelf section. Its visibility setting lives on the shelf and the
standalone Coffee tab has been removed.

Verified on Windows against `c8ffe801` with source tree `e073246b`.
Both APKs passed packaged DEX verification and Sandbox passed isolation.
The function body check reported zero new violations. Direct JUnit passed
72 profile tests, 85 shelf tests and 18 app tests across 23 suites.
The first build stalled after Sandbox compilation. An alternate JVM
invocation could not connect to a Gradle daemon. The in-process retry passed.

| Variant | Package | Local build | APK SHA-256 |
|---|---|---|---|
| `directSandbox` | `com.coffeepeek.sandbox` | `1.1.7.2895-sandbox` | `481b253adbd5c684a3e376db51592251b9c766d6531cd94965ab51ffd6680ef4` |
| `directDebug` | `com.coffeepeek` | `1.1.7.2895` | `8c458da9326060ec3d378e0762d5c10bc9e6b34a5c7a1581abdbf1c80e946df1` |

No device was used. Manual light and dark checks of profile tabs, scan
return, dragging, archive undo and visibility remain open, as does the
native iOS build.

## Profile calendar and feed scrolling — 2026-11-13

The profile header moves between the ordinary scroll container and the
editable shelf with its existing composition. Switching tabs preserves the
week calendar and its lifecycle observer. The Community feed uses the list's
scroll direction to collapse and reveal the header with its notification button.

Verified on Windows from `a8e9ffa60` plus the local changes. Both Android
variants assembled and passed packaged DEX verification. Sandbox isolation and
the function body limits check passed.

| JVM unit suite | Suites | Tests | Failures / errors / skips |
|---|---:|---:|---:|
| Profile impl | 7 | 72 | 0 / 0 / 0 |
| Check-in history impl | 8 | 82 | 0 / 0 / 0 |
| Community impl | 4 | 51 | 0 / 0 / 0 |

Manual checks used `directSandbox` (`com.coffeepeek.sandbox`,
`1.1.10.2936-sandbox`) on the API 37 Pixel 8 AVD (`emulator-5554`).
The calendar retained seven accessible days after repeated tab changes, including
on the narrow shelf. Opening history and pressing Back returned to the selected
shelf. The feed title and notification button left the accessibility tree when
collapsed and returned on reverse scrolling before reaching the top. The checks
covered light and dark themes, plus a 320 dp window with 1.3× text. The first
dark reverse gesture was inconclusive. A longer reverse gesture confirmed the
header had returned while the second post was still at the top of the viewport.

The first Gradle invocation could not write the wrapper lock in the sandbox.
The next invocation reached and passed the Android build and test tasks. Common
metadata compilation stopped in the existing iOS design-system source with
`Unresolved reference 'animateWithDuration'` in `CoffeePeekPlatformSegments.ios.kt`.
Native iOS behavior and live-backend checks remain open.

## Profile check-in filters and card parity — 2026-11-16

Issue #362 adds the type and calendar-day filters to the profile tab. The
profile uses the feed's gallery and shelf bag row, including the public bag
photo and product link. The initial implementation kept a personal history
link. The check-in request exposes retry after a 15-second timeout, and the
loader stops offering more pages when a cursor repeats.

Android verification used `8b559dd86` plus the task changes on
`codex/profile-checkins-362`. The `directSandbox` package
`com.coffeepeek.sandbox` (`1.2.0.3035-sandbox`) passed assembly,
isolation, packaged DEX and function body checks. Early builds found stale
imports, an oversized action handler and a missing layout import. These were
corrected before the successful builds.
The final APK passed signature verification and launched the profile tab.
APK SHA-256 `50b4781f5130b605b4e2e485bb21de0a80d6c401b68a6304c2d8e4d94aa9917c`.

| JVM unit tests | Suites | Tests | Failures / errors / skips |
|---|---:|---:|---:|
| Profile impl | 7 | 74 | 0 / 0 / 0 |
| Profile data | 7 | 43 | 0 / 0 / 0 |
| Community impl | 4 | 55 | 0 / 0 / 0 |
| Community data | 3 | 24 | 0 / 0 / 0 |

Manual Sandbox checks used an isolated Pixel 10a AVD running Android API 37
(`CoffeePeekProfile362`, `emulator-5564`). The `Paged` fixture confirmed month
expansion in light and dark modes, selection of an active day, the home filter,
and a paged home check-in with its photo and a working coffee detail link. The
`PageFailsOnce` fixture kept existing cards visible, exposed a retry, and loaded
the second page after retry. The verified Android behavior used fake data.
Native iOS, live backend and physical-device checks remain open.

### Calendar and filter follow-up — 2026-11-16

The profile animates between the original week and a compact month. The
owner's calendar uses the existing session-scoped date counts and thumbnails.
Other profiles use photos from their loaded public check-ins. Selecting a date
filters the tab in the same time zone as its calendar. The type filter is a
dropdown and the profile's navigation to personal history was removed.

The Sandbox protected-photo fixture now sends matching revalidation headers
for both image data and a conditional response. The first manual run showed
the photo placeholder in the public calendar and its corresponding card. The
revised fixture rendered the actual image in the public month grid.

Verified on `8b559dd86` plus local changes with the same isolated Pixel 10a
API 37 AVD. Manual checks covered expanding and collapsing the calendar,
owner and public date thumbnails in light and dark themes, retaining the date
while selecting a type from the dropdown, and the absence of the personal
history link. The initial build identified a stale preview callback; the
corrected `directSandbox` build passed isolation, packaged DEX and function
body checks. JVM suites passed 74 Profile and 82 Check-In History tests
across 15 suites with zero failures, errors or skips. Final signed test APK
`1.2.0.3035-sandbox` SHA-256
`7de4932bb528fa945b4fc0ba0d46143fa92fa4dc6a4a31a2541df2490d294e02`.
Native iOS and the live backend remain unverified.

### Calendar in the shared profile header — 2026-11-16

The calendar now appears below the bio and above the profile tabs. It opens
expanded by default, and tapping the month title toggles the grid. The
selected month and day are retained when switching tabs or collapsing the
grid. Selecting a day opens the Check-ins tab with the date applied. The type
filter was removed. The owner profile without a public handle retains a link
to its private history, which opens the full history.

On `8b559dd86` with the local profile changes, the function-body check and
both Android assemblies passed (`directSandbox` and `directDebug`, version
`1.2.0.3035`). The Sandbox isolation and packaged runtime-class checks passed.
The JVM suites ran 74 Profile and 82 Check-In History tests across 15 suites
with no failures or skips. The first Kotlin/Native compile found Android-only
preview imports. After switching the affected files to the multiplatform
annotation, the Profile and Check-In History `compileKotlinIosArm64` tasks
passed. This revision still needs manual UI review on a device. No full iOS
app, device, or live backend run was performed.

### Current week in the collapsed calendar — 2026-11-16

The calendar below the bio starts with the current Monday–Sunday week. The
month title toggles the full grid without a chevron, and the open grid keeps
plain previous and next actions. It retains its selected month and date when
collapsed. Tapping the selected date again clears the filter. The profile no
longer shows a separate reset label or a spinner under the month grid.

On `8b559dd86` with these local changes, the function-body check and Android
`directSandbox` and `directDebug` assemblies passed. Both `1.2.0.3035` APKs
passed packaged runtime-class checks, and Sandbox passed isolation. The JVM
suites ran 75 Profile and 83 Check-In History tests, with zero failures,
errors or skips. Both affected Kotlin iOS Arm64 compile tasks passed. Device
interaction, the full iOS app and live backend behavior remain unverified.

### Calendar header controls — 2026-11-16

The expanded profile calendar puts the previous and next arrows around the
month title. The separate expand/collapse arrow stays at the end of the
header. The title still toggles the calendar on touch. The standalone history
calendar keeps its existing controls.

On `26d6e5498` with this patch, the function-body check and Android
`directSandbox` and `directDebug` builds passed, including packaged runtime
classes and Sandbox isolation. Profile and Check-In History compiled for
Kotlin iOS Arm64. Added light/dark previews for both header states at a narrow
width. The previews were not rendered, and no device or full iOS app was run.

### Calendar header motion — 2026-11-16

The month title fades and interpolates its size when the displayed month
changes. The side arrows fade and expand into position, and the disclosure
chevron rotates in place. The calendar card and header share a 220 ms timing
that follows the existing iOS reduced-motion setting. The owner calendar
keeps the header mounted when switching week and month data sources.

On `cc02e1f6a` with this patch, the function-body check and both Android
assemblies passed, including packaged runtime classes and Sandbox isolation.
Profile and Check-In History compiled for Kotlin iOS Arm64. The animation was
reviewed in source. Device and full iOS app behavior remain unverified.

### Date filtering and scroll position — 2026-11-16

Picking a day updates the date filter without explicitly scrolling the
Check-ins list to the calendar. Selecting a date from another tab still opens
Check-ins. The active date can still be cleared by tapping it again.

On `c054e4081` with this patch, the function-body check and both Android
assemblies passed, including packaged runtime classes and Sandbox isolation.
Profile compiled for Kotlin iOS Arm64. The scroll behavior was reviewed in
source and remains unverified on a device.

### Own profile display name (#421) — 2026-11-26

The native owner header now uses the feature-owned presentation name: the
current own profile's nonblank userName, then the public nickname, then the
canonical profile slug. A stale public nickname cannot override the current
own name. No email or authentication identifier is used. Inactive or missing
owner identities expose no presentation name. The native bridge only maps
this name; existing header @ styling, profile URLs and check-in author data
retain their responsibilities.

Verification scope: seven common presentation policy regressions cover names
that differ from slugs, stale/public/blank fallbacks and missing identity.
Native snapshot and account-session assertions cover replacement and closure.
The paired native header previews now include a display name with a space.
The implementation is based on integration parent 43dfcc43f0b71b42223a0d8f1033e7f1d3612f14.
On that parent with this patch, Profile impl ran 110 JVM tests across ten suites
with zero failures, errors or skips, including all seven new name cases. The
function-body check and directSandbox assembly passed on the first run, including
Sandbox isolation and packaged DEX runtime-class checks (build 1.2.2.3385-sandbox,
com.coffeepeek.sandbox, APK SHA-256
b6af7d9b0f3f6f6f754a20742296cf7dbade69ce658cc77e3f0a85ac3cf8dcfd).
The native snapshot/session test changes have not been compiled or executed;
Kotlin/Native and full iOS validation remain pending. Previews were not rendered.
No device, real account, backend write or TestFlight validation was performed.

### Android profile parity with native iOS — 2026-11-26

User-requested checkpoint `codex/migration-android-profile-parity` starts from
`02fde81e362a10860f12f7a54277c7556c55b3ef` in develop. This is presentation
parity for the existing owner profile, not a new feature or navigation migration.
The native reference is `OwnProfileScreen.swift` and `ShelfNativeGridScreen.swift`.

Verification plan before edits: preserve profile tab/calendar scroll ownership,
collection cover caching and editor return/cancel behavior, shelf drag/finish/undo,
archive search and account/session guards. Collections owns its square two-column
cards; Profile owns the single floating full-library entry above navigation;
Shelf owns its floating settings control and searchable archive modal. Application
composition remains wiring and supplies the existing callbacks and nav clearance.
No new Gradle boundary or cross-feature implementation dependency is needed.

Run the existing Shelf, Collections and Profile unit/contract suites, compile the
application through directSandbox assembly, and require packaged runtime classes,
Sandbox isolation and function-body checks. Check resource-key parity and paired
colocated previews. IDE tools are unavailable in this session. Previews may be
compiled but must not be reported as rendered. No automated UI/instrumented tests,
device install/launch, live-account write or production analytics is authorized by
this verification plan. Manual themes, narrow/large-text layouts, accessibility,
modal placement/IME, tab scroll and back/cancel remain device acceptance gates.
The already running iOS TestFlight 2.0 (3398) uses the prior source and is unaffected.

Verification on the Windows/JDK 17 host, base 02fde81 plus this patch:
Collections impl 7 suites/110 cases, Profile impl 10 suites/110 cases and Shelf
impl 11 suites/89 cases, 309 total, zero failures/errors/skips. Collections/Profile
ran after their final edits. The final Shelf rerun followed the modal first-event
guard; no failed test or build was retried. Affected Android lint reports contain
zero issues. Function limits passed (125 recorded, zero new), with no baseline
extension or Gradle/dependency changes. RU/BE resource keys match in all three
features and composition. Colocated fake light/dark previews compile.

Both initial integration and final-guard directSandbox assemblies passed,
including packaged DEX runtime-class and Sandbox isolation checks. The local APK
is com.coffeepeek.sandbox, 1.2.2.3398-sandbox / 3398 (source base plus working patch),
SHA-256: 2ed4033053cb5ec4b7637f87e970550571045691c7697e75436d7aa10e3f948d.

No preview was rendered and no device was installed or launched. Studio tools
were unavailable; the read-only ADB inventory could not connect to its daemon,
so no manual/device acceptance is implied. The manual gates listed above remain
open, as does compilation of this new common UI on a macOS/native runner.
Existing Preview and expect/actual compiler deprecation warnings remain; affected
lint reports are clean. Analytics inventory found the existing favoritesOpened
callback and no Shelf event producer in these changed presentation paths; the
new single entry reuses that callback without emitting new production events.
