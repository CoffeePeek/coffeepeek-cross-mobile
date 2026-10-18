# CoffeePeek Architecture

## Purpose

CoffeePeek evolves toward a feature-based Clean Architecture for Kotlin
Multiplatform, with Android Compose and native iOS SwiftUI. The goal is
predictable ownership, isolated changes, testable logic, and controlled Gradle
dependencies — not the maximum number of modules or classes.

This document describes the target and the migration rules. The actual Gradle
configuration is authoritative for the current state.

## Current and target states

Current code is arranged by technical layers:

```text
composeApp/                         UI, navigation, theme, Koin, platform glue
modules/domain/                     models and repository interfaces
modules/network/                    Ktor, DTOs and API services
modules/data/                       repository implementations and mapping
modules/room/                       Room persistence
```

Target ownership is feature-based:

```text
application composition
        │
        ├── features/<business-capability>
        │       ├── api
        │       ├── domain
        │       ├── data
        │       └── ui
        │
        └── core/<shared-infrastructure>
```

The directories above are logical first, Gradle modules second. A feature should
begin with clear packages in one KMP module. Extract a submodule only where it
provides a meaningful boundary.

## Dependency model

```text
app / current composition module
          │
          ├───────────────┬────────────────┐
          ▼               ▼                ▼
      feature A       feature B        core infrastructure
          │               │
          └── other feature API only ──┘

within one feature:   ui → domain ← data
```

Allowed dependencies:

| Consumer | May depend on |
|---|---|
| Application composition | Required features and core modules |
| Feature UI | Its own domain/api and needed core infrastructure |
| Feature data | Its own domain and needed core infrastructure |
| Feature domain | Kotlin and deliberately allowed domain dependencies |
| Feature API | Minimal public dependencies only |
| Core | Other appropriate core infrastructure only |

Forbidden dependencies:

- `core → feature`;
- `feature A → feature B` implementation (`domain`, `data`, or `ui`);
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

### Data

Data owns external and persistence representations and implementations:

```text
remote/       feature-specific Ktor APIs and DTOs
local/        feature-specific Room entities and DAOs
repository/   repository implementations
mapper/       data ↔ domain mapping
```

Repository implementations decide remote/local access, caching, persistence,
and infrastructure-to-application error mapping. Map DTOs and entities at the
boundary; never expose them to domain, UI, or a feature API. Simple mappings may
be `internal` extension functions rather than artificial mapper interfaces.

### UI

UI owns ViewModels, immutable UI state, actions/events, presentation models,
screens, feature components, and feature navigation builders. Compose renders
state and emits actions; it contains no network calls, database queries, or
business rules. ViewModels coordinate presentation and domain interactions but
do not use Ktor, DAOs, DTOs, entities, SQL, or navigation implementation.

Use a UI model only when the UI needs presentation-specific data; otherwise a
domain model may be used directly.

### Core

Core modules exist only for shared, infrastructure-oriented, semantically named
responsibilities. Examples:

```text
core/network          HttpClient, auth/interceptors, serialization, logging, errors
core/database         Room factory/configuration, database bootstrap, migrations
core/coroutines       dispatcher and application-scope abstractions
core/navigation       shared navigation infrastructure, if truly required
core/design-system    theme, typography, dimensions, icons, UI primitives
```

Feature-specific API services, DTOs, entities, and DAOs are not core code. Do
not create broad `base`, `common`, `utils`, `helpers`, `misc`, or `shared`
modules. Name cross-cutting code by its actual responsibility instead.

## Navigation and DI

A feature publishes a small serializable navigation contract only when another
feature must navigate to it. The composition module owns the root graph and
wires feature navigation builders together. Pass route arguments, not
ViewModels, repositories, services, or implementation objects.

Koin definitions may be declared by a feature, but application composition
assembles them. DI must not circumvent Gradle boundaries or fetch another
feature's internal class.

## KMP platform boundaries

```text
commonMain  shared Kotlin domain, data, presentation, and compatible Compose UI
androidMain Android-only APIs, integrations, and UI
iosMain     Kotlin/Native actual implementations of iOS platform abstractions
iosApp      native Swift/SwiftUI application, lifecycle, navigation, integration
```

`commonMain` does not mean “non-UI”: Compose Multiplatform UI may live there.
Only Android-specific Compose/API code belongs in `androidMain`. Native SwiftUI
always belongs in `iosApp`, not `iosMain` or shared feature modules. Prefer
shared code where library support permits; use `expect`/`actual` only for a real
platform difference.

## Incremental migration

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

`:features:author-blocking` owns author-block HTTP DTOs and pagination, repository
state, account/session invalidation, mutation reconciliation, visibility snapshots,
and presentation coordination. These moved from `composeApp/.../authorblocking`;
repository/domain/ViewModel implementations are `internal`, so composition and
other features cannot import them. The Gradle boundary is justified by enforcing
that ownership; the earlier packages inside `composeApp` could not enforce it.

Allowed consumers are application composition and other features through
`com.coffeepeek.feature.authorblocking.api` only. The public surface consists of
author identities, visibility/action contracts, and a small presenter/state entry
point for renderers. Presentation state is not a serialized wire DTO and contains
no session token or HTTP object. A composition-only factory accepts the existing
configured `HttpClient` and legacy `SessionRepository`; these are deliberate
transitional dependencies, not interfaces for feature UI consumers. The module
also depends on coroutines and serialization; its tests use
`core/network` and Ktor MockEngine. It never depends on `composeApp`, another
feature implementation, Compose, lifecycle, Koin, navigation or platform APIs. The
presentation coordinator's scope is owned by the feature's explicit `close()`;
it is not registered with a platform ViewModel store. Domain remains
independent of HTTP, presentation and platform libraries.

`AuthorBlockingHost` and `BlockedAuthorsScreen` temporarily remain in `composeApp`
as pure Cp UI rendering/navigation adapters. They consume the public presenter,
render state and emit actions; network, account coordination and business rules
live in the owning module. Moving those renderers now would require a forbidden
feature-to-application dependency because Cp UI/theming/navigation still live in
`composeApp`. A later coherent design-system/navigation slice can move the
renderers without extracting unrelated UI here. New author-blocking business
logic must be added to `features/author-blocking`, never to these adapters.

Privacy behaviour is retained: session changes clear visibility and pending
confirmations, stale responses cannot cross account generations, incomplete page
loads stay closed, and a timed-out mutation reconciles before another write.
Avatar URL resolution clears the list request's pagination query and fragment
before preserving the avatar URL's own query/fragment. Common module tests cover
those cases; application integration tests cover feed visibility and real Compose
menu/rendering adapters. Verify `:features:author-blocking:testDebugUnitTest`, the
application flavor tests and Android instrumented compilation; Kotlin/Native
compilation verifies the shared presentation/dependency boundary for iOS.

## Verification checklist

After an architectural change, verify:

- affected modules compile and relevant tests pass;
- no new forbidden dependency or cycle exists;
- DTOs/entities remain inside data;
- domain is infrastructure- and platform-independent;
- UI does not access data implementations;
- core contains no feature business logic;
- API surfaces are minimal;
- KMP source sets and platform responsibilities remain correct.
