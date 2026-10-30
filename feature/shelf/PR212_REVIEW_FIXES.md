# PR #212 shelf review fixes — 2026-10-30

The imaging component flood now delegates its bounding-box update to a short helper. Archive restore and delete outcomes refresh page 1 so shifted items remain reachable before later pages are loaded. The archive continues to show the outcome message and uses the current query.

Verification source: `origin/develop` at `1f09980b6`. Shelf data ran 37 tests in 7 suites and shelf implementation ran 57 tests in 8 suites, with zero failures, errors or retries. `:composeApp:assembleDirectSandbox`, packaged DEX class and Sandbox isolation checks passed for `com.coffeepeek.sandbox`. Kotlin iOS Arm64 compilation of shelf data and implementation passed. No device was selected or app installed. Manual archive paging, light/dark, accessibility and native iOS review remain open; no live account data was changed.
