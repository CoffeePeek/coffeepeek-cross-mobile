# Control sizing and HIG alignment

Scope: shared controls, their Android/iOS implementations and size overrides found
in the 2026-11-14 dimension audit. Base revision: `99de162dc`; child checkpoint:
`codex/migration-hig-controls`. Business behavior, navigation and analytics stay
with their existing owners.

This is the first sizing-pass record. The subsequent [full HIG audit](HIG_AUDIT.md)
supersedes its test/device status and adds typography, contrast and consumer fixes.

## Contract

- Use a 44-point target on iOS and a 48-dp target on Android for application-owned
  actions. Treat these as minimums; text may require more space.
- Apply width/minimum-height options before platform dispatch. Native UIKit
  controls retain native appearance, intrinsic sizing and accessibility behavior.
- Prefer system segmented controls on iOS; keep short-label selection semantics.
  Large text uses horizontally scrollable options instead of shrinking targets.
- Keep safe areas, viewport sizes, native switch geometry, font scaling and
  animation frames continuous. HIG does not require an even-number grid.
- Use a 4-dp spacing scale with 2-dp intermediate values for our fixed geometry.
  This is a CoffeePeek convention, not an Apple requirement. Thin strokes,
  typography (including 13/17-point styles) and artwork have separate rules.
- Preserve artwork aspect ratios inside consistently sized containers.

Sources checked 2026-11-14:
[Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons),
[Accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility),
[Layout](https://developer.apple.com/design/human-interface-guidelines/layout),
[Segmented controls](https://developer.apple.com/design/human-interface-guidelines/segmented-controls),
[Typography](https://developer.apple.com/design/human-interface-guidelines/typography).
Apple's accessibility table also lists a smaller compact minimum; CoffeePeek
chooses the general 44-point button recommendation as its normal action target.

## Verification plan

Preserve click/selection callbacks, enabled/loading states, semantic labels,
localization and existing analytics. Check unit contracts for metrics, constrained
rating layouts and artwork sizing; retain existing tests. Compile shared Android
and iOS code, assemble DirectDebug with its packaged DEX checks, and run the
repository boundary/function-body checks. Do not add automated UI tests.

Colocated light/dark previews cover normal and large-text controls. Device-only
gates: manual Sandbox review at large text, narrow widths and RTL; iOS UIKit
intrinsic sizing and Dynamic Type; VoiceOver/TalkBack and touch spacing. A Windows
compile or unit test cannot certify those gates. No app run is implied by assembly.

## Implemented

`CoffeePeekDimens.minimumTouchTarget` is platform-owned (iOS 44, Android 48).
Buttons, icon actions, fields, selectors and compact actions consume it. Button
width/minimum-height modifiers reach the native implementation. UIKit buttons
use intrinsic remeasurement and multiline titles; the search input supplies an
intrinsic height based on its system-font field instead of a fixed outer height.

iOS segments now use the system control rather than a separately drawn selection
plate. Large text and insufficient room switch to scrollable options. Compose
selection backgrounds are inset inside the full-height touch region. Rating
choices keep their action size and scroll in narrow containers; they no longer
divide insufficient space into smaller targets. Search clear actions clamp their
target, including the former 28-dp roaster override. Explicit 44-dp common icon
actions now use the platform metric.

The Android custom switch uses a 52x32 track and a 28-dp thumb; iOS retains its
native UISwitch. Fixed layout outliers in the audit were normalized, including
the shop footer maximum (352), rating gaps, badges, grouped rows and feature icon
sizes. BYN artwork retains its aspect ratio inside even-height boxes. Capsule
sentinels now express a percentage shape. Tiny informational counters/navigation
captions have an 11-sp floor; image-placeholder lettering remains scalable artwork.
Android's compact navigation retains its existing large-content viewer and full
accessible labels; iOS retains its native navigation.

No domain/data contracts, callbacks, events, analytics emission, resources or
dependency graph changes were introduced.

## Verification record — 2026-11-14

Source: `codex/migration-hig-controls`, based on `99de162dc`, working-tree changes.

- Executed 778 unit tests across design-system and eight affected feature impl
  modules: 0 failures, 0 errors, 0 skipped. Design-system: 71 tests (four new).
  Other counts: history 82, contributions 81, discovery 173, home check-in 81,
  profile 72, roaster 23, shelf 87, shop 108.
- DirectDebug assembly and packaged DEX runtime-class verification passed.
- Design-system Android and iosArm64 compilation passed.
- The final integration run passed Android assembly, packaged DEX checks,
  design-system tests and function-body verification again. The requested
  `composeApp:compileKotlinIosArm64` task was **skipped**, not compiled: the
  application's SwiftPM cinterop cannot run on the Windows host. The successful
  design-system iosArm64 compilation does not certify the complete iOS app.
- Function-body verification passed. Baseline regeneration only removes/shrinks
  existing entries; it adds no accepted violations. Field decoration/keyboard
  handling and rating rendering were split into cohesive private functions.
- Repeated the static dimension inventory across 1,781 production source/resource
  files, including 174 design-system Kotlin files. The remaining non-even dp
  candidates are strokes, shadows, artwork, platform insets or defensive layout
  bounds; the audited fixed control/spacing outliers have been addressed.
- The first combined full run stopped because Gradle disallows running the
  baseline generator and its verifier together without a declared task dependency.
  The separate verification retry passed. This was not a failing product test.
- Existing Compose Preview deprecation and expect/actual beta warnings remain;
  preview rendering was not inspected. Large-text paired previews were added to
  buttons, outlined fields and search; segments retain their large-text previews,
  and rating previews include a constrained-width example. Platform actuals use
  the shared component previews; native segment previews remain colocated.
- ADB reports no connected devices. No install, app launch, account operation,
  instrumented test or screenshot test was performed. Manual Sandbox review,
  actual Dynamic Type/VoiceOver/TalkBack behavior, full iOS application compilation
  and Apple framework linking remain device/macOS gates. Source alignment is not
  a certification of every HIG topic or of visual behavior on devices.
