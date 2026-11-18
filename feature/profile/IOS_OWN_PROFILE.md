# Signed-in iOS Profile tab

The authenticated Profile tab now hosts SwiftUI. Its Kotlin session owns the
same account and public-profile ViewModels used by Compose. The existing
history ViewModel supplies the two calendar modes and decides where selected
rows navigate. The root navigation still owns the screen visit and all
destination pushes. The tab identity, active-account checks and profile
snapshot cache are retained.

The signed-in check-in list loads more shared history rows when the user
reaches the end of the loaded rows. The iOS image observer uses the same
protected photo access as the viewer for check-in photos and coffee bags.

The selected profile section and calendar mode belong to the current native
controller. A new login lifetime revokes the previous profile and history
sessions and recreates native navigation. Token renewal within the
same login keeps that stack. The guest and public profiles keep their existing
entry points.

The shelf and collections feature views remain embedded until their own
migration. Their list behavior, mutation and preferences stay with those
features. The photo viewer, edit profile, full history and other detail
routes keep their existing owners.

Verification uses the existing profile and activity Kotlin/Native suites and
new session replacement and cancellation checks. Manual iPhone acceptance
still needs both themes and languages, VoiceOver, large text, scrolling the
embedded shelf, navigating back from edit/history/coffee and account switching.
For the check-in list, verify that scrolling appends a second page, authorized
photos load and open, and unavailable photos show the CFPK placeholder in both
themes. Reopen Profile after signing in again to verify the new session.
