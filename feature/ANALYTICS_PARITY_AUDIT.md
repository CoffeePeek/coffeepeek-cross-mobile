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

## Verification and remaining work

## Coffee checkpoint #104

Search/filter controls lost their legacy `search`/`filters` visibility markers
(`placement=coffees`, `page=coffees`). Restore them through the existing screen
port, together with the filters' overlay ownership. A missing coffee photo must
not count as an exposed photo; the current placeholder had inherited that marker.
Search/filter/content click events and detail/related correlation already have
active adapters and are retained, not emitted a second time.

Parent #43 verification: Shop impl **98** and application Direct **445** unit
tests pass (zero failures/errors/skips), including new field/save/privacy and
favorite mutation regressions. Direct Debug assembly and packaged DEX checks
pass; the final unchanged-source verification took 19s. No UI tests or device
launches were performed. IDE checks completed without errors for the feature
action mapper and form ViewModel; the first form-adapter inspection timed out
and is not counted as successful.

Use in-memory unit/contract tests, APK assembly/DEX checks and manual Sandbox
review. Do not create or run UI tests or emit production test telemetry.
Record the parent build and then audit child history/Home/Coffee/Roasters
adapters and active DI/routing before claiming stack-wide parity. Device
chooser behavior and actual analytics delivery are separate acceptance gates.
