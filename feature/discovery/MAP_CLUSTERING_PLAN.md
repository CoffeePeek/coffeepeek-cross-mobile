# Map clustering parity

## Scope and verification plan

Checkpoint: `codex/migration-map-clusters`, based on
`codex/migration-activity-leaderboard` / `cfeb3d0e` (open PR #197).
Web reference: `coffeepeek-cross-web` commits `e169755` and `937a409`.

Preserve map selection, shop navigation, city/filter queries, nearby carousel,
zone overlays, lifecycle disposal, cancellation and existing analytics guards.
Verify the server response through DTOs, domain, state, application canvas and
native renderers; run unit/contract checks, Android Debug/Sandbox assemblies and
their packaged DEX checks. Manually review only the isolated Sandbox on the
explicitly identified device. Native iOS runtime and live API acceptance remain
separate gates.

## Ownership and changes

- Discovery domain/data/api/impl now carry server clusters and their exact bounds
  and counts. Viewport reads use the actual integer camera zoom, rather than
  forcing all shops at zoom 22. Nearby reads retain their detailed zoom and feed
  the carousel without duplicating grouped shops on the canvas.
- Composition adapts the feature's clusters into the existing `CoffeeMap` port.
  No new modules, dependencies, DI services or public navigation routes.
- Android uses native button targets anchored to map projections. Their cached
  bitmap faces retain the shared design-system colors and existing type icons.
  Positions, alpha and scale interpolate for 380 ms with the web easing curve;
  smallest-containing previous/next groups provide split/merge origins. An
  interrupted animation starts from its current visual state. Exiting targets
  cannot receive clicks/accessibility focus; disposal cancels animations.
- Cluster faces are 34.1 dp/pt in both themes, independent of count, with 12 sp/pt
  extra-bold labels capped visually at `99+`. A count of one uses the existing
  specialty point. Android targets are at least 48 dp; iOS retains its 44 pt
  native annotation targets. Exact counts remain in accessibility descriptions.
- Taps fit bounds with 25% minimum-dimension padding, advance at least one and
  at most two zoom levels (maximum 22), and animate the camera for 350 ms.
  System reduced-motion settings suppress transitions.
- Current-response zones remain count groups when their overlays are hidden or
  in the small preview. Cached overlays from previous viewport responses do not
  become stale groups at detailed zooms.
- iOS on this parent checkpoint still uses the retained legacy Discovery screen.
  Its existing repository read was corrected to consume the server response at
  viewport zoom, including cluster DTOs already present in its contract. The
  compatibility mapper preserves public addresses and filters; this is not a
  new feature implementation in legacy modules. The native Swift renderer is
  shared by Xcode/xtool and uses the same faces and interruptible transitions.
- Sandbox Paged fixtures return zone groups, clusters and detailed shops as
  mutually exclusive representations. Each visible shop is counted once.

Analytics inventory: existing `map_interaction` load/load-failure, nearby,
selection, zones, zoom controls and geolocation reporting stays on the existing
injected port/service with its consent/session guards. No second service or
production test events. The load result count describes represented shops.

## Verification log — 2026-10-26

- First Android pass: 114 executed tests (18 Discovery data, 55 Discovery impl,
  41 map/native-payload/Sandbox contracts), zero failures/errors/skips.
  Direct Debug and Sandbox assemblies, both runtime-class checks and Sandbox
  isolation passed. No failing test retries.
- First device pass: Pixel 9, Android API 37, `directSandbox`, package
  `com.coffeepeek.sandbox`, version `1.1.1.2462-sandbox` / 2462. Installed 2461
  and built 2462 signing certificates matched; updated in place, no uninstall,
  downgrade or data clearing. Source: parent `cfeb3d0e` plus this working diff.
- The small map showed count 38; expansion showed groups 23/15, and a group tap
  advanced the camera and changed grouping. Initial manual checking also exposed
  a zone detail opening underneath a cluster tap. The original observation is
  retained: native overlay targets now explicitly take priority over zone hit
  testing; the final device pass must recheck it.
- Final pass: 142 selected unit/contract results (18 Discovery data, 57 Discovery
  impl, 2 retained iOS mapper, 65 application map/DI/Sandbox), zero failures,
  errors or skips. Unchanged passing suites were reused where Gradle reported
  them up to date. Debug/Sandbox assemblies, packaged runtime-class guards,
  Sandbox isolation and `compileKotlinIosSimulatorArm64` passed. The latter
  compiles Kotlin KLIBs, not the native Swift renderer or an executable iOS app.
- Final Sandbox APK: version 2462, SHA-256
  `9d61da92ac4b8b105338a2a844d802085916ae79c962f37b0b1747c9a99d9545`.
  The same signing certificate was rechecked before its in-place update.
- Final Pixel 9 review: light preview/full map count groups, splitting a group
  of three into individual shop icons, full-screen retention and no zone detail
  opening beneath that tap. The tree exposes native image buttons with exact
  count descriptions and 48 dp targets. Dark point icons and contrast were
  visually checked via Sandbox's own theme control, without system changes.
  Reverse merging, dark count groups and full accessibility/window-size review
  remain open; device use interrupted the remaining manual pass. Incidental
  captures of another application were deleted. Sandbox remains in dark mode.
- Android marker light/dark previews compiled. Dependency/import/DTO containment
  review and `git diff --check` pass; no added Gradle edges or dependency cycles.
  Existing compiler warnings were not expanded into unrelated cleanup.

## Contextual Android location permission — 2026-10-26

Scope: Android Discovery permission timing in `impl`, its public host port and
the application permission adapter. Preserve map movement, nearby reads,
navigation and consent/session-guarded geolocation analytics. Do not prompt on
startup, viewport loading or programmatic shop focus. The first expand, zoom,
location-button or completed map gesture requests permission. Repeated passive
interactions never request again; the Android host persists the one-shot request
so a refusal also survives screen recreation and application restart. Explicit
location clicks recheck permission, allowing a grant made in system settings to
take effect without another permission dialog. A location-button camera request
waits for the permission result; a grant during zoom preserves the camera.

Verification plan: run Discovery implementation unit tests, assemble exact
`directSandbox` and `directDebug` variants and their packaged DEX checks. Manual
Sandbox checks must cover fresh startup, first interaction, refusal/reopening,
gesture delivery to the native map and permission grant/approximate location.
The retained legacy iOS screens are outside this Android change; the iOS actual
only accepts the additional parameter (iOS already suppresses repeat dialogs).
No new Gradle dependencies, data/domain changes, UI tests or real-account writes.

Verification on 2026-10-26, source `520ae529` plus this working diff:

- 65 executed Discovery implementation unit tests, including 28 map tests;
  zero failures/errors/skips and no failing test retries. The new cases cover
  startup/programmatic focus, first interaction, both zoom buttons, refusal,
  duplicate pending location clicks, permission grant and camera preservation.
- `assembleDirectSandbox`, `assembleDirectDebug`, both packaged runtime-class
  checks and Sandbox isolation passed. The final pass includes the saved
  in-flight permission guard for activity recreation. Kotlin compilation for
  `iosSimulatorArm64` passed; this does not build Swift or run an iOS application.
- Final Sandbox: `com.coffeepeek.sandbox`, version `1.1.1.2467-sandbox` / 2467.
  No APK was installed and no device or real account was used. Manual system
  dialog, refusal/restart persistence, approximate location, native gesture
  delivery and light/dark/accessibility checks remain unverified. Screen content
  and its visuals were not changed; only the runtime interaction adapter changed.
- No dependency/source-set edges were added. `git diff --check` passed. Studio
  MCP was unavailable, so IDE inspection/rendering was not performed. Existing
  preview deprecation and expect/actual compiler warnings remain outside scope.

## Remaining gates

Native Swift compilation/signing and iOS runtime, VoiceOver/TalkBack execution,
large font/narrow window coverage, real map tiles/backend acceptance, and native
performance with production-sized data require their own verification. Sandbox
is deliberately offline and its grouping is a synthetic fixture, not evidence
of the server's geographic algorithm. Existing compiler/preview deprecation
warnings are outside this change. Studio MCP/rendering was unavailable; paired
light/dark Android marker previews are compiled, not IDE-rendered.

No automated UI, Compose instrumentation or screenshot tests/hosts were created
or run. Screenshots/recordings stay local outside tracked sources. Pre-existing
release ABI and run-mode documentation edits are excluded from this checkpoint.

## Develop publication — 2026-10-26

At the user's request, the map patch was cherry-picked independently onto
`origin/develop` / `105b8994`. Parent PR #197 remains separate. Stable patch ids
match the original `a54bce61`; the only additional change is this record.
Build source: `a5728e32`, followed by this documentation-only amend.

All 142 selected unit/contract results pass on this base (18 Discovery data,
57 Discovery impl, 2 retained-iOS mapper, 65 application contracts), zero
failures/errors/skips. Direct Debug/Sandbox assemblies, both packaged runtime
class checks and Sandbox isolation pass. Native iOS/device acceptance gates
above remain open; no further device installation or UI checks were performed.
The primary checkout's two pre-existing local edits were not included.


## Cluster density and top inset follow-up — 2026-10-26

User report: city groups are too sparse and expanded Android map has a blank
status-bar band. Scope: both retained/migrated map HTTP adapters, the retained
Compose root and its Main entry callback, request-contract tests. No backend,
marker animation, native map renderer, theme or account/data changes.

Request one server zoom level above the camera integer bucket (clamped 0–22):
server grid cells are half the previous width, zones unfold one level earlier.
Camera position and viewport cache buckets stay unchanged. Preserve filters,
server truncation, bounds, cluster transitions and the nearby carousel. The root
receives the existing expanded-map visibility state; only the expanded map skips
the root top inset. Its search/close controls retain their own status-bar inset.

Verification plan: request zoom boundary/filter contracts; existing map/view-model,
choreography and DI unit contracts; Direct Debug/Sandbox assembly and packaged
runtime class guards. Manual Sandbox review: expand/collapse, top coverage and
controls, denser groups, light/dark, Back and tab/detail return. Native iOS and
live-backend geographic acceptance remain separate gates. Device is not yet
verified for this follow-up; no UI or instrumentation automation is permitted.


Follow-up results: source `4aea0e1c` (only this verification record is added
subsequently), Direct Sandbox `1.1.1.2457-sandbox`, Direct Debug `1.1.1.2457`.
All 143 executed unit/contract checks pass: Discovery data 19, Discovery impl 57,
retained mapper 2, application 65; zero failures/errors/skips and no retries.
Both Android assemblies, both packaged DEX runtime-class checks, Sandbox isolation
and Kotlin iOS simulator KLIB compilation pass (`BUILD SUCCESSFUL in 3m 8s`).
No warnings were reported against the changed Kotlin files; existing warnings
elsewhere remain outside scope. Studio MCP was unavailable.

Manual follow-up review was not run: ADB returned no connected devices; the user
was asked for a device handoff. No APK was installed or launched, and no phone,
account or application data was changed. The previous phase's Pixel 9 checks
are historical and do not certify this fix. Top-edge/control placement, map
expand/collapse animation, light/dark, font/window sizes, Back/tab/detail return
and live backend density remain unverified. The one-level policy also makes
individual-shop responses (and their existing server truncation cap) apply one
camera bucket earlier; truncation signaling is preserved. Native Swift/iOS UI
runtime is not certified by KLIB compilation. No automated UI tests were run.


## Pixel 9 follow-up review — 2026-10-26

Code source `771913fd`. Device: Pixel 9, API 37, 1080 x 2424, density 420,
font scale 1.0. Explicit environment: `directSandbox`, package
`com.coffeepeek.sandbox`, `SandboxActivity`, synthetic Discovery `Paged` fixture.
The existing installed APK hash matched the previously recorded Sandbox build;
its signing certificate matched the new APK. An external local Gradle init
script set only Sandbox output metadata to version code 2463 / version name
`1.1.1.2463-map-density-sandbox` so the update advanced installed version 2462.
No repository version setting changed. APK SHA-256:
`57ed296807761f32b7ee7393301175f5878ac13b1f77eb88c48448128e67d466`.
Sandbox assembly, packaged DEX class guard and isolation pass in 2m 8s. In-place
`install -r` succeeded; installed version/package were verified. No downgrade,
uninstall, data clearing, ordinary Debug install or live backend access occurred.

The first launch attempt stopped at the lock-state guard. After the phone was
unlocked, a fresh Sandbox launch selected the Paged fixture. Manual observations:
expanded native map bounds are [0,0]–[1080,2424]; search and close remain below
the status bar, with no separate top band. Dark and light states were inspected.
The overview shows two groups of 3 alongside the synthetic zone count 32;
tapping a group exposes separate typed shop controls without opening a zone
sheet. Zoom-out regroups the shops, then merges the two groups into 6; zoom-in
splits 6 back into two groups of 3. Count circles and shop icons remain legible
in both themes. These are manual state observations, not timing/performance or
real-server density acceptance, and the fixture includes coincident shop points.

The attempted post-Back check stopped on the foreground guard after the UI dump;
the dump was not pulled, no subsequent tab tap was sent, and temporary device
files were removed. A later attempt also stopped before UI interaction because
Sandbox was not foreground. The user was asked to hand back Sandbox for the
remaining navigation check. The original interruptions are retained here;
Back/tab return is not certified yet. Sandbox was last reviewed in light theme.
No automated UI, Compose instrumentation or screenshot test infrastructure was
created or run; review images/dumps stay local outside tracked sources. Large
font/window, screen reader execution, native iOS and live backend gates remain.
