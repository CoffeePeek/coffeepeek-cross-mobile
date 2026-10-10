# Shop report feature

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
