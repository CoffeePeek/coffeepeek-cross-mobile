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
  This checkpoint has 447 selected unit-test executions, zero failures.
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
