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
