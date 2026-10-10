# Favorites: Android completion plan

This plan finishes and verifies the Android favorites migration without changing
the iOS screen, binding, writer, or shared legacy code still needed by iOS. It
continues from the current migration tip. Related steps can share one checkpoint
PR; the working split point is about 50 changed files in its diff. Each step
must be verified before moving on even when it does not get its own PR.

## Progress

- [x] Synchronize local `main`, all seven open checkpoint branches, and the
  current work branch from `origin/main` without changing PR diffs or merging
  anything into `main`.
- [x] Verify the synchronized Android baseline with
  `./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest
  :feature:favorites:data:testDebugUnitTest
  :feature:favorites:impl:testDebugUnitTest --no-daemon` (passed).
- [x] Create `feature/favorites-android-finalization` from #67 and carry the
  rules and plan forward. The published #68 remains unchanged while favorites
  work is paused there.
- [x] Map Android favorite readers and writers in step 1.
- [x] Decide and test session-cleanup failure behaviour in step 1. All logout
  paths continue after favorite-storage failure; the failure is returned to
  explicit logout / shown on forced logout, disk-cache cleanup still runs, and
  cancellation still propagates.
- [x] Prove historical Room persistence compatibility in step 2: v1 and v2
  fixtures both migrated to v3 on Pixel 10a / Android 17; saved favorites were
  readable through the new repository and legacy adapter, mutations persisted,
  and clear remained clear after reopening.
- [ ] Verify the complete Android user journey in step 3. Unit tests now cover
  late membership updates in feed/detail, signed-in feed-heart writes and
  optimistic rollback, shop-detail add/remove round trips and failed writes,
  and destination callbacks. Feed and shop-detail ViewModels now suppress
  overlapping heart writes per shop, and the feed disables that heart while a
  write is pending. Logout, location-permission states, and process recreation
  are not verified end-to-end.
- [ ] Finish UI, accessibility and locale QA in step 4. The favorite-card
  removal target is now 48dp and only Russian favorites resources exist;
  visual/screen-reader QA and product locale confirmation remain open. The
  latest UI instrumentation run had 2/7 tests lose the Compose hierarchy while
  the test Activity was not foreground.
- [ ] Perform Android-only cleanup and the final gate in step 5.

## Baseline and boundaries

- The Android root still uses Navigation 2. Its Favorites destination already
  displays the new feature screen through `FavoritesEntry`; the Navigation 3
  feature entry is prepared but is not the application root.
- One Android `FavoritesRepository` singleton backs the new screen and the
  temporary old-contract adapter. Its stored JSON key and Room settings table
  must remain compatible with existing installations.
- The old iOS screen and writer continue to use the shared legacy contracts.
  Do not delete `FavoriteSync`, legacy models/repository code, or shared routes
  merely because Android no longer renders the old favorites screen.
- No new favorites HTTP API, generic DI module, second storage writer, or new
  application module is required.

## 0. Put the work in the intended PR order

