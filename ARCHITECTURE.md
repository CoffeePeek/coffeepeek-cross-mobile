# CoffeePeek Architecture

## Purpose

CoffeePeek evolves toward a feature-based Clean Architecture for Kotlin
Multiplatform. Today Android uses Compose and iOS hosts the shared Compose UI in
SwiftUI; native iOS screens remain a future per-feature choice. The goal is
predictable ownership, isolated changes, testable logic, and controlled Gradle
dependencies — not the maximum number of modules or classes.

This document describes the target and the migration rules. The actual Gradle
configuration is authoritative for the current state.

Owner clarification, 2026-10-25: migration is complete only with zero legacy
consumers and removal of `modules/`, not merely when new routes are selected.
`composeApp` keeps startup, root DI, platform configuration and root navigation;
business UI, presentation state and ViewModels belong to feature implementations.
Reusable UI and platform infrastructure belong to the appropriate core owner.
Active legacy slots, adapters and removal checkpoints are tracked in
[feature/LEGACY_EXIT_PLAN.md](feature/LEGACY_EXIT_PLAN.md). The layout snapshot
below describes earlier transitional ownership, not a reason to retain it.

## Current and target states

Current code is arranged by technical layers:

```text
composeApp/                         UI, navigation, theme, Koin, platform glue
modules/domain/                     models and repository interfaces
modules/network/                    Ktor, DTOs and API services
modules/data/                       repository implementations and mapping
modules/room/                       Room persistence
core/                               prepared infrastructure/design-system
feature/favorites/                  api/domain/data/impl; Android screen active
feature/shop-report/                api/domain/data/impl; Android report active
feature/shop/                       api/domain/data/impl; Android menu gallery active
feature/check-in-history/           api/domain/data/impl; Android combined history active
feature/home-check-in/              pure domain/data and Android detail/editor api/impl;
                                   active in Android history/global creation; profile/iOS legacy
feature/coffee/                     api/domain/data/impl picker, MVI and Android UI prepared;
                                   current catalogue screens/DI still in composeApp
feature/roaster/                    shared api/domain/data/impl and Compose UI;
                                   common application composition selects Android/iOS entries
```

Target ownership is feature-based:

```text
application composition
        │
        ├── feature/<business-capability>
        │       ├── api
        │       ├── domain
        │       ├── data
        │       └── impl
        │            └── ui
        │
        └── core/<shared-infrastructure>
```

The agreed migration target is api/domain/data/impl Gradle boundaries per
business feature, created progressively under singular feature/. A feature di
Gradle module is not required for each feature; favorites Koin assembly and its
temporary legacy compatibility bridge now live in composeApp/androidMain.
Root Koin assembly and platform bridges normally belong in composeApp packages.
The split provides a small cross-feature ABI, independently testable domain/data and
hidden presentation/composition. Do not scaffold all future features or force
domain/data onto infrastructure modules with no business responsibilities.
The existing core modules and legacy layout remain authoritative until migrated.

## Dependency model

```text
app / current composition module
          │
          ├───────────────┬────────────────┐
          ▼               ▼                ▼
      feature A       feature B        core infrastructure
          │               │
          └── other feature API ──┘

within one feature:
    impl → api
    impl → domain ← data
    impl/ui → domain, api, design-system (never data implementation)

application composition:
    composeApp DI → api, domain, data factories, impl entry factory
    composeApp bridges → legacy storage/contracts (temporary, platform-owned)
```

Allowed dependencies:

| Consumer | May depend on |
|---|---|
| Application composition | Required features and core modules |
| Feature UI | Its own domain/api and needed core infrastructure |
| Feature impl composition | Its api/domain and data wiring entry points |
| Feature data | Its own domain and needed core infrastructure |
| Feature domain | Kotlin and deliberately allowed domain dependencies |
| Feature API | Minimal public dependencies only |
| Core | Other appropriate core infrastructure only |

Forbidden dependencies:

- `core → feature`;
- `feature A business/UI → feature B` domain or implementation (`data`, `impl`, or `di`);
- `domain → data`, UI, Android, Compose, Ktor, Room, SQL, HTTP, or Koin;
- `data → ui`;
- UI → DTO, entity, DAO, Ktor service, or repository implementation;
- any dependency cycle.

If a cycle appears, identify the shared responsibility and introduce the minimum
valid API contract or relocate the responsibility. Do not hide the cycle in a
generic `core/common` module.

## Layer ownership

### Feature API

`api` contains only intentionally public contracts: typed routes, feature entry
points, and small contracts needed by another feature or application boundary.
It must not contain DTOs, entities, DAOs, Ktor services, repository
implementations, use cases, ViewModels, Compose screens, or databases.

