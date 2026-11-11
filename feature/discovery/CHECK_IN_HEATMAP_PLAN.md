# CoffeePeek public check-in heatmap

## Zoom continuity fix plan (2026-11-11)

- Report: the heatmap disappears when zooming in/out. Confirmed code paths clear
  already loaded points in camera pause, viewport refresh start and refresh failure.
  Native heatmap styling has fixed opacity/radius and no zoom-dependent hiding rule.
- Retain the rendered heatmap during camera-only movement and asynchronous viewport
  refreshes. Reuse the completed prefetched area during zoom-button pauses. Swap the
  points only after a successful response; a successful empty result still clears them.
- Filter/account invalidation, toggling off and late-response cancellation must still
  clear/reject obsolete data. Keep CoffeePeek as the sole source and keep the legend hidden.
- Add JVM regressions for zoom buttons, gesture-like bounds changes, delayed/error
  replies and selection/account isolation. Verify a failing baseline, then compile
  Android, run affected suites and packaged DEX checks, and update the connected Pixel.
  Device startup and manual/native iOS rendering remain separate verification layers.

## Current scope and verification plan (2026-11-11)

- Owner requested restoring CoffeePeek check-in counts as the only heatmap source.
  Remove the bundled third-party dataset, matching code, source-specific tests and
  their documentation. No database import, write or migration is involved.
- Keep the legend and source selector removed. The existing fire button toggles
  the heatmap; coffee zones and heatmap remain mutually exclusive. Map previews,
  marker selection, carousel, navigation, filters and analytics guards stay intact.
- Discovery domain owns point/result contracts; data joins CoffeePeek's existing
  map and search responses; impl owns state, cancellation and the toggle. Native
  Android/iOS bridges only render points with weights.
- No new modules/dependencies. Shared code remains in commonMain; native renderer
  integrations stay in the application boundary. Shared toggle previews are paired.
- Verify pagination, filter/query parity, invalid/zero counts, cache isolation,
  failures/cancellation and ViewModel stale-response guards with JVM tests.
- Assemble directDebug with packaged DEX runtime-class checks. Inspect the APK for
  retired source classes/data, then update the connected Pixel in place using the
  existing user authorization. No uninstall, data clearing or downgrade.
- Manual Sandbox themes/font/window/accessibility review and native iOS build/rendering
  remain separate gates; startup is not a full visual or live-backend acceptance test.

## Data flow

The CoffeePeek map endpoint supplies shop coordinates and slugs. Its search endpoint
supplies public all-time check-in counters. Request individual map points at zoom 22,
fetch search pages of 100 with the same search/filters, and join by exact shop slug.
Deduplicate markers and ignore zero/negative counts and invalid coordinates.
Represent missing counters, residual clusters or truncated map output as partial data.

Cache one completed public counter response for 60 seconds per selection. Do not reuse
personalized visited/favorites selections across accounts. Failed or cancelled page
loads never publish a partial cache. Cancellation remains cancellation through
requestResult. Fixed presentation weight is checkInCount / 15.0 × 2.6 across
viewports. At intensity 1, 15 public check-ins put the center of an isolated
shop near the red end of the palette. Nearby shops can reach red sooner.

## Workspace and verification history

Starting revision: d9be67264 plus pre-existing worktree edits. Branch isolation was
blocked by read-only Git metadata; changes remain uncommitted on the current migration
branch. Other contributors' changes are preserved.

Earlier development failures were retained in local logs: a cancellation test needed
an explicit Unit return for JUnit, central analytics validation needed the new action
names, and a subsequently removed normalization helper used the wrong zip overload.
Earlier success/install records are archived locally; they do not establish acceptance
of this current revision.

Gradle uses six workers, in-process Kotlin compilation, writable temporary caches and
a byte-identical temporary copy of the existing debug signing key. JUnitCore executes
the Gradle-compiled test classes using exported runtime classpaths because sandboxed
Gradle test-worker sockets are unavailable. No IDE diagnostics are claimed.

## Verification before the zoom fix (2026-11-11)

- **206 JVM tests passed**, no failures/retries in this revision: discovery data 36,
  discovery impl 162, composition/native-payload/analytics 8.
- `assembleDirectDebug` passed in 1m 37s, including packaged DEX runtime-class checks.
  Package `com.coffeepeek`, Activity `com.coffeepeek.admin.MainActivity`, version
  `1.1.8.2897` / `2897`; APK debug signature verified.
