# Структура проекта CoffeePeek

Документ описывает, как репозиторий устроен сейчас: где лежит общий код, где Android, где iOS и что у каждой платформы своё.
Источник истины по сборке — Gradle-файлы; документ обновляется вместе с ними.
Даты в других документах `feature/*` и `docs/*` — исторические снимки миграции, а не текущая структура.

## 1. Коротко

- Один репозиторий, один Gradle-проект, Kotlin Multiplatform + Compose Multiplatform. Цели: Android, iOS (`iosArm64`, `iosSimulatorArm64`).
- Бизнес-код и **почти весь UI общий**: он написан на Compose Multiplatform и живёт в `feature/*` и `core/*`.
- Платформенное сделано тремя способами: `expect/actual` в Kotlin, порт-интерфейс с реализацией в `androidMain` / `iosMain`, и Swift-код в `iosApp`.
- `composeApp` — только сборка приложения: старт, конфигурация, корневой DI, корневая навигация и платформенный клей. Бизнес-экранов в нём нет.
- Android — это модуль `composeApp` (приложение). iOS — Xcode-проект `iosApp`, который подключает Kotlin-фреймворк `ComposeApp`, собранный из того же `composeApp`.

```
CoffeePeek/
├── composeApp/      приложение: общий код + Android + iOS + Sandbox (Android)
├── feature/         бизнес-фичи, по 4 модуля на фичу (api / domain / data / impl)
├── core/            общая инфраструктура: сеть, сессия, БД, медиа, дизайн-система
├── iosApp/          Xcode-проект iOS (SwiftUI-оболочка, SPM-пакеты, xcconfig)
├── iosXtool/        Swift-исходники, общие с iosApp, и сборка через xtool
├── build-logic/     конвенции Gradle и список модулей (Modules.kt)
├── buildSrc/        общие зависимости и мелкие Gradle-задачи
├── tools/           скрипты iOS и релизов (Python)
├── docs/            документация процессов и подсистем
├── design/          исходники иконок уведомлений
└── .github/workflows/  CI: Android, iOS, релизы
```

## 2. Три уровня кода

| Уровень | Где | Что содержит | Зависит от |
|---|---|---|---|
| Приложение | `composeApp` | старт, корневой DI (Koin), корневая навигация, адаптеры между фичами, платформенные интеграции | всех фич и core |
| Фичи | `feature/<имя>/{api,domain,data,impl}` | бизнес-возможность целиком: контракты, правила, сеть, UI и ViewModel | core, `api` других фич |
| Core | `core/*` | сеть, сессия, БД, изображения, дизайн-система, база MVI | только внешние библиотеки |

Правило: фича не знает о приложении и о других фичах напрямую. Всё, что нужно от других (навигация, чужие данные, платформенные вещи),
она получает через интерфейсы в своём `api`, а реализацию подставляет `composeApp`.

## 3. Модуль фичи

Каждая фича состоит из четырёх Gradle-модулей. Исключение: `settings` не имеет `data`.

| Модуль | Содержимое | Платформы |
|---|---|---|
| `api` | интерфейсы-точки входа (`…Entry`), порты для приложения (аналитика, навигационные колбэки, чужие данные), публичные модели | KMP, Android + iOS |
| `domain` | модели, репозиторные интерфейсы, правила, use case | чистый KMP, без Compose и сети |
| `data` | HTTP-клиент на Ktor, DTO, мапперы, реализации репозиториев; тесты на `MockEngine` | KMP; только у `shelf` есть платформенный сканер |
| `impl` | UI на Compose, ViewModel (MVI на базе `core/presentation`), строки и ресурсы фичи | KMP; платформенный код — в единичных случаях (ниже) |

Зависимости: `data` → `domain`; `impl` → `api` + `domain` + `core`; `api` не зависит от `impl`. `composeApp` зависит от всех четырёх.
Каждый модуль, у которого есть ресурсы, владеет своим `composeResources` (`values` — русский, `values-be` — белорусский).

### Фичи

`app-update`, `auth`, `author-blocking`, `business-ownership`, `check-in-history`, `coffee`, `collections`, `community`, `content-report`,
`contributions`, `discovery`, `favorites`, `home-check-in`, `leaderboard`, `notifications`, `profile`, `roaster`, `settings`, `shelf`,
`shop-report`, `shop` — всего 21.

