# SwiftUI check-in creation

The center + button keeps the existing Kotlin auth gate. In an authenticated
session its entry sheet now renders the coffee shop, home and shelf bag choices
in SwiftUI. The existing GlobalCheckInFlow owns the active session, analytics,
cancellation and one-time outcome delivery. UIKit presents the sheet and
reports visibility and interactive dismissal to that flow.

The destination editors, the shop search with MapLibre, and the full history
are separate migration slices. The picker does not introduce another writer or
permission flow. The completed home editor continues to refresh the existing
feed through its current callback.

The iOS Kotlin tests cover all three options, single delivery, stale sheet
dismissal after selecting home, and session replacement. The local iOS tooling
and signing tests, launcher labels, Xcode targets and RU/BE keys were checked.
On an iPhone, verify the guest auth gate, each choice, VoiceOver, extra-large
text and swipe dismissal in both themes with test data.
