# Coffee catalogue — Android-first migration

## Baseline and scope (2026-10-18)

Current synchronization baseline (2026-10-19): **main dfb00e63**, inherited
parent-first through #43 -> #95 -> #99 -> #101 -> #102 -> #104. Coffee entries
remain inactive. Home/history photo-variant/original and migrated blocking-menu
parity are explicit stack gates before activation, not certified by a clean merge.
See ../ANDROID_REFACTORING_PLAN.md for the current complete inventory/order.
The final commit is CI-only (two signing-profile tests pass); application code
and Android/Native verification retain baseline 28917758.

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
| api | Existing shared picker capability and minimal Android list/detail entries | Kotlin shared picker; Compose runtime/UI only in androidMain | App and other feature callers through public ABI, not implementations. |
| impl | Shared list/detail MVI/picker adapter; Android screens/content, components, resources, previews | Own api/domain, core/presentation; Android design-system/lifecycle/Compose | App composition through entry factory. Keeps UI independent of data/legacy/root navigation. |

Create modules only with real content. First slice adds domain/data; the next
adds the actual picker contract/adapter and tested list/detail MVI to api/impl,
not empty placeholders. The pure-logic slices used the shared-library convention;
the UI slice uses shared-android-compose-library for api/impl, keeping Compose,
Android resources and entry renderers in androidMain. Native picker/MVI remain
shared, without introducing an iOS screen. Domain/data use
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
- [x] Add api/impl entries and typed list/detail MVI. Preserve debounce, query
  limit, refresh/pagination, cancellation, filter draft/apply/reset/retry and
  stale-result protections. No meaningless one-call use cases.
  - [x] Existing picker public contract and manual repository-to-API factory.
  - [x] Typed state/action/event and lifecycle MVI implementation prepared.
  - [x] Execute MVI/picker contract tests, including out-of-order and cancelled results.
  - [x] Add actual renderer/screen entry contracts with their Compose implementation.
- [x] Extract components into compose/component, state/action/events into model;
  stateless ScreenContent, colocated light/dark previews and localized resources.
  Preserve sizing/spacing/image treatment and offer/details behavior.
- [x] App-owned analytics ports (2026-10-19): `CoffeeAnalytics`/`CoffeeListAnalytics`/
  `CoffeeDetailAnalytics` (commonMain) and `CoffeeScreenAnalytics` (androidMain) in the api module;
  list/detail ViewModels and screens report search start/results/failure/open position, filter
  open/apply/reset, detail content interactions, detail view and exposure through them (defaults are
  silent). `composeApp/.../di/coffee/AndroidCoffeeAnalytics.kt` adapts them to the existing
  `SearchTracker`/`Analytics`/`DetailEntryMemory` with the legacy event names. Unit-tested (impl ports
  test, adapter test); entries are **still not activated** — Koin/routes/picker remain item below.
- [x] Android activation (2026-10-19): `CoffeeScreenRenderer` (commonMain, Legacy default for
  iOS/shared) used by MainScreen's Coffee tab and Navigator's CoffeeDetail routes (incl. restored);
  `di/coffee/CoffeeModule.kt` provides the migrated repository/picker over the plain client and
  `AndroidCoffeeScreenRenderer` owns navigation, browser, share, gallery and floating-nav clearance.
  Android Home/history catalogue ports now consume the migrated `CoffeePicker`. Legacy composeApp
  catalogue code stays for iOS. Verified by unit/composition tests, Direct/Play/Sandbox assemblies,
  instrumentation and iOS compilation; **not** checked on a device or in the full Sandbox walkthrough.
