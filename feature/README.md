# Feature migration: API / implementation

The active Android audit and execution order are in
[ANDROID_REFACTORING_PLAN.md](ANDROID_REFACTORING_PLAN.md). Historical stack and
completed-slice notes below are context, not the next-work checklist.

This directory now contains six api/domain/data/impl families: favorites,
shop-report, shop, check-in-history, home-check-in and coffee. The first five
have active Android entries; Coffee list/detail/picker/UI are prepared but not
yet selected by application DI/navigation. Android Shop gallery, check-in form, detail and
public check-in list are wired and have passed automated Android/device checks;
live cup/picker/authenticated-write QA remains pending before release. See
shop/README.md and shop/ANDROID_PARITY_PLAN.md for active versus prepared paths.
Android application composition owns favorites Koin and its compatibility adapter; feed
and detail receive membership through application-owned adapters, and the Android favorites route
renders the new screen. The shared root still uses Navigation 2, while iOS keeps
the old favorites screen and binding. See favorites/README.md.
Android Shop Report is also routed through its feature entry; the shared/iOS
route intentionally keeps the legacy renderer. See shop-report/README.md.
Actual registration remains in build-logic Modules.all. Android is the integration
target; no native iOS implementation is part of these slices.

As of 2026-10-19, Home has populated domain/data/api/impl, not only the earlier
read domain. Android combined history and global creation use its new repository,
detail and editor entries through account-owned app adapters. Shared/iOS and
profile Activity keep their legacy consumers. See home-check-in/MIGRATION_PLAN.md
for implementation and acceptance evidence.

Combined personal history is integrated. Its populated
check-in-history/domain and data modules prepare read-only pagination/calendar
contracts; impl now uses one shared typed MVI and production-shaped Android
All/Shop/Home calendar/timeline without data/HTTP dependencies. The obsolete
prepared Calendar/List screen/coordinator is retired, not activated alongside it.
API exposes narrow screen/date ports and required Home/create callbacks.
Android app composition supplies revocable account leases, a navigation-owned
feature store, zone-aware calendar snapshots, lazy DI and the configured image
loader/photo viewer. Current-main feed/thumb/detail representations and aligned
explicit originals now pass through history and Home data/UI/gallery contracts;
live protected-photo acceptance remains open. All three Android history routes
now render this combined entry, including initial Home edit-by-ID. Application
composition delegates to the new Home detail/create/edit/delete workflow and
Shop creation picker; successful Home writes refresh the combined reader.
Shared/iOS selection retains ProfileCheckInsScreen and the existing writer.
Backend source confirms inclusive Home civil dates and Shop lower-inclusive,
upper-exclusive instant bounds. Physical-device/deployed-backend parity remains
open; automated fake-data checks do not certify live behavior.
See [check-in-history/README.md](check-in-history/README.md) for the audited boundaries,
ordered implementation and session/time-zone risks. No empty API module or
new application module is part of these migration slices.

## Target layout and graph

For each migrated business capability (not each individual screen), normally:

```text
feature/<name>/
  api/                   routes and minimal cross-feature capabilities
  domain/
    model/               business models
    repository/          business contracts
    usecase/             meaningful rules/coordination, when needed
  data/
    backend/             feature HTTP APIs and DTOs, only when present
    local/               persistence interfaces/entities/DAOs, when present
    mapper/              representation ↔ domain conversion, when present
    repository/          implementations and narrow construction factories
  impl/
    src/commonMain/.../
      <Feature>ApiImpl.kt
      navigation/        feature entry registration; root stack stays in app
      ui/
        <Feature>ViewModel.kt
        compose/
          <Feature>Screen.kt
          component/      one component per file, preview beside component
          model/          state, actions, actual one-off events
```

Each boundary becomes a separate KMP Gradle module as its code is prepared.
Do not create empty placeholders or a second application. Api has no data/UI
implementation or Koin declarations. Domain uses pure Kotlin/approved domain
dependencies; it may depend on api only when those contracts are equally pure.
Data depends on domain and needed core infrastructure. Impl depends on api/domain
and design-system; new MVI ViewModels extend core/presentation's typed base, while
its UI cannot access data internals. Favorites Koin and the
temporary legacy bridge live in composeApp/androidMain. New features should be
assembled by composeApp Koin/platform packages unless another Gradle boundary
has a demonstrated need.
Keep data implementations internal and expose only the narrow construction
surface required for feature composition. Enforce UI import boundaries as well
as Gradle edges.

