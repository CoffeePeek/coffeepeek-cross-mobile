# Profile, detail headers and Home layout fixes

## Scope and ownership

Requested on 2026-10-23 for Android and iOS. Base: develop `cdfbadb6`.
Migration branch: `codex/migration-profile-home-layout`.

- Profile: safe area, prevent duplicate profile destinations, open summary directly as check-in history.
- Roaster/shop: consistent back icon and aligned hero controls that move with the hero.
- Home: map follows scrolling/pull refresh, visible refresh indicator.
- Sheets: remove two-stage opening of calendars and other consumers of the same presentation path.

Preserve account identity, history filters, back/cancel, map expansion/search,
favorites, analytics ownership/consent, keyboard behavior and platform conventions.
No data migration, backend/account writes or architectural migration is intended.

Three workers own disjoint profile/navigation, roaster/shop headers and Home/sheet
sources. Root owns integration, this record, serialized builds and manual review.

## Verification plan

1. Trace shared and platform entry points and existing design-system primitives;
   capture current Sandbox behavior before replacement.
2. Add focused pure/unit regression checks for changed non-trivial navigation,
   geometry or presentation readiness logic. Run relevant existing profile,
   roaster, map, refresh and calendar unit/contract suites. No automated UI,
   instrumentation or screenshot tests or test Activities/APKs.
3. Build `assembleDirectSandbox` and `assembleDirectDebug` sequentially in one
   invocation with `--max-workers=6`, including packaged DEX/isolation checks.
4. Before installation, verify package/launcher/version/signature; update only
   Sandbox in place on the already used unlocked `emulator-5554`, Pixel_8 AVD,
   model `sdk_gphone16k_x86_64`. No downgrade, uninstall or data clearing.
5. Manual Sandbox checks: profile safe areas and summary/back; repeated own/public
   profile navigation; roaster/shop headers and scroll; Home slow refresh,
   map scroll/expand/back; calendar and other shared sheets open/cancel. Cover
   light/dark and record size/accessibility limits. Keep images/diagnostics in
   the chat visualization directory outside tracked sources.
6. Record exact source/APK identity, executed suite counts and all limitations.

Windows can compile the configured Kotlin iOS KLIB targets; Xcode linking and native app execution require macOS. iOS source paths and pure shared
logic can be reviewed here; native sheet/nav/IME and device safe-area acceptance
remain an explicit macOS/device gate. IDE rendering and TalkBack are separate
checks, not implied by compilation or previews.

## Results

Baseline installed Sandbox reports
version 1.1.1.1232-sandbox / 1232; its exact source includes the earlier auth patch
and separate local packaging edits, so it is baseline visual evidence only.
## Implemented behavior and findings

- The user's clarification identifies the profile favorites carousel, not a
  device cutout. Profile outer horizontal padding clipped the scrolling viewport.
  The carousel now uses the full width and owns 16 dp start/end padding inside
  its scroll content; other profile sections keep their former 16 dp margins.
  The speculative cutout padding was removed before the final build.
- Root navigation on Compose and native iOS restores an existing UserProfile by
  slug. Display name/avatar changes do not create another identity. New pure
  route tests cover different users, changed metadata and unrelated destinations.
- Own summary opens existing VisitedPlaces directly; public summary opens a
  UserCheckIns page using the existing viewed-author VM, pagination and privacy
  filtering. Existing sheet consumers reuse the extracted list; own-card action
  semantics and analytics page ownership remain intact.
- Roaster and shop top controls share vertical alignment, platform Back shape and
  hero scrolling. Roaster adopts the shop edge-to-edge root contract; extra hero
  height preserves the former text position. iOS shop keeps native glass buttons.
  Separate photo Haze sources avoid capturing the controls into their own blur.
- Home map preview uses the measured placeholder anchor, including pull offsets.
  Both pull-refresh primitives move content through layout rather than only a
  graphics translation, so native map interop and the loader follow the layout.
- The active Android history calendar could move 0 -> 72 dp -> 0 when one initial
  read returned entries before other reads completed. A dedicated existing-state
  refresh intent now drives PTR only for an explicit Refresh, until its final
  read completes/fails/cancels. Initial/month/filter reads do not move the page.
  Three deferred VM tests cover these cases and session/cancellation cleanup.
- iOS native sheets previously waited for the opening transition, then started a
  second animateChanges after measurement. The detent is now invalidated as soon
  as measurement is available, without that second delayed animation. The same
  native path serves Home filters and photo-source selection. The exact second
  sheet remembered by the user is not identified without iOS reproduction.
- The former public calendar-only rendering is no longer reachable through the
  summary after direct navigation; provisional edits there were removed. Android
  wrappers already skip the partial detent and do not call show/expand twice.

## Verification results, 2026-10-23

