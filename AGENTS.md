# CoffeePeek — Agent Rules

These rules apply to every change in this repository. `ARCHITECTURE.md` is the
detailed source of truth; this file is the operating checklist.

## Current state and migration

The current Gradle layout is transitional:

```text
composeApp/                 current Android/KMP application composition module
modules/domain/             legacy shared models and repository contracts
modules/network/            legacy HTTP infrastructure and feature API code
modules/data/               legacy repository implementations
modules/room/               legacy Room infrastructure and persistence
core/                      prepared independent infrastructure/design-system
feature/                   favorites, shop-report, shop api/domain/data/impl;
                           Android feature slices integrated
iosApp/                     native iOS application boundary, when present
```

Do not treat the complete target `app/`, `core/`, `feature/` layout as already
implemented. Before a structural change, inspect `settings.gradle.kts`, relevant
module build files, source sets, DI, navigation, and existing abstractions.

Migrate incrementally. Do not rename, split, or replace all modules in one
operation; do not create a second application module merely because a target
diagram names one. Keep legacy code temporarily when needed, but do not add new
code for an already migrated feature back to legacy modules.

## Ownership and boundaries

Organize new code by business feature, not by a global technical layer.

```text
feature/<name>/
├── api/       intentionally public contracts only
├── domain/    business rules, domain models, repository interfaces
├── data/      backend/local implementations, DTOs, entities, DAOs, mappers
└── impl/      screen entry point and ui/ presentation implementation
```

The agreed target for migrated business features is separate api/domain/data/impl
Gradle boundaries under singular feature/. Create these incrementally for the
feature being migrated, not empty modules for every future screen. UI packages
live inside impl; composition assembles data via narrow factories. Favorites
Koin assembly and its temporary legacy bridge live in composeApp/androidMain.
Put root Koin assembly and platform/legacy bridges in composeApp, grouped
by feature; only extract a separate integration module for a demonstrated need.
This does not require every core infrastructure module to have a domain/data
pair. See feature/README.md for the migration sequence and dependency graph.

Current main uses server-backed favorites (shops, roasters and drinks). Android
composition adapts that same server source into the migrated feature. Never
restore the retired local writer, upload old local rows automatically, or interpret
logout/clear-cache as deleting server favorites. Older local-foundation tests are
not evidence of active production wiring. Check feature/MAIN_SYNC_AUDIT.md before
using older roadmap/product assumptions.

- `api` is public by necessity, not by default. Keep routes, entry points, and
  minimal cross-feature contracts there.
- `domain` must not depend on Android, Compose, SwiftUI, Ktor, Room, SQL, HTTP,
  Koin, or navigation.
- `data` implements domain contracts and contains feature-specific Ktor APIs,
  DTOs, Room entities/DAOs, data sources, and mappers. These must not escape
  data.
- `ui` renders state, handles user events, and depends on domain abstractions —
  never on Ktor, DAOs, DTOs, entities, or repository implementations.
- `core` owns genuinely shared infrastructure only. It never depends on a
  feature. Never introduce `core/common`, `core/base`, `core/utils`, or another
  dumping ground.
- The application composition module owns application startup, root DI, root
  navigation, and platform configuration; it does not own feature business logic.

Features may depend on another feature only through its `api`. Application
composition supplies adapters between independent domain contracts, including
shop favorites; business and UI code never import another feature's domain/data/impl/di.
Domain/data use constructor/manual DI. Koin modules are Kotlin definitions, not
automatically separate Gradle modules. Application composition assembles them;
no DI wiring belongs in domain/data or generic core DI. Keep feature UI independent
of data/legacy implementations. Do not introduce Dagger/Hilt into this KMP project
without a separate, justified architecture decision.
The dependency graph must remain acyclic. DI and navigation must not bypass these
boundaries.

Within a migrated feature, group code by its real responsibility:

```text
domain/{model,repository,usecase}/
data/{backend,local,mapper,repository}/
impl/<Feature>ApiImpl.kt
impl/navigation/
impl/ui/<Feature>ViewModel.kt
impl/ui/compose/<Feature>Screen.kt
impl/ui/compose/component/
impl/ui/compose/model/          state, actions, one-off events
```

Use `backend` only for actual remote APIs/DTOs, `local` for persistence, and
`mapper`/`formatter` only for real conversions. `ui/data` is an exceptional home
for strictly presentation-specific preparation, never a replacement for feature
domain use cases or repository implementations. Do not create empty packages.
The feature API implementation adapts the public entry contract; feature
navigation registration may live in `impl/navigation`, while the application
still owns its root back stack and cross-feature routing.

## Presentation and resources

