---
name: mobile-design
description: Apply CoffeePeek's existing Compose and native iOS design contracts without introducing a parallel design system.
---

# CoffeePeek mobile design

Read [AGENTS.md](../../../AGENTS.md), the owning feature README and the current [design-system](../../../core/design-system/README.md). Inspect the runtime entry: Android Compose, native SwiftUI/UIKit, profile preview and modal variants may have different hosts.

## Before a change

1. Identify the requested behavior and platform from the session and code. Reuse already authorized choices; ask only when information actually blocks the task.
2. Inspect shared tokens/components and existing native primitives before introducing a feature component.
3. Preserve navigation, session/consent, cancellation, analytics, accessibility and saved data.
4. Keep strings and descriptions in the owner's resources; verify supported locales and safe text scaling.

## UI review

Use the existing CoffeePeek palette, typography, spacing, icons and glass controls. Respect system safe areas, keyboard, Reduce Motion, higher-contrast/opaque treatment and actual touch targets. Keep native menu/sheet/popover behavior consistent with platform hosts. Do not use React Native/Flutter/web examples as implementation guidance for this KMP app.

Shared Compose components have colocated fake light/dark previews. Review layout, privacy, Back/Cancel and modal placement manually in an explicitly chosen [Sandbox](../../../composeApp/SANDBOX.md). Native/device acceptance is a separate gate; compilation and a fake backend do not certify real OAuth, camera, push or persistence.

Use [Android run modes](../../../composeApp/ANDROID_RUN_MODES.md), [native iOS navigation](../../../docs/IOS_NATIVE_NAVIGATION.md) and [verification flow](../../../docs/VERIFICATION_FLOW.md). Do not create/run automated UI, instrumented Compose or screenshot tests. The old generic reference library and its unavailable audit-script command were retired; repository contracts are the maintained source.
