# Account analytics preference — 2026-11-13

Owner request: keep analytics selection on the server and initialize it to true.
Known explicit choices remain authoritative. Default enabled is not recorded as
explicit consent. No production deployment, database update or telemetry is part
of local verification.

## Ownership and migration

Before: composeApp AnalyticsInstallation writes an installation-wide Room setting.
After: settings/domain owns AnalyticsPreferences; settings/data owns the HTTP DTOs,
account checks, pending writes and one-time local migration. composeApp supplies
existing transport/session/database and maps the setting to the existing SDK gate.
A new data module enforces this boundary; see data/README.md. No new SDK or analytics
service. Domain has no infrastructure dependency. Impl depends on its public port.

Backend Account owns Users.AnalyticsEnabled (database default true) and nullable
AnalyticsPreferenceUpdatedAtUtc (default is not an explicit decision). The latter
also provides concurrency protection. Account API uses the authenticated identity
and verifies the submitted accountId before a write. Other services are unchanged.

API: GET/PUT /api/Users/me/analytics-preference returns the standard envelope with
accountId, enabled, updatedAtUtc. PUT takes accountId, enabled and onlyIfUnset.
Registration and optional Google/Apple initialization accept analyticsEnabled.
A previous explicit server choice wins over social initialization and local import.

1. Deploy Account migration and API first.
2. Publish client: load before enabling collection, reload on account/foreground.
3. Existing local choice moves only to the account restored at startup, only if
   the server has no explicit choice. Missing/deleted local data cannot be recovered.
4. Failed writes remain account-scoped and are retried; local refusal stops SDK
   delivery immediately. Old account responses and operation scopes cannot enable
   replacement-account collection. Guest collection remains off.

## Verification plan and evidence

Mobile source: child of 31b678b5f, branch codex/migration-analytics-account-preference.
Backend: isolated worktree, base 4be05cf1, branch codex/analytics-account-preference.
Environment: Windows, 2026-11-13. Studio MCP unavailable; shell diagnostics only.
Behavior: default-on after server resolution, explicit false roundtrip/reinstall,
account switching, delayed responses, offline pending choices, cancellation,
legacy import, registration and preserved sign-in events.

- Initial settings/data production compilation passed; function limits: 0 new.
- Initial mobile test compilation failed because the new fixture lacked Ktor
  content-negotiation/json dependencies; corrected before rerun.
- Backend domain: 240/240; application: initially 227/227, final 236/236 after nine
  registration/OAuth cases; final persistence/controller/generated-handler checks: 9/9.
  Persistence tests use EF InMemory; model checks use Npgsql without connecting.
- Mobile settings/data: 9/9; auth/data: 29/29; auth/impl: 39/39; app: 659/659;
  settings/impl: 42/42. Total 778 unique executed tests, 0 failures/errors/skips.
- DirectDebug and DirectSandbox assembled; both packaged DEX runtime-class checks
  passed, Sandbox isolation check passed. No APK installation or application run.
- EF reports no pending model changes. Backend secret scan passed (1191 commits).
- Combined baseline-generation/verification command hit Gradle implicit-task-dependency
  validation; both tasks then passed as separate sequential commands. Only the
  generated removal for AnalyticsService.reconcile is retained; unrelated baseline
  drift is excluded. Final repository hygiene checks continued on 2026-11-14.
  No test failure was hidden by retry.

Not verified: actual PostgreSQL migration execution, live backend/PostHog ingestion,
real OAuth, iOS/Xcode build, or manual device UI. No instrumented Compose/screenshot
suite created or run. Manual Sandbox review must cover light/dark, large text,
accessibility, back/cancel; no device selected or APK installed. Colocated previews
use fake state. Published privacy text/store disclosures remain a release gate.

Pre-existing build warnings include AGP/KMP deprecations and backend NU1903 for
Microsoft.AspNetCore.WebUtilities 8.0.20; dependency upgrades are outside this change.