The earlier `feature/favorites-android-completion` branch descended from
`feature/shop-report-domain-data` (#68). The active
`feature/favorites-android-finalization` branch now starts at
`feature/design-system-colocated-previews` (#67), so its eventual PR can merge
before #68. Keep #68 open but pause further shop-report work. Once favorites is
ready and merged, retarget #68 to `main` and verify its diff and tests. Do not
force-push or delete the earlier work branch until all content is accounted for.

Exit: favorites can be reviewed and merged before shop-report; #68 still has
only its intended shop-report diff against its updated base.

## 1. Establish a reproducible Android baseline

Audit findings so far: Android DI disables `dataModule`'s legacy favorite writer
and registers one `FavoritesRepository`, its old-contract adapter, and the
screen entry from the Android Room settings storage. Feed and shop details still
write through the old contract but observe the new repository's membership flow.
`ShopRepositoryImpl`, `AuthRepositoryImpl`, and session cleanup also consume the
old contract, which resolves to that adapter on Android. iOS keeps the old writer
and old favorites screen; `FavoriteSync` is still part of that shared path.
No second Android writer has been found.

1. [x] Check the current Android app build and the affected feature, bridge,
   and app tests. Record exact commands and results below.
2. [x] Audit every Android consumer of favorite reads, writes, membership and
   session cleanup: the new screen, feed, shop detail, `ShopRepositoryImpl`,
   `UserSessionCleaner`, explicit logout and forced logout. Verify that Android
   DI resolves a single new repository plus its old-contract adapter, that the
   old Android writer binding is disabled, and that no direct writes bypass the
   adapter. Record which shared consumers remain for iOS before cleanup.
3. [x] Capture the current navigation and data behaviour with existing tests
   before changing it; avoid a root navigation rewrite in this step.
4. [x] Define and implement the failure policy for session cleanup. The cleaner
   always attempts disk-cache cleanup; explicit logout reports the cleanup
   failure but continues Google sign-out and review-draft cleanup; forced
   logout reports the cleanup warning while continuing sign-out/navigation.
   The Android favorites binding is unchanged; shared behavior was regression-
   compiled and tested for iOS Simulator.

Exit: a documented consumer/writer map, a green baseline or explicit failures,
no unaccounted second Android writer, and an explicit logout failure policy.

## 2. Prove persisted-data compatibility

1. [x] Add Android instrumentation coverage for real, historically accurate Room
   database fixtures at versions 1 and 2, migrated to the current version 3.
   Verify that `local_favorite_shops` survives, the new repository can read it,
   and the legacy adapter sees the same rows. Version 1's exported schema is
   present under `modules/room/schemas`; version 2's exported schema is in
   repository history at `e440150`. Use those actual schemas rather than
   fabricating version fixtures.
2. [x] Exercise save, remove and session clear after migration, including reopening
   the database. Migration fixtures cover old singular-logo JSON and unknown
   fields; repository tests cover duplicate IDs, ordering, malformed data, and
   removing the last item. All instrumentation uses uniquely named disposable
   databases, never a developer or user database.
3. [x] Retain coverage for malformed JSON and cancellation: read failures must not
   silently overwrite saved rows, ordinary failures stay in `Result`, and
   cancellation must not become `Result`. Test that clear is only an explicit
   session/user operation, not a response to a failed read.

Exit: both historical migration paths use their exported schemas and pass,
and the shared-writer behaviour remains verified after database reopen.

Verification: `:composeApp:compileDebugAndroidTestKotlin` and the focused
`connectedDebugAndroidTest` for `FavoritesRoomMigrationTest` passed on Pixel 10a
(Android 17). `:feature:favorites:impl:compileDebugKotlinAndroid`,
`:feature:favorites:impl:testDebugUnitTest`, and the instrumentation-test Kotlin
compile also passed after increasing the remove target to 48dp. Existing common
repository tests cover duplicate IDs/order, malformed JSON, non-destructive read
failure, `Result` failures, and cancellation propagation.

Session cleanup verification: `:modules:data:allTests` passed, including a new
test that proves a favorite-clear exception is preserved while cache cleanup
still runs. `:composeApp:compileDebugKotlinAndroid`,
`:composeApp:testDebugUnitTest`, `:composeApp:assembleDebug`, and
`:modules:data:allTests` (including iOS Simulator compilation/tests) passed.
`:composeApp:assembleRelease` also passed. On the latest UI instrumentation rerun,
5 of 7 tests passed and 2 failed with `No compose hierarchies found`; device
logs show the test Activity was not foreground when Compose assertions ran. No
unrelated foreground app was closed or altered.

## 3. Verify the complete Android journey

1. Test a signed-in account with existing favorites: feed and shop-detail heart
   state, opening the favorites list, opening a shop, removing a card, returning
   to feed/detail, and clearing favorites on both explicit and forced logout.
2. Cover loading, empty, read error, write error, retry, repeated remove taps,
   quick heart taps in feed/detail, Back, process recreation and return to the
   screen. Reconcile favorite membership after late or refreshed shop responses.
   Automate stable behaviour at the appropriate feature/app layer and record a
   device smoke-test checklist for what cannot be reliably automated.
3. Verify distance with permission already granted and without permission;
   opening favorites must not request a new location permission.
4. Use test accounts/fixtures for destructive scenarios; do not clear a real
   user's saved rows to exercise logout or migration behaviour.

Exit: Android user journeys pass without a stale heart, duplicate write, lost
saved row, crash, or navigation regression. Record the device/API used.

Verification so far: `:composeApp:testDebugUnitTest` passes with the signed-in
feed/detail write-path, add/remove round-trip, and failed-write tests. This
proves the ViewModel contracts with fakes, not the full authenticated app
journey.

Code inspection confirms favorites only calls `rememberPermittedUserLocation`,
which reads location after `PlatformLocation.hasPermission()` and never invokes
the permission-request effect. The no-permission / already-granted behavior
still needs a device smoke test.

Device smoke checklist (use a designated QA account with disposable favorites):

1. With location permission denied, open Favorites and confirm no permission
   prompt appears and the saved list still loads.
2. With location permission already granted and saved coordinates present,
   confirm distance is shown; missing coordinates remain omitted.
3. From feed, favorite a shop; open Favorites, open that shop, remove it there,
   return to feed and confirm the heart and list agree.
4. Recreate the activity/process and confirm the saved rows remain; remove a
   row, sign out, and confirm session cleanup behavior without using a personal
   account or personal saved data.
5. Repeat with forced session expiry only if QA has a safe way to trigger it;
   verify navigation continues and the cleanup warning is visible if cleanup
   fails.

## 4. Finish Android UI and resource quality

1. Inspect light/dark previews and the running screen with real photos/logos,
   missing images, long titles, large system font and RTL layout. Check touch
   targets, accessibility labels and screen-reader order. In particular, the
   saved-card remove control had a 36dp target and is now 48dp. Fix only issues
   owned by favorites; shared component defects belong to the design system.
2. Audit actual supported app locales. Favorites currently has Russian strings
   only and this checkout has no alternate favorites resource directory or app
   locale configuration. Keep translation pending until the product's
   supported locale list is confirmed; keep all feature strings/accessibility
   text in resources.
3. Compare the saved card with the legacy Android behaviour using only fields
   persisted in favorites. Document unavoidable differences such as unavailable
   `isNew`/shop type instead of inventing missing data.

Exit: visual/accessibility findings are fixed or explicitly accepted, previews
remain colocated and paired light/dark, and locale coverage is truthful.

## 5. Android-only cleanup and final gate

1. Remove only obsolete Android-specific adapters or branches whose consumers
   are proven migrated. Keep shared legacy code used by iOS; document every
   retained bridge and its removal condition. Do not force a platform split
   solely to delete a small transitional call.
2. Re-run affected unit/instrumented tests and `:composeApp:assembleDebug` after
   integration changes; also verify a release build. Check dependencies, source
   sets, `Result`/cancellation, DTO containment and the absence of feature data
   imports in UI. If shared `commonMain` code changes, compile the existing iOS
   target as a regression guard without implementing new iOS behaviour.
3. Review the checkpoint diff against its actual parent branch, list remaining
   QA limitations, and mark draft PRs #49, #53 and #57 ready only when their own
   gates are met. Confirm local tests, device smoke tests and required CI checks
   before the next feature proceeds.

Exit: Android favorites is verified and merge-ready as a feature. This does
**not** mean that iOS, shared legacy deletion or the application-wide root
Navigation 3 migration is complete.

## Deferred, separate decisions

- iOS favorites presentation/storage integration and deletion of legacy paths
  still used by iOS.
- A root Navigation 3 migration. The current Android favorites entry works
  under Navigation 2; changing the shared root affects more than favorites and
  must have its own platform-aware plan and regression suite.
