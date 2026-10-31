# Home audit implementation — 2026-10-31

GitHub tracking: [#236](https://github.com/CoffeePeek/coffeepeek-cross-mobile/issues/236).
The audit's 20 findings are individual issues, #237–#256. Each fix gets a focused
migration branch and PR into `develop`; the user's request authorizes those merges.
Keep the existing brand palette and the HIG/Liquid Glass direction.

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
