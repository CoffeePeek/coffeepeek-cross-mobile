# Notification mascot

Simplified from the [bean-in-a-cup source](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=469-2).
Revised toward the supplied wide mascot reference: the bean is lower and wider,
with larger eyes and a seam that crosses the whole bean. A 1.5-unit transparent
gap separates it from the cup. Tiny hands are omitted so they cannot merge with
the eyes or rim when the icon is reduced. The cup handle uses a large open cutout.

- [Figma board: light/dark previews at 16, 20, 24 and 32 px](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=718-189).
- [Figma export master: 24 px, SVG/PNG at 1×, 2× and 3×](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=718-207).
- `coffeepeek-notification.svg`: editable white silhouette on a transparent 24×24 canvas.
- Android: `composeApp/src/androidMain/res/drawable/ic_stat_coffee.xml`, the same paths
  and even-odd cutouts as the SVG, rendered at 24 dp.

The cup remains available as a design alternative. Android applies its own tint
to a small icon's white alpha mask. No opaque background, full-color illustration,
gradients or shadows are included in either small icon.

The selected variant uses the [standing mascot holding a bean](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=446-7218).
It keeps the oval body, eyes, central seam, feet and the held coffee bean. Broad
palms cross the transparent outline around the held bean; fingers and texture are
omitted for small-size rendering.

- [Standing mascot Figma board: 16, 20, 24 and 32 px on light/dark backgrounds](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=724-189).
- [Standing mascot Figma export master](https://www.figma.com/design/9nyb8euonMZAwm4mCbPatB/CoffeePeek?node-id=724-210).
- `coffeepeek-notification-bean.svg`: the selected editable 24×24 silhouette.
- `preview-bean.png`: the checked Figma preview.
- `composeApp/src/androidMain/res/drawable/ic_stat_coffee_bean.xml`: matching
  Android vector used by both `CoffeePeekMessagingService.setSmallIcon` and the
  Firebase manifest default for foreground and background notifications.

On iOS, ordinary notification headers use the app icon from
`iosApp/iosApp/Assets.xcassets/AppIcon.appiconset`. The current light/dark icons show
the full-color cup mascot. `preview-ios.png` is a schematic notification preview
using those actual assets; it is not a screenshot from a device and does not
reproduce version-specific system materials. See [Apple notification appearance](https://developer.apple.com/documentation/usernotificationsui/customizing-the-appearance-of-notifications).
