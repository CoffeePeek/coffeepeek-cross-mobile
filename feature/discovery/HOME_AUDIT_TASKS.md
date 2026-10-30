# Home audit implementation — 2026-10-31

GitHub tracking: [#236](https://github.com/CoffeePeek/coffeepeek-cross-mobile/issues/236).
The audit's 20 findings are individual issues, #237–#256. Each fix gets a focused
migration branch and PR into `develop`; the user's request authorizes those merges.
Keep the existing brand palette and the HIG/Liquid Glass direction.

## #237 — explicit pagination retry

Base: `33ffd3ee59f924c6a586d8ba4efda860a94e46b9` (`develop`).
Branch: `codex/migration-home-pagination-retry`.

Preserve loaded shops, pagination order, query/filter mapping, account and
cancellation guards, and search analytics. Android Discovery owns its presentation
state; the legacy Feed remains the active iOS path and needs the same bug fix.
The shared design system owns only the loading/error/retry footer, with localised
resources and paired previews. No module, dependency, palette or navigation migration.

Verification plan: demonstrate the original automatic retry with a failing unit
regression; exercise explicit retry, repeated failure, refresh/filter/query/account
changes in in-memory ViewModel tests; run relevant Sandbox backend contracts;
assemble Direct Debug and Direct Sandbox with packaged DEX/isolation checks;
compile the shared design system for iOS. Manually review the second-page failure
and recovery in Direct Sandbox on the existing unlocked API 37 emulator, including
light/dark themes and an enlarged font. No automated UI or screenshot tests.

The manual `PageFailure` Sandbox fixture returns the ordinary paged data, fails the
first attempt at page two, then succeeds on retry. Loading page one rearms it.

### Verification record

- Original regression: one executed test failed, confirming repeated page-two requests.
- An initial test command used a nonexistent Sandbox unit-test task; no tests ran.
  Application unit tests use `testDirectDebugUnitTest`, with in-memory repositories,
  without installing or launching the live application.
- Final verification on 2026-10-31: 213 tests in the selected suites, zero failures,
  errors or skips. Discovery presentation (109) and application Feed/Sandbox
  contracts (67) executed; unchanged domain (15) and data (22) tests were up to
  date from the passing audit run. Earlier focused execution passed 36 Search
  and 22 Feed ViewModel tests. No retries were needed after the fix.
- Direct Sandbox and Direct Debug APK assembly passed, including packaged DEX
  runtime-class checks and Sandbox isolation. Design-system iOS Arm64 KLIB
  compilation executed successfully. Existing Compose preview deprecation and
  unrelated test warnings remain; no IDE inspection/rendering service was available.
- Manual run: unlocked `emulator-5554`, `sdk_gphone64_x86_64`, API 37,
  1080×2400, density 420, font scale 1.0. Source was the base above plus this
  task's patch; `directSandbox`, package `com.coffeepeek.sandbox`,
  `SandboxActivity`, version `1.1.4.2574-sandbox` / 2574. Verified a matching
  signing certificate and updated the older 2573 installation in place.
  APK SHA-256: `76917f493448dbcb22e494e8cc98ef1b18c6f3bc43298b8009de3cb8c9a59b83`.
- Light and dark: scroll through page one, observe a stable inline failure,
  explicitly retry, and see page-two cards appended with earlier cards retained.
  Error text and retry remain reachable above navigation; Back returns to Home.
  The initial install briefly showed Android's updating screen; review began only
  after the settled app launch. Screenshots/dumps/logs remain in ignored build output.
  Original light app theme and font scale 1.0 were preserved/restored.
- Enlarged-font review is pending: automatic approval review rejected changing
  the emulator's system font scale, including a proposal with guaranteed restore,
  citing the device-settings rule. No font-scale command ran; clarification was
  requested. TalkBack, narrow windows, native iOS visual review and real backend
  acceptance remain separate gates. No automated UI tests were created or run.
