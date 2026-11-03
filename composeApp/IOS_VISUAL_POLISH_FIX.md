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

## First native build and installation

- Executable source for the first installed checkpoint:
  `df9c3d5d8d9d7d8075b723366c1177345226bd1c`.
  [Actions run 41321124100](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/41321124100)
  built the complete native Swift/Kotlin Debug iPhone application and passed
  the embedded ONNX minimum-OS check. It also executed 13 non-UI native
  contracts across four suites, zero failures/errors/skips: native tab visibility
  5, glass icon configuration 3, navigation delegate 2, tab delegate 3. The task
  executed (not skipped/up-to-date); the complete CI job passed.
- Verified archive SHA-256:
  `e1808abd3c4d7d8251d010891293a6b35193157a5b5b30876078274b81fea037`.
  The archive's source revision matched the final checkpoint; the executable is
  Mach-O arm64 for iPhoneOS, version `1.1.4` / build `2637`, minimum iOS `18.0`,
  with `CoffeePeekDebug = YES`.
- Local installation packaging changes only the unsigned bundle identity to
  `com.coffeepeek.debug` and display name to `CoffeePeek Debug`. xtool applies its
  development prefix/signature. No application code or version is replaced.
- Physical device: iPhone 15, iOS `27.0.1`, USB, Developer Mode enabled. The user
  unlocked it for installation; no device settings/security were changed by the
  agent. The first profile attempt failed with Apple `409` (no matching current
  iOS devices). A retry returned device-registration `201` / `ENABLED`, then
  signed and installed successfully. The cause of the first response is unknown.
- Installation was re-read from the device: Debug `2637` is present alongside
  original `com.coffeepeek` TestFlight `2633`, which remains unchanged. Neither
  application was uninstalled or cleared. No account or favorite writes ran.
- Command-line startup failed with `DebugserverClient.Error.unknown`. Successful
  installation is not a startup pass; the user was asked to open Debug manually.
  The user then confirmed that the main screen opened successfully on the phone.
  This is a user-confirmed startup check, not a complete visual review or a
  successful command-line launch. The subsequent device-feedback changes below
  require a new native build; this result does not certify those later changes.
- Android artifact identity: `com.coffeepeek`, `1.1.4.2635` / `2635`,
  SHA-256 `bef82e390b0216a4e19975fac329132fe0fc6622c905eb9cb660bad258e674a7`.
  It was assembled from `4e4bc39e` plus the working UI changes later committed as
  `31438c3d`; the final lifecycle follow-up changes only iOS source. This Android
  artifact was not installed or launched.

Manual acceptance must distinguish verified checks from open gates: light/dark themes, increased
font size, native accessibility, search/back transitions, map expand/collapse and
push/pop/tab transitions, sheet button layers, segmented control material, shelf
geometry and selected favorite rendering. No real-account writes are authorized.

## Follow-up from the first device review

The user requested seven additional corrections while reviewing Debug `2637`.
These stay in the same UI checkpoint and will be delivered in an updated build.

- Creation picker: centered resource-backed `Создать` heading and separators
  between every option, in both actual app and history entries. The static picker
  no longer uses the platform keyboard/IME scroll wrapper. No editor or remote
  request was created before selecting an option. Native first-measure detent
  sizing is retained; no unsafe off-window measure wait or animation timer is
  introduced. Opening-speed improvement still needs device confirmation.
- Copy notification: the actual shop path uses `CpSnackbarHost`. A one-line row
  is now approximately 48 dp instead of 80 dp, with an accessible plain X and
  preserved queue, timeout, dismiss, retry and loading actions.
- Custom modal sheets: restore opaque theme-surface backgrounds on native and
  Compose roots and remove the full-sheet blur/glass wrapper. Existing detents,
  grabber, dismissal locks and keyboard behavior stay intact. System UIKit alert
  dialogs use their existing separate presentation path.
