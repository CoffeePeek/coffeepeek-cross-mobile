# Insets and layout-direction recipes

## Decision and ownership

Do not add a generic public `Insets`, safe-area wrapper or base screen to core.
Compose foundation already provides direction-aware, density-aware inset APIs.
The design-system exports foundation; no dependency or new module is needed.

Screens own edge-to-edge policy, which edges their
chrome handles, scrolling and keyboard behaviour. Android application composition
owns Activity edge-to-edge/soft-input configuration. The later shared-sheet
policy is recorded below; no iOS implementation is introduced.

## Prefer layout-phase padding

For a screen whose top/bottom chrome already handles vertical insets:

```kotlin
Box(
    Modifier.windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    )
) { /* screen content */ }
```

`Horizontal` preserves physical cutout/bar edges in both directions. Use
`WindowInsetsSides.Start`/`End` only when deliberately protecting a logical edge.
Prefer `safeDrawing` for protection from visual obstruction; choose `safeContent`
only when the consumer also needs gesture protection. Do not make that larger
inset a global design-system default. For all edges use `safeDrawingPadding()`.
Apply padding inside a background if the background should draw behind bars.

Nested `windowInsetsPadding` automatically accounts for consumed insets. Do not
replace it with remembered raw pixel-to-dp values or repeated ordinary padding.
No app-global size observer or hardcoded `LayoutDirection.Ltr` is needed.

## Scaffold and lazy content

If a Scaffold supplies `innerPadding`, apply and consume it at the content owner:

```kotlin
Box(Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
    // Descendants can use windowInsetsPadding for still-unconsumed edges.
}
```

For a LazyColumn that intentionally uses Scaffold padding as `contentPadding`,
also attach `Modifier.consumeWindowInsets(innerPadding)` to the list. This does
not apply padding; it informs descendants about the spacing the list applies.
Don't also add the same ordinary padding outside it. IME handling and list-end
spacers still need a consumer-specific scrolling policy.

`asPaddingValues()` returns raw, unconsumed insets. Use it only for APIs requiring
PaddingValues, with explicit consumption where appropriate. If converting to
logical start/end, use `calculateStartPadding(LocalLayoutDirection.current)` and
`calculateEndPadding(...)`, never left/right computed with a fixed LTR direction.
Prefer modifiers for animated insets, which read their values during layout.

## Exact-size inset surfaces

When a consumer actually needs a separate bar/side surface, use:

```kotlin
Spacer(Modifier.fillMaxHeight().windowInsetsStartWidth(insets))
Spacer(Modifier.fillMaxHeight().windowInsetsEndWidth(insets))
Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(insets))
Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(insets))
```

Add caller-owned background and size constraints appropriate to the parent.
Start/end swap physical edges in RTL; a spacer occupies one inset width, not
symmetric padding twice that width. These modifiers respect prior consumption
but do not consume insets themselves. Subsequent sibling content must not apply
the same edge again; if it handles insets, explicitly consume the space already
provided at that content boundary.

## Keyboard and modal boundaries

Use `imePadding()` at the scrolling/content boundary that owns keyboard avoidance.
Nested inset padding avoids re-applying bars already covered by the IME. If
combining inset types explicitly, `navigationBars.union(ime)` takes the maximum
per edge, whereas `add` would sum overlapping protection. Do not blindly combine
outer keyboard padding, Scaffold padding and Material modal defaults.

The shared `CoffeePeekSwipeDismissModalBottomSheet` explicitly supplies all-edge
`WindowInsets.safeDrawing` through Material's `contentWindowInsets` callback.
Material's bundled Android default protected top/bottom only; the explicit policy
also protects horizontal cutouts. Material applies and consumes content insets
and owns dialog IME avoidance. Do not add raw navigation-bar heights or a second
outer IME spacer. Nested consumption-aware padding may still protect remaining
edges. The callback can be overridden for a deliberately different content policy.
Native iOS application sheets have their own UIKit/SwiftUI safe-area ownership.
Fractional height stays on inner content, preserving Material's bottom anchors.
This does not change Activity configuration or system/predictive Back policy.

## Verification boundary

Reviewed against current source on 2026-11-27. Colocated previews provide fake
LTR/RTL and asymmetric-inset examples. The older measured Compose UI fixtures
and their test hosts were removed; their historical pass/failure logs are not
current executable checks. Unit tests such as CoffeePeekScaffoldInsetsTest
cover pure policy only. No new build, preview rendering or device run was
performed for this documentation update.

Manually verify safe drawing, cutouts, RTL, keyboard transitions, nested scrolling,
modal sizing and Back/cancel in the actual chosen Sandbox/device environment.
Native iOS application controllers own their safe areas and keyboard behavior;
these Compose recipes do not authorize restoring old Compose root containers.
