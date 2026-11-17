# Signed-in iOS Profile tab

The authenticated Profile tab now hosts SwiftUI. Its Kotlin session owns the
same account and public-profile ViewModels used by Compose. The existing
history ViewModel supplies the two calendar modes and decides where selected
rows navigate. The root navigation still owns the screen visit and all
destination pushes. The tab identity, active-account checks and profile
snapshot cache are retained.

The selected profile section and calendar mode belong to the current native
controller. Replacing the account revokes its profile and history sessions,
clears visible data and closes its sheets and photo viewer. The guest and
public profiles keep their existing entry points.

The shelf and collections feature views remain embedded until their own
migration. Their list behavior, mutation and preferences stay with those
features. The photo viewer, edit profile, full history and other detail
routes keep their existing owners.

Verification uses the existing profile and activity Kotlin/Native suites and
new session replacement and cancellation checks. Manual iPhone acceptance
still needs both themes and languages, VoiceOver, large text, scrolling the
embedded shelf, navigating back from edit/history/coffee and account switching.
