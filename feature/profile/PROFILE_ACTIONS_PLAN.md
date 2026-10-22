# Profile actions follow-up — 2026-10-22

Source: `codex/profile-actions-sheet`, based on `b2868ad0` / parent PR #160.
That parent was squash-merged into develop as `2b2501ba` while this work was in
progress. The two parent trees are identical; this child is rebased onto develop
and its PR targets develop with only the profile/settings follow-up.
This is a profile/settings presentation correction requested after screenshots
of the Settings row and the oversized oval profile menu. It overrides the older
inventory's placement of blocked authors in Settings; the account guard remains.

## Scope and behavior

- Move blocked authors into the signed-in owner's profile actions on Android and
  shared/iOS. Remove the Settings row, unused callback/state and its old labels.
  The existing `BlockedAuthors` root destination and analytics page stay unchanged.
- Android's migrated profile menu uses an explicit 16dp corner radius, neutral
  surface/border and width bounded at 304dp, with separators between every row.
  It no longer inherits the application theme's 50% button shape. The shared/iOS
  menu retains its anchored platform implementation and marks sign-out destructive.
- Keep contribution actions and their root navigation. Logout's label and icon
  are destructive red. Close the menu before opening confirmation or navigating.
- Replace profile alerts with an action sheet matching author blocking. The
  Android design-system `CoffeePeekDestructiveConfirmationSheet` has a rounded
  title/message card with a divided red text action and a separate cancel card.
  Its transparent Material modal host animates from below, applies safe insets,
  owns focus/Back/scrim/swipe dismissal and lets the cards scroll for large text.
  There is no grabber or filled destructive button. The shared/iOS profile uses
  the same existing native action-sheet presenter as author blocking.
- Confirmation closes before calling the existing logout action. Cancel/dismiss
  only close the sheet. Account cleanup order, failure handling, drafts, favorites,
  OAuth sign-out and ViewModels are unchanged. No real account operation is tested.

The common app and Profile Android resources currently have only default Russian
strings; no additional locale is claimed. Changed runtime labels use resources.
Paired previews remain beside their components; confirmation content is previewed
without opening a modal or resolving DI. No new module/dependency or UI test host.

## Verification plan

- Execute core, Profile impl and Settings impl unit tests plus application
  Profile/Settings unit regressions, including the existing logout-order tests.
- Assemble `directDebug` and require the packaged DEX runtime-class check.
- Compile app KLIBs for iOS device and simulator. Executable linking and iOS
  presentation require macOS/device verification.
- Check resource XML, caller/API consistency and the final diff. Studio MCP is
  unavailable. No automated UI, Compose instrumentation or screenshot tests run.
  Two pre-existing instrumented-test callers receive the new callback argument
  solely to preserve source compatibility; no test cases or assertions are added.

The initial checks below did not install or launch an app. The later requested
Sandbox visual review is recorded separately; it does not certify real logout.

## Local results — 2026-10-22

Windows, the source above plus this patch; no device, installation or account
operation. All **147 unit results** pass with zero failures/errors/skips: core
32, Profile impl 48, Settings impl 17 and application Profile/Settings 50. Existing
logout regressions cover reset-before-sign-out, cleanup order and server failure.
The first complete check passed in 2m57s; the final run after adding scrolling
passed in 55s. There were no failed-test retries. Both iOS app target KLIBs compiled;
Native tests/executables were not run or linked on Windows.

Android `assembleDirectDebug` and packaged DEX checks pass. Resource XML validates
with unique names, obsolete Settings callbacks/resources have no remaining source
references, and `git diff --check` passes. All dependency and public callback
changes stay within the existing application/Profile/Settings boundaries.
Paired previews compile, including Android menu content and common confirmation
content; rendering was not inspected. Gradle/KMP, interop and the existing project
preview annotation's deprecation warnings remain; no IDE diagnostics are claimed.

## Initial requested Sandbox visual review — 2026-10-22

This records the first design, superseded by the user's reference correction
below; these screenshots do not certify the final design.

Source `a0d5deef181c72dcbc88cf57a8e7f4235bb4bc4e`, `directSandbox`, package
`com.coffeepeek.sandbox`, version `1.1.1088-sandbox` / code `1088`. The APK SHA-256
is `bf935a229b1e8c0cdd364bbd7147d0c8d2407fd928525bf3ec757e0c1b30699a`.
Assembly, Sandbox manifest isolation and packaged DEX runtime-class checks passed
in 2m08s, with no failed-build retry. No additional unit suite was run for this
visual-only follow-up; the 147 passing unit results above remain applicable.

No physical device was connected. Manual review used the existing Pixel 8 AVD,
explicit serial `emulator-5554`, API 37 / x86_64, 1080x2400 at 420dpi, font scale
1.0. It was launched with a temporary read-only disk and no snapshot saving.
The screen was awake/unlocked and Sandbox was absent before installation; the
installed package/version and launcher were verified. Production app/data were
not opened or replaced. Sandbox has no INTERNET permission or analytics startup.

Menu and confirmation screenshots were inspected in light and dark themes: menu
labels fit, blocked authors appears between dividers, logout icon/text and the
confirmation button are red, and the sheet occupies the bottom with readable
content above the navigation inset. Light-theme Cancel and dark-theme system Back
both returned to the same signed-in local profile. The new blocked-authors entry
opened the existing empty blocking screen without changing any block or follow.
Screenshots, UI dumps and build diagnostics remain outside tracked sources.
No automated UI/instrumented/screenshot tests were created or executed.

Not checked: physical devices/iOS presentation, animation smoothness, larger
fonts or alternate window sizes, TalkBack, outside/swipe dismissal, and confirming
logout. No real account actions or backend acceptance are claimed.

