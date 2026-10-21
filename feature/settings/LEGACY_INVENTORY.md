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

## Owner decisions (2026-10-21)

All ten questions were answered with the recommendations:

1. `ThemeManager` and `CityPreference` stay app-owned behind ports.
2. Add shop / Add roaster rows stay as in prod (shown to guests; no new sign-in redirect).
3. **Deliberate improvement**: the city screen gets a loader, an error and a retry instead of an empty card.
4. App update is its own owner; Settings gets only `UpdateCheckPort`.
5. The «оценки в App Store» wording stays verbatim on Android until a product decision.
6. The version is shown twice (row description and footer), as in prod.
7. The legacy `ProfileViewModel` stays for the iOS/shared Profile tab until iOS migrates.
8. No analytics events in these screens.
9. `BlockedAuthors` stays on the unmapped `other` page.
10. `feature/settings`, package root `com.coffeepeek.feature.settings`, resource prefix `settings_`.

## Step 0 result — Sandbox fixtures (2026-10-21)

`SandboxApplicationBackend` now serves, selectable from the Sandbox controls (new chip rows «Обновление приложения» and «Города»; they survive scenario
changes, and «Заполнить»/«Очистить» return them to the defaults):

- `GET /api/v1/app-version` — `UpToDate` (default: version 1, nothing newer, so the root check never prompts), `Available` (optional newer version), `Required`
  (`minVersionCode` above any build, cannot be dismissed), `Failure` (503). The download URL is on the blocked sandbox host; `SandboxUpdateInstaller` already refuses to download.
- `GET /api/catalogs/cities` — `Single` (Минск, default, unchanged for the existing tests), `Several` (Минск, Брест, Гродно), `Empty` (`[]`), `Failure` (500).

`SandboxScenario.ServerError`/`Offline` still win over both. Covered by `SandboxApplicationBackendTest.settingsFixturesServeTheUpdatePolicyAndTheCityCatalogThroughTheRealParsers`
(real `AppVersionResponse.toUpdate()` validation, `ShopApiService.getCities`, defaults after `reset`, scenario precedence). The `dismissed_app_version` setting lives in the Sandbox's own
database and survives a restart of the process; clearing the app data resets it.

Not covered: the Play/APK installer flows (the Sandbox installer never downloads), a catalogs failure that leaves other catalogs working (the app reads cities from the same call), real `app-version` acceptance.

Device check (Pixel 7, Sandbox): the new chip rows render in the controls dialog; choosing «Есть версия» and tapping «Проверить обновления» on the Settings tab changes the row
description to «Доступна новая версия CoffeePeek». Selecting a chip re-creates the app (as the existing scenario chips do) and closes the dialog. The other policies and the city modes were
checked only through the unit test, not on the device.

## Step 1 result — parity tests on the legacy Settings code (2026-10-21)

No production code changed. Added to `composeApp/src/commonTest` (all green on the Android host tests, repeated on a clean run; iOS test compilation green):

- `theme/ThemeManagerTest` (2): the saved mode is restored, an invalid or missing value means `SYSTEM`, `setTheme` publishes the flow before the setting is saved (same key, enum name), a second
  `initialize` is ignored (the manager is a process-wide `object`, so the persistence rules are one ordered scenario), the mode order.
- `settings/CityPreferenceStorageTest` (5) next to the existing pure `CityPreferenceTest` (3): a saved city that still exists is kept and not rewritten; missing/removed → first city, persisted;
  empty catalog → `null` and nothing saved; the in-memory choice wins on the next resolve; `select` ignores blank and repeated ids. Shared `FakeSettingRepository`.
- `ui/screen/profile/ProfileViewModelTest` (+3, now 11): a catalog failure leaves the list empty with no selection and `setCity` ignores everything (this is the **empty city screen** the owner decided to improve);
  a saved city survives the next start, a removed one falls back; the signed-in flag that Settings reads follows the session. The existing «city is resolved even if Settings is never opened» case stays.
- `analytics/SettingsPagesTest` (2): `SettingsTab/SettingsGraph/CitySettings/ThemeSettings` → `settings`, `UsagePolicy` → `privacy`, `BlockedAuthors` → `other`.

