# Home audit implementation — 2026-10-31

## #241 — catalog recovery

Base: develop `cdc20c94`. Branch: `codex/migration-home-catalog-recovery`.
Android Discovery owns loading/ready/failure catalog state and explicit retry.
Retain filters/options on failure, coalesce retries and preserve cancellation;
search stays available. Successful empty catalogs do not trigger reload on every
sheet opening. Retried city resolution must not overwrite a filter draft on Done.
Reuse design-system loader/retry components; preserve palette, routes and analytics.
Legacy iOS parity is recorded under #252, not assumed from Android checks.

Verification plan: catalog state/retry/concurrency/cancellation unit regressions,
Discovery and Sandbox contracts, Direct Sandbox/Debug assemblies and DEX/isolation.
Manual Sandbox CatalogFailure (persistent bean failure; recovery covered by contracts), CatalogEmpty,
Slow, both themes and retained selections. No automated UI tests or live writes.

Results on 2026-10-31: 126 Discovery unit tests (11 suites) and five Sandbox
contracts passed, zero failures/errors/skips. Direct Sandbox and Direct Debug
assemblies passed, including packaged DEX and Sandbox isolation. After correcting
the failure fixture, its five contracts and both assemblies passed again.
No IDE service was available. Native iOS behavior remains a #252 gate.

Manual: unlocked emulator-5554 / sdk_gphone64_x86_64, API 37, 1080x2400,
density 420, font scale 1.0. com.coffeepeek.sandbox / SandboxActivity updated
in place from 2591 to 2594 (1.1.4.2594-sandbox), matching signature. Source is
the base above plus this patch; final APK SHA-256:
ec99195b3ca1e78dae3d7983317776603a93d3b20ea594783086542698511776.
Persistent catalog error/retry is readable in light/dark; price selection survives
retry, Done and reopening, and search still returns results. Back dismisses the
sheet. CatalogEmpty returned empty non-roaster sections, but the existing disk
roaster cache retained options: the fully empty message is covered by unit state
tests/colocated preview, not claimed as device-verified. Loading is unit-tested;
Slow loading was not captured manually. The initial finite failure fixture was
consumed by other startup readers, so it was replaced with a persistent failure;
successful recovery is a contract test. Restored Seeded mode; no live writes,
data clearing or automated UI tests. Screenshots/dumps remain local and ignored.

