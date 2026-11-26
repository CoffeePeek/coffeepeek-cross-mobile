# iOS notification inbox

The community tab mounts the SwiftUI bell in its current header action slot.
The hosting controller presents a native sheet. The bell and inbox share one
Kotlin presentation session connected to the same NotificationFeature that
supplies badge state and settings. Navigation reuses the application-owned
NotificationNavigation instance used by push opens. The iOS session isolates
each sheet and stops destination work when it closes or the account changes.

The Kotlin inbox verifies the active account generation before acting on a row,
defers the permission opportunity until the visible sheet has settings, groups
dates using the existing Kotlin formatter, and forwards refresh, paging and
read actions to the feature. SwiftUI reports a row seen only after half of it
remains visible for two seconds while the app is active.

Contract checks cover permission timing, cancellation on account replacement,
stale read actions, failed destination navigation and guest sign-in. The
ios-verify workflow builds the simulator app and executes notification tests.
Manual checks still needed on device cover light and dark appearance, Russian and
Belarusian, Dynamic Type and VoiceOver, swipe dismissal, paging, an actual push
open, enabling permission, and leaving or replacing the active account.

The community tab, post cards, check-in detail, leaderboard and inline
edit/delete/report sheets remain Compose. They form the next community UI slice.

## Native sheet chrome — 2026-11-25

Source: `dce8989e5` plus `codex/migration-ios-shop-filter-parity` working changes.
The inbox opens at the large native sheet detent. A modal-owned NavigationStack
provides the inline title and system close toolbar button instead of a drawn header.
Date groups use compact rows and inset separators; Read all includes the Android
checkmark and existing gold action color. Existing localized strings are reused.

The Kotlin sheet version, account guards, permission opportunity, pagination and
destination actions are unchanged. The existing SwiftUI lifecycle-owned task still
requires half the row to be visible continuously for two seconds in the foreground;
the viewport measurement now naturally excludes the native navigation bar. No new
task, isolation escape, permission request or analytics emission was added. Fake-state
previews cover populated and guest sheets in both themes, long body text, read/unread
rows, both date groups, Belarusian guest copy and large text.

All 35 notification unit tests in 4 suites executed successfully, with zero failures,
errors, skips or retries. Swift 6 syntax parsing, native launcher registration checks,
Android assembly/DEX and function limits passed. The failed Room metadata attempt and
subsequent successful Android/unit run are recorded in
[the report verification](../shop-report/README.md#native-form-visual-parity--2026-11-25).
iOS-only inbox session contracts, full SwiftUI compilation, IDE preview rendering and
manual iPhone sheet, VoiceOver, scroll/visible-read, background cancellation and swipe
dismissal checks remain open. No device was selected, app installed or live data changed.
