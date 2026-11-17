# SwiftUI check-in creation

The center + button keeps the existing Kotlin auth gate. In an authenticated
session its entry sheet now renders the coffee shop, home and shelf bag choices
in SwiftUI. The existing GlobalCheckInFlow owns the active session, analytics,
cancellation and one-time outcome delivery. UIKit presents the sheet and
reports visibility and interactive dismissal to that flow.

The Home editor now has a native presentation entry behind the same
`HomeCheckInEditorEntry`. Android keeps its existing Compose screen. On iOS the
application composition creates `NativeHomeCheckInSession`, whose existing
`HomeCheckInEditorViewModel` remains the sole owner of load, validation, search,
editing, save, analytics and confirmation. The Swift surface receives copied
presentation values through `IosHomeCheckInEditor`; its subscription does not
own or destroy the form when the camera covers it. Composition disposes the
session only when the entry leaves composition.

Entry coverage is the global create flow (Home or shelf bag), the shelf's brew
action, and existing Home edit consumers. `preparationId == null` is create;
any non-null ID is edit. Shop creation remains its own session and feature; a
Shop editor route does not imply an API for editing an existing Shop record.
The shop search with MapLibre and full history are separate migration slices.
No new writer, network client, auth flow or image pipeline is introduced.

The native Home API preserves the owner lease, server timestamps when selecting
the same civil date, nullable publication state and explicit saved confirmation.
Photo delivery is request-scoped, bounded, copied, and discarded after closing
or replacing the owner. The existing platform picker handles camera/gallery and
compression; edit still forbids changing server photos. Private shelf images
use the existing authorized fetcher under a cancellable account lease. The
completed Home editor refreshes the existing feed through its existing callback.

The iOS Kotlin tests cover all three options, single delivery, stale sheet
dismissal after selecting home, and session replacement. The local iOS tooling
and signing tests, launcher labels, Xcode targets and RU/BE keys were checked.
On an iPhone, verify the guest auth gate, each choice, VoiceOver, extra-large
text and swipe dismissal in both themes with test data.

## Native editor verification (2026-11-17, working tree after 11535611b)

Added non-UI Home session contracts for local-day projection and field rules,
unchanged edit timestamp and unknown visibility, owned/bounded/single-use photo
delivery, picker cancellation and stale callbacks, submit blocking while photos
are pending, owner revocation, one saved outcome after confirmation, and detail
request cancellation on owner disposal. Tests are authored, not claimed executed
here; the coordinating task owns serial Gradle checks and the iOS build.

Manual gates remain: create/edit and shelf entry placement in both themes,
native date and bean selection, keyboard/large text/VoiceOver, cancelled photo
picker and camera round trip, back/cancel and account replacement. Opening a
real-account form is read-only acceptance only; persistence and analytics writes
require an explicitly authorized safe account and are not certified by previews.
