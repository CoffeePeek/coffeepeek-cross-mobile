# CoffeePeek with xtool

Use the `iosArm64` debug framework or the native `CoffeePeek.app` from the
`iOS Compose Framework` GitHub Actions workflow. The native app artifact is an
unsigned macOS-built validation bundle; installation on a phone still needs a
development signature. The framework path below remains the easiest route with
`xtool` and signs locally on the connected device.

1. Extract the downloaded artifact ZIP. Verify the tar archive against its
   SHA-256 file (the checksum currently refers to `artifacts/<archive>`).
2. From the repository root, extract the tar archive into
   `composeApp/build/bin/iosArm64/debugFramework/`. This directory must contain
   `ComposeApp.framework/ComposeApp`.
3. Download `ios-launch-resources` from the **iOS build verification** workflow
   at the same source revision. Extract its `Assets.car` into
   `iosXtool/CoffeePeek/.build/launch-resources/`, then prepare resources on Linux:

   ```bash
   bash iosXtool/CoffeePeek/prepare-resources.sh
   ```

   You can pass the downloaded `Assets.car` path as the script's first argument
   instead. On macOS with Xcode, preparation compiles the catalog directly.
   The compiled catalog preserves both `LaunchLogo` appearances and the named
   `LaunchBackground` color for the native launch screen and SwiftUI splash.
   Loose PNGs cannot provide these; preparation removes older staged copies.

4. Build, sign, and install on a connected, trusted iPhone:

   ```bash
   cd iosXtool/CoffeePeek
   xtool dev
   ```

The iOS target now links MapLibre Native through Swift Package Manager and
uses the same OpenFreeMap style as Android. The Swift package is resolved by
the native macOS CI job; keep the checkout at the same commit as the framework
artifact when running `xtool`.

The preparation script stages the resources under
`.build/compose-resources/composeResources/`, matching the static framework's
iOS resource reader. Run it again after resource changes or cleaning `.build`.
`xtool.yml` copies this directory into the app and merges
`Info.plist`, including the API URL and privacy descriptions. Google Sign-In
is not configured in this launcher yet.

An installation ending with `Verifying 100%` does not prove the UI has launched.
Open CoffeePeek on the phone to check it. For command-line launch, obtain the
prefixed identifier using `xtool ds identifiers list`, then run
`xtool launch <identifier>`. If debugserver launch fails, manual launch is
still necessary to verify runtime behavior.

The framework is generated on macOS CI; Kotlin/Native on Linux can compile
KLIBs and prepare resources but cannot link this Apple framework.
