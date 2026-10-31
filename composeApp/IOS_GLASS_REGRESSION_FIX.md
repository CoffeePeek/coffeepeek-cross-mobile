# iOS glass regression — 2026-10-31

Reported against the current main/TestFlight build: rectangular black areas around
rounded controls, unresponsive filters, system-blue selected buttons and a tab bar
over the expanded discovery map. Base: develop `6478430e` (same tree as main
`e963f6dc`). User screenshots remain local and are not part of this record.

## Scope and verification plan

- Preserve the brand palette, typography, native Liquid Glass and existing action,
  navigation and analytics callbacks. No Android redesign or feature migration.
- Correct the iOS design-system interop cutout and decorative touch ownership;
  keep native views below Compose where foreground content/clipping requires it.
- Set explicit UIKit tint and selection configuration for favorite/map buttons.
- Connect expanded-map visibility to the native tab bar and bottom clearance,
  including push/pop and tab changes.
- Run design-system unit tests, Android Sandbox and Debug assemblies (including
  packaged DEX checks), and compile the design-system iOS KLIB locally.
- Run non-UI UIKit selection/navigation contracts and the full iOS Simulator build
  on the macOS CI runner. No automated UI or screenshot tests.

## Device acceptance still required

Windows cannot verify UIKit rendering or physical-device touch dispatch. On an
iPhone, check light/dark themes, filters/search/FAB/menu/copy actions, selected
favorites and map layers, scrolling beneath headers, expanded map close/push/back,
large text and VoiceOver. Existing colocated light/dark component previews remain
available; previews and Android checks do not certify UIKit behaviour.

## Results

Pending implementation and checks.
