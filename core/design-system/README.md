# Design-system preparation

As of 2026-10-20, the agent-authored instrumented UI suites and their hosts are
removed at the user's request. Earlier test descriptions/results below are
historical. Unit tests and colocated previews remain; use manual Sandbox checks
for current visual verification. See [removal scope](../../feature/UI_TEST_REMOVAL.md).

Responsibility: reusable visual tokens, themed primitives, icon facade and shared
visual modifiers. Allowed consumers: application composition and feature UI.
Dependencies: Compose runtime/UI/foundation/Material3, Haze and Phosphor icons.
No application, feature, domain, data, navigation, Koin, Room or Ktor dependencies.
Compose/Haze types are exported because public APIs expose them; the icon vendor
is hidden behind `CoffeePeekIcons`. An independent Gradle boundary allows future features
to reuse UI without depending on the application or accidentally accessing its
business models. No umbrella utility or base-screen abstraction is introduced.
On iOS, `CoffeePeekNativeActionMenu` provides a glass UIKit button with an anchored
system menu. Features supply localized labels and callbacks; glass styling and
theme mapping reuse the same implementation as `CoffeePeekGlassButton`.

## Resource language

The application and Discovery currently ship Russian base copy, not a complete
English locale. Design-system fallback copy follows that same Russian policy,
including semantics and loading/retry messages. `values-ru` stays aligned with
`values`; an English system language must not produce isolated English labels
inside the Russian UI. Do not claim English support until the application,
features and shared components all provide it. OS-owned accessibility phrases
may still follow the device language. Features supply business-specific labels.

## Naming

Shared components, supporting UI types, tokens and Kotlin source filenames use
the `CoffeePeek` prefix. Examples: `CoffeePeekButton.kt`, `CoffeePeekTextField`,
`CoffeePeekTopBar`, `CoffeePeekIcons`, `CoffeePeekDimens`, `CoffeePeekColors`.
Ordinary functions/Modifier extensions follow lower camel case:
`coffeePeekTypography`, `coffeePeekLiquidGlass`; the glass composition local is
`LocalCoffeePeekGlassHazeState`. The packaged Manrope family is exposed as
`CoffeePeekFontFamily`; font binaries, resource identifiers and license text keep
their original names. Legacy application components are separate and unchanged;
feature-owned UI keeps its feature name. No old-name forwarding aliases are added.
Consumer imports, colocated previews and test source now use the renamed API;
build/test/preview execution for this rename is deferred until the current
Android feature implementation is ready, as requested.

## First slice

`theme/`: shared palette/dimensions, light/dark Material theme and typography.
Auth-specific decoration and application header/bottom-navigation metrics remain
outside core. CoffeePeekTheme now defaults to the six packaged Manrope weights,
while allowing explicit FontFamily overrides for callers/tests. The generated
resource accessors are internal to design-system, with no app-resource dependency.
Legacy font copies remain in composeApp until consumer integration.

`icons/`: existing Phosphor-backed `CoffeePeekIcons` facade, including intentional aliases.
Feature mappings (brew methods, prices and ratings) stay with feature presentation.

`component/`: CoffeePeekButton and generic grouped section/checkmark/action/switch/stepper
rows. The CatalogItem-based CheckmarkSection is deliberately excluded. Stepper
accessibility descriptions must be supplied by the caller, not hardcoded in core.
CoffeePeekButton uses a minimum height instead of a fixed height so text can grow with
font scale. Brand styles are otherwise retained; screens are not redesigned.

`modifier/`: coffeePeekLiquidGlass, its optional Haze composition local and CoffeePeekGlassIconButton.
No backdrop source means the same translucent fallback as the legacy component.
Haze ownership belongs to the screen; no global renderer or platform abstraction
is created. Shared UI lives in commonMain. Android was the initial build target;
the rating slice also declares the application's existing iosArm64 and
iosSimulatorArm64 targets so shared application UI can consume this module
directly. No native screen/navigation/DI migration is implied by Native compilation.

## Single-choice fields

`CoffeePeekSelectField` is the shared trigger and selection contract for city,
consumed drink and home brewing-method fields. `CoffeePeekSelectOption` contains
only presentation identity, localized label, selection, optional Compose icon and
callback; catalogs, optional choices, saved values, loading/retry and validation
remain feature-owned. Unique IDs and at most one selection are checked before rendering.

Android uses one bounded, scrollable Material dropdown anchored to the button;
iOS uses the same trigger with a UIKit single-selection menu, following the
check-in drink selector. UIKit owns touch and VoiceOver, including field label,
value and error hint; optional Compose menu icons are Android-only. Disabled or
empty fields cannot open a menu. The trigger grows with text and has a minimum
48.dp touch target. Error text remains caller-owned.

