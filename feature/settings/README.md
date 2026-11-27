# Settings and account analytics preference

Reviewed against `develop` on 2026-11-27. This guide replaces the completed account
preference rollout plan; exact historical build/deployment evidence is in Git.

## Ownership

`api/domain/data/impl` expose settings, theme, language, city and usage policy
through narrow entries and ports. Android uses feature Compose; iOS registers
native settings, theme/language/city/policy views. Application composition adapts
existing account, preferences, session, external actions and SDK/update services.
Settings does not own root navigation or a second analytics SDK.

Settings domain owns `AnalyticsPreferences`. [Settings data](data/README.md)
implements account preference HTTP calls, pending choices and one-time local
migration using the configured network/session/database infrastructure. DTOs,
keys and repository implementations stay private to data. Application assembly
maps the resolved setting to the existing analytics service gate.

## Analytics contracts

- `GET/PUT /api/Users/me/analytics-preference` uses the authenticated account and
  returns `accountId`, `enabled`, `updatedAtUtc`. PUT supplies `accountId`,
  `enabled`, `onlyIfUnset`. The server owns authorization and concurrency.
- Server default enabled is not an explicit user decision. Load the account
  preference before collection and reconcile on account/foreground changes.
  A failed server read cannot turn an unresolved local default into permission.
- A known explicit server preference wins over optional registration/OAuth
  initialization and imported installation choice. Import applies only to the
  account restored at startup and uses `onlyIfUnset`; guest legacy preferences
  cannot be attributed to a future sign-in account.
- Failed writes remain pending for the same account and retry on a later load.
  A local refusal stops delivery immediately. Late responses or operations from
  another account cannot enable the replacement account's collection. Guest
  collection remains off.

Keep published privacy text/store disclosures aligned with the actual SDK gate;
do not claim a backend migration was deployed just because the client compiles.
See [analytics](../../docs/ANALYTICS.md) for the shared event/consent ownership.

## Remaining source-level follow-up

The application `ThemeManager.setTheme` publishes its in-memory choice before
launching `SettingRepository.save` in a `Default + SupervisorJob` scope without
local exception handling or a result passed back to the settings UI. Handling
theme persistence failure remains a follow-up; fixes to city/language persistence
do not establish that this path is fixed. This is source evidence, not a newly
reproduced device crash or a reason to change runtime code in this docs update.

## Source and verification

Start with [API](api/src/commonMain), [domain](domain/src/commonMain),
[data](data/src/commonMain) and [presentation/native sessions](impl/src).
Existing tests cover settings/theme/language/city/policy state, review prompt
policy and account preference migration, pending writes, cancellation and account
replacement. Auth and app analytics integration have their own contract suites.

Behavior changes need affected tests, Android runtime-class build checks and
relevant Native compilation/test selectors. Manual checks cover both themes,
language/city choice, large text/accessibility, sheet/back/cancel, and actual
account preference persistence only in an authorized test setup. Fake tests do
not certify live PostgreSQL migration or PostHog ingestion. This docs review
performed no deployment, app build, telemetry emission or device action.
