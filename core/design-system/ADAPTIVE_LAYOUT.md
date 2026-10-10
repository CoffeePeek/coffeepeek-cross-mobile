# Large type and RTL preparation

This slice remains inside design-system. The application, old components,
navigation, platform configuration and dependency graph are unchanged.

## Corrected contract

StepperRow previously constrained each action and its container to 32.dp height.
The two custom click targets now have minimum 48.dp width/height and content
padding; their height/width can grow with the symbol typography. The container
has a minimum height, not a fixed height. Caller-owned value/actions and logical
decrease/increase ordering remain unchanged. In RTL, decrease is at logical
start (right), increase at logical end (left); arithmetic signs are not mirrored.
The group row grows with a wrapped label. This intentionally increases the
compact legacy control size rather than relying on overlapping expanded touch
regions. There is no API signature change or legacy component update.
Grouped rows also use a 48.dp minimum instead of 44.dp. Action rows and stepper
halves explicitly expose the button role; checkbox/switch roles are preserved.

## Checks and preserved policies

AdaptiveLayoutContractTest uses packaged Manrope, a deterministic 320.dp viewport
and injected density/fontScale. It checks:

- Stepper action bounds at scales 1.0 and 2.0, LTR/RTL ordering, unoverflowed
  label/value/symbol text and caller callbacks.
- Long AppButton text wraps without visual overflow at scale 2.0 in both layout
  directions and both themes; the button remains clickable.
- Long switch labels wrap at scale 2.0; selection and callbacks remain hoisted.
- Ordinary-scale grouped action/checkbox rows have 48.dp minimum targets in both
  directions and keep their callbacks and selection semantics.
- Settings labels keep their full semantic text despite intentional one-line
  ellipsis; a caller-provided trailing slot stays on the correct logical side
  without overlapping the label at scale 2.0.
- Segmented selection preserves logical order and 48.dp minimum target height
  at scale 2.0 in both directions.

Settings titles, top-bar titles, search and segmented labels retain their
intentional compact/one-line policies. This is not a universal promise to display
arbitrarily long text without ellipsis. Extremely narrow parents, large stepper
values, unusually wide trailing slots and excessive tab counts need consumer
layout decisions, not a global adaptive base class.

Simple String Text semantics in the pinned Compose version reconstruct a
MultiParagraph at the original parent maxWidth, which can exceed the compact
measured text width and falsely report overflow (e.g. a 17px value in a 288px
parent). The test helper reconstructs layout at the measured width, with the
same intrinsics, maxLines and overflow policy, before checking fit/ellipsis.
This is test-side normalization, not a production text workaround.

## Preview and remaining QA

`GroupedRows.kt` contains LTR and RTL paired previews at injected
fontScale 2.0, with Manrope and Russian labels. It also includes top bar, fields,
search and checkmark rows for manual inspection; these additional families are
not all covered by new large-font geometry tests. Existing paired previews cover
ordinary scale. Fixtures do not depend on app DI/network.

These tests verify text layout, measured bounds and semantics, not screenshots,
pixel-perfect brand parity, glyph mirroring, real locale translation, TalkBack
traversal or OS nonlinear font scaling. Actual screen layout at system font and
display-size settings still needs integration QA, including focus/IME, cutouts,
scrolling, modal Back and large text in dialogs/sheets. IDE previews are compiled;
their visual rendering must be inspected in Android Studio.

Reference: [Compose minimum touch targets](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
