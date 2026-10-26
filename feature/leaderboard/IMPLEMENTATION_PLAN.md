# Activity leaderboard

## Period-switch flicker and repeated reads — 2026-10-26

Follow-up to the review of `7f318348`: the previous geometry fix did not prevent
the preview from replacing all rows with a loader. Selecting an uncached period
also cleared the full list; `ON_RESUME` refreshed whenever the lazy item returned
to composition. Remove that automatic refresh and keep the last rows while the
first request for a different period runs, with a small header loading indicator.
Replace results atomically after success. On failure, do not present the previous
period as the selected period's result; keep same-period refresh failures retryable.

Keep at most one week and one month result in this lifecycle-owned ViewModel.
Switching back and reopening the sheet reuse them. Explicit feed refresh and every
privacy-context invalidation clear both, without changing HTTP no-store, public
profile navigation, cancellation or server scoring. Results are refreshed by the
feed refresh gesture, not polling/resume; there is no persistent cache or TTL.

The user then requested a roughly 10% lower period control. Use a 44dp minimum
segment in this feature instead of 48dp: including the existing 2dp container
padding on each side, the capsule becomes 48dp rather than 52dp (about 8% lower).
Text size remains unchanged and the minimum still allows larger text to grow.
Expose that minimum on the existing shared primitive with its 48dp default;
other segmented-control consumers retain their current size. Add compact paired
previews beside the shared component and retain the feature's paired previews.

Verification plan: delayed switch retention, repeated switches/sheet reopen,
duplicate refresh suppression, refresh invalidation of both periods, privacy
invalidation of both periods, failure/retry and non-cooperative late-response
regressions. Compile paired updating light/dark previews and shared iOS KLIB;
assemble Debug/Sandbox with DEX/isolation checks. Manually review the slow local
Sandbox scenario on the agreed Pixel 9, if connected and unlocked. No automated
UI/instrumented/screenshot tests or live backend writes.

### Verification results

Verified on 2026-10-26, `7f318348` plus this follow-up's source changes:

- Cache/retention build: **BUILD SUCCESSFUL**, 3m09s, 2025 tasks / 140 executed.
  Final compact-control build: **BUILD SUCCESSFUL**, 1m29s, 2011 tasks / 150
  executed. Both used `--max-workers=6`, with no concurrent Gradle runs.
  Debug/Sandbox assembly, packaged DEX checks, Sandbox isolation and shared iOS
  Simulator Arm64 KLIB compilation passed; no failed builds/tests or retries.
- **60 selected unit/contract results**, zero failures/errors/skips: data 7,
  leaderboard MVI 15, Community 33, app privacy context 2 and Sandbox HTTP 3.
  Valid up-to-date results were reused. The new regressions cover retention,
  both-period reuse, sheet reopen, duplicate refresh, both-period refresh/privacy
  invalidation, failed uncached period/retry and a late non-cooperative response
  after returning to a cached period. A 60-second virtual idle adds no requests.
- Updated only `directSandbox` / `com.coffeepeek.sandbox`, version
  `1.1.1.2461-sandbox` / code 2461, in place on the agreed Pixel 9 / API 37.
  Verified package/entry/signature/version before installation. Final APK SHA-256:
  `017c27731f1929a11969daffb3bb942560d78f208d80438c07cfe120456efc67`.
  No downgrade, data clearing, uninstall, security/settings changes or live writes.
- Manual final light-theme review: compact periods render in the feed and sheet,
  retaining text size. With the local Slow scenario (900ms), the first month
  request shows the previous 100-point rows plus a small header loader; the
  completed response shows 200 points. No blank-list replacement was observed.
  Local screenshots were inspected and remain outside tracked sources. Restored
  the local scenario to Ready afterwards and left the feed showing loaded rows.
- Captured attempts interrupted by system UI or moved to the explanatory footer
  were excluded as period-switch evidence. The repeated-cache/request-count
  assertions above are unit results, not a claim of device network tracing.
- `git diff --check` and caller/import review pass. Other segmented-control
  consumers retain their default height. Updated light/dark loading and compact
  previews compile; they were not rendered. Existing Preview/AGP/Native warnings
  remain. Studio MCP is unavailable; no IDE inspection is claimed.
- Final dark theme, large font/narrow-window, TalkBack, native iOS runtime and live
  API acceptance remain unverified. Data freshness now follows explicit feed
  refresh/context invalidation; there is no periodic or resume-triggered refresh.
  No automated UI/instrumented/screenshot tests were added or run. Original local
  release ABI/run-mode edits remain excluded from this feature's changes.

## Design corrections — 2026-10-26

