# Design-system preparation

Responsibility: reusable visual tokens, themed primitives, icon facade and shared
visual modifiers. Allowed consumers: application composition and feature UI.
Dependencies: Compose runtime/UI/foundation/Material3, Haze and Phosphor icons.
No application, feature, domain, data, navigation, Koin, Room or Ktor dependencies.
Compose/Haze types are exported because public APIs expose them; the icon vendor
is hidden behind `CpIcons`. An independent Gradle boundary allows future features
to reuse UI without depending on the application or accidentally accessing its
business models. No umbrella utility or base-screen abstraction is introduced.

## First slice

`theme/`: shared palette/dimensions, light/dark Material theme and typography.
Auth-specific decoration and application header/bottom-navigation metrics remain
outside core. CoffeePeekTheme now defaults to the six packaged Manrope weights,
while allowing explicit FontFamily overrides for callers/tests. The generated
resource accessors are internal to design-system, with no app-resource dependency.
Legacy font copies remain in composeApp until consumer integration.

`icons/`: existing Phosphor-backed `CpIcons` facade, including intentional aliases.
Feature mappings (brew methods, prices and ratings) stay with feature presentation.

`component/`: AppButton and generic grouped section/checkmark/action/switch/stepper
rows. The CatalogItem-based CheckmarkSection is deliberately excluded. Stepper
accessibility descriptions must be supplied by the caller, not hardcoded in core.
AppButton uses a minimum height instead of a fixed height so text can grow with
font scale. Brand styles are otherwise retained; screens are not redesigned.

`modifier/`: liquidGlass, its optional Haze composition local and GlassIconButton.
No backdrop source means the same translucent fallback as the legacy component.
Haze ownership belongs to the screen; no global renderer or platform abstraction
is created. Shared UI lives in commonMain with Android as the initial build target.
Native iOS integration is outside this stage.

## Fields and top bars slice

CompactOutlinedTextField exposes the basic decoration contract. AppTextField adds
the brand label/error style, accessible field label/error, enabled/read-only state
and caller-configured keyboard actions. Ordinary fields default to a text keyboard,
not email. Password visibility is caller-owned; callers supply the localized toggle
description matching that state and handle onPasswordVisibilityChange.

CpSearchField keeps query state outside the component, emits search/clear actions
and requires a localized clear description. Disabled/read-only search cannot clear
the query. Fields use minimum rather than fixed heights; search has a 48.dp clear
target. CpTopBar has no default back action or Navigator dependency. Its caller
supplies the back callback/description; the back glyph mirrors in RTL.

These APIs prepare existing visual families, not search/auth business rules.
Focus handling on the search IME action remains local UI behaviour.

## Handle-dismiss sheet and floating actions

SwipeDismissModalBottomSheet preserves the legacy handle-only vertical drag:
Material sheet-body gestures are disabled; a 72.dp downward distance or
900.dp/s downward velocity dismisses, and short drags animate back. Back and
scrim dismissal remain Material behaviour. The handle is now 48.dp and exposes
a caller-localized accessible dismiss action. Visibility/removal is caller-owned.
Content is a ColumnScope slot: review/check-in/photo-source models stay outside.
Material provides modal focus and default system/IME inset handling; validate
keyboard/content scrolling and any custom consumer insets at integration.

FabMenu accepts neutral icon/description/callback/enabled actions, not routes or
feature models. It retains joined rounded corners and the existing success
palette, with minimum 48.dp action targets. The legacy component currently has
no call sites; this is a small prepared primitive, not a reason to create a module
or add a menu to existing screens.

## Pull-to-refresh

CoffeePeekPullToRefresh is prepared independently from the three legacy feed
consumers (normal list, error and empty states). Pass the same LazyListState
to the component and LazyColumn, and attach its scrollModifier to that list.
The caller owns isRefreshing, request deduplication, failures and cancellation;
this component emits onRefresh only and never calls a repository.

The legacy visual defaults remain: 72.dp threshold, 0.5 drag resistance, 1.4x
offset cap and branded loader. Cooldown is an explicit finite nonnegative
Duration (default one second), shared by pull and accessible refresh actions.
It throttles UI signals, not network jobs. A caller must publish isRefreshing
promptly and prevent duplicate work at its own boundary. Zero cooldown is allowed.
At the exact cooldown boundary another signal is allowed (legacy used strictly
greater than one second).

Only user-input downward scroll remaining at the top contributes to a pull.
Upward movement retracts it; release below threshold or away from top resets
without a request. Disabled/refreshing states block requests. Active pull release
consumes only vertical fling velocity, never horizontal/ordinary list flings.
Like legacy, visual offset resets immediately on release/completion; this stage
does not introduce new settle animation or platform pull-to-refresh behaviour.

Threshold/density/cooldown changes rebuild the private gesture policy, resetting
its pull and cooldown; updated list/callback/enabled/loading values are read live.
This corrects stale-density and non-user-scroll handling in the legacy copy.
Strings are caller supplied, with determinate pulling vs indeterminate loading
semantics and a custom accessibility refresh action (also available away from top).

