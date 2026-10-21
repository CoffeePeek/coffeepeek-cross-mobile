# Shared affinity heart — 2026-10-18

Later manual follow-up: after unlocking Pixel 7, unchanged floating/compact like
sources were visually checked in Sandbox 829 and 843 in light/dark themes. Header
glass circles match neighboring actions; selected is gold, unselected neutral,
and compact photo controls remain compact. See Shop's final device follow-up for
exact build identity and remaining motion/live-media gates. Initial locked-phone
captures below were not treated as acceptance evidence.

## Contextual floating control — 2026-10-20

Owner: Coffee/shared-like checkpoint #104, based on `d34ba4c7` after parent
Shop corrections `6e118f14` were propagated through the stack. `FloatingGlass`
is an explicit variant for grouped floating actions: its plate uses the shared
`buttonHeight` and the same untinted `coffeePeekLiquidGlass` material/shadow as
`CoffeePeekGlassIconButton`. Shop's header opts in; no other caller is resized.
`Glass` remains the compact 32dp dark photo badge and `Plain` stays unplated.
All variants keep the 48dp minimum touch target and ripple-free toggle behavior.
The floating heart uses theme-neutral unselected foreground and accessible gold
when selected, not the destructive/error palette. Loading, semantics, callbacks
and exposure tracking are unchanged. Paired previews include both floating states.

Verification plan: pure style-size and palette unit tests, Shop/Coffee/app unit
regressions, Debug/Sandbox assembly and packaged DEX/isolation guards; then
propagate to Roasters and verify the final tip. Manual Sandbox light/dark,
actual-photo/Haze and motion acceptance remain separate device gates. No
automated UI suites or test-only hosts are added or run.

Checkpoint `6e176444`: **726** unit results (design-system 26, Shop 101, Coffee
46, app Direct 553), zero failures/errors/skips. Debug/Sandbox APKs and packaged
DEX/isolation guards pass (4m52s). Studio's completed repeat reports no errors in
the like toggle and Shop author eligibility helper; the earlier generated-resource
resolution errors cleared after resource generation. The installed Sandbox
verification source is `6e176444`, version **1.0.829-sandbox / 829**, with matching
installed/new signing certificates and no INTERNET permission. Pixel 7/API 37
was explicitly selected; ordinary CoffeePeek and Pixel 10a were not updated.
The phone locked during installation, so no successful visual or motion check
is claimed from the initial launcher/black captures. No data was cleared.

## Compact interaction follow-up — 2026-10-19

Source: Coffee child `da67051b` plus this patch, inheriting parent UI fix
`58edfdbd` through the existing stack. The shared toggle separates a **32dp visual
plate** (default 24dp heart) from its **48dp minimum touch target**. A larger caller
layout no longer stretches the glass circle. Passive photo markers use the same
compact plate. Plain and Glass remain explicit styles; custom icon sizes may grow
the plate enough to retain glyph padding.

The toggle has no press ripple: controlled selection alone changes the heart.
Unselected hearts use `onSurfaceVariant`, not red or primary text emphasis;
selected hearts retain the existing accessible gold palette. Over-photo outlines
remain readable white. Loading is caller-supplied visual state with a localized
progress label and disabled dispatch, never an operation launched by core.

Shop favorites and helpful hearts reuse that toggle. The adjacent helpful count
is plain text outside the action, so pressing it cannot toggle the heart or paint
a ripple. Own/read-only counts remain static, not disabled fictional actions.
The unplated `CoffeePeekDrinkLabel` and square monetary `CoffeePeekBynIcon` are
inherited from the parent. Coffee prices use main's 20dp square currency glyph;
Shop menu prices use 16dp, without resizing the amount typography.

The fractional-rating renderer already receives the unrounded average on this
full branch. The earlier parent-only visual fixture still used its historical
whole-star renderer; do not duplicate the newer renderer back into that parent.
The full-branch pixel contract explicitly covers 4.0/4.3/**4.7**/5.0 in every
size and both themes. No rating formula or server value is changed here.

Full-stack checks: Direct Debug and Direct Sandbox APKs, packaged DEX/isolation
guards, Shop lint and **690 selected unit results** pass (core 23, Shop impl 91,
Coffee impl 41, app Direct 535). Pixel 7 / API 37 self-targeted core UI suite:
**84/84 pass**, no failures/retries in this full-branch run. This includes actual
fractional pixels, 32dp plate geometry inside a 64dp caller target, unchanged
pixels while pressed (no ripple), loading/disabled behavior, static drink label,
square currency dimensions and real modal keyboard/inset checks. Parent-only
test-harness retries remain documented in `INSETS.md`; they are not erased by
this later successful run. No normal CoffeePeek installation, account/backend
write, native iOS UI check or live-photo/Haze acceptance is claimed.

The full self-targeted Shop suite also passes **36/36**, without failures/retries,
including the new physical count tap that must not vote, followed by a heart tap
that must change controlled selection exactly once. Combined full-branch device
result: **120/120**. A final `overImage` argument rebuild for the Shop photo heart
passes Debug/Sandbox APK and DEX guards. Scoped Studio checks pass; the first
LikeButton inspection timed out, its completed retry reports no errors.

## Audit and decision

The same state used unrelated foreground sources: app FavoriteButton inherited
white/black text colors, catalog/public-profile read-only hearts used primary
yellow, History used primary, Shop used error red, and Favorites drink/roaster
cards inherited onSurface. This was visual drift, not different backend states.

