# Content reports — route migration

This owner migrates `ReportCheckIn` and the retained `ReportReview(id, isPreview)`
compatibility route from composeApp/review. Actual entry points are Shop detail,
public check-in lists and public profile cards. No active producer of ReportReview
was found; its route and review-specific HTTP contract are retained deliberately.
The Community feed's existing inline report dialog remains with Community.
This is moderation of a publication, distinct from Shop-report's inaccurate-place data.

| Gradle boundary | Responsibility / dependencies | Consumers / hidden details |
|---|---|---|
| api | Runtime-only composable entry, no domain or transport exports | App composition; hides state, VM, routes and request details |
| domain | Report target, 1–2000 trimmed-character rule, repository and failure categories; Kotlin only | Own data/impl and app adapters; no Ktor/Compose/DI |
| data | Existing two POST contracts via app's configured client; domain, core/network, Ktor/serialization | App construction only; backend/request DTOs and response validation hidden |
| impl | Typed MVI form, resources, previews and entry; own api/domain, design-system, presentation/lifecycle | App rendering only; no data/legacy imports |

Separate Gradle boundaries enforce the same dependencies as other migrated owners:
packages inside composeApp would retain legacy UI/network dependencies and would
not prevent UI from reaching transport. No empty/future modules are registered.
Composition owns session-bound lifetime, root navigation and client configuration.
The additional build boundaries are justified by independent Android/Native
protocol tests and enforcing that the reusable presentation cannot import Ktor
or legacy repositories. Settings includes them through the existing Modules.all
registry; no second application or generic reporting framework is introduced.

Preserve free text, limit 2000, explicit submit, submitted/preview confirmation,
back/Done and guest submit → sign-in. Preview never reads a session or sends data.
No reason categories, second analytics service or replacement root navigation.
The old form emitted no report-specific analytics; root screen exposure and callers
remain owners. Only Russian `values` exists for this form.

This feature never retries delivery failures automatically; the configured client's
existing auth-refresh handling remains in place. A received 4xx rejection can be corrected
and resubmitted; lost/invalid acknowledgements and 5xx are conservatively unconfirmed
and block another submission in that form instance. This cannot guarantee global
deduplication after a new route/process without server idempotency support.
Community and legacy HTTP paths remain compatibility implementations until their
callers can safely share this owner; do not change their dialog semantics here.

Migration: add isolated contracts/data/MVI → bind account-scoped app renderer on
both platforms → switch both existing navigation hosts → contract/unit and Android/
Native build gates → manual Sandbox/iOS acceptance. Retain legacy fallback until
that acceptance. Live reports, analytics and account writes are not test fixtures.

## Activation / verification — 2026-10-25

Android and iOS select the same feature renderer for both existing route hosts.
The account-bound child store clears the draft and cancels operations on account
replacement, preserves known-user token renewal and blocks late results. Missing
stable user IDs use token identity and fail closed on replacement. SessionReader
is read-only: this form does not copy legacy requireAuthSession's credential clearing.
Preview bypasses the account host entirely. The old screen/VM and its two unit
tests remain as fallback until manual acceptance; the compatibility Review route
retains the old check-in wording rather than inventing a new product surface.

Sandbox implements check-in report POST as an in-memory fixture with validation,
own-record rejection and reset. New local visits may lack authorId: ownership
also comes from the private visit store. Review reports have no active Sandbox
producer; their transport is covered by MockEngine contracts, not a fabricated
review dataset. Neither fake scenario certifies deployed moderation behavior.

Unit/contract, Android/Native build identities, original failures and remaining
manual gates are in the shared iOS plan. No automated UI/instrumentation/screenshot
tests, test Activities or real reports are used. CI's existing all-project
testDebugUnitTest task includes the new feature tests; CI itself was not run here.
