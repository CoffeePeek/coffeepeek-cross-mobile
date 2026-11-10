# Android Google sign-in: localized context regression

## Scope and verification plan — 2026-11-10

Owner: `feature/auth/impl`, Android presentation integration. Both sign-in and
registration use the same launcher. Preserve account selection, cancellation,
token delivery, backend contracts and analytics. Verify auth unit tests and
Android assembly, then manually verify the Google chooser on the connected phone
using a correctly configured, compatibly signed release APK. Debug and Sandbox
disable Google sign-in and cannot establish real OAuth acceptance.

## Cause and fix

The launcher cast `LocalContext.current` directly to `Activity`. The application
language environment supplies a `ContextThemeWrapper`, so the cast returns null
and the click reports an unavailable Activity before launching Google.
Use `androidx.activity.compose.LocalActivity.current`, which unwraps the context.
The cast originated in `80cabbc518`; the language wrapper arrived in `4474d9a94`.

## Verification evidence

- Source: `d9be672648ad129db6d34bcb1e698cdb8208395f` plus the local launcher fix.
  Pre-existing working-tree changes remain outside this fix.
- `git diff --check` passed for the changed launcher.
- Inspected cached AndroidX Activity Compose 1.11.0 sources: `LocalActivity`
  delegates to `findOwner`, which traverses `ContextWrapper.baseContext`.
- Attempted `:feature:auth:impl:testDebugUnitTest` and
  `:composeApp:assembleDirectDebug` with `--max-workers=6`. The wrapper failed
  to acquire its cache lock (access denied). A separate writable Gradle home
  initialized successfully but failed to connect to its single-use daemon.
  Compilation, unit tests and packaged DEX checks did not execute; zero tests ran.
- ADB device discovery failed to start its server. Foreground diagnostics report
  `failed to create fdevent interrupt socketpair: Invalid argument`.
  Device model, lock state, installed version and signature could not be verified.
- Local Google web client ID and release-signing environment configuration are
  absent. No release APK was built, installed or launched; no account was changed.
- Remaining gates: successful assembly/tests and DEX checks, compatible signed
  in-place device update, chooser opening/cancellation from login and registration,
  and authorized real OAuth acceptance. No automated UI tests were added or run.

### Requested retry — 2026-11-10

Repeated ADB discovery, wrapper build, and the build with the separate writable
Gradle home. The same failures remain: ADB cannot start its server, the wrapper
cache lock is inaccessible, and the isolated Gradle client cannot connect to its
daemon. No compilation, tests, installation or phone verification completed.
Local OAuth and release-signing configuration are still absent.