- Shop favorite: keep the native glass plate visually unselected for hearts;
  retain gold foreground plus selected/disabled accessibility traits. Other
  controls, including selected map buttons, retain their native visual state.
  Add non-UI UIKit contracts for that distinction and the loading state.
- Shop check-in CTA: restore wide shared glass `Чекин` button in both actual iOS
  legacy and migrated shop footers. Preserve loading/auth/analytics and the shop
  selection variant; the global creation `+` stays unchanged.
- Coffee segments: the custom transparent normal track disables UIKit's default
  selected thumb. Supply explicit resizable selected-state capsules while keeping
  the real glass carrier, including highlighted/disabled combinations.
- Shelf reference: use a frosted gray front rail, bevel/reflection, shadow and two
  metallic fasteners, with an 8 dp overlap over bag bottoms. Preserve real data,
  columns, slot sizes and drag callbacks; the row step accounts for that overlap.
  A narrow shared decorative frosted-surface API owns Haze/opaque fallback and an
  iOS-only Reduce Transparency observer; feature code owns shelf artwork.

Verification for this follow-up: recompile affected modules, execute existing
design-system/shelf/shop/history unit contracts, assemble Android Debug with DEX
checks, compile design-system iOS locally, then build the native iPhone app and
run the updated non-UI native contracts on macOS. Gradle remains serialized with
at most six workers. Paired fake-state previews remain colocated; no automated
UI/instrumented/screenshot suites are added or run. Updated device installation,
startup and visual checks remain pending until that build is delivered.

Local follow-up verification passed: 273 executed unit/contract tests across
39 suites (design-system 54, shelf impl 57, shop impl 108, history impl 54), zero
failures/errors/skips. All four test tasks executed. Android DirectDebug assembly
and its packaged DEX runtime-class guard passed; design-system iOS Arm64 compiled
the frosted surface, accessibility observer, opaque modal and selected capsules.
The updated application UIKit selection code and its three additional native
contracts still need the next full macOS build/test run.

## Follow-up delivery identity

The follow-up application source is `87587ca264822335d90d6a94a7d92161d49716c5`.
The exact-source local DirectDebug rebuild passed its packaged DEX guard and
produced `com.coffeepeek` version `1.1.4.2638` / `2638`. Its APK SHA-256 is
`49ae19695a666bde9f20a4cb273ce9231c0ded48d279110c9d18296c4cc52ee8`.
This local APK has the workstation Debug certificate and was not installed.

The user also requested installation on the connected Android phone while iOS
verification continues. Pixel 9 currently has normal CoffeePeek `2630`, signed
with a different application certificate. No uninstall, clear-data, downgrade,
or incompatible install was attempted. The user confirmed unlocking the phone;
lock state must be checked again immediately before deployment.

For a compatible in-place update, temporary build-only CI run
[`41326626499`](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/41326626499)
checks out the fixed source above, assembles `directDebug` with six workers, and
signs that Debug APK with the existing application identity inside CI. It checks
the installed certificate digest, package/version, debuggable flag and unchanged
DEX entries before uploading the APK. Signing material stays in CI and is removed
after use; no Release build or distribution upload is requested. Installation
and startup are pending this artifact.

Full iOS follow-up build and native contract run
[`41325888227`](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/41325888227)
uses the same fixed source. Updated iPhone installation and the additional native
contracts are pending its result. The earlier `2637` startup confirmation does
not certify `2638`.

Android CI `41326626499` completed successfully, including compatible signature,
unchanged Debug DEX and cleanup checks. Downloaded APK SHA-256:
`63d1ff9410878484dfa779538a758f7eecb9ce46f230e2f220cf7a8b812be10f`.
The iOS device app in run `41325888227` also built and packaged successfully;
the updated native contracts are still running. Downloaded tar SHA-256:
`73d3657cd577f400c84028fa333fc84ec498198961ddfc546780f4be39260a1a`.
Both artifact checksums and fixed source identities were checked locally.
Before installation, both previously selected USB devices were no longer visible.
The user was asked to reconnect and unlock them. Neither update is installed yet.

