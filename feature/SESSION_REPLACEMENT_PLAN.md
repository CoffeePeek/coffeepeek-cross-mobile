# Session replacement — inventory and plan

Goal: nothing in `feature/*`, `core/*` or migrated app code depends on the legacy
`com.coffeepeek.domain.repository.SessionRepository` / `model.Session`, so that
`modules/domain`, `modules/data` and `modules/network` can later be deleted in one sweep
([LEGACY_REMOVAL.md](LEGACY_REMOVAL.md) section D, step 1). No product behaviour changes.
Inventory taken 2026-10-21 at `feature/legacy-consolidation`.

## 1. What exists today

| Piece | Where | Notes |
|---|---|---|
| `Session(accessToken, refreshToken?, userId?)` | `modules/domain` | plain data class |
| `SessionRepository` (10 methods) | `modules/domain` | peek/apply/isActive/get/persist/replace/save/warmCache/observe/isLoggedIn |
| `SessionRepositoryImpl` | `modules/data` | in-memory snapshot + `writer` mutex; reads secure store, imports old `SESSION`/`API_KEYS` rows from the legacy Room settings table (uses `DatabaseCore`, `AuthResp`) |
| `SessionAuth.isActive`, `JwtUtils` | `modules/data/util` | active = non-blank token + userId, and (unexpired token or a refresh token) |
| `SessionSecureStore` + Android/iOS platform stores, `RoomSessionSecureStore` | `modules/data/session` | Keychain / encrypted storage; carries `AuthResp` |
| `SessionTokenProvider` | `modules/data/session` | adapts the repository to the legacy API client (`getToken`, `saveToken`, `replaceToken` lambdas in `DataModule.kt`) |
| `UserSessionCleaner` | `modules/data/session` | logout: clears session, favorites for the owner, HTTP/app caches |
| `BearerSession`, `SessionTokens` | `core/network` | the new transport contract; **no implementation yet** |
| `SessionAuthentication`, `SessionRefreshCoordinator` | `modules/network` | legacy token refresh; goes away with `modules/network` |

## 2. Who uses it

~75 files reference `SessionRepository` (about 45 production, the rest tests/fixtures):

- `composeApp/androidMain/di/*` — **23 files**, the adapters of already migrated owners (Shop,
  Profile, Settings, Home, History, Coffee, Roaster, Favorites...). They only **read**:
  `peekSession` (16 call sites), `isActiveSession` (9), `observeSession` (13), `getSession` (1).
- `composeApp/commonMain` — ~21 files: legacy ViewModels (`Navigator`, `Feed`, `Shop`, `Profile`,
  roaster, review, delete-account, visited places...), `SessionRealtimeManager`, photo session
  (`CpImage`, `ProtectedPhotoSession`, `PhotoSessionInvalidation`), analytics installation,
  favorites, community.
- `modules/data` — 14 files (repositories that attach the user id, `AuthRepositoryImpl`, cleaner).
- Fields read from `Session` in the app: `userId` (~42 uses) and `accessToken` (~10, photos/transport).
- **Writers** (save/apply/replace): only `AuthRepositoryImpl` (login/Google/refresh), the legacy
  client `saveToken`/`replaceToken` lambdas, `UserSessionCleaner`, and `BaseViewModel` (session
  expiry). Everything else reads.