- [~] Sandbox device walkthrough (2026-10-19, Pixel 7 / API 37, Direct Sandbox 1.0.746, updated in
  place; ordinary CoffeePeek untouched): Coffee tab list, search (empty state and match), filter sheet
  and badge, detail, photo gallery open/close, roaster route and back, share (blocked by Sandbox's
  external-app guard, as designed), dark and light themes. No crash in this run. Manual smoke only:
  no populated/slow/error scenarios, large font, pagination, restored-route or picker return checks,
  and no live API/browser/share-target verification. A 2026-10-20 follow-up with richer Sandbox fixtures
  covered taste/roast/available-only filters, offers and similar coffees (see composeApp/SANDBOX_PLAN.md).
- [x] (superseded wording) Wire Android tab/detail/restored routes, picker and app-owned analytics using
  factories. Keep shared/iOS fallback until its consumers deliberately migrate.
  Preserve SearchTracker's normalized-query/filter fingerprint, consent/session
  generation and result/open positions without importing an SDK into the feature.
  MVI logic prepared in this slice has no telemetry adapter yet; do not activate
  it in the app until search/detail/exposure analytics parity is explicitly wired
  and tested. Existing legacy analytics stays active and unchanged for now.
- [x] Synchronize main 28917758 (including #105–107) through the parent stack.
  Preserve app-owned DI, protected photo/session logic and fake Sandbox transport.
- [ ] Close current-main Home/history photo-variant/original/calendar and migrated
  author-blocking-menu gaps. Coffee's entry gallery uses the same app-owned
  viewer; its feature does not introduce a parallel transport/cache. Current main
  links cover shops/roasters/users; no Coffee app-link is invented here.
- [ ] Verify catalogue/picker/roaster return, browser/share/photo cancellation,
  empty/error/slow/out-of-order data and long/scaled text in fake UI/full Sandbox.
- [x] Publish a child PR (#104) targeting `feature/shared-rating-component`, with only
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

### Parent-first main synchronization — 2026-10-19

Main 28917758 is inherited from #43 through all open parents into Coffee source
32cc4d57. The audit/plan changes after that revision are documentation only.
All library Android unit suites and both app variants pass: **1704 results**,
zero failures/errors/skips; four build-logic checks pass separately. Direct/Play
Debug APK/DEX, Direct Sandbox isolation/DEX, both app test APKs and Coffee/Shop/
Direct-app lint pass. Full app Kotlin compiles for iOS Simulator Arm64, not an
iOS feature/UI test. Initial broad packaging failed with D8 Java heap exhaustion;
the sequential retry with a temporary 5 GiB heap passes without project-setting
changes. No device deployment, fresh UI run, real account or backend write.
Current-main Home/history photo variants and migrated blocking menus remain
explicit prerequisites; Coffee analytics/routing/picker bindings are still inactive.

### Android UI slice — 2026-10-18–19

Prepared actual `CoffeeListEntry` / `CoffeeDetailEntry` and manual implementations,
lifecycle-owned Screen adapters and fake-state ScreenContent. Cards, prices,
labels, search header, filter content/sheet, offer cards, intro and roaster row
have their own files and colocated light/dark Android previews. Russian strings
are moved into feature resources; current app/Coffee sources have no second
translated locale, so no English localization is claimed. Search, grouped rows,
switches/checkmarks, refresh, segmented weights, loader, currency, glass controls,
icons/theme and brand placeholders reuse design-system. The existing outlined
detail card is prepared there as CoffeePeekContentCard; grouped sections now
support the existing titleless variant without an empty header gap.

App supplies image loading (including Fit/Crop and failure rendering), gallery,
localized dates, navigation and platform share/browser callbacks. No Koin,
HTTP client, root navigator or production analytics SDK enters the new UI.
No active Android route, Sandbox binding or iOS screen is switched. Search and
detail analytics are still a required next integration slice, not an implicit
side effect of creating these entries.

Direct/Play Debug and Direct Sandbox assemblies pass with packaged DEX/isolation
guards; ordinary CoffeePeek remains 1.0.648/code 648 on Pixel 7. App unit suites
execute 450 + 450 cases; explicit fresh feature/core unit execution passes
4 domain + 14 data + 41 impl + 23 design-system cases, **982 relevant executions**,
zero failures/errors/skips. Three impl cases preserve price formatting. Both
api/impl and the new shared design-system code compile for Native arm64 and
Simulator arm64; this is compatibility, not iOS UI acceptance.

New UI tests are self-targeted at `com.coffeepeek.feature.coffee.impl.test`
(target API 36) on Pixel 7 `panther`, API 37; no production Application, account,
network permission or external actions. Fake entry tests use the real lifecycle
ViewModels and injected repository/image/gallery ports. The first content run
had 7/48 harness failures: four merged-tree image lookups and three controlled
search tests not feeding typed query back into state. Corrected the node lookup
and controlled state without changing expected dimensions or actions; the full
expanded 51-case suite then passed. Final modal Back/Done and post-lint evidence
is recorded below. Design-system UI regression passes **71/71**, first pass,
including titleless group height and exact outlined-card padding.

Studio's new Android source-set classpath is not synced to its Gradle convention
change, so unresolved IDE references are not counted as a clean feature inspection.
CLI compilation succeeds; CoffeePeekContentCard IDE error inspection is clean,
grouped rows have existing preview warnings, and the icon inspection timed out.
IDE preview rendering, pixel-identical live catalogue acceptance, platform
gallery/browser/share and actual server/media acceptance remain separate gates.
Shared controls retain their design-system accessible hit areas; this slice does
not certify exact 44/48dp legacy-control pixel parity. Product photo height,
main card/detail spacings and type roles retain the baseline treatment.

Before the subsequent card-builder request, the final post-lint Coffee APK
`8fa5edd081c05f4de8aafc88a5f9c2b0bfed6f35a054c59721edf2761fa3eb22`
passed **52/52** self-targeted UI/entry cases (including actual filter-sheet
Back discarding the draft and Done searching once). The preceding expanded
52-case run also passed. Core's 71-case APK was
`474adb05f927e9ca2836b9ce6265709cd772e0ba123fb31af077baa318958d73`.
Only the light-list fixture with real Manrope was manually inspected; the test
host's light-theme system-bar icons are not application window-style acceptance.
A later detail fixture launch delivered an intent to the existing Activity and
did not constitute a new dark-detail visual check.

### Requested card builder / readable intro / photo-heart correction — 2026-10-19

`CoffeePeekCard` is the shared declarative builder: full-bleed header, padded
body and optional footer; click is optional. CoffeePeekContentCard is its
outlined preset, not a second copied card implementation. Coffee card uses
photo/summary/offer-footer components, retaining the 216dp photo, existing
section spacing and nested weight-selection behavior. Business models stay in
the feature. CoffeeDetailIntro now only assembles heading, expandable description
and traits; generic controlled expandable text and read-only value/caption
rendering live in design-system. No mutable builder registry, framework-specific
domain model or new module is introduced.

Photo favorites now use the shared Glass style, a translucent black tint and
subtle rim, rather than solid black containers; neutral and selected gold glyphs
remain unchanged. Tint retains sufficient density for the glyph over bright
photos. A Haze source enables backdrop blur; absent a source this is explicitly
a translucent glass-style fallback, not a claim of real blur. Plain style has
no plate and remains the default away from images. Passive markers use the
read-only CoffeePeekLikeBadge, never a disabled/fictional toggle. Normal app
catalogue/public favorite cards and migrated Favorites delegate this appearance;
auth, request handling, analytics and persistence are unchanged.

Final Android builds/DEX/Sandbox guards, both app unit suites and feature/core
unit suites pass; api/impl lint has zero issues. Both complete Native app targets
compile for shared-code compatibility, not iOS screen acceptance. Studio error
inspection of CoffeePeekCard, CoffeePeekExpandableText and CoffeePeekLikeButton
succeeds. Pixel 7 initially disconnected, then was reconnected/unlocked by the
user. Final self-targeted suites pass **143/143**: core **77** (four new
builder/text and two glass/badge contracts), Coffee **52**, Favorites **14**.
No failures/skips or assertion changes in this component slice. Core's initial
77-case run also passed; after an import-only rebuild the complete final 77-case
suite was repeated and passed against the exact final APK below. Coffee's 52
and Favorites' 14 cases then passed on their final APKs. Earlier UI harness
failures are historical and remain recorded above, not hidden by this success.

Manually inspected Glass/Plain with selected/unselected hearts on synthetic
white, coffee-brown and black backgrounds in both themes. The plain white
outline vanishes on a white photo, so Glass is the photo default; Plain remains
available for non-photo surfaces. Also inspected the slot-based dark list/card
and the simplified dark detail intro with real Manrope. Test-host system-bar
styling/fixture header overlap is not production window-style acceptance;
actual photo blur with a Haze source, live image loading and exact product
pixel parity remain integration gates. Screenshots stay local.
No ordinary app or Sandbox update, server/account write or device setting change
was made by this slice. Build identity on top of `d0ec1930`:

- Core test APK SHA-256: `c7b0f23ed4c92616722e28675d8ad31e32e096811985ee9914fee70c2be081d6`.
- Coffee test APK SHA-256: `370a6e4c73e6394db3a93fa7e6aed7d9a121bbabd82a1f236ef666fc226c61dd`.
- Favorites test APK SHA-256: `8a2e555e4f06295acaeb0071d0ffa4407e2f2434640dc344172b2e5d6218cb27`.

MVI slice prepared on top of `1ba91f8d`. Remote main rechecked at `0137ee7`.
Prepared public picker API, its manual factory and shared list/detail ViewModels
with typed models. No Compose screen, app Koin, Android entry, Sandbox binding
or iOS screen is switched by this slice. State flags replace raw UI error strings;
Result failures and cancellation remain feature-local. No new use-case wrappers
or DI modules. Planned tests include debounce/query bounds, filter draft apply/
cancel/reset, refresh/page failures and duplicate guards, stale non-cancellable
responses, independent detail/labels/similar reads, lifecycle cancellation, safe
navigation/media state and narrow picker results.

2026-10-18: **38 new impl unit cases execute successfully** (17 list, 16 detail,
five picker), zero failures/errors/skips. Domain/data remain green (four + 14),
**56 Coffee feature tests** in total. Both new api/impl libraries compile for
Android and both Native targets. First test compilation failed in two assertions
because singleton/derived expected-event lists needed the explicit sealed-base
type; corrected the test types without changing assertions, then reran the full
selection successfully. No product assertion failed. Raw exceptions are not
rendering state; Result cancellation is rethrown, including cancellation returned
incorrectly as a failure by a port. No backend/DTO/DI/navigation imports enter
these presentation models.

Studio reports these new files outside imported content roots until Gradle Sync;
CLI compilation/tests are the evidence here, not a clean IDE inspection. App
Direct/Play Debug + Direct Sandbox builds pass with packaged DEX and Sandbox
isolation guards; existing app unit suites execute **450 + 450** cases, all pass.
Together with the feature selection this is **956 relevant unit cases** with no
runtime test failures/errors/skips. App verification completed in 3m33s; the full
feature retry completed successfully after the two test-compilation fixes.
No device tests or app installation are needed/claimed for these inactive pure
presentation models; the new UI/analytics/app-binding gates remain open.

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

## Retroactive parity audit — 2026-10-20

Open gaps from the legacy-first audit: C1–C4 (medium: C1 mascot load-error slot). Full table and verification status: [LEGACY_PARITY_AUDIT.md](../LEGACY_PARITY_AUDIT.md).

Fixes applied 2026-10-20 and owner decisions on intentional differences: see the "Fixes applied" and "Intentional differences" sections of [LEGACY_PARITY_AUDIT.md](../LEGACY_PARITY_AUDIT.md). Device verification is still open.
