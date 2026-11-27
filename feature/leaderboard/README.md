# Activity leaderboard

Reviewed against `develop` on 2026-11-27. The completed implementation/visual-fix
ledger is retained in Git history rather than presented as current setup steps.

## Runtime status and ownership

The feature's `api/domain/data/impl`, repository, MVI and rendering remain in the
project. The current application Community host explicitly supplies `leaders =
null`, so the user-facing tab renders Feed and notifications without a Leaders
switch. A persisted former Leaders selection falls back to Feed. Existing module
availability is not evidence that the leaderboard is visible in production.

`LeaderboardEntry` is the public presentation surface. Data reads
`GET /api/v1/leaderboard`; scoring remains a backend contract. The application
supplies the current privacy context, account/session and public-profile routing.
Domain stays platform-independent and DTOs remain in data.

## Contracts to preserve before re-enabling

`LeaderboardViewModel` retains at most one week and one month result for its
lifetime. A period switch keeps the last rows during an uncached read with a
header progress indicator, then replaces the result atomically. A failed new
period must not present the previous period as its successful result.
Switching back and reopening the sheet reuse the period cache.

Explicit feed refresh and privacy-context invalidation clear both periods,
including while a request is already pending. The consumed refresh key lives in
the ViewModel, not a disposable lazy row. Old responses are generation-guarded.
There is no persistent cache, polling, resume-triggered refresh or TTL. Preserve
the existing HTTP no-store behavior and caller-owned profile navigation.

Use the existing design-system segmented control and shared dimensions; historic
pixel-height corrections are not an instruction to restore an old default.
Keep the feature's fake light/dark previews beside its components.

## Source and verification

Inspect [API](api/src/commonMain), [data](data/src/commonMain) and
[presentation](impl/src/commonMain). The current visibility decision is in
`composeApp/.../di/community/CommunityFeatureScreenRenderer.kt`.
Existing data and MVI tests cover period retention/reuse, refresh suppression,
both-period invalidation, privacy changes, failure/retry and late results.

Re-enabling the slot is a product/integration change, not a documentation edit.
It needs affected Community/app tests, platform builds and manual Sandbox checks
for loading, periods, sheet reopening, profile navigation, both themes, large
text and accessibility. Do not certify live scoring or telemetry through fake
data. This documentation review executed no app build, test or device run.
