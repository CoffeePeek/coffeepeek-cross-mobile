# Buttons, typography, color and spacing audit

2026-11-14. Scope: production UI in core/design-system, all feature impl consumers,
application composition/presenters, and the native iOS boundary. This continues
the sizing checkpoint described in [HIG_CONTROLS.md](HIG_CONTROLS.md), on
`codex/migration-hig-controls` based on `99de162dc`.

## Criteria and verification plan

Preserve callbacks, enabled/loading state, localization, analytics, navigation and
feature ownership. Inspect shared defaults and caller overrides together.

- Buttons: platform action targets, clear hierarchy, pressed/disabled/loading
  feedback, labels, minimum rather than fixed text heights and narrow layouts.
- Typography: readable roles, explicit leading, platform font fallback, scaling,
  wrapping and full accessible labels. Retain brand fonts where appropriate.
- Colors: actual foreground/background pairs in both themes, including composited
  alpha. Use 4.5:1 for active text and 3:1 for meaningful non-text controls as
  conservative checks; decorative and disabled content are classified separately.
- Spacing: coherent fixed tokens, separation of actions, safe-area/IME ownership,
  RTL and adaptable layouts. A 2/4-dp grid is our convention, not an HIG rule.

Use source inventory plus manual call-site review, focused unit regressions,
Android compilation/assembly with DEX verification, shared iOS compilation and
repository function/boundary checks. Do not create automated UI tests. Colocated
previews cover changed reusable components; manual Sandbox and Apple device
checks must be reported separately from source/test results.

