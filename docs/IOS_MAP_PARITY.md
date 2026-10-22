# iOS map rendering parity

The Android map is the visual reference. Both iOS launchers compile
`iosXtool/CoffeePeek/Sources/CoffeePeek/MapLibreMapProvider.swift` (the Xcode
project references that file directly). No new module or dependency is required.

## Ownership

- Shared map presentation produces the native JSON payload, including selected
  shop, zone shop counts, closed latitude/longitude rings and cluster bounds.
- `mapZoneOutline` supplies the polygon/radius fallback to both platforms. Swift
  converts latitude/longitude pairs directly to `CLLocationCoordinate2D`.
- The Swift provider owns only UIKit/MapLibre rendering and forwards shop/zone
  taps and viewport changes to Kotlin. State and callbacks belong to each map
  instance so embedded detail maps cannot overwrite the full-screen map.

## Rendering and interaction

- Circular white (light theme) / brown (dark theme) pins with contrasting glyphs:
  bean for specialty, cup for coffee bar, fork and knife for cafe. Normal pins
  are 31 pt, selected pins 41 pt with a gold outline, matching Android.
- Reusable annotation views and updates by ID preserve selection across content
  refreshes. Selection has a short scale transition and a 1.6 s repeating halo;
  Reduce Motion replaces animation with a static halo. Offscreen views stop it.
- Zones use fill and line style layers, with pill images for name and shop count.
  They use feature collections to preserve the attributes used for color and
  hit testing. Labels are hidden at zoom 15 and above. Shop views render on top
  and take precedence when hit targets overlap.
- The base map receives the same light/dark palette and noise-layer filtering
  as Android rather than using the stock OpenFreeMap colors.
- Camera defaults: city overview 12, location 15, explicit shop target 16. Targets
  sent before native view attachment, layout or style loading are retained.
  Camera movement takes 450 ms (cluster fit: 350 ms), or is immediate with Reduce
  Motion. Cluster fit uses the same padding and zoom cap as Android.
- UIKit handles pan/pinch directly without Compose's interop gesture delay.

## Verification

### Marker update — 2026-10-22

Scope: native Android/iOS pin rendering, theme-aware image caches, singleton
cluster pins, image resources and the colocated Android light/dark preview.
Selection callbacks, accessibility titles, reduced-motion behavior, clustering
and multi-shop cluster images retain their existing behavior.

The supplied `coffeepeek-bean.svg`, `cup-fill.svg` and `fork-knife.svg` were
rasterized without changing their paths or aspect ratio to transparent 256 px
PNGs. Android uses `drawable-nodpi`; iOS uses the identical Compose drawable
copies through the existing bundle loader. Runtime tint uses the design-system
white/brown palette and `GoldWarm` selected outline; no image library was added.

Verification plan: check asset transparency/platform equality, run map unit
contracts, assemble `directSandbox` and `directDebug` including packaged DEX
checks. Review default/selected pins in both themes using the colocated preview
and manual Sandbox checks when a device is selected. Windows cannot certify
Swift/UIKit compilation, iOS asset loading or device rendering.

Initial source: `codex/map-markers-migration`, parent `90faf57c` (migration stack tip).
Verified the uncommitted marker-update working tree on 2026-10-22:

- `:composeApp:testDirectDebugUnitTest --tests 'com.coffeepeek.admin.map.*'
  --tests 'com.coffeepeek.admin.ui.screen.map.*'`: 18 tests executed across six
  suites, zero failures/errors/skips, no test retries.
- `:composeApp:assembleDirectSandbox` and `:composeApp:assembleDirectDebug`
  passed with `--max-workers=6`, including both packaged runtime-class checks
  and Sandbox isolation. Build version code: `1097`; Debug `1.1.1097`, Sandbox
  `1.1.1097-sandbox`. No Gradle dependencies or source sets changed.
- All three Android PNGs match their shared copies byte for byte; all six
  resource entries are present in each APK. Transparency and 256 px dimensions
  passed the asset self-check.
- Inspected an asset illustration in both themes and selection states.
  The colocated light/dark Compose preview compiled; IDE rendering was
  unavailable because the accessible IDE did not have this project open.
- Initial build-only pass: no device selected or app installed/launched. Manual Sandbox map review,
  font/window-size/accessibility checks, and native Swift/iOS build and runtime
  asset loading remain unverified. No automated UI/screenshot tests were added.
- Existing Gradle/AGP/KMP deprecation and unrelated source warnings remain;
  no adjacent cleanup was included.

Subsequent user-approved read-only Debug check on 2026-10-22:

- Sole connected device `emulator-5554`, `sdk_gphone16k_x86_64`, Android 17,
  unlocked, 1080 × 2400, font scale 1.0. Checked APK package/entry/version and
  matching signing certificate before updating Debug 1092 to 1097 in place.
  Sandbox 1097 was also installed separately but was not launched.
- Opened the public Minsk map as a guest, expanded it, zoomed in, hid zones,
  and selected a specialty shop. Actual map tiles, all three glyph types,
  light/dark marker fills and the selected gold outline rendered successfully.
  Selection stayed active after changing theme. Screenshots remain local,
  outside tracked sources; no automated UI/screenshot tests were introduced.
- Location prompts were cancelled with Back; location access was not granted.
  No account login, favorites, records, consent controls or server writes were
  exercised. Restored the original System/Auto app theme after screenshots.
- This narrow visual check does not cover alternate font/window sizes, screen
  readers, physical hardware, Sandbox UI, iOS compilation/rendering or release.

Develop integration on 2026-10-22: replayed only the marker commit onto
`develop` at `4b717ef4`; `git range-diff` confirmed an unchanged patch. Re-ran
the same 18 map tests (six suites, zero failures/errors/skips) and both
`directDebug`/`directSandbox` assemblies with `--max-workers=6`. Packaged DEX
checks and Sandbox isolation passed again. This final-base verification was
build/unit only; the earlier emulator screenshots came from the initial base.

### Earlier parity verification

On Windows, Android Kotlin and iOS ARM64 Kotlin compilation passed. The 18 map
tests passed, including the new payload/selection/count contract, coordinate
order, polygon closure and radius fallback checks. Swift/UIKit compilation and
device rendering were not executable here: Xcode and an iOS runtime are absent.

Before release, use Xcode/TestFlight to verify:

1. Compare the same center and zoom in both themes; pins, zone borders and labels
   should match the Android reference. Confirm labels hide after zoom 15.
2. Tap a pin, swipe the shop carousel, and tap another pin. Exactly one marker
   enlarges/pulses; refreshing bounds must not restart its animation.
3. Tap a zone label and empty polygon area; then tap a shop inside a zone. The
   shop tap must open/select the shop rather than selecting the zone.
4. Open a detail map while a full-screen map exists, return, change theme, and
   reopen the map. Each map must retain independent callbacks and state.
5. Verify startup with granted/denied location permission, a pending shop target,
   slow style loading, cluster taps, zoom controls, pan and pinch.
6. Toggle Reduce Motion while a marker is selected; the halo becomes static.

API signatures were checked against the MapLibre iOS 6.29.0
[map view header](https://github.com/maplibre/maplibre-native/blob/ios-v6.29.0/platform/ios/src/MLNMapView.h)
and [annotation view header](https://github.com/maplibre/maplibre-native/blob/ios-v6.29.0/platform/ios/src/MLNAnnotationView.h).
