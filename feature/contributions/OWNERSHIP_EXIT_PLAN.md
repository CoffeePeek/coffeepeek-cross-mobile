# Contributions ownership exit — 2026-10-26

Branch `codex/migration-contributions-ownership`, child of
`codex/migration-discovery-ui-ownership` (#201). This is a separate feature
checkpoint; Discovery/Roaster changes remain in the parent diff.

## Scope and ownership

Existing api/domain/data/impl modules remain in place. No new Gradle module,
backend endpoint, submission flow, product feature or analytics service is added.
Android and iOS already call the feature entries through root renderers.

- [x] Remove the public application-owned photo/price/brew/logo/cover/remote-image
  UI slots. Hosts supply only native launchers, permission, native map canvas and
  success/error feedback. The feature composes existing design-system primitives.
- [x] Move photo-source/attachment presentation out of composeApp. Shared DS photo
  attachments have an explicit Simple mode preserving the old 96dp grid and
  remove badge; reorder, position numbers, manage hint and viewer are absent in
  this mode. Existing default consumers retain their ReorderAndPreview behavior.
- [x] Keep platform URI/JPEG/camera handling in composition behind a narrow API.
  Launchers are scoped to requests; feature delivery bounds selection to current
  room, owns byte buffers, drops duplicates/superseded/disposed callbacks and
  checks the account at delivery. Account changes re-key ephemeral form UI.
- [x] Move map selection state/dialog/header/pin/confirmation to Contributions.
  The app supplies CoffeeMap only, with the existing native provider. The feature
  retains 350ms address debounce, sub-meter drift tolerance, center/target/location
  callbacks, zoom 16, compact-landscape layout and a fixed two-line address panel
  outside the viewport. Confirmation requires the currently resolved point.
- [x] Preserve native iOS map-control clipping/z-order through the existing DS
  presenter capability. No UIKit implementation is copied into a feature.
- [x] Preserve price thresholds, slider clamp, RU/EN brew-name compatibility,
  28dp logos/6sp placeholders, 16sp cover fallback, saved-photo Crop and haptics.
- [x] Delete seven obsolete app UI files with no remaining external consumers:
  AddShop, AddShopSuccess, PickShopLocationDialog, AddRoaster, MyContributions,
  SuggestShopChange and ShopChangeRequestDetail.
- [ ] Replace ShopContributionCatalogs legacy details/catalog/drink reads in a
  following supporting slice of this checkpoint, using public owner APIs or
  Contributions-owned data contracts as appropriate. Do not pass legacy DTOs
  through core or import another feature's data/domain from feature code.
- [ ] Retire remaining old ViewModels and their DI after their last test/data
  consumers migrate. Preserve other contributors' tests: ShopChangeEditorScreen
  still has an existing ShopMenuEditorTest consumer; it is retained for that
  reason, not selected by either production route. This UI suite is not run,
  extended, restored or compiled by this checkpoint.

## Parity and verification

Legacy behavior reference: [LEGACY_INVENTORY.md](LEGACY_INVENTORY.md).
Root routes and analytics page mapping are unchanged: MyContributions and
ShopChangeRequestDetail map to contributions; SuggestShopChange/ShopChangeEditor/
AddShop map to shop_edit. Legacy forms had no feature-owned event tracker. This
slice adds no events, SDK or second consent/session policy.

The first compile found a trailing-lambda preview mismatch after extending the
shared attachment and the required dismiss accessibility label. Both were fixed;
the Android/iOS compile and pre-existing Contributions/DS unit suites then passed
(59s). New cancellation/photo-delivery regressions were added afterwards, so that
earlier result does not certify them. Final verification is recorded below.

Verification plan: Contributions domain/data/impl and design-system unit tests;
app non-UI Contributions/location/renderer/analytics/Sandbox contracts; Android
Debug/Sandbox assembly with packaged DEX guards; shared metadata and iOS production
compile/framework/common test compile. Gradle runs sequentially, --max-workers=6.
No automated UI/instrumentation/screenshot tests or test Activities/APKs.

Manual Sandbox theme/font/window/accessibility review, real native map/picker/
camera permissions, keyboard, modal/back/cancel and full Xcode app build remain
separate acceptance gates. No account mutation, production telemetry, app install
or device run is authorized merely by an assembly or fake contract result.

The user's local iosApp project.pbxproj diff is excluded from this checkpoint.
The parent #195 CI repair is isolated from these uncommitted feature changes and
propagated through #201 before publishing this child.

## Final verification — 2026-10-26

Source `89778c23` + this ownership slice; 9m34s, all requested tasks successful.
Counts come from the tasks in this run, excluding stale retired-module outputs:

- Library testDebugUnitTest: **1501 tests / 231 suites** across 55 tasks;
  app Direct and Play: **825 tests / 172 suites each**, zero failures/errors/skips.
  The variants are not added together as unique scenarios. Cached/up-to-date
  results are distinguished from freshly executed tasks. Contributions
  domain/data/impl results: 16/19/76, DS: 57.
- Shared metadata compilation: core media/DS and Contributions domain/api/impl.
- Direct/Play Debug and Direct Sandbox APKs assembled; packaged DEX guards
  include new API/entry/map/photo components; Sandbox isolation passed.
- Full Android CI aggregate unit and lint tasks passed. iOS Arm64 production
  compile, Simulator Debug framework link and common Native test compilation
  Contrib impl/DS/app passed. Native tests were not executed; full Swift/Xcode
  app build is not certified by a framework link.
- Studio inspections of map state and photos timed out without reported errors:
  partial diagnostics, not a clean IDE or preview-rendering result. Existing
  preview deprecation/expect-actual/opt-in warnings remain outside this slice.
- New unit contracts cover 350ms debounce, drift, current-point confirmation,
  stale/non-cooperative cancelled geocoding, missing/failed address, capacities,
  copied bytes, duplicate/superseded/disposed photo callbacks and account gating.
  No automated UI tests or real native device actions were used.

APK SHA-256:

- DirectDebug: `9c3bd229aec977ec840aaf74994fa894d51e0f78f3ad5121630e8dc724c216db`
- DirectSandbox: `e6d2467318c14cb1468824c011eda0e42a77662e104e8f2a3d80cb076e4df77e`
- PlayDebug: `106c64f609736357c281166f16c3aad98340e0bfa4b8b07d8d6691decae81862`

Parent CI repair `027c2029` has a successful GitHub Android CI run
[40384202349](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/40384202349).
Its iOS verification was still running at the last observed status; local success
is not substituted for a remote result. The same full Android CI also passed
locally on #201 (`89778c23`) before being passed to this branch.
