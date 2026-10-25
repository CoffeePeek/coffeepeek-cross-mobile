# Legacy removal map

**Current execution plan (2026-10-25): [LEGACY_EXIT_PLAN.md](LEGACY_EXIT_PLAN.md).**
Both platforms now select feature entries; the tables below are the historical
2026-10-21 baseline, not the current list of iOS consumers. Removal has started:
unused renderer fallbacks must not remain as defaults in the application graph.

Single place that answers: **what is still legacy, who still needs it, and what must be true
before it can be deleted in one sweep.** Counts were taken on 2026-10-21 at the head of
`feature/legacy-consolidation` (settings stack) with the commands at the bottom; rerun them
instead of trusting the numbers after the next owner lands.

Rule: legacy code is deleted only when its recompute command below returns nothing outside the
legacy code itself. Until then it stays, even if Android no longer uses it.

## A. Legacy Gradle modules (`module.legacy.*`)

| Module | Lines | Files importing it (all source sets) | Deleted when |
|---|---|---|---|
| `:modules:domain` (`com.coffeepeek.domain`) | 940 | ~197 in `composeApp` (none in `feature/*`) | every owner talks to its own domain and a session port; `SessionRepository`/`Session` get a replacement in `core` |
| `:modules:data` (`com.coffeepeek.data`) | 4435 | 5 in `composeApp` | no consumer of its repositories remains (Koin wiring in `KoinApp.kt` is the last one) |
| `:modules:network` (`com.coffeepeek.api`) | 5103 | 16 in `composeApp` | all owners use `core/network` clients |
| `:modules:room` (`com.coffeepeek.room`) | 338 | ~21 in `composeApp` | settings/city/session storage moved to `core/database` |
| `:modules:home-checkin` (`com.coffeepeek.legacy.homecheckin`, `module.legacy.homeCheckIn`) | 660 | ~25 in `composeApp` (legacy Home UI, profile activity calendar, Koin, tests) | iOS and the profile activity calendar use the migrated Home domain; then delete with `composeApp/.../feature/homecheckin` |

Most used legacy symbols in `composeApp` main source sets: `SessionRepository` (43 files),
`CatalogItem` (20), `CheckIn` (19), `ShopRepository` (18), `UserRepository` (13), `Session` (11),
`City` (10). `SessionRepository` is the real blocker: it is imported everywhere in `composeApp`. Migrated
features do not import it: they receive a port (for author blocking, `AuthorBlockingAccount`,
adapted by `composeApp/.../di/authorblocking/SessionAuthorBlockingAccount.kt`).

## B. Legacy code inside `composeApp`

Status per area. "Android" = Android uses a migrated owner behind a renderer/entry; the legacy
screen stays for iOS/shared until iOS consumes the new modules.

