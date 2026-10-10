# Insets and layout-direction recipes

## Decision and ownership

The legacy `composeApp/.../ui/component/Insets.kt` has no external call sites in
the current source tree. It hardcodes LTR, depends on app `SizeObserver` and
uses symmetric padding to size side boxes (twice the requested side width).
Do not copy it or add a public `Insets`, safe-area wrapper or base screen to core.
Compose foundation already provides direction-aware, density-aware inset APIs.
The design-system exports foundation; no dependency or new module is needed.

This slice provides executable Android fixtures and recipe regression tests,
not a new production API. Screens own edge-to-edge policy, which edges their
chrome handles, scrolling and keyboard behaviour. Android application composition
owns Activity edge-to-edge/soft-input configuration. Nothing is integrated into
composeApp, and no iOS implementation is introduced.

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

The prepared sheet retains Material's default inset handling. This slice does
not alter sheet behaviour, Activity configuration, navigation or any old screen.
System/predictive Back routing on API 37 remains unverified (see README).

## Verification and limits

`CpTopBar.kt` supplies separate LTR/RTL paired previews with
asymmetrical synthetic dp insets, nested horizontal padding, logical side
spacers and a synthetic keyboard. They work without platform bars, app DI or
network and are not screenshots of an actual edge-to-edge screen.

`InsetsRecipeTest` checks measured pixel geometry for physical edges in LTR/RTL,
exact logical spacer widths, nested consumption, Scaffold-style consumption,
zero remaining spacer width, density conversion, mutable inset remeasurement,
and overlapping keyboard/navigation protection. These lock down the chosen
recipes using the repository's actual Compose versions; they do not prove OS
inset delivery, real IME animation, rotation, cutouts or screenshot parity.

Before integrating a consumer, verify gesture/three-button navigation, keyboard
open/close and focus scrolling, landscape/cutouts, LTR/RTL, large font scale,
light/dark brand-font layout, and modal Back/scrim behaviour on actual screens.
Legacy Insets may be deleted only in a separately scoped integration/cleanup PR.

Reference: [Compose inset sizing and consumption](https://developer.android.com/develop/ui/compose/system/insets-ui).