The user selected brand gold after comparing red versus yellow: filled = selected
gold (`Like`, #EAB308), outline = not selected with the theme's neutral foreground.
Use a darker gold (`LikeOnLight`, #B68A2E) on light surfaces to preserve icon
contrast, bright gold and a white outline over photo controls. Affinity remains
separate from `Error`. Disabled controls reduce opacity. Keep counter text themed,
and keep existing photo/card/glass containers and sizes. Section/empty-state
heart decorations are not interactive affinity state and are not recolored.

`CoffeePeekLikeIcon` owns the glyph/color for passive counters and custom glass
containers. `CoffeePeekLikeButton` composes it in a stateless accessible toggle;
callers provide localized descriptions, actions and existing container styling.
No favorite/vote repository, auth, haptic, analytics, loading or count policy
moves into core. No new module or persistence/API dependency is introduced.

## Scope

- Shared app FavoriteButton (catalog/discovery/roaster consumers)
- Catalog/public-profile passive favorites and legacy Shop/Review rendering
- Migrated Favorites shop/roaster/drink controls
- Migrated Shop favorite and helpful controls, History read-only like count

Shared Compose reuse remains KMP compatible; no iOS-native screen/control/writer
implementation is changed. Native controls that do not render the Compose slot
are not certified by Android tests. Work stays in the current Coffee child PR
as the user's explicit shared-UI correction; Coffee migration itself remains
at its domain/data checkpoint.

## Verification plan

- Compile app Direct/Play Debug + Direct Sandbox and packaged DEX/isolation guards.
- Execute affected unit suites; compile both Native app targets for shared reuse.
- Self-targeted design-system tests: actual gold/neutral pixels in both themes, outline vs
  fill, photo backgrounds, controlled selection, disabled dispatch/appearance,
  one label/48dp target, nested-card click isolation and passive numeric semantics.
- Run complete relevant library UI suites on the explicitly resolved Pixel 7
  if unlocked; no production app update/account changes/network writes. Sandbox
  visual inspection is separate from library tests/live backend acceptance.
- Inspect changed files in Studio where imported; record limitations, source,
  test counts/failures/retries and untested device/Native gates below.

## Verification record

2026-10-18, worktree based on `b78a563a`, Direct Sandbox **1.0.671** / code 671.
Installed APK SHA-256:
`1c79673a427afe2faf4dfe7cddc220f912e032546391e1a435ab12cab77eaef8`.
This identifies the tested pre-commit build, not a source revision inferred only
from its installed version.

- **1073 unit executions**, all pass with zero failures/errors/skips: core 23,
  Shop 89, Favorites 15, History 46, app Direct 450 and Play 450. Four new pure
  palette tests include selected/neutral/photo states and light/dark contrast.
- Direct/Play Debug + Direct Sandbox APKs, packaged DEX and Sandbox isolation
  guards pass. Both Native app target compilations pass (compatibility only).
  A final preview/comment-only rebuild also passes APK guards.
- Pixel 7 / API 37: **136/136 self-targeted UI cases**, all first-pass, no retries:
  core 69 (including seven new heart contracts), Shop 33, Favorites 14, History 20.
  Actual pixel checks verify gold/neutral in both themes and white outline over
  a dark photo-control container; callbacks, disabled state, semantics, 48dp and
  nested-card isolation pass. The pre-existing core host targets SDK 26; the
  feature hosts target 36. This is not new modal/window-inset certification.
- Variant/package/instrumentation targets checked before in-place test updates;
  each test package targets itself, with no production Application. Phone was
  unlocked by the user; foreground test-window flags only, no security/settings
  changes. No normal app installation or live-account write.
- Updated only full Sandbox from 1.0.660 to 1.0.671. Manually inspected the Shop
  heart selected/unselected in light theme and selected in dark theme; a local
  fake remove/add restored the original favorite state and descriptions.
  Sandbox remains open on the fake Shop in dark theme. Ordinary CoffeePeek
  remains **1.0.648 / code 648**, unchanged.
- Scoped Studio error inspections pass for the shared icon/button, legacy
  FavoriteButton and migrated Shop/Favorites wrappers. One busy inspection timed
  out initially; its completed retry reports no errors. Existing preview/AGP
  deprecation warnings remain. IDE rendered previews, TalkBack and native iOS
  visual acceptance were not performed; real backend behavior is not certified.

All screenshots/UI dumps and instrumentation/build logs remain local outside
tracked sources. No account identity, private media or raw diagnostics are
published with the PR.

## Photo container correction — 2026-10-19

Requested during Coffee UI review: remove opaque black plates around photo
favorites. CoffeePeekLikeButton now supports Glass and Plain. Away from images
Plain remains transparent; photo controls use the shared glass modifier with a
translucent tint/rim, preserving the gold/neutral glyphs, selection semantics and
48dp actionable target. Existing custom container callers remain supported.
Haze sources can supply blur; without one this is a translucent fallback, not
real backdrop sampling. CoffeePeekLikeBadge covers passive photo markers without
inventing an action. Catalogue/public-profile/favorites photo callers reuse these
components; request/auth/analytics/haptic behavior and ordinary account data are
unchanged. Existing Shop glass controls are not replaced with a second toggle.

Android APK/DEX/isolation, unit suites and both Native app compilations pass.
Two new glass/badge UI tests execute alongside the new card-builder contracts.
Pixel 7 disconnected initially, then the user reconnected/unlocked it. Final
full core **77**, Coffee **52** and Favorites **14** self-targeted cases pass:
**143/143**, no failures/skips in this slice. Core also passed its first 77-case
run before the import-only final rebuild. Manually inspected both styles and
selection states on synthetic white/brown/black photo backgrounds in both
themes; Plain's white outline disappears on white, so Glass is the photo
default. Earlier 136/136 and 71/71 records above remain historical checkpoints.
See the Coffee plan for exact final test-APK hashes, prior harness retries and
remaining real-photo/Haze/integration gates. No normal app deployment or new
Sandbox/live-account mutation is claimed.