- Prefix reusable design-system components, supporting UI types, tokens and their
  Kotlin source files with `CoffeePeek` (for example `CoffeePeekButton.kt`,
  `CoffeePeekTextField`, `CoffeePeekIcons`, `CoffeePeekDimens`). Ordinary functions
  and Modifier extensions use lower camel case (`coffeePeekLiquidGlass`);
  composition locals use `LocalCoffeePeek…`. Do not add new `App…` or `Cp…`
  aliases. Feature components keep their feature-specific names; legacy names
  remain until their consumers are migrated. Resource and font-license names
  are not Kotlin component names and must retain their provenance.
- Give each independent screen or component its own descriptively named Kotlin
  file. Keep small private implementation helpers with their owner; do not create
  a file solely to collect unrelated components or previews.
- A screen's `NameScreen` is the runtime adapter: obtain its lifecycle-owned
  ViewModel/dependencies at the feature entry or composition boundary, collect
  state, and pass state/actions to a stateless `NameScreenContent`. Preview and
  test `NameScreenContent` with fake state, no Koin, network, or database.
- For MVI screens, model real user intents as `Action`, durable rendering data as
  immutable `State`, and non-replayable effects such as navigation as `Event`.
  Send actions into one ViewModel entry point. The screen maps events to caller
  callbacks; it does not own the app's navigation implementation. New MVI
  ViewModels inherit `core/presentation`'s typed `MviViewModel`; the old
  application `BaseViewModel` is not their parent. Do not invent unused event
  classes: use `Nothing` when appropriate. The base launches each submitted
  action in its lifecycle scope; features implement suspending
  `handleActionInternal`. Independent actions may overlap, so features guard
  duplicate or conflicting work. `Result` failures and cancellation policy
  remain explicit in each feature.
- Place each component's preview beside its component in the same source file.
  For shared `commonMain` Compose UI, use paired light/dark multiplatform
  previews in that file; for Android-only UI, use `PreviewLightDark` there.
  Do not create preview-only files. Keep preview tooling platform-appropriate.
- Own user-facing text, accessibility descriptions, and images in explicit
  resources. Audit actual supported locales before claiming a translation exists;
  keep translations aligned and avoid new hard-coded UI strings. Put reusable
  visual assets/tokens in design-system only when truly shared; feature-specific
  resources stay with their feature.
- Keep the existing application `BaseViewModel` transitional. The new typed
  `MviViewModel` owns a private state flow and buffered event channel but not
  navigation, Koin, custom scopes or global error/loading policy. Its
  `updateState` delegates to the atomic `kotlinx.coroutines.flow.update`; do
  not add an unbounded action queue or swallow action failures in the base.
  Preserve `Result` failures and coroutine cancellation semantics.

## KMP and platforms

Prefer `commonMain` for platform-independent domain, data, shared presentation
logic, ViewModels, UI state, and Compose Multiplatform UI where applicable.

- `androidMain`: Android APIs, Android-only integrations, and Android-only UI.
- `iosMain`: Kotlin/Native implementations of actual platform abstractions.
- `iosApp`: native Swift/SwiftUI application, lifecycle, native navigation, and
  integration with public KMP APIs.

Do not put Android APIs, SwiftUI, or platform-specific business logic in
`commonMain`. Do not use `expect`/`actual` merely to compensate for a poor
module boundary.

## Before coding

- Continue the branch stack from the current migration tip, not main. Each new
  PR targets the previous open checkpoint branch and describes only the changes
  since that branch. Work only in migration branches. Once the current business
  feature checkpoint is complete, start the next feature in a new child branch
  and PR; do not keep adding new features to a completed checkpoint. Supporting
  slices required to finish the same feature stay together (including Home reads
  required by combined history). Use roughly 100–150 changed files as a review
  guide, not one PR per small task or a target to fill with unrelated work.
  A genuine review/risk boundary may justify an earlier PR; an indivisible change
  may exceed the guide with an explanation. Never mix unrelated work to fill it.
  Parent-scoped fixes may stay in that parent's PR; update its descendants after
  publishing them. Before merging a child, retarget it to main after its parent
  has reached main; recheck its diff and tests. Do not merge children into an
  already merged parent and assume they reached main.
- Branch isolation does not prove application correctness: every integration
  slice must compile the Android app and test the affected behaviour.
- Before migrating a screen, trace its actual entry points in fresh main and the
  running app. Distinguish full-screen destinations from profile previews and
  modal variants of similar content. A new modal does not prove an old route is
  retired; a passing full-screen test does not certify the modal's migration.
- Current favorites completion is Android-first. Do not change the iOS screen,
  binding or writer, or remove shared legacy paths still used by iOS. A root
  Navigation 3 migration is an application-wide decision, not a prerequisite
  for finishing the Android favorites feature.

1. Identify the business feature and the owning layer.
2. Search for a reusable existing implementation before creating a parallel one.
3. Inspect relevant Gradle dependencies and source sets.
4. For a new Gradle module, document: responsibility, allowed dependencies,
   allowed consumers, hidden implementation, and why packages are insufficient.
5. For a major migration, document current location, target location, public API,
   migration steps, dependencies, and risks.

