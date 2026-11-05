# Light timber shelves — 2026-11-05

Base: freshly fetched `origin/develop` `43c9969ce`; working branch
`codex/migration-shelf-light-timber`.

Replace the shared shelf ledge's frosted glass and silver fasteners with a
solid light oak beam: shallow rounded corners, a softly lit upper bevel and
subtle horizontal wood grain. Keep the same light wood in both app themes.
The owning layer is `feature/shelf/impl` shared Compose presentation; existing
design-system cards and frosted surfaces do not represent this decorative
material. No new module, dependency or public contract is needed.

Preserve the 40 dp ledge height, 8 dp bag overlap, row spacing, bag interactions,
landing animations and accessibility actions. The existing `ShelfLedge` is
also used by profile previews and empty/guest shelves, so all placements share
the new material. Remove the grid's glass backdrop capture when it becomes
unused. Keep existing colocated light/dark previews.

The ledge and grid own no analytics calls; actions continue through the existing
`ShelfAction` callback. No ViewModel, analytics port, data or platform binding
changes are required.

Verification plan: execute existing shelf presentation/drag tests, compile
shared shelf UI for Android and iOS Arm64, and assemble `directSandbox`,
including packaged DEX class and Sandbox isolation checks. Do not add UI or
screenshot tests. IDE preview rendering is unavailable in this session. No
device has been agreed, so no app installation or launch is planned; manual
light/dark, narrow/large text, empty/guest/profile and drag review remain gates.

## First flat beam pass — verification

Verified on 2026-11-05 from base `43c9969ce` plus this scoped patch:

- `:feature:shelf:impl:testDebugUnitTest` executed 85 tests in 11 suites,
  including all 8 drag-state tests. Zero failures, errors, skips or retries.
- Android shelf implementation and `:feature:shelf:impl:compileKotlinIosArm64`
  compiled successfully; the updated iOS Arm64 KLIB was generated. This is
  shared Kotlin compilation, not a native Swift/iOS application build.
- `:composeApp:assembleDirectSandbox` passed, including
  `verifyDirectSandboxRuntimeClasses` and `verifyDirectSandboxIsolation`.
  APK: `com.coffeepeek.sandbox`, `1.1.7.2721-sandbox` / version code 2721.
  The combined build/test invocation completed in 4m 15s with six workers.
- `git diff --check` passed. Dependencies, source sets and domain/data contracts
  are unchanged. Grain paths and brushes are cached at the ledge's actual size.
- Existing Preview annotation deprecations remain in the changed files;
  existing Gradle/KMP configuration warnings also remain. No IDE diagnostics
  or preview rendering ran, and no automated UI/screenshot tests were added.

No device was selected, installed or launched. Manual light/dark, font/window
sizes, accessibility, profile/empty/guest placements, back/cancel and drag/landing
visual review remain unverified. Native iOS application build and visual review
also remain open. No live backend or account data was accessed for verification.

## Reference refinement — 2026-11-05

The user supplied a coffee shelf reference with three upright bags on a thin
oak board, a visible trapezoidal upper plane, a darker front edge and a title
beside an Add action. Replace the first flat 40 dp beam with an 18 dp board:
8 dp upper depth plus 10 dp front thickness. Keep the 8 dp placement offset,
draw boards behind the bags, and align bag bottoms with the front of the top
plane. Derive row and profile preview heights from the shared board dimensions.
Keep the same three-column order and all existing drag/accessibility actions.

Use the existing design-system text button for the shelf title's Add action,
dispatching the existing `ShelfAction.AddBag` for signed-in owners. Public
profile previews keep their existing permissions and callbacks. Russian and
Belarusian are the actual resource sets; add the button label to both.

Add one feature-owned local oak texture, generated with the built-in imagegen
tool, and map it onto separately shaded top/front planes. The bags remain
dynamic content and are not part of the image asset. No new dependency, module,
navigation route, platform bridge or analytics implementation is required.

Verification plan: rerun the existing shelf unit/contract regressions, compile
shared Android/iOS Arm64 presentation, assemble `directSandbox` and verify its
DEX classes/isolation. Check resource parity and colocated light/dark previews.
IDE rendering and manual device gates remain unavailable/unverified; do not add
UI/screenshot test infrastructure or launch an unselected app/device.

