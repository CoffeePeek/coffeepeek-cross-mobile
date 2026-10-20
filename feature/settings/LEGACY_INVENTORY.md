# Settings — legacy-first inventory and plan — 2026-10-21

Method: the rule in [ANDROID_REFACTORING_PLAN.md](../ANDROID_REFACTORING_PLAN.md#legacy-first-parity-gate-mandatory-for-every-owner), format as in
[../profile/LEGACY_INVENTORY.md](../profile/LEGACY_INVENTORY.md). Read-only analysis of the legacy `composeApp` code on branch
`feature/profile-refactoring` (`8530d25b`): no code changed, Gradle not run, nothing tried on a device. Every row is **open** until the owner
confirms it and ends as **preserved**, **improved (how)** or **intentionally removed (why, with approval)**.

Why this owner exists: the profile inventory (Q1, answered "separate owner later") left the Settings tab and its sub-screens in `composeApp`
next to the legacy `ProfileViewModel` singleton. After the profile migration the Android Profile tab no longer uses that singleton, but Settings
and the city screen still do, so on Android it now exists only to give Settings `isLoggedIn`, the city list and the theme.

There is **no `feature/settings` module yet**; this directory holds only this file.

## Scope and sizes (legacy, lines)

| Area | Files | Lines |
|---|---|---|
| `ui/screen/profile` — `SettingsScreen` 234, `ThemeScreen` 154, `CityScreen` 136, `UsagePolicyScreen` 200 | 4 | 724 |
| `ui/screen/profile/ProfileViewModel` (shared with the legacy Profile tab, now iOS/shared only) | 1 | 245 |
| `theme/ThemeManager` (object) 52, `settings/CityPreference` 45, `legal/LegalUrls` 9 | 3 | 106 |
| `ui/component/SettingsList` (rows, sections, icon palette, `AppVersionFooter`) 150, `SettingsIconBadge` | 2 | ~190 |
| `feature/appupdate` — `AppUpdateState` 85, `AppUpdatePrompt` 248, domain 56, `AppUpdateRepositoryImpl` 48, installers (Play/APK/Store), effects | ~12 | ~900 |
| `feature/authorblocking/ui/BlockedAuthorsScreen` 93 (owned by author-blocking, only linked from Settings) | 1 | 93 |
| Tests today: `ProfileViewModelTest` (8, includes the city-resolve case), `CityPreferenceTest` 28, appupdate 288 (4 files), `ReviewDraftPolicyTest` 32, `NativeTabRoutesTest`. **No test** for `SettingsScreen`, `ThemeManager`, `UsagePolicyScreen`, the consent toggle, or the settings rows | — | ~450 |

The design system already has `CoffeePeekSettingsSection/Row/Divider` (`core/design-system/.../CoffeePeekSettingsList.kt`), a newer copy of the
app's `SettingsList`; the app's copy is still what these screens use.

## (1) Inventory

### Settings tab (`SettingsScreen`)

| Piece | Behaviour to preserve |
|---|---|
| Header | Title `Настройки` (headlineMedium, bold). A signed-out user sees `GuestAuthCard(Войти → Auth, Регистрация → Register)` first; the rest is shown to guests too |
| Section `Добавить` | `Добавить кофейню` («Предложить новое место для CoffeePeek») → `AddShop`; `Добавить обжарщика` («Помогите сообществу открыть новых обжарщиков») → `AddRoaster`. **Neither the row nor `AddShopScreen`/`AddRoasterScreen` checks the session** (grep: no login/guest handling), so a guest reaches the forms; whether submitting fails with 401 was not verified |
| Section `Настройки` | `Заблокированные авторы` («Управление блокировками», violet) → `BlockedAuthors`, **signed-in only**; `Город` (aqua; trailing = selected city name or `Выберите`, chevron) → `CitySettings`; `Тема` (violet; trailing = `Авто/Светлая/Тёмная` + chevron, icon follows the mode) → `ThemeSettings` |
| Section `Другие настройки` | description «Управление полезными дополнениями, оценки в App Store и настройки конфиденциальности» (App Store wording shown on Android too); `Связаться с поддержкой` → `OpenInBrowser("https://coffeepeek.by/support")`; `Политика использования` → `UsagePolicy`; `Поделиться` → `ShareHelper.shareText(COFFEEPEEK_SHARE_TEXT)` |
| Section `Обновление приложения` | one row: label `Проверяем обновления…` + `PlatformActivityIndicator` (row disabled) while `checking`, else `Проверить обновления`; description = `state.message` ?: `Доступна новая версия CoffeePeek` (if an update is known) ?: `Версия ${versionName}`; tap → `AppUpdateState.check(manual = true)`. Below the sections `AppVersionFooter` (`Версия …`, so the version is shown twice) |
| Layout | `PlatformScrollableColumn`, status bar padding, bottom padding `spacing8 + LocalFloatingNavClearance`, page padding `settingsPagePadding`, section spacing `settingsSectionSpacing` |
| State source | `ProfileViewModel` singleton: `uiState.isLoggedIn`, `themeMode`, `cities`, `selectedCityId` — only these four are read |

### Theme (`ThemeScreen`, `ThemeManager`)

| Piece | Behaviour to preserve |
|---|---|
| Screen | `CpTopBar("Тема")`; headline `Внешний вид`, body «Выберите, как CoffeePeek будет выглядеть на вашем устройстве.»; one card with three rows (`Системная` cyan, `Светлая` gold, `Тёмная` violet; icons ThemeSystem/Light/Dark), selected row shows a check with content description `Выбрано`; tap sets the theme immediately, no confirmation |
| `ThemeManager` | **Global `object`**, started in `KoinApp` (`ThemeManager.initialize(settingRepository)`); `SettingRepository` key `theme_mode`; enum `SYSTEM/LIGHT/DARK`; unknown/missing value → `SYSTEM`; `setTheme` updates the flow first, applies the platform night mode (`applyPlatformNightMode`, expect/actual), then persists on `Dispatchers.Default`; `initialize` is idempotent and collects the setting flow forever |
| Consumers outside Settings | `App.kt` (theme of the whole app), iOS `NativeScreenController`, iOS `PlatformRootNavigation`, Sandbox root (a debug toggle). The theme is therefore **app infrastructure**, not a Settings-only state |

### City (`CityScreen`, `CityPreference`)

| Piece | Behaviour to preserve |
|---|---|
| Screen | `CpTopBar("Город")`; headline `Ваш город`, body «Определяет, какие кофейни показывать в первую очередь.»; a card with one row per city (aqua location badge, name, check `Выбрано` on the selected one); tap → `ProfileViewModel.setCity(id)` (ignored when the id is not in the loaded list). If the list is still empty (catalogs not loaded / failed) the screen shows an **empty card, no loader, no error, no retry** |
| `CityPreference` | key `selected_city_id`; `resolve(cities)` = saved id if still in the list, else the first city, persisted when it changed; `select(id)` ignores blank/same, updates the flow, persists asynchronously |
| Who resolves | `FeedViewModel` (:186) and `MapViewModel` (:376) both call `resolve(catalogs.cities)` themselves and observe `selectedCityId`; `ProfileViewModel.init` → `loadCities()` → `shopRepository.getCatalogs()` also resolves. So the city is resolved by Discovery with or without Settings; the singleton's resolve is only needed for the Settings list and the trailing value. The `ProfileViewModelTest` case «city is resolved even if Settings is never opened» pins the **current** startup behaviour and would have to change with the singleton's removal |
| Consumers outside Settings | Feed and Map filter by `selectedCityId` (Discovery owner) |

### Usage policy / privacy (`UsagePolicyScreen`)

| Piece | Behaviour to preserve |
|---|---|
| Layout | `CpTopBar("Политика использования")`; label `КОНФИДЕНЦИАЛЬНОСТЬ`; cards for consent, terms, privacy |
| Consent row | shown **only when** `Analytics.service != null && state.authReady && !state.userId.isNullOrBlank()` (a signed-in user with initialized analytics); title `Аналитика использования`, text «С вашего согласия события использования отправляются в PostHog (ЕС), чтобы улучшать CoffeePeek. Без текстов поиска, заметок, фото и точных координат.»; the whole card is `toggleable(Role.Switch)`, the switch itself is cleared from semantics; toggle → `analytics.setConsent(Accepted/Declined)` in a `rememberCoroutineScope` (Declined erases the queue and stops delivery) |
| Link cards | `Правила использования` («Узнайте, как работает CoffeePeek.») → `LegalUrls.TERMS` (`https://coffeepeek.by/terms`); `Политика конфиденциальности` («Как мы обрабатываем и защищаем ваши данные.») → `LegalUrls.PRIVACY`; `Role.Button`, `OpenInBrowser.openInBrowser` |
| Shared | `LegalUrls` is also used by the registration screen (Auth owner) — stays app/shared |

### App update (`feature/appupdate`, entry point = the Settings row)

| Piece | Behaviour to preserve |
|---|---|
| State | `AppUpdateState` (Koin `single`, `internal`): `check(manual)` ignores a call while `checking`; policy from `GET /api/v1/app-version?platform&channel` (`Cache-Control: no-store`, 10 s timeout, HTTPS-only URL, no credentials in the URL, `versionCode ≥ minVersionCode`); manual check with no update → `Установлена актуальная версия`, failure → `Не удалось проверить обновления. Попробуйте позже.`; a dismissed version is remembered (`dismissed_app_version` setting) and a **required** update (`current < minVersionCode`) cannot be dismissed; manual checks always prompt |
| Prompt | `AppUpdatePrompt` is composed by `App.kt` at the root (not by Settings); Android has Play and APK installers (`PlayUpdateRetry`, `ApkUpdateInstaller` with signer-compatibility check), iOS opens the store |
| Verdict for this owner | The Settings row only reads `state` and calls `check(manual = true)`. The update machinery has its own tests (288 lines), installers per store flavor and an iOS side — it is **its own owner (App update)**, not part of Settings. Settings gets a port |

### Blocked authors

`Заблокированные авторы` navigates to `BlockedAuthors`, whose screen and presenter belong to the author-blocking feature (`features/author-blocking`,
the screen is app-side `feature/authorblocking/ui`). Settings keeps only the row and the signed-in rule.

### Routes, tabs, analytics

| Piece | Behaviour |
|---|---|
| Routes (`Navigator.Screen`) | `SettingsTab`, `SettingsGraph`, `CitySettings`, `ThemeSettings`, `UsagePolicy`, `BlockedAuthors`; the last four are `isHandledByRootNav`. Class names are part of saved navigation state (iOS restores stacks) — keep |
| Tab shell | `MainScreen` composes `SettingsScreen()` for `SettingsTab` (always available, guests too, `mainTabScreens`); iOS uses the native tab shell by index (`nativeTabIndex`: Settings = 4) |
| Analytics | No `Analytics.track` calls in these screens. Pages by route class: `SettingsTab/SettingsGraph/CitySettings/ThemeSettings` → `settings`, `UsagePolicy` → `privacy`, `BlockedAuthors` → `other` (unmapped, pre-existing). `api_request_completed` for `/api/v1/app-version` and `/api/catalogs/*` is classified by `RequestObserver` — unchanged as long as the same client is used |
| Sandbox | `/api/catalogs/cities` is served; **`/api/v1/app-version` is not** (a manual check shows the failure text); `/api/v1/me/blocks` is served. The theme can be changed in the app |

## (2) Target architecture and plan

### Principles

Same as Profile/Coffee: Android first, iOS/shared keep the legacy `composeApp` UI through renderers with `Legacy…` defaults, no feature code in
`composeApp`, cross-feature access through `api`, domain models own their types, strings in `res/values/strings.xml` with a `settings_` prefix, Russian
verbatim, typed MVI (`core/presentation`), app owns Koin, navigation and analytics adapters.

### Modules

| Module | Responsibility | Depends on |
|---|---|---|
| `feature/settings/domain` | `ThemeMode` (moved as a domain type with the **same names**), `City` as a settings-owned `SettingsCity(id, name)`, `resolveSelectedCityId` (pure), `UsageConsent`, the settings-row availability rules as pure functions (e.g. blocked-authors row only when signed in) | — |
| `feature/settings/api` | Entries (Android Compose in `androidMain`): `SettingsEntry`, `ThemeEntry`, `CityEntry`, `UsagePolicyEntry`; **ports** supplied by the app: `ThemePort` (observe, set), `CityPort` (observe selected id, `loadCities()`, `select(id)`), `SettingsAccountPort` (observe signed-in), `UsageConsentPort` (availability + state flow, `set(accepted)`), `UpdateCheckPort` (observe `{checking, message, updateAvailable}`, `check(manual = true)`), `SettingsNavigation` callbacks (open shop/roaster forms, blocked authors, city, theme, policy, login, register, browser URL, share text), `SettingsAnalyticsPort` (none today; added only if the owner wants events) |
| `feature/settings/impl` | `SettingsViewModel`, `ThemeViewModel`, `CityViewModel`, `UsagePolicyViewModel` (typed MVI), stateless Compose content built on the design-system settings list, resources, previews, entries | api, domain, `core/presentation`, `core/design-system` |

No `data` module: the repositories behind the ports are the existing app ones (`SettingRepository` through `ThemeManager`/`CityPreference`, `ShopRepository.getCatalogs`,
the analytics service, `AppUpdateState`). If the owner later wants `ThemeManager` and `CityPreference` themselves moved, that is a separate infrastructure step (they are read by
`App.kt`, iOS and Discovery).

Stays **app-owned**: routes and serialization, `MainScreen`/`NativeTabRoutes` and the guest tab rules, `ThemeManager` and `CityPreference` (startup initialization,
platform night mode, iOS shell, Discovery), `AppUpdateState`/`AppUpdatePrompt`/installers, `Analytics` and its consent plumbing, `LegalUrls`, `OpenInBrowser`, `ShareHelper`,
`AppConfig`, `GuestAuthCard`, the Sandbox fixtures, Koin wiring (`KoinApp.kt`, `PlatformKoin.android.kt`).

### Staged migration (each step one PR-sized change, legacy kept)

| # | Step | Files (est.) | Verify |
|---|---|---|---|
| 0 | Fix this inventory with the owner (questions below); Sandbox: `GET /api/v1/app-version` (no update / update available / required / failure), a catalogs failure and an empty city list | 3–4 | `SandboxApplicationBackendTest` |
| 1 | **Parity tests on legacy first** (no behaviour change): `ThemeManagerTest` (default, invalid value, persistence order, idempotent initialize), settings-row rules (guest vs signed-in), city list/selection (`setCity` ignores unknown ids, trailing value), consent visibility (`authReady && userId`), consent toggle calls | 5–6 | unit tests green before and after |
| 2 | `feature/settings/domain` + `api` ports and the app adapters over `ThemeManager`, `CityPreference`+`ShopRepository`, `SessionRepository`, `Analytics`, `AppUpdateState`; adapter tests | ~15 | adapter unit tests |
| 3 | `impl` Theme + City + Usage policy (leaf screens first, they have no navigation) with VM tests and resources; Android entries behind `ThemeSettings`/`CitySettings`/`UsagePolicy`, iOS keeps legacy | ~25 | VM tests, device check in Sandbox, both themes, large font |
| 4 | `impl` Settings tab (`SettingsEntry`): rows on the design-system list, guest card slot, update row through `UpdateCheckPort`; Android entry behind `SettingsTab`; retire the singleton's Settings use on Android (the legacy `ProfileViewModel` stays for the iOS/shared Profile tab until iOS migrates) | ~20 | tab, guest/signed-in, update states in Sandbox, rotation |
| 5 | Retroactive audit against this file; decide on removing the legacy Settings copies only when iOS consumes the new module | docs | audit rows closed |

### Risks / regressions to guard

| id | sev | stable prod behaviour to keep | guard |
|---|---|---|---|
| S1 | H | The theme applies immediately and survives a restart; invalid saved value → system; startup initialization stays app-owned so `App.kt` and iOS see the same flow | `ThemeManagerTest`; device: switch, kill, relaunch |
| S2 | H | Discovery still resolves the city by itself (Feed/Map), so moving the Settings list must not remove or delay that; a saved city missing from a new list falls back to the first one | keep `CityPreferenceTest`; add a test that Feed/Map resolve without Settings (replaces the `ProfileViewModelTest` case) |
| S3 | H | The analytics consent row appears only for a signed-in user with initialized analytics, and `Declined` erases the queue | visibility table test, adapter test against `Analytics.service` fake |
| S4 | M | Row rules: `Заблокированные авторы` only signed in; the guest card only for guests; Add shop/roaster rows are shown to everyone (see question 2) | settings-row rule tests |
| S5 | M | `Поделиться` shares exactly `COFFEEPEEK_SHARE_TEXT`; support opens `https://coffeepeek.by/support`; terms/privacy URLs from `LegalUrls` | table tests |
| S6 | M | Update row states: checking (disabled + spinner), no update, update available, failure, required update not dismissable; the root prompt keeps working | adapter test over `AppUpdateState`; Sandbox fixtures (step 0) |
| S7 | M | Singleton `ProfileViewModel` still drives the iOS/shared Profile tab; removing its Settings use on Android must not change iOS | iOS compile; legacy `ProfileViewModelTest` stays |
| S8 | M | Analytics page names come from route classes — class names do not change; `BlockedAuthors` stays `other` unless the owner maps it | `analyticsPage` table test |
| S9 | L | Texts verbatim (including the App Store wording, see question 5) and content descriptions (`Выбрано`); large-font layouts of the rows and the consent card | string snapshot, manual large font |
| S10 | L | Recreation: the screens hold no draft state, but the consent switch and the update `checking` state must survive rotation without a duplicate check | manual rotation |

### Open questions for the owner

1. **Theme and city infrastructure**: keep `ThemeManager` and `CityPreference` app-owned behind ports (recommended: Discovery, iOS and `App.kt` read them), or move them into the module later as a separate step?
2. **Add shop / Add roaster rows for guests**: preserve (guests reach the forms; submit probably fails with 401), or send a guest to sign-in first (a deliberate difference)?
3. **Empty city list**: preserve the empty card, or add loader, error and retry (a deliberate improvement; the legacy screen has none)?
4. **App update**: confirm it is its own owner and Settings only gets `UpdateCheckPort`; the root `AppUpdatePrompt` and installers stay as they are.
5. **App Store wording** in the «Другие настройки» description on Android («оценки в App Store»): keep verbatim, or reword for Android in a deliberate difference?
6. **Duplicate version text** (row description and `AppVersionFooter` both show the version): keep, or drop the footer?
7. **Singleton retirement**: after step 4 the legacy `ProfileViewModel` is used only by the iOS/shared Profile tab. Keep it as is until iOS migrates (recommended), or split it now so iOS gets a slimmer view model?
8. **Analytics**: still no events in these screens. Keep none (parity), or add events (theme/city changed, consent toggled)? New events need the wire contract in `AnalyticsEvent`.
9. **`BlockedAuthors` analytics page** is `other` today. Leave it or map it to `settings`?
10. **Naming**: `feature/settings` (singular, as Shop/Roaster/Profile) with package root `com.coffeepeek.feature.settings` and resource prefix `settings_`: confirm.

### Recommendations

Answer 1 app-owned, 2 preserve, 3 improve (loader/error/retry is low risk), 4 yes, 5 keep verbatim until a product decision, 6 keep, 7 keep, 8 none, 9 leave, 10 yes.
Start with step 0–1 (fixtures and parity tests), then the leaf screens, then the tab.
