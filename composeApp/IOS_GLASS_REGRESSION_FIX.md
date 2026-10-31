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

- Local code revision `13e84d1d`: `:core:design-system:compileKotlinIosArm64`
  compiled successfully. Android `assembleDirectSandbox` and `assembleDirectDebug`
  passed, including both packaged runtime-class checks and Sandbox isolation.
- Design-system `testDebugUnitTest` was up-to-date: existing reports contain
  50 tests, zero failures/errors/skips. These were reused, not re-executed.
- `:composeApp:compileKotlinIosArm64` is skipped on Windows (MapLibre/host gate).
  Its successful Gradle exit is **not** an iOS application compile result.
- Further inspection found fully native photo/card icon buttons used overlay
  placement, allowing them to escape scrolling/header clips. They now use the
  same shaped underlay mechanism while retaining their native action handling.
- Full iOS build and eight new non-UI contracts are tracked in
  [PR #279](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/279).
  The PR's checks are the source of the final macOS CI result.
- First macOS run `41214021568` failed compiling the app: missing import of
  UIKit's `hidesBottomBarWhenPushed` category extension in the new visibility
  controller. Added the explicit import to the controller and its contract test;
  this failure is retained here separately from the subsequent verification.
- The tab-visibility callback releases its UIKit controller references when the
  root host is disposed, preventing a new controller/content reference cycle.
  Late callbacks from disposed content are covered by a non-UI contract.
- macOS run `41215749066` built the full iOS app and executed 52 native contracts:
  49 passed, three map-visibility assertions failed; all three color contracts
  passed. The visibility condition used Kotlin wrapper identity for UIKit
  controllers. Replaced `===` with native object equality (`==`); the failing
  tests are retained unchanged for the next run.
- No device run: the changes are iOS-only; Android assembly does not establish
  UIKit rendering/touch acceptance. The iPhone matrix above remains open.