- APK SHA-256: `d9cc4bc486ed35557cc5b02227e946c6151f2245f8124c633a58dcbca1ea6c0f`.
  Scanned all 36 packaged DEX files: no retired source classes, matching helper,
  source URL or source point-ID prefix remain. Source searches and `git diff --check`
  also pass. No Gradle dependency or application configuration changes were needed.
- Pixel 9 briefly appeared, then disconnected before version/lock/signature comparison.
  A bounded reconnection retry also timed out. **This revision was not installed**;
  no device data or account state was changed.
- Existing AGP/KMP and preview warnings remain. UI previews compile; manual visual
  review and native iOS build/rendering remain unverified.

## Current zoom-fix verification (2026-11-11)

- Source: `codex/migration-business-check-in-qr`, `d9be67264` plus uncommitted
  heatmap work. This fix changes only MapViewModel, its heatmap JVM tests and this plan.
- Before the production change, 4 of 166 impl tests failed: zoom-button retention,
  expanded viewport retention, delayed empty response and failed viewport refresh.
  The original failure log is retained locally as `coffeepeek-heatmap-zoom-red.log`.
- After the change, **210 JVM tests passed**: discovery data 36, discovery impl 166,
  composition/native-payload/analytics 8. The four regressions now pass, alongside
  account/filter clearing and late/cancelled reply protection. No further retry needed.
- `assembleDirectDebug` passed in 2m 55s with packaged DEX runtime-class checks.
  Package `com.coffeepeek`, Activity `com.coffeepeek.admin.MainActivity`, version
  `1.1.8.2897` / `2897`; APK debug signature verified. Existing AGP/KMP/preview warnings
  remain; no dependency, source-set or native-renderer change was required for this fix.
- APK SHA-256: `ea7031b6697891a4c6db19a008674df567e1d93a379bfcba6645528f58c2aadf`.
  All 36 DEX files were scanned; retired dataset/source symbols remain absent.
- Repeated device inventories, including after assembly, returned no connected device.
  The agreed Pixel 9 could not be checked or updated. This APK has **not been installed**;
  device startup, manual zoom/theme/accessibility review and native iOS build/rendering
  remain unverified. No device/account data was changed.

## Verification after syncing develop (2026-11-11)

- Current migration branch and local develop fast-forwarded to `b71e9033f`.
  The heatmap changes were restored from a stash, which remains as a backup.
- The first function-body check reported three violations. After extracting map
  effects and JSON builders and giving the repository factory an explicit body,
  the retry exposed one remaining Android map violation. Extracting the location
  marker update resolved it. The final check reported zero new violations.
- Gradle ran 210 JVM tests with zero failures: discovery data 36, discovery impl
  166, and composition payload and analytics 8.
- `assembleDirectDebug` and packaged DEX runtime-class checks passed.
  Package `com.coffeepeek`, version `1.1.8.2928` / `2928`.
  APK SHA-256: `24343f043f61d8bb832fc4c94600f5ab96cf53d0289230ab8dbcaaa57264c3fc`.
- Installed on an unlocked Pixel 9 on 2026-11-11. The installed debug signature
  matched, and an in-place update from `1.1.8.2897` to `1.1.8.2928` succeeded.
  `MainActivity` reported a cold start and was still in the foreground five
  seconds later. Manual heatmap review and native iOS build/rendering remain
  unverified.

## Calibration for 15 public check-ins (2026-11-11)

- On `b71e9033f` with local heatmap changes, all 167 discovery impl JVM tests
  passed, including the isolated-shop peak-density regression. Function-body
  and packaged DEX runtime-class checks passed.
- `assembleDirectDebug` produced version `1.1.8.2928`, APK SHA-256
  `813ff68d1d4c31fea8fac329a25362867b626d4d39260ff588f7c6c02845dc1c`.
  Installed over the matching debug signature on Pixel 9 on 2026-11-11 after
  two USB disconnects. Cold start passed.
- Read-only review on the live map of Minsk in light theme at city zoom showed
  turquoise, yellow, orange and reddish areas. Cluster markers cover parts of
  the strongest spots. Cluster labels count shops, independent of the public
  check-in weights. The app moved to another screen before zoom could be
  verified. Isolated 15-check-in rendering, dark theme and native iOS remain
  unverified.
