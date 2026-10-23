# Authentication layout alignment

Date: 2026-10-23. Source baseline: `bcba3a3c` on
`codex/migration-public-check-in-default`, plus existing local work.

## Scope and preservation

- Owner: existing shared authentication presentation in `composeApp/commonMain`.
  This is a layout correction, not a feature/module/navigation migration.
- Entry points: root `Auth`, `Register`, `ForgotPassword` destinations in
  `Navigator`; registration retains Email/Details/Success, recovery retains
  Email/EmailSent. No route or ViewModel contract changes.
- Reuse existing Auth fields/buttons/scaffold and design-system theme/tokens.
  The design-system form rail is vertical and does not implement registration
  progress. Auth progress stays local to authentication.
- Keep the existing 40 dp card side insets. Constrain progress and social buttons
  to that same content width; show all progress labels with wrapping rather than
  truncation. Keep active/completed steps and accessible selection.
- Use 16 dp between form sections, 8 dp inside the title/description group.
  Remove extra field/chip/footer margins and the unused social divider.
- Recovery uses the same mascot, title style, email icon and top reserve as login.
- Runtime screens retain lifecycle ViewModel ownership; stateless content has
  paired fake-state multiplatform previews beside its implementation, including
  registration/recovery completion and narrow/large-text progress previews.
- Preserve validation, terms and optional analytics consent, busy/error rendering,
  password handling, keyboard dismissal, cancellation, and all navigation actions.

## Analytics inventory

Existing ViewModels own password/Apple/Google sign-in analytics, deferred password
sign-up identification, and explicit analytics consent. Registration captures the
optional choice before its existing register request. UI forwards the same
callbacks; no new analytics/exposure owner, service, consent policy or event is
introduced. Recovery has no UI-owned analytics. Sandbox emits no production data.

## Verification plan

1. Build `:composeApp:assembleDirectSandbox --max-workers=6`, including isolation
   and packaged DEX runtime class checks.
2. Run existing non-UI auth/password/recovery/registration-consent unit tests.
   No automated UI, Compose instrumentation, screenshot tests or test hosts.
3. On the existing unlocked Pixel_8 emulator, explicitly target its serial;
   check APK/package/version/signature before an in-place Sandbox update.
4. Manually review login, registration email/details/success and recovery in both
   themes, with keyboard, scrolling, back/cancel and validation. Capture originals
   outside the repository and compare content edges with the previous gallery.
5. Record results, source/APK identity, device, failures/retries and limitations.

Remaining gates: iOS compilation/device, OAuth/live backend/email delivery,
TalkBack, rendered IDE previews, and additional font/window sizes. New previews
do not count as device verification. Unrelated local platform-presentation work
and its diagnostics remain outside this change.

## Results

Verification completed on 2026-10-23.

- Verification source: baseline `bcba3a3c`, then migration tip `50c32fba` plus
  the authentication patch and pre-existing packaging/run-mode edits. Packaging
  edits are separate from the authentication change.
- `assembleDirectSandbox`, `assembleDirectDebug`, Sandbox isolation and both
  packaged DEX runtime-class checks passed in one Gradle invocation with
  `--max-workers=6` (1m 24s, 1121 tasks: 70 executed, 1051 up-to-date).
- Seven existing unit suites executed: **41 tests, 0 failures, 0 errors,
  0 skipped**. PasswordSignUpAnalytics 4, AppleLoginFlow 8, SocialLoginError 16,
  PasswordRecoveryRepository 3, PasswordRecoveryViewModel 4,
  RegisterAnalyticsConsent 3, PasswordValidation 3. No test retries.
- Retained initial failures in local logs: the Sandbox unit-test task does not
  exist (use DirectDebug unit tests); the next compile rejected the unsupported
  multiplatform Preview `fontScale` parameter. Fixed it using preview-local
  density; the final build/test run above passed. No production logic failure
  was found and no tests were weakened.
- Installed only `directSandbox` on unlocked `emulator-5554`, AVD `Pixel_8`,
  model `sdk_gphone16k_x86_64`; 1080×2400, 420 dpi, font scale 1.0.
  Package `com.coffeepeek.sandbox`, Activity
  `com.coffeepeek.admin.sandbox.SandboxActivity`, version
  `1.1.1.1232-sandbox` / 1232. The matching debug certificate allowed an
  in-place update of 1230. No uninstall, downgrade, data clearing, permission
  changes or real-account operations.
- APK SHA-256:
  `1D8ADB0EAAD8310687D983DFD935D84C7C5ADE73D94DAB01A1B48B97ADF02244`.
- Manual review: login, registration Email/Details/Success and recovery Email in
  both themes; 20 curated screenshots plus intermediate evidence, kept outside
  tracked sources in the chat's local `auth-after` artifact directory.
  Original before screenshots were preserved in `auth-review`.
- Registration progress occupies x=147…933 px, matching fields/buttons. Its
  three columns are 147…409, 409…671 and 671…933 px; all labels visible.
  Login/recovery headings now both begin at y=695 px (recovery was y=411).
  Recovery's long title wraps normally instead of being truncated.
- Verified local edit-email → continue preserves the typed name, local
  registration reaches Success, and empty recovery submission shows validation
  and hides the keyboard. Tested back from recovery and keyboard dismissal.
  With the keyboard open, login's CTA is below the initial viewport and is
  reachable by scrolling; a separate scrolled capture proves access.
  Long registration also requires scrolling. No fixed bottom CTA was introduced.
- Fake data only: `auth-review@example.invalid`, Sandbox, dummy password;
  optional analytics remained off. Original dark theme/local Sandbox session
  restored after capture.
- Paired previews compile, including narrow 208 dp content and 130% text
  previews for progress; their IDE rendering was not available. Configured IDE
  access does not expose this project. Existing unrelated Gradle/KMP/AGP and
  deprecation warnings remain. Changed functions fit review body limits; larger
  unchanged legacy background/social-button bodies were not rewritten.
- No automated UI/instrumentation/screenshot tests or test hosts were created
  or run. No Gradle/source-set/dependency/DI or business contract changes.
- Remaining: iOS/device compilation, OAuth/live backend/mail delivery, recovery
  EmailSent rendered on a device, TalkBack, extra window sizes and device font
  scales. Recovery EmailSent has fake-state previews only.
- The comparison HTML and original PNGs remain local, outside tracked sources.
  Manual Android observations are the visual verification evidence.
