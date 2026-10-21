# Analytics and route parity repair — 2026-10-20

The migrated Shop omitted existing telemetry: detail views, section exposures,
content/actions, guest gates and the check-in funnel. Migrated shop favorites
and saved-roaster removal bypassed the old tracking wrapper. The route effect
also hard-coded Yandex instead of the existing Android provider chooser.

## Parent checkpoint #43

- Shop uses injected `ShopAnalytics` / `ShopCheckInAnalytics` ports in api.
  Feature UI does not import the SDK or application analytics globals.
- Application adapters use the current consent-aware service, event schema,
  related-detail metadata and check-in tracker. No new event names or private
  values are introduced. Existing global-create handoff is consumed once.
- Form events follow actual field changes, validation, accepted save/failure
  and cancellation, not recomposition. Session revocation suppresses telemetry.
- Shop favorite writes and saved-roaster removal report attempts/results through
  the existing server-favorite event schema. Scope generation excludes stale
  account results; coroutine cancellation is rethrown. Logout/cache clearing is
  not reported as a favorite deletion. Drink removal had no legacy event and
  does not acquire an invented entity type.
- Shop route effects invoke `ShopRoutePicker`, restoring the existing system
  chooser for Google/Yandex/other installed maps with a web fallback. Platform
  selection remains in composition; the feature publishes coordinates only.

## Coffee checkpoint #104

Search/filter controls lost their legacy `search`/`filters` visibility markers
(`placement=coffees`, `page=coffees`). Restore them through the existing screen
port, together with the filters' overlay ownership. A missing coffee photo must
not count as an exposed photo; the current placeholder had inherited that marker.
Search/filter/content click events and detail/related correlation already have
active adapters and are retained, not emitted a second time.

## Full stack wiring audit

| Area | Existing contract and repair |
|---|---|
| Favorites | Server-backed shop changes and saved-roaster removal regain attempt/result events; heart exposure is injected into the active entry. Drink removal had no matching legacy event. |
| Shop detail | Detail view, hours/menu/photos/roasters/contacts/favorite/share/check-in/directions exposure; content/actions and auth gates are restored through the active Android factory. Address/map and route-button entry points remain distinct. |
| Shop check-in | The actual MVI form reports fields, submit, validation, save/failure/cancel; a photo upload failure retains its phase/cause rather than appearing as a save failure. Global flow handoff and session guard remain active. |
| Shop report | The missing parent report action/gate is restored. Existing root navigation and shared HTTP request observations remain; no unsupported report event is invented. |
| Combined history | Active renderer still injects create-picker exposure/overlay and calls `HistoryCreationAnalytics` through real callbacks. Root navigation owns the screen event. No second read-only timeline event is invented. |
| Home | Active global/history composition still supplies `AndroidHomeCheckInAnalytics`, preserving field/stage/validation/save/cancel, inherited flow and account guards. Existing contracts are retained and checked by unit tests. |
| Coffee | Active Android list/detail factories pass event and screen adapters. Search/results/open/filter/detail/related correlation remains; missing search/filter exposure, filter overlay and empty-photo eligibility are repaired. |
| Roasters | Active Discovery/detail factories pass both analytics adapters and the tracked favorites port. Settled search/results/open, related-shop entry, detail/content/exposure and contacts still use the legacy schema. No duplicate tracking wrapper is added. |

`TrackNavigationScreen`, consent restoration, identity synchronization and request
observation remain application-owned. Sandbox intentionally initializes the graph
with `analyticsEnabled=false`; that is not a delivery failure and is not changed.

## Verification

Final stack source: `f01829fed53a425aa77dcca5809eca7fcf2e6ba6`, followed only by
this audit update. Sequential Gradle verification (`--max-workers=6 --no-parallel`)
succeeded in **55s**: Direct Debug, Play Debug and Direct Sandbox APKs, all
packaged DEX guards and Sandbox isolation. The complete unit-task selection
passes: **1805** executions, zero failures/errors/skips; five tasks reran and
25 reused up-to-date results. Direct and Play repeat common cases, so this is
not a count of unique cases. No UI suites, device launches, live account operations or
production telemetry were used.

The earlier full tip run also passed (2m 8s), before the final address-placement
and photo-phase additions. Final unit-task output distinguishes executed tasks
from reused up-to-date results; no failed run or retry was hidden.

The data-factory IDE inspection completed without errors. Later app adapter
inspections timed out and are not counted as clean IDE passes; Gradle compiled
the same sources. Existing preview and AGP/KMP deprecation warnings remain.

Parent #43 verification: Shop impl **98** and application Direct **445** unit
tests pass (zero failures/errors/skips), including new field/save/privacy and
favorite mutation regressions. Direct Debug assembly and packaged DEX checks
pass; the final unchanged-source verification took 19s. No UI tests or device
launches were performed. IDE checks completed without errors for the feature
action mapper and form ViewModel; the first form-adapter inspection timed out
and is not counted as successful.

Use in-memory unit/contract tests, APK assembly/DEX checks and manual Sandbox
review. Do not create or run UI tests or emit production test telemetry.
Device chooser behavior and actual analytics delivery after explicit consent
remain separate acceptance gates. No installed app is updated by this repair;
in-memory analytics contracts do not prove PostHog ingestion or dashboards.
