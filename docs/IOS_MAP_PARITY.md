# Native iOS map rendering

Current source guide, reviewed 2026-11-27. Both application launchers use
`iosXtool/CoffeePeek/Sources/CoffeePeek/MapLibreMapProvider.swift`; Xcode references
that shared native source directly. Android rendering lives in the application
map bridge; shared Discovery state/criteria remain in KMP.

## Ownership and updates

Kotlin provides the native payload: shops/selection, clusters/bounds and zones with
closed latitude/longitude rings/counts. The shared zone-outline policy owns the
polygon/radius fallback. Swift renders MapLibre objects and forwards taps, camera
center and viewport changes; it does not query endpoints or duplicate favorites,
city, location-permission or filter policy.

Each `CoffeeMapView` retains independent callbacks/content/annotations/pending
camera. Embedded maps cannot overwrite the full-screen map. Targets arriving
before attachment, layout or style load remain pending, and nonzero bounds are
reported once layout/style are ready. `DiscoveryMapLayout` covers first-render
geometry independently from feature network results.

Annotations are reused by stable ID. Movement/enter/exit transitions retain
per-marker generation identity; a late removal cannot delete a replacement pin.
Selection, theme and cluster updates preserve their explicit state. Reduce Motion
and view reuse stop/replace animations; do not restart a pulse on every payload.

## Appearance and interaction

Light/dark pins use CoffeePeek's type glyphs and gold selection treatment.
Selected-shop markers, clusters, zone fill/outline and label pills are distinct.
Shop taps take precedence over an overlapping zone. Zones retain their hit-test
attributes; labels have their configured zoom visibility. Theme palette/noise
filtering is applied to the loaded style rather than relying on stock map colors.
Native UIKit/MapLibre owns pan and pinch without a Compose interop gesture delay.

Camera focus, cluster fit, controls and expanded-map bar policy flow through the
existing native/shared contracts. Do not hardcode a selected shop or user position
in Swift to mask missing state. Permission revocation clears location-dependent
origins/distances; chosen-city context is independent of fresh GPS permission.

## Verification gates

Use existing common/native payload, bounds, transition and selection contracts;
include new tests in the actual iOS workflow selection. Full Swift/Kotlin app
linking is separate from compiling a shared KLIB.

Manual selected-device review must cover both themes, stable selection after
refresh/theme changes, pin/carousel synchronization, clusters/zones and overlap,
independent detail/full maps, pending focus with slow style loading, first render,
permission deny/grant/revoke/foreground recovery and reduced motion. Network tiles,
real permission interaction and screen-reader behavior are not certified by unit
tests. No fresh map/device check ran for this documentation-only refresh.
See [native lifetime](IOS_NATIVE_ROOT_SESSION_CONTRACTS.md) and
[setup](IOS_SETUP.md).