## Additional Android empty-search feedback

The user then requested removing the redundant no-results title/body and query
recovery button while keeping the existing two add-shop/add-roaster suggestion
cards. The migrated Android Discovery global `allResultsEmpty` state now renders
only those cards and retains their messages/callbacks. Separate filter recovery,
partial results, loading and failure gates remain unchanged. The iOS legacy
entry already uses the two suggestion cards without that extra block; it is not
changed. Paired Android previews cover normal/global-filtered empty states and
the independent filter recovery state.

Verification: the existing `SearchEmptyStateTest` executed four contracts with
zero failures/errors/skips. DirectDebug assembly and packaged DEX checks passed
in 12 seconds. No new UI tests were added or run. These source changes are later
than `87587ca2` and are not in either `2638` artifact above; updated Android
delivery will identify its own source and version.

## Updated device delivery results

- iPhone was reconnected and verified unlocked. The existing development-signed
  Debug `2637` was updated in place to `2638` with the same application identity.
  xtool installation completed successfully. A fresh device read confirmed Debug
  `2638` and original TestFlight `2633` both present. No uninstall or data clearing
  occurred. The user then confirmed that the updated Debug main screen opened.
  This confirms startup, not complete visual or live-backend acceptance.
- The additional Android search source checkpoint is
  `9b23ccc2539bb83814bedfeae87ec643e7956466`. Its exact-source DirectDebug `2639`
  rebuild passed assembly and the packaged DEX guard in 26 seconds; package,
  debuggable flag and entry Activity were re-read from the APK.
- Android compatible-signature build
  [`41327987587`](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/41327987587)
  checks out that exact checkpoint for `2639`. Pixel 9 remains disconnected;
  the ready `2638` Android artifact was not installed. Latest Android installation
  and startup remain pending the new artifact and selected device connection.

The full `2638` macOS run completed successfully. Downloaded XML confirms 16
executed native contracts across four suites: glass configuration 6, native tab
visibility 5, navigation delegate 2, tab delegate 3. No failures/errors/skips.
These cover source `87587ca2`, not the later changes below. Android `2639` CI
also completed all build/DEX/signature/unchanged-code/cleanup checks; its artifact
was downloaded and checksum/signature checked. Pixel 9 was still absent on the
subsequent targeted connection check. The user was asked to reconnect it or defer
installation; no Android update/startup is claimed.

## Further device feedback: shelf, loaders and filters

- The user requested doubling the transparent rail again: 20 to 40 dp. The
  existing height-derived row and drag geometry accounts for the increase;
  bags, 8 dp overlap, material, bevel and fasteners remain the same.
- Inventory found 18 direct Material circular indicators and one linear download
  indicator in 16 app-owned source files, plus native loaders in the shared iOS
  loader, update-check row, glass buttons and operation-status banner. All now
  render the existing CoffeePeek contour. The shared loader accepts optional
  finite/clamped determinate progress, retaining percent labels and accessibility
  range semantics; unknown totals remain indeterminate. Sizes, colors and request
  guards are preserved. The scan beam is a domain-specific photo animation and
  remains unchanged.
- Native UIKit controls host that same Compose renderer through explicitly owned
  child controllers, removed on disposal. No loader controller is created when
  inactive. The transparent 24 pt image slot preserves button layout, native
  spinners are disabled, and disabled/selected/loading accessibility is retained.
  Independent source review found no owner capture/retain-cycle blocker and
  confirmed scene disposal on leaving the window hierarchy in Compose 1.12.1.