GitHub tracking: [#236](https://github.com/CoffeePeek/coffeepeek-cross-mobile/issues/236).
The audit's 20 findings are individual issues, #237–#256. Each fix gets a focused
migration branch and PR into `develop`; the user's request authorizes those merges.
Keep the existing brand palette and the HIG/Liquid Glass direction.

## #242 — readable accent and status foregrounds

Base: `d85d045da63f2d5a38d26727aa2a1c41958330f4` (`develop`).
Branch: `codex/migration-home-accent-contrast`.

Keep gold fills, selection, photo rating accents and Manrope. Separate surface
text/thin-icon foregrounds from brand fills in the shared design system. Apply
them to default text actions, grouped action/checkmark rows, shop location and
status badges; remove overriding gold from both Home filter-sheet headers.
Existing explicit destructive/custom button colors remain caller-owned. Legacy
accent text aliases the shared role so snackbar/detail consumers stay consistent.
No data, analytics, dependencies or navigation change.

Verification plan: contrast regressions against actual light/dark surfaces and
composited status fills (normal text >=4.5:1, WCAG 2.2 contrast-minimum); existing
design-system/Discovery tests, Direct Sandbox/Debug assembly and DEX/isolation,
shared iOS KLIB compilation. Manual Home/filter sheet and selection in both
themes on the existing API 37 Sandbox. Existing paired component previews cover
enabled/disabled actions, selected rows and open/closed cards. Native iOS visuals,
large fonts, narrow windows and TalkBack remain separate gates.

Results on 2026-10-31: 164 unit tests (48 design-system, 116 Discovery;
24 suites), zero failures/errors/skips. Both assemblies, packaged DEX checks,
Sandbox isolation and design-system iOS Arm64 KLIB passed. Follow-up builds
verified preview background and the active floating-tab icon using the same role.
No IDE service; existing preview deprecation warnings remain. Full native iOS
compilation/visual review requires macOS; local application iOS tasks are skipped.

Contrast ratios (unrounded values used by tests): accent light on white 5.43:1,
on background 5.20:1; accent dark on dark card 9.91:1. Open/closed labels are
checked against their 14% fills composited over each card, all >=4.5:1. Brand
fill #EAB308 and photo badges stay unchanged. Shared callers include pagination,
expandable descriptions, catalog suggestions and grouped selection/action rows;
explicit custom/destructive colors are preserved. The legacy accent alias also
serves snackbar/detail text. Guidance: https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html.

Manual review: unlocked emulator-5554 / sdk_gphone64_x86_64, API 37, 1080x2400,
density 420, font scale 1.0. Direct Sandbox 2582 updated in place to 2584, same
verified signature, package com.coffeepeek.sandbox / SandboxActivity. Final APK
SHA-256: 4fd6aa258c7518583eee231c4c7e0c8735c3550c7b68291bff369b3d8cd05cab.
Source: base above plus this patch. Light/dark Home and filter headers, enabled/
disabled Reset, selected row/checkmark and Cancel passed. Gold filled add/filter
controls and photo accents remain. Initial launch dump had no root while starting;
review began after settled state. Local screenshots/dumps remain ignored.
Native iOS visuals, large fonts, narrow windows and TalkBack were not executed.

## #237 — explicit pagination retry

Base: `33ffd3ee59f924c6a586d8ba4efda860a94e46b9` (`develop`).
Branch: `codex/migration-home-pagination-retry`.

Preserve loaded shops, pagination order, query/filter mapping, account and
cancellation guards, and search analytics. Android Discovery owns its presentation
state; the legacy Feed remains the active iOS path and needs the same bug fix.
The shared design system owns only the loading/error/retry footer, with localised
resources and paired previews. No module, dependency, palette or navigation migration.

Verification plan: demonstrate the original automatic retry with a failing unit
regression; exercise explicit retry, repeated failure, refresh/filter/query/account
changes in in-memory ViewModel tests; run relevant Sandbox backend contracts;
assemble Direct Debug and Direct Sandbox with packaged DEX/isolation checks;
compile the shared design system for iOS. Manually review the second-page failure
and recovery in Direct Sandbox on the existing unlocked API 37 emulator, including
light/dark themes and an enlarged font. No automated UI or screenshot tests.

The manual `PageFailure` Sandbox fixture returns the ordinary paged data, fails the
first attempt at page two, then succeeds on retry. Loading page one rearms it.

### Verification record

- Original regression: one executed test failed, confirming repeated page-two requests.
- An initial test command used a nonexistent Sandbox unit-test task; no tests ran.
  Application unit tests use `testDirectDebugUnitTest`, with in-memory repositories,
  without installing or launching the live application.
- Final verification on 2026-10-31: 213 tests in the selected suites, zero failures,
  errors or skips. Discovery presentation (109) and application Feed/Sandbox
  contracts (67) executed; unchanged domain (15) and data (22) tests were up to
  date from the passing audit run. Earlier focused execution passed 36 Search
  and 22 Feed ViewModel tests. No retries were needed after the fix.
- Direct Sandbox and Direct Debug APK assembly passed, including packaged DEX
  runtime-class checks and Sandbox isolation. Design-system iOS Arm64 KLIB
  compilation executed successfully. Existing Compose preview deprecation and
  unrelated test warnings remain; no IDE inspection/rendering service was available.
- Manual run: unlocked `emulator-5554`, `sdk_gphone64_x86_64`, API 37,
  1080×2400, density 420, font scale 1.0. Source was the base above plus this
  task's patch; `directSandbox`, package `com.coffeepeek.sandbox`,
  `SandboxActivity`, version `1.1.4.2574-sandbox` / 2574. Verified a matching
  signing certificate and updated the older 2573 installation in place.
  APK SHA-256: `76917f493448dbcb22e494e8cc98ef1b18c6f3bc43298b8009de3cb8c9a59b83`.
- Light and dark: scroll through page one, observe a stable inline failure,
  explicitly retry, and see page-two cards appended with earlier cards retained.
  Error text and retry remain reachable above navigation; Back returns to Home.
  The initial install briefly showed Android's updating screen; review began only
  after the settled app launch. Screenshots/dumps/logs remain in ignored build output.
  Original light app theme and font scale 1.0 were preserved/restored.
- Enlarged-font review is pending: automatic approval review rejected changing
  the emulator's system font scale, including a proposal with guaranteed restore,
  citing the device-settings rule. No font-scale command ran; clarification was
  requested. TalkBack, narrow windows, native iOS visual review and real backend
  acceptance remain separate gates. No automated UI tests were created or run.

## #238 — system Back from roasters

Base: `1d53ebfdd6398e2e1b7fe53dc2517651c7c544ee` (`develop`).
Branch: `codex/migration-home-roasters-back`.

Own this fix in Discovery presentation: resolve the tab's Back target in the order
roasters, expanded map, search, then delegate to root navigation. IME and modal
windows keep their platform Back handling. The roaster arrow and system Back
share the existing close/cancel operation; retain Home's list state and analytics.
No visual, resource, dependency or root-navigation changes are planned.

Verification plan: unit-test target priority for Home, roasters with/without a
query, map and search; execute Discovery presentation tests and assemble Android
Direct Sandbox/Debug with runtime-class/isolation checks. Manually verify blank
and nonblank roaster queries, keyboard-first Back, arrow parity, retained scroll,
map/search/modal dismissal and ordinary Home Back in the explicitly selected
Direct Sandbox on the existing API 37 emulator. Native iOS is unchanged.

Original failure reproduced on 2026-10-31 with Sandbox 2574: Home → all roasters →
system Back opened `NexusLauncherActivity`. Previous PR #257's Android and iOS CI
have now both passed.

Results on 2026-10-31: all 115 Discovery presentation unit tests (10 suites,
including six new Back-priority cases) executed and passed, with zero failures,
errors or skips. Both Android assemblies passed packaged DEX checks; Sandbox
isolation passed. Existing preview/BackHandler deprecation warnings remain.

Manual checks used unlocked `emulator-5554`, `sdk_gphone64_x86_64`, API 37,
1080×2400, density 420, font scale 1.0. The matching-signed Direct Sandbox APK
updated 2574 in place to `1.1.4.2576-sandbox` / 2576; package
`com.coffeepeek.sandbox`, entry `SandboxActivity`. Source: the base above plus
this task's patch. APK SHA-256:
`ac9b00cd5b2aac0f863f9c0dbdcf43b56a447e11a4e76f6d3272946a0b42edb1`.

Light/dark Back from all roasters returned to Home at the same scroll position
(roaster section bounds unchanged). With a nonblank query, the first Back hid
the IME and retained the query/overlay; the second closed the overlay and cleared
the query. Arrow parity, sheet-before-search dismissal, map collapse and ordinary
Home Back to launcher also passed. Gboard's stylus onboarding appeared during
input and was dismissed before testing IME Back; it was not an app failure.
Screenshots, UI dumps and build logs remain local in ignored build output.
No automated UI tests were created or run. Large fonts, narrow windows, TalkBack
and predictive-back gesture animation remain separate device-review gates.

## #243 — readable modal sheet fallback

Base: `e2a983e7407040ac2c358b32028d48f7cd349ee3` (`develop`).
Branch: `codex/migration-home-sheet-readability`.

The shared fallback sheet used a translucent surface in a separate modal window
without backdrop blur, allowing sharp Home text and cards to show through.
Restore its opaque theme surface until cross-window blur can be verified. Keep
the existing palette, shape, gestures, insets, content and iOS native sheet path.
The existing colocated light/dark sheet previews exercise this surface.

Verification plan: execute existing design-system and filter/detent unit tests;
assemble Direct Sandbox/Debug with DEX/isolation checks and compile iOS KLIB.
Manually inspect light/dark filter sheets in Direct Sandbox, check Back and
drag-to-dismiss, discard an edited filter draft, apply a draft, and verify keyboard
and accessibility semantics. No automated UI/screenshot tests or color-value tests.

Results on 2026-10-31: 45 design-system and 10 application filter/detent tests
executed (15 suites total), zero failures, errors or skips. Android Direct
Sandbox/Debug assembly, packaged DEX and Sandbox isolation passed; design-system
iOS Arm64 compilation executed successfully. Existing preview deprecation
warnings remain. Native iOS presentation still takes the original native path.

Manual run: the same unlocked API 37 emulator (`emulator-5554`,
`sdk_gphone64_x86_64`, 1080×2400, density 420, font scale 1.0). Matching-signed
Direct Sandbox updated 2576 in place to `1.1.4.2578-sandbox` / 2578,
`com.coffeepeek.sandbox` / `SandboxActivity`. Source: base above plus this patch.
APK SHA-256: `eded8c4939f018010f5a27b8ef42951018edba829b1397fe88927668aa3b805b`.

The original light-theme screenshot showed sharp Home labels/card silhouettes
through the sheet. Light/dark screenshots after the fix show an opaque readable
surface. Editing Arabica then Back discarded the draft; reopening showed no
selection. Applying it with Done retained the checked selection on reopening.
Back and handle drag dismissed the sheet; opening from a search with visible
IME kept the sheet usable and Back returned to the preserved query. UI semantics
retain the close description and checkable/checked filter states. Original light
theme and empty search/filters were restored. No live backend writes occurred.

Screenshots/UI dumps/logs stay in ignored build output. TalkBack traversal,
large fonts, narrow windows and native iOS visual review remain unexecuted gates;
semantics inspection does not certify those. Cross-window optical blur is still
unimplemented: this task intentionally supplies the readable fallback from #243.

## #260 — Home tag sizing and alignment

Base: `dce41017568df1dfbb2935d4f9bc064710245113` (`develop`).
Branch: `codex/migration-home-tag-alignment`.

The user requested this check before continuing #239. Direct Sandbox 2578 on
2026-10-31 confirmed that quick-filter labels were about 2.5 dp above the centre
of their 48 dp pill: the minimum-height Box placed shorter content at the top.
Roaster tags used a different Material chip with a 32 dp visual and larger text.
Both rows started at the correct 16 dp inset, but their gaps were 4 and 8 dp.

Use the existing shared glass chip for both rows: 44 dp minimum visual height,
48 dp minimum touch target, centred content, 14 sp labels and consistent 8 dp
gaps. Keep scalable minimum heights, existing palette/native background, actions,
filters and analytics. The shared RoasterTagFilters also serves the full catalog;
verify that route. Existing paired previews cover icons, plain labels and selected
states; roaster previews now include the actual long label. No module changes.

Verification plan: existing design-system, Discovery and roaster unit tests;
Android Direct Sandbox/Debug assembly with DEX/isolation checks, shared iOS KLIB
compilation. Manually inspect dimensions/alignment, selected/unselected states,
horizontal scrolling, roaster selection/reset and both themes in Direct Sandbox
on the existing API 37 emulator. System font scale remains 1.0; enlarged-font,
narrow-window, TalkBack traversal and native iOS visuals remain separate gates.
No automated UI/screenshot tests or value-mirroring unit tests are introduced.

Results on 2026-10-31: 183 unit tests executed (45 design-system, 115 Discovery,
23 roaster; 26 suites), zero failures/errors/skips. Both Android assemblies,
packaged DEX checks and Sandbox isolation passed. Design-system and roaster iOS
Arm64 KLIB compilation passed. Existing preview deprecation warnings remain;
Studio MCP and preview rendering were unavailable.

Manual review: unlocked `emulator-5554`, `sdk_gphone64_x86_64`, API 37,
1080×2400, density 420, font scale 1.0. Matching-signed Direct Sandbox updated
2578 in place to `1.1.4.2580-sandbox` / 2580, `com.coffeepeek.sandbox` /
`SandboxActivity`; source is the base above plus this patch. APK SHA-256:
`c188a6891715bf420df4d926e8b81f71602b97a3ec4cd8f46f812f87a41cdf52`.

Quick-filter text bounds changed from y=365..415 to 371..421 inside the unchanged
y=333..459 touch target: centres now both equal 396 px. Plain/icon labels and
selected/unselected states retain that alignment. Roaster labels centre at
1527 px inside y=1464..1590; both rows have 126 px / 48 dp touch height and
21 px / 8 dp gaps. Light/dark appearance, horizontal scroll to the last quick
filter, selection/reset semantics, full roaster catalog and Back passed.
Screenshots/dumps remain local in ignored build output. Native iOS visuals,
large fonts, narrow windows and TalkBack traversal were not run.

## #239 — retained-content refresh feedback

Base: `614d9d98a16432ef97185f5e1ce80b68b79c39cb` (`develop`).
Branch: `codex/migration-home-refresh-feedback`.

Both Android Discovery and the retained iOS Feed path preserve loaded shops on a
failed refresh but render errors only for an empty list. Add a localized inline
refresh error before retained cards, with the existing guarded Refresh action.
Reuse the shared list retry presentation through a refresh-specific component;
keep its polite live region, palette and native button path. Initial-load errors
and pagination stay separate. Discovery pagination also uses localized copy,
not an unreviewed raw server message. No navigation, data or analytics rewiring.

Verification plan: ViewModel regressions for retained cards, retry, duplicate
refresh suppression and recovery, plus legacy Feed parity and fake-backend
contracts. Add a manual-only Sandbox fixture that succeeds on the first search,
fails the first refresh of that query and succeeds on explicit retry. Compile
Android Sandbox/Debug, check DEX/isolation, compile affected shared iOS code.
Manually cover Home and search results, both themes, error/retry persistence,
initial failure and pagination separation. UI previews are colocated; no
automated UI tests. Native iOS visuals and larger font/window gates remain open.

Results on 2026-10-31: 214 unit tests executed (116 Discovery, 45 design-system,
53 legacy Feed/Sandbox contracts; 25 suites), zero failures/errors/skips. The
refresh regression covers retained cards, Home/search presentation, duplicate
requests, first-load separation and successful recovery. The fixture contract
also verifies per-query failure isolation, pagination and resetting its mode.
Android Direct Sandbox/Debug assembly, DEX runtime classes and Sandbox isolation
passed. Design-system iOS Arm64 KLIB compilation executed and passed. The requested
`composeApp:compileKotlinIosArm64` was SKIPPED by the Windows-host build; full iOS
compilation remains a macOS CI gate, not a completed local check.

Localization audit: Discovery Android resources and legacy application resources
currently provide Russian copy; the design-system defaults support English and
Russian. Both screen paths explicitly pass their own message/retry resources so
an English system locale does not introduce English into the Russian screen.
The first check run passed; a second build/test run verified these resource
overrides. Existing preview/test warnings remain; no Studio inspection service.

Manual review used unlocked `emulator-5554`, `sdk_gphone64_x86_64`, API 37,
1080×2400, density 420, font scale 1.0. Verified matching signature and updated
Direct Sandbox 2580 in place to `1.1.4.2582-sandbox` / 2582, package
`com.coffeepeek.sandbox`, entry `SandboxActivity`. Source: base above plus this
patch. APK SHA-256:
`ce229f88c45c2d7b94ef0a930d35a685447cd06d8faf02cd43bfc741aaffb02e`.

Home refresh failure retained the same shop cards and displayed the localized
message/retry in both themes; it stayed visible until explicit retry. Recovery
removed the message and retained the cards. A nonblank `Sandbox Cafe` search
retained its query/card through failure and retry. `SearchFailure` still showed
the existing initial-load error without a retained-content message. `PageFailure`
showed a distinct localized footer, then appended cards after explicit retry.
Screenshots/dumps remain local in ignored build output. The install briefly
showed Android's updating screen and an early UI dump had no root; checks began
after a settled fresh Sandbox launch. Original light theme and Seeded fixture
were restored. No live services or UI automation tests were used; system settings
were not changed.
TalkBack traversal, enlarged fonts/narrow windows and native iOS visuals remain
unexecuted gates; the reused retry component retains its polite live region.

## #244 — shared glass search control

Base: `18fd530709d10cb761b5911901cf1cf7dc7af393` (`develop`).
Branch: `codex/migration-home-search-glass`.

The remaining mismatch is the search field's opaque fill/solid border. Reuse
CoffeePeekNativeControlBackground (Android glass approximation; iOS decorative
UIKit glass, with Compose owning input/accessibility). Home filter/map buttons,
add action and navigation already use their shared glass path; photo ratings
remain content badges. HIG materials distinguish controls/navigation from content:
https://developer.apple.com/design/human-interface-guidelines/materials.

The retained iOS Feed calls CpSearchField; its NativeSearchField currently returns
false. Delegate that fallback to the shared field, keeping the native hook and
focus/search callbacks. Preserve text selection/IME composition via existing
SyncedTextFieldState. Keep a 44 dp visual pill inside a stable 48 dp minimum
input/clear target, matching the corrected filters and avoiding a height change
when Clear appears. No data/filter/analytics changes or new module.

Verification plan: existing input-state/Discovery/roaster regressions; Android
Sandbox/Debug assembly and DEX/isolation; shared iOS KLIB compilation. Colocated
light/dark previews include empty, populated and disabled input. Manual Sandbox
Home/search, typing, clear, Back, horizontal filters and both themes; inspect
clear touch target and stable geometry. Native iOS visuals/full build remain
macOS gates; font/window/TalkBack gates remain unexecuted.

Results on 2026-10-31: 187 tests (48 design-system, 116 Discovery, 23 roaster;
27 suites), zero failures/errors/skips. Existing input regressions exercise delayed
echo, active IME composition and external clear. Both Android APK assemblies,
DEX/isolation checks and design-system iOS Arm64 KLIB passed. Final build also
verified the additive keyboardOptions argument preserving legacy iOS native IME
configuration. No Studio renderer; existing preview deprecation warnings remain.

Manual: emulator-5554 / sdk_gphone64_x86_64, API 37, 1080x2400, density 420,
font scale 1.0. Direct Sandbox updated 2584 to 2586 in place, matching certificate,
com.coffeepeek.sandbox / SandboxActivity. Source: base above plus this patch;
APK SHA-256: 4db4d7fa721de92cf457d073270dafb31f8b9f59b31ffb82b89e363d2ec48c65.
Light/dark search material matches the adjacent controls. Input bounds y=170..296
remain 126 px / 48 dp both empty and populated; Clear's clickable parent is also
126x126 px. Typing, results, clear, Back and horizontal quick-filter scroll passed;
light theme and empty Home restored. The first immediate ADB input after focus
produced `andbox`; after waiting for settled focus, `Sandbox` and dark `Coffee`
were received intact. Keep this first observation: physical/IME startup timing
was not conclusively diagnosed by this manual run. Screenshots/dumps are local.
Native iOS input/visual review, larger fonts, narrow windows and TalkBack remain
unexecuted gates. No UI automation tests or live backend writes.

## #240 — favorite failure feedback

Base: `a340c1ee9286e4247b9d7580379ec96b8afd2e93` (`develop`).
Branch: `codex/migration-home-favorite-feedback`.

Android Discovery SearchViewModel owns the optimistic write and rollback. Add
a typed/identified pending failure to rendering state, acknowledged by its own
ID so an old dismissal cannot erase a newer failure. Account changes clear it;
late old-account failures cannot publish it. Preserve authoritative observed
favorites, per-shop duplicate guards and cancellation (including Result-wrapped
cancellation). Keep existing composition favorite/analytics adapters and the
retained iOS writer untouched, as this finding targets migrated Android Discovery.

Reuse shared content-card/icon-button primitives for the feature-specific
localized notice, with SnackbarHost's accessible timeout and polite live region.
Place it above navigation and the floating add control; defer display under
modals/map/roaster overlays or an inactive tab. No new backend calls or analytics.

Verification plan: reproduce silent failure with an in-memory regression, then
cover Result/throw failures, retry/dismissal, authoritative membership, duplicate
writes, cancellation and account switching. Run Discovery and existing Sandbox
contracts; assemble Sandbox/Debug with DEX/isolation. Manual-only Sandbox SaveError
checks on Home/search, both themes, dismissal and successful retry. Native iOS
writer/UI is outside this Android finding; enlarged fonts/windows and TalkBack
remain separate gates. No automated UI tests or real-account writes.

Results on 2026-10-31: the original focused regression executed and failed (1/1):
rollback had no pending feedback. After the fix, 121 Discovery and 31 Sandbox
backend contract tests executed successfully; 48 unchanged design-system tests
were UP-TO-DATE from #244 (200 tests across the selected suites, zero final
failures/errors/skips). Five new tests cover thrown failure/recovery, identified
dismissal, account reset and thrown/Result cancellation. Existing tests cover
optimism, duplicate suppression, authoritative membership and late account writes;
assertions now check feedback too. Android Direct Sandbox/Debug assembly and
packaged DEX/isolation passed. No new Gradle dependencies or boundary changes.
Discovery's favorite adapter and search/gate/exposure analytics ownership are
unchanged; tests use in-memory sources. No IDE inspection service was available.

