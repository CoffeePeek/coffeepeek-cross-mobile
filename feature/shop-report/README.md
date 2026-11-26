# Shop report feature

## SwiftUI report destination — 2026-11-16

Package 5 of `docs/IOS_SWIFTUI_EXECUTION_PLAN.md` starts with the full-screen
`Navigator.Screen.ReportShop(shopId, shopTitle)` destination. The entry remains
the report action in a coffee shop detail. The existing shop feature handles
guest access before emitting `ShopDetailEffect.ReportShop`. Both the inspected
`origin/main` navigator and the `742e4e84` migration base use this route.

`NativeShopReport` gives iOS a snapshot and actions over the existing
`ShopReportViewModel`. SwiftUI renders a native form with six categories, the
Other description, validation, submitting, success and uncertain-delivery states.
The existing owner enforces the Unicode-safe 500-character limit, one pending
submission and blocked retry after an uncertain result. Known failures allow
retry with the same form. Russian and Belarusian strings live in the feature's
`ShopReport` iOS resource table.

Application composition supplies the authenticated repository and the session
reader. The adapter rejects actions and queued repository calls after the account
generation changes, clears the draft and cancels in-flight work. Token refresh
for the same account preserves the draft. Leaving the visible screen cancels its
Swift subscription. Removing the hosting controller closes the form owner on the
main queue. UIKit continues to own Back and interactive-pop transitions, with
`Navigator.popBack` as the explicit callback. The root navigation tracker remains
the screen analytics owner and the shop report action retains its existing shop
analytics event.

Verification covers five feature bridge contracts and five iOS adapter contracts
using in-memory repositories. The feature tests cover validation and Unicode,
known-failure retry, uncertain-delivery deduplication, pending-work cancellation
and unknown categories. The iOS tests cover subscription resumption, account
replacement, actions queued across replacement and same-account token refresh.
The integration adds these suites to `ios-verify`. Execution results are recorded
with the PR and CI run for the final revision.

On 2026-11-16, revision `dd417e1d8` passed Xcode Simulator compilation,
binary compatibility and all selected Kotlin/Native suites in GitHub Actions
`43866392032` for PR #374. Local JVM tests passed 11 cases in two suites,
with no failures, errors or skips. Function-body limits and the 22 RU/BE keys
and Xcode resource registration checks passed. Windows skipped application
iOS compilation and execution, so the macOS run supplies that evidence.
The subsequent integration with native tab routing `8648736bd` receives
its own final PR verification.

Manual macOS/iOS checks remain open for both languages, light/dark appearance,
large Dynamic Type, VoiceOver, keyboard dismissal, the Back button, completed and
cancelled interactive pop, and form behavior on fake success/failure responses.
No live report was submitted. This slice completes the ReportShop implementation
within package 5. Search, catalogues and the other package-5 destinations remain
in the execution plan.

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

## Native form visual parity — 2026-11-25

Source: `dce8989e5` plus `codex/migration-ios-shop-filter-parity` working changes.
The active native report route uses a scrolling column instead of SwiftUI's grouped
Form. Shop title and prompt stand above one outlined reason card. Its 24-point
corners, 16-point gutters, 12-point gaps, 56-point rows, inset separators and reason
order follow the Android renderer. Submission, success and recovery use the existing
full-width native action button. Other's outlined editor scales with Dynamic Type.

Verification plan: preserve single selection, Other validation/500-character limit,
pending-state protection, errors, successful/unconfirmed delivery and session-ended
recovery. Kotlin's existing report/session owners retain these rules and requests;
Swift only renders state and forwards actions. Screen analytics remain owned by the
root route controller; no analytics service or emission was added. Colocated fake-state
previews cover both themes, Other, errors, sending, success, unconfirmed delivery,
Belarusian copy and large text, without production DI or submissions.

Executed checks: 11 report unit tests in 2 suites and 35 notification unit tests in
4 suites passed, with zero failures, errors, skips or retries. Android `directDebug`
(`com.coffeepeek.debug`, `1.2.0.3221-debug`) assembly and the packaged DEX runtime-class
check passed with `--max-workers=6`; function limits reported 130 recorded and zero
new violations. Both native launcher registration contracts passed. Swift 6.4.2
syntax parsing with `-swift-version 6 -strict-concurrency=complete` passed for the
changed report, list and inbox files; parsing is not SwiftUI type checking or a
concurrency-safety build. No new Task, DispatchQueue, unchecked Sendable or isolation
escape was introduced.

The attempted `:composeApp:compileIosMainKotlinMetadata` failed in
`:core:database:compileCommonMainKotlinMetadata`: Room's generated
`CoffeePeekDatabaseConstructor` actual and expect declarations are in the same
module. Database source/configuration is unchanged. This failed run is retained
separately from the successful Android/unit run. Full iOS compilation and iOS-only
contracts remain unverified. No device was selected or app installed. IDE/preview
rendering, manual iPhone light/dark, keyboard, Dynamic Type, VoiceOver, navigation
and modal checks, and real backend delivery remain acceptance gates.
