# Shared native iOS sources and xtool launcher

Reviewed against the repository on 2026-11-27. `Sources/CoffeePeek` contains the
SwiftUI/UIKit feature screens and native bridges shared by the Xcode application
and the SwiftPM/xtool launcher. It is not a second business implementation:
feature state/repositories/writers remain in KMP. The active native root is
registered by both bootstraps before `MainViewController` is constructed.

Use [the Xcode guide](../../docs/IOS_SETUP.md) for the normal application and
[the xcross guide](../../docs/IOS_XCROSS.md) for the scannerless Windows Debug path.
Do not assume a historical “iOS Compose Framework” workflow/artifact still exists:
the current workflows are `ios-verify.yml` and `ios-testflight.yml`.

## SwiftPM/xtool prerequisites

The package targets iOS 17+, imports the linked KMP package and expects a matching
`composeApp/build/bin/iosArm64/debugFramework/ComposeApp.framework`. Framework,
Swift sources, resources and package resolution must come from the same source
revision. `Package.swift` and its resolved dependencies are authoritative; a
framework alone does not package/sign an application.

From the repository root:

```bash
bash iosXtool/CoffeePeek/prepare-resources.sh
```

This invokes the launch-catalog/auth preparation and
`:composeApp:assembleIosArm64MainResources --max-workers=6`, then stages Compose
resources under `.build/compose-resources/composeResources/`. On macOS, actool
compiles the shared launch asset catalog. Elsewhere, supply the `Assets.car` from
the same revision's `ios-launch-resources` CI artifact as the first script argument.
Keep the compiled catalog; loose PNGs lose appearance metadata. `xtool.yml` copies
resources/localizations/privacy and merges the generated `.build/Info.plist`.

After resolving the package and configuring an appropriate development signature,
`xtool dev` from this directory builds/signs/installs to the explicitly selected
trusted device. Inspect that tool's target selection before deployment; a generic
command must not choose another connected phone or overwrite a newer app.
Do not uninstall/clear data to recover installation. Successful verification or
installation alone does not establish startup, OAuth, push, Keychain or scanning.

The current source includes `SocialSignInProvider` and GoogleSignIn; actual
availability depends on Debug policy, IDs, URL scheme and signature/capabilities.
Do not describe provider setup as absent solely because a button is hidden.
No xtool build, install or device acceptance was run for this docs update.
