# iOS visual polish — 2026-11-01

## Base and scope

The user requested seven visual corrections from physical-iPhone screenshots and
explicitly asked for parallel agents and a main → develop merge first. Main
`a90f0ce1` was merged into develop `cc0262da` without conflicts; merge
`4e4bc39e` was published to develop. The implementation continues in
`codex/migration-ios-visual-polish-20261101` from that merge.

Preserve existing actions, navigation, favorite state, analytics, accessibility,
platform ownership and Android appearance. No feature/module migration or live
account mutations are part of this work. Screenshots and device identifiers stay
outside tracked sources.

## Verification plan

- Trace actual iOS entry points before changing migrated or legacy UI.
- Remove full-map canvas safe-area gaps while keeping controls reachable around
  the status bar/home indicator. Remove map-tag horizontal gutters without
  changing the Home row defaults.
- Correct search material, leading-back animation, shelf/catalog segments and
  calendar-sheet button layers through existing design-system/platform controls.
- Double the physical shelf-board thickness from 10 to 20 dp, confirmed by the
  user. Preserve drag/drop geometry derived from that dimension.
- Remove the second outline drawn over selected roaster hearts. Keep the shared
  gold selection and unselected icon behavior.
- Inspect the active other-author check-in menu. It is a stock UIKit UIMenu with
  20-point images and no application row inset. Keep the native menu rather than
  replacing it or using private APIs to change system-owned spacing.
- Compile affected code, run relevant existing unit/contract suites, assemble
  Android Debug with packaged DEX checks, and build the full native iPhone Debug
  app on macOS. Run Gradle serially with at most six workers.
- Use paired colocated previews and manual physical-iPhone checks for UI. Do not
  add/run automated UI, instrumented Compose or screenshot tests.

## Build and device gates

The initial device build of old checkout `a5f6f1b8` (Actions run `41317756405`)
was cancelled after the expanded request and discovery of the newer source base.
It did not produce an installable app and is not counted as verification.

The connected iPhone already has a newer TestFlight CoffeePeek installation.
The requested Debug build will use a separate local application identity and
display name, preserving the TestFlight installation and its data. Default Debug
OAuth/push configuration is not production acceptance.

## Verification results

- The initial local combined Gradle run executed 111 unit tests: design-system
  54 tests / 15 suites, shelf impl 57 tests / 8 suites; zero failures, errors or
  skips. The tasks executed, rather than reusing an up-to-date test result.
- That run failed at design-system iOS Arm64 compilation because the new segments
  code treated `UIBarMetricsDefault` as an enum member. The Kotlin binding is a
  top-level constant; the import/call was corrected. The original failure is
  retained separately and is not an Android assembly pass.
- Independent review identified an IME external-reset edge case in the new native
  search field. An accepted external replacement must not be overwritten by an
  old composing-text callback. Authoritative replacements now unmark/apply text
  while suppressing native edit callbacks; matching echoes retain composition.
  Independent re-review found the original issue resolved.
- The sequential retry passed design-system `compileKotlinIosArm64` and app
  `assembleDirectDebug`, including `verifyDirectDebugRuntimeClasses`. All new
  UIKit search/material/segment source compiled. The iOS application itself
  still requires the full macOS build; this is not an iPhone runtime result.
- Existing Gradle/AGP and preview deprecation warnings remain outside this scope.
  No Studio MCP tool is available in this session; no IDE diagnostic pass is
  claimed.
- A lifecycle review of the built KLIB IR confirmed that the initial Clear
  handler captured its `CoffeePeekSearchInput` owner through a property access.
  The handler is now created in a private factory capturing only its target
  parameter, removing the direct view/button/action owner cycle. Intermediate
  macOS run `41320052263` for checkpoint `31438c3d` was cancelled before an
  installable result; the final device build must use the follow-up checkpoint.
- Design-system iOS Arm64 compilation passed again after the lifecycle factory
  change. Android/common sources are unchanged by that follow-up; the successful
  Android assembly/DEX and 111 executed unit tests remain applicable.

Pending: full native iOS build, source/build identity and device
installation/startup. Manual acceptance
must distinguish verified checks from open gates: light/dark themes, increased
font size, native accessibility, search/back transitions, map expand/collapse and
push/pop/tab transitions, sheet button layers, segmented control material, shelf
geometry and selected favorite rendering. No real-account writes are authorized.
