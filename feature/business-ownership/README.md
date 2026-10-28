# Business ownership: Android and iOS

Base checkout: develop 2016c84f, updated before implementation as requested.
This feature is isolated from the other open migration checkpoints; their heads and worktrees are unchanged.
Contract: backend PR #364, docs/business-ownership-api.md at b6b2c33.

Owns user claims/history, current owned businesses, profile editing and profile galleries.
The new api/domain/data/impl modules enforce the same boundaries as roasters: api exposes
only the shared screen entry and platform image/picker slots; domain owns models, account
and repository/upload ports; data hides HTTP DTOs and maps Result failures; impl contains
shared MVI and Compose UI. Composition adapts the existing session and Media uploader.
Packages alone cannot prevent UI from importing HTTP DTOs or platform/legacy code.
Domain depends on coroutines only; data on domain/core-network/Ktor/serialization;
impl on its api/domain, presentation/design-system and lifecycle. Only application
composition consumes the factories. No other feature internals are imported.

Both root navigators and Android/iOS profile entry points must reach the same entry.
Ordinary authenticated User accounts can apply; no global Owner/Roaster role is required.
Approval/rejection stays in the web administration interface. No document uploads,
employees, email verification, notifications or automatic account-deletion revocation.

Verification plan: contract tests for user-scoped endpoints, slug submissions, paging,
If-Match, gallery Media IDs/URLs and preservation of untouched sections; ViewModel tests
for validation, duplicate mutations, conflicts, revocation and session changes. Build the
Android directSandbox variant and its runtime DEX checks. Common UI uses paired previews;
no automated UI/instrumented/screenshot tests. Manual Sandbox/device checks require an
available, verified device. Native iOS compilation and contract tests use macOS CI;
Linux Android assembly is not evidence of native iOS/device acceptance.

New UI strings use feature Compose resources (Russian matches the current client locale).
Profile saves retain the original city identity, schedules and catalog associations unless
explicitly edited; unknown/public slugs are never sent in UUID fields.

## Verification record — 2026-10-28

Local checks on this feature tree:
- 25 ownership unit/contract tests (domain 5, data 9, presentation 11), no failures/errors/skips.
- 60 affected Profile tests and 4 new application navigation/Sandbox HTTP tests, no failures/errors/skips.
- Shared ownership Compose/KMP metadata compilation, Direct Sandbox and Direct Debug APKs,
  packaged DEX guards and Sandbox isolation passed.
- Build-only work: no app was installed/launched, no real account, camera, consent, analytics
  or production-backend actions were performed. ADB inventory had no connected devices.
  Studio MCP/preview diagnostics are unavailable in this session.

Retained failures/retries: initial shared UI compilation exposed missing loading descriptions,
then the explicit BackHandler dependency/experimental opt-in. Android compilation passed
once corrected. Metadata compilation separately caught the JVM-only no-arg cancellation
constructor; it now supplies a common message and metadata compilation passed. An initial
attempt named a nonexistent Sandbox unit-test task; the existing Direct Debug unit task
already includes Sandbox backend sources and is the executed integration-test path.

Android UI light/dark/font/window/accessibility/modal/native picker/back checks remain
manual device gates. Native iOS simulator app build and execution of shared contract tests
are delegated to the existing macOS workflow; until a successful run is recorded, native iOS
build/runtime acceptance is unverified. Live API acceptance also remains open and needs
backend #364 plus migration 20261028141439_AddBusinessOwnershipClaims.

Profile editing covers name/about/contact links/address/coordinates and profile galleries.
Existing city identity, schedules and catalog associations are preserved; this UI does not
add controls to replace those fields. Raw claim evidence/contact is neither persisted nor
sent as analytics properties. Native navigation restores only the optional public type/slug.