| Area (path under `composeApp/.../admin`) | Lines | Android | iOS/shared | Notes |
|---|---|---|---|---|
| `ui/screen/shop`, `shopchange`(part), `checkins` | 3941 + 1488 + 1413 | migrated (Shop, check-in list/form, report) | legacy | `LegacyShop*Renderer` x5 |
| `feature/coffee`, `ui/screen/coffee` | 1035 + 22 | migrated | legacy | `LegacyCoffeeScreenRenderer` |
| `ui/screen/roaster` | 1575 | migrated | migrated (shared UI, commit `d36390a4`) | `LegacyRoasterScreenRenderer` removed; add/edit roaster screens still legacy |
| `feature/profile`, `ui/screen/profile`, `editprofile`, `deleteaccount` | 1482 + 1314 + 557 + 311 | migrated (public/own profile, account, edit, delete) | legacy | `ProfileViewModel` singleton kept for iOS/shared |
| `ui/screen/profile` Settings pieces | included above | migrated (Settings, Theme, City, Usage policy) | legacy | `LegacySettingsScreenRenderer` |
| `feature/favorites`, `ui/screen/favorites` | 501 + 281 | migrated (`feature/favorites`) | legacy | iOS uses `FavoritesScreen` |
| `feature/homecheckin` | 1354 | migrated (`feature/home-check-in`) | legacy | also feeds profile activity calendar |
| `ui/screen/checkins` history | in row 1 | migrated (`feature/check-in-history`) | legacy | |
| `feature/authorblocking` | 145 | Cp UI over the migrated `feature/author-blocking` | legacy | move UI into `impl` on design-system |
| `feature/appupdate` | ~250 | migrated (`feature/app-update`, audit done) | legacy prompt and `StoreUpdateInstaller` used by iOS | delete with the Legacy renderer when iOS migrates |
| `ui/screen/auth`, `passwordrecovery` | 1699 + 159 | migrated (`feature/auth`: sign-in, Google, registration, recovery) | legacy | `LegacyAuthScreenRenderer`; Apple, log-out and `ProfileViewModel` still use the legacy repository |
| `ui/screen/feed`, `map`, `main`, `feature/catalog` | 1428 + 1406 + 463 + 691 | not migrated | used by iOS (`DiscoveryTabContent` through `LegacyDiscoveryScreenRenderer`, `CheckInTabContent`); Android runs `feature/discovery` | owner: Discovery |
| `feature/community` | 770 | migrated (`feature/community`: feed, likes, edit, hide, report, blocking) | legacy | `LegacyCommunityScreenRenderer`; inventory in `feature/community/LEGACY_INVENTORY.md` |
| `ui/screen/addshop`, `contributions`, `shopchange` | 1873 + 513 + 1488 | migrated and connected on Android | legacy on iOS and shared code (`LegacyContributionsScreenRenderer`) | owner: Contributions (`feature/contributions/LEGACY_INVENTORY.md`); `AddRoaster` (643, in `ui/screen/roaster`) goes with it |
| `ui/screen/review` (report of a review/check-in, `ConsumedDrinkField`) | 215 | Shop migrated, check-in report inside Community sheet | legacy Shop review flow, legacy check-in report | owner: Shop (iOS migration), not Contributions |

Not-migrated areas are the real remaining work; everything marked "migrated" on Android only
waits for iOS.

## C. The former `features/*` directory (done)

The extra top-level `features/` directory no longer exists:

- `features/author-blocking` -> `feature/author-blocking/{api,domain,data,impl}`. It is a real owner,
  not legacy, and no longer depends on `:modules:domain`: it takes the `AuthorBlockingAccount` port.
  Its Cp UI (`AuthorBlockingHost`, `BlockedAuthorsScreen`, 145 lines in
  `composeApp/.../feature/authorblocking`) is still in `composeApp`; moving it to `impl` on
  `core/design-system` is the remaining owner work (the earlier reason, "Cp UI lives in composeApp",
  no longer holds).
- `features/home-checkin` -> `modules/home-checkin`, registered as `module.legacy.homeCheckIn`
  and re-packaged to `com.coffeepeek.legacy.homecheckin` (it used to share the
  `com.coffeepeek.feature.homecheckin` prefix with the new `feature/home-check-in`, registered as
  `module.feature.homePreparation.*`). Deleted with the other legacy modules.

## D. Suggested deletion order

1. Replace `SessionRepository`/`Session` with a core session port (unblocks everything in A).
2. Migrate the remaining owners (App update, Discovery, Auth, Community, Contributions).
3. Point iOS at the migrated modules owner by owner; remove each `Legacy*Renderer` and its screen.
4. Delete `modules/home-checkin`, then `modules/data`, `modules/network`, `modules/room`,
   `modules/domain` (in that order: data depends on the others).
5. Delete the `legacy` group from `Modules.kt` and the Koin wiring that built it.

## Recompute

```bash
# files importing each legacy package
for p in domain data api room; do echo $p; grep -rl --include='*.kt' "^import com.coffeepeek.$p\." composeApp feature core features | grep -v /build/ | wc -l; done
# Gradle consumers of legacy modules
grep -rn --include=build.gradle.kts 'module.legacy\|:modules:' . | grep -v /build/
# remaining Legacy renderers
grep -rho --include='*.kt' 'object Legacy[A-Za-z]*Renderer' composeApp/src/commonMain
# iOS dependencies on legacy UI
grep -rn --include='*.kt' 'import com.coffeepeek.admin.\(ui.screen\|feature\)' composeApp/src/iosMain
```