Source: `cdfbadb6` plus the reviewed local changes in this migration worktree.
Exact changed-source hashes (39 source/resource files; manifest SHA-256 `aeb595e2334e28aa4d4abc6f7ba2b32ceba477235e3b447748fd79573da3ccbc`) are retained outside Git in `source-sha256.txt` beside
local screenshots and build logs. No other task's edits were included.

- First invocation: both Android assemblies/DEX checks, Sandbox isolation,
  five module unit tasks and both iOS Kotlin targets plus iOS common-test
  compilation passed in 3m28s; 1299 tasks, 728 executed, 571 from cache.
- After the clarified carousel fix and active history fix, the final invocation
  passed in 1m39s; 1304 tasks, 93 executed, 2 from cache, 1209 up-to-date. Unit
  tasks already verified without changed inputs were reused; app and changed
  profile/history suites executed as required. No failing tests or retries.
- Final unit results: **969 tests, 177 suites; zero failures/errors/skips**.
  Application DirectDebug:703/146 suites; design-system:35/9; profile impl:58/6;
  roaster impl:19/2; shop impl:103/11; check-in-history impl:51/3.
- `compileKotlinIosArm64`, `compileKotlinIosSimulatorArm64` and
  `compileTestKotlinIosSimulatorArm64` pass. Native unit tests were compiled,
  not executed. Xcode/framework linking and native runtime were not exercised.
- Final Sandbox APK: package `com.coffeepeek.sandbox`, entry
  `com.coffeepeek.admin.sandbox.SandboxActivity`, version
  `1.1.1.1963-sandbox` /1963. SHA-256:
  `5CC9F488E0E4D3FD621936095558F063C4064DC923977CE9A899502022702AA3`.
- Matching existing debug certificate verified before two in-place Sandbox
  updates (1232 ->1963, then updated same version with the final source). No
  downgrade, uninstall, data clearing, security change or live-app installation.
- Device: explicitly targeted unlocked `emulator-5554`, Pixel_8 AVD,
  `sdk_gphone16k_x86_64`; 1080x2400,420dpi,font scale1.0. Manual Sandbox uses only
  its existing synthetic local accounts, check-ins, photos and favorites.
- Manual coverage: dark profile carousel endpoints/middle, light resting carousel and direct own summary;
  Home held pull (loader visible and map moves), map expand/back; shop/roaster
  hero controls and scrolling; history month change and explicit pull/release.
  Home filter sheet opened/cancelled; a blank shop form was opened/cancelled
  without saving while returning the test environment to its initial state.
- Carousel proof: start card begins x42px=16dp; cards during scroll reach x0/1080
  rather than clipping at42/1038; the final card keeps the42px end margin.
- Header proof: Back centers y228px and favorite centers y227px on both detail
  screens (one-pixel pixel rounding); the old roaster centers differed15px.
  Roaster title still begins at y909px. Shop and roaster controls move with hero.
- Direct public history shows only Sandbox Friend's public visit; own combined
  history retains local private home entries. Public history and back were
  manually checked. Repeated-profile proof: Community -> Friend profile ->
  favorite shop -> Friend's check-in author returns to the existing profile;
  one Back returns directly to Community instead of another profile/shop copy.
- Calendar month title y553px stays unchanged for October ->September ->October;
  explicit pull still shows PTR and returns to the same resting position. Slow
  partial-read timing and cancellation are proved by the deferred unit tests,
  not claimed from the fast fake backend screenshots.
- Original dark theme and synthetic signed-in profile restored at the end.
- No automated UI/Compose/instrumentation/screenshot tests or test hosts created,
  restored or run. Screenshots/UI dumps remain local outside tracked sources.
- No Gradle dependencies, source sets, data repositories or persistence contracts
  changed. `git diff --check` passed. Original workspace still has only its two
  pre-existing packaging/run-mode edits. Existing preview deprecation/KMP/AGP
  warnings remain; IDE render/inspection was unavailable for this project.
- New helpers/components fit the function review limits. Existing large screen
  functions with localized slot/modifier changes were not mechanically rewritten.

Remaining gates: native iOS sheet opening/IME/rotation/back swipe and device safe
areas, Xcode linking, TalkBack/VoiceOver, additional font/window sizes and live
backend acceptance. The shared iOS sheet fix is source/KLIB checked only; do not
claim its animation is device-verified. The initial verification remained local; later direct develop publication is recorded below.

## Add-check-in FAB follow-up, 2026-10-23

Requested: the + action area must cover its entire visible circle. Both shared
legacy and migrated history use Material FAB (56 dp); the description was on
the inner 28 dp icon. Before editing, Sandbox XML confirmed the labeled node
was 28 dp and the clickable parent 56 dp; a manual tap near the top edge already
opened the picker. The fix moves the label/merged accessibility semantics onto
the full FAB and makes the icon decorative in both implementations. Preserve
size, styling, click callback, navigation and analytics exposure. Shared label
uses an explicit app resource. Paired local button previews use empty callbacks;
no ViewModel, Koin, backend or database is required.