### Где внутри фич есть платформенный код

| Фича | Android (`androidMain`) | iOS (`iosMain`) |
|---|---|---|
| `app-update` (api, impl) | установщики обновлений: APK и Play | — (магазин подключает `composeApp`) |
| `auth` (impl) | вход через Google (`AndroidGoogleSignIn`, `AndroidAuthEntries`) | — (нативные провайдеры в Swift и `composeApp`) |
| `business-ownership` (impl) | `OwnershipProfileMenu.android` | `OwnershipProfileMenu.ios` |
| `profile` (impl) | `UsernameRetryAt.android` | `UsernameRetryAt.ios` |
| `shelf` (data) | сканер пачки на ONNX Runtime (`AndroidBagScanner`, `OnnxPackNetworks`) | сканер на ORT (`IosBagScanner`, `OrtPackNetworks`, `IosShelfPhoto`) |

Остальные фичи целиком в `commonMain`.

## 4. Core

| Модуль | Назначение | Платформенные части |
|---|---|---|
| `network` | Ktor-клиенты (`ApiClients`), авторизация по сессии, обновление токена, кэш публичных ответов, диагностика | движок: OkHttp (Android) / Darwin (iOS); `HttpDebugLog` |
| `session` | хранилище сессии (`SessionStore`/`SessionReader`) | токены: `EncryptedSharedPreferences` (Android) / Keychain (iOS) |
| `database` | Room (KMP), общая настройка | построитель БД на каждой платформе (`MyRoomRepository.android/.ios`) |
| `media` | загрузка и кэш изображений, просмотрщик, предзагрузка | декодеры и загрузчики: `AndroidImageCodec`, `IosImageBitmapDecoder`, `ImageFetcherFactory` |
| `design-system` | токены, тема, шрифты, иконки, компоненты, «стекло», нативные контролы | 42 платформенных файла (17 Android, 25 iOS): на iOS нативные UIKit-контролы, на Android Compose-реализации |
| `presentation` | базовый MVI `ViewModel` | — |
| `datetime` | разбор и форматирование дат с сервера | `ServerJavaInstant` (Android) |

Ресурсы `core/design-system` и `core/media` также двуязычные (ru, be).

## 5. composeApp

Наборы исходников (число `.kt` файлов на момент написания):

| Набор | Файлов | Что внутри |
|---|---|---|
| `commonMain` | 270 | общий код приложения |
| `androidMain` | 62 | Android-реализации `expect`, Android-точки входа |
| `iosMain` | 51 | iOS-реализации `expect`, мосты к Swift |
| `androidSandbox` | 45 | автономная версия с фальшивым бэкендом (только Android) |
| `commonTest` | 94 | общие тесты |
| `androidUnitTest` | 59 | unit-тесты Android |
| `androidInstrumentedTest` | 10 | инструментальные тесты на устройстве |
| `iosTest` | 9 | тесты iOS |

### commonMain

| Пакет | Содержимое |
|---|---|
| `App.kt` | корневой Composable: тема, язык, навигация, корневые хосты |
| `di/` (146 файлов) | Koin-модули по фичам (`di/shop`, `di/favorites`, `di/profile` …), рендереры экранов, адаптеры и host-ViewModel |
| `ui/` | `Navigator` и маршруты, `MainScreen`, плавающая панель вкладок, интерфейсы-рендереры `ui/screen/*Renderer`, диалоги, платформенные контролы |
| `analytics/` | PostHog и события |
| `session/`, `auth/`, `settings/`, `theme/`, `location/`, `map/`, `notifications/`, `utils/`, `config/` | платформенно-нейтральная часть приложения; их платформенные части — через `expect` |
| `base/` | базовый класс ViewModel приложения |

Правило для `di/`: сюда попадает **соединение** (кто что получает), а не бизнес-логика. Экраны и ViewModel лежат в `feature/*/impl`.

### androidMain

- `MainActivity`, `CoffeePeekApplication`, `AndroidManifest.xml`, ресурсы Android (`res/`).
- `di/PlatformKoin.android.kt` — Android-граф: модули фич и фабрики рендереров.
- Android-актуалы: геолокация, карта (`map/CoffeeMap.android.kt`, маркеры), нативные контролы, выбор фото, push (`AndroidPush`, `CoffeePeekMessagingService`), вход через Google, подсказка выбора коллекции после «сердечка» (`di/collections/*`).

