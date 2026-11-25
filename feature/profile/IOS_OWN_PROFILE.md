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

## Action icons — 2026-11-25

At `dce8989e5` plus `codex/migration-ios-shop-filter-parity` working changes,
Edit profile uses `person.crop.circle`; Shop changes retains `square.and.pencil`.
The menu actions, localized labels, session guards and analytics are unchanged.
Colocated fake-state light/dark previews show the distinct icons without DI or network.
Swift 6.4.2 syntax parsing, Android directDebug assembly/DEX and function limits passed
(130 recorded, zero new violations). No new tests were needed for the icon-only change.
Full native compilation, preview rendering and manual iPhone menu/VoiceOver checks
remain open; no device was selected, app installed or account data changed.


## Shared profile header and connections — 2026-11-25, Debug 3227

Owner/public header rendering is shared within each platform. The identity row
contains avatar, username and exactly three business statistics: check-ins, shops,
roasters (including zero). Followers and following move to two separate full-width
connection buttons below it; Follow is shown only for another user. Android uses
PublicProfileHeader/PublicProfileConnectionsRow; SwiftUI uses OwnProfileHeader/
CoffeePeekProfileConnectionsRow. Existing avatar, contribution and follow guards
are preserved. Count labels have aligned RU/BE one/few/many forms.

The existing PublicProfileData connections endpoint returns totalCount. Shared
presentation loads Following page1/pageSize1 after a visible profile loads; it keeps
an unknown nullable count on failure rather than manufacturing zero, and never opens
a sheet for this request. Refresh/account/cache-scope/visibility guards and explicit
cancellation protect responses. A normal Following list page supersedes the header
request. Owner exposure/resume refreshes only this counter (Android lifecycle and
the native owner session), so a changed relationship appears after returning from
another user's profile; the older pending request is cancelled.

The native owner bridge now exposes both connection kinds through its existing
PublicProfileViewModel. Membership-checked navigation resolves the current list;
owner/public render one native connections sheet, header and person row. The old
followers-only native snapshot/type/actions and their last consumers were removed.
No repository implementation, backend API, DI boundary, Gradle module or dependency
was added. Existing business data and analytics ownership remain unchanged.

Verification plan: existing profile domain/data/presentation contracts, seven new
count/stale/cancellation/return contracts, Android directDebug/DEX, function limits,
iOS full application build/sign and read-only manual layout/navigation review.
Colocated fake previews cover both themes, zero roasters, large text, known/unknown
counts and both connection lists. No automated UI/screenshot tests or live follow/
unfollow/account writes are used for testing. Android device rendering and complete
iPhone visual/network/permission gates remain separate from compilation.


Shared validation passed on 2026-11-25: profile impl 88 tests/9 suites executed,
including seven new count/unknown/stale/page/blocked/return regressions, all with
zero failures/errors/skips. Android directDebug assembly and packaged DEX runtime
class checks passed. No Android app was installed/launched. The function limit
check reported 129 recorded, zero new; regenerated baseline only removes/reduces
old entries after the profile action/connection-handler refactor. RU/BE count keys
are aligned and native localization tables have no duplicate keys.

The native username display uses the same single @ prefix as Android. This
Swift-only presentation adjustment and alignment of the Follow button appearance
were made while native build was still compiling Kotlin, before Runner could read
its Swift sources. The final source/resource hashes must be verified after linking.
