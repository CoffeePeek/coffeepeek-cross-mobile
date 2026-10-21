# Shared fractional rating checkpoint

## Develop compatibility — 2026-10-19

Inherited Sandbox `e3ddf72d` includes approved develop `fc85f8e5` and retired
drink cutouts. Shared fractional ratings/hearts/cards remain. This checkpoint's
legacy Home rating preview used the old positional model constructor; remove
the retired slot and name visibility explicitly. This is a preview compatibility
fix, not a new legacy feature implementation or iOS screen migration.
Verify this parent independently before passing the fix to Coffee #104.

Independent parent verification passes (2m09s): **649 selected unit results**
(design-system 19, Shop impl 96, app Direct 534), Debug and Sandbox APKs with
DEX/isolation guards. No device app installed by this build. The first Coffee-tip
build exposed the obsolete preview argument and was not accepted; parent repair
is inherited before repeating final-tip checks. Local log:
`coffeepeek-develop-rating-parent-build-2026-10-19.log`.

Date: 2026-10-18. Branch `feature/shared-rating-component`, child of
`feature/sandbox-application` / PR #101 at 4fcd08af. This is a shared UI checkpoint,
not a new business feature or a Sandbox-only change.

## Preserve / change

- Preserve business averages, optional Home rating and repeat-tap clear policy,
  feature MVI/actions, localized category labels, brand colors and numeric styles.
- Replace all five-star render/input loops with one core component. Read-only
  fill follows the displayed tenth (4.3 -> 4 + 0.3), with Small/Medium/Large sizes.
  Compact remains a symbol plus numeric label, not a one-star rating scale.
- Reuse in migrated Shop, combined History, Home editor/detail, favorites badge,
  and shared legacy feed/profile/review/Home display paths. Keep standalone
  information-card stars and continuous category-average bars unchanged.
- Shared app files cannot import an Android-only dependency. Enable existing
  platform-independent design-system sources for the app's two Native targets,
  and move its existing app dependency from androidMain to commonMain. No iOS
  screen/binding/writer/navigation extraction, new module or expect/actual shim.
- No network/data/schema/DI dependency or rating contract changes. All new core
  helpers are internal; feature/domain models do not enter design-system.

## Verification plan

- [x] Calculation cases: decimals, rounded tenths, boundaries, non-finite values.
- [x] Compile the component, affected feature tests and Android app APKs/DEX.
- [x] Recheck both Native targets after making the shared dependency available.
- [x] Execute self-targeted rating tests: actual fractional pixels in all three
  sizes and both themes, RTL clipping, one read-only description/range, integer
  callbacks/repeat selection, disabled input and narrow/large-font layout.
- [x] Run full design-system and affected feature UI suites after targeted checks.
- [x] Inspect the installed Sandbox feed/history/form with fractional averages;
  never install over or mutate ordinary CoffeePeek.
- [x] Record exact counts/failures/source/APK identity and remaining gates below.

## Evidence and failures

Initial test compilation failed because a test read the composable theme inside
SideEffect. Capture the color during composition instead; no assertion changed.
The next Android run passed 19 core, 89 Shop, 75 Home, 46 History, 15 favorites and
450 Direct app unit cases (**694 executions**, no failures/errors/skips), both
ordinary debug APK/DEX guards and Direct Sandbox isolation/DEX guards.
Log: coffeepeek-shared-rating-regression-2026-10-18.log (1m 2s), outside Git.

The subsequent Native compatibility check failed because design-system was
declared only in androidMain, confirming the dependency boundary problem above.
It is not an IDE-only warning. Fixed the module targets/dependency location;
results of the complete rerun and physical checks are recorded after completion.

The fixed shared dependency passed both app Native compilations (iosArm64 and
iosSimulatorArm64), both app unit variants (450/450 each), core unit cases (19/19),
both ordinary Android debug APK/DEX and Direct Sandbox isolation/DEX checks.
Log: coffeepeek-shared-rating-platforms-2026-10-18.log (1m 56s).
Affected feature unit selection above is unchanged; together **1144 unit
executions**, zero failures/errors/skips, without counting repeated reruns.

Physical Pixel 7 / API 37: targeted rating **6/6**, then full design-system
**62/62** (123s), first pass for both. Tests use a self-targeted package
com.coffeepeek.core.designsystem.test with a plain ComponentActivity, no product
Application/DI/account/network. Pixel checks verify actual themed glyph colors
increase monotonically for 4.0/4.3/4.7/5.0 and that partial fill stays within the
correct edge of the fifth star in LTR/RTL. Narrow input tested at 180dp / 2x font
scale through injected density, not changes to phone settings. Logs:
coffeepeek-rating-ui-targeted-2026-10-18.log and
coffeepeek-rating-ui-designsystem-full-2026-10-18.log, outside Git.

Affected feature UI so far: Shop **33/33** (61s), Home **40/40** (83s), first
pass. Their self-targeted manifests/ComponentActivity hosts were checked before
deployment. Logs: coffeepeek-rating-ui-shop-full-2026-10-18.log and
coffeepeek-rating-ui-home-full-2026-10-18.log. The favorites UI APK also compiles.

The first full History UI run was interrupted by a physical-device lock:
3 cases passed, 15 reported missing Compose hierarchies, case 19 was in progress
when the test-only process was stopped, and case 20 did not run. Device trust
diagnostics confirmed deviceLocked=1. This is an aborted run, not a green suite
or evidence to weaken a product assertion. Request another unlock and rerun the
complete 20-case History suite plus favorites and Sandbox visual checks.
Log: coffeepeek-rating-ui-history-full-2026-10-18.log; do not overwrite it.

