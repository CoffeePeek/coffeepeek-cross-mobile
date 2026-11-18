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

`communityHost()` injects both native renderers. `PostSheets` retains its existing
design-system sheet around the injected body, so `IosNativeSheetPresenter` remains
the sole owner of the analytics overlay enter/leave and UIKit dismissal delegate.
Saving/sending disables controls, cancel and swipe dismissal. The Compose body is
still needed by Android and fake Compose previews; it has no production iOS caller
after this injection.

The feature resolves its own strings (current ru/default and be resources), saved
drink names and bundled rating-image URIs. Swift receives app-owned JSON transport,
not feature implementation/domain payload types. The three rating axes preserve
their meanings, the unchanged drink keeps its saved name even after catalog edits,
and catalog failures retain retry plus the current valid selection. The original
text limits, validation/server errors and shared submission actions are preserved.

## Lifetime and callbacks

Every mounted body has a distinct `IosCommunitySheet`. Composition closes it on
disposal; the UIKit hosting controller retains its Swift model and the root view
observes that model. Swift model deinitialization cancels only its observation.
Accepted dismissal disposes and revokes the transport, so an old body cannot
edit a later reopening. A requested dismissal never closes the source ahead of
the shared owner: a save may already have started before the next native render. Account identity/generation are checked synchronously for
every action and asynchronously for visible state; account replacement clears the
native draft. Feed sheet keys include its session generation and post identity.

The existing feature analytics callbacks still run once through their original
ViewModel actions. No second overlay or analytics service is installed.

## Modal audit

`feature/community/impl/ui` has two sheet types: editor and report, both dispatched
through the native body hooks above from feed and standalone post. Photo previews
remain delegated through `CommunityHost.photoViewer` (native viewer checkpoint
#407); transient messages remain delegated through `CommunityHost.snackbar` (#406).
Share uses the existing platform share port; author blocking uses its independently
injected feature API. This checkpoint adds no hidden Compose dialog/body on iOS.

## Verification — 2026-11-18

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