### iosMain

- `MainViewController.kt` — вход Compose в UIKit, обработка ссылок (`handleAppLink`).
- `di/PlatformKoin.ios.kt` — iOS-граф (`iosPlatformModules`).
- `ui/PlatformRootNavigation.ios.kt` — нативная навигация (см. раздел 7), нативная панель вкладок.
- `map/` — мост к нативной карте (`IosNativeMapRegistry`, куда Swift регистрирует провайдер MapLibre).
- iOS-актуалы: геолокация, нативные контролы, камера и фото, вход через Apple и Google (`IosAppleAuthorizer`, `IosGoogleSignInLauncher`), установщик обновления (ссылка на магазин).

### androidSandbox

Отдельный вариант приложения без реального бэкенда: `SandboxApplication`, `SandboxActivity`, фальшивый HTTP-бэкенд (`sandbox/backend`), тестовые данные.
Только Android. Запуск и сценарии описаны в `composeApp/SANDBOX.md` и `composeApp/ANDROID_RUN_MODES.md`.

## 6. Общее и своё у платформ

| Область | Общее (`commonMain`) | Android | iOS |
|---|---|---|---|
| Экраны и ViewModel фич | всё | — | — |
| Тема, шрифты, иконки, строки | всё | — | — |
| Навигация: маршруты и `Navigator` | да | Compose `NavHost` в общем `Navigator` | те же маршруты, но стеки UIKit |
| Корневая панель вкладок | список вкладок | плавающая Compose-панель | нативный `UITabBarController` |
| Сеть и репозитории | всё | движок OkHttp | движок Darwin |
| Хранение токенов | интерфейс `SessionStore` | `EncryptedSharedPreferences` | Keychain |
| База данных | Room (KMP), схемы | builder Android | builder iOS |
| Изображения | загрузка, кэш, просмотрщик | декодеры Android | декодеры iOS |
| Карта | интерфейс `CoffeeMap`, данные | Android SDK MapLibre в `androidMain/map` | нативный MapLibre в Swift, подключается через `IosNativeMapRegistry` |
| Вход через Apple | API | недоступен (`isSupported = false`) | нативный `IosAppleAuthorizer` |
| Вход через Google | API | Android-реализация (`AndroidGoogleSignIn`) | Swift `SocialSignInProvider` + мост `IosGoogleSignInLauncher` |
| Push | включение, токен | `FirebaseMessagingService`, `AndroidPush` | Firebase в Swift (`PushAppDelegate`) |
| Фото и камера | интерфейсы, сжатие, загрузка | системные пикеры Android | системные пикеры iOS, `IosShelfCamera` |
| Геолокация | интерфейс `PlatformLocation` | Android | iOS |
| Сканер пачки (полка) | интерфейс, UI | ONNX Runtime | ORT |
| Обновление приложения | экран и логика проверки | APK-установщик и Play | переход в магазин |
| Аналитика | события | PostHog Android | PostHog iOS (`PostHogTransport`) |
| Нативные контролы (меню, шторки, переключатели) | интерфейс в `core/design-system` | Compose-реализации | UIKit-контролы |

Как принято решать, где писать код:
1. Если нужен общий UI или логика — в `feature/*/impl` или `domain`, без платформенных зависимостей.
2. Если платформа должна что-то сделать (геолокация, фото, вход) — интерфейс в `api` фичи или `expect` в `composeApp`, реализация в `androidMain` и `iosMain`.
3. Если реализация на Swift — Swift-класс регистрируется при старте в Kotlin-реестре (пример: `IosNativeMapRegistry.provider`), Kotlin о нём знает только через интерфейс.

## 7. Навигация и DI

### Навигация
- Маршруты и `Navigator` общие (`composeApp/.../ui/Navigator.kt`, Navigation Compose).
- Android: единый Compose `NavHost`, внизу плавающая панель вкладок (`FloatingBottomNavBar`).
- iOS: `PlatformRootNavigation.ios` возвращает управление UIKit. Нативный `UITabBarController` с пятью `UINavigationController` (Поиск, Кофе, Лента, Профиль, Настройки; у гостя четыре), каждый экран — Compose внутри `UIViewController`.
  Нативные переходы, жест «назад», шторки через `UISheetPresentationController`. Подробности: `docs/IOS_NATIVE_NAVIGATION.md`.
