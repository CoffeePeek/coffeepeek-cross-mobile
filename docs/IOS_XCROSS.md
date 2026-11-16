# Local iOS Debug builds with xcross

Windows integration checkpoint, 2026-11-16. Linux and Release are untested.
Build and device acceptance are recorded separately below.

**Windows build, signing, USB installation and startup passed.**
CoffeePeek Xcross 1.2.0 (2949), ARM64, iOS 17+, bundle ID `com.coffeepeek.xcross`.
Built using xcross 2.1.1-coffeepeek.6 and installed on the selected iPhone15,4
running iOS 27.0.1. The onboarding screen rendered and the app process remained
running after debugger detachment. The existing `com.coffeepeek` 1.2.1 (3050)
was preserved alongside it.

Signed app: `build/xcross-ios/CoffeePeek Xcross.app`.
Signed archive: `build/xcross-ios/CoffeePeek-Xcross-1.2.0-2949-signed.ipa`
(48,986,388 bytes; ZIP CRC and executable permissions verified).
SHA-256: `d5780cff710aab819b665092cc665770c9094ae10512f870ea9ad8ead8ade618`.
The development profile includes the selected phone, allows debugging and expires
on 2027-11-16. The build identity JSON and startup screenshot remain in ignored
build directories. Screenshots and profiles are local evidence, not tracked assets.

The owner approved temporarily disabling the shelf bag scanner in local xcross
builds. This mode omits ONNX Runtime, reports the scanner as unavailable, and
blocks preparation, camera entry and scanning. Android and ordinary Xcode builds
retain the scanner. Other native integrations and the SwiftUI host remain included.

## Build command

Use Python 3.11+ and a fresh terminal with Swift and its Windows runtime on PATH.
From the repository root:

```powershell
python tools/ios/xcross_build.py --xcross /path/to/patched/xcross.exe --config .gradle/xcross-config.yaml
```

The wrapper selects Debug, sets `coffeepeekXcross=true` for Gradle and
`COFFEEPEEK_XCROSS=1` for SwiftPM, and flattens Debug.xcconfig with Shared and
optional Local includes. Build numbering follows git commit count plus
BUILD_NUMBER_OFFSET. Local developer settings remain ignored. The wrapper restores
launch metadata, expands development entitlements, copies an existing Firebase
plist, and validates the executable and bundle resources.

Expected outputs: `build/xcross-ios/CoffeePeek.app`, a versioned `-unsigned.ipa`,
and `build-identity.json` beside them. They are unsigned. A compile does not certify iPhone installation or
startup. Do not run `xcross compose run` without the same environment settings:
it would select the ordinary scanner configuration again.

Signing and installation need the CoffeePeek Apple Developer team and an explicitly
selected, trusted iPhone. The deployment wrapper installs a separate
`com.coffeepeek.xcross` application named **CoffeePeek Xcross**, preserving the
existing `com.coffeepeek` app and its data. It rejects a downgrade of an existing
test installation and never uninstalls or clears app data.
Never commit credentials, certificates, provisioning profiles or device logs.

Authenticate once with `xcross auth --issuer-id <UUID> --key-id <KEY_ID>
--private-key <path-to-p8>`; credentials stay in the xcross user directory.
Use an Admin team key for provisioning a device that is not already registered.
Then build, sign, install and launch over USB in one command:

```powershell
python tools/ios/xcross_run.py --udid <IPHONE_UDID> --xcross /path/to/patched/xcross.exe --config .gradle/xcross-config.yaml
```

This uses the same scanner-disabled Debug environment as the unsigned wrapper,
checks USB pairing and Developer Mode, and explicitly selects the test bundle ID.
The API base URL still comes from Debug/Shared/Local.xcconfig; this is not a fake
backend. Live account writes and OAuth/push acceptance require separate testing.

Measured on this Windows machine:

| Scenario | Time |
| --- | --- |
| Full native framework rebuild | 10m27s, plus other compilation steps |
| Full app build | About 11–12 minutes before signing/install |
| Cached build, signing, USB install and launch | 95.98 seconds total |
| Cached framework step alone | 2.3 seconds |
| Signing / USB install | 3.2 seconds / 11.3 seconds |

Kotlin/Compose changes invalidate the native framework, so budget roughly twelve
minutes for that loop on this machine. The cached result was measured with no
Kotlin changes; it is not a claim that edited Compose code hot-reloads. Native-only
edits and changed Swift packages have not been timed separately.

## Installed Windows toolchain

| Component | Version / source |
| --- | --- |
| xcross | 2.1.1 with compatibility patch below |
| Swift | 6.4.2, including Windows runtime |
| LLVM | 23.1.5; stock ld64.lld |
| Java for xcross | Temurin 21.0.13+9, separate from project Java 17 |
| MSVC / Windows SDK | 14.44.35207 / 10.0.22621.0 |
| Device transport | pymobiledevice3 11.56.1 |
| Apple SDK | User-provided Xcode_27.3_beta.xip |
| Kotlin/Native | 2.4.21 Windows compiler with macOS iOS platform libraries |
| Dart for xcross | 3.14.0 |