Texture asset: `impl/src/commonMain/composeResources/drawable/shelf_oak_texture.png`.
Generation prompt: photorealistic clean light natural oak surface, straight-on
orthographic texture, horizontal grain, warm pale honey-beige unvarnished matte
wood, understated fibers and pores, edge-to-edge full-bleed single surface under
even diffuse light. No objects, board edges, perspective, seams, background,
shadows, text or logos. Geometry and lighting are drawn by the app. Built-in
imagegen was used; the selected original is copied into feature resources.

Refinement verified on 2026-11-05 from `43c9969ce` plus the current patch:

- `:feature:shelf:impl:testDebugUnitTest`: 85 tests / 11 suites executed again,
  including 8 drag tests; zero failures, errors, skips or retries.
- Shared shelf UI compiled for Android and iOS Arm64, including the local
  image resource and the new header's paired light/dark previews.
- `:composeApp:assembleDirectSandbox`, packaged DEX runtime-class verification
  and Sandbox isolation passed in the same invocation (1m 1s, six workers).
  Build identity remains `com.coffeepeek.sandbox`, `1.1.7.2721-sandbox` / 2721;
  it is the rebuilt reference refinement, not the earlier flat-beam APK.
- The actual APK contains the feature-local oak resource at
  `assets/composeResources/com.coffeepeek.feature.shelf.impl.resources/drawable/shelf_oak_texture.png`
  (2,270,826 bytes; source raster 2172 × 724). It requires no image/network
  request at runtime. The generated original was preserved.
- Russian/Belarusian string-key parity and `git diff --check` passed. No
  Gradle/dependency, data/domain, navigation or analytics changes were needed.
  Existing Preview annotation deprecations remain with the current tooling.

No IDE preview or manual device review ran. Light/dark appearance, large text,
narrow windows, profile/guest/empty placement, Add/back/cancel and drag/landing
visual acceptance remain open, as does the native iOS application build. No
device was selected or app installed; no account/backend writes were performed.

## Current header follow-up — 2026-11-05

The subsequent user request supersedes the Add header action: it has been
removed, and the existing archive icon is now aligned with the Coffee shelf
title for signed-in users. The summary and drag hint remain on their own row.
The board material and geometry are unchanged. Integration with the profile
tabs/menu and the latest 266-test Sandbox verification are recorded in
`../profile/PROFILE_TABS_PLAN.md`.

## Full profile shelf before publication — 2026-11-05

The user additionally requested every bag in the profile shelf, not just the
first three. The repository maps every item returned by the existing public
shelf endpoint and the ViewModel retains them; the truncation is the fixed
single three-column UI row. Replace that row with all item chunks in a naturally
sized column, keeping three bags per wooden board and stable item identities.
Retain public/owner visibility and the existing photo/catalogue click ports.
Add paired seven-bag previews (three rows, including an incomplete last row).

Verification plan: execute the existing Shelf presentation tests, compile its
shared UI for Android/iOS Arm64, assemble `directSandbox` and require runtime
DEX/isolation checks. No new UI/screenshot tests or live account operations.
Manual scrolling and multirow visual review on a device remain open. After
verification, publish the approved complete patch to both develop and main;
both fresh remote branches were at `43c9969ce` before this final correction.

Full-shelf verification passed on 2026-11-05: all 85 Shelf implementation tests
in 11 suites executed again, with zero failures, errors, skips or retries.
Shared Android/iOS Arm64 compilation, `assembleDirectSandbox`, packaged DEX
runtime-class verification and Sandbox isolation passed in 13s with six workers.
The seven-bag paired previews compile but were not rendered. No device was
selected or app launched. Existing Preview/BackHandler deprecations remain.

Final local Sandbox artifact: `com.coffeepeek.sandbox`, `1.1.7.2721-sandbox` /
2721, SHA-256
`13d8a599af9f3e70848f9269a3d8c58e037d2b98976229bac03fd6d078d7c969`.
The earlier profile-integration artifact hash is historical and does not include
this final multirow shelf correction.