Common unit tests exercise drag/cap/retraction/threshold/cooldown and invalid
configuration. Android tests exercise real list pulls, short/away-from-top pulls,
disabled/running states, caller completion and accessible action throttling.
Interactive Preview covers idle/refresh completion; a second light/dark fixture
shows the running indicator. IME/RTL/horizontal nesting, density changes during
an active gesture and pixel parity still require consumer-level integration QA.

## Insets and RTL recipes

No new public Insets wrapper is needed: use Compose foundation's direction-aware
padding/size modifiers and explicit consumption at Scaffold/list boundaries.
The unused legacy Insets object is not copied or modified. INSETS.md documents
ownership, horizontal safeDrawing, exact logical spacers and keyboard policy.
`CpTopBar.kt` keeps paired LTR/RTL light/dark inset fixtures with synthetic asymmetry;
InsetsRecipeTest checks measured geometry, consumption, density and inset updates.
Real system bars/IME/modal integration and large-font screen QA remain separate.

## Light/dark previews

`GroupedRows.kt` keeps LTR/RTL light/dark fixtures at fontScale 2.0. Stepper
actions now have growing 48.dp minimum targets rather than fixed 32.dp height.
Grouped rows use a 48.dp minimum; action rows/stepper halves expose button roles.
ADAPTIVE_LAYOUT.md documents the targeted measured-layout/semantic tests and
preserved ellipsis policies. Large-font screen, OS scaling and pixel parity QA
remain integration responsibilities; this is not a complete accessibility audit.

Open a component's own source file in Android Studio Design/Split mode. Every
prepared family has colocated paired multiplatform `@Preview` functions with
explicit `CoffeePeekTheme(darkTheme = false/true)`, including fields/error/disabled
states, grouped rows, badges, segmented control, loader, sheet, dialogs and FAB.
`CpTopBar.kt` also has LTR/RTL inset recipes; `GroupedRows.kt` has LTR/RTL
large-font fixtures. Refresh has both idle/interactive and running samples.
Previews require no app DI, network, navigation or data. The Compose preview
annotation is in commonMain; Android Studio's rendering tooling remains a
debug-only Android dependency. This module currently has an Android target only.

Use Interactive Preview / Run Preview for modal windows and stateful callbacks;
static layout previews are not guaranteed to display separate dialog windows.
Compilation is checked, but IDE rendering and brand-font visual parity require
manual inspection. After each slice, review both themes before integration.
Keep paired light/dark coverage up to date whenever adding a reusable UI family.

## Migration and verification

Presentation slice: IconBadge/its palette are neutral visual primitives, reused
by settings and contribution UI. SettingsSection/Row/Divider exclude AppVersionFooter:
version lookup and localization stay in composition. Section titles are rendered
as supplied (callers decide uppercase); row callbacks and enabled state are explicit.
Directional row chevrons mirror in RTL. CapsuleSegmentedControl exposes tab
selection, accepts enabled state and requires nonempty unique options with a valid
selection. Its minimum-height targets can grow with typography.

CoffeePeekLoader retains the existing animation and exposes localized indeterminate
progress semantics; it does not start work. ErrorDialog requires all user-facing
strings and emits dismiss. LoadingDialog visibility is caller-owned and uses a
non-dismissible modal instead of the legacy touch-consuming full-screen overlay.
This is an intentional new contract, not a drop-in visual/interaction replacement.
At integration check back handling, modal sizing/focus and cancellation UX explicitly.
There are no timers, global loading/error state or Navigator dependencies.

Existing components/resources/screens remain unchanged and have no dependency on
this module. New independent copies are a temporary migration boundary, not a
second long-term source of truth. Integrate one consumer family at a time,
check light/dark rendering, accessibility, RTL and font scaling with the brand font,
then remove legacy implementations when unused. See AUDIT.md for remaining slices.

Unit tests cover tokens, font injection and icon aliases. Android instrumentation
tests render buttons, a grouped checkbox and the glass fallback, verifying labels,
enabled state and callbacks. These are behavioural contracts, not pixel-perfect
parity tests or verification of Haze backdrop rendering and every icon.
Input tests additionally cover hoisted editing, error semantics, disabled fields,
password action labels/state, search IME/clear/read-only behaviour and explicit
top-bar back actions. Large-font/RTL screen layouts require integration QA.

Presentation tests cover enabled/selected actions, settings callbacks, progress
semantics, error dismissal and modal back/visibility behaviour. They do not
prove pixel parity, all animation frames or outside-touch geometry.
Overlay tests cover accessible dismissal, the modal Back dispatcher, long/short
handle drags and enabled/disabled FAB callbacks. Direct system-key injection on
the API 37 emulator did not close the Material modal; dispatcher testing does not
prove system/predictive gesture routing. That remains an explicit integration QA
item, along with velocity-only flings, scrim geometry, IME and nested scrolling.

```shell
./gradlew :core:design-system:testDebugUnitTest :core:design-system:assembleDebug
./gradlew :core:design-system:connectedDebugAndroidTest
```

Future consumers use `implementation(project(module.core.designSystem))`.