Follow-up scope from the user's Pixel 9 review of `e6e220b0` / Sandbox 2459:
reserve the three-row preview's height during week/month loading to prevent feed
reflow; collapse that preview as the owning feed moves away from its top; remove
Close/Refresh/calendar text from the sheet header; show action counts as
additions/edits/check-ins (`4/3/6`) directly below the points, right-aligned, with a readable accessibility description; show
the first 30 leaders in one request with no load-more control; explain the counts,
weights, eligibility and period rules at the bottom.

The existing public own-rank response and server competition ranks remain intact.
Scope reaches leaderboard api/domain/data/impl, both Community integrations and
Sandbox fixtures/contracts. Remove obsolete leaderboard paging actions and
request options rather than keep an unused pager. Preserve immediate privacy
invalidation, cancellation, profile navigation and existing feed analytics.

Verification plan: updated API/top-30/MVI/context/Sandbox regressions; Android
Debug/Sandbox APK and DEX/isolation checks; shared iOS KLIB compilation. Use
colocated collapsed/loading/light/dark previews and manual Sandbox review only,
with no automated UI or screenshot suites. The phone disconnected during the
first capture session; reconnect/unlock is needed for a new device run.

### Follow-up verification results

Verified on 2026-10-26, source `e6e220b0` plus this follow-up's source changes:

- Both combined Gradle runs passed with `--max-workers=6`, sequentially: initial
  six corrections in 3m33s (2025 tasks, 182 executed), then the user's requested
  right-aligned counters in 1m19s (2011 tasks, 129 executed). Debug/Sandbox APKs,
  both packaged DEX guards, Sandbox isolation and iOS Simulator Arm64 KLIB
  compilation passed. No failed tests or failing-test retries in this follow-up.
- **55 selected unit/contract results**, zero failures/errors/skips: leaderboard
  data 7, leaderboard MVI 10, Community edit/timeline 33, app context 2 and Sandbox
  HTTP 3. Up-to-date valid results were reused. Removed only this feature's now
  obsolete paging cases; top-30 and refresh/retry regressions replace them.
- Installed only `directSandbox`, `com.coffeepeek.sandbox`, version
  `1.1.1.2460-sandbox` / code 2460 on the agreed Pixel 9, Android API 37.
  In-place updates preserved data and the existing signing certificate; no
  downgrade, uninstall, data clearing or phone security/settings changes.
  Final APK SHA-256:
  `7ed51376f47c4f15de0a458be07e78d39fd98bf245e9e63d20187f1f6790f797`.
- Manual dark-theme Sandbox review: week/month values change (100/200 for the
  first synthetic row), while the first post stays at the same vertical position;
  an 80px upward gesture collapses the preview to its title/All row; returning to
  the feed top restores the preview. Back dismisses the sheet. Its header has
  only the title and period control; counters are below/right-aligned with points.
  The list ends at participant 30 despite 40 synthetic backend participants.
  The explanation below is readable and includes counts, weights and eligibility.
- Local screenshots/UI dumps are outside tracked sources. Final dark sheet,
  participant-30 boundary, explanatory footer and collapsed-feed screenshots were
  visually inspected; captures interrupted by personal notification banners were
  discarded. No screenshots or account data are published in the PR.
- The Pixel 9 disconnected again before the light-theme operation could execute.
  Light theme, large font/narrow-window, TalkBack, slow-response animation and
  native iOS runtime checks remain open. Paired previews compile but were not
  rendered. Sandbox checks do not certify live API, analytics, GPS or persistence.
  No automated UI/instrumented/screenshot tests or live writes were run.
- `git diff --check` and caller/dependency review pass. The original local release
  ABI packaging and run-mode documentation edits remain outside this change.

## Initial scope and verification plan — 2026-10-26

Baseline: `8b8d88c9`, migration branch synchronized with freshly fetched
`origin/develop` at `105b8994`. Backend contract: CoffeePeek/coffeepeek-backend#362,
`4891229540e1697568e68e1fbe99585a2690af10`, `docs/api/activity-leaderboard.md`.
The backend PR is open; its endpoint must be deployed before live acceptance.

Add a shared leaderboard card above Community check-ins, week/month selection,
the first three server rows, and a full-list sheet with paging, own rank and
activity counts. Preserve server competition ranks and points, public-address-only
profile navigation, existing feed analytics, session/block privacy and cancellation.
No local scoring, persistence, historical backfill, check-in GPS collection,
moderation controls, prizes or notification campaign.

New modules enforce the existing api/domain/data/impl boundaries; packages inside
composeApp would allow UI to import DTOs and infrastructure. All four contain
production code and use commonMain on Android and iOS:

