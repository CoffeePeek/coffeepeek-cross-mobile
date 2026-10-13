# Native iOS interaction layer

## Ownership and migration

The current application remains `composeApp`; no modules or domain contracts
were moved. Shared routes, feature UI, ViewModels, repositories and Android
navigation remain in their existing locations.

The app composition boundary hosts a `UITabBarController` with five retained
`UINavigationController` stacks (`PlatformRootNavigation`). Each screen uses a
`NativeScreenController` hosting Compose content and the `AppContent` environment.
The discovery controller retains the map. UIKit owns tabs, push/pop animations,
and cancellable edge back gestures; iOS no longer runs the Compose tab graph.
Navigation events are serialized across transitions. Session changes recreate
the navigator through the existing login-state key.

Shared UI components expose small platform presentation contracts. Their
UIKit implementations live in `iosMain`; Android keeps Compose implementations.
No platform objects enter feature state, data, or domain contracts.

## Components

- `NativeModalSheet`: UIKit page sheet with medium/large detents and a system
  grabber. Busy forms disable interactive dismissal. Presentation waits until
  the presenting screen is attached and its navigation transition is finished.
- `PlatformAlert` / `NativeActionSheet`: system confirmation dialogs and action
  menus; destructive actions use the system destructive style. Photo menus
  finish dismissing before invoking the next picker. iPad menus have a popover
  anchor. Rich report forms use sheets instead of a text-only alert.
- `NativeAuthInput`: UITextField, secure-entry toggle preserving text/selection,
  username/password/newPassword content types, Next/Done return keys and an
  accessory Done button. Native fields expose accessibility labels and follow
  the system text size. Website-specific credential suggestions also depend
  on Apple associated-domain provisioning; this change does not configure the
  website's apple-app-site-association or signing entitlements.
- `NativeKeyboardDoneAccessory`: UIKit Done toolbar for numeric/phone Compose
  inputs, anchored to the keyboard layout guide, removed when focus/IME closes.
- `PlatformScrollableColumn`: actual UIScrollView for finite forms/settings
  and filter contents; content stays Compose. UIKit owns bounce, status-bar
  scroll-to-top and interactive keyboard dismissal. Keyboard avoidance and
  active native-field visibility are coordinated with keyboard frame updates.
  Do not use this eager form container for paginated lists or nest LazyColumn
  in its unbounded content.
- `NativeDatePicker`: system date/time wheels. Calendar dates are represented
  as UTC days to match the existing Material date picker contract; future
  check-in dates remain disallowed. Time selection retains minute precision.
- `PlatformSwitch` / `NativeSegmentedControl`: UIKit controls; whole-row switch
  semantics stay in Compose to avoid duplicate VoiceOver announcements.
- Photo viewer: vertical drag dismissal/cancellation, horizontal paging,
  pinch/double-tap zoom and tap-to-toggle controls. This viewer remains Compose.
- Selection/success haptics: favorite actions, native controls, refresh
  threshold and successful check-in. Navigation, tab transitions, refresh
  settling and photo dismissal respect Reduce Motion.

## Lists and remaining platform limits

Paginated feeds remain virtualized Compose LazyColumns. Their pull-to-refresh
keeps the shared implementation and native loader, with threshold feedback.
`PlatformScrollToTop` is a narrow UIKit status-bar-event bridge to LazyListState;
it does not take over list gestures or emulate interactive keyboard movement.
Interactive keyboard tracking is provided by the native form scroll containers;
other Compose surfaces retain pan-to-dismiss behavior.

The bottom navigation now uses UITabBarController. Automatic native
tab minimization and a full UIKit table/collection-view migration are outside
this implementation.

## Verification

Windows checks passed:

```text
:composeApp:compileKotlinIosArm64
:composeApp:assemblePlayDebug
:composeApp:testPlayDebugUnitTest          111 passed
:modules:network:testDebugUnitTest         47 passed
```

An Android emulator smoke check covered the coffee catalog, filter sheet and
scrolling, password recovery inputs, visibility toggle, Done dismissal and
local mismatch validation. No AndroidRuntime errors were reported. Test input
was synthetic and no password-reset submission reached the server.

Windows cannot run UIKit or link an Xcode application. Historical simulator
checks in IOS_NATIVE_NAVIGATION.md do not validate this migration.

Device acceptance checks:

1. Push/pop coffee, shop, roaster and profile routes; cancel and finish edge
   gestures, rapidly navigate, then verify retained map/tab state.
2. Open filters, check-in and nested date/photo sheets. Check medium/large
   heights, keyboard interaction, busy-state dismissal and iPad popovers.
3. Sign-in/register/reset fields: Next/Done, Passwords AutoFill, secure toggle
   while editing, selection retention and the first edit after toggling.
4. Scroll forms with the keyboard open; reverse an interactive dismissal;
   test numeric fields' Done toolbar and large accessibility text sizes.
5. Photo paging/pinch versus vertical dismissal, VoiceOver controls, Reduce
   Motion toggled while running, and status-bar taps on the active list.
