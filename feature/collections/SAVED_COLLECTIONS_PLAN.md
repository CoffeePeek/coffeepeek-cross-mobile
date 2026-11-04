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