| Module | Responsibility / dependencies | Consumers / hidden implementation |
|---|---|---|
| api | Composable entry and image slot; Compose only | App composition; no models/DTOs/VMs |
| domain | Period, rows/pages and repository; Kotlin only | data/impl and composition; no infrastructure |
| data | GET transport, wire DTOs, mapping; domain/core-network/Ktor/serialization | Composition factory only; DTOs/implementation internal |
| impl | Typed MVI, card, rows, full sheet and resources; api/domain/core-presentation/design-system | Composition factory only; UI/VM internal |

Composition supplies the configured authenticated HTTP client, existing image
loader and a context invalidation flow derived from session and author visibility.
Use fresh develop's `core/network` envelope and `core/session` read-only contract.
The full list uses the existing sheet primitive; root navigation remains app-owned.
Existing feed events and consent/session guards are preserved; opening the sheet
uses the application's analytics overlay ownership. No new analytics service.

Changed integration points: module registry, app dependencies/root Koin,
Android's migrated CommunityHost above-posts slot, retained iOS Community list
header/refresh, Sandbox HTTP fixtures and in-memory contracts. The slot is supplied
by application composition; Community does not import leaderboard implementation.
Use the existing shared card, segmented control, loader, buttons and sheet;
add configurable selected colors to the segmented control with current defaults.
Feature resources use Russian defaults, matching the current app and adjacent
feature modules; no additional locale is claimed.

Checks: data HTTP/envelope/mapping/pagination/cancellation contracts; VM period
races, duplicate paging, error/retry and context invalidation tests; Sandbox
contract; Android directDebug assembly including packaged DEX guards and
directSandbox assembly. Shared iOS KLIB compilation when supported.
No automated UI/instrumentation/screenshot tests. Manual light/dark, large font,
accessibility, modal/back placement and physical-device checks remain gates until
performed on an explicitly selected Sandbox device. Live API rollout and native
iOS executable/device acceptance are separate gates.

## Initial implementation results (before device review)

The first build invocation was stopped before tests/assembly to integrate fresh
develop (the earlier local remote-tracking ref was stale). It is not test evidence.

Verified on 2026-10-26, source `8b8d88c9` plus the implementation in this checkpoint:

- Final combined Gradle invocation: **BUILD SUCCESSFUL**, 1m30s, `--max-workers=6`;
  2025 tasks, 129 executed and 1896 up-to-date. No concurrent Gradle runs.
- **58 selected unit/contract results**, zero failures/errors/skips: data 8,
  leaderboard MVI 12, Community edit 14 / timeline 19, app privacy-context 2 and
  production-repository/Sandbox HTTP contracts 3. Valid preceding task results
  were reused where up-to-date; this is not 58 fresh executions in the final run.
  No unit test failed or needed a failing-test retry.
- `assembleDirectDebug` and `assembleDirectSandbox` pass, including both packaged
  DEX checks and Sandbox isolation. The added leaderboard factories/assembly are
  explicitly included in those runtime-class guards.
- APK metadata: `1.1.1.2459` / code 2459, `directDebug` / `com.coffeepeek`;
  `1.1.1.2459-sandbox`, `directSandbox` / `com.coffeepeek.sandbox`.
  APKs were assembled only; neither was installed or launched.
- `compileKotlinIosSimulatorArm64` passes for the application and its shared
  leaderboard dependencies. This is KLIB compilation, not native executable
  linking or device acceptance.
- Retained build failures before the successful run: the fresh shared loader
  required an accessibility description; a Sandbox JSON builder used an
  unsupported `putAll`; the app initially declared the shared feature only in
  androidMain. All three were fixed before the final pass. Local original logs
  remain outside tracked sources.
- Dependency/import review and `git diff --check` pass. Domain is Kotlin-only;
  UI contains no DTOs, HTTP, data implementations or Koin. No unused use cases,
  persistence or local scoring were introduced. Shared segmented-control defaults
  and develop's scrollable mode remain intact.
- Paired light/dark previews cover card, periods, rows, loading/error states,
  screen content and the full list; rows also have large-font previews. Previews
  were compiled, not rendered/visually certified. Existing deprecation/AGP/Native
  warnings remain. Studio MCP was not available; no IDE inspection is claimed.

Device: `adb devices -l` reported no connected device. No automated UI,
instrumentation or screenshot tests were added or run. Manual light/dark,
large-font/narrow-window, accessibility, avatar failure, sheet placement/back and
native iOS checks remain open. No live account, analytics event or backend write
was used. Live read acceptance waits for backend #362 deployment. GPS capture for
new check-in credit and administrative eligibility controls are outside this
leaderboard-read feature.

Pre-existing local Android release-ABI packaging/run-mode documentation edits were
preserved and excluded from this feature's commit; the packaging block only
selects directRelease and does not change the verified Debug/Sandbox variants.
