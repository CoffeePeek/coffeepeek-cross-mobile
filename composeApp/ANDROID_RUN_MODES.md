# Android: what to build and run

## Choose the environment first

| Purpose | Build Variant | Package / launcher | Data and backend |
|---|---|---|---|
| Default development, visual checks, safe create/edit/delete, simulated failures | `directSandbox` | `com.coffeepeek.sandbox` / CoffeePeek Sandbox, purple icon | Local fake HTTP backend, synthetic photos; no INTERNET permission |
| Real-backend parity, actual platform integrations, read-only account checks | `directDebug` | `com.coffeepeek.debug` / CoffeePeek Debug, gold icon | Real configured backend; separate local storage/session; Google sign-in and production updates disabled |
| Same real-backend Debug through Play delivery behavior | `playDebug` | `com.coffeepeek.debug` / CoffeePeek Debug, gold icon | Same Debug storage and live backend; Google sign-in and production updates disabled |
| Distribution / production | `directRelease`, `playRelease` | `com.coffeepeek` | Release signing/configuration; not a routine test run |

`direct`/`play` select delivery behavior; `debug`/`sandbox` select the execution
environment. `playSandbox` is also fake and shares the Sandbox package; do not
install both Sandbox delivery variants expecting two separate apps. Debug is
production-like behavior compiled from the current branch, **not** an assertion
that it exactly equals the currently released source. Release Google sign-in
still needs valid OAuth configuration; Debug explicitly ignores that key.

`main` distributes Release (`com.coffeepeek`); the development branch is named
`develop` and distributes Debug (`com.coffeepeek.debug`) through Firebase. All
three environments, including Sandbox, can coexist. Direct/Play variants of the
same environment share one package. Debug's launcher uses `GoldWarm` (`#D4A84B`)
in light and dark themes and displays `CoffeePeek Debug` in every locale.

`directRelease` packages native libraries only for `arm64-v8a` and
`armeabi-v7a`; its single APK is for ARM phones. Debug/Sandbox retain x86 ABIs
for emulators, and the `playRelease` AAB retains all four ABIs for Play delivery.
Do not use the direct APK to update an x86-only installation.

## Android Studio

For day-to-day use, choose `directDebug`; switch to `directSandbox` only when
you want fake scenarios. The agent's safe-testing default does not automatically
switch your chosen variant. All three apps can remain installed side by side; after
installation you can also open either directly from its launcher icon.

1. Sync Gradle after changing modules or variants.
2. For build-only work select **CoffeePeek Sandbox — Build** (the safe default),
   or **CoffeePeek Debug — Build** for the explicit live variant. These shared
   Gradle configurations name exact tasks and never install/launch the app.
3. To launch, open **Build Variants** and select `directSandbox` for `composeApp`.
   Select the existing **composeApp** Android run configuration and the agreed
   unlocked device. Verify the resulting package/entry before deployment:
   Sandbox launches `com.coffeepeek.admin.sandbox.SandboxActivity`; ordinary
   Debug launches `com.coffeepeek.admin.MainActivity`.
4. To intentionally run the live app, select `directDebug` instead. The generic
   configuration name **composeApp** does not pin a variant and must never be
   treated as proof that it will launch Sandbox. Inspect its launch settings
   rather than running a manually overridden Activity from another environment.

No automatic APK installation is attached to the build-only configurations.
Do not run two Gradle builds at once. Agent-driven builds use `--max-workers=6`.

## Terminal / explicit device selection

Build only (no phone required):

```shell
./gradlew :composeApp:assembleDirectSandbox --max-workers=6
./gradlew :composeApp:assembleDirectDebug --max-workers=6
```

Run these sequentially, only for the environments needed. APK locations:

- Sandbox: `composeApp/build/outputs/apk/direct/sandbox/composeApp-direct-sandbox.apk`
- Debug: `composeApp/build/outputs/apk/direct/debug/composeApp-direct-debug.apk`

Before installation, resolve the serial/model and lock state with `adb devices
-l` and `adb -s DEVICE_SERIAL shell dumpsys window policy`. Replace
`DEVICE_SERIAL` with the verified agreed device, never the default device.
Inspect the APK package/version/manifest and the installed version/signature;
record the source revision and variant. Only then install and open Sandbox:

For manual resource-language checks, a fresh Sandbox launch accepts
`--es sandbox_locale ru` or `--es sandbox_locale en`. This overrides only that
Compose context's resource configuration and is cleared on a fresh launch without the
extra; it does not change phone settings or advertise a translated app locale.

For manual layout review, fresh launches also accept `--ef sandbox_font_scale 1.3`
(1.0–2.0), `--ei sandbox_width_dp 320` and `--ei sandbox_height_dp 740`.
These constrain only Sandbox Compose content and its local window info; the
physical window/system settings are unchanged. Dialogs are separate windows;
verify their text scale independently. `--es sandbox_discovery LongText` supplies
long shop titles and addresses. Omit extras on a fresh launch to restore defaults.

