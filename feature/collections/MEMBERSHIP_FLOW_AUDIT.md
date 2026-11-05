# Collection membership and favorite projection audit

Date: 2026-11-04. Mobile base: `b5bde6ac9591fc1cb3f1f96017b15b0fd2f59e25`
on `codex/migration-collections-membership-flow`. Scope: Android collection
selection, its server-backed favorite projection, and the matching backend
contract. Post likes and the native iOS favorite writer/screen are unchanged.

## Verification plan and reproduced defects

Preserve immediate sheet opening, explicit confirmation, cancel/back without a
write, account isolation, private legacy favorites and analytics consent/session
guards. Test add, last removal, one-of-many removal, a move between lists, no-op,
fresh/stale reopening, failed writes and late/failed reads. Run affected unit and
HTTP/composition contract suites plus exact Direct Debug/Sandbox assemblies and
their packaged runtime-class checks. Manual Sandbox checks remain a separate
device gate; no real-account mutation is authorized by this audit.

Three independent investigations traced picker state, client cache/projections,
and backend persistence/HTTP behavior. The screenshot alone does not establish
whether confirmation had completed; the defects below were reproduced with
synthetic data and exact source revisions.

1. Backend main `e6da3360933a9a8c4e2e2c529c0b8c46c94a43ca` removed collection
   items without updating `DiscoveryFavorites`. The next favorite read restored
   the heart. Adding a catalog place without a preliminary legacy favorite PUT
   also failed. The same six new regressions failed four cases on main and
   passed on develop `14d0fe7b162fde42dc956683282a8f6dba707179`.
2. The client ran the mutation/refresh path even when the selection had no delta.
   A new picker also refreshed a valid day-long cache, producing unnecessary
   loading. The analytics adapter awaited two forced favorite GETs around a PUT.
3. Card IDs depended on complete favorite metadata. A successful membership PUT
   did not synchronously update those IDs, and an older in-flight read could
   disagree with the confirmed write.

The server correction existed only in develop when mobile 1.1.6 was published.
The previous release verification did not check that production contract
dependency; passing client fake tests did not establish server compatibility.
The last successful main deploy workflow was run 41798536619, job 126475969670.
The actual running container SHA was not independently inspected.

## Confirmed behavior

| Action | Client requests and state | Server result |
| --- | --- | --- |
| Open with a fresh owner/target snapshot | Show the sheet and verified selection immediately; no membership reload | No mutation |
| Open missing/expired data | Load inside the sheet; stale rows remain visible but cannot overwrite unverified membership | Read; initialize canonical default only when explicitly needed |
| Done with no changes, including toggle then undo | Close without busy/loading, GET, PUT, analytics mutation or projection change | No mutation |
| Add a catalog place | Rebase current membership, send one membership PUT; apply its confirmed favorite receipt to the card before closing | Add membership and favorite atomically; no preliminary legacy writer |
| Remove from one of several lists or move between lists | Apply the confirmed favorite receipt; heart stays selected | Preserve other active owned memberships |
| Remove the last membership | Clear the confirmed card key immediately after the successful PUT | Remove the normalized target from favorite reads |
| Membership PUT fails | Retain verified cache/card and the selection draft for retry | No partial mutation |
| A late or failed favorite GET follows a successful PUT | Preserve the newer confirmed key; a missing metadata row cannot certify a complete full-list snapshot | A later fresh read supplies metadata |
| A stale draft removes an already absent membership | Apply authoritative favorite state without a change event or background refresh | Preserve valid private legacy favorites and report actual no-op |
| Cancel/back or empty collection cover | No membership write; no detail-photo request for a zero-count row | No mutation |

The existing 24-hour cache remains in-memory and owner/target scoped; it does not
survive a process restart. Explicit reload bypasses it. A real membership change
still rebases before the PUT for old-server compatibility and independent
concurrent edits. The server applies delta requests under its owner transaction
lock. One cancellable background favorite refresh remains necessary for metadata
and non-picker list edits; it is outside the confirmation path.

Full rows and metadata-independent favorite keys share one server cache. Confirmed
keys are guarded by account identity, account generation and revision. They never
create fake `PublicAddress` metadata. Unknown global membership does not trigger
an analytics-only network read or a guessed favorite event. Only confirmed real
picker writes project the authoritative receipt; a read or no-op cannot clear a private
legacy heart merely because it has no collection membership.

The existing response data remains an array for old clients. Successful membership
PUTs add `X-CoffeePeek-Is-Favorite` and `X-CoffeePeek-Membership-Changed`, computed
with the mutation under the existing owner lock/transaction. The first reports
effective persisted favorite state; the second reports an actual membership
change. The collections domain returns these as an immutable command receipt.
Missing/invalid headers are unknown: a positive membership proves true, but an
empty list cannot prove false. Unknown false is left to the existing background
authoritative refresh. Confirmed no-op receipts update cached state without a
mutation event, refresh or `FAVORITE_CHANGED` analytics. No extra client GET or
post-commit controller query is added.

## Backend and Sandbox fidelity

