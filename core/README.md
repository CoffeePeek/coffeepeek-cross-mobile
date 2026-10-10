# Core preparation roadmap

Prepare and verify independent infrastructure before integrating each boundary
into the application. Do not pre-create every example module in ARCHITECTURE.md.

## Prepared foundations

- `coroutines`: injectable IO dispatcher and caller-owned supervised scope.
- `network`: shared transport settings, engine-injected factory and Kotlin
  `Result` request boundary, plus independent bearer-session callbacks and
  origin-scoped refresh, opt-in anonymous HTTP cache and safe diagnostics.
  Cancellation is rethrown, not turned into failure.
- `database`: Room builder configuration with bundled SQLite; no application
  schema, feature entities or migrations are moved into core. Android test-only
  schemas cover persistence, caller migrations and fail-safe missing migrations.
- `presentation`: typed MVI ViewModel base shared by migrated feature
  implementations. It owns state/event mechanics; favorites is the first
  subclass and still owns actions, Result failures and error presentation.

All foundations are independent duplicates prepared for later migration.
`design-system` now prepares shared visual tokens, injected-font typography, icons,
generic controls/rows, fields/search, stateless top bars and the glass modifier.
The presentation slice adds badges/settings rows, segmented selection and
loading/error surfaces. Its audit tracks remaining UI slices.
The legacy network client retains its own transport configuration. Application
composition and favorites consume selected core boundaries; this is not a
wholesale migration of legacy infrastructure.

Each new module uses a dedicated `feature/...` branch and PR. Small related
changes may remain in the current PR; split large migrations with many new files.
Application integration must be a separate, explicitly planned stage.

## Remaining preparation, in order

1. Network: transport, auth/refresh, anonymous cache and diagnostics are prepared
   independently. Platform storage, debug sinks and session adapters remain for
   integration; coordinate logout with active refresh and choose cache limits.
2. Database: runtime contracts are covered by Android instrumentation fixtures.
   Keep actual schema/migration ownership above feature persistence; decide
   composition and validate production migration history during integration.
3. Design system: handle-dismiss sheet/FAB and paired theme previews are prepared.
   Pull-to-refresh now has isolated gesture/cooldown contracts and paired previews.
   Six Manrope weights and their license are packaged; previews use the brand font.
   RTL-safe inset recipes now have paired previews and measured-layout tests;
   no unused legacy Insets wrapper is duplicated. Real OS/IME/cutout behaviour
   still requires consumer-level QA. Do not add speculative core modules.
   Targeted large-font/RTL tests and scale-2 paired previews are also prepared;
   stepper targets grow from a 48.dp minimum instead of fixed 32.dp height.
   Existing controls are not integrated. Verify large font scale, RTL and visual
   parity per consumer before removing legacy copies.
4. Navigation: add infrastructure only if the first feature's entry-point
   contract demonstrates a shared need. Root graph and Koin assembly stay in
   application composition; no generic core DI module is required.

## Integration gate

No core navigation, DI, common/utils, image-loading or storage module is required
merely to complete a diagram. Extract them only for concrete shared consumers.
Settings/preferences persistence needs a separate ownership audit; auth tokens,
feature entities, DAOs and business UI must not become design-system/core code.

## Merge provenance

PRs #39 (cache/diagnostics) and #42 (inputs/top bars) were merged into their
parent feature branches after those parents had reached main. Consequently main
01bc457 did not contain those slices. The controls branch replays their commits
and resolves only the overlapping roadmap text. Its PR must target main, not
an already merged feature branch.

Compile each affected module and run its contract tests first. Then integrate
one infrastructure boundary at a time, build the Android application and verify
existing behaviour. Finally choose one business feature and migrate its domain,
data and UI boundaries without exposing DTOs or DAOs. Remove legacy code only
after all consumers move. Native iOS implementation is outside this stage.
