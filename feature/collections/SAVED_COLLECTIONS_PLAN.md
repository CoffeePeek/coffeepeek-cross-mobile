# Editable saved collections — 2026-11-03

## Scope and ownership

Add an owned library, private/public lists, metadata editing, a membership sheet
and an optional single featured public list. Private means owner-only. Existing
global server favorites and exported snapshot links remain separate. Cancelling
the membership sheet never removes a successfully saved global favorite.

Collections api owns narrow entry/target/local-picker contracts; domain owns
list metadata, validation and repository contracts; data owns HTTP, DTOs, guarded
cover uploads and authenticated private-cover reads; impl owns state and UI.
Application composition owns business favorite-success events, platform picking,
root navigation and profile/settings slots. No new Gradle modules are required.

The old Composer/Public ABI remains. Anonymous Public recognizes `mode=list` and
shows the new public list; private lists never fall back to anonymous media or
bearer tokens in URLs. Cover bytes and pending input are cleared on account change.

## Verification plan

- Domain: empty lists, title/description rules, stable shop/roaster item ordering.
- Data: owner/auth pinning; public calls remain anonymous; private covers only use
  authenticated feature-owned paths, bounded bytes/time; revisions/409; atomic
  membership; cover claims/cleanup; cancellation and replaced-account completion.
- Presentation: library/metadata/picker/account transitions, failed saves retaining
  edits, duplicate writes, one featured public list or none, public view no owner
  reads, export and current snapshot/reorder regressions.
- Parent runs sequential Gradle with max-workers=6, Android assembly/runtime checks
  and relevant unit/contract tests. No UI/instrumentation/screenshot tests.
- Manual gates: explicitly chosen app/device; light/dark, RU/BE, font/window sizes,
  IME/sheet placement, Back/Cancel and real platform gallery. Picker → quick create
  replaces the current modal and Back returns to the same membership draft; verify
  only one modal is present, including the native iOS presentation. Production writes and
  public/private backend acceptance require the agreed safe test setup. Native
  iOS compilation/device checks remain separate from Android/preview compilation.

## Evidence