## Reference correction — 2026-10-22

The user supplied the author-blocking confirmation and divided profile menu as
visual references after seeing the first Sandbox screenshots. Reach: the common
design-system confirmation and its existing Android profile caller; shared/iOS
profile presentation adapter; migrated Android profile menu; colocated paired
previews and this plan/design-system documentation. No account/session/favorites
behavior, analytics, routes, resource labels or dependencies change.

Before-check plan: execute design-system/Profile unit suites and app
Profile/Settings regressions; assemble directDebug and directSandbox with their
packaged DEX/isolation checks; compile both iOS KLIB targets. Manually inspect
the new light/dark menu and confirmation in explicitly selected directSandbox
on Pixel 8, checking Cancel/Back without confirming logout. Keep diagnostics and
screenshots outside tracked sources. No automated UI tests or live app actions.

Results: this reference-correction patch on `f72042a7` passed all **130 executed
unit results**, zero failures/errors/skips: design-system 32, Profile impl 48 and
application Profile/Settings 50. `assembleDirectDebug`, `assembleDirectSandbox`,
both packaged DEX checks, Sandbox manifest isolation, `compileKotlinIosArm64` and
`compileKotlinIosSimulatorArm64` passed in one 3m20s run, without failed-test/build
retries. No dependency or source-set changes. Paired previews compile; there was
no IDE renderer/diagnostic access, and existing KMP/preview deprecation warnings
remain. No automated UI tests were added or run.

Manual review: Pixel 8 AVD `emulator-5554`, API 37/x86_64, 1080x2400/420dpi,
font 1.0, temporary read-only disk/no snapshot save. No physical device connected.
The emulator was awake/unlocked; Sandbox was absent before install. Verified
package/launcher and installed `1.1.1089-sandbox` / code `1089`, APK SHA-256
`3c717b50f2fe1c5134b6fca1138d92a9d816302d9c14194c95125b14b239cdc2`.
Only `com.coffeepeek.sandbox` was launched, without INTERNET or analytics startup.

Inspected new menu and confirmation screenshots in both themes against the
references: every menu row is divided, labels fit at the reference menu width,
and logout stays red. Confirmation has an inset bordered card, a red text row
below its divider and an independent cancel card, with no grabber or filled
button. Light-theme Cancel and dark-theme system Back close it and leave the
same signed-in local profile. Screenshots/UI dumps/build logs are local outside
tracked sources. The temporary emulator was closed after capture.

Remaining gates: real iPhone/native presentation and executable linking, physical
Android, animation smoothness, larger fonts/window sizes, TalkBack, outside/swipe
dismissal and confirmed logout. No real account/data actions were performed.

## Android release CI regression — 2026-10-22

Run `39749237148`, job `119725315494`, source `5cf6b2c8`, stopped at
`:composeApp:testPlayDebugUnitTest`: 658 tests, one failure in legacy
`ProfileViewModelTest.withoutACacheItRefreshesAndAFailureWithoutContentShowsTheFixedError`.
Signed release assembly and publication were skipped.

The legacy profile cache and session collectors run independently on Default.
A late initial null cache emission can overwrite a completed refresh failure,
removing its error and restoring the loading state. Preserve the current
account's error atomically when rendering an empty cache; session reset still
clears account state. Reach: the existing legacy ViewModel, its common unit
test/fixtures and this plan. Its default scope, DI callers, migrated Android
feature, logout, analytics, resources and Gradle boundaries stay as they are.

Verification plan: force session/refresh-before-cache scheduling in a unit
regression and observe it fail before the fix; execute both delivery variants'
profile regressions, then the Android unit/lint checks and Debug APK assembly
with packaged DEX verification. Checks started in the existing migration
checkout at `4742a16d` plus pre-existing local work, which was independently
committed as `69bb8bc7` during verification. The correction remains on
`codex/migration-profile-cache-race`, based on that commit. The legacy ViewModel
at the failed run's exact revision matches the local base. IDE access does not
have this project open. No device deployment, UI/instrumented tests, live account
operation or release publication is required for this state-update correction.

Local unit results (Windows, JDK 17): the first attempt stopped at test
compilation because the scope `plus` extension import was missing (1m42s).
After that correction, the controlled regression failed as expected before the
production fix: one test, one assertion failure, expected profile error but got
null (30s). Its original log/XML are retained locally outside tracked sources.
After the fix, all 671 application directDebug tests and the 35 selected
playDebug profile tests passed (1m19s), including the new scheduling regression
and its assertion that ending the session clears the error and all account state.

The broader Android check executed 866 library unit tests and all 671 playDebug
application tests; the 671 directDebug results from the preceding run were
up-to-date. All **2208 full-suite results** have zero failures/errors/skips.
Both Debug APKs assembled with packaged DEX runtime-class verification:
`directDebug` / `playDebug`, package `com.coffeepeek`, version `1.1.1093` / code
`1093`. No APK was installed or launched. Remaining gates: manual device review,
iOS compilation/execution, signed release assembly and a new GitHub CI run after
the patch is published. Existing Gradle/KMP, preview, namespace and unrelated
test-source warnings remain; no IDE inspection is claimed.

The full Android unit/lint/build command completed successfully in **12m24s**:
root `testDebugUnitTest` / `lintDebug`, application directDebug/playDebug unit
tests and lint, plus both Debug assemblies and their runtime-class checks.
Each application lint report has zero errors and 84 warnings, none in the
changed ViewModel/test. `git diff --check` passes. No failing tests were retried
after the production fix; the initial compile failure and intentional red
regression remain recorded above.