Verification plan: Android Sandbox/Debug assemblies and DEX/isolation checks;
both Kotlin iOS compilations for the shared change; manual Sandbox bounds and
center/top/bottom/left/right inside-edge taps on global and history FABs, cancelling
each picker without saving. No new logic or UI tests. Native iOS hit testing and
VoiceOver remain device gates.

FAB verification completed on the same emulator/variant/account described above:

- Both Android assemblies, DEX/isolation checks and both Kotlin iOS targets pass
  in 1m47s:1262 tasks,94 executed,1 from cache,1167 up-to-date.
- Targeted unit suites:18 global check-in flow/store tests plus51 history tests;
  69 tests/5 suites,zero failures/errors/skips,no retries. Previous 969-test full
  run remains recorded above; it was not repeated for this semantics-only change.
- Matching certificate/package/version verified; updated only Sandbox in place.
  Final APK SHA-256:
  `0B1AE315C2AA4F172CB79FA03A6C30397CFA36C05AB0369CC39CDDF0DBAEAAA8`.
- Before: labeled icon bounds[928,2027][1002,2101],28dp,not clickable; parent
  button bounds[891,1990][1038,2137],56dp,clickable. After: one labeled Button
  at[891,1990][1038,2137],clickable/focusable,with no separate icon target.
- History labeled Button bounds[891,2148][1038,2295],also56dp.
- Manually tapped center and four inside edges of both implementations:10 picker
  openings and cancellations succeeded. Global points(965,2064),(965,2003),
  (903,2064),(1026,2064),(965,2125); history points(965,2222),(965,2160),
  (903,2222),(1026,2222),(965,2283). All pickers cancelled, no check-in saved.
- Initial UI dump during app restart returned a null root; after waiting for the
  actual UI, the next dump succeeded. The startup retry was not a test failure
  or a claimed successful first capture. Before/after/edge XML remains local.
- Paired button previews compile; no IDE preview rendering, automated UI test,
  native iOS hit test or TalkBack/VoiceOver run was performed. Native iOS device
  accessibility/hit testing remains the unverified gate.
- Exact source snapshot is saved as `fab-source-sha256.txt`; original source
  snapshot retained. No dependencies/navigation/analytics contracts changed.


## Roaster profile like size follow-up, 2026-10-23

Requested: match the shop profile like button size. Active shared Roaster detail
uses RoasterFavoriteHeart(overImage=true), which selected the compact DS Glass
plate32dp. Android shop uses FloatingGlass with the shared buttonHeight44dp;
legacy/native iOS shop uses the same44dp control token. Roaster profile now uses
that existing FloatingGlass style. Its accessible48dp touch target remains, and
roaster list-card calls(overImage=false) retain Plain. No new component, sizing
option, token or dependency. Both existing heart previews now show the relevant
profile style in light/dark and selected/unselected states.

Verification plan: existing DS like style unit regressions plus Roaster impl VM
tests; Android Sandbox/Debug assembly and packaged DEX/isolation; both iOS Kotlin
targets; manually compare shop/roaster headers in Sandbox with local fake data.
No new tests for a trivial visual style selection, no automated UI tests. Native
iOS appearance/hit testing still needs a device.

Like size verification completed:

- Both Android assemblies, DEX/isolation and both Kotlin iOS compilations pass
  in52s;1220 tasks,76 executed,1144 up-to-date. DS style regressions3 and roaster
  impl tests19:22 tests,zero failures/errors/skips,no retries.
- Sandbox version1963/package/entry and matching existing certificate checked;
  in-place update only. APK SHA-256:
  `9FC22A0ECE5307F0894F1BB47845BBE496460EEE360A7285E82FDF5CDF321339`.
- Manual comparison in light/dark on the same unlocked emulator: both profile
  plates are44dp, centered at the former y227px line; both touch targets remain
 48dp(126px). Screenshots like-size-shop/roaster and their light variants retained
  outside Git; comparison gallery updated. No favorite/account change performed.
- Shared core style helper already tests FloatingGlass against buttonHeight; no
  size arithmetic or extra component was introduced. Source snapshot retained as
  like-size-source-sha256.txt. Paired previews compile; IDE rendering unavailable.
- No automated UI/instrumentation tests. iOS native appearance remains a device
  gate; compilation is not visual verification. Original Sandbox dark theme
  restored. No new dependencies or analytics/favorite flow changes.


## Develop publication

Direct develop push requested on2026-10-23 after all follow-ups. Latest fetched
develop is `7ae09c7a` (hide native iOS map compass); the reviewed UI patch must
retain that commit. Final integration checks/publication identity are recorded
locally in `publish-verification.md` beside the preserved screenshots/build logs.
The original checkout remains separate, including its unrelated packaging and
native map edits; they are not part of this publication.
