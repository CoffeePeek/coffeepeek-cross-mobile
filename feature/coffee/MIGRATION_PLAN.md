# Coffee catalogue — Android-first migration

## Baseline and scope (2026-10-18)

Child branch `feature/coffee-catalog-refactoring` starts at rating checkpoint
`72cbae1f` (PR #102), not at main. Remote main checked at `0137ee7`.
Preserve the existing product, requests and visuals; no catalogue redesign,
new favorite type, roaster migration or iOS screen switch.

Current ownership is `composeApp/.../feature/coffee`: combined domain models,
Ktor repository with private DTOs, list/detail ViewModels and Compose screens,
filter sheet and a picker adapter. MainScreen's Coffee tab renders the list;
Navigator's CoffeeDetail destination opens details (including external routes).
Details link to roasters, sellers, sharing and a photo gallery. Application
Koin uses `CoffeePeekClient.plainClient`. Existing Home composition uses the
CoffeePicker contract to choose beans; this is not a third catalogue screen.
The same routes/repository are exercised by full-app Sandbox fixtures.

## Target and module-creation gate

| Module | Responsibility / hidden implementation | Allowed dependencies | Consumers / why a package is insufficient |
|---|---|---|---|
| domain | Catalogue/offer/filter/page models and repository contract; no DTO/HTTP/UI | Kotlin only | Own data/impl and app adapters. Compile-time independence from app and infrastructure. |
| data | Existing catalogue endpoints, request/response DTOs, validation and mapping; expose only a factory returning domain repository | Own domain, core/network, Ktor, serialization, coroutines as needed | App composition via factory. Prevent transport types from leaking into UI/domain. |
| api (later UI slice) | Minimal list/detail entry/navigation and genuinely needed picker ABI | Minimal Compose/route dependencies only when needed | App and other feature callers through public ABI, not implementations. |
| impl (later UI slice) | Typed MVI, screens/content, components, resources, previews | Own api/domain, core/presentation/design-system | App composition through entry factory. Keeps UI independent of data/legacy/root navigation. |

Create modules only with real content. First slice adds domain/data; api/impl
come with screen contracts/renderers, not empty placeholders. Domain/data use
manual constructor injection. App owns Koin, lifecycle/navigation, analytics
adapters and plain-client lifetime. Cross-feature wiring goes through public API
or composition adapters, never another feature's domain/data/impl. Reuse shared
design-system primitives rather than copying legacy App/Cp implementations.

## Ordered work

- [x] Inspect current main, navigation, DI, picker, HTTP models and existing tests.
- [x] Document design-system reuse and explicit live Debug versus fake Sandbox
  launch policy; disable Google sign-in only in ordinary Debug, not Release.
- [x] Extract independently testable domain/data with parity tests; leave the
  active repository/screens unchanged until Android composition is ready.
- [ ] Add api/impl entries and typed list/detail MVI. Preserve debounce, query
  limit, refresh/pagination, cancellation, filter draft/apply/reset/retry and
  stale-result protections. No meaningless one-call use cases.
- [ ] Extract components into compose/component, state/action/events into model;
  stateless ScreenContent, colocated light/dark previews and localized resources.
  Preserve sizing/spacing/image treatment and offer/details behavior.
- [ ] Wire Android tab/detail/deeplink, picker and app-owned analytics using
  factories. Keep shared/iOS fallback until its consumers deliberately migrate.
- [ ] Verify catalogue/picker/roaster return, browser/share/photo cancellation,
  empty/error/slow/out-of-order data and long/scaled text in fake UI/full Sandbox.
- [ ] Publish a child PR targeting `feature/shared-rating-component`, with only
  this checkpoint's changes and accurate remaining gates.

## Verification plan and risks

UI reuse map for the next slice: search/clear/filter affordances compose
`CoffeePeekSearchField` and the existing icon/button primitives; pagination and
refresh use `CoffeePeekLoader` / `CoffeePeekPullToRefresh`; offer weights use
`CoffeePeekCapsuleSegmentedControl`; filters use the existing sheet, grouped
section, checkmark/divider and switch primitives; prices use the design-system
BYN indicator. Image loading/gallery and external browser/share stay injected
application ports, not new core business/network dependencies. Coffee cards,
offer details and labels stay feature-owned. Verify API/visual fit before reuse,
without forcing a different shared default spacing onto the current screen.

Data/domain: execute tests for exact paths/verbs/body, eight filter groups and
sorting, page/total metadata, photos/canonical address, characteristics and offer
fallback, invalid inputs, HTTP/envelope/decode failures and cancellation for all
requests. Preserve current wire names and query/page bounds; no speculative API
fixes. Compile Android and both Native library targets without iOS UI changes.

Build/configuration: assemble Direct/Play Debug plus Direct Sandbox sequentially
with `--max-workers=6`; check packaged DEX and Sandbox isolation finalizers,
generated Debug OAuth blank and Sandbox manifest. Build-only IDE configurations
must name exact variants, never install/launch the live app as a side effect.

Integration/UI: later run isolated fake-state tests before switching Android,
then app composition tests, full Sandbox and Pixel 7 visual checks in both themes.
Record date/source/variant/counts/failures/retries. Actual server contract parity,
platform media/browser/share, OAuth and iOS visual acceptance are separate gates;
no real account writes, photo access or live deployment implied by this plan.

Most important regression risks: changing an offer fallback, losing unknown
availability or null prices, silently changing server field names, duplicated
analytics, stale search pages, changing current visuals, or removing the shared
Coffee repository/picker while iOS still consumes it.

## Verification record

The user also requested a shared affinity-heart correction during this checkpoint.
It is implemented through design-system LikeIcon/LikeButton, selected gold and
neutral outline, without changing Coffee catalogue ownership or adding new
catalogue favorites. See [shared-heart evidence](../../core/design-system/LIKE_PLAN.md):
1073 relevant unit executions and 136 self-targeted Pixel 7 UI cases pass, plus
scoped fake Sandbox visuals. The catalogue's api/impl/UI migration below is still
pending; this is not certification of the new catalogue screen.

2026-10-18, first domain/data slice on top of `72cbae1f`: **18 new Android unit
tests executed** (four domain, 14 data), zero failures/errors/skips. Both new
libraries compile for Android, iOS arm64 and iOS Simulator arm64; no Native test
execution or iOS UI acceptance is claimed. Active Coffee screens/repository/DI
are unchanged; the new factory is intentionally not wired yet.

Android Studio discovers both new build-only run configurations. XML syntax and
`composeApp/build.gradle.kts` IDE error inspection pass. New feature files are
not yet in the IDE content roots until Gradle Sync; no clean IDE feature
inspection is claimed. Direct/Play Debug and Direct Sandbox generated BuildConfig
all have an empty Google web client ID; Release configuration is unchanged.
Whole-app assembly/test verification passes: Direct and Play Debug APKs with
packaged DEX guards, Direct Sandbox APK with manifest isolation and runtime-class
guards; **450 app unit tests executed per delivery variant**. Together with the
18 new feature tests, **918 executions**, zero failures/errors/skips. The first
verification build completed successfully in five minutes. No app was installed,
no phone/account data was changed, and no UI/device suite was run for this
non-UI slice. Existing AGP/KMP and preview deprecation warnings remain out of scope.
