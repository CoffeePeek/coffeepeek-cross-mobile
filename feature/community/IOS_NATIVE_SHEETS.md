# Community editor and report — native iOS checkpoint

Scope: the sheet bodies opened from the native timeline and standalone post. Shared
`CommunityViewModel` and `PostViewModel` still own drafts, validation, catalog reads,
mutations, account guards, queues and analytics. No repository, writer or business
rule changes are part of this checkpoint.

## Actual entries

| Entry | Shared owner | Native body |
| --- | --- | --- |
| Feed card edit / rejected-post edit | CommunityViewModel | CommunityEditorContent |
| Standalone post edit | PostViewModel | CommunityEditorContent |
| Feed card report | CommunityViewModel | CommunityReportContent |
| Standalone post report | PostViewModel | CommunityReportContent |

`NativeCommunityFeed` and `NativeCommunityPost` now own the existing ViewModels
through explicit stores. Their ordinary presentation flows supply the native
controllers directly. `IosCommunityOverlays` keeps a sheet transport for the same
draft key; SwiftUI owns sheet/photo-cover presentation. The old feed/detail and
editor/report Compose renderer hooks were removed with their last iOS consumers.
Android retains its feature screens, sheets and colocated previews.

The feature resolves its own strings (current ru/default and be resources), saved
drink names and bundled rating-image URIs. Swift receives app-owned JSON transport,
not feature implementation/domain payload types. The three rating axes preserve
their meanings, the unchanged drink keeps its saved name even after catalog edits,
and catalog failures retain retry plus the current valid selection. The original
text limits, validation/server errors and shared submission actions are preserved.

## Lifetime and callbacks

Every mounted body has a distinct `IosCommunitySheet`. The native feature owner
closes it on accepted dismissal, key replacement or account revocation. The Swift
model cancels only its observation on deinitialization. Controller destruction
closes its feature store and collectors; temporary modal coverage keeps the draft.
Accepted dismissal disposes and revokes the transport, so an old body cannot
edit a later reopening. A requested dismissal never closes the source ahead of
the shared owner: a save may already have started before the next native render. Account identity/generation are checked synchronously for
every action and asynchronously for visible state; account replacement clears the
native draft. Feed sheet keys include its session generation and post identity.

SwiftUI owns container and keyboard safe areas directly. The old Compose
Surface, content measurement and height-reporting transport have been removed
from this route. Native medium/large detents and scrolling accommodate the form;
manual keyboard/rotation acceptance is still required.

The existing feature analytics callbacks still run once through their original
ViewModel actions. No second overlay or analytics service is installed.

## Container exit — 2026-11-25

Swift lays out the feed header and notification control. Transient messages use
the shared native message view, a cancellable serialized request and identity-bound
dismissal. Their ordinary timeout is four seconds; VoiceOver keeps the message
available for explicit dismissal. Shared SnackbarHostState remains only on the
Android path. No Compose host renders or positions the native message.

One lifecycle-owned date ticker runs while started. Feed return refresh happens
once after its external Home editor; post resume uses its existing Reload guard.
Initial resource preparation is distinct from closed state: a null initial
snapshot does not close the destination, and an unavailable post retains back and
retry presentation. App account-generation/user/sign-in guards close the entire
owner even while the controller is hidden.

Resolved feature text feeds one pure card mapper. Thumbnail fallback, inline
detail upgrades, moderation and offered actions have one implementation. Native
prefetch shares the global two-request limit and six-image budget, waits 150 ms,
stops under a photo cover and never renews a display grant or fetches a protected
original. The existing analytics service retains feed-view and exposure ownership.

The platform-independent sheet/message transports, JSON mapping and ten existing
guard contracts were moved from iosMain/iosTest to commonMain/commonTest. These
contracts now execute on the host JVM as well as remaining available to native
test targets. The application modal/global-flow selection executed 31 contracts
in four suites without failures. Message constraints update with navigation
clearance and native fade transitions respect Reduce Motion.

## Modal audit

The native owners offer two sheet types: editor and report. Photo previews use the
existing protected native viewer through an ordinary source; transient messages
use the same native message component. `CommunityHost` keeps Android rendering
services and the independent card hook still needed by its remaining consumers.
Share uses the existing platform share port; author blocking uses its independently
injected feature API. This checkpoint adds no hidden Compose dialog/body on iOS.

## Historical verification — 2026-11-18

- Windows common feature metadata compilation passed. The feature's iOS metadata
  task is `NO-SOURCE`: its presentation is shared commonMain code.
- Body gate found no new community/app violations; the pre-existing ownership
  factory violation on the parent checkpoint is tracked separately.
- 29 iOS build/configuration Python tests passed; PBX Swift-file chains and IDs
  validated; `git diff --check` passed.
- New non-UI tests cover rating-axis mapping, saved/custom/catalog drinks,
  independent feed/post actions, report limits, account replacement, busy-state
  protection, accepted-dismissal revocation, reopening and JSON escaping.
- iOS CI now runs the complete community Kotlin/Native suite, the app sheet tests,
  Xcode compilation, and retains their logs/XML. Results remain pending publication.
- Colocated light/dark SwiftUI previews are supplied for editor and report, plus
  a sending-state preview. They have no DI/network dependency.

Manual iPhone gates remain: keyboard and growing text in the existing content-sized
sheet, light/dark and accessibility font sizes, VoiceOver rating labels, bundled
images, catalog failure/retry, back/cancel/swipe while sending, and safe-account
server acceptance. No automated UI, instrumented or screenshot tests, real-account
writes, Android builds or device runs were performed.

## Native owner verification — 2026-11-25

Kotlin/Native application compilation and the body gate passed after native route
selection and old-renderer removal. The repeated normal Android directDebug
assembly/packaged DEX checks passed, with community 87 tests / 10 suites, media
73 / 10 and selected refresh/analytics application contracts 6 / 2, no failures.
Nine tooling checks passed, including compiled Swift lifecycle/photo model
contracts and native-controller registration. Controller visibility combines its
actual showing state with scene foreground state, preserving the same owner under
a cover and preventing a hidden entry from resuming when its scene becomes active.
Updated framework/Swift 6 executable
verification and manual modal/keyboard/theme/
accessibility acceptance remain pending. No device or live-account writes ran,
and this checkpoint has not been uploaded to TestFlight.
