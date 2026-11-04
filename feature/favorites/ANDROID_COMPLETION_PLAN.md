# Favorites — current Android completion checklist

Updated 2026-10-15 against main 4976e53. This replaces the historical seven-PR
ordering and old task names. Global scope: ../ANDROID_REFACTORING_PLAN.md.

## Scope / branch

Work remains in parent PR #43 (`feature/core-design-system-controls` → develop).
History is a separate child #95, not a place for half of this favorites fix.
Publish parent corrections and explicitly update descendants. Use the agreed
100–150-file checkpoint guide for related work; do not reopen obsolete #49/#53/
#57/#67/#68 checkpoints or create a PR for each small fix.

No iOS screen, binding, native navigation or feature migration. The user explicitly
approved only a shared store safety fix (expected owner plus cancellation) while
preserving the existing JSON and callers. Do not delete shared legacy consumers.

## Implemented and automatically verified

- [x] New Android screen uses pure favorites domain contracts, typed MVI and
  app-owned DI/legacy adapters, without a root navigation framework change.
- [x] Saved shops and saved roasters are both rendered, including roasters-only
  content. Loading/errors from one source do not erase the other source's cards.
- [x] Match main's roaster section/card geometry, count labels and theme colors.
  Preserve stored logo, catalogue-photo fallback and factory placeholder.
  Stateless components and screen content have colocated paired theme previews.
- [x] Distinguish shop/roaster lazy-list keys even when IDs match. A heart is a
  separate 48dp action; pending writes disable only the affected removal.
- [x] Navigate through existing root callbacks using the roaster's public slug.
  Missing addresses are not converted into invented routes; removal remains usable.
- [x] Reuse the existing account-owned RoasterFavorites writer and persistence
  format. No duplicate store/repository endpoint or feature DI module.
- [x] Check the expected account under the existing writer mutex. Preserve Result
  failures, reject stale-owner writes and propagate CancellationException.
- [x] Observe external changes; retain cards on failed removal and block duplicate
  writes. Bound fallback reads to four and reject late previous-account results.
  Same-account token renewal keeps current reads.
- [x] Both Android debug APKs and packaged DEX guards, both app unit variants,
  favorites domain/data/impl tests, app/feature lint and instrumentation APKs pass.
  This checkpoint has 449 selected unit-test executions, zero failures.
- [x] All 12 isolated favorites UI tests and three app route-adapter UI tests pass
  on the temporary read-only emulator. Fixtures use no account/backend writes.
- [x] Shared code compiles for iOS Simulator Arm64; this is not iOS feature QA.

Existing shop Room/legacy migration fixtures and cancellation/session-cleanup tests
are retained. Their older device results are dated evidence, not a fresh signed-in
journey or a claim that this checkpoint ran every historic migration on a phone.

## Remaining gates — not completed

- [ ] Safe live account journey with pre-existing shops **and roasters**: open from
  profile/discovery, inspect actual logos/counts, navigate to each detail, remove
  a disposable item and return. Verify observation agrees across all current
  consumers. Backend writes/removal of real saved rows require explicit safe QA.
- [ ] Account A/B replacement, guest, expiry and logout at the real integration
  boundary, including in-flight logo/removal work. Preserve main's account-scoped
  roaster persistence; do not invent a new policy that deletes it on logout.
- [ ] Process/activity restoration with actual persisted rows; real photo failures,
  long names, OS large font/RTL, accessibility focus/order and screen-reader output.
- [ ] Location already allowed/denied: existing shop distance behavior, no new
  permission prompt, no invented location/distance for roasters.
- [ ] Actual IDE preview rendering and physical-device comparison against main.
  Fake-theme screenshot/test inspection is not a production screenshot comparison.
- [ ] Confirm supported product locales. This feature has default Russian resources;
  no unverified second translation or locale promise.
- [ ] Final release build/required CI and safe integration smoke, after the remaining
  gates. Local checks do not imply the newly triggered CI has finished.
- [ ] Remove legacy code only after every platform consumer has migrated; until
  then retain the shared screen/contracts and FavoriteSync consumers.

## Not part of this fix

Root Navigation 3 migration, iOS presentation/storage migration, new favorites
HTTP synchronization, generic core DI/storage modules, catalogue/roaster rewrites,
new product sections and resurrection of removed/unused screens.

## Retroactive parity audit — 2026-10-20

Open gaps from the legacy-first audit: B1–B6 (medium: B1 saved-shop card, B2 drink heart exposure/size). Full table and verification status: [LEGACY_PARITY_AUDIT.md](../LEGACY_PARITY_AUDIT.md).

Fixes applied 2026-10-20 and owner decisions on intentional differences: see the "Fixes applied" and "Intentional differences" sections of [LEGACY_PARITY_AUDIT.md](../LEGACY_PARITY_AUDIT.md). Device verification is still open.

## Initial loading and scroll anchoring — 2026-11-04

This supporting correction belongs to the active `codex/migration-saved-collections`
checkpoint, after profile-display integration `af2d23a2`; the older branch/test
records above remain historical. It does not change iOS favorites or any writer.