Design-system now supplies Android, iosArm64 and iosSimulatorArm64 variants so
legacy shared forms and migrated Android shop UI consume the same primitive.
No feature/domain/platform-app dependency or new module is introduced. The app's
design-system dependency therefore lives in commonMain. Command/action menus
retain their separate platform component.

Paired previews live with the primitive and each replaced field. Use Interactive
Preview to open the Android popup; static previews show the trigger only.
UIKit menu placement, VoiceOver and iOS framework linking require a macOS/device
check; Windows Android builds do not verify them.

Verified on 2026-10-20: design-system (19), shop impl (96) and DirectDebug app
(438) unit tests pass; `:composeApp:assembleDirectDebug` and its packaged DEX
runtime-class check pass. Both `:core:design-system:compileKotlinIosArm64` and
`:composeApp:compileKotlinIosArm64` pass. No connected device was available;
IDE preview rendering and final iOS framework linking were not run.

CoffeePeekSwitch owns the already-prepared production switch colors and a full-size
thumb for enabled/disabled, checked/unchecked controls in both themes. Its palette
matches the application's current PlatformSwitch; core does not import that
application implementation. Shop visibility and CoffeePeekSwitchRow use this one
primitive. State/callbacks and localized labels are caller-owned; rows clear the
child switch's semantics so the whole row remains the only accessible toggle.

## Shared rating

`CoffeePeekRating` owns the star rendering used by Shop/History/Home and the
shared legacy feed/profile/review UI. Feature wrappers retain their rating
averages, category descriptions, validation, state/actions and clear-on-repeat
policy. Core accepts only a number and visual/accessibility configuration, never
a feature/domain model. No new module or platform abstraction is introduced.

Read-only `Stars` uses five glyphs with fractional clipping, not integer or
half-star rounding: 4.3 is four full stars and 30% of the fifth. Values are
normalized to the nearest tenth for both default label and fill; non-finite
values render zero and finite values clamp to 0..5. Existing feature code still
decides whether an absent/invalid rating should be shown at all. The fill direction
mirrors with RTL. `Small`/`Medium`/`Large` are 16/20/24dp glyphs, not different
rating algorithms. `Compact` is one rating symbol with a number (used by the
favorites badge), not a one-star scale. Other standalone decorative stars or
information-card icons need not become rating controls.

Supplying `onRatingChange` enables integer 1..5 input with five selected/disabled
accessible buttons. Targets are 44dp high and share up to 224dp width, shrinking
equally in narrow containers; glyph sizes do not silently shrink. Ripple stays
inside each rounded target. The feature supplies its localized per-score labels;
fallback rating/choice resources are English with Russian translations. Read-only
ratings expose one description/range, not five misleading accessibility targets.
That group merges the visible numeric label instead of clearing its semantics,
so metadata remains observable both by accessibility and normal UI queries.
Paired previews are colocated in the component and changed feature owners.
Verification/failure history: [RATING_PLAN.md](RATING_PLAN.md).

## Shared likes and favorites