Other features use api for cross-feature contracts. Shop declares its own
ShopFavorites capability; application composition maps favorites snapshots to it.
Business/UI code never depends on another feature's domain/data/impl/di.
Composition owns the root graph and assembles feature implementations.

HTTP requests belong to feature data/backend, not screens. Room persistence belongs
to data/local. Core provides transport/driver infrastructure without feature DTOs,
DAOs or endpoints. Preserve Kotlin Result and rethrow coroutine cancellation.
Not every feature needs HTTP, Room or a use case wrapping a single repository call.
Avoid empty packages; `formatter` and `ui/data` appear only for distinct real work.

## Modules DSL

Keep existing module.core.network / module.legacy.domain accessors compatible.
As actual feature modules are added, introduce a feature catalog following the
existing object-plus-instance-accessor pattern:

```kotlin
implementation(project(module.feature.favorites.api))
implementation(project(module.feature.favorites.domain))
implementation(project(module.feature.favorites.data))
implementation(project(module.feature.favorites.impl))
```

Favorites accessors are now callable for these four boundaries.
Register only real module
paths in Modules.all. Domain/data are nested by business owner, not new global
technical-layer buckets. Core api/impl splits need their own concrete ABI/reuse
reason; a transport module does not need artificial business domain/data modules.

## Branch and PR stack

Current chain: #43 -> #95 -> #99 -> #101 -> #102 -> #104. The parent #43 targets
develop; each child targets its predecessor's branch. Ownership is respectively
core/favorites/Shop, combined history, Home, full-app Sandbox, shared ratings and
prepared Coffee. Main dfb00e63 is inherited parent-first through the chain;
its final CI-only change does not alter the verified 28917758 application baseline.
Former checkpoint PRs remain closed/superseded. Parent fixes stay with their
owner and flow into descendants; see the roadmap's exact branch table.

Every next work branch starts from the current migration tip, not main. Group
related tested steps into a checkpoint PR, using roughly 100–150 changed files
as the guide. Finish the current business feature, then start the next in a new
child branch and PR. Supporting slices needed by the same feature stay together;
do not split every small task or mix unrelated work to fill the count. A clear
review/risk boundary may justify an earlier split; an indivisible change may
exceed the guide with an explanation. Each PR
targets the previous open checkpoint branch, not an individual closed slice.
Historically, favorites foundation/integration were collected in PRs #53, #57
and #62, and presentation follow-ups in #67; they are now superseded by #43.
After a parent merges to main, retarget its child to main before merging it.
If a parent changes, update descendants explicitly and re-run affected checks;
never rewrite published stack history or unrelated work without agreement.

## Ordered slices

1. Confirm these boundaries and finish shared UI readiness: refresh policy,
   brand font/resources and RTL-safe insets. Keep light/dark previews current.
2. Extend build-logic for actual feature paths; extract convention plugins only
   for repeated configuration observed while adding the first modules.
3. Choose one pilot capability. Prepare its api/domain and contract tests.
4. Prepare data implementations/adapters and mapper/persistence tests, with no
   application switching yet. Document current vs target models/schema.
5. Prepare impl/ui using core design-system, injected dependencies, caller
   navigation callbacks and colocated paired light/dark previews.
6. Integrate only that capability through root DI/navigation in a dedicated PR.
   Build the Android app, smoke-test user flows and verify stored-data compatibility.
7. Delete legacy copies only after every consumer has switched. Repeat by owner;
   do not relocate all screens/models/repositories at once.

## Next slices after Android favorites integration

Treat each item as a separately verified step, not necessarily a separate PR.
Group related work under the checkpoint rule above. Do not turn this list into
empty modules or remove the iOS legacy path before its replacement works.

1. Give favorites UI its own text/accessibility resources. Audit actual locale
   directories first; do not invent a second translation to complete a diagram.
