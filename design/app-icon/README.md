# Application icon

Owner-supplied artwork, updated 2026-11-27: the coffee-bean mascot in a cream cup on a flat `#2D241F` background. `source.png` is the unchanged 1028×1028 PNG; SHA-256 `02fd8dc092aadec764c5d5f9387b067c206edf4dcdf6b0d1481061ec700d31b8`.

## Generated resources

- iOS: `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png` and `AppIcon-1024-dark.png`; identical opaque RGB 1024×1024 PNGs. Keep the complete square artwork; iOS applies its own mask. Existing catalog and Xcode/xcross selection remain valid.
- Android adaptive foreground: artwork centered at 66dp on a 108dp brown canvas, exported to the existing five density directories for both light/dark. Background resources use the same `#2D241F`.
- Android fallback square/round WebP: all existing five densities and light/dark variants, derived from the adaptive icon's central 72dp viewport. Play Store artwork remains an opaque 512×512 PNG.
- Debug: the same supplied art inside the existing 264px disk on a 432px transparent canvas, retaining the gold outer launcher background. Sandbox keeps its separate purple environment marker.

Scaling is deterministic Lanczos resampling; the supplied illustration is not redrawn. Splash logos and monochrome notification masks have separate purposes and are unchanged. Shared iOS asset selection and the alternative xtool packaging are described in [iOS setup](../../docs/IOS_SETUP.md) and [xtool host](../../iosXtool/CoffeePeek/README.md).

## Verification record

2026-11-27, Windows, source base `25728581f` plus these asset changes:

- Source SHA, iOS 1024/RGB/no-alpha/PNG CRC and complete-image provenance checked; Android dimensions, day/night pairs and palette checked. Local asset-only preview reviewed for square and round masks, including Debug's gold border.
- Existing non-UI configuration/packaging tests: 14 passed (4 presentation config, 5 xtool launch resources, 5 xcross build fixtures). The first xtool run had three failures because restricted Git Bash could not create its MSYS named object (`0xC0000022`); the unchanged fixture suite passed outside that restriction. No production compiler or device ran in those fixture tests.
- Android `assembleDirectDebug` passed with packaged DEX/runtime checks. Initial assembly: 1m39; final incremental assembly after straight-alpha Debug edge cleanup: 15s. APK `com.coffeepeek.debug` / `1.2.2.3451-debug` / code `3451`. Decoded main and Debug icon pixels in the built APK match the final resources; the manifest selects the Debug icon. Existing Compose-preview/expect-actual and AGP deprecation warnings remain.

No installation, app launch, account operation or automated UI/screenshot test is part of this change. Actual launcher cache refresh and signed iOS archive/device appearance remain separate runtime gates; PNG validation and fixture tests do not certify them.