For manual accessibility review, `--ez sandbox_opaque_glass true` selects the
opaque high-contrast glass treatment without changing the device contrast. A
fresh launch with `--ez sandbox_reduce_motion true` supplies a zero
MotionDurationScale to Sandbox's lifecycle-owned Compose Recomposer; both
Compose animations and the application's motion decision use that same scale.
These fixtures verify rendering, not the real system-setting observer or
physical haptic quality. Omit extras to follow system settings again.

```shell
adb -s DEVICE_SERIAL install -r composeApp/build/outputs/apk/direct/sandbox/composeApp-direct-sandbox.apk
adb -s DEVICE_SERIAL shell am start -n com.coffeepeek.sandbox/com.coffeepeek.admin.sandbox.SandboxActivity
```

For an explicitly approved live Debug check, after the same compatibility checks:

```shell
adb -s DEVICE_SERIAL install -r composeApp/build/outputs/apk/direct/debug/composeApp-direct-debug.apk
adb -s DEVICE_SERIAL shell am start -n com.coffeepeek.debug/com.coffeepeek.admin.MainActivity
```

Debug, Release and Sandbox have separate packages, storage and sessions. Existing
data stays in its original package; installing Debug does not migrate or clear
Prod data. Debug still connects to the configured live backend.

Local Debug uses the normal debug signing key. Firebase Debug uses the configured
distribution keystore for stable updates. Those two Debug APKs share a package
but can have different signatures. Stop and report an incompatible signature or
newer installed APK: never uninstall, clear data or force a downgrade.

For push/Firebase integration, provide a registered `com.coffeepeek.debug` client
in `composeApp/src/debug/google-services.json` (or alongside the production client
in `composeApp/google-services.json`). Never rename the package in a production
Firebase config. See [Firebase distribution configuration](../docs/FIREBASE_CD.md).

## What each check proves

Use the remaining unit/contract tests for behavior, then manual full Sandbox checks for
root navigation, fake CRUD, themes and error paths. Use the live Debug app only
when actual backend/device behavior is the subject of the check. Real-account
writes, logout, favorites, consent changes and destructive actions still need
explicit authorization and a safe account. Sandbox uses in-memory local fixtures,
not a server-side test account; it does not certify OAuth, real media,
analytics, backend persistence or iOS readiness. See [Sandbox coverage](SANDBOX.md).

The agent-authored feature/design-system UI suites and test-only hosts were
removed on 2026-10-20 at the user's request; earlier recorded runs are historical.
Other contributors' application UI tests and database instrumentation remain.
See [removal scope](../feature/UI_TEST_REMOVAL.md).

## Debug identity verification — 2026-11-13

Source: `497814408` plus the working-tree Debug identity changes; Android version
`1.2.0.2945-debug` / code `2945`. No device was installed or launched.

- `assembleDirectDebug`, `assemblePlayDebug`, `assembleDirectSandbox`: passed,
  including packaged DEX runtime-class checks and Sandbox isolation.
- Merged manifests and APK metadata confirm separate package/provider authorities,
  the Debug label/icon, and no Debug icon assets in Sandbox. Release retains
  `com.coffeepeek` and its original launcher resources.
- `AppUpdateAdaptersTest`: 3 executed, 0 failures; release tooling: 29 executed,
  0 failures. Function body limits: 0 new violations. Workflow YAML and embedded
  Python parsed; synthetic Firebase configs accepted one Debug client and rejected
  production-only and duplicate Debug clients.
- Initial build failed while packing a locked Kotlin cache; the no-build-cache
  retry passed Direct Debug/Sandbox but hit an inaccessible unit-test cache.
  Removing only that generated task cache allowed the final unit/Play checks to
  pass. Existing preview deprecations and unrelated coroutine opt-in warnings remain.
- Local adaptive-icon preview checked with a round launcher mask; packaged color
  is `#FFD4A84B`, with one foreground/background for light/dark and one label for
  every locale. Device launcher/theme review, Firebase registration/secret setup,
  distribution signing, push and live-backend checks remain unverified.

The Debug-only foreground is `src/debug/res/drawable-nodpi/ic_launcher_debug_foreground.png`.
It was edited with built-in `image_gen`; Android draws the exact gold background
separately. The final edit prompt was:

> Precise background removal for an Android adaptive launcher icon foreground. Edit target is the existing CoffeePeek icon provided. REMOVE ONLY the white background, including inside the cup handle, making the entire backdrop genuinely transparent, including the outer padding. Keep the ivory coffee cup and olive-green bean mascot with both eyes and hands and coffee exactly unchanged in design, placement, scale, lighting and color. Preserve soft contact shadow with partial alpha only, no white matte. Do not enlarge, recenter or redesign the subject: it should occupy the central roughly 58% of the square width, like the original, for adaptive icon safe zone. Output a square RGBA transparent PNG with the original large margins. No gold backdrop; the app draws an exact #D4A84B background as a separate layer. No text, no other elements.