SDKs and caches stay in xcross user directories. The literal include(":composeApp")
in settings allows xcross discovery without evaluating our settings plugin;
Gradle's idempotent include preserves the module graph.

## Compatibility patch

[xcross-2.1.1-coffeepeek.patch](../tools/ios/xcross/xcross-2.1.1-coffeepeek.patch)
targets upstream commit `251be22c033fa31fd03a7bf107308d07d28abf81` (v2.1.1).
Upstream is AGPL-3.0. No SDKs or credentials are included.

- Adds published Kotlin 2.4.21 archive hashes.
- Prefers the existing SwiftUI @main over the Kotlin UIViewController factory.
- Handles Windows extended paths and excludes .build from source discovery.
- Reads the selected Xcode application's source phase, including its references
  to the existing native bridges under iosXtool.
- Limits Gradle to six workers and selects one Kotlin/Native distribution.
- Parses comments before SwiftPM binary checksums.
- Skips the Flutter-only module warm-up target in Compose and serializes Windows
  SwiftPM compilation to avoid shared module-cache races.
- Forwards SwiftPM header search paths and compiler macros to the Swift host.
- Uses the configured iOS minimum, preserves launch metadata, expands entitlements,
  and copies native localization, privacy and optional Firebase resources.
- Rejects skipped/no-source Kotlin compilation even when an older KLIB exists.
- Never automatically revokes team certificates on quota/stale-cache failures,
  and preserves provisioning profiles created by other tools.

Rebuild in a separate checkout:

```text
git clone --branch v2.1.1 --depth 1 https://github.com/arxdeus/xcross.git
cd xcross
git rev-parse HEAD
git apply /absolute/path/to/xcross-2.1.1-coffeepeek.patch
dart pub get
cd packages/xcross
dart test test/compose/coffeepeek_entry_test.dart test/compose/swift_app_host_test.dart test/swiftpm/swiftpm_binary_target_test.dart test/compose/compose_host_test.dart test/compose/swiftpm/host_swift_packages_test.dart
dart run -DXCROSS_VERSION=2.1.1-coffeepeek.6 tool/build_xcross.dart
```

In PowerShell quote the complete '-DXCROSS_VERSION=2.1.1-coffeepeek.6' argument.
Source builds omit OpenAppleMacrosServer by default. Copy the official 2.1.1
bundle's OpenAppleMacrosServer.exe, swiftCore.dll, msvcp140.dll, vcruntime140.dll
and vcruntime140_1.dll into the new bundle's lib directory, retaining the fresh
ABI bridge. Upstream also supports compiling its pinned macro server submodule.

Use an ignored XCROSS_CONFIG YAML: roots.xcross points at the patched executable,
roots.javaHome at Java 21, roots.konanData at an isolated cache, toolchains.swift
and toolchains.llvm at installed tool directories.

## Verification and remaining gates

Baseline: `38b368c11` plus this checkpoint's uncommitted changes, isolated branch
`codex/migration-ios-xcross`. Only the separate test application was installed.

Plan: preserve Android scanning, compile disabled iOS mode, execute shelf ViewModel
regressions, assemble Android Debug with packaged DEX checks, then validate the
native iOS binary/resources. Device startup, maps, authentication and other real
integrations need separate device checks. No automated UI tests are used.

- SDK extraction, xcross setup and ARM64 C/linker probe: passed.
- Patched xcross regression suites: 63 passed on Windows.
- Wrapper checks: 4 passed (configuration, launch/entitlement metadata, ARM64 and
  ONNX exclusion).
- Deployment version guards: 3 passed (newer version/build and unknown version
  rejected, equal/older test app accepted without uninstall).
- Apple provisioning unit suite: 23 passed, including certificate quota and
  preservation of other tools' profiles. Changed Dart files pass static analysis.
- Scanner-disabled :composeApp:compileKotlinIosArm64: passed with app task executed.
- Native Swift packages: built on Windows.
- ComposeApp.framework: native compilation/link passed (first full pass 9m49s).
- Full wrapper build: passed. Swift packages 4m29s, Kotlin sources 12.8s,
  native framework 9m52s, Swift host 11.0s, bundle assembly 1.3s.
- Separate test app build with safe signer: passed. Swift packages 24.7s,
  Kotlin sources 11.5s, native framework 10m27s, Swift host 10.5s, assembly 1.2s.
- All five Swift app sources compiled and linked with the real framework,
  SwiftPM headers and macro server.
- Shelf unit suite: 88 tests, zero failures/skips, including the disabled scanner
  regression. Executed on Android JVM; no instrumented or automated UI tests.
