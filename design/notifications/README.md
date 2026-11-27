# Notification artwork and provenance

Reviewed against tracked assets and notification wiring on 2026-11-27. These are
editable source assets/previews, not device acceptance screenshots.

The selected Android small icon is the standing bean mascot holding a bean:

- [Original standing mascot](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=446-7218)
- [Design preview board](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=724-189)
- [Export master](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=724-210)
- `coffeepeek-notification-bean.svg` — editable transparent 24×24 silhouette;
  `preview-bean.png` — design preview.
- `composeApp/src/androidMain/res/drawable/ic_stat_coffee_bean.xml` — runtime vector,
  used by both `CoffeePeekMessagingService` and the Firebase manifest default.

The alpha silhouette preserves broad body/eye/seam/foot/held-bean shapes; fine
texture/fingers are omitted for small-size legibility. Android tints the small
icon; opaque backgrounds, gradients and full-color illustrations do not belong
in this mask.

The cup alternative is retained as design provenance:
[bean-in-cup source](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=469-2),
[preview board](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=718-189),
[export master](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=718-207).
`coffeepeek-notification.svg` and `preview.png` retain that alternative;
`ic_stat_coffee.xml` is not the current Firebase default.

Ordinary iOS notification headers use the actual application icon catalog at
`iosApp/iosApp/Assets.xcassets/AppIcon.appiconset`. Inspect its current PNGs rather
than treating an older mascot description as the current restored app icon.
`preview-ios.png` is a historical schematic design preview, not evidence of the
current catalog, delivered push or version-specific system material rendering.