Manual verification used unlocked emulator-5554 / sdk_gphone64_x86_64, API 37,
1080x2400, density 420, font scale 1.0. Verified identity/signature and updated
Direct Sandbox 2586 to 2588 in place (com.coffeepeek.sandbox / SandboxActivity).
Source: base above plus this patch. APK SHA-256:
cb455d5ace9dbc6fabd2eaa4a925ba4099df28957cd12f83f3eaaf746c3c12f5.
SaveError produced the localized notice and restored the selected heart on Home
in both themes and in a nonblank Cafe search without losing its query. Repeated
failure and explicit Close worked; no feedback after successful fake removal
and re-add in Ready. Notice text y=1780..1886 stays above the add button and
navigation; the close button remains reachable. Ready/Seeded, light theme, empty
Home and original fake favorite were restored. No live writes or system settings
changed. Screenshots/UI dumps remain in ignored local build output.
Native iOS writer is outside this Android finding; native iOS visuals, large
fonts/windows and TalkBack traversal remain unexecuted gates.

### Final CI cross-check for this three-task batch

At final review, PR #263 Android CI attempt 1 failed in
`:composeApp:mergeExtDexPlayDebug`: D8 `OutOfMemoryError: Java heap space`, run
41179324931. This is retained as a failed check, not relabeled successful. The
failed job was rerun without source changes (attempt 2 still running at handoff).
An additional local `:composeApp:assemblePlayDebug --max-workers=6` on the combined
implementation (`89b643a2`) passed, including packaged runtime-class verification;
external DEX merge was FROM-CACHE. Play Debug was neither installed nor launched.
PR #261 and #262 CI are now successful on both platforms. New #263–#265 CI
(including full macOS iOS builds) is still running; local KLIB compilation does
not replace the full native build/visual gates.
