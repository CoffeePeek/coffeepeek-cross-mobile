# Home preparations — incremental read foundation

2026-10-16: prepare a pure domain boundary for the combined Android history in
PR #95. This does not migrate or replace the existing Home editor/writer.

## Module gate and migration boundary

`domain` owns preparation read models, civil-date query bounds, page metadata and
a read-only page/calendar contract. Dependencies: Kotlin only. Allowed consumers:
Home data/presentation and application composition. Other features must not
import this domain: history owns its own read port/models, adapted by the app.
HTTP, DTOs, uploads, repository implementations and DI stay outside domain.

A package inside `features/home-checkin` would also expose that artifact's Ktor
dependencies and mixed read/write implementation. A separate populated module
enforces the independent boundary. No empty api/data/impl modules are created.
The existing legacy artifact and all iOS/editor/writer consumers remain intact.

Current path: legacy HomeCheckInRepository → Android app compatibility reader →
new Home domain → app history adapter → history-owned port. The Android history
account lease must wrap the final port before presentation consumes it. Resolving
DI does not open a lease or make a request. There is no new client or endpoint.

`preparedOn` stays a civil date distinct from `preparedAtUtc`/`createdAtUtc`.
The compatibility reader forwards the existing `from`/`to` date strings unchanged,
including equal day bounds and first/last calendar days; it does not substitute
Shop UTC-exclusive bounds. This preserves the audited client's convention, not
independently verified backend inclusivity. Verify the server boundary before
activating the combined renderer. Unknown visibility stays null and unknown
moderation stays Unknown. Recipe fields and issued photo URLs are retained;
no URL is synthesized from an ID/storage key.

Next: combined history MVI/UI, delegated Home actions and Android route checks.
Full Home data/editor migration is a later feature checkpoint, not a prerequisite
for this compatibility read foundation. No root route is switched here.

Verification: three pure Home model tests, two new history model tests and 14
app read/adapter/session/transport scenarios per Android unit variant pass.
Both app APKs and expanded packaged DEX checks pass, as do app/Home/history lint
and shared domain compilation for iOS Simulator Arm64. Unknown states, recipe/
photo values, same-day bounds, cancellation and account replacement use fake
repositories/HTTP only. No real account, writer, camera or iOS screen was touched.
