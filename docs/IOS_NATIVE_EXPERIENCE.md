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

- `NativeModalSheet`: UIKit page sheet with one content-sized custom detent and
  a system grabber on iOS 16+. Short bodies wrap; long forms stay bounded by the
  available height and the caller's height cap. Measurement is independent of
  the current detent, so replacing a short picker with a form (and back) updates
  the same controller without a resize loop. The host reserves the bottom safe
  area once and consumes that inset for descendants. Busy forms disable
  interactive dismissal. Presentation waits until the presenting screen is
  attached and its navigation transition is finished. iOS 15 uses the existing
  content-sized Compose sheet; date/time wheels use the same entry point.
- `PlatformAlert` / `NativeActionSheet`: system confirmation dialogs and action
  sheets for photo sources and external map applications; destructive actions
  use the system destructive style. Photo menus
  finish dismissing before invoking the next picker. iPad menus have a popover
  anchor. Rich report forms use sheets instead of a text-only alert.
- `PlatformMenuButton`: check-in commands and drink selection use a native
  `UIButton` / `UIMenu` anchored to the visible trigger. Android uses a bounded,
  scrollable Compose dropdown. Single-choice lists retain a selection checkmark.
- Authentication fields use the shared `AuthTextField`/`CompactOutlinedTextField`
  styling, including rounded borders and the password visibility button.
  `NativeAuthInput` returns false. iOS keyboard options preserve
  username/password/newPassword/name content types and Next/Done return keys.
  Password editing stays Compose-owned to preserve the visibility toggle;
  other fields can use Compose's native text input integration.
  Website-specific credential suggestions also depend
  on Apple associated-domain provisioning; this change does not configure the
  website's apple-app-site-association or signing entitlements.
- `NativeKeyboardDoneAccessory`: UIKit Done toolbar for numeric/phone Compose
  inputs, anchored to the keyboard layout guide, removed when focus/IME closes.
- `PlatformScrollableColumn`: shared Compose scrolling and IME padding for
  finite forms/settings and filters. `NativeScrollColumn` returns false, so
  these surfaces do not create a nested Compose controller inside UIScrollView.
  Do not use this eager form container for paginated lists or nest LazyColumn
  in its unbounded content.
- `NativeDatePicker`: system date/time wheels. Calendar dates are represented
  as UTC days to match the existing Material date picker contract; future
  check-in dates remain disallowed. Time selection retains minute precision.
- `PlatformSwitch`: Compose control using the shared switch palette. Whole-row
  switch semantics stay in Compose to avoid duplicate VoiceOver announcements.
  `NativeSegmentedControl` remains UIKit, with an explicit transparent overlay
  and colors updated from the application theme.
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
Forms use Compose IME padding and the existing pan-to-dismiss behavior; they do
not provide UIScrollView's interactive keyboard tracking.

The bottom navigation now uses UITabBarController. Automatic native
tab minimization and a full UIKit table/collection-view migration are outside
this implementation.

## Verification

See [the interop audit](IOS_COMPOSE_INTEROP_AUDIT.md) for current rendering rules,
the complete integration inventory, and verification of the visual fixes.

Earlier migration checks on Windows passed:

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
3. Sign-in/register fields: Next/Done, Passwords AutoFill, secure toggle
   while editing, selection retention and the first edit after toggling. Password
   recovery: email request, browser reset from the email and return to login.
4. Scroll forms with the keyboard open; test pan-to-dismiss, numeric fields'
   Done toolbar and large accessibility text sizes.
5. Photo paging/pinch versus vertical dismissal, VoiceOver controls, Reduce
   Motion toggled while running, and status-bar taps on the active list.
