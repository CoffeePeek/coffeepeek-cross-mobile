# Control sizing and accessibility conventions

Current source contract, reviewed 2026-11-27. This replaces historical sizing and
HIG-audit rollout logs; it is not a blanket claim of device accessibility acceptance.

## Targets and geometry

`CoffeePeekDimens.minimumTouchTarget` resolves to 44 points on iOS and 48 dp on
Android. Ordinary actions, icon controls, fields and selectors consume that
minimum before platform dispatch. Minimum heights allow text to grow; UIKit
controls retain intrinsic measurement and system geometry.

Editable `CoffeePeekRating` is the current owner-approved compact exception:
glyph-sized targets (24 dp for Large), 4 dp default gaps, packed start alignment
and horizontal scrolling in narrow containers. The component preserves individual
selected/enabled descriptions and reserves caption space. Read-only stars are
not exposed as five actions. Compact calendar/native-chrome targets also require
consumer-specific review rather than mechanically applying button dimensions.

Use the shared 4 dp spacing tokens, with 2 dp intermediate fixed geometry where
needed. This is a CoffeePeek convention, not an Apple requirement. Do not round
font metrics, hairlines, continuous safe areas, animation frames or artwork aspect
ratios to that grid. iOS system controls/SF Symbols and Android Manrope/Compose
rendering retain their platform behavior.

## Type, color and semantics

Use theme typography and explicit foreground/on-colors. Active unfilled text and
icons use the adaptive accent foreground; gold fills keep their readable on-color.
Primary and affinity are currently #FACC15. Secondary/error/success and opaque
photo backings come from semantic tokens, not per-feature arbitrary alpha.

Theme unit contracts check text/non-text contrast pairs, alpha-composited status
and photo backgrounds, palette tokens and type roles. Those calculations do not
certify arbitrary photos, native OS materials or every transient animation frame.
Decorative/disabled content is distinct from active text and actionable symbols.

Every action needs a localized purpose; a switch row is one accessible toggle,
not both row and child announcements. Busy/disabled controls must retain their
meaning and prevent duplicate dispatch. Callbacks, feature validation, analytics
and navigation do not move into the design-system.

## Review gates

Keep light/dark and large-text previews colocated and fake. Check actual consumers
at narrow/wide windows, increased text, RTL and keyboard/modal boundaries. Manual
Sandbox review, TalkBack/VoiceOver, Dynamic Type, reduced motion/transparency,
UIKit intrinsic sizing and touch spacing remain explicit platform checks.
Do not infer their success from unit tests, compilation or historical ledgers.
No automated UI/instrumented Compose/screenshot tests should be added or run.

Related: [adaptive layout](ADAPTIVE_LAYOUT.md), [insets](INSETS.md),
[component contracts](README.md). Reference design guidance:
[Apple buttons](https://developer.apple.com/design/human-interface-guidelines/buttons),
[accessibility](https://developer.apple.com/design/human-interface-guidelines/accessibility),
[typography](https://developer.apple.com/design/human-interface-guidelines/typography).