[Backend PR #379](https://github.com/CoffeePeek/coffeepeek-backend/pull/379)
contains the three existing related develop commits (`13f52770`, `a8827499`,
`14d0fe7b`) and six new regression cases. The production fix synchronizes only
affected targets, preserves legacy direct writers and their ambient transaction
semantics, and accepts catalog additions without a second favorite writer.
Two review regressions are also corrected: existing unavailable places can be
retained/reordered while availability is checked only for additions, and an
actual membership no-op cannot clear private legacy favorite rows. Receipt
metadata prevents a stale client draft from confusing this no-op with a real
last removal. Wolverine code generation is checked for the typed handler result;
unrelated generated churn is excluded.

Favorite reads are the union of active owned memberships and remaining legacy
favorites. Actual membership changes normalize only the affected target; a no-op
leaves legacy state intact and returns its authoritative state in the receipt.
Canonical default initialization adopts an existing active default, otherwise
the oldest exact-title `Избранное`, otherwise creates a private list. First
private adoption seeds only visible legacy shops/roasters absent from all owned
lists. An existing canonical default is never reseeded. A public default never
receives private legacy favorites; a private heart without membership is valid
in that case. Reads, metadata, featured edits and no-op Done do not clear it.
Drinks remain legacy favorites. There is no mass cleanup of legacy hearts.

Sandbox now models those rules, including catalog-only additions, direct-writer
compatibility, selective synchronization for replace/delete, public-default
privacy and no implicit default creation during raw reads or metadata edits.
Its catalog includes the existing paged Discovery fixtures, not only currently
favorited places. Sandbox contract results certify the fake setup, not a live
backend deployment.

## Validation record

Backend: .NET 10.0.113, isolated checkout, synthetic local data. **219 unique
cases passed** across the audit, with zero failures/skips, deduplicated by class
and parameterized test name. The initial 189 include 37 membership/favorites
cases (five real PostgreSQL 17 migration/locking/rollback cases), 106 HTTP
contracts and 46 read projections. Review follow-ups add 22 infrastructure
cases, four HTTP receipt cases, two generated-handler gates and two PostgreSQL
atomic-receipt cases. The latest receipt gate passed 168 cases: 54 infrastructure,
112 HTTP/codegen and two PostgreSQL. These counts overlap the original gate and
are not added twice. No-op receipt tests check both kinds, stale/empty delta,
private legacy hearts and actual last removal; PostgreSQL verifies uncommitted
visibility plus joint membership/favorite commit and rollback.

Reproductions are retained: four failures on original main, four unavailable-item
failures before its fix, and eight no-op failures before its fix. Required
Wolverine `codegen write` completed successfully; inferred result variables made
the target adapter semantically unchanged, and unrelated generated churn was
excluded. Ephemeral PostgreSQL containers and credential files were removed.
Local tests used no production database, container or account. The mandatory
secret scan passed before publication.

Mobile: JDK 21, sequential Gradle with `--max-workers=6`. The initial gate failed
to compile a new HTTP trace using `URLBuilder.encodedPath`; it was corrected to
use the built URL. The next gate exposed an existing Discovery fixture regression
(favorite PUT for `discovery-3` returned 404); the fake catalog was corrected and
the failure retained in local diagnostics. A subsequent full gate passed 1,496
unit/contract cases plus Direct Debug/Sandbox assembly, DEX runtime-class checks
and Sandbox manifest isolation. After adding the metadata-race and integration
cases, the final gate passed **1,504 cases**, zero failures/errors/skips:
modules/data 80, collections/data 46, collections/impl 79, discovery/impl 141,
shop/impl 109, roaster/impl 23, application Direct Debug 1,026. Direct Debug and
Sandbox assembly, runtime-class verification and Sandbox manifest isolation
passed again. Existing Preview/expect-actual and unrelated test warnings remain.
This was a corrected gate after code changes, not a flaky retry.

The atomic-receipt follow-up on `0fc83589` then passed **1,520 cases**, zero
failures/errors/skips: data 80, collections data 54, collections impl 79,
discovery 141, shop 109, roaster 23, app 1,034. Direct Debug/Sandbox assemblies,
both DEX checks and Sandbox isolation passed again. New coverage includes strict
true/false/missing/invalid response metadata, a stale actual-no-op preserving a
private legacy heart without another GET, no-op analytics, consent revocation,
and actual last removal. The Sandbox receipt is captured under its fixture lock.

No Studio MCP was available. Read-only ADB inventory found no connected device.
No APK was installed or launched, and no automated UI/instrumented/screenshot
tests were created or run. Existing colocated light/dark picker previews compile;
IDE rendering, manual Sandbox theme/font/window/accessibility/back/modal checks,
native iOS and live-account persistence acceptance remain unverified. Logs and
test result artifacts stay outside tracked sources.

## Publication order

Publish the backend before the Android fix. Merging backend PR #379 into main
triggers the normal production deployment, including additive migration
`20261104105519_AddCanonicalSavedCollectionDefault`: a marker column and filtered
unique owner index, without deletes or bulk backfill. Inspect CI, migration and
health results before declaring production fixed. Prefer retaining this additive
schema if service images need rollback; dropping the marker loses canonical
identity. No manual legacy-favorite cleanup is part of this change.

The user approved backend publication after successful CI. PR #379 is ready for
review; deployment remains gated on that CI and its normal migration/health
workflow. A passing local suite, an APK build or a PR alone is not deployment or
live-account acceptance.