Implementation is on `codex/migration-saved-collections`, parent poster checkpoint
`246edb8c` (retained in develop by PR #301). Colocated previews use fake state
only; they do not certify device behavior. Android Studio MCP was unavailable
in the active tool inventory, so source review and shell Gradle were used.

The integrating parent executed the first combined
`:composeApp:compileDirectDebugKotlinAndroid` successfully (1m36s, 1456 tasks).
That is compilation evidence, not execution of the newly added tests or manual UI.
Domain/transport/ViewModel cases cover empty/private defaults, revision conflicts,
create-ID retention after upload failure, cancellation/account changes, bounded
authenticated covers, anonymous public reads, atomic membership, featured refresh
and omission of private URLs from exports. Executed results are recorded below.

The first parent unit/assembly attempt stopped at data tests: four of ten new
transport cases hit virtual 8s/20s deadlines because MockEngine used its own
dispatcher while runTest advanced another scheduler. Those original failures
are retained in the parent's run log; reruns replaced XML results. No production
timeout was removed.
Fixtures now share one StandardTestDispatcher with the engine. A single
feature-data Semaphore(4) also bounds visible/prefetched cover loads; the 8s
deadline includes waiting for the permit, and owner identity is checked again
before sending. Two transport regressions verify the queue deadline and owner
replacement while queued. The subsequent parent rerun passed those regressions.

Upload flow creates and retains the new list ID before uploading. Media reservations
are private owner-bound pending objects with a 24-hour TTL; transactional server
attachment claims a successful cover. DTO URL fields are ignored for delivery:
data builds the owner/public cover endpoint from validated list/photo UUIDs.
Signed object-storage PUTs use the anonymous client and the required temporary tag.

Library, preference and featured entries refresh on lifecycle resume. Featured
clears its old card before refreshing, including when a list becomes private or is
deleted. Gallery composition is keyed to the account generation; local image input
and temporary downloaded bytes are cleared on cancellation/account replacement.

Cross-contract review after the parent's successful 262-case run and Android
assemblies found a masked upload schema error: test reservations used `id/url`,
while the existing Media endpoint returns `photoId/uploadUrl/storageKey`. The
reservation DTO now requires the actual Media names; attached cover DTOs remain
`id/url`. Updated fixtures exercise the real upload response and account-change
guard, and a regression rejects obsolete or mixed aliases without any storage PUT.
No live upload was attempted to establish the original failure; the mismatch was
found from the server contract. The previous green tests did not certify this
wire shape. The corrected contract passed the parent's subsequent data/app rerun.

## Executed verification and remaining gates

The combined local verification on 2026-11-03 passed 263 unit/non-UI contract
cases, with zero failures, errors or skips: collections domain 7, data 22,
impl 43; favorites impl 18; profile impl 62; settings impl 38; selected application
contracts 73. The application cases cover favorite-success prompts, session and
analytics guards, repository/DI adapters, and fake HTTP contracts. These are not
device UI or authenticated production-acceptance tests.

Sequential Direct Debug and Direct Sandbox assemblies passed packaged DEX
runtime-class checks. No Sandbox installation or run was performed for this
checkpoint: the user explicitly retained ordinary Debug. The earlier Debug
artifact (SHA-256 `9DB2B5B75E3C7447E7558E22D2ACF36AAB0C64B7531E8237E68B979D09C34CC6`)
was installed in place and MainActivity foreground was confirmed on Pixel_8 AVD,
`emulator-5580`, Android 17/API 37, x86_64 with 16 KB pages. Version 1.1.5.2681,
package `com.coffeepeek`; data and first-install time were preserved. This proves
startup of that artifact, not end-to-end collection editing or the later compact
picker layout.

Original failed verification attempts remain in ignored local logs:
`build/saved-collections-first-tests-failed.log` (MockEngine scheduler deadlines),
`build/saved-collections-second-tests-failed.log` (test fixture Flow compilation),
and `build/saved-collections-third-tests-failed.log` (enriched fixture mismatch,
expected 404 exception policy, and real expectSuccess=true/409 mapping bug).
The fixtures and 409 mapping were corrected; cancellation is still rethrown.
The later Media reservation wire mismatch was discovered independently of these
green tests and is described above; no live upload was used to establish it.

On 2026-11-04 the picker was changed to hug content for error/loading/empty/short
states, with a 75% window budget for long lists. Quick-create uses the same
bounded scrolling treatment. No global design-system behavior was changed.
Independent review found that quick-create after a failed list load could clear
the transient error and commit incomplete memberships. A durable successful-load
guard now blocks all membership writes/creation until lists load successfully;
three unit regressions cover failure, retry preserving prior memberships, and
creation after a successful empty result. The final impl rerun passed 46 cases,
zero failures/errors/skips, bringing the verified affected-suite total to 266.
Direct Debug assembly and packaged DEX checks passed in the same sequential
Gradle invocation (1m55s; log `build/saved-collections-picker-final.log`).

The final ordinary Debug APK SHA-256 is
`B21187B65C6B4C347923F9D31C99F3E17904C98444AA2F25D09D5C4D6BF89F4D`.
Package/launcher/version matched the prior install and both signing certificates
matched (`48ad0071309c72a29cab9ab9747519c06754837d7a3066de2a7b5a8f72a08abe`).
It was updated in place on the same unlocked Pixel_8 AVD, `emulator-5580`, on
2026-11-04; `am start -W` reported a successful cold MainActivity launch and
foreground activity was confirmed. Version remains 1.1.5.2681; first-install time
2026-10-20 10:23:54 was preserved. No uninstall, downgrade, data clearing, live
favorite or collection writes occurred. The compact sheet has source/preview and
assembly evidence; full manual layout/keyboard review remains unexecuted because
the chosen live Debug account was not used to manufacture favorite changes.

RU/BE resource keys align: collections 84/84, favorites 23/23. Light/dark previews
are colocated for editor, library, picker (including error/empty/loading/long
states), quick-create, preference and featured/public content. IDE preview
rendering, manual light/dark/font/window/keyboard/accessibility checks and real
gallery acceptance are not claimed. No automated UI/instrumentation/screenshot
tests were created or run.

Android heart integration is the implemented platform checkpoint. The native iOS
favorites screen/binding/writer is unchanged, and its picker host is intentionally
inactive. Shared iOS compilation and native device integration remain separate
gates; the parent PR #301 iOS success does not certify this new patch.

Backend PR #377 is merged at `1a15a24e2f4b61dd385ee84b86b42af0043ae26b` and
main workflow 41692942924 deployed successfully on 2026-11-03. All three dedicated
real PostgreSQL migration cases ran without skips (Shops 2, Media 1). Web PR #36
is merged at `c48ec5c1dab3cbef2dc670c6ee06c276aba0ae38`; client/admin production
deployments succeeded. Subsequent backend hardening was reviewed against the
mobile wire contract. Anonymous production saved-list requests return 401 and
nonexistent public list requests return 404; these prove routing/protection only,
not successful owner reads/writes. Real-account data was not modified by agent
checks. Authenticated owner loading and public/private end-to-end acceptance
still require a safe, explicitly authorized test setup.

## Profile and collections correction — 2026-11-04

The owner profile now previews the owner's actual saved collections, including
private ones. Its "All" route opens the grouped library. The library already
provided one top-right create button, public/private sections that omit an empty
public group, and sharing on individual collection pages. The default cover
uses the first coffee shop's photo when there is no uploaded cover; the editor
description has explicit vertical padding. A successful owner-list read now
ensures a private `Избранное` exists. Concurrent reads are serialized within one
repository instance, and account changes/cancellation stop creation. Server-side
uniqueness is still needed to rule out cross-device duplicate creation.

Read-only Debug diagnostics from the selected emulator found `400` responses for
featured and membership PUTs, while collection POST and metadata/items PUTs
succeeded. Those two JSON bodies had compact 32-character collection IDs; the
server binds them as GUIDs in hyphenated form. The client now sends canonical
hyphenated IDs in those bodies and retains the existing URL path format. Mock
HTTP tests cover both request bodies. No live owner write was used to verify the
change; authenticated production persistence remains an acceptance gate.

Verification on 2026-11-04 from branch `codex/migration-profile-collections-polish`
based on `fd62d742`: collections data 29/29, impl 54/54, profile impl 62/62,
and shelf impl 78/78 Android unit tests passed with no skips. The first full
application unit run completed 985 cases with two failures in old Sandbox
fixtures that expected no default list; both fixtures were corrected and their
focused rerun passed 7/7, with no retries. Direct Debug and Direct Sandbox APK
assemblies passed their packaged runtime-class checks. Sandbox was not installed
for this correction, respecting the user's ordinary Debug selection.

The final committed-code Debug APK (`com.coffeepeek`, version 1.1.5.2688,
SHA-256 `86F4EABA0DFEE610E4F1C96B7DDBA6E39B375467BCC28C271823C88271A2AACF`)
was installed in place on `emulator-5554`, Android 17/API 37, x86_64, 16 KB
pages. Package, launcher and certificate matched the prior install; first
install time was preserved. MainActivity remained foreground with no fatal
startup log. The app displayed a signed-out Settings screen, so owner profile,
collection edits, shelf and preference layout were not inspected in the live
account. Light/dark, large font, narrow window, accessibility, modal and real
backend write acceptance remain unverified. No automated UI or screenshot
tests were added or run.

## Collection flow follow-up — 2026-11-04

The user's later instruction supersedes branch/PR defaults: this follow-up is
implemented directly on `develop`, with no new PR. Current rules and the full
favorites/collections audit are recorded in [FLOW_AUDIT.md](FLOW_AUDIT.md).
The default starts private but publishing/renaming preserves its server identity.
New collection editing no longer reads/imports global favorites. A cover failure
keeps the draft without creating an orphan row. Unedited memberships are not
replaced by metadata saves. Picker saves send only added/removed marks, rebased
against a fresh read for older servers and applied atomically by the new server.
Mutations invalidate the library/preference views and refresh heart projections;
notifications received during loading schedule a subsequent refresh.

Card captions now overlay the photo above a fading blur/gradient. The cover edit
control sits beside the name; description and visibility follow below. Profile
shelf section/photo loading uses the shared shimmer, with a neutral no-photo
fallback and no visible bag names. New/existing, loading/empty and light/dark
previews remain colocated.

Android verification: collections data 34/34, impl 59/59, shelf impl 78/78 and
selected application collection/DI contracts 24/24 passed, zero failures/skips.
No UI/instrumentation/screenshot tests were added or run. Live account writes,
native iOS compilation and manual theme/font/window/accessibility acceptance
remain separate gates. Backend develop changes require deployment before the
server uniqueness and atomic membership-delta guarantees are live.

Final Direct Debug and Direct Sandbox assemblies and their packaged DEX guards
passed; Sandbox isolation passed and it was not installed. Debug 1.1.5.2689
(`com.coffeepeek`, APK SHA-256
`CEB5EE3F52CFB88676030B9A6335CFEB590867618C8E67C0AF4613A0A69FA8B3`)
was updated in place on unlocked `emulator-5554`, API 37/x86_64 with 16 KB pages.
The certificate matched, the version increased, and first-install time remained
2026-11-04 10:32:16. The launch command failed twice with Activity-not-found;
package inspection and a Direct Boot-aware resolver confirmed MainActivity is
present, but Android reported user 0 as `BOOTING` even with boot-completed set.
Startup and authenticated manual UI review are therefore not claimed. No
uninstall, data clearing, device-security change or live account write occurred.

### User-selected physical phone follow-up

The user redirected the check to a connected Pixel 9, API 37.
Its installed `com.coffeepeek` 1.1.5.2682 has a different signing certificate, so
it was preserved. The existing `-PpushTestApp=true` Debug option built the same
code checkpoint `b21be5e9` as `com.coffeepeek.pushdebug` 1.1.5.2689, using the
normal backend, for installation alongside it. APK SHA-256:
`221238B5E54022ACEA5E35CCBFB5F51950C6FB0017189E0A576455A89B9C5212`.
USB disconnected during installation and the command failed; after reconnect,
package inspection confirmed the installed version. The subsequent cold launch
succeeded in 1013 ms and the process remained alive. A later hot launch also
succeeded. USB disconnected again during manual screenshot capture, so full
profile/shelf/editor visual checks are not claimed. The original phone app was
not replaced, uninstalled, cleared or signed out. No live account write occurred.

## Profile and collection re-entry follow-up — 2026-11-04

The user requested this continuation directly on `develop`, split among three
subagents, with ordinary Debug on the connected phone. Verification covers
first-frame cached content, background refresh and refresh failure, empty versus
unloaded snapshots, account replacement/logout, editor menu/visibility/save,
and the profile shelf. No live account mutations are part of verification.

The owner collection repository now keeps verified process-memory snapshots,
including an explicitly loaded empty library. Library and picker ViewModels
hydrate before their first coroutine and refresh without replacing existing
cards with a loading screen. Successful mutations update the cached summaries;
queued invalidation still runs after a pending refresh. The profile entry uses
the same collection snapshot.

Profile snapshots are shared by both Android profile entries and bounded to four
profiles. Check-ins/cursor and activity survive re-entry. Session generation and
author-visibility revision isolate the snapshots; credential refresh preserves
the account cache, while logout/re-login or account replacement invalidates it.
The legacy own-profile getter rechecks the active account synchronously before
exposing cached data. Own shelf metadata is reused during refresh; public visitor
shelves retain their existing fail-closed refresh policy. Photo bytes are not
retained by the new profile/shelf snapshot stores.

The editor uses the shared liquid glass Save button, the short edit title and
one top-right overflow menu for available link/photo sharing and deletion.
The public/private explanation is inside its switch control. The user's next
correction restores the rounded shelf edge and uses flat bolt heads with one
straight slot. Existing paired previews cover these states.

The first unit run exposed two fixture assumptions: the fake collection response
was captured before its gate, and the profile account-switch fixture returned
the old failure for the new account's background refresh. Both fixtures now model
separate responses while preserving their original assertions. The next run
passed 1,480 unit/contract tests, with zero failures/skips, and both Direct Debug
and Direct Sandbox assemblies including packaged DEX checks. A final targeted
run remains necessary for subsequent account-scope and activity-slot changes.

The week's counts/photo URLs also have one account-and-date-scoped presentation
snapshot, cleared on logout and Koin shutdown. The profile collection preference
retains both its library and the nullable featured-list selection, preventing a
checked switch from flashing off on re-entry. General Android Settings no longer
renders the profile-preferences section. The Save control now floats above the
scrolling editor, with safe bottom clearance and no separate bottom panel.

### Final regression gate

The subsequent full gate passed on 2026-11-04: 1,569 unit/contract tests,
zero failures and skips. Counts: session 23, legacy data 72, profile impl 66,
collections data 39/impl 70, shop impl 109, roaster impl 23, shelf data 49/impl 81,
settings impl 38 and application Direct Debug 999. Direct Debug and Direct
Sandbox APK assembly, packaged DEX runtime-class guards and Sandbox isolation
passed. Sandbox was built only, never installed or launched for this follow-up.
An initial final-gate attempt stopped on a new shop test's generic-list type
inference; an explicit event list type corrected that compile error before the
passing full run. No automated UI/instrumentation/screenshot tests were added.

Shop and roaster heart taps now open the same membership picker for either
heart state. This includes migrated detail/list surfaces and legacy feed/search,
favorites cards/previews and shop/roaster screens. No optimistic favorite write
runs before the picker. Android confirmation analytics use the existing service,
consent/account operation scope and verified before/after union; metadata edits,
cancel and moves that keep the place favorited do not create a favorite-change
event. The iOS writer remains unchanged.

Read-only physical startup is the remaining delivery check. Full authenticated
visual review, theme/font/window/accessibility, live account writes and native
iOS compilation remain unverified gates. Backend develop CI is green as recorded
in FLOW_AUDIT.md; production deployment was not performed.

After the full gate, a narrow shop accessibility correction keeps the membership
picker available while the separate favorite projection is loading/unavailable
and labels that action as choosing collections. Shop impl 109/109 and application
Direct Debug 999/999 passed again. Intermediate compile attempts caught a
stateless-screen VM reference and a missing resource import; both were corrected
before that passing run. The final phone APK is rebuilt from the committed fix.

### Final physical delivery

Source `8028eef5`, `directDebug -PpushTestApp=true`, package
`com.coffeepeek.pushdebug`, version 1.1.5.2691 was built with the packaged DEX
checks and installed in place on the user-selected Pixel 9, API 37. APK SHA-256:
`8808B559C804E178F11053714C558790DAD3D61757F308D395F9395D4D674FBB`.
The installed and built certificates matched; version increased from 2689 and
first-install time remained 2026-11-04 14:26:23. The existing normal application
was not replaced, uninstalled or cleared.

The unlocked phone reported a successful cold start in 917 ms. A later process
and foreground-activity check succeeded; a local screenshot confirms the normal
Discovery page rendered in dark mode. USB dropped again before the scoped crash
buffer read, so that read was interrupted rather than claimed as passed. This
proves startup/rendering, not authenticated collection persistence or a complete
profile/editor/theme/font/accessibility review. No live account mutation was
performed; screenshots and device diagnostics remain in ignored local build
outputs. Direct Sandbox assembly/isolation was verified at the preceding full
gate and was not installed or launched.


## Profile header and editor follow-up — 2026-11-04

Scope: match the supplied profile-header reference, use the editor's full bottom
viewport, provide the floating Save button with a real blur source, and preserve
the shelf-visibility control on re-entry. Existing profile navigation/actions,
zero-roaster hiding and immediate visibility saves remain intact. This work uses
develop separately from an unrelated in-progress migration merge.

The header places the name and one counter row beside the avatar, then the bio
below. Avatar space adapts to a 320 dp window; extra-large text uses one full-width
counter row with horizontal overflow instead of reducing the user's font scale.
Loading geometry follows the header. Paired previews cover own/public profiles,
320 dp, no roasters and 1.5 font scale. Font metrics of the existing Manrope labels
fit the allocated 220 dp row at the narrow normal-size layout.

The collection editor's scroll content extends behind the bottom gesture region.
The floating Save control retains its touch-safe clearance, samples the content
through CoffeePeekPhotoBackdrop and uses a thin theme outline on uniform light
backgrounds. A colored-content pair of previews demonstrates its blur source.
The immediate-save hint is removed from profile editing.

Shelf visibility caches both true and false by account and session generation.
ViewModels hydrate synchronously and revalidate in the background. The first
uncached read uses shimmer; successful writes invalidate earlier reads so they
cannot restore the old switch state. Unit regressions cover session changes,
failed refresh, cached false and read/write races.

The first compile attempt caught a nested-layout receiver reference and positional
cache-constructor arguments after the new field. Both were fixed. The subsequent
gate passed 1,272 unit/contract tests: profile impl 66, collections impl 70, shelf
data 52/impl 85, application Direct Debug 999; zero failures/skips. Direct Debug
assembly and packaged DEX checks passed. No automated UI/instrumented/screenshot
tests were added. Preview rendering through the IDE was unavailable; exhaustive
manual theme/font/window/accessibility and authenticated writes are not claimed.


The remote develop advanced with leaderboard integration while this patch was
being verified. The patch rebased cleanly over `1110ea12`; the repeated full gate
passed 1,277 tests (the application suite is now 1,004), zero failures, and Direct
Debug assembly/DEX checks. The code is published as `87383183`.

On the user's immediate-update request, that source was built as
`directDebug -PpushTestApp=true`, package `com.coffeepeek.pushdebug`, version
1.1.5.2702, SHA-256
`43BB4743F06F4A7F477758F457E451F1C102F5B0095D310676825BA2941D70EF`.
The certificate matched the existing Debug. Installation succeeded in place on
Pixel 9/API 37, increasing version 2691 to 2702 and preserving first-install time
2026-11-04 14:26:23. Cold launch completed in 890 ms; the later process check
confirmed it remained alive. No data clear, uninstall, downgrade, Sandbox run or
live-account edit was performed. Full authenticated visual acceptance remains a
separate gate.

### Immediate heart picker — 2026-11-04

Verification plan: open the picker for either heart state without waiting for a
favorite read/write; show initial/background membership loading inside it; allow
Back/Cancel while loading without a write. Preserve authentication continuation,
membership confirmation, account guards and confirmation-only analytics. Run
discovery/collections presentation and application unit/contract suites, then
Direct Debug assembly and the packaged DEX gate. Manual Sandbox theme, font/window
size, accessibility and modal checks remain device gates.

The migrated Discovery search still called the direct favorite writer, which
loaded shop details and waited for a server write before delivering the picker.
Its API now exposes a narrow synchronous collection-selection handoff. Android
composition supplies the existing FavoriteCollectionsPrompt; search dispatches
the tap before optimistic state or write handling. Guest taps use the same
application auth continuation as the other hearts. Confirmation remains owned by
collections and its existing account/consent-aware analytics adapter; opening or
cancelling the picker does not emit favorite-mutation events.

The root picker supplies a composition-owned ViewModelStore, cleared when the
request leaves composition, so dismissed loads are cancelled and reopening the
same target creates a fresh model. Membership refresh displays the shared shimmer
inside the sheet even when cached lists are already visible. Existing load errors
and retry stay inside the sheet. Paired light/dark refresh previews are colocated
with the changed status component. No resources or Gradle dependencies changed.

Verified source: `85caf6dc` plus this chat's local working changes on
`codex/migration-segmented-control-height`. JDK 21, `--max-workers=6`: discovery
impl 141 tests, collections impl 71, application Direct Debug 1,005; total 1,217,
zero failures/errors/skips, no retries. Direct Debug assembly and
`verifyDirectDebugRuntimeClasses` passed. Regression coverage includes both heart
states without writes, guest handoff, an unavailable favorite source, and Back
during a gated collection load. Existing Preview deprecation and unrelated test
warnings remain. No automated UI, instrumented or screenshot tests were created
or run. No device was selected, installed to or launched; previews compiled but
were not rendered in an IDE. Manual Sandbox and native iOS review remain open;
the iOS writer/host was not changed. Diagnostics remain local outside tracked
sources; no real account operation or production analytics event was performed.
