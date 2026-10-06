# iOS navigation bar

## Scope and ownership

The application composition module owns the root navigation. Android uses the
existing Compose `FloatingBottomNavBar`. iOS hosts a native `UITabBar` through
`PlatformFloatingBottomNavBar`; on iOS 26 the system renders its Liquid Glass
appearance. iOS glass icon buttons use `UIButtonConfiguration.glass()`.

Detail transitions and sheets remain Compose UI. Business contracts,
repositories, and navigation routes are shared.

## Integration

- `Navigator` continues to own detail/authentication routes. Its Main route
  uses the same Compose implementation on both platforms.
- Each tab remains inside the Compose navigation graph, including the map.
- `Navigator.pendingTabSelection` selects tabs for actions such as
  showing a shop on the map; pending map focus remains owned by `Navigator`.
- `AppContent` supplies each Compose controller with the shared theme and
  image configuration. It does not create another root navigator or DI container.
- Tab selection is saved in the Compose navigation entry.
- Tab content receives the existing Compose bottom clearance.

## Appearance and limits

On Android, the bar keeps its Compose appearance from `LiquidGlass.kt`. On
iOS, `UITabBar` and UIKit glass buttons use system materials. The iOS 26
appearance therefore follows the OS, while older iOS versions use standard
UIKit gray button configurations. UIKit views are placed above the Compose
surface so their transparent glass backgrounds show the screen beneath them.
The tab bar extends through the bottom safe area at its natural height. The
native tab bar is embedded in Compose; it does not replace the shared
navigation graph.

Automatic scroll-driven tab minimization is not implemented: Compose scroll
containers do not expose a native UIScrollView to UIKit. This change does not
claim to provide every scroll-edge effect or native detail navigation gesture.

## Verification and deployment

Local checks passed:

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