2. Completed: move the temporary favorites Koin/Room/legacy bridge into Android
   application composition, preserving one repository instance/writer and its
   persistence tests. The former favorites/di module is removed.
3. Completed: audit the existing BaseViewModel against lifecycle, cancellation,
   Result and error presentation. Fix cancellation handling and lifecycle cleanup;
   do not require migrated ViewModels to inherit a generic base.
4. Completed: move design-system preview-only fixtures beside their components
   in scoped groups, with paired themes and commonMain UI ownership. IDE/modal
   visual rendering still requires manual inspection.
5. Deferred by current product scope: iOS favorites presentation. Keep its legacy
   screen, binding and writer intact; do not infer that Android integration
   completed the migration on iOS.
6. Continue Android feature migration in small owner-based slices. Shop issue
   reporting starts with isolated domain/data preparation; see
   `shop-report/README.md`. Its app switch comes only after api/impl and tests.
7. Migrate root navigation separately after feature entry contracts work on both
   platforms. Do not mix a Navigation 3 root swap with persistence or UI parity.
8. Repeat the feature migration by business owner; remove legacy modules only
   after all consumers on Android and iOS have moved.

## Pilot audit: favorites (Android screen and DI integrated)

Legacy UI remains in composeApp ui/screen/favorites for iOS; Android now enters
the new feature screen through the shared root's Favorites destination.
Current contract: modules/domain FavoriteRepository and CoffeeShop/Details.
Current data: modules/data FavoriteRepositoryImpl and LocalFavoriteShopDto,
using modules/room DatabaseCore.settingRepository and local_favorite_shops JSON.
There is no favorites HTTP service to move.

Remaining integration concerns:

- The legacy iOS screen uses feed.ShopCard and app location helpers. Do not
  depend on feed impl or put CoffeeShop business presentation in design-system
  just to bypass this on Android.
- FavoriteSync remains for the legacy iOS screen; Android feed/detail consume
  the feature domain membership flow. Retire the event path only when every
  remaining consumer has switched.
- The Android feature screen uses injected navigation callbacks and a
  lifecycle-owned ViewModel; the shared root Navigator remains transitional.
- Persistence must preserve existing serialized rows/key during transition.
- Session cleanup also consumes favorites: audit all repository consumers before
  deleting the legacy implementation.

Possible pilot public capabilities: favorite observation/change and route/entry
contracts. Final signatures depend on consumer audit, not speculative DTO export.
Domain/data/UI preparation can proceed independently; switching the app requires
its own testable integration slice.

## Legacy BaseViewModel audit

Twenty-one legacy ViewModels inherit BaseViewModel; only auth and registration
call its launchRequest helper. Those callers unwrap repository Result with
getOrThrow, so request failures still enter their existing local/global error
paths. launchRequest now rethrows CancellationException and releases the global
loading indicator in finally. ViewModelStore disposal cancels its work scope
through the registered closeable; the duplicate onCleared cancellation was
removed and is covered by a lifecycle test. Unused stateHere and handleError
base wrappers were removed after a repository-wide reference search.

The legacy workScope uses Dispatchers.Default and a SupervisorJob. Changing all
inheritors to viewModelScope here would alter threading and lifecycle behaviour
outside favorites, so that migration belongs to each owning feature. The global
LoadingHandler is a Boolean, not request-counted: overlapping legacy requests
may clear one another's indicator. ErrorHandler likewise owns global presentation.
Do not copy these patterns into new MVI screens or create another generic base;
use lifecycle-owned jobs, local state/events and explicit Result handling there.

## Completion gate per slice

Document owner, allowed consumers/dependencies, public surface, migration steps
and risks. Compile changed modules and app whenever integration changes; run
contract/UI tests, check cycles/imports and DTO/entity containment. Inspect both
preview themes, record modal/font/RTL limitations, and keep main unchanged until
the corresponding PR is merged.

## Legacy-first parity gate

Before migrating or activating any owner, inventory the legacy composeApp behaviour (entries, states, analytics, platform behaviour, cross-feature contracts) and map every row to code and tests. Details and checklist: [ANDROID_REFACTORING_PLAN.md](ANDROID_REFACTORING_PLAN.md#legacy-first-parity-gate-mandatory-for-every-owner).