- Android :composeApp:assembleDirectDebug and verifyFunctionBodyLimits: passed,
  including verifyDirectDebugRuntimeClasses on the packaged APK (1m08s).
- Final bundle: ARM64 executable, iOS 17.0, Debug flag, build 2949, launch metadata,
  ru/be localization, privacy manifest, Compose resources and both required
  dynamic frameworks verified. No ONNX Runtime embedded.
- Executable SHA-256: `350a1366e5f18614058c0989a44513ba93aefa21659d7bbb1ed0d8e488b8593f`.
- Existing application presentation configuration tests: 4 passed.
- Apple Devices 1.1792.39682.0 installed from Microsoft Store; its UI detected
  the selected iPhone15,4 on iOS 27.0.1. USB pairing and Developer Mode verified.
  Existing production app 1.2.1 (3050) is newer than this checkpoint; deployment
  therefore uses a separate test bundle ID. Signing, install and startup passed.
- The iOS 27 Cryptex1 Developer Disk Image was downloaded, personalized by Apple
  and mounted successfully over the userspace USB tunnel. Device settings were
  not changed. No app was downgraded or removed.
- The first signing attempt returned HTTP 403 with the Developer-role key at
  test App ID registration. After the owner supplied a new Admin key, xcross
  provisioned the test App ID, development certificate and device profile,
  signed the bundle and installed it. The certificate-preservation patch was
  active throughout provisioning; no team certificate was revoked.
- Manual startup screenshot: onboarding visible in dark mode. CoreDevice process
  query confirmed the test app still running after debugger detachment. No
  account creation, sign-in, consent or live-data edits were performed. Maps,
  OAuth, push, all-theme/accessibility UI review and scanner acceptance remain
  untested. The scanner is intentionally disabled in xcross builds.
- Paired light/dark previews were added; IDE/manual rendering was not performed.

Earlier failures: ONNX enabled caused Windows to disable Kotlin SwiftPM cinterop
and skip application compilation. An isolated binding experiment then failed on
Objective-C constructors; it is not a production dependency. Missing Swift module
warm-up targets and duplicate SDK libraries motivated the xcross patch above.
The first full framework then exposed missing SwiftPM headers and macro flags at
the Swift host stage; a separate successful link verified those corrections.

Local logs and probes remain in ignored build directories. Review before sharing.
Restoring xcross scanning requires compatible ONNX bindings and real-device scanner
acceptance; removing the disabled UI alone is insufficient.

References: [xcross Compose](https://xcross.sh/docs/compose/),
[upstream v2.1.1](https://github.com/arxdeus/xcross/tree/v2.1.1),
[Apple device registration roles](https://developer.apple.com/help/account/devices/register-a-single-device/).


## Native branch integration (2026-11-16)

The checkpoint was rebased onto native iOS tip `175f7b31e` and pushed to
`codex/ios-native` as `1342b8c86`. Existing SwiftUI screens and scanner source
changes from that branch were retained. This section supersedes the older
baseline above for native-branch acceptance.

The first native-branch build failed in Swift compilation: the framework exported
an older MainViewController signature. Dart uppercases environment variable names
on Windows, so Gradle did not recognize the mixed-case
`ORG_GRADLE_PROJECT_coffeepeekXcross` suffix. ONNX cinterop disabled app Kotlin
compilation, but xcross consumed the previous KLIB. The shelf build now also reads
the existing uppercase `COFFEEPEEK_XCROSS=1` flag; the explicit Gradle property
still takes precedence. The xcross Gradle bridge rejects an unexecuted, disabled
or no-source compile task. Valid up-to-date/build-cache results remain accepted.

Verification of this correction:
- Confirmed Windows Dart exposes the mixed-case project variable in uppercase.
- Negative integration check: excluded `:composeApp:compileKotlinIosArm64` while
  retaining its old output; `dumpIosDeps` rejected the stale KLIB.
- Positive integration check: `dumpIosDeps` succeeded with only
  `COFFEEPEEK_XCROSS=1`, without an explicit scanner Gradle property; current app
  compilation was up-to-date and current Compose resources were included.
- Changed Dart file: static analysis passed. Existing xcross suites: 63 passed.
- Python build/deployment wrapper suites: 7 passed.
- Native-branch shelf JVM suite: 89 tests, zero failures/errors/skips.
- Native-branch Android DirectDebug assembly and packaged runtime-class check
  passed. No Android app was installed.
- `verifyFunctionBodyLimits` failed on pre-existing
  `createBusinessOwnershipEntry()` (56 body lines vs 20). The source blob is
  identical to native parent `175f7b31e`; this checkpoint does not alter that
  function or extend its baseline. This check is not green on the native tip.

Native-branch device deployment remains pending until the corrected full build
and manual startup verification finish. The earlier installed build 2949 is not
evidence for this integration.