Public by necessity; implementation details are `internal` by default.

### Domain

Domain owns business models, rules, validations, repository interfaces, and
use cases with actual business meaning. A use case is warranted for business
behaviour, coordination of repositories, reusable domain logic, or a business
rule — not merely to delegate a repository call.

Domain models are neither DTOs, Room entities, nor UI models. Repository
interfaces describe capabilities without exposing HTTP, SQL, Ktor, Room, DTO,
or entity details.

Within a feature, place domain models, repository contracts and meaningful use
cases under `model/`, `repository/` and `usecase/` respectively. Add another
package only when it has a named responsibility.

### Data

Data owns external and persistence representations and implementations:

```text
backend/      feature-specific Ktor APIs and DTOs, when present
local/        feature-specific Room entities and DAOs
repository/   repository implementations
mapper/       data ↔ domain mapping
```

Repository implementations decide remote/local access, caching, persistence,
and infrastructure-to-application error mapping. Map DTOs and entities at the
boundary; never expose them to domain, UI, or a feature API. Simple mappings may
be `internal` extension functions rather than artificial mapper interfaces.

### UI

Function-size review limits are 40 non-blank, non-comment body lines for a
Composable and 20 for an ordinary function. Nested blocks count; signatures do
not. Extract cohesive components/helpers, not compressed formatting. Apply to
new/substantively changed functions without mass-rewriting unrelated code or
adding an automated lint/build limit.

UI owns ViewModels, immutable UI state, actions/events, presentation models,
screens, feature components, and feature navigation builders. Compose renders
state and emits actions; it contains no network calls, database queries, or
business rules. ViewModels coordinate presentation and domain interactions but
do not use Ktor, DAOs, DTOs, entities, SQL, or navigation implementation.

Use a UI model only when the UI needs presentation-specific data; otherwise a
domain model may be used directly.

For migrated Compose features, keep the screen in `impl/ui/compose/`, its
independent components in `compose/component/`, and actual state/action/event
types in `compose/model/`. Keep the feature ViewModel at `impl/ui/`. A narrowly
scoped `impl/ui/data/` may own presentation-only formatting, but business use
cases stay in domain and storage/network mapping stays in data. Do not create
empty packages to fill a template.

Use one-way MVI flow where it clarifies behaviour: UI emits typed actions,
ViewModel reduces durable state or emits one-off events, and the runtime screen
maps navigation events to caller callbacks. New MVI ViewModels can inherit the
typed core/presentation base; legacy ViewModels remain on their current path.

Give each standalone component its own named file. `NameScreen` is the runtime
adapter that observes a lifecycle-owned ViewModel and forwards state/events to
stateless `NameScreenContent`; feature entry/application composition supplies its
dependencies. `NameScreenContent` and components are previewed with
fake state and callbacks. Keep each preview in the component or screen's own
source file, not in a preview-only file. Shared `commonMain` Compose UI uses paired
multiplatform light/dark previews; Android-only UI can use `PreviewLightDark` in
`androidMain`. Do not move shared UI to Android solely for preview tooling.

The old application BaseViewModel remains transitional for legacy screens. New
MVI ViewModels inherit the typed core/presentation base, which owns a private
state flow and one-off event channel on top of the multiplatform lifecycle
ViewModel. It launches each submitted action independently in the standard
lifecycle scope, without a serialized queue, global catch-all, or the old
loading/error behaviour. Features implement suspending action handlers and own
their Result failures, cancellation, and conflicting-action policy.
Place reusable visual tokens/components in design-system, feature-specific UI and
assets in the feature, and user-facing strings/accessibility text in resources.
Verify supported locales and translation parity before documenting their number.
Migrated UI must consume applicable `core/design-system` components, icons,
modifiers and tokens rather than copying them or creating parallel legacy-style
implementations. Feature-owned components compose these primitives. Missing
shared behavior is added to the existing primitive when truly reusable; custom
business rendering and ordinary Compose layout remain in the feature.

### Core

Core modules exist only for shared, infrastructure-oriented, semantically named
responsibilities. Examples:

```text
core/network          HttpClient, auth/interceptors, serialization, logging, errors
core/database         Room factory/configuration, database bootstrap, migrations
core/coroutines       dispatcher and application-scope abstractions
core/presentation     typed MVI ViewModel base and state/action/event contract
core/navigation       shared navigation infrastructure, if truly required
core/design-system    theme, typography, dimensions, icons, UI primitives
```