- The three active filter entries (legacy global iOS, Android Discovery, and
  shared coffee catalogue) use a bounded 80% viewport height. Native filters
  start with that known detent instead of provisional half-height followed by an
  unanimated measured resize. Other sheets retain content sizing; nested sheets
  do not inherit the flag. Existing native animation/Reduce Motion, safe insets,
  keyboard, reset/apply/cancel and analytics are preserved. Removed duplicate
  bottom insets from those filter bodies only. Added light/dark compact-window
  and large-font previews and three pure height contracts.

Verification: 299 executed unit/contract tests in 43 suites, zero failures,
errors or skips (design-system 57, shelf 57, coffee 46, Discovery 139). The first
combined run executed those suites but failed design-system iOS compilation due
to four missing UIKit category imports in the new loader bridge. Imports were
fixed; the retry passed in 26 seconds, reusing the already-executed test results,
compiling design-system iOS Arm64 and assembling Android with packaged DEX checks.
The app's `compileKotlinIosArm64` task was SKIPPED on Linux: this does not certify
app UIKit compilation or execution. A full macOS device build and updated native
contracts remain required. Direct standard-spinner source audit and diff checks
pass; existing SDK/preview deprecation warnings remain outside scope.

Manual gates remain: new 40 dp rails, every loader context (including native
buttons and progress), filter entrance/scroll/close on both platforms, compact
window/font scale 2, light/dark, accessibility and Reduce Motion. The successful
`2638` startup does not certify these later changes. No automated UI,
instrumented-Compose or screenshot suites were added or run.

The requested Russian/Belarusian language selection will be a separate child
checkpoint. Audit found 1801 production base strings plus 56 array items and no
Belarusian resources, alongside active hardcoded legacy/native UI and fixed-Russian
dates. A selector alone cannot establish full Belarusian support; resource
translation, active hardcode extraction, locale persistence/application and native
labels must be implemented and checked together.


## Build 2640 delivery and selected-pill follow-up

Source `3b8fbbe1e1a56479d7a87c5db7250e84e57cb957`: full iOS run
[41329297178](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/41329297178)
and compatible Android Debug run
[41329298270](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/41329298270)
passed. Downloaded native XML confirms 16 executed contracts in four suites with
zero failures/errors/skips. iPhone Debug was updated in place from 2638 to 2640;
a fresh device read confirmed Debug 2640 and retained TestFlight 2633. The user
confirmed that filters now open smoothly and scroll normally. This is a manual
filter flow check, not complete loader, theme, font or accessibility acceptance.
The user explicitly deferred Android installation; neither ready APK was installed.

A subsequent dark-theme screenshot preserved a failure: the selected Coffee tab
thumb still had a straight/cropped inner edge. The UIImage background is subject
to native segment/divider geometry. The fix places a rounded, non-interactive,
non-accessible UIView behind the native segmented control inside the existing
single glass carrier. Native hit testing, selection, enabled state and labels
remain owned by UISegmentedControl. Frame updates animate for selection changes,
respect Reduce Motion, and avoid restarting on unchanged Compose updates. Disposal
removes the target and clears its callback to release captured view references.

The user also requested rounder shelf corners and clearer fasteners. Rail height
40 dp and overlap 8 dp stay intact. Radius is now 14 dp, with matching bevel and
highlight; fasteners are 10 dp circular metallic heads with a horizontal slot.
No bag, row or drag/drop data geometry is changed.

The narrow parent fix was isolated from unfinished language work in a managed
worktree. The first iOS compile failed on an unnecessary category import; removing
it produced a successful 3-second retry. Design-system iOS Arm64 compiled. Shelf
unit results show 57 passing cases in eight suites, reused from the build cache;
this was not a new execution of those cases. Preview declarations cover first/last
selection, 2-4 segments, disabled state and light/dark; actual previews were not
rendered because Studio MCP is unavailable. Android app assembly and full iPhone
packaging for this source remain delivery gates. No automated UI/screenshot tests
were added or run. The unchanged navigation/button contract suites passed for
2640 and do not directly exercise this visual-only thumb/rail change.
