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