Design-system Kotlin components, UI types, tokens and source files use the
`CoffeePeek` prefix: `CoffeePeekButton`, `CoffeePeekTextField`, `CoffeePeekIcons`
and `CoffeePeekDimens`. Non-composable functions/Modifier extensions use lower
camel case (`coffeePeekTypography`, `coffeePeekLiquidGlass`), and composition
locals use `LocalCoffeePeek…`. Do not create parallel `App…`/`Cp…` compatibility
aliases for the new API. Feature-owned UI keeps feature names; legacy components
retain their existing names until migration. Font/resource provenance is unchanged.

Feature-specific API services, DTOs, entities, and DAOs are not core code. Do
not create broad `base`, `common`, `utils`, `helpers`, `misc`, or `shared`
modules. Name cross-cutting code by its actual responsibility instead.
The presentation base extends the multiplatform lifecycle ViewModel and owns
private `MutableStateFlow` plus a buffered one-off event channel. It exposes
read-only `state`/`events`, atomic `updateState`, `currentState`, and suspending
`sendEvent`. Its public `onAction` launches a coroutine for each action and
delegates to the feature's suspending `handleActionInternal`. It does not own
an action queue, custom scope, global errors or loading. Feature impl modules
keep Result/error decisions local and guard actions that must not overlap.
Event-less screens may use `Nothing` without an event class.

## Navigation and DI

A feature publishes a small serializable navigation contract only when another
feature must navigate to it. The composition module owns the root graph and
wires feature navigation builders together. Pass route arguments, not
ViewModels, repositories, services, or implementation objects.
Keep feature-specific entry registration/adapters in `impl/navigation/` and the
public route/entry contract in `api/`. Neither layer owns the application back
stack. Extract `core/navigation` only after multiple features demonstrate the
same infrastructure requirement.

Koin definitions may be declared by a feature, but application composition
assembles them. A logical Koin module does not require its own Gradle module.
The existing composeApp owns root Koin and platform wiring; do not create a
second application or universal feature DI aggregator without a concrete need.
DI must not circumvent Gradle boundaries or fetch another feature's internal
class. Dagger/Hilt is not part of the current KMP composition strategy.

For favorites, domain/data use manual constructor/factory injection. Android
application composition owns Koin assembly and temporary adapters to the existing
server-backed shop/roaster/drink source; the earlier Room foundation is not the
active writer. Impl UI sees its own domain only and does not perform Koin lookups.
Other features define their own domain ports; application composition adapts them
to favorites contracts. Cross-feature dependencies use only intentionally public API.
API remains the navigation/screen-entry ABI, not a re-export of business models.
Composable entry interfaces are allowed there; screen/VM implementations are not.

## KMP platform boundaries

```text
commonMain  shared Kotlin domain, data, presentation, and compatible Compose UI
androidMain Android-only APIs, integrations, and UI
iosMain     Kotlin/Native actual implementations of iOS platform abstractions
iosApp      native Swift/SwiftUI application, lifecycle, navigation, integration
```

`commonMain` does not mean “non-UI”: Compose Multiplatform UI may live there.
The current iOS SwiftUI shell hosts the shared Compose framework; it does not
yet provide native SwiftUI versions of migrated screens. A native screen may
later use shared domain/data through a deliberate public bridge without copying
business rules.
Roasters uses one commonMain Compose implementation and shared application
adapters on Android and iOS; its native app navigation and platform callbacks
stay application-owned. Other features retain their existing migration scope.
Only Android-specific Compose/API code belongs in `androidMain`. Native SwiftUI
always belongs in `iosApp`, not `iosMain` or shared feature modules. Prefer
shared code where library support permits; use `expect`/`actual` only for a real
platform difference.

## Incremental migration

Use stacked PRs from the current migration branch tip. Group cohesive migration
steps in a checkpoint PR instead of opening one for every small task. The
working guide for the next checkpoint is roughly 100–150 changed files in the PR
diff against its base. Once a business feature checkpoint is complete, begin
the next feature in a new child branch and PR. Keep supporting slices needed to
finish that same feature together; do not append new features to an already
completed checkpoint. A distinct review/risk boundary can justify
an earlier split; an indivisible change may exceed the guide with an explanation.
Never combine unrelated work to fill a PR. Fixes to a parent capability may stay
in its PR, followed by explicit descendant synchronization. A child PR targets the previous open
checkpoint branch. After merging a parent to main, retarget its child to main
and verify the diff before merging; do not merge into stale already-merged
branches. Branches isolate unfinished work from main, not runtime behaviour
from users. Verify each logical integration step even when several steps share
one PR.

Use strangler migration. A temporary mixture of legacy and feature-based code is
valid, provided new code does not reproduce legacy violations.