- Ссылки (`appLinkScreen`) и открытие экранов из уведомлений обрабатываются на обеих платформах одним кодом.

### DI (Koin)
- `di/KoinApp.kt` (общий) собирает корневой граф: транспорт, сессию, фабрики рендереров.
- `PlatformKoin.android.kt` и `PlatformKoin.ios.kt` добавляют платформенные модули и рендереры.
- Фича сама Koin не использует: её `impl` предоставляет функцию `create…Entry(...)`, вызывает её `composeApp`.
- Android-подсказка коллекций (`androidCollectionFavoritesModule`) включается только в Android-графе.

## 8. Android

| Что | Где |
|---|---|
| Приложение | `composeApp` (`namespace`/`applicationId` `com.coffeepeek`; minSdk 31, targetSdk 36, compileSdk 37) |
| Варианты | измерение `delivery`: `direct` (обновления по APK) и `play`; типы сборки: `debug`, `sandbox`, `release` |
| Типичные сборки | `directDebug` (реальный бэкенд), `directSandbox` (фальшивый бэкенд, пакет `com.coffeepeek.sandbox`), `playDebug`, релизы `directRelease` / `playRelease` |
| Тесты | `commonTest` и `androidUnitTest` — на JVM; `androidInstrumentedTest` — на устройстве |
| Проверки сборки | `verify*RuntimeClasses`, `verifyDirectSandboxIsolation` — в `composeApp/build.gradle.kts` |

Подробности запуска: `composeApp/ANDROID_RUN_MODES.md`, `composeApp/SANDBOX.md`, `build-logic/ANDROID_RUNTIME_PACKAGING.md`.

## 9. iOS

| Что | Где |
|---|---|
| Xcode-проект | `iosApp/iosApp.xcodeproj` (схема `iosApp`, цель — `CoffeePeek.app`) |
| Вход | `iosApp/iosApp/CoffeePeekApp.swift`: SwiftUI `@main`, регистрирует нативные провайдеры и показывает Compose-корень |
| Swift-провайдеры | `iosXtool/CoffeePeek/Sources/CoffeePeek/`: `MapLibreMapProvider`, `SocialSignInProvider`, `CoffeePeekLaunchView` (подключены в Xcode-проект по ссылке) |
| Kotlin-фреймворк | `ComposeApp` (статический), собирается Gradle из `composeApp`; в проекте скрипт-фаза «Compile Kotlin Framework» |
| Swift-пакеты | MapLibre Native, GoogleSignIn, Firebase (через SPM), плюс сгенерированный `KotlinMultiplatformLinkedPackage` |
| Конфигурация | `iosApp/Configuration/*.xcconfig` (`Shared`, `Debug`, `Release`, `Local.xcconfig.example`), `Info.plist`, `CoffeePeek.entitlements` |
| Firebase-конфиг | `iosApp/copy-firebase-config.sh`; без конфига Debug собирается (push недоступны) |
| Локализация системных строк | `ru.lproj` и `be.lproj` (`InfoPlist.strings`) |
| ONNX | скрипт `tools/ios/fix_onnx_framework.py` выравнивает минимальную версию iOS у встроенного фреймворка |
| Сборка без Xcode | `iosXtool/CoffeePeek` (xtool), см. `iosXtool/CoffeePeek/README.md` |

Общее с Android на iOS: все экраны, ViewModel, репозитории, строки, тема. Своё: нативные навигация и вкладки, карта, вход через Apple и Google,
push, камера, нативные контролы дизайн-системы.

Подробности: `docs/IOS_SETUP.md`, `docs/IOS_NATIVE_EXPERIENCE.md`, `docs/IOS_NATIVE_NAVIGATION.md`, `docs/IOS_MAP_PARITY.md`.

## 10. Ресурсы и локализация

- У каждого feature/core-модуля свои `composeResources` (`values` — русский, `values-be` — белорусский). Единого модуля со всеми строками нет.
- Доступ в коде: `Res.string.<ключ>` модуля; язык приложения выбирается пользователем и применяется через `coffeePeekStringResource` и
  `LocalCoffeePeekLanguageTag` (`core/design-system`).
- Строки приложения (заголовки вкладок, диалог ошибки, относительное время) — `composeApp/.../composeResources/values/app_ui_strings.xml` и `values-be`.
- Правило: новый ключ добавляется в оба языка; белорусский перевод пока делается без носителя языка.
- Картинки и шрифты: `composeApp/.../composeResources/drawable|font`, а также в `core/design-system` и фичах.