Create a use case only for meaningful business behaviour, coordination, or rules;
do not wrap one repository call without value. Prefer composition over generic
`Base*`, `Manager`, `Helper`, or `Utils` abstractions unless an existing base
demonstrably removes a shared, correct pattern. Prefer `internal` for
implementation details.

## IDE and testing workflow

- Define a short verification plan before changing code: the behavior to preserve,
  affected layers, relevant tests and remaining device/release gates. State
  assumptions; keep each changed line tied to the task. Do not mix speculative
  abstractions, adjacent cleanup or product redesign into a migration.
- Check `<module-root>/AGENTS.md` when first entering a module, including included
  builds, and read it if present. Load module rules lazily, not repository-wide.
- Prefer the configured Android Studio `studio` MCP for project navigation,
  source inspection, semantic refactoring and file diagnostics when available.
  Discover its tools/capabilities first and always specify this project's path.
  Respect the host agent's file-editing constraints. Keep Git and device/system
  operations in their appropriate tools; IDE access does not broaden permission.
  If IDE access fails, report it and use standard tools. Never claim IDE checks
  ran merely because the server is configured. Keep local MCP endpoints, session
  identifiers and permissions out of repository documentation and commits.
- Inspect existing IDE run configurations or file run points before executing
  them. Prefer relevant test configurations; do not blindly launch the application
  configuration or another platform. Apply temporary overrides only when supported.
  For shell Gradle runs use `--max-workers=6`; apply the same limit in IDE Gradle
  arguments when supported. Do not run concurrent Gradle builds.
- Before device tests, resolve the current device serial/model, connection and
  lock state. Explicitly target the agreed device for every ADB operation. Never
  use the default device when multiple phones are connected, or change a phone's
  security/settings to make a test run. Ask for unlocking when needed.
- Verify the built variant, application/test package, instrumentation target and
  installed version before deployment. Prefer a non-destructive in-place update;
  never silently downgrade, uninstall, clear data or replace the user's newer APK.
  Record source revision/variant when verified; an installed version alone does
  not establish its exact source revision.
- Separate verification layers: unit tests, self-targeted feature UI tests with
  fake state/ports, application-composition tests with injected dependencies, and
  read-only checks in the real app. A self-targeted APK must not initialize the
  production Application, DI or network. App-targeted instrumentation may still
  initialize real background services/analytics; inspect this before running it.
- Real-account create/edit/delete, logout/account replacement, favorite changes,
  consent changes and destructive recovery require explicit authorization and a
  safe test setup. Empty live history is not permission to manufacture records.
  Opening/cancelling a form is not evidence that persistence or analytics passed.
- UI tests must synchronize with actual state, scroll lazy containers to semantic
  targets rather than assuming item indices, and inject image/picker ports. Cover
  light/dark themes, relevant font/window sizes, accessibility semantics, modal
  placement, privacy and back/cancel behavior without real camera/network writes.
  Classify failures as product, test harness or environment only from evidence;
  rerun affected tests and the complete relevant suite after a harness fix. Never
  hide a flaky failure or weaken an assertion merely to obtain a green run.
- Keep screenshots/UI dumps and sanitized diagnostics local, outside tracked
  sources. Do not publish account identity, private photos, full logcat, credentials
  or authorization headers. Inspect IDE warnings on changed files when possible;
  report pre-existing warnings without expanding scope to unrelated migrations.
- Record test date, source/build identity, device, suite counts/failures/retries,
  visual checks and untested gates in the feature plan. Distinguish compiled tests
  from executed tests and fake scenarios from live backend acceptance. An IDE
  inspection, preview, passing retry or APK assembly alone is not feature completion.

## After coding

- Compile affected modules and run relevant tests.
- Android debug assembly must pass the packaged DEX runtime-class checks.
  Compilation alone does not prove startup correctness; smoke-test startup on
  a device when diagnosing packaging/DI crashes, without clearing user data.
- Check Gradle dependencies, imports, source sets, and dependency cycles.
- Verify DTO/entity containment; infrastructure independence of domain; and that
  UI does not access data implementations.
- Keep changes scoped: architecture work must not silently include unrelated UI,
  dependency, navigation, or product changes.
- Never commit credentials, API keys, passwords, or tokens.
- Add/update paired light/dark previews beside every changed reusable component
  and migrated screen content. Keep Android-only preview tooling in androidMain;
  shared previews use multiplatform tooling in commonMain. Use fake/injected state,
  no production DI/network, and record modal/IDE rendering limitations.
- Preserve Kotlin Result request contracts and rethrow CancellationException;
  do not use suspend runCatching where it would swallow cancellation.

## Decision rule

When a change seems to require a convenient forbidden dependency, stop and ask:

1. Which feature owns this behaviour?
2. Which layer owns the responsibility?
3. Must it be public outside the feature?
4. What is the smallest change that preserves the boundary?

Prioritize correctness, dependency boundaries, business ownership, testability,
platform independence, and minimal public API over convenience.