After unlock, the complete History suite passed **20/20** (43s), without code or
assertion changes: coffeepeek-rating-ui-history-rerun-2026-10-18.log.
Favorites then ran **14 cases, 13 passed / 1 failed**: the existing metadata
test could no longer observe the compact badge's numeric text in the merged
semantics tree. The rating's clearAndSetSemantics discarded that child text.
Replace it with one merging semantics group, retaining its precise description
and range while exposing the rendered label. Keep the favorites assertion
unchanged; strengthen both core read-only/compact tests to require visible text
in the default merged tree. Rebuild and rerun before treating this as resolved.
Initial failure log: coffeepeek-rating-ui-favorites-full-2026-10-18.log.

After the semantic repair, Favorites passed **14/14** (24s) with its original
metadata assertion unchanged; the strengthened rating class passed **6/6** (19s).
Logs: coffeepeek-rating-ui-favorites-fixed-2026-10-18.log and
coffeepeek-rating-ui-targeted-semantics-2026-10-18.log. The build/unit/Native/APK
selection passed again (57s): coffeepeek-rating-semantics-regression-2026-10-18.log.
No failed run was hidden or assertion weakened. The phone locked again after
these checks; final Sandbox visual inspection and final complete suites await
another unlock, without changing device security/settings.

Repeated idle locks motivated a narrowly scoped harness repair: the five
self-targeted test Activities set FLAG_KEEP_SCREEN_ON on their own foreground
windows. Activity disposal removes that window; there is no system setting,
lockscreen flag, keyguard dismissal or production/Sandbox Activity change.
All five test APKs compile (5s): coffeepeek-rating-test-window-build-2026-10-18.log.
The flag does not unlock an already locked phone; final device execution remains
pending human unlock, not assumed from compilation.

The updated Direct Sandbox **1.0.660-sandbox** was installed in place with the
semantic repair, SHA-256
`37fbb89c5472450d15279efe5f9778247ae578f2470a7574007c5c1273709127`.
Its runtime source is 611d3462 plus the merging-semantics repair; the additional
keep-screen-on code is test-only. The 1.0.659 identity below is historical.

After the next human unlock, physical Sandbox visual checks passed on 1.0.660:
actual feed in dark and light themes showed four complete stars and a partial
fifth beside 4.3. Profile -> All favorites showed a readable compact 4.5 badge
and its numeric text in the UI hierarchy. Profile -> All check-ins -> Coffee shop
filter -> visit opened the History card with the same fractional fill and localized
4,3 label. The Shop form's large stars accepted Coffee=5 and exposed the checked
score; the form was cancelled without submitting a record. Screenshots/UI dumps
are fake-only and local, outside Git. This is scoped visual/manual acceptance,
not live server/iOS/TalkBack/device-permission certification.

Prepared Direct Sandbox: **1.0.659-sandbox**, SHA-256
`d24794bc318f1cc15cdfd1a7c75df0060b150ee970dd263e60ea412b6027c4c9`.
Built from 4fcd08af plus this rating checkpoint's runtime diff. Only Sandbox
and self-targeted feature test packages are in the deployment scope. Final
Sandbox visual acceptance remains pending the unlock; APK assembly/installation
is not that acceptance. Ordinary CoffeePeek is not updated or mutated.
In-place Sandbox update succeeded while the phone was locked; the ordinary
package remains **1.0.648**. No uninstall, downgrade or data clear.

Studio inspected the core component and test file: no source errors there,
deprecated preview/test-rule warnings remain consistent with existing tooling.
Its inspection of app shared imports also exposed the missing common dependency.
After the dependency change, standalone Gradle compilers pass; the IDE's cached
source inspection still reports unresolved common imports until model refresh.
The requested scoped IDE build returned success with existing AGP/KMP warnings.
No clean final IDE source inspection, IDE preview rendering or iOS visual/release
acceptance is claimed.

## Final verification after all repairs

The runtime and test-host source is **d5fea551**; later plan updates do not change
the APKs. Source inspection of CoffeePeekRating reports no errors. The final
build/unit/Native selection and subsequent feature unit rerun (15s,
coffeepeek-rating-feature-units-final-2026-10-18.log) pass: **1144 unit executions**
across the same selection, zero failures/errors/skips. No repeated executions
are added to that count. Core/iOS compiler, ordinary APK/DEX and Sandbox
isolation/DEX checks remain green after the actual semantic fix.

Final complete UI reruns (self-targeted fake APKs, Pixel 7 / API 37): design-system
**62/62** (132s), Shop **33/33**, Home **40/40** (87s), History **20/20** (42s)
and Favorites **14/14** (23s) pass after both fixes: **169/169 UI cases**, with
zero failures/skips in this final complete selection. The targeted six rating
cases are included in 62, not counted again.
Logs use the suffix `-final-2026-10-18.log`, retaining all earlier failed,
aborted and intermediate runs separately. No assertions were relaxed to hide the
numeric-text regression. No device-lock interruption recurred across this final
selection. Foreground test-window keep-screen-on avoids changing phone settings;
human unlock is still required if the phone is already locked.

The scoped Android rating checkpoint is implemented and verified; the earlier
pending device notes above are historical and superseded by this final result.
No main/develop merge, live-account/backend acceptance, iOS visual test, actual
TalkBack session or IDE preview rendering is included. Those are not claimed as
passed merely because the common Native compilation and fake UI tests pass.
