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
  surface/border and bounded width, with separators before privacy and sign-out.
  It no longer inherits the application theme's 50% button shape. The shared/iOS
  menu retains its anchored platform implementation and marks sign-out destructive.
- Keep contribution actions and their root navigation. Logout's label and icon
  are destructive red. Close the menu before opening confirmation or navigating.
- Replace both profile alerts with design-system
  `CoffeePeekDestructiveConfirmationSheet`, using the existing swipe-dismiss
  modal sheet, typography and button. It animates from below, respects safe insets,
  and supports cancel, outside dismissal and the accessible dismiss handle. The
  content scrolls when long text or large fonts exceed the available height.
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

## Requested Sandbox visual review — 2026-10-22

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