Already covered before this step and left as is: `AppUpdateStateTest`/`AppUpdateTest`/`PlayUpdateRetryTest`/`UpdateInstallationTest` (the update row's state source).

**Not covered on purpose** (the logic is inline in composables, and the Compose UI suites were removed at the owner's request, see `feature/UI_TEST_REMOVAL.md`): the row rules of `SettingsScreen`
(blocked-authors row only signed in, guest card only for guests, add-shop/roaster rows for everyone), the support/share/terms/privacy targets, and the consent row (`authReady && userId`, toggle → `setConsent`).
These move into `SettingsViewModel`/`UsageConsentPort` at steps 3–4, where they get unit tests; until then Sandbox and device checks cover them. Nothing here changed behaviour, so no test had to flip.

## Step 2 result — `feature/settings` domain and api, ports and app adapters (2026-10-21)

- **Modules** `feature/settings/domain` and `feature/settings/api` (KMP, iOS targets compile), registered in `Modules.kt` and used by `composeApp`. No `impl` yet, so nothing is bound in Koin and no screen
  changed: the legacy Settings code still runs on every platform.
- **domain**: `ThemeMode` (same names as the persisted setting), `SettingsCity`, `UsageConsentState(available, accepted)`, `UpdateCheckState(checking, message, updateAvailable)`. The pure
  `resolveSelectedCityId` from the plan was **not** copied: its only consumer is the app-owned `CityPreference`, so a second copy would be dead code.
- **api ports**: `ThemePort`, `CityPort` (`loadCities` = load the catalog and resolve the saved choice, as production did; a failure changes nothing), `SettingsAccountPort`, `UsageConsentPort`,
  `UpdateCheckPort` (a check is always manual), `SettingsExternalActions` (support, terms, privacy, share) and `SettingsAppInfo(versionName)`. Navigation callbacks come with the entries (steps 3–4).
- **App adapters** (`composeApp/androidMain/di/settings/AndroidSettingsPorts.kt`) over `ThemeManager`, `CityPreference` + `ShopRepository`, `SessionRepository`, `AnalyticsService`, `AppUpdateState`,
  `OpenInBrowser`/`ShareHelper`/`LegalUrls`. `AndroidSettingsPortsTest` (10): theme mapping both ways through the global manager; city resolve-on-load, failure changes nothing, select persists and
  ignores blank; signed in = active session **with a user id** (legacy rule) and the distinct flow; consent unavailable without a service and before a signed-in identity, follows the toggle and the store,
  turns unavailable on sign-out; update row mapping with manual checks (update available, current version, failure message); the legacy URLs and share text.
- **Checks**: `testDirectDebugUnitTest --tests '*di.settings*'` green, `compileKotlinIosSimulatorArm64` (domain, api, composeApp) and `compileDirectDebugKotlinAndroid` green.

## Step 3 result — Theme, City and Usage policy in `feature/settings/impl` (2026-10-21)

- **Module** `feature/settings/impl` (resource prefix `settings_`, Russian strings verbatim except the two new city texts), `feature/settings/api` now a Compose library with the entries `ThemeEntry`, `CityEntry`,
  `UsagePolicyEntry` (`createThemeEntry`, `createCityEntry`, `createUsagePolicyEntry` in impl). The design system gained `Shield` and `FileText` icons.
- **ViewModels** (typed MVI, state in the destination's ViewModel store): `ThemeViewModel` (starts on the current mode, applies at once, follows changes made elsewhere), `CityViewModel` (loads the catalog on open,
  only listed ids can be selected, a loaded list is not reloaded, two retries in one frame load once), `UsagePolicyViewModel` (the switch follows the app's analytics state, toggling while unavailable does nothing).
  11 tests (3 + 5 + 3).
- **Deliberate improvement (decision 3)**: the city screen shows a loader, then either the list or «Не удалось загрузить города» / «Список городов пуст» with «Повторить» instead of an empty card. The empty-catalog
  text is mine (decision 3 only named loader/error/retry): an empty successful answer is treated as a retryable problem too.
- **App**: `SettingsScreenRenderer` (Legacy default for iOS/shared, Android entry), `Navigator` routes `ThemeSettings`/`CitySettings`/`UsagePolicy` use it, Koin param `settingsRendererFactory`. The legacy Settings tab
  still runs and reads the same `ThemeManager`/`CityPreference`, so a choice made on the new screens shows on the tab immediately.
- **Sandbox**: the city catalog is cached by `ShopRepositoryImpl` after its first successful load, so the «Города» chips only take effect before the first load. The activity now also reads launch extras:
  `am start -n com.coffeepeek.sandbox/com.coffeepeek.admin.sandbox.SandboxActivity --es sandbox_cities Several|Empty|Failure|Single --es sandbox_update Available|Required|Failure|UpToDate`
  (applied on a fresh launch only, chips show the chosen values). The chips remain for the update policy, which is not cached.
- **Checks**: `:feature:settings:impl:testDebugUnitTest` (11), `compileDirectDebugKotlinAndroid`, `compileKotlinIosSimulatorArm64` (api, impl, composeApp), `assembleDirectSandbox` green.
- **Pixel 7 Sandbox check**: Theme in light and dark (check mark, immediate switch, back button, the tab row follows: «Тёмная»); City with one city, three cities (Брест selected, the tab row shows «Брест»), empty list and
  load failure with «Повторить»; Usage policy with the two link cards in dark and light; font scale 1.5 on Theme and Usage policy (cards wrap, the long top-bar title is cut with an ellipsis as in the legacy bar).
  Font scale and theme were restored afterwards.
- **Not checked**: the consent switch (Sandbox runs with analytics disabled, so the row is hidden; only the unit tests cover it), tapping the terms/privacy cards (Sandbox blocks external apps), retry succeeding after a failure,
  rotation, the City screen at font scale 1.5, TalkBack.

## Step 4 result — the Settings tab in `feature/settings/impl` (2026-10-21)

- **api**: `SettingsEntry` (callbacks for add shop/roaster, blocked authors, city, theme, usage policy, login, register; `bottomClearance`), `SettingsSlots(guestCard)` (the app keeps `GuestAuthCard`).
- **impl**: `SettingsViewModel` (signed-in flag, theme, selected city name, update row; it loads the city catalog on creation like the legacy singleton did — that also resolves the saved city — and loads it again when
  the selection names a city it does not know yet) and `SettingsScreen` on the design-system settings list. Row rules are state: guest card for guests, blocked-authors row only signed in, add-shop/roaster rows for
  everyone (decision 2), city trailing «Выберите» until the name is known, update row with the checking spinner / message / «Доступна новая версия CoffeePeek» / version, the version footer too (decision 6), the App Store wording
  verbatim (decision 5). 6 new tests (module total 17).
- **Design system**: `CoffeePeekSettingsSection` now upper-cases its title like the legacy `SettingsSection` (found on the device: the first build showed «Добавить», production shows «ДОБАВИТЬ»); it had no other consumer.
- **App**: `SettingsScreenRenderer.Tab()` (Legacy default, iOS keeps `SettingsScreen` via its native shell), `MainScreen` composes the renderer, Android entry wired with the navigation callbacks, `LocalFloatingNavClearance`, `AppConfig.versionName`
  and the Koin singletons (`ShopRepository`, `CityPreference`, `SessionRepository`, `AppUpdateState`). On Android the legacy `ProfileViewModel` singleton is no longer created by any screen (the Profile tab left it in step 5, Settings now), so its
  startup work (session observer, city catalog) happens in `SettingsViewModel` instead; Feed and Map still resolve the city themselves. It remains for the iOS/shared Profile tab (decision 7).
- **Checks**: `:feature:settings:impl:testDebugUnitTest` (17), `compileDirectDebugKotlinAndroid`, `compileKotlinIosSimulatorArm64` (impl, composeApp), `assembleDirectSandbox` green.
- **Pixel 7 Sandbox check**: signed-in tab (all sections, theme «Авто», city value follows the selection: «Брест» after the city screen, «Минск» fresh), the update row with the `Available` policy → «Доступна новая версия CoffeePeek» and with `UpToDate`
  → «Установлена актуальная версия», guest mode (card «Присоединяйтесь…» with «Войти»/«Зарегистрироваться», no blocked-authors row, no Profile tab) and «Войти» opening the login screen, section titles upper-cased, bottom clearance above the floating bar.
- **Not checked**: the checking spinner state (the Sandbox answers instantly), the update row for `Required`/`Failure`, «Зарегистрироваться» and the add-shop/roaster rows' targets, support/share (Sandbox blocks external apps), large font and rotation on the tab, TalkBack,
  the blocked-authors row target (author-blocking owner).

## Step 5 result — retroactive audit of the activated Android path (2026-10-21)

Checked the risk table (S1–S10), the inventory rows and the strings against the migrated code, tests and the Pixel 7 Sandbox runs. **No defect needing a code change was found in this step.** The one real
regression of the migration (section titles not upper-cased) was found on the device in step 4 and is already fixed. No code was changed in this step.

| id | status |
|---|---|
| S1 theme | kept: `ThemeManager` stays app-owned behind `ThemePort`; `ThemeManagerTest`, `ThemeViewModelTest`, the adapter test; device: immediate switch in both directions, the choice survived a process restart, the tab row follows |
| S2 city | kept: Feed (`FeedViewModel.loadCatalogs`) and Map (`MapViewModel.loadCatalogs`) still call `CityPreference.resolve` themselves. Guard: `FeedNearbySelectionTest.locationBeforeCatalogsKeepsInitialSearchInTheSelectedCity` (Feed resolves with no Settings screen anywhere). **Map has no such test** — verified by reading the code only. Fallback to the first city: `CityPreferenceStorageTest`, the port test; device: one, three cities, the tab and the screen agree |
| S3 consent | availability rule and toggle covered by the adapter test (unavailable without a service and before a signed-in identity, follows `setConsent`, unavailable after sign-out) and `UsagePolicyViewModelTest`. **Not on a device**: the Sandbox runs with analytics disabled |
| S4 row rules | state properties tested (`SettingsViewModelTest`); device: signed-in tab and guest tab (card, no blocked-authors row, no Profile tab), add rows for both |
| S5 links | `AndroidSettingsExternalActions` test pins support, terms, privacy and the share text; not tapped on the device (the Sandbox blocks external apps) |
| S6 update row | adapter test (available / current / failure message) + device: current, available, failure («Не удалось проверить обновления…»), required (row text only). The root `AppUpdatePrompt` lives in `App.kt`, which the Sandbox root does not compose, so the prompt for a required update is **not testable in the Sandbox**; the checking spinner is too fast to catch |
| S7 singleton and iOS | the legacy `ProfileViewModel` is no longer created by any Android screen. Every consumer of the cached profile loads it itself (`getMe`/`refreshProfile` in the shop session, shop detail, community, public profile, edit, deletion), so nothing relied on its startup refresh. iOS compile green; the legacy `ProfileViewModelTest` (11) and `SettingsScreen` stay for iOS/shared |
| S8 analytics pages | `SettingsPagesTest` (route class names unchanged) |
| S9 texts | every Russian string literal of the four legacy screens exists verbatim in `settings/impl` `strings.xml` (checked mechanically; the only miss is the version template, implemented as `Версия %1$s`); new texts: the two city error messages. Large font 1.5 on the theme, usage-policy and tab screens: wrapping works, long top-bar titles and the 3-line row descriptions end with an ellipsis exactly as the legacy rows did |
| S10 recreation | ViewModels live in the destination's store; device: landscape on the tab keeps the scroll position and content; no draft state exists. Rotation on the leaf screens and a rotation during an update check were not run |

**Inventory rows**: preserved — rows, texts, rules, theme and city persistence, update row states, consent rules; improved on purpose — city screen (loader, error, retry, empty text), guest tapping nothing new; unchanged
by design — add-shop/roaster rows for guests, App Store wording, duplicate version text, no analytics events, `BlockedAuthors` on `other`.

**Legacy that stays and why**: `SettingsScreen`, `ThemeScreen`, `CityScreen`, `UsagePolicyScreen` and the `ProfileViewModel` singleton remain the iOS/shared path (Legacy renderers and the iOS native tab shell). They can go
only when iOS consumes `feature/settings`. `ThemeManager`, `CityPreference`, the update machinery and `LegalUrls` stay app-owned by decision.

**Open, not verified**: Map city resolution test, consent switch on a device, required-update prompt, spinner state, external links, TalkBack, rotation of the leaf screens. Next owners: App update (own owner), Discovery, Auth, Community, Contributions.