## 11. Тесты

| Где | Что проверяет | Как запускать |
|---|---|---|
| `feature/*/domain/src/commonTest` | правила и use case | `./gradlew :feature:<имя>:domain:testDebugUnitTest` |
| `feature/*/data/src/commonTest` | HTTP-контракты на `MockEngine`, мапперы | `:feature:<имя>:data:testDebugUnitTest` |
| `feature/*/impl/src/commonTest` | ViewModel, UI-модели, части экранов | `:feature:<имя>:impl:testDebugUnitTest` |
| `core/*/src/commonTest` | инфраструктура | `:core:<имя>:testDebugUnitTest` |
| `composeApp/src/commonTest`, `androidUnitTest` | соединение: адаптеры, DI-графы, Sandbox | `:composeApp:testDirectDebugUnitTest`, `:composeApp:testPlayDebugUnitTest` |
| `composeApp/src/iosTest` | iOS-граф DI и платформенные части | цель `iosSimulatorArm64` (в CI на macOS) |
| `androidInstrumentedTest` | на устройстве | запускается вручную или в CI |

Всегда добавляй `--max-workers=6` и не запускай несколько Gradle одновременно.

## 12. Сборка и CI

- `build-logic/` — конвенционные плагины (`com.coffeepeek.kmp.shared-library`, `com.coffeepeek.kmp.android-compose-library`, `com.coffeepeek.modules`)
  и `Modules.kt` со списком модулей. **Модуль, не внесённый в `Modules.kt`, в сборку не попадает.**
- `buildSrc/` — общие зависимости и мелкие задачи.
- `gradle/libs.versions.toml` — версии (Kotlin, Compose Multiplatform, Koin, Ktor, Room, MapLibre, ONNX Runtime, PostHog и др.).
- `.github/workflows/`: `android-ci.yml` (проверки Android), `android-apk.yml` (релиз APK), `android-firebase-develop.yml` (раздача develop),
  `ios-verify.yml` (проверка iOS-сборки), `ios-testflight.yml` (TestFlight).
- `tools/` — скрипты iOS и публикации релизов с тестами на Python.

## 13. Как добавить

**Новую фичу:** создать `feature/<имя>/{api,domain,data,impl}`, добавить пути в `build-logic/.../Modules.kt`, подключить модули в `composeApp/build.gradle.kts`,
создать `create…Entry` в `impl`, соединить в `composeApp/di/<имя>`.

**Платформенную возможность:** описать интерфейс (`api` фичи или `expect` в `composeApp`), реализовать в `androidMain` и `iosMain`; для Swift — зарегистрировать
реализацию в Kotlin-реестре из `CoffeePeekApp.swift`.

**Экран фичи:** UI и ViewModel — в `feature/<имя>/impl`; маршрут и открытие — в `composeApp` (`Navigator`, `di/<имя>`). Нативных iOS-экранов сейчас нет.

**Строку:** ключ в `values` и `values-be` модуля, который её владеет.

## 14. Что намеренно остаётся в composeApp

- старт приложения и точки входа (`MainActivity`, `CoffeePeekApplication`, `MainViewController`, Swift-оболочка);
- корневой DI, корневая навигация, плавающая панель вкладок, глобальное действие «создать чекин»;
- рендереры маршрутов `ui/screen/*Renderer` и host-ViewModel для времени жизни презентеров;
- адаптеры между фичами и платформенные мосты (авторизация, push, геолокация, карта, фото, нативные контролы iOS);
- Sandbox (только Android).

## 15. Известные ограничения и устаревшие места

- `ARCHITECTURE.md`, `feature/README.md`, `core/README.md` и часть плановых документов `feature/*` содержат исторические снимки миграции; ориентируйтесь на этот документ и на Gradle.
- Журнал выхода из старой структуры: `feature/LEGACY_EXIT_PLAN.md`; решения владельца об осознанных отличиях платформ: `feature/LEGACY_PARITY_AUDIT.md`.
- Подсказка выбора коллекции после «сердечка» работает только на Android; на iOS избранное сохраняется без неё.
- Лидерборд временно скрыт в ленте (код и модули остаются).
- Белорусские строки не проверены носителем языка.
