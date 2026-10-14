# iOS navigation bar

## Scope and ownership

The application composition module owns the root navigation. Android uses the
existing Compose `FloatingBottomNavBar`. iOS hosts a native `UITabBarController`
with five UINavigationController stacks; the system owns tab selection and bar
appearance. iOS glass icon buttons use `UIButtonConfiguration.glass()`.

Detail transitions now use a native UINavigationController and sheets use
UISheetPresentationController. Business contracts, repositories, and navigation
routes are shared. See [IOS_NATIVE_EXPERIENCE.md](IOS_NATIVE_EXPERIENCE.md) for
the migration, ownership, and device verification requirements.

## Integration

- `Navigator` publishes shared detail/authentication routes. UIKit dispatches
  them into the active tab's stack; Android retains its Compose graph.
- Each native tab retains its own root content, including the discovery map.
- `Navigator.pendingTabSelection` selects tabs for actions such as
  showing a shop on the map; pending map focus remains owned by `Navigator`.
- `AppContent` supplies each Compose controller with the shared theme and
  image configuration. It does not create another root navigator or DI container.
- Direct tab-bar selection preserves the destination stack. Programmatic tab
  entry (including "show on map") returns to that tab's root.
- UIKit reserves bar/safe-area space; shared floating clearance is zero. The
  Compose host consumes the insets already reserved by UIKit.
- CpTopBar forwards detail headers/actions to the native navigation item.
  Feed, profile and settings use shared left-aligned root headings. Custom hero
  headers keep their existing layout. CpSearchField uses the shared Compose field.

## Appearance and limits

On Android, the bar keeps its Compose appearance from `LiquidGlass.kt`. On
iOS, `UITabBar` and UIKit glass buttons use system materials. The iOS 26
appearance therefore follows the OS, while older iOS versions use standard
UIKit gray button configurations. Transparent UIKit controls are placed above
the Compose surface so their backgrounds show the screen beneath them. Opaque
map and root controller hosts remain below Compose overlays. Theme and content
color are propagated explicitly; see [the interop audit](IOS_COMPOSE_INTEROP_AUDIT.md).
The tab controller owns the bottom safe area at its natural height. Detail
pushes hide its bar. The discovery root keeps the bar while expanding the map.

Automatic scroll-driven tab minimization is not implemented: Compose scroll
containers do not expose a native UIScrollView to UIKit. This change does not
claim to provide every scroll-edge effect. Native detail navigation is now
handled by each tab's UIKit navigation controller. See
[the staged migration plan](ios-native-ui-migration.md) for remaining work.

## Verification and deployment

Historical checks before the native interaction migration passed:

```bash
./gradlew :composeApp:compileKotlinIosArm64 :composeApp:compileDebugKotlinAndroid
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,id=83E409DF-61B0-47D8-9D04-C9CAB89D35A1' \
  CODE_SIGNING_ALLOWED=NO -quiet build
```

The app was installed and launched on an iPhone 17 simulator running iOS 27.
All four tabs opened, the map rendered, and the shop header buttons appeared
without rectangular backgrounds. A physical iOS 26 device check is still
required.

Further device checks:

1. Confirm the appearance and touch response on iOS 26 hardware.
2. Return to feed/map and confirm their state survives tab switches.
3. Use "show on map" from a shop; confirm the selected tab and focus.
4. Open authentication, sign in/out, and verify the root navigation resets safely.
5. Change theme, rotate, and check content clearance, VoiceOver, and keyboard behavior.