Conclusion: the API surface migrated features need is tiny and read-only ("who is the active
account, is somebody signed in, tell me when it changes"). `author-blocking` already got exactly
that as a port (`AuthorBlockingAccount`).

## 3. Target

New KMP module `core/session` (`module.core.session`), no dependency on Room, `modules/*` or Ktor:

```kotlin
data class Session(val accessToken: String, val refreshToken: String? = null, val userId: String? = null)

interface SessionReader {                      // what features/adapters get
    fun peekSession(): Session?                // in-memory snapshot
    fun isActiveSession(session: Session?): Boolean
    fun observeSession(): Flow<Session?>
    suspend fun getSession(): Session?         // loads the stored session first if needed
}

interface SessionStore : SessionReader {       // auth, transport, logout only
    suspend fun saveSession(session: Session?)
    suspend fun replaceSession(expected: Session?, session: Session?): Boolean
    fun applySession(session: Session?)
    suspend fun persistSession(session: Session?)
    suspend fun warmCache()
    suspend fun isLoggedIn(): Boolean
}
```

`SessionAuth` and `JwtUtils` move with it (they define "active"). The implementation, secure
stores (Android/iOS) and `SessionTokenProvider` move from `modules/data` into `core/session`.
The one-time import of old `SESSION`/`API_KEYS` rows from Room stays in the app as an injected
`legacyImport: suspend () -> Session?` so core does not depend on Room. `UserSessionCleaner` stays
legacy (it needs the legacy `FavoriteRepository`) and just uses `SessionStore`.

Owner-facing rule afterwards: feature impls never see sessions; the app adapter hands them an
opaque account key or a small port (as for author blocking). `SessionReader` is what app adapters
and legacy ViewModels receive.

## 4. Steps (each leaves the app building and all tests green)

0. **Parity tests first** on the legacy code: `SessionAuth.isActive` table (blank token/userId,
   expired access token with and without refresh token), `replaceSession` expected-mismatch,
   observe emits on save/apply, import of old settings rows. Existing:
   `SessionRepositoryConcurrencyTest`, `SessionTokenProviderTest`, `UserSessionCleanerTest`.
1. **Create `core/session`** with `Session`, `SessionReader`, `SessionStore`, `SessionAuth`,
   `JwtUtils` (moved, tests moved with them). No behaviour change.
2. **Bridge with zero churn:** in `modules/domain`, `typealias Session = com.coffeepeek.core.session.Session`
   and `interface SessionRepository : SessionStore`. All ~200 existing imports keep compiling.
3. **Move the implementation** (`SessionRepositoryImpl`, secure stores, token provider) into
   `core/session`, keeping the Room import behind the injected `legacyImport`. Android + iOS
   compile; run the moved concurrency tests.
4. **Migrate readers by area, smallest blast radius first:** (a) the 23 `androidMain/di` adapters
   to `SessionReader`; (b) `SessionAuthorBlockingAccount` and other adapters; (c) legacy commonMain
   ViewModels as each owner (App update, Auth, Community, Contributions, Discovery) is migrated —
   new owners are written against `SessionReader` from day one.
5. **Writers:** Auth owner takes login/Google/refresh/logout; transport switches to
   `core/network` `BearerSession` implemented on `SessionStore`, retiring `SessionTokenProvider`
   and `modules/network` refresh code.
6. **Delete the bridge** (typealias and `SessionRepository`) when step 4/5 consumers are gone.

## 5. Risks and gates

- Security-sensitive: tokens, refresh races and account switching. Do not change algorithms while
  moving; moved tests must pass unchanged. Account-switch tests exist in author blocking, Shop
  and Profile adapters and must stay green.
- iOS: the Keychain store and `compileKotlinIosSimulatorArm64` are part of every step's check.
- Stored data: users must stay signed in across the update (secure store keys unchanged, old
  Room rows still imported once). Verify on a build upgraded over an existing install, not only
  in Sandbox; Sandbox covers signed-in/out flows with fake sessions only.
- Out of scope: auth screens and `AuthRepositoryImpl` (Auth owner), `modules/network` removal.

## 6. Decisions (accepted 2026-10-21, all as recommended)

1. Module `core/session` with the `SessionReader` / `SessionStore` split.
2. Zero-churn bridge: `typealias Session` + `SessionRepository : SessionStore`.
3. `UserSessionCleaner` stays in `modules/data` until legacy favorites go away.

## 7. Step 0 result — parity tests (2026-10-21)

Added to `modules/data/src/commonTest` (no production change):

- `util/SessionAuthTest` (8): null/blank token or userId, unexpired token without refresh token,
  expired token with/without refresh token, tokens without a readable `exp`, the 30 s leeway,
  `JwtUtils` subject/expiry extraction and malformed tokens.
- `repository/SessionRepositoryParityTest` (11): stored credentials -> `Session` with the user id
  from the JWT `sub`; signed-out when empty; import of old `SESSION` / `API_KEYS` rows (once, rows
  deleted, `API_KEYS` fallback, secure store wins and leaves old rows, unreadable row ignored);
  `applySession` is memory-only while `saveSession`/`persistSession` write the store (`null`
  refresh token stored as `""`); `replaceSession` match/mismatch/clear; `observeSession` emits the
  loaded value then only changes; `isActiveSession` rules; `warmCache`.

Already covered before: `SessionRepositoryConcurrencyTest` (3, stale persistence, cold read vs
newer session, cancelled write vs logout), `SessionTokenProviderTest`, `UserSessionCleanerTest`,
`UserRepositorySessionTest`.

Verified: `:modules:data:testDebugUnitTest` 70 tests, 0 failures; `:modules:data:compileKotlinIosSimulatorArm64`
passes. Not covered by unit tests and left for the device gate in step 3: the real Android/iOS
secure stores and an upgrade over an existing signed-in install.

## 8. Steps 1-2 result — `core/session` and the bridge (2026-10-21)

- New KMP module `:core:session` (`module.core.session`, package `com.coffeepeek.core.session`):
  `Session`, `SessionReader`, `SessionStore` (with the default `replaceSession`), `SessionAuth`,
  `JwtUtils`; `SessionAuthTest` (8) moved with them. Depends on coroutines only.
- Bridge in `modules/domain` (api dependency on `core:session`): `typealias Session` and
  `interface SessionRepository : SessionStore`. No consumer import changed; `SessionRepositoryImpl`
  and the secure stores stay in `modules/data` until step 3.
- Verified: `:core:session:testDebugUnitTest`, `:modules:data:testDebugUnitTest`, Android unit and
  instrumented test compilation, iOS simulator compilation (`core:session`, `composeApp`) and
  `assembleDirectSandbox` all pass.

## 9. PR slicing (rule: at most 40 files per PR, one logical layer group per PR)

| PR | Content | Expected size |
|---|---|---|
| A | plan + parity tests + `core/session` + bridge (steps 0-2) | ~20 files |
| B | step 3: move the session store implementation and secure stores into `core/session` | 18 files (done) |
| C | step 4a: the `androidMain/di` adapters to `SessionReader` | 28 files (done) |
| D+ | step 4b/4c and 5: by area, each under 40 files, as owners migrate | |

## 10. Step 3 result — implementation moved to `core/session` (2026-10-21)

- `core/session` now holds `DefaultSessionStore` (the former `SessionRepositoryImpl`, same logic),
  `SessionSecureStore`, `SessionCredentials` (stored JSON `{"accessToken","refreshToken"}`, same
  shape as before, `refreshToken` defaults to `""`), the Android encrypted-preferences store and the
  iOS Keychain store (same prefs/Keychain names, so existing installs keep their session), and an
  `InMemorySessionSecureStore` for tests. Android adds `security-crypto` there; `modules/data` no
  longer depends on it.
- Room stays out of core: the one-time import of old `SESSION` / `API_KEYS` rows goes through the
  `LegacyCredentialRows` port, implemented in `modules/data` by `RoomLegacyCredentialRows`.
- `modules/data`: `SessionRepositoryImpl(store)` is now a thin delegate over `SessionStore`.
  `DataModule` additionally exposes `SessionStore` and `SessionReader` (same instance) for the
  readers migrated in step 4. `RoomSessionSecureStore` (plaintext fallback, test-only) was removed;
  the one test that used it now uses `InMemorySessionSecureStore`. `SessionTokenProvider` and
  `UserSessionCleaner` stay in `modules/data` (they adapt to the legacy API client / favorites).
- Tests moved with the code and run against core types: `SessionStoreParityTest` (10),
  `SessionStoreConcurrencyTest` (3), `SessionAuthTest` (8); new `RoomLegacyCredentialRowsTest` (3)
  covers the old-row JSON handling (blank/"null"/unreadable rows are nothing).
- Verified: `:core:session:testDebugUnitTest` 21 and `:modules:data:testDebugUnitTest` 51, 0
  failures; iOS simulator compilation of `core:session`, `modules:data` and `composeApp`; Android
  unit and instrumented test compilation; `assembleDirectSandbox` and `assembleDirectDebug`.
- **Not verified (gate before merging):** the real Android encrypted store and the iOS Keychain
  store have no unit tests (they need a device). The risk is limited to a mechanical move with the
  same keys, but an upgrade over an existing signed-in install should be checked by the owner of
  the release build; this session does not install the regular app on the phone.

## 11. Step 4a result — Android adapters read through `SessionReader` (2026-10-21)

- All 23 `composeApp/androidMain/di` adapters (Shop, Profile, Settings, Home, History, Favorites)
  now take `SessionReader` / `core.session.Session` instead of the legacy `SessionRepository`.
  They only read, as the inventory predicted; one of them needed `getSession()` (favorites owner
  lookup), so `getSession()` moved from `SessionStore` into `SessionReader` ("current session,
  loading it first if needed").
- `CoffeePeekApplication` is unchanged on purpose: it hands the store to the legacy
  `SessionRealtimeManager` (commonMain), which migrates with the legacy ViewModels (step 4c).
- Koin: adapters resolve `SessionReader`, registered in `DataModule` as the same instance as
  `SessionRepository`. Three module tests (`FavoritesModuleTest`, `CheckInHistoryModuleTest`,
  `ShopCheckInModuleTest`) now also register `SessionReader`. Test fakes still implement the
  legacy interface; they are converted when the bridge goes away.
- Verified: `:composeApp:testDirectDebugUnitTest` (di, sandbox, favorites, profile, settings: 297
  tests) and `:composeApp:testPlayDebugUnitTest` (di: 199) with 0 failures; Android unit and
  instrumented test compilation; iOS simulator compilation; `assembleDirectSandbox`.
