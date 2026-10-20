# Profile / account — legacy-first inventory — 2026-10-20

Method: the rule in [ANDROID_REFACTORING_PLAN.md](../ANDROID_REFACTORING_PLAN.md#legacy-first-parity-gate-mandatory-for-every-owner),
format as in [LEGACY_PARITY_AUDIT.md](../LEGACY_PARITY_AUDIT.md). This is a read-only analysis of the legacy `composeApp`
code on branch `feature/roaster-refactoring` (`154259e4`). No code was changed, Gradle was not run, nothing was
tried on a device. Status of every row is **open** until the owner confirms it; each row ends as **preserved**,
**improved (how)** or **intentionally removed (why, with approval)**.

There is **no `feature/profile` Gradle module yet**; this directory holds only this file. The only existing
code with that name is the legacy package `com.coffeepeek.admin.feature.profile` inside `composeApp` (public
profiles, with its own README). The words "profile" and "account" cover four different things in the legacy code, kept apart below:

1. **Own profile tab** (`ProfileScreen`, Android/iOS tab 3, signed-in only).
2. **Account editing and deletion** (`EditProfile`, `DeleteAccountPending`).
3. **Public profile of any author** (`UserProfile` route, app link `/users/{slug}`), also embedded in (1).
4. **Settings tab** (`SettingsScreen`, `CitySettings`, `ThemeSettings`, `UsagePolicy`) — lives in `ui/screen/profile` and
   shares one `ProfileViewModel` with (1), but is not account data.

## Scope and sizes (legacy, lines)

| Area | Files | Lines |
|---|---|---|
| `ui/screen/profile` — `ProfileScreen` 161, `ProfileViewModel` 245, `SettingsScreen` 234, `ThemeScreen` 154, `CityScreen` 136, `UsagePolicyScreen` 200, `PersonalProfileActivityCalendar` 91 | 7 | 1221 |
| `ui/screen/editprofile` — `EditProfileScreen` 405, `EditProfileViewModel` 152 | 2 | 557 |
| `ui/screen/deleteaccount` — `DeleteAccountPendingScreen` 138, `DeleteAccountPendingViewModel` 173 | 2 | 311 |
| `feature/profile` (public profile) — `PublicProfileViewModel` 371, `OtherUserProfileScreen` 400, `PublicProfileRepositoryImpl` 260, `PublicProfileCards` 212, `PublicProfileActivity` 174, `PublicProfileFavoriteCard` 118, `PublicProfileConnectionsSheet` 105, `PublicProfile` (domain) 103, README 33 | 9 | 1776 |
| Outside this owner but wired into it: `feature/favorites/ui/FavoritesProfilePreviewContent` 130 (+ `api/FavoritesProfilePreview`), `ui/screen/checkins/ProfileCheckInsScreen` 324 + `CheckInCalendar*`/`VisitedPlaces*` (check-in history owner, legacy kept for iOS) | — | — |
| Shared, **not** moved (`modules/*`): `UserRepository` + `UserRepositoryImpl` 158, `UserApiService`, `UserProfile`, `AccountDeletionRequest`, `AuthRepository`, `SessionRepository` | — | — |
| Tests today: `PublicProfileViewModelTest` 387 (16 tests), `PublicProfileRepositoryTest` 246, `PublicProfilePresentationTest` 864 (androidInstrumented), `PersonalProfileActivityCalendarTest` 332 (androidInstrumented), `CityPreferenceTest`, `SwitchPaletteTest`, Sandbox `SandboxApplicationBackendTest` (profile reads). **No test at all** for `ProfileViewModel`, `EditProfileViewModel`, `DeleteAccountPendingViewModel`, `SettingsScreen`, logout, consent. | — | ~1830 |

Moveable code is about 3.8k lines in 20 files (+ tests): small enough that the problem is not size but the
shared state and the cross-feature seams listed below.

## What exists today

### 1. Entry points, routes, consumers

| Piece | Where | Behaviour to preserve |
|---|---|---|
| Profile tab | `Navigator.Screen.ProfileGraph/ProfileTab` (`Navigator.kt:124`), `MainScreen.kt:237` `composable<ProfileTab> { if (isLoggedIn) ProfileScreen() }`; tab list `NativeTabRoutes.mainTabScreens` adds `ProfileTab` **only when signed in**; `MainScreen.kt:156-163` bounces a guest standing on `ProfileTab` back to `FeedGraph` | Tab hidden for guests; sign-out while on the tab leaves it. `ProfileScreen`'s guest branch (GuestAuthCard) is unreachable through the tabs today but is the fallback after account switches |
| Settings tab | `SettingsGraph/SettingsTab`, `MainScreen.kt:242`, always visible | Guests see the GuestAuthCard on top |
| iOS tabs | `iosMain/.../PlatformRootNavigation.ios.kt:154-160` builds `ProfileScreen()` (index 3) and `SettingsScreen()` (index 4) inside `NativeScreenController`, analytics page list `("discovery","coffees","feed","profile","settings")` | iOS keeps the legacy composables; they must stay compiling in `composeApp` |
| Root routes (`isHandledByRootNav`, `Navigator.kt:208-240`, composables `:504-518`, fallbacks `:541-556`) | `UserProfile(slug, nickname?, avatarUrl?)`, `EditProfile`, `DeleteAccountPending`, `CitySettings`, `ThemeSettings`, `UsagePolicy`, `BlockedAuthors`, `Favorites`, `VisitedPlaces`, `MyContributions(kind)` | Serialized route names are part of saved navigation state (iOS restores stacks) — keep class names and fields |
| App link | `AppLinks.kt:23` `coffeepeek.by/users/{slug}` → `UserProfile(slug)` (no nickname/avatar); `AppLinksTest` exists | Slug validation (no `/`, `\`, control chars, `.`/`..`) is in `appLinkScreen` and stays app-owned |
| Callers of `UserProfile` | `AndroidShopDetailScreenRenderer.kt:108`, `AndroidShopCheckInListScreenRenderer.kt:45` (lease-guarded), `CommunityScreen.kt:147`, legacy `ShopDetailScreen.kt:1361`, `ShopReviewsScreen.kt:83`, own `ProfileScreen`, connections sheet, `PublicProfileRoute` (`Navigator.kt:562`) | Nickname/avatar are an optimistic header before the load |
| Callers of own-profile routes | `ProfileScreen` → `EditProfile`, `Favorites`, `VisitedPlaces`, `MyContributions`, `ShopDetail`, `RoasterDetail`, `Auth`, `Register`; `EditProfileViewModel.startAccountDeletion` → `DeleteAccountPending`; `SettingsScreen` → `AddShop`, `AddRoaster`, `BlockedAuthors`, `CitySettings`, `ThemeSettings`, `UsagePolicy` | Navigation stays app-owned callbacks |
| Koin (`KoinApp.kt`) | `:165` `single<PublicProfileRepository> { PublicProfileRepositoryImpl(client, get(), get(), get()) }`; `:166` `factory { (slug) -> PublicProfileViewModel(slug, get(), get(), users = get(), visibility = get()) }`; `:205` `single { ProfileViewModel(6 deps) }` (**a singleton**); `:206` `factory DeleteAccountPendingViewModel`; `:208` `factory EditProfileViewModel`; `:171` `single AppUpdateState`; `:177-178` `single CityPreference`, `single ReviewDraftStore`; `:102` `ThemeManager.initialize(settingRepository)` | `PlatformKoin.android/ios.kt` contain no profile bindings today (favorites/shop/check-in-history only) |
| Sandbox | `SandboxApplicationBackend.kt:51-72`: `PATCH /api/users/me/username`, `GET /api/users/me/deletion-request` (404), `/api/v1/profiles/{slug}/favorites`, `/activity`, followers/following; friend slug with `isBlockedByViewer` | **Not served**: `PATCH about`, `PATCH avatar`, `DELETE /api/Users/me`, follow PUT/DELETE (verify), error payloads, deletion-request existing/resend window |

### 2. Own profile tab (`ProfileScreen` 161 + `ProfileViewModel` 245)

| Piece | Behaviour to preserve |
|---|---|
| States | `ProfileUiState`: `isLoggedIn`, `isLoading` (default **true**), `isRefreshing`, `error`, `refreshError`; `hasContent = displayName.isNotBlank() \|\| avatar`. Full-screen loader only when no content and no error; with content a failed refresh sets only `refreshError` (inline retry); without content shows `LoadErrorContent(onRetry = refreshProfile)`. Error text is the literal `Не удалось загрузить профиль` |
| Two rendering paths | (a) signed in **and** `publicAddress.slug` non-blank → the full public-profile screen (`UserProfileScreen`, `isOwnProfile = true`) wrapped in `key(slug)` with a `PublicProfileViewModel` from Koin (`parametersOf(slug)`) closed on dispose, and refreshed via `snapshotFlow { publicAddress, (name, about, avatar) }.drop(1)` whenever the cached own profile changes; (b) **cached account without a public address** → simple header + stats card + "Избранное" button + favorites preview + "Чекины" button (guest: `GuestAuthCard` with login/register, "Чекины" goes to `Auth`) |
| Top bar actions menu (`ProfileActions`) | `PlatformMenuButton`, content description `Действия с профилем`; items `Правки кофеен` → `MyContributions(Changes)`, `Отправленные кофейни` → `MyContributions(Shops)`, `Отправленные обжарщики` → `MyContributions(Roasters)`, `Выйти` (destructive) |
| Logout | `PlatformAlert`: title `Выйти из аккаунта?`, message `Вы уверены? Потребуется повторный вход.`, buttons `Выйти`/`Отмена`, destructive. `logout()`: reset UI state **first**, then `authRepository.logout()` (failure → `ErrorHandler.showError(message or local_data_cleanup_error)`), then `GoogleAuth.signOut()`, then `reviewDrafts.clearAll()` ("drafts belong to the signed-in user") |
| Session interplay | `observeSessionChanges`: userId (active sessions only) `distinctUntilChanged`; null → reset; new id → reset if another id was loaded, `isLoggedIn=true,isLoading=true`, then use cached `userRepository.observeProfile().value` or `refreshProfile()`. `refreshProfile` bails if session inactive / `loadedForUserId` differs, and ignores late failures of an old user. `applyProfile` re-checks the active session and drops the data if it changed. `observeProfileCache` null → `ProfileUiState(isLoggedIn/isLoading only while an active session with a loaded user exists)` |
| Cached account | `UserRepositoryImpl` keeps the profile in memory (`StateFlow`), cleared on session end/user change; `updateUsername` also replaces `address` (slug may change → the `key(slug)` block re-creates the public VM), `updateAbout` patches `about`, `updateAvatar` re-fetches; `getPublicAvatarUrl` has a map cache cleared on username change. Profile is **not** persisted in Room (verify) |
| Preview slots | `favoritesPreview = FavoritesProfilePreview(openShop, openRoaster)` (legacy `FavoritesViewModel`, own loader/empty/error/retry, remove-favorite from the card), `activityCalendar = PersonalProfileActivityCalendar()` (joins `VisitedPlacesViewModel` + `HomeCheckInViewModel`; both closed on dispose; month sync; one retry reloads both) |
| Insets | bottom content padding = `20.dp + LocalFloatingNavClearance` (floating tab bar) |
| Analytics | `Analytics.track(FAVORITES_OPENED, "entry_point" to "profile")` on "all favorites" in **both** paths. Screen view comes from the route class (`analyticsPage`: `ProfileTab`/`ProfileGraph` → `profile`); iOS passes `analyticsPage = "profile"` to the native controller |
| Other | `Settings`' hidden `ProfileViewModel` is the same singleton, so it keeps observing the session even when the tab is never shown |

### 3. Settings tab and sub-screens (`SettingsScreen` 234, `ThemeScreen` 154, `CityScreen` 136, `UsagePolicyScreen` 200)

| Piece | Behaviour to preserve |
|---|---|
| Rows | Title `Настройки`. Guest: `GuestAuthCard(onLogin → Auth, onRegister → Register)`. Section `Добавить`: `Добавить кофейню` → `AddShop`, `Добавить обжарщика` → `AddRoaster` (no guard seen in `SettingsScreen`; whether the target screens guard guests was not read). Section `Настройки`: `Заблокированные авторы` → `BlockedAuthors` (**signed-in only**), `Город` (trailing: selected name or `Выберите`) → `CitySettings`, `Тема` (trailing Авто/Светлая/Тёмная + icon) → `ThemeSettings`. Section `Другие настройки`: `Связаться с поддержкой` → `OpenInBrowser("https://coffeepeek.by/support")`, `Политика использования` → `UsagePolicy`, `Поделиться` → `ShareHelper.shareText(COFFEEPEEK_SHARE_TEXT)`. Section `Обновление приложения`: `AppUpdateState.check(manual = true)`, label `Проверяем обновления…` + spinner while checking, description = message / `Доступна новая версия CoffeePeek` / `Версия ${AppConfig.versionName}`, `AppVersionFooter`. Bottom clearance `spacing8 + LocalFloatingNavClearance` |
| Theme | `ThemeManager` (object, `SettingRepository` key `theme_mode`, enum `SYSTEM/LIGHT/DARK`, invalid value → `SYSTEM`, applies platform night mode, also read by iOS `NativeScreenController`/`PlatformRootNavigation`). `ThemeScreen` titles `Системная/Светлая/Тёмная`, check icon description `Выбрано`, cards use design-system geometry |
| City | `CityPreference` (key `selected_city_id`, `resolve()` falls back to first city and persists it, `select()` ignores blank/same). `ProfileViewModel.loadCities()` (at VM creation) calls `shopRepository.getCatalogs()` → `_cities` + `cityPreference.resolve(cities)`; `setCity` ignores ids not in the loaded list. **Also consumed by** `FeedViewModel` and `MapViewModel` (discovery) — the resolve side effect in `ProfileViewModel.init` is relied on only indirectly (verify who else resolves) |
| UsagePolicy | Section header `КОНФИДЕНЦИАЛЬНОСТЬ`; the analytics-consent switch row (`Аналитика использования`, PostHog (ЕС) text) is shown **only when** `Analytics.service != null && state.authReady && !state.userId.isNullOrBlank()`; toggle calls `analytics.setConsent(Accepted/Declined)` (Declined erases the queue and stops delivery); two cards (`Правила использования`, `Политика конфиденциальности`) open URLs in the browser. Screen view page `privacy` |
| Analytics | No `Analytics.track` calls in these screens. Pages: `SettingsTab/SettingsGraph/CitySettings/ThemeSettings` → `settings`, `UsagePolicy` → `privacy`, `BlockedAuthors` → `other` (not mapped) |

### 4. Edit profile (`EditProfileScreen` 405 + `EditProfileViewModel` 152)

| Piece | Behaviour to preserve |
|---|---|
| Load | Cached `observeProfile().value` if present (no request); otherwise `refreshProfile()`; failure → blocking dialog with `err.message ?: "Ошибка загрузки профиля"` and an empty form (verify the screen still renders the form after the dialog) |
| Fields and validation | Username 2–64 (`Имя не может быть пустым`/`Минимум 2 символа`/`Максимум 64 символа`; computed from the **untrimmed** value, saved trimmed), about ≤ 600 (`Максимум 600 символов`), input is hard-truncated (`take(64)`, `take(600)`), counters `n/64`, `n/600`, error shown for username only once non-empty. `hasChanges` compares trimmed values + `pendingAvatar != null`; Save enabled when valid, changed and not saving |
| Avatar | Gallery or camera via `PhotoSourceBottomSheet` (title `Фото профиля`), `rememberPhotoPicker(maxSelection = 1)`, **5 MB** client limit (`Размер файла не должен превышать 5 МБ`), avatar tap `onClickLabel = "Изменить фото профиля"`, initials fallback `username.take(2).uppercase()` or `?` |
| Save sequence | Sequential, stop at first failure: avatar (`updateAvatar` = `photoRepository.uploadAvatar` → `PATCH /api/Users/me/avatar` → refetch profile; failures wrapped `Не удалось загрузить фото: …`/`Не удалось сохранить аватар: …`) → username (`PATCH /api/Users/me/username`, cache + slug update) → about (`PATCH /api/Users/me/about`). Fallback texts `Ошибка обновления аватара/имени/описания`. All OK → `Navigator.popBack()`. **Quirk**: after an avatar success followed by a name failure the avatar is already live but `pendingAvatar` stays set (retry re-uploads it) |
| Delete account | Red text button `Удалить аккаунт` → alert `Удалить аккаунт?` / `Мы отправим ссылку на почту для подтверждения. Аккаунт останется активным, пока вы не подтвердите удаление в письме.` (`Удалить` destructive / `Отмена`) → `Navigator.navigate(DeleteAccountPending)` |
| Platform | IME-safe about field (`rememberSyncedTextFieldValue`, `platformTextInputOptions`, `limitTextLength`), error alert `Ошибка`, `platformViewModel()` (survives configuration change on Android), page `profile` for analytics |
| Return result | None. The profile tab sees the change only because `UserRepositoryImpl` updates the shared cache; **counters (check-ins, followers) are not refreshed** on return |

### 5. Pending account deletion (`DeleteAccountPendingScreen` 138 + ViewModel 173)

| Piece | Behaviour to preserve |
|---|---|
| Open side effect | `load()`: email from the cached profile, else `getMe()`; then `GET /api/Users/me/deletion-request` — if a request exists, show it; **if none (`null` = "not found"), immediately `DELETE /api/Users/me`, i.e. opening the screen requests the deletion and sends the e-mail** (the server keeps the account active until the link is confirmed). Errors: `Ошибка загрузки статуса`, `Не удалось отправить письмо` in an `Ошибка` alert |
| Resend | Button enabled only when `resendAvailable && !isResending`; `resendAvailableAtUtc` is parsed leniently (adds `Z` if no offset), unparsable/blank = available; a 1 s ticker flips the flag; strings `delete_account_pending_resend` / `_resend_wait` (resources); `Закрыть` → `popBack` |
| Session end | `observeSessionEnd`: after having seen an **active** session, if it becomes inactive → `GoogleAuth.signOut()` + `Navigator.openLoginAfterSessionEnd()` (root then re-keys the navigator, clears check-in drafts/errors/loading and opens `Auth`). The same path triggers for any sign-out while the screen is open |
| Analytics | Page `profile`; no events |

### 6. Public profile (`feature/profile`, README: `composeApp/.../feature/profile/README.md`)

| Piece | Behaviour to preserve |
|---|---|
| Entry | `OtherUserProfileScreen(slug, nickname?, avatarUrl?, onBack, onCoffeeShopClick, onRoasterClick, onLogin, onUserClick)` and the embeddable `UserProfileScreen(...)` with slots `topBarActions`, `favoritesPreview`, `activityCalendar`, flags `isOwnProfile`, `onEditProfile`, `onFavoritesClick`, `onCheckInsClick`; VM via `platformViewModel(key = slug, parameters = { parametersOf(slug) })` |
| Network | `GET /api/Users/{slug}` (`Cache-Control: no-store`, slug validated, canonical slug from `address`, avatar URL resolved against the request URL), `PUT`/`DELETE /api/Users/{slug}/follow`, `GET /api/v1/profiles/{slug}/favorites` (bounded concurrency `Semaphore(4)` for shop/roaster enrichment; failed enrichment keeps the entry; own favorites never used as fallback), `GET /api/v1/profiles/{slug}/activity` (year, month, `Europe/Minsk`), `GET /api/Users/{slug}/followers|following` (page size 20, max page 10 000), check-ins through the legacy `FeedRepository` with the author filter. Shared client → `RequestObserver` classifies every `/users`/`/profile` path as category `profile` for `api_request_completed` |
| State | Independent loading/error/retry for profile, check-ins (cursor paging, de-dup by id), favorites, activity (month `YYYY-MM`, 2000–2100, late old-month response rejected), connections (page counter, de-dup by slug). `PublicProfileUnavailable` → `Профиль недоступен` and all sections cleared; other failure → `Не удалось загрузить профиль`; inline errors `Не удалось обновить профиль/изменить подписку/загрузить избранное/обновить избранное/загрузить чекины/загрузить активность/загрузить пользователей` |
| Follow / identity | Guest → `needsLogin` → `onLogin()` then `loginHandled()`. Own-profile detection via cached own `address.slug` or `canonicalPath` (never by account id), waits for the own profile before following, own profile cannot be followed, server response is authoritative for flag and follower count; every request is guarded by a `(generation, account)` pair so a late response of an old viewer or month cannot overwrite the new state; account change resets state and reloads |
| Author blocking | `AuthorVisibility` (`features/author-blocking`, `com.coffeepeek.feature.authorblocking.api`): refresh before the first load (failure → `Не удалось загрузить блокировки. Повторите попытку.`), blocked profile shows `Автор заблокирован` + `Разблокировать` and drops favorites/activity/check-ins/connections; top-bar menu `Заблокировать автора`/`Разблокировать автора` through `AuthorBlockActions`, hidden for the own profile |
| Share | `ShareHelper.shareText("https://coffeepeek.by" + canonicalPath)` only for paths starting with a single `/`; menu item `Поделиться профилем`, content description `Поделиться профилем` |
| UI | Sections `Избранное` (carousel, "all" opens a sheet or `onFavoritesClick`), `Чекины` (own: `onCheckInsClick` → `VisitedPlaces`; others: sheet with `Загрузить ещё`, `CheckInTimelineContent`, photo viewer, `PrefetchListPhotos`), `Активность` (calendar card, `По времени Минска`, accessibility `"<day>, есть чекины"`, plural `день/дня/дней с чекинами`), followers/following sheet (`Подписчики`/`Подписки`, `@name`, initials fallback, open `UserProfile`). Drinks are listed but not clickable |
| Analytics | **None** (no follow/share/block events); screen page `user_profile` from the route class (own tab embeds it as `profile`) |
| Tests | VM (16 cases: pagination, partial failures, follow guards, own-profile detection, account switching, late responses, unavailable), repository (incl. cancellation and `PublicProfileUnavailable`), 864 lines of Android instrumented presentation tests, Sandbox backend test |

### 7. Cross-owner seams

| Seam | Today | Consequence |
|---|---|---|
| Favorites preview | `FavoritesProfilePreview` in composeApp `feature/favorites/api` → legacy `FavoritesViewModel` (not the migrated `feature/favorites` module) | A slot supplied by the app, never imported by profile |
| Activity calendar / check-ins link | `PersonalProfileActivityCalendar` uses `VisitedPlacesViewModel` + `HomeCheckInViewModel` (legacy, composeApp). The `VisitedPlaces` route already goes through `CheckInHistoryScreenRenderer` (Android: `AndroidCheckInHistoryScreenRenderer`, iOS: legacy `ProfileCheckInsScreen`) | The calendar slot stays app-owned until the history owner offers an api entry |
| Public check-ins | `PublicProfileRepositoryImpl` takes `FeedRepository` and `CheckInTimelineContent`/`CheckIn` from legacy modules | A temporary port is needed (`PublicCheckInsPort` over `FeedRepository`) |
| Catalog cards | `ShopCard`/`RoasterCard`/`CoffeeShopImage`/`CpImage`/`PrefetchListPhotos` (composeApp `feature/catalog/api`, ui components) | Card and image rendering come through slots or `core/design-system`, not copies |
| Session/auth | `SessionRepository.peekSession/observeSession/isActiveSession`, `AuthRepository.logout`, `GoogleAuth.signOut` (expect/actual), `Navigator.openLoginAfterSessionEnd`, `SessionRealtimeManager` also calls `GoogleAuth.signOut` | Auth stays app-owned; profile receives a `SessionPort` and a `SignOutPort` |
| Drafts | `ReviewDraftStore.clearAll()` on logout (`LegacyShopReviewDraftStore` adapts it for the Shop feature); `Navigator` clears `checkInDrafts` on login-state change | Logout side effects are an app composition concern (list them in one adapter) |
| `UserRepository` consumers | Shop (`AndroidShopBrowsingSession.viewer`, detail renderers), Community, legacy `ShopDetailViewModel`, `PublicProfileViewModel`, iOS | The repository and `UserProfile` stay in `modules/*`; profile gets narrow ports |
| Settings consumers | `ThemeManager` (iOS shell too), `CityPreference` (Feed/Map), `AppUpdateState`, `OpenInBrowser`, `ShareHelper` | Not profile data; see open question Q1 |

## Proposed target (nothing created yet)

### (1) Boundaries — `feature/profile/{api,domain,data,impl}`, package root `com.coffeepeek.feature.profile`

Same rules as Roasters/Coffee: create a module only with real content, Android first, iOS/shared keep the
legacy composeApp UI, no new feature code in composeApp, cross-feature access only through `api`.

| Module | Responsibility | Depends on |
|---|---|---|
| `feature/profile/domain` | Public profile models (`PublicProfile`, stats, favorite, activity, connection, check-in page), `PublicProfileRepository` contract, errors (`PublicProfileUnavailable`, `PublicProfileAuthenticationRequired`), own-profile identity rule (slug/canonicalPath match) as a pure function, activity-month validation | Kotlin only |
| `feature/profile/data` | `PublicProfileRepositoryImpl` + DTOs/mappers behind a factory with the **same** paths, headers, `Semaphore(4)`, page limits; reads `HttpClient` from the app's shared `CoffeePeekClient` so the `RequestObserver` keeps category `profile` | domain, `core/network` |
| `feature/profile/api` | Entries (Android Compose in `androidMain`): `PublicProfileEntry(slug, nickname?, avatar?)`, `OwnProfileEntry`, `EditProfileEntry`, `AccountDeletionEntry`; **ports** supplied by the app: `AccountSessionPort` (active account key, observe, `isActive`), `OwnAccountPort` (observe cached profile, refresh, `getMe`, update username/about/avatar, request/get deletion request — adapter over `UserRepository`), `SignOutPort` (logout + Google sign-out + draft cleanup + error surfacing), `AuthorVisibilityPort`/`AuthorActionsPort` (adapters over `features/author-blocking` api), `PublicCheckInsPort` (over `FeedRepository`), `ProfileNavigation` callbacks, `ProfileSlots` (favorites preview, activity calendar, check-in timeline, shop/roaster card, image/viewer), `ProfileAnalyticsPort` (see below), `ShareLinkPort` |
| `feature/profile/impl` | Typed MVI view models (public profile, own tab, edit, deletion), stateless content, resources (`strings.xml` — keep the Russian texts verbatim), previews, entries | api, domain, `core/presentation`, `core/design-system` |

Stays **app-owned** (`composeApp`): routes and serialization (`Navigator.Screen.*`), `AppLinks`, tab shell and guest tab
rules (`MainScreen`, `NativeTabRoutes`), iOS composition, all adapters over `UserRepository`/`SessionRepository`/`AuthRepository`/
`GoogleAuth`/`ReviewDraftStore`/`Navigator`, the analytics adapter (`Analytics.track`, screen-page mapping, exposure markers),
the legacy `ProfileCheckInsScreen`/`VisitedPlaces*`/`PersonalProfileActivityCalendar` and `FavoritesProfilePreview` slot providers
(replaced by the history/favorites `api` entries when those owners expose them), Koin wiring (`PlatformKoin.android.kt`, `KoinApp.kt`), Sandbox fixtures.
Stays in `modules/*` and is **not** moved: `UserRepository`, `UserProfile`, `UserApiService`, `AccountDeletionRequest`, `AuthRepository`, `SessionRepository`.
Own-account writes keep going through the existing `UserRepositoryImpl` (single cache, single session guard) so the legacy
iOS/other consumers and the new Android UI cannot diverge.

Analytics port: the legacy profile owner emits only `FAVORITES_OPENED(entry_point=profile)`; the rest is route-class page mapping.
So `ProfileAnalyticsPort` has one method (`favoritesOpened()`), implemented in the app with the exact legacy call, and the screen-page
mapping in `AnalyticsVisibility.analyticsPage` is **not** changed by moving screens (the routes stay in the app).

Settings (theme, city, usage policy, update check, support/share rows) are **not** in this owner by default — see Q1.
If the owner keeps them, a `feature/settings` owner follows later and the singleton `ProfileViewModel` must first be split
(step 1 below) so the profile tab no longer owns city/theme state.

### (2) Staged migration (each step one PR-sized change, 100–150 files guide; tests first, legacy kept)

| # | Step | Files (est.) | Verify |
|---|---|---|---|
| 0 | Fix this inventory with the owner (Q1–Q10), add Sandbox fixtures: PATCH about/avatar, DELETE `/api/Users/me` (+ existing deletion-request, resend window), follow PUT/DELETE success/401/500, 404 slug, blocked author, user without public address, slow/failed `/users/me` | 4–6 | `SandboxApplicationBackendTest` |
| 1 | **Parity tests on legacy first** (no behaviour change): `ProfileViewModelTest` (session switch, late failure, reset on logout, cached account), `EditProfileViewModelTest` (validation, trimmed compare, 5 MB, sequential save, partial failure, quirk recorded), `DeleteAccountPendingViewModelTest` (open side effect, resend gating with a fake clock, session end), logout order test; move the Settings-only state (`cities`, `selectedCityId`, `themeMode`) out of the profile VM only if Q1 says settings leave | 6–8 | unit tests green before and after |
| 2 | Create `feature/profile/domain` + `data`: move public-profile models, repository contract and impl (260 lines) with the repository tests (246); app Koin binds the new factory; legacy `OtherUserProfileScreen`/VM import from the new package | ~15 | `PublicProfileRepositoryTest`, Sandbox test unchanged |
| 3 | `feature/profile/api` ports + app adapters (`AccountSessionPort`, `OwnAccountPort`, `AuthorVisibilityPort`, `PublicCheckInsPort`, `ShareLinkPort`, `ProfileAnalyticsPort`) with adapter tests | ~15 | adapter unit tests |
| 4 | `impl` public profile: typed MVI + stateless UI (cards, activity, connections, favorite card) with the VM test port (387 lines, 16 cases rewritten against ports) and the presentation tests; Android entry `PublicProfileEntry` behind `Navigator.Screen.UserProfile`, iOS keeps legacy | ~30 | VM tests, Pixel 7 Sandbox: guest/follow/blocked/own-via-alias |
| 5 | `impl` own tab (`OwnProfileEntry`): reuse the public screen with slots; cached-account fallback path; logout dialog and menu; slots wired from the app (favorites preview, activity calendar) | ~15 | tab, guest-bounce, logout, account-switch checks |
| 6 | `impl` edit profile + account deletion (entries, state, strings), including `SignOutPort` for the session-end redirect | ~20 | edit/delete VM tests, manual checklist (avatar picker, IME, rotation: state must survive configuration change — see Home H1) |
| 7 | Retroactive audit of the activated Android path against this file (table in `LEGACY_PARITY_AUDIT.md` style), then decide on removing legacy copies only when iOS consumes the new module | docs | audit rows closed |
| later | Settings owner (theme/city/usage policy/update) if Q1 = separate; shared `ProfileViewModel` singleton retired only then | — | — |

### (3) Risks / regressions to guard with a parity test

Sev H/M/L. "Guard" is the test that must exist before the step that can break it.

| id | sev | stable prod behaviour to keep | where it lives today | guard |
|---|---|---|---|---|
| P1 | H | Logout order: reset UI → `authRepository.logout()` (failure shown via `ErrorHandler`) → `GoogleAuth.signOut()` → `ReviewDraftStore.clearAll()`; plus root clears check-in drafts and re-keys the navigator on login-state change | `ProfileViewModel.logout`, `Navigator.kt:296-312` | VM/adapter test with fakes recording call order; manual: sign out on every screen |
| P2 | H | Account switch never leaks the previous user's data: `(userId)` guard on refresh/apply, late failures ignored, `applyProfile` re-checks the active session; public VM uses `(generation, account)` and own-profile identity is cleared on account change | `ProfileViewModel`, `PublicProfileViewModel` (tests exist for the latter only) | new `ProfileViewModelTest` + keep `oldAccountsLateFollowCannotChange…`, `accountChangeRejectsOldCachedOwnership…` |
| P3 | H | Opening `DeleteAccountPending` with no existing request **issues the deletion request itself** (and the e-mail); resend gating by server time; session end → Google sign-out + login. Must not move to "on button tap" or fire twice (recreation, retry, double open) | `DeleteAccountPendingViewModel.load/observeSessionEnd` | fake-repo test counting `requestAccountDeletion` calls on open, recreation and resend |
| P4 | H | Own-profile detection by public address (slug/canonicalPath), never account id; own profile cannot be followed; guest follow → login; follow waits for own identity | `PublicProfileViewModel.isOwnProfile/toggleFollow` | existing 5 tests ported to ports |
| P5 | H | Blocked authors: visibility refreshed before profile load; blocked profile hides favorites/activity/check-ins/connections; block/unblock menu hidden on own profile and for guests per the existing decision | `PublicProfileViewModel`, `OtherUserProfileScreen` | VM test + Sandbox friend slug; manual walkthrough (still open in the plan) |
| P6 | M | Own tab shows the full public profile only when a public slug exists; otherwise the simple header path; `key(slug)` recreation when the username (hence slug) changes after edit; refresh trigger on cached name/about/avatar change | `ProfileScreen.kt` | state-holder unit test with a fake `OwnAccountPort` emitting slug changes (no new UI suite, Q12) |
| P7 | M | Edit save is sequential avatar → username → about, stops on first failure, trimmed compare, 2–64/≤600, 5 MB, `popBack` only on full success; partial-failure quirk decided explicitly (preserve or fix with approval, see Q5) | `EditProfileViewModel` | VM test per branch |
| P8 | M | Edit loads the cache without a request; counters on the tab are not refreshed after edit (stale until a refresh) — decide whether to keep (Q6) | `EditProfileViewModel.loadProfile` | test records `refreshProfile` count |
| P9 | M | Analytics: exactly one `favorites_opened {entry_point: profile}` per tap, on both own-tab paths; page names `profile`/`user_profile`/`settings`/`privacy`/`my_check_ins` come from the **route class**, so route class names must not change; `api_request_completed` category `profile` stays because the same client and paths are used; consent toggle visibility rule (`authReady && userId`) | `ProfileScreen.kt:36`, `AnalyticsVisibility.kt:22-38`, `RequestObservation.kt:66` | adapter test against the legacy tracker (see `ANALYTICS_PARITY_AUDIT.md`), `analyticsPage` table test |
| P10 | M | Public activity: month/year validation, `Europe/Minsk`, server-side counts, old-month response dropped; connections paging limits and de-dup; favorites enrichment `Semaphore(4)`, failed enrichment keeps the entry, own favorites never used | `PublicProfileViewModel`, `PublicProfileRepositoryImpl` | ported repository/VM tests |
| P11 | M | Guest tab rules: Profile tab only signed in, guest on `ProfileTab` bounced to Feed, Settings guest card, `Заблокированные авторы` only signed in | `MainScreen.kt`, `NativeTabRoutes.kt`, `SettingsScreen.kt` | existing `NativeTabRoutesTest` + manual guest checklist |
| P12 | M | Theme persistence and initial value (`theme_mode`, invalid → SYSTEM) and city persistence/fallback (`selected_city_id`, first city) with Feed/Map consumers; iOS shell reads `ThemeManager` | `ThemeManager`, `CityPreference` (+ `CityPreferenceTest`) | keep tests; add a test that the city is resolved even if the settings screen is never opened (see Q4) |
| P13 | M | Share rule: only canonical paths starting with one `/`, prefixed with `https://coffeepeek.by`; app link slug validation | `OtherUserProfileScreen`, `AppLinks.kt` | table tests |
| P14 | M | Activity recreation: Edit form (draft text, picked avatar), logout dialog, deletion screen, connections/sheets must survive rotation, theme/font/locale change (the Home check-in editor lost state, H1) | legacy `platformViewModel()` / `rememberSaveable` usage | Pixel 7 manual checklist + a test that the new entry uses the host `ViewModelStoreOwner` |
| P15 | L | Error and confirmation texts are verbatim Russian strings (list above); content descriptions (`Действия с профилем`, `Действия с автором`, `Поделиться профилем`, `Выбрано`, `Изменить фото профиля`, `Предыдущий месяц`, `Следующий месяц`) and large-font layouts | all screens | string-resource snapshot test; manual large font |
| P16 | L | The singleton `ProfileViewModel` also loads cities and keeps observing the session while no screen is open; removing it changes startup work and when `getCatalogs` runs | `KoinApp.kt:205` | startup test/inspection (Q4) |
| P17 | L (Sandbox) | Fixtures missing for about/avatar/deletion/follow errors; real server not used | `SandboxApplicationBackend` | step 0 |

### (4) Open questions for the owner

1. **Scope**: do Settings (theme, city, usage policy, update check, support/share, add-shop/roaster entry rows) belong to this
   owner or to a separate `settings` owner? Recommendation: separate, later; but split the shared `ProfileViewModel` singleton first.
2. **Module naming/shape**: `feature/profile` (singular, as Shop/Roaster) is assumed; the plural `features/author-blocking` exists
   as a separate layout. Confirm singular `feature/profile/{api,domain,data,impl}` and package root `com.coffeepeek.feature.profile`.
3. **Own-account data**: keep `UserRepository`/`UserProfile`/deletion-request in `modules/*` behind a port (recommended: shared with Shop,
   Community, iOS, Auth), or give profile its own data module for `/api/Users/me*` later?
4. **City default**: `ProfileViewModel.init` resolves the city via `getCatalogs()`. Is that intentional for startup, or does Discovery
   already guarantee it? (affects whether removing the singleton changes behaviour.)
5. **Partial-save quirk** in Edit profile (avatar applied, name failed, avatar re-uploaded on retry): preserve exactly or fix (clear `pendingAvatar` after its success)?
6. **Stale counters** after edit/return and no pull-to-refresh/resume refresh on the own tab: keep as prod, or refresh on return?
7. **Deletion screen side effect** (opening it requests the deletion/e-mail): keep (prod) or require an explicit button? Any back-end idempotency guarantee?
8. **Guest rules**: is the guest branch of `ProfileScreen` intentionally dead code (tab hidden), may it be dropped in the new entry? Should `AddShop`/`AddRoaster`
   rows keep working for guests (guard not verified here)? Same question as G5 (guest opening an author profile from a shop card).
9. **Analytics**: no profile events exist for follow/unfollow, share, block, logout, edit save, deletion request. Keep none (parity), or is a product decision wanted? New events would need the wire contract in `AnalyticsEvent`.
10. **Slots**: for favorites preview and the activity calendar, is it acceptable that the new profile entry takes the **legacy** composeApp composables through app-provided slots until Favorites and Check-in history expose api entries?
11. **iOS**: confirm iOS keeps `ProfileScreen`, `SettingsScreen`, `EditProfileScreen`, `DeleteAccountPendingScreen`, `OtherUserProfileScreen` unchanged in `composeApp` (legacy copies stay, so shared domain/data may be consumed by both only if it compiles for iOS targets — the new `data` module is KMP `commonMain`).
12. **UI tests**: `feature/UI_TEST_REMOVAL.md` removed agent-authored UI suites from the migration branches at the user's request (the existing profile instrumented tests, 864 + 332 lines, were not in its list). Confirm that no new Compose UI suites are wanted for this owner (coverage = unit tests, Sandbox, manual checklist) and that the existing ones stay untouched.
13. **Sandbox**: may step 0 extend the fake backend (about/avatar/deletion/follow/error payloads)? Real-account acceptance remains a separate authorised gate.

Not verified in this inventory (read-only pass over profile code only): guards inside `AddShop`/`AddRoaster`/`BlockedAuthors`,
`AppUpdateState` details, `PublicProfileCards`/`PublicProfileFavoriteCard` visual geometry, Room persistence of the own profile,
actual iOS behaviour, `PlatformMenuButton`/`PlatformAlert` semantics beyond the strings above.

## Owner decisions — 2026-10-20

- Settings (theme, city, usage policy, update check) are a separate later owner; split the shared `ProfileViewModel` first.
- Deletion: opening the pending-deletion screen must not send `DELETE /api/Users/me` by itself; the request goes out only after an explicit user confirmation (deliberate difference from prod).
- Edit profile: fix the partial-save quirk, a retry must not re-upload an avatar that already succeeded (deliberate difference from prod).
- Own-account data (`UserRepository`, `UserProfile`, deletion request) stays in `modules/*` behind narrow ports.
- Other open questions keep the inventory's recommendations until the owner says otherwise.

## Step 1 result — parity tests on the legacy code (2026-10-20)

Added to `composeApp/src/commonTest/.../ui/screen/`: `profile/ProfileViewModelTest` (8), `editprofile/EditProfileViewModelTest` (8),
`deleteaccount/DeleteAccountPendingViewModelTest` (6). All 22 pass, repeated runs stable (the ViewModels use real dispatchers, so the
tests wait on conditions instead of virtual time). No production code changed.

Covered: cached/uncached load, fixed error texts, refresh failure with and without content, session end reset, late failure of a
previous account ignored (P2), logout order reset → server → drafts and failure message (P1), city resolved at creation and unknown
ids ignored (P12/P16), edit validation/truncation/trim/5 MB/save order/stop at first failure, the recorded avatar re-upload quirk
(flips when the owner-approved fix lands), deletion open side effect recorded once (flips with the approved confirmation), resend gating.

Not covered, with reason: `GoogleAuth.signOut` and `Navigator.openLoginAfterSessionEnd` (no observable output: private flag/platform
object) — keep on the manual checklist; analytics page names (P9) and the Sandbox fixtures (step 0).

New finding: `DeleteAccountPendingViewModel.resendEmail()` checks `isResending` before launching, so two taps in the same frame can send
two requests (the server window limits the damage). Not changed; decide with the deletion-confirmation change.

## Step 0 result — Sandbox fixtures (2026-10-20)

Done in `SandboxApplicationBackend` (see `composeApp/SANDBOX_PLAN.md`, "Profile/account fixtures"). Correction to the inventory: PATCH
about/avatar and the deletion-request GET already existed but were unconditional; they now validate and keep state. Open: 401 follow,
no-public-address account, blocked-profile follow rule, device walkthrough.

## Step 2 result — `feature/profile/{domain,data}` (2026-10-20)

Created and registered in `Modules.kt`; `composeApp` depends on both in `commonMain` (iOS compiles them: `compileKotlinIosSimulatorArm64`).

- **domain** (`com.coffeepeek.feature.profile.domain`): public profile models, own `ProfileAddress` (no legacy `PublicAddress`),
  `PublicProfileData` (profile, follow, favorites, activity, connections), `PublicProfileFavoriteDetails` port (+ `FavoriteShopDetails`,
  `FavoriteRoasterSummary/Details`), the two typed exceptions.
- **data**: `createPublicProfileData(client, details)` (impl internal) and the moved `PublicProfileRepositoryTest` (10 cases, the roaster-catalog
  case rewritten against the port). Feature modules do not depend on legacy `modules/*`, so nothing legacy leaks in.
- **app**: `AppPublicProfileRepository` = `PublicProfileData by data` + the author's check-ins (legacy `CheckIn`/`FeedRepository`, stays until the
  profile UI moves, step 4); `LegacyPublicProfileFavoriteDetails` adapts `ShopRepository`/`RoasterRepository`; Koin binds both; the
  favorite card converts `ProfileAddress` to the legacy `PublicAddress` for the shared shop card.
- Behaviour unchanged: 15 `PublicProfileViewModelTest`, 8+8+6 step-1 parity tests, 16 Sandbox backend tests, 2 app repository tests pass;
  instrumented tests compile; Direct Debug, Direct Sandbox and iOS simulator compile build. Pixel 7 not run in this step.
- Deviation from the plan table: the plan said "move the repository"; check-ins could not move without a legacy dependency, so they stay in the app.

## Step 3 result — `feature/profile/api` ports and app adapters (2026-10-20)

`feature/profile/api` (registered, iOS compiles) holds only ports with a real consumer today:

- `ProfileAccountPort` (+ `OwnProfile`): active account key (user id, else token; null for guest/inactive), distinct account changes,
  the shared own-profile cache and `loadOwnProfile(forceRefresh)` with the legacy rule (cache unless forced, else `getMe`). Read side only;
  edit/deletion/sign-out ports come with steps 5-6 when their screens move.
- `ProfileAuthorVisibility` (+ state with `hides`, hides everything until ready) and `ProfileAnalyticsPort.favoritesOpened()`.

Adapters in `composeApp/.../androidMain/di/profile/AndroidProfilePorts.kt`: over `SessionRepository`/`UserRepository`, the author-blocking
`AuthorVisibility` (a profile-owned port, so the feature module does not import the legacy-dependent author-blocking module) and the exact
legacy `Analytics.track(FAVORITES_OPENED, entry_point=profile)`. `AndroidProfilePortsTest` (6) pins key rules, distinct account flow, cache/force
semantics, mapping, visibility semantics and legacy-vs-adapter analytics parity. Not bound in Koin yet: the first consumer is the public-profile
entry (step 4).

Deviations from the plan table: `PublicCheckInsPort` is deferred (the check-in model is the legacy timeline `CheckIn`; it moves with the
history/Community timeline owner, not here) and `ShareLinkPort` is dropped (sharing is an action callback of the entry, not a port).
