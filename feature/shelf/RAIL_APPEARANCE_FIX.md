# Rounded shelf rail — 2026-11-02

The shared `ShelfLedge` rail uses a corner radius equal to half its 40 dp height,
giving both ends a semicircular outline. The decorative fastener layer is
removed. This applies to populated, empty and guest shelves on Android and iOS.

Verification plan: compile the shared shelf UI and Sandbox application, execute
shelf presentation tests, and inspect the existing paired light/dark and opaque
material previews. Shelf placement and drag geometry must remain intact.

Verified from `origin/develop` `94dc6d160` plus this scoped change:

- `:feature:shelf:impl:testDebugUnitTest`: 58 tests in 8 suites, zero failures,
  errors, skips or retries.
- `:composeApp:assembleDirectSandbox`, Sandbox isolation and packaged DEX class
  checks passed. No device was selected and no app was installed.
- Existing paired previews compile. Their legacy Preview annotation emits the
  existing deprecation warning. This project is not open in the connected IDE;
  preview rendering, manual Android/iOS visual review and native iOS compilation
  remain unverified locally.