The Android destination receives three independent shop, roaster and drink flows.
Previously, the first nonempty result replaced the full-screen loader with a lazy
list immediately. If a lower section completed first, its stable item key became
the first scroll anchor. Later shop results were inserted before that anchor,
leaving the user below the beginning of the new list without an explicit scroll.

Independent integration review found a second part of the original cause: all
three production adapters emitted a synthetic empty success before the real
server/cache hydration. First-result tracking alone would therefore release the
skeleton too early. The adapters now suppress only the first owner-clear of a new
observation, hydrate authoritative server favorites before publishing a real raw
success, and retain later owner-revocation clears for existing consumers. Those
composition changes are verified by the integrating root, not by presentation
fixtures alone.

The ViewModel records when every initial authoritative source has returned a
result, including empty/error results. Until then, an independent non-scrollable skeleton
shows cafe and roaster shapes in section order. It never shares the real list's
scroll state or keys. The destination holds its real `rememberLazyListState`
outside the loading/content branch: a new destination starts at the top, while
Back from detail preserves its existing place. No resume/refresh scroll-to-top
effect was added. Retry retains cached cards and the initial-settlement marker;
an empty section being retried receives its own placeholder without replacing
the populated list. Existing removal guards, Result/cancellation handling,
navigation events and analytics/exposure ownership are preserved. A narrow pure
`FavoritesAccount` port is supplied by application composition. A distinct owner
change clears all three sections and starts a fresh generation; null leaves an
empty signed-out state without starting observers. Same-owner retry retains
cached content. Reads and pending removal completions verify both the generation
and current owner, including a current-ID change arriving before its Flow callback.
Removal completion uses an owner epoch separate from observation/retry generation,
so a same-owner retry cannot strand a pending heart or discard its error.
Callers without the optional port retain their earlier behavior.

No shared shimmer/skeleton primitive existed in the current design-system API.
`CoffeePeekShimmer` supplies one reusable neutral shape, with theme colors and
no new dependency. It becomes static when `animate=false` or inherited Compose
`MotionDurationScale` is zero. The loading container supplies the existing RU/BE
loading description once; decorative shapes expose no duplicate semantics.
Favorites resource keys remain aligned at 23/23. Paired light/dark previews cover
the primitive, cafe/roaster skeletons, initial/partial loading and cached retry.

Verification plan: execute the six initial-load ViewModel cases for independent completion
order, empty/error settlement, cached retry, detail navigation and fresh-entry
state, plus seven account cases for warming, replacement, same-owner emissions,
sign-out, late result/write rejection, stale removal completion and same-owner
retry during an active removal. Run existing
favorites operation/analytics and production-adapter regressions, then assemble
the exact requested Debug variant with packaged DEX checks. The integrating root
records executed counts, build/source identity and the agreed device below.
Manual gates remain actual initial loading/top position, Back-from-detail scroll,
light/dark, font/window sizes and reduced motion on the explicitly selected app.
These changes did not create or run automated UI/instrumentation/screenshot tests.

Executed shimmer verification on 2026-11-04: design-system common metadata and
Android compilation passed (30s); favorites impl31 unit cases passed (17s);
selected application adapter/cache/analytics cases55 passed after the fixes below.
Final Direct Debug assembly and packaged DEX checks passed in the same app
verification invocation (52s). These executed suites verify state/contracts,
not animation rendering or complete manual accessibility.

The first application attempt failed to compile a new test's direct serializer
plugin imports; app tests now reuse core/network's configureApiTransport instead
of adding dependencies. Its log is retained locally as
build/favorites-shimmer-first-compile-failure.log. The next run executed55 cases
but the cancellation case timed out: mapLatest cancelled its transform child
without terminating the hot subscription. The normalizer now uses ordinary
suspending map; the raw-failure and cancellation assertions remain and also cover
CancellationException returned by getFavorites. That original log is retained in
build/favorites-shimmer-cancellation-failure.log. The subsequent55-case run passed
with zero failures/skips. The real ServerFavoritesRepository/MockEngine case
checks three normalized subscriptions share one RPC, and cached resubscription
adds no RPC. Production resets and delayed hydration are exercised separately
from the earlier presentation-only fixtures that masked synthetic empty values.

Direct Debug1.1.5.2683/code2683 (package com.coffeepeek) was installed in place on
the explicitly requested Pixel_8 AVD, emulator-5556, API37/x86_64 with16KB pages.
APK SHA-256 B3EDF57EC9CDEC602F921C50FB4B5BA6B416D16185A1B2198142F13784A2E0E2;
source is af2d23a2 plus this loading patch. Installed/built signing certificates
matched; first-install time2026-11-04 09:20:28 remained unchanged. MainActivity
foreground was confirmed; launch state was UNKNOWN/warm and is not claimed cold.
No clearing, downgrade, uninstall, favorite mutation or account replacement
occurred. Read-only manual navigation opened Profile then All Favorites and
confirmed the destination/header and rendered content. The fast live request did
not provide captured shimmer-animation evidence; initial geometry, Back/scroll,
dark/font/window and reduced-motion review remain explicit manual gates. UI dumps
remain ignored/local and contain no committed account identity or screenshots.
