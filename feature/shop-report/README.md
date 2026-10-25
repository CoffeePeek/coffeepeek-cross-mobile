# Shop report feature

## iOS activation — 2026-10-25

Android and iOS now select the shared feature entries through app composition.
Native navigation, photo/map/date presenters and the existing backend contracts
are retained. Legacy screens remain a fallback until acceptance; the older notes
below describe the original Android-first checkpoints. Current verification and
remaining device/server gates: [iOS migration plan](../IOS_MIGRATION_PLAN.md).

## Grouped reasons correction — 2026-10-15

All six existing reasons now share one outlined rounded group with dividers.
A yellow check replaces the visible radio control, but selectable-group and
single-choice accessibility semantics remain. Nothing is selected initially;
the existing Other/500-character rule and submit intents are unchanged. Reason
changes are disabled during submission. Component/screen previews remain
colocated in paired themes; no domain, transport, root DI or iOS change is needed.

Self-targeted Android tests and a fake visual activity exercise these controls
without application DI, session storage, network or report submission. The
activity exists only in the instrumentation APK, not the shipped app.

All four isolated reason-list UI tests pass on the temporary emulator: both
themes/single selection, divider-only row gaps, Other/typed submit intent and
pending-state protection. Both app variants assemble; feature/app lint and the
five Shop Report MVI unit tests also pass. Real server submission/error handling
and individual IDE preview rendering remain release gates, not inferred results.

Android-first migration of the “report incorrect coffee shop data” capability.
Android composition now supplies the feature entry with the application's
configured authenticated HTTP client. The existing shared/iOS route and legacy
Koin binding remain active; no iOS screen or native integration was switched.

| Module | Responsibility and public surface | Dependencies and consumers |
|---|---|---|
| `api` | Minimal composable `ShopReportEntry` screen contract | Compose runtime; application composition |
| `domain` | `ShopIssueCategory` and `ShopIssueReportRepository`, returning `Result<Unit>` | Kotlin/coroutines only; feature impl/data |
| `data` | `createShopIssueReportRepository(HttpClient)` construction surface | Domain, core/network, Ktor and serialization; application composition only |
| `impl` | MVI form, resources, API adapter, and Navigation 3 entry registration | API/domain/design-system/core presentation; application composition |

`data` owns the request, response DTOs, mapper, and repository implementation.
Its backend implementation is hidden. UI never sees transport types. The
application supplies its configured authenticated `HttpClient`; no second
client or Koin module is created in the feature.

The form preserves the existing category set, requires a description for
`Other`, limits it to 500 characters, reports request failures without leaking
backend text into UI, and keeps `CancellationException` semantics. The screen
content is stateless and its previews use fake state. User-visible copy is in
feature resources; the repository currently has only the default resource
locale for migrated features, so no unsupported translation is claimed.

## Remaining migration

1. Done: assemble the repository and entry in Android application composition
   using the existing authenticated client.
2. Done: route Android Shop Report through the new feature while the shared/iOS
   fallback continues using the legacy screen.
3. Done: compile the app and cover request success/failure/cancellation and MVI
   validation, duplicate submission, retry and back navigation with unit tests.
4. Manually inspect both previews and the submit flow against a non-production
   endpoint; automated tests do not verify the rendered modal or live server.
5. Retire legacy code only after all platform consumers have moved.

The current root navigator is still a shared Navigation 2 host and retains
ownership of route arguments and back-stack changes. A root Navigation 3
migration is not part of this feature slice.

## Retroactive parity audit — 2026-10-20

Open gaps from the legacy-first audit: S1–S5 (medium: S1 pre-send failures block resubmission). Full table and verification status: [LEGACY_PARITY_AUDIT.md](../LEGACY_PARITY_AUDIT.md).

Fixes applied 2026-10-20 and owner decisions on intentional differences: see the "Fixes applied" and "Intentional differences" sections of [LEGACY_PARITY_AUDIT.md](../LEGACY_PARITY_AUDIT.md). Device verification is still open.
