# Android notification navigation — 2026-10-25

Source: `codex/migration-notification-navigation`, based on develop `2748f07a`.
Backend companion: CoffeePeek-NET `codex/notification-navigation`, commit `1269fa6e`.
The backend branch is retained locally in that repository; no service was deployed.

## Scope and verification plan

Open the current shop card for newly published shops and moderated corrections,
including already-read inbox rows. Preserve push preferences, permission timing,
read snapshots, account fences, analytics/session consent and ordinary app links.
Notifications owns transport/presentation; application composition adapts contribution
reads and root navigation. No new Gradle modules or domain infrastructure dependencies.
Existing notification flow has no tap analytics event; this change adds none. Existing
native receipt refresh, notification channel, foreground/session and consent behavior stay.

Verify destination/read contracts, current public slug versus submission ID, account
changes during requests, cancellation, retry and native tap handoff with unit/contract
tests. Compile actual Android variants and review the modal/navigation manually using
only the existing isolated Sandbox with synthetic accounts/data. No automated UI,
instrumented Compose or screenshot-test infrastructure is introduced or executed.

## Automated checks

- Notifications impl: 22 executed tests in 3 suites; data: 4 in 1 suite.
- Application DirectDebug: 818 executed tests in 163 suites.
- Total: 844 tests, zero failures/errors/skips. New coverage includes destination
  decoding, correction-to-shop routing, published submission pagination, unavailable
  targets, account replacement, coroutine cancellation and retained native requests.
- Notifications impl lintDebug passed. Existing unrelated Compose preview/experimental
  API/test warnings remain. Android Studio tools were unavailable; IDE inspections and
  rendering of colocated light/dark fake-state previews were not performed.
- assembleDirectDebug and assembleDirectSandbox passed, including both packaged DEX
  runtime-class checks and Sandbox isolation. No concurrent Gradle runs; max-workers=6.
- Backend: 23 focused Account destination/inbox/Gateway tests passed, zero failures/skips;
  AccountService Release build passed with existing nullable warnings. Ownership and
  signed Gateway identity, hidden/renamed shops and no read side effects are covered.
  Gitleaks 8.30.1 checked staged changes and all reachable history with no findings.
- Retained earlier failures: first app compile used the legacy session adapter with
  SessionReader; corrected at composition. The next test compilation needed explicit
  Screen list types in three new assertions. Both were compilation failures, not
  failing executed test cases. Final complete Gradle pass succeeded in 19 seconds.
  A subsequent host readiness fix waits until notification state matches the active
  session; application tests and both assemblies/DEX checks passed again in 18 seconds.
- git diff --check passed. New/substantively changed functions were reviewed against
  the 20-line ordinary / 40-line composable limits; no build-enforced limit was added.

## Manual Sandbox review

Read-only Pixel_5 emulator, API 37, model sdk_gphone16k_x86_64. Installed exact
DirectSandbox output from this branch, package com.coffeepeek.sandbox, version
1.1.1.2450-sandbox/code 2450. The Sandbox package was absent before installation;
the real com.coffeepeek package was not modified. Manifest has no INTERNET permission.
Local captures and UI dumps are outside tracked sources and contain synthetic data.

Confirmed light/dark inbox placement, actionable chevrons and read/unread styling;
correction opens Sandbox Cafe and closes the sheet, reducing the unread count from
6 to 5. A pre-read notification also opens Sandbox Cafe. A warm native intent opens
Sandbox Lab, and a cold launch with a correction intent opens Sandbox Cafe. A missing
shop displays a dismissible error while retaining the previous card. Back returns to
the feed. These are manual fake-data checks, not actual FCM delivery or live acceptance.

## Rollout and remaining gates

Deploy the additive Account destination endpoint before this client. NewShop resolution
uses Shops' existing trusted internal public-address endpoint and service discovery;
existing notification rows need no migration/backfill. Corrections read the current
shop slug from the owned change request. Unpublished shop decisions fall back to own
contributions; roaster/check-in decisions retain their respective history destinations.

Real FCM foreground/background/terminated delivery, backend deployment integration,
TalkBack, alternate font/window sizes, and native iOS build/navigation are unverified.
Android previews compile; preview rendering is unavailable. iOS UI/writer paths are
unchanged. No real account writes, production analytics or camera/network writes occurred.
