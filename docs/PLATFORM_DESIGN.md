# Platform rendering and visual ownership

Current source contract, reviewed 2026-11-27. Shared Kotlin owns feature behavior,
state and infrastructure. Android uses migrated Compose UI; active iOS screens
and navigation use SwiftUI/UIKit through ordinary feature sessions.

The shared [design-system](../core/design-system/README.md) owns neutral visual
primitives/resources; `CoffeePeekNativeStyle` mirrors its palette and metrics in
the native boundary. Android defaults to Manrope. Shared Compose iOS typography
uses the system font, while active SwiftUI style loads the packaged Manrope
weights with Dynamic Type scaling and a system fallback. Do not conflate those
two rendering paths or add platform rendering to domain/data.

## Materials and content

Android glass is a Haze/tint/rim/shadow approximation. Controls use a screen-owned
backdrop where valid; absent/disabled blur uses a dense readable fallback. Popup
menus have their own backdrop ownership; native MapLibre content is not
automatically a Compose Haze source. A bounded photo source cannot cross a popup
window or capture its own overlay.

Native iOS presenters use system glass when available, with older-system
material/filled fallbacks. UIKit owns tabs, navigation transitions, native menus,
modal detents and safe-area delivery. Keep one material layer per control and
readable content surfaces. SwiftUI app views do not need a Compose wrapper to
access KMP state. [Native navigation](IOS_NATIVE_NAVIGATION.md) and
[root/session contracts](IOS_NATIVE_ROOT_SESSION_CONTRACTS.md) describe lifetime.

Current palette source is `CoffeePeekColors` (Primary/Like #FACC15, light background
#FAFAF9, dark background #1A1412), not historical design-plan hex values. Consume
semantic theme roles rather than copying arbitrary alpha/hex values into features.
Use existing resources, normal action targets and explicit compact exceptions
from [HIG_CONTROLS](../core/design-system/HIG_CONTROLS.md).

## Integration and verification

Keep callbacks, selected/busy/disabled states, localizable labels, analytics and
accessibility in their current owners when changing rendering. Migrate the actual
screen, profile preview and modal entries; retire an obsolete fallback with its
last verified consumer. No new global renderer, base-screen/module or hidden
Compose root is needed.

For code changes, execute relevant non-UI unit/contracts, compile affected modules,
assemble Android with DEX checks and link the native app. Run host Gradle builds
sequentially with `--max-workers=6`. Do not add/run UI, instrumented Compose or
screenshot suites. Preview compilation and source review do not establish rendered
acceptance. Manual selected-environment checks cover both themes, larger text/
windows, RTL, keyboard/back/cancel, accessibility, reduced motion/transparency
and older-iOS material fallback. No such new acceptance is claimed by this docs pass.
