# TestFlight startup linkage fix

## Evidence and scope

The supplied TestFlight 1.1 (47.1) report records a foreground abort on
2026-10-21, about 0.3 seconds after launch, on iPhone14,5 / iOS 26.6.2.
The first frames are `throwIrLinkageError`, inline `koinViewModel`,
`platformViewModel`, and the root `Navigator`. The report does not include
the missing symbol or source commit, so the exact unresolved declaration
and the fixed device behavior remain unconfirmed.

The suspected cause is outdated Koin native binaries after the shared
Kotlin/Compose/Lifecycle upgrades. Koin 4.0.1 was built with Kotlin 2.0.21,
Compose 1.6.11 and JetBrains Lifecycle 2.8.3. The application's catalog declares
Kotlin 2.4.20, Compose 1.12.1 and Lifecycle 2.9.6; the iOS dependency graph
actually selects AndroidX Lifecycle 2.11.0 through JetBrains 2.11.0-beta01.

This application-composition fix updates the existing Koin artifacts together
to 4.1.1, built against Lifecycle 2.9.3, without changing screen callers,
navigation, account storage or feature boundaries. Both iOS targets now use
`-Xpartial-linkage-loglevel=ERROR`, so native linking must reject unresolved
calls instead of shipping runtime error stubs. See the upstream
[Koin release](https://github.com/InsertKoinIO/koin/releases/tag/4.1.1) and
[Kotlin partial-linkage behavior](https://kotlinlang.org/docs/whatsnew24.html#consistent-partial-library-linkage-across-kotlin-compilers).

## Verification plan and results — 2026-10-22

Source: `codex/fix-testflight-startup`, based on `0e3f93e8`, plus this working diff.
Host: Windows; no application was installed or launched and no account data changed.

Preserve owner-scoped ViewModel retention/clearing, navigation keys, qualifiers
and constructor parameters. The two new common contract tests exercise Koin's
non-UI resolver used by `koinViewModel`; they do not execute the composable entry.

- `./gradlew --max-workers=6 :composeApp:assembleDirectDebug
  :composeApp:testDirectDebugUnitTest --tests 'com.coffeepeek.admin.di.*'`:
  passed, including packaged DEX runtime-class verification; 210 tests executed,
  zero failures/errors/skips, including both new tests. No failed test retries.
- `./gradlew --max-workers=6 :composeApp:compileKotlinIosArm64
  :composeApp:compileTestKotlinIosArm64`: passed; app and test KLIBs compiled.
  iOS tests were compiled, not executed.
- `dependencyInsight --configuration iosArm64CompileKlibraries
  --dependency lifecycle-viewmodel-compose`: confirmed the resolved versions above.
- Existing Gradle Android/KMP compatibility, deprecated preview/interop APIs
  and unrelated source warnings remain. Studio diagnostics were unavailable.

## Remaining gates

On macOS, link the iOS simulator Debug framework and run the common non-UI
tests, then build the TestFlight archive with the strict linkage check enabled.
Any remaining linkage errors require dependency alignment; do not relax the check.
Smoke-test startup on an iPhone, preserving the installed account and data.
Native framework linking, native test execution, iPhone startup, Android device
startup, visual checks and TestFlight upload were not performed on Windows.
Cross-compilation and JVM tests alone do not confirm this iOS crash is resolved.

## CI follow-up — 2026-10-22

The verification workflow now runs its actual macOS `build` job for iOS/KMP
changes in PRs to main/develop and on manual dispatch. The previous Linux job
that accepted a skipped build was removed. Both workflow path filters include
`feature/`, `features/` and `tools/ios/`; Android-only and Markdown changes are excluded.
The simulator workflow additionally runs just `KoinViewModelCompatibilityTest`
through `iosSimulatorArm64Test --tests`, without starting the application or UI tests.
Branch rulesets remain unchanged; required merge checks need separate configuration.

FirebaseCore/FirebaseMessaging 12.19.2, its resource reference and `-ObjC` now
live in the Xcode project. The unused Swift project-editing package, including
its Crashlytics support, was deleted. Unsigned verification copies a synthetic
plist and requires no secrets. The TestFlight validator rejects this fixture;
release signing, configuration validation, memory settings and main-only upload
remain in place. Local Xcode builds now need the ignored Firebase plist resource.

Local checks: four Python tests passed with zero failures and no retries;
the new fixture-rejection regression covers the production validator.
Actionlint 1.7.12 passed for both iOS workflows with the existing custom
`xcode-27` runner label allowed. OpenStep parsing and Xcode object-reference
checks passed, including the Firebase products, resource and both `-ObjC` settings.
Trigger cases passed for shared/legacy features and CI scripts, with Android-only
and documentation exclusions preserved. All 12 Bash steps and embedded Python
blocks passed syntax checks. Gradle task help confirmed native test filtering support.
`./gradlew --max-workers=6 :composeApp:compileTestKotlinIosSimulatorArm64`
also passed (49 seconds); simulator test KLIBs were compiled, not executed.
The new validator regression was checked against the baseline: the old validator
accepted the synthetic config and the updated validator rejects it.

The simulator executable linking/test execution and signed archive remain macOS
gates. Verification now saves the
resolved SPM lockfile, build/test logs and native test results as CI artifacts.
No manual workflow was dispatched and no TestFlight build was published in this follow-up.

## First macOS verification and Compose ABI correction — 2026-10-22

PR #158 at `2e6c7a64`: Android CI passed, but iOS run `39736101502` failed
at `linkDebugFrameworkIosSimulatorArm64`. The strict partial-linkage guard
reported three unimplemented abstract methods: roaster `Section`, `Search`
and `Content`. The original failure is retained in that run's build-log artifact.
Native contract tests did not execute because framework linking failed first.

Local KLIB IR inspection confirms the mismatch: interface parameters use
non-null `Modifier`/`header`, whereas the cross-module overrides use nullable
types after Compose transforms inherited default arguments. This matches
[KT-87326](https://youtrack.jetbrains.com/issue/KT-87326). Remove defaults from
those three interface methods and supply the same `Modifier` explicitly at the
two callers that omitted it. Search already supplies all arguments. Rendering,
ViewModel ownership and feature boundaries are unchanged; the strict check stays.
No new UI test infrastructure is needed: the real native framework link checks
these overrides, and local IR comparison checks the corrected signatures.

Xcode successfully resolved the Firebase/PostHog package graph before the Kotlin
failure. Its generated `Package.resolved` is copied from the CI artifact into
the project, replacing the stale lockfile. Native linking, contract execution
and signed/device checks still require successful subsequent verification.

With this correction, the Android `directDebug` assembly and packaged DEX
checks passed again, as did all 210 DI unit tests (zero failures/errors/skips).
App/test KLIB compilation passed for both `iosArm64` and `iosSimulatorArm64`.
An assert-based IR comparison confirmed all three original nullable-parameter
mismatches, then verified identical API/override signatures after recompilation.
The lockfile parses as JSON, has unique pins and resolves Firebase 12.19.2.
These local checks took two minutes; native executables still await macOS CI.