1. Inspect the actual module graph and identify the business owner of code.
2. Establish logical feature boundaries before extracting Gradle modules.
3. Move only one cohesive responsibility at a time, then update dependencies.
4. Verify build and tests before the next slice.
5. Stop adding feature-specific code to its legacy location after migration.
6. Delete legacy code only after all consumers have switched.

Never mechanically copy a legacy class into a new folder. Split only where a
class mixes responsibilities and the split improves a real boundary. Do not mix
feature work with unrelated framework swaps, mass formatting, dependency
upgrades, or UI redesign.

## Module-creation gate

Before adding a Gradle module, answer all of these:

1. What responsibility does it own?
2. Which modules may depend on it?
3. What may it depend on?
4. Which implementation details become hidden?
5. Why is a package boundary insufficient?
6. Does its isolation justify Gradle and build complexity?

If any answer is unclear, keep a package boundary and revisit later.

## Author-blocking migration slice

`feature/author-blocking/{api,domain,data,impl}` owns author-block HTTP DTOs and pagination,
repository state, account invalidation, mutation reconciliation, visibility snapshots and
presentation coordination. It moved from `composeApp/.../authorblocking`, then from the old
top-level `features/author-blocking`, and now follows the same four-module layout as the other
owners. The repository implementation and ViewModel are `internal`; consumers use
`com.coffeepeek.feature.authorblocking.api` only: author identities, visibility/action contracts,
the presenter/state entry point and the `AuthorBlockingAccount` port.

The account port replaces the old dependency on the legacy `SessionRepository`: the application
adapts its session (`SessionAuthorBlockingAccount`) into an opaque account key that is null when
nobody is signed in. `domain` depends on coroutines only, `api` on coroutines, `data` on
`api`/`domain`, Ktor and serialization, and `impl` composes them behind
`createAuthorBlockingFeature(client, account)`. Nothing depends on `composeApp`, another feature
implementation, Compose, lifecycle, Koin, navigation or platform APIs. The presentation
coordinator's scope is owned by the feature's explicit `close()`.

`AuthorBlockingHost` and `BlockedAuthorsScreen` still live in `composeApp` as Cp UI
rendering/navigation adapters over the public presenter; moving them into `impl` on
`core/design-system` is the remaining slice. New author-blocking business logic belongs in the
feature modules, never in these adapters.

Privacy behaviour is retained: session changes clear visibility and pending confirmations, stale
responses cannot cross account generations, incomplete page loads stay closed, and a timed-out
mutation reconciles before another write. Avatar URL resolution clears the list request's
pagination query and fragment before preserving the avatar URL's own query/fragment. Module tests
cover those cases; application integration tests cover feed visibility and the Compose
menu/rendering adapters. Verify `:feature:author-blocking:data:testDebugUnitTest`,
`:feature:author-blocking:impl:testDebugUnitTest`, the application flavor tests and Android
instrumented compilation; Kotlin/Native compilation verifies the shared dependency boundary for iOS.

## Verification checklist

Android execution environments are explicit: Sandbox is the agent's default local fake
environment in a separate `com.coffeepeek.sandbox` package; ordinary Debug uses
the live configured backend and `com.coffeepeek` storage, with Google sign-in
disabled. Direct/Play are delivery flavors, not mock/live selectors. See
`composeApp/ANDROID_RUN_MODES.md`. Build-only configurations never authorize
deployment, and Sandbox acceptance does not establish live-backend readiness.
The user chooses the active Studio variant; this policy does not silently
switch their ordinary Debug workflow to Sandbox.
Do not create, restore or run automated UI/Compose instrumentation/screenshot
tests or test-only hosts. UI verification uses colocated previews and manual
Sandbox review. Unit, non-UI contract/DI and database tests remain in scope;
other contributors' existing UI tests are not removed or expanded by agents.
Dated UI results in old migration plans are historical only.

Analytics is part of migration parity: preserve existing event/property names,
screen/exposure ownership, search correlation, consent and session invalidation.
Inject narrow feature API ports and adapt the current service in composition.
Verify through in-memory unit/contract tests, without production telemetry.

After an architectural change, verify:

- affected modules compile and relevant tests pass;
- Android debug APKs contain the required startup class definitions (the
  assembly finalizers verify packaged DEX, not only compile/runtime JARs);
- no new forbidden dependency or cycle exists;
- DTOs/entities remain inside data;
- domain is infrastructure- and platform-independent;
- UI does not access data implementations;
- core contains no feature business logic;
- API surfaces are minimal;
- KMP source sets and platform responsibilities remain correct.
