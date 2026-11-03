# Rounded shelf rail — 2026-11-02

The shared `ShelfLedge` rail uses a corner radius equal to half its 40 dp height,
giving both ends a semicircular outline. Each end now has a small flush silver
fastener. This applies to populated, empty and guest shelves on Android and iOS.

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

## Flat silver fasteners — 2026-11-02

Source: `origin/develop` `a43f96347` plus this patch, branch
`codex/migration-filter-be-shelf-fasteners`. Each rail has two 8 dp silver
heads with a thin metal rim and recessed cross slot. They are decorative Canvas
drawing on the rail's face; the rail keeps its 40 dp height, 8 dp bag overlap
and existing drag/landing geometry. The four colocated light/dark and opaque
material previews include the new hardware.

Verification: all 58 existing shelf presentation tests / 8 suites passed
without failures, errors or skips. Shared shelf UI compiled for iOS Arm64;
Direct Sandbox assembly and DEX/isolation checks passed
(`1.1.5.2662-sandbox` / 2662). Existing Preview deprecation warnings remain.
Previews compiled but were not rendered in an IDE. No device was selected or
app launched, so light/dark, transparency and drag visual review remains a
manual device gate. No UI or screenshot tests were added.
