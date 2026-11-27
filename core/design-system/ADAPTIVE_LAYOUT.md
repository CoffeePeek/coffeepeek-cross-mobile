# Larger text and RTL

Current source guidance, reviewed 2026-11-27. Layout belongs to the component and
its actual screen constraints; no generic adaptive base screen is needed.

- Buttons/fields/grouped rows use minimum heights, allowing wrapped content to
  grow. Stepper halves keep logical decrease/increase order and normal platform
  action targets; arithmetic symbols do not mirror.
- Directional chevrons and rating fill respect layout direction. Use logical
  start/end padding for content and physical horizontal safeDrawing protection
  where both cutout edges must be covered.
- Settings labels/descriptions may wrap. A compact search/title/segment can retain
  an intentional single-line policy, with the full localized semantic label.
- Segmented choices adapt to limited width/larger text through scrollable options
  rather than shrinking their action target. UIKit owns native segment geometry.
- Editable rating intentionally remains compact and scrolls at narrow widths;
  its glyph-sized targets are documented in [HIG_CONTROLS](HIG_CONTROLS.md).
- Excessive tab counts, large values and unusually wide trailing slots still need
  a screen-level decision. Do not promise arbitrary text always fits.

Paired component previews include larger text, narrow layouts and LTR/RTL cases
where relevant. Open them beside their source in Android Studio; static modal
previews may need Interactive/Run Preview. Common previews use multiplatform
annotations; Android-only tooling belongs in androidMain.

The old measured Compose UI fixtures were removed with the repository's automated
UI-test infrastructure. They are not a current executable acceptance suite.
Current unit contracts and preview compilation are separate from manual visual
review. Follow [INSETS](INSETS.md) for consumption and keyboard ownership.

Manual acceptance covers real system font/window settings, TalkBack/VoiceOver,
focus traversal, modal Back/cancel, cutouts, scrolling and keyboard placement in
both themes. Do not change a user's device settings for testing without approval,
or describe a compiled preview as rendered/accepted.
