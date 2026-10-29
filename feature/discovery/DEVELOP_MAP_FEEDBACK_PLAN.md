# Navigation and map feedback on develop — 2026-10-29

User-authorized scope: adapt feedback items 2, 4 and 5 to the active develop
application and merge them into develop. Do not merge the pending iOS/Discovery/
Contributions migration stack. Starting revision: `7796d34d` on
`codex/migration-map-feedback-develop`. The original migration checkout and its
uncommitted changes remain separate.

## Ownership and behaviour

- Item 2: the selected Coffee tab requests scroll-to-top. Android keeps the
  existing feature/coffee androidMain entry; iOS keeps its current shared
  legacy Coffee screen. A root-scoped non-replaying signal preserves query,
  filters, navigation state, lifecycle and Reduce Motion behaviour.
- Item 4: list and map share text, filter selections and user origin. Shared
  Discovery domain/data call POST `/api/v1/coffee-shops/map` using the same
  filter mapping as list search. The endpoint returns points/clusters/zones;
  there is no silent fallback to the legacy unfiltered GET. Preserve account
  isolation, favourites invalidation and cancellation. The map camera must not
  replace the Nearby origin. Android's migrated feature and iOS's current
  screen are adapted at their existing boundaries.
- Item 5: retain card details through selection changes, preserve image/hours
  geometry and keep controls clear of the carousel. Do not include unrelated
  search-order, menu, root gesture or migration changes.

The paired backend change is CoffeePeek/coffeepeek-backend#369, targeting
develop. It shares the query predicates before list pagination or map bounds/
aggregation and retains GET `/api/Map` for old clients. This mobile version
requires the new backend endpoint to be deployed before release.

## Verification plan

Execute affected Coffee/Discovery/app unit and HTTP/Sandbox contracts. Compile
Android Direct Debug and Direct Sandbox with packaged DEX and Sandbox isolation
checks. Compile Kotlin iOS production/native tests locally; GitHub iOS CI checks
Swift/Xcode and the selected native contract tests. No automated UI,
instrumentation or screenshot tests will be added or run. No app install,
real-account mutation, production test telemetry or backend write is planned.

Manual device checks remain separate: repeated tab taps, search/map round trips,
filter apply/reset/cancel, card swipes, light/dark themes, font/window sizes,
accessibility and native iOS interaction. A new checkout needs its own evidence;
the original migration branch's test counts do not certify this adaptation.

## Results

The first integrated pass on the develop base plus this patch succeeded on
2026-10-29: 14 Discovery domain, 22 Discovery data, 88 Discovery presentation,
46 Coffee presentation and 848 application unit/contract tests (1,018 total,
zero failures or skips). Direct Debug and Direct Sandbox APK assembly passed,
including packaged runtime-class and Sandbox isolation checks. Kotlin iOS
Arm64 and simulator production code and simulator test sources compiled.
Native test execution and Swift/Xcode validation require the macOS CI runner.

The first compilation exposed two cross-module smart-cast errors; local
variables fixed them. A subsequent test observed an immediate new nearest-shop
response before checking the cleared state. It now explicitly holds and then
releases that response, asserting both invalidation and replacement results.
Original failed logs are retained locally. Existing preview/expect-actual and
test opt-in warnings remain outside this scope.

Review additionally found missing iOS favourite-membership/account invalidation
and variable rating-row height. The fixes clear results, selection, details and
the loaded-area cache; account/generation guards reject obsolete replies. Three
additional tests cover membership refresh, account A-to-B viewport replies and
late card details. Both card renderers reserve the same font-scale-aware rating
row height. The final pass reran Discovery presentation (88) and application
tests (851), all passing without skips, then rebuilt both Android APKs with
runtime/isolation checks and recompiled iOS production and test sources. The
unique local suite total is now 1,021 passing tests. Read-only peer review
confirmed all three findings closed.

The local source is develop `7796d34d` plus this scoped patch; PR CI identifies
the published commit. Native execution/Swift validation is recorded by the PR
checks. No application was installed or launched, no device/UI acceptance was
claimed, and no real account or backend data was changed.
