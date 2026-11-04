# Responsive post likes — 2026-11-04

Scope: post likes in the migrated community timeline, shared-post screen and the
shop's check-in preview/list. Collection selection remains a separate favorite
interaction: it opens its sheet immediately and shows loading inside the sheet.

Verification plan: show the selected heart and adjusted count before a delayed
reply; reconcile the authoritative count on success and restore only the failed
post on failure/cancellation. Keep guest sign-in, ownership/publication rules,
account/author-block guards, duplicate-write suppression and analytics intact.
Verify another timeline post can be liked while the first request is pending.
Run community, shop, collection data/presentation and application unit/contract
suites, then Direct Debug assembly and packaged DEX verification. Manual Sandbox
theme/font/window/accessibility checks remain device gates.

Community presentation now applies each post's optimistic heart/count before
launching its request. Pending writes are tracked per post in the timeline;
duplicate taps on that post do not send another write, while other posts remain
available. Refresh/edit/report guards are retained. Success uses the server count;
failures restore the original heart/count and use the existing safe message.
Cancellation is rethrown, and old account/generation replies cannot change the
current timeline/post. Shop check-in preview/list likes likewise apply the heart
and count first; pending network writes keep the heart visible instead of showing
a spinner. Existing shop duplicate/refresh guards remain.

Analytics inventory: timeline Like/Unlike interaction and guest like-gate events
retain their existing owners and properties. No favorite attempt/change event is
introduced for post likes; collection confirmation retains its existing adapter.
No synthetic event was emitted to a live service. Shared design-system like
components, backend contracts and feature dependencies were not changed. Paired
light/dark pending-like previews are colocated with the timeline/post screens and
shop helpful button.

Verified base: `aeb6cac3`, plus local changes on
`codex/migration-responsive-likes-cache`. JDK 21, `--max-workers=6`:

| Unit/contract suite | Tests | Failures/errors/skips |
| --- | ---: | ---: |
| community impl | 46 | 0 |
| shop impl | 109 | 0 |
| collections data | 43 | 0 |
| collections impl | 71 | 0 |
| application Direct Debug | 1,005 | 0 |
| Total | 1,374 | 0 |

Direct Debug assembly and `verifyDirectDebugRuntimeClasses` passed. The first
application run had one failure among 1,005 tests:
`SandboxSavedCollectionsContractTest.membershipUpdatesBothKindsAtomicallyWithoutChangingGlobalFavorites`.
A membership response cached before the first verified library read could skip
default-list initialization. Cache freshness now requires that verified read for
the same owner/target. The next full gate passed; the final gate also passed after
adding cancellation/account-cache regressions. There were no unchanged-source
flaky retries. Initial failure diagnostics remain local under `/tmp`.

Existing Preview/tooling warnings remain. Previews compiled but were not rendered
in an IDE. No device was selected, deployed to or launched. No automated UI,
instrumented or screenshot tests were created or run. Manual Sandbox review,
live-backend timing/write acceptance and native iOS compilation/review remain
unverified; no live account operation was performed.
