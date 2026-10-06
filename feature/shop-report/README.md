# Shop issue reporting: prepared domain and data

This is an Android-first migration slice. The existing `ShopReportScreen`,
ViewModel, Koin binding, repository, and endpoint remain active in the legacy
modules. Neither Android navigation nor the iOS application is switched here.

| Module | Responsibility and public API | Allowed dependencies | Allowed consumers | Hidden implementation / why a module |
|---|---|---|---|---|
| `domain` | `ShopIssueCategory` and `ShopIssueReportRepository`, returning `Result<Unit>` | Kotlin only | This feature's future UI/data; explicitly approved pure business consumers | No HTTP, Compose, Koin or legacy domain dependency. A Gradle boundary prevents those imports as the feature grows. |
| `data` | `createShopIssueReportRepository(HttpClient)` for application composition | domain, core/network, Ktor and serialization | Application composition only | Backend DTOs, endpoint, mapper and repository implementation are `internal`; a package inside `composeApp` could not enforce the UI/data boundary. |

There is no `api` or `impl` module yet: no new route or screen entry is published
until the Android presentation slice is prepared. Domain/data use manual
construction; Koin assembly belongs to application composition when Android
switches. The factory expects the application's configured authenticated client
and does not create another client or session.

## Migration sequence and risks

Current code: `ShopReportScreen`/`ShopReportViewModel` in `composeApp`,
`ShopIssueReportRepository` and category in `modules/domain`, its implementation
in `modules/data`, and `/api/ShopIssueReports` service/DTO in `modules/network`.
The new domain mirrors the existing category set and Result contract. New data
keeps the same endpoint and JSON names, but handles coroutine cancellation via
`core/network.requestResult` instead of suspend `runCatching`.

1. Prepare and test domain/data in isolation (this slice). No active binding is
   changed, so there is no second writer or duplicate request at runtime.
2. Add an intentionally small `api` entry contract only when the new Android
   screen needs to be registered. Prepare `impl` with MVI, resources and colocated
   light/dark previews. Keep presentation labels out of domain.
3. Switch only the Android composition binding/route and test submission,
   validation, duplicate taps, cancellation and navigation. Keep the legacy
   route/binding for iOS until its own approved migration.
4. Remove legacy copies only after all consumers on both platforms have moved.

Risks: the server may return unsuccessful HTTP statuses or PascalCase response
fields; both must remain Result failures. The current screen enforces a 500-char
comment cap and requires a description for `Other`; the future UI must preserve
those rules and localized copy. No iOS screen or storage work is included here.
