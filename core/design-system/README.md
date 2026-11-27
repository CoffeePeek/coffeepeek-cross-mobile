# CoffeePeek design system

Reviewed against the current source on 2026-11-27. This is the shared UI dependency
used by migrated features, not an unintegrated preparation library. It owns visual
tokens, resources, themed primitives, icon facades, generic controls and shared
visual modifiers. Feature state, validation, business labels, analytics, routes
and persistence stay with their feature/application owners.

The module targets Android, iosArm64 and iosSimulatorArm64. It depends on Compose,
Material 3, Haze and `core/media`; media owns loading/access/decoding, while this
module owns rendering and Compose viewer interaction. Vendor icon access stays
behind `CoffeePeekIcons`. No feature, application, Koin, Room or endpoint DTO
dependency belongs here. Active iOS application screens render in SwiftUI/UIKit;
shared iOS actuals support their narrow visual/platform contracts and are not a
reason to restore a Compose application root.

## API and resources

Reusable component/types/tokens/files use `CoffeePeek`; ordinary functions and
Modifier extensions use lower camel case (`coffeePeekLiquidGlass`), composition
locals `LocalCoffeePeek…`. Feature-specific components keep feature names.
Do not introduce new App/Cp aliases.

`CoffeePeekTheme` uses Android Manrope or the iOS system font by default and
accepts a font override. Active SwiftUI CoffeePeekNativeStyle separately loads
packaged Manrope with Dynamic Type/system fallback. Generated `Res` accessors are internal. Russian base
resources, `values-ru` and Belarusian `values-be` exist; English is not a complete
app locale. Feature-owned copy and accessibility descriptions come from feature
resources. [RESOURCES](RESOURCES.md) records provenance and packaging gaps.

## Component selection

| Need | Existing family |
| --- | --- |
| Actions | CoffeePeekButton; filled/tonal/outlined/text/icon and glass variants |
| Input | CoffeePeekTextField, compact/multiline fields, SearchField, SelectField |
| Choice | CapsuleSegmentedControl, Switch, grouped/checkmark/stepper rows |
| Layout/status | Scaffold, TopBar, settings sections, pagination, loader, error/empty content |
| Images | Image, PhotoBackdrop, PhotoStrip/Attachments and PhotoViewer |
| Modal actions | SwipeDismissModalBottomSheet, destructive confirmation and native menu/dialog contracts |
| Shared cards | RoasterCard and neutral metric/badge primitives |

Inspect the actual component signature before adding another primitive. Data and
callbacks are hoisted: a control never fetches a catalog, owns a repository or
navigates. `SelectField` validates unique option IDs and at most one selection;
loading/retry/optional values remain caller-owned. Single-choice menus are bounded
on Android and use the native platform contract where installed.

## Ratings and affinity

`CoffeePeekRating` renders a normalized tenth in 0..5. A 4.3 value fills four stars
and 30% of the fifth; non-finite input renders zero. Small/Medium/Large glyphs are
16/20/24 dp. Compact means one symbol plus value, not a one-star scale. Read-only
ratings expose one description/range; RTL mirrors fill direction.

Editable Stars emit integer 1..5 choices. The caller owns validation and optional
repeat-tap clearing. The current compact layout uses glyph-sized targets with
4 dp default spacing, packed from the start; it is an explicit exception to the
general action-target policy. Narrow rows scroll horizontally. When an editable
caption is present, its space is reserved before measuring the star strip.
Do not restore the superseded per-star 44/48 target layout as a documentation fix.

`CoffeePeekLikeButton` is a stateless toggle; `CoffeePeekLikeIcon` supports existing
controls and static counts. Selected affinity uses `CoffeePeekColors.Like`,
currently equal to Primary (#FACC15). Unselected outlines use theme foreground or
over-photo white. Plain, compact Glass and FloatingGlass styles are explicit;
visual plates are separate from normal minimum touch targets. Loading disables
dispatch and exposes progress. Feature owners retain counts, account rules,
optimistic state, haptics, analytics and server-backed writes.

## Glass, modal and inset ownership

Android approximates glass with a screen-owned Haze backdrop, lit rim, tint and
shadow. Missing/unsupported blur uses the dense theme-colored fallback. Native
iOS presenters own their system material, using an older-iOS blur/filled fallback.
Do not layer multiple glass backgrounds on one action.

`CoffeePeekPhotoBackdrop` captures only its bounded photo slot; overlay controls
are siblings and cannot sample themselves. Root capture belongs to tab/menu
backdrops, not every in-window control. Do not pass a photo source across a popup
window; MapLibre surfaces do not automatically become Compose backdrop sources.
Popup menus use their separate menu backdrop/readable fallback.

`CoffeePeekSwipeDismissModalBottomSheet` retains handle-only vertical drag,
Back/scrim dismissal and caller-owned visibility. `maxSheetHeightFraction` sizes
inner content; putting fractional fillMaxHeight on the external modal modifier
breaks anchors. Content must scroll within the bounded body. Busy/dismiss policy
is explicit. Native application sheets own their UIKit detents/lifetime.
[INSETS](INSETS.md) describes single-owner safe-area/keyboard consumption.

`CoffeePeekPullToRefresh` receives the same list state as its list, emits refresh,
and owns no network work. The caller publishes refreshing promptly and guards
duplicates. Threshold/cooldown/gesture policy has unit tests. `CoffeePeekLoader`
uses the shared contour animation and progress semantics; it does not start work.

## Verification

[HIG_CONTROLS](HIG_CONTROLS.md) documents sizing/contrast conventions and explicit
exceptions; [ADAPTIVE_LAYOUT](ADAPTIVE_LAYOUT.md) covers larger text and RTL.
Paired light/dark fake previews are colocated with components. Use interactive
preview for modal/stateful examples; compilation does not mean IDE rendering ran.

Current common unit contracts cover tokens, typography/contrast, sizing, rating,
affinity, refresh, image/viewer state and sheet geometry. For scoped code changes:

```shell
./gradlew :core:design-system:testDebugUnitTest :core:design-system:assembleDebug --max-workers=6
```

Do not create/restore/run Compose UI, screenshot or instrumented UI suites. Manual
Sandbox/device review covers themes, larger text/windows, RTL, keyboard, modal
placement/back/cancel and accessibility. Native linking, Dynamic Type and actual
system-material behavior require macOS/device checks. No such build or manual
acceptance is claimed by this documentation-only refresh.