References checked on the audit date:
[Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons),
[Typography](https://developer.apple.com/design/human-interface-guidelines/typography),
[Color](https://developer.apple.com/design/human-interface-guidelines/color),
[Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility),
[Layout](https://developer.apple.com/design/human-interface-guidelines/layout).

## Coverage

The production inventory includes 1,782 Kotlin/Swift/XML files, including 185
design-system sources/resources. Test, generated and build files are excluded.
The initial scan identified 1,286 color, 366 typography, 1,227 control and 1,172
spacing candidates; these are review locations, not counts of defects. Review
combined shared defaults, platform actuals and feature overrides. It is not a
claim that every possible runtime state was rendered.

Reviewed families: filled/tonal/outlined/text/icon actions, native menus and
dialogs, fields/search/select, segmented choices, sliders/rating/switches,
grouped/settings rows, cards/photo overlays, status/error/loading content,
navigation/sheets and typography/theme tokens. Consumer review spans discovery,
shop/check-ins, home/history, shelf, profile, settings, roasters/coffee,
collections, community, contributions, ownership, reports, auth and leaderboard.
The prior fixed-dimension audit remains recorded in HIG_CONTROLS.md.

## Findings and fixes

| Area | Finding | Implemented correction |
|---|---|---|
| Action targets | Several compact actions, photo removal and narrow ratings reduced the touch region; platform dispatch could bypass dimensions | Normal targets use 44pt iOS / 48dp Android; photo removal keeps its complete target inside the image; ratings scroll instead of shrinking |
| Text growth | Fixed button/field heights and compact profile statistics constrained larger text | Minimum heights, intrinsic UIKit sizing, scrollable segments, wrapping labels and two-column profile statistics at large text |
| Settings at 200% | Manual 320dp review found truncated action names and descriptions | Remove line limits from the shared settings row; retain intrinsic row height and scrolling, with paired large-text previews |
| Feedback | Some custom iOS actions bypassed pressed feedback; other actions faded readable content | Shared press treatment for Compose controls on both platforms; standard ripple where appropriate; native UIKit feedback retained |
| Typography | Body default was smaller than the iOS default; some roles inherited leading; card/profile labels were reduced independently | Explicit size/leading for all 15 roles, 17sp body, readable 13/12sp secondary roles, removal of local 10/11sp reductions where a normal role fits |
| Unfilled actions | Brand gold on white was only 1.92:1 | Retain gold fills; use the adaptive accent foreground for text/icons, focus/cursors, dates, loaders and native menu tint |
| Secondary text | Light secondary text on the badge surface was 4.36:1 | Darker secondary token; remove additional opacity from active placeholders and rating captions |
| Filled semantic controls | White on warm-gold secondary containers was 3.15:1; white on bright green was 2.28:1 | Explicit readable on-colors; darker light-theme success; adaptive error and explicit error containers |
| Surface roles | Unspecified Material surface/inverse roles introduced unrelated defaults | Complete both surface and inverse palettes, with transparent elevation tint |
| Status and control colors | Moderation pills, catalog tags, badge icons and native switch tint had weak contrast | Semantic status foregrounds, measured icon palettes, outline tokens and selected switch track contrast |
| Photos | White text/actions depended on arbitrary image brightness or gradients | Shared 68% black backing for readable captions/actions; decorative gradients retained |
| Spacing | Odd fixed geometry, negative photo-action offsets and tight action regions were inconsistent | 4dp spacing with 2dp intermediate geometry; percentage capsules; full target regions and adaptive layouts |

No domain/data contract, callback, navigation, analytics event or dependency graph
was changed. Shared component and affected screen previews remain colocated and
paired for light/dark. Large-text previews exercise buttons, fields, search,
segments, typography and profile content. Previews are source fixtures; compilation
does not mean they were rendered in an IDE.

## Intentional exceptions and limits

- HIG does not require even typography or every dimension to be divisible by four.
  17/13/11sp roles, line-height metrics, hairlines, shadows and continuous insets
  are not rounded mechanically. Brand artwork preserves its aspect ratio.
- Android retains Manrope; iOS uses the existing system font. HIG allows custom
  fonts. SF Symbols/native navigation and system-owned control geometry remain
  platform-specific; Android is not made into a pixel copy of UIKit.
- Compact calendar cells and native chrome are reviewed separately from normal
  buttons. Decorative dots, badges and disabled colors are not treated as active
  text or standalone touch targets. Availability indicators also include words.
- Safe-area/IME ownership follows INSETS.md and existing Scaffold consumption;
  system insets, keyboard dimensions and layout direction remain dynamic.
- Contrast tests measure actual theme tokens and composited color pairs. They do
  not certify every photo, glass backdrop, transient frame or native OS rendition.
- Full UIKit/Dynamic Type, VoiceOver, real TalkBack navigation, RTL rendering and
  Apple framework linking require their own device/macOS checks.

## Verification record — 2026-11-14

Source: `codex/migration-hig-controls`, base `99de162dc`, uncommitted working-tree
changes. No instrumented or automated screenshot/UI suite was used.

- JVM/unit reports: **2,595 tests, 0 failures/errors/skips**. The broad run executed
  2,457 cases across 58 tasks; 135 cases were up-to-date and 3 restored from cache.
  The 80 design-system cases were executed during this audit and repeated after
  the settings fix. Nine added color/typography tests check actual semantic
  pairs, alpha-composited status/photo backgrounds, all icon badge hues, native
  switch track contrast and all 15 typography roles. Existing sizing tests remain.
- `testDebugUnitTest :composeApp:testDirectDebugUnitTest verifyFunctionBodyLimits`
  passed. Composition/Sandbox contract coverage includes 657 unit cases. This is
  unit/contract evidence, not device UI or backend acceptance.
- DirectDebug and DirectSandbox assembled successfully. Both packaged runtime
  class checks and Sandbox manifest-isolation verification passed. These checks,
  design-system tests and function-body verification passed again after the
  manually discovered settings correction.
- Design-system `compileKotlinIosArm64` compiled successfully, including platform
  actuals. Full application iOS compilation/linking remains unavailable on this
  Windows host because of its SwiftPM cinterop; no full iOS build is claimed.
- Baseline comparison against HEAD found **zero added or grown entries**.
  `git diff --check` passed. No Gradle dependency, domain/data or ViewModel changes.
  No Android Studio MCP tools were available, so IDE inspections/rendered previews
  are not claimed. Existing Preview/expect-actual and coroutine opt-in warnings
  remain outside this change.

Manual environment: existing `CoffeePeek_API_37` AVD, `emulator-5554`, model
`sdk_gphone64_x86_64`, unlocked. Only `directSandbox` / `com.coffeepeek.sandbox` /
`com.coffeepeek.admin.sandbox.SandboxActivity` was installed and launched. The
package/version, Activity, absence of INTERNET and matching signing certificate
were verified before in-place updates; no uninstall, downgrade or data clearing.
The installed build is **1.2.0.2947-sandbox**, SHA-256
`1E82B2B1E93478537682EA84182E960005973ADFCCFBB7C795EB7F0502B083EE`.

Manual observations use Russian copy, light/dark themes and the existing Sandbox
local 320dp x 740dp / 2.0 font-scale fixtures; no device display/security settings
were changed. The initial ordinary-scale search/cards/profile checks passed.
Dark profile, large-text discovery and two-column profile statistics were
inspected. Home entry, the add-action sheet, home form fields/rating/switch/primary
button and photo-source sheet were opened. A synthetic photo was added to an
unsaved draft to inspect the in-photo removal target, then the form was closed
without publication; back/cancel returned to profile. No live account, camera,
gallery, analytics or backend write was used.

The first large-text Settings review showed ellipsized action names/descriptions;
this failure was retained and corrected in CoffeePeekSettingsRow. The final
light-theme 320dp/200% recheck confirmed full wrapped action names and descriptions
instead of ellipses. Separate Android dialog/modal windows did **not**
inherit the artificial Sandbox font scale: those observations certify their
actual rendered normal text only, not 200% modal text. System-font modal checks,
VoiceOver/TalkBack, RTL and physical-device/iOS visual review remain open gates.

Local inventory CSVs, before/after summaries and sanitized build/unit evidence
are kept outside tracked sources under the task's `hig-full-audit` artifact
directory. No private device logs or account screenshots were published.

Retained first failures: function-body verification initially rejected a one-line
growth in the Android map composable; extracting its attribution component fixed
it without extending the baseline. A prior switch test expected the old grey
literal; it was updated for the new palette. Integration compilation then found
an incorrectly renamed `primaryContainer` reference; the reference was corrected.
These failures are separate from the successful retries recorded below.

## Develop integration — 2026-11-14

The HIG checkpoint `800bb3ced` and its author-visible check-in publication parent
`99de162dc` were integrated with develop `ae6572821` before publication.
Resolutions preserve both platform sizing/contrast and develop's animation,
Reduce Motion, guest-feed, photo fallback and note-limit behavior. Confirmed saved
posts remain visible while the empty-feed loader transitions. Debug retains
develop's isolated `com.coffeepeek.debug` package.

The first integration run passed product unit tests but failed the function-body
check after a navigation change grew the root function by one line. Its business
mode navigation effect was extracted without changing route/save/restore behavior.
Baseline regeneration only shrank/removed entries; no new debt was accepted.

The final combined run passed: `testDebugUnitTest`,
`:composeApp:testDirectDebugUnitTest`, design-system `compileKotlinIosArm64`,
DirectDebug/DirectSandbox assembly, both packaged DEX checks, Sandbox isolation
and `verifyFunctionBodyLimits`. Current reports contain **2,624 unit cases,
0 failures/errors/skips**; unchanged tasks were reused on the final retry.
Both staged and working-tree whitespace checks passed.

The manual observations above identify the pre-integration APK. No additional
manual device pass or full iOS application build is claimed for this merge.
