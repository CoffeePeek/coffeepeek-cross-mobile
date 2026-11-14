# Profile tabs and controls — 2026-11-05

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
