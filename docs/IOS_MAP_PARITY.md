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

- Circular mascot crop, type-specific fill and border, 31 pt normal / 41 pt
  selected pins, selected gold halo. Matches Android's logical sizes and crop.
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
