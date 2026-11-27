# Local iOS Debug builds with xcross

Current repository wrapper contract, reviewed 2026-11-27. The normal macOS/Xcode
path is [IOS_SETUP](IOS_SETUP.md). xcross is a separate scanner-disabled local
Debug verification path, not the App Store archive/release pipeline.

## Toolchain and patches

The tracked compatibility patches target upstream xcross v2.1.1, commit
`251be22c033fa31fd03a7bf107308d07d28abf81` (AGPL-3.0). No SDKs, provisioning
profiles or credentials are included. In a separate upstream checkout, apply in order:

1. [xcross-2.1.1-coffeepeek.patch](../tools/ios/xcross/xcross-2.1.1-coffeepeek.patch)
2. [Swift runner response-file patch](../tools/ios/xcross/xcross-2.1.1-swift-runner-response.patch)
3. [Windows framework-staging patch](../tools/ios/xcross/xcross-2.1.1-windows-framework-staging.patch)

Build the resulting compatibility executable with version
`2.1.1-coffeepeek.8`. Keep Windows Swift runtime/macro server support and required
MSVC/LLVM components available. This path was developed with Swift 6.4.2,
Kotlin/Native 2.4.21, Java 21 for xcross and JDK 17 for this project's Gradle;
these are compatibility provenance, not a claim about currently installed tools.
Apple SDKs are supplied locally by the developer. Do not download/commit SDKs
or silently replace a user's configured toolchain.

Patches preserve source-phase discovery for the shared Swift files, response-file
quoting/long argv on Windows, SwiftPM framework staging, native source macros/
headers, resource/launch metadata and bounded build concurrency. They reject an
unexecuted Kotlin compile rather than consume an old KLIB. The settings literal
`include(":composeApp")` supports discovery; module registration remains owned by
the settings plugin. Changes to upstream signer internals need renewed validation.

For rebuilding the patched tool, run its included Dart tests before app builds;
the patch adds tests under `test/compose`, `test/swiftpm` and provisioning areas.
Source builds omit the macro server bundle by default: use upstream's pinned
macro-server build or the matching official 2.1.1 support files while preserving
the fresh ABI bridge. Do not mix unrelated framework slices or tool versions.

## Build wrapper

Python 3.11+ and a terminal with Windows Swift/runtime on PATH are required.
Use an ignored local YAML config with xcross executable, Java home, isolated
Kotlin/Native cache and Swift/LLVM roots.

```powershell
python tools/ios/xcross_build.py --xcross /path/to/patched/xcross.exe --config .gradle/xcross-config.yaml
```

The wrapper selects Debug, sets `coffeepeekXcross`/`COFFEEPEEK_XCROSS`, flattens
Shared/Debug/optional Local xcconfig, stages resources and validates bundle/
executable metadata. It temporarily omits ONNX scanner linkage from the generated
SwiftPM manifest and restores the manifest on exit. Normal Xcode and Android
scanner behavior is not changed. The scanner is reported unavailable in xcross;
removing a disabled UI guard is not a verified scanner implementation.

Outputs under `build/xcross-ios` include `CoffeePeek.app`, versioned unsigned IPA
and `build-identity.json`. An unsigned app or a compile does not establish a signed
install, launched UI, session persistence or OAuth acceptance.

## Signing and device deployment

The USB wrapper targets the separate `com.coffeepeek.xcross` application, preserves
`com.coffeepeek`, checks pairing/Developer Mode and refuses a newer installed
version/build or unparseable identity. It does not authorize uninstall, data
clearing, certificate revocation or changing device security settings.

```powershell
python tools/ios/xcross_run.py --udid <IPHONE_UDID> --xcross /path/to/patched/xcross.exe --config .gradle/xcross-config.yaml
```

Run only for the explicitly selected trusted, unlocked device and authorized
integration scenario. Configuration still points to the normal API; this is not
an in-memory Sandbox. Real account writes and OAuth/push checks need a safe setup.
No device is selected and no installation is performed by this documentation pass.

Cached development signing uses `tools/ios/sign_cached_debug.dart` and
`tools/ios/verify_signed_app.py`. Concrete runtime Keychain groups must replace
profile wildcards; an install/startup alone does not prove session persistence.
The signer derives Apple Sign-In availability from the actual profile capability.
The verifier checks embedded entitlements/resources, not OS trust validation.
Keep app identifier/signing identity compatible for an in-place update.

## Acceptance gates

Keep source revision, configuration, signed entitlements, bundle identity and
actual test/build outcomes together in local diagnostics. Do not copy old build
numbers, device IDs, machine timings or successful screenshots into current
acceptance. Recheck native imports/framework embedding, startup, maps, Keychain,
modal/back/cancel, themes/accessibility and real provider capabilities on the
resulting device artifact. Scannerless acceptance cannot certify ONNX scanning.
Release/TestFlight remains the macOS workflow in [IOS_SETUP](IOS_SETUP.md).