Use `CoffeePeekLikeButton` for stateless favorite/like toggles and
`CoffeePeekLikeIcon` inside existing glass controls and read-only counters.
Selected hearts use the same gold (`Like`, #B68A2E) on light/dark surfaces and
photos. Unselected outlines use the theme's neutral foreground, or white with
`overImage`. Disabled icons are muted. Do not tint
affinity with `Error`, arbitrary primary/foreground colors or duplicate heart
rendering in features. Keep feature actions, counts, loading, descriptions,
authentication, haptics and analytics in their existing owners. Containers,
button/heart sizes and localized labels remain caller-configurable; paired
previews live beside each component. See [LIKE_PLAN.md](LIKE_PLAN.md).

## Fields and top bars slice

CoffeePeekCompactOutlinedTextField exposes the basic decoration contract. CoffeePeekTextField adds
the brand label/error style, accessible field label/error, enabled/read-only state
and caller-configured keyboard actions. Ordinary fields default to a text keyboard,
not email. Password visibility is caller-owned; callers supply the localized toggle
description matching that state and handle onPasswordVisibilityChange.

CoffeePeekSearchField keeps query state outside the component, emits search/clear actions
and requires a localized clear description. Disabled/read-only search cannot clear
the query. Fields use minimum rather than fixed heights; search has a 48.dp clear
target. CoffeePeekTopBar has no default back action or Navigator dependency. Its caller
supplies the back callback/description; the back glyph mirrors in RTL.

These APIs prepare existing visual families, not search/auth business rules.
Focus handling on the search IME action remains local UI behaviour.

## Handle-dismiss sheet and floating actions

CoffeePeekSwipeDismissModalBottomSheet preserves the legacy handle-only vertical drag:
Material sheet-body gestures are disabled; a 72.dp downward distance or
900.dp/s downward velocity dismisses, and short drags animate back. Back and
scrim dismissal remain Material behaviour. The handle is now 48.dp and exposes
a caller-localized accessible dismiss action. Visibility/removal is caller-owned.
Content is a ColumnScope slot: review/check-in/photo-source models stay outside.
Use maxSheetHeightFraction for a bounded sheet. It sizes the inner handle/content
column, never the outer Material viewport: fractional height on the external
modifier changes Material's anchor constraints and incorrectly pins the sheet to
the top. Null keeps wrap-content behavior. Callers fill/scroll the remaining body;
they must not put fillMaxHeight(fraction) on the sheet modifier.
Material provides modal focus and default system/IME inset handling; validate
keyboard/content scrolling and any custom consumer insets at integration.

`CoffeePeekDestructiveConfirmationSheet` uses a transparent Material modal host
for two floating rounded cards: title/message with a divided red text action,
and a separate cancel card. There is no grabber or filled destructive button.
The cards animate from below, respect safe insets and scroll for large text;
Material owns modal focus, Back, scrim and swipe dismissal. The caller closes it
before performing the operation; core owns no account, navigation or persistence
behavior. The shared/iOS profile uses the existing native action-sheet presenter,
matching author blocking on that platform.

CoffeePeekFabMenu accepts neutral icon/description/callback/enabled actions, not routes or
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
`CoffeePeekTopBar.kt` keeps paired LTR/RTL light/dark inset fixtures with synthetic asymmetry;
InsetsRecipeTest checks measured geometry, consumption, density and inset updates.
Real system bars/IME/modal integration and large-font screen QA remain separate.

## Light/dark previews

`CoffeePeekGroupedRows.kt` keeps LTR/RTL light/dark fixtures at fontScale 2.0. Stepper
actions now have growing 48.dp minimum targets rather than fixed 32.dp height.
Grouped rows use a 48.dp minimum; action rows/stepper halves expose button roles.
ADAPTIVE_LAYOUT.md documents the targeted measured-layout/semantic tests and
preserved ellipsis policies. Large-font screen, OS scaling and pixel parity QA
remain integration responsibilities; this is not a complete accessibility audit.

Open a component's own source file in Android Studio Design/Split mode. Every
prepared family has colocated paired multiplatform `@Preview` functions with
explicit `CoffeePeekTheme(darkTheme = false/true)`, including fields/error/disabled
states, grouped rows, badges, segmented control, loader, sheet, dialogs and FAB.
`CoffeePeekTopBar.kt` also has LTR/RTL inset recipes; `CoffeePeekGroupedRows.kt` has LTR/RTL
large-font fixtures. Refresh has both idle/interactive and running samples.
Previews require no app DI, network, navigation or data. The Compose preview
annotation is in commonMain; Android Studio's rendering tooling remains a
debug-only Android dependency. The module also supplies iOS variants for shared consumers.

Use Interactive Preview / Run Preview for modal windows and stateful callbacks;
static layout previews are not guaranteed to display separate dialog windows.
Compilation is checked, but IDE rendering and brand-font visual parity require
manual inspection. After each slice, review both themes before integration.
Keep paired light/dark coverage up to date whenever adding a reusable UI family.

## Migration and verification

Presentation slice: CoffeePeekIconBadge/its palette are neutral visual primitives, reused
by settings and contribution UI. CoffeePeekSettingsSection/Row/Divider exclude AppVersionFooter:
version lookup and localization stay in composition. Section titles are rendered
as supplied (callers decide uppercase); row callbacks and enabled state are explicit.
Directional row chevrons mirror in RTL. CoffeePeekCapsuleSegmentedControl exposes tab
selection, accepts enabled state and requires nonempty unique options with a valid
selection. Its minimum-height targets can grow with typography.

CoffeePeekLoader retains the existing animation and exposes localized indeterminate
progress semantics; it does not start work. CoffeePeekErrorDialog requires all user-facing
strings and emits dismiss. CoffeePeekLoadingDialog visibility is caller-owned and uses a
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
```

Future consumers use `implementation(project(module.core.designSystem))`.

### Glass backdrop ownership

`CoffeePeekPhotoBackdrop` captures only its photo slot, inside explicit photo bounds.
Overlay controls consume that local source as siblings of the captured layer;
they never capture themselves. Shop-card favorites use this source on Android.
Native iOS presenters own their material and bypass this Haze capture.
This is an Android approximation, not identical iOS optical rendering.

Flat header/roaster controls use the dense readable fallback. Native MapLibre
surfaces do not supply a Compose photo source; their overlay controls retain the
fallback until a valid map capture is available. The root capture belongs to the
tab bar and popup-menu backdrop, not all in-window controls. A photo source must
not cross a popup/window boundary. Missing/unsupported blur uses the existing
nearly opaque tinted fallback without changing brand colors or touch targets.
