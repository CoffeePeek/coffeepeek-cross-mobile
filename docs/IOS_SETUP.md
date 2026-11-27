# Сборка и настройка iOS

Проверено по конфигурации репозитория 27 ноября 2026 года. Основное приложение —
`iosApp/iosApp.xcodeproj`, схема `iosApp`; создавать новый проект не нужно.
Активный интерфейс — SwiftUI/UIKit, общие данные и ViewModel — Kotlin/KMP.

## Окружение и локальные параметры

- Mac с Apple Silicon, Xcode 27 и установленным iOS Simulator runtime.
- JDK 17 и Android SDK 37 для конфигурации общего Gradle-проекта.
- Цели Kotlin: `iosArm64`, `iosSimulatorArm64`; минимум приложения **iOS 17**.
- SPM-зависимости и их lockfile задаются Xcode-проектом; первый resolve требует сети.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cp iosApp/Configuration/Local.xcconfig.example iosApp/Configuration/Local.xcconfig
open iosApp/iosApp.xcodeproj
```

`Shared.xcconfig` содержит общие значения, `Debug.xcconfig`/`Release.xcconfig` —
режим, игнорируемый `Local.xcconfig` — локальные overrides. URL в xcconfig записывают
как `https:/$()/example.com/`, чтобы `//` не превратилось в комментарий.
Не коммитьте локальную конфигурацию, Firebase plist, сертификаты и ключи.

Debug использует обычный API, а не fake backend. В Debug отключены Google Sign-In
и магазинная проверка обновлений; email/password остаётся доступен. Без настоящего
`iosApp/iosApp/GoogleService-Info.plist` Debug собирается без Firebase push.
Синтетический `tools/ios/GoogleService-Info.verification.plist` предназначен только
для unsigned CI и отклоняется release-проверкой. Analytics Debug требует явного
opt-in; см. [ANALYTICS](ANALYTICS.md).

## Сборка и запуск

Выберите схему `iosApp` и конкретный iPhone Simulator в Xcode, дождитесь resolve
SPM и запускайте через Run. Xcode вызывает
`:composeApp:embedAndSignAppleFrameworkForXcode`, затем компилирует Swift-приложение.
`GRADLE_MAX_WORKERS = 6` задан как build setting; host Gradle не запускают параллельно.

Unsigned-проверка без запуска:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build
```

Список конкретных устройств: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme
 iosApp -showdestinations` (одной строкой). При нескольких одинаковых именах
выбирайте UDID. Для реального iPhone нужны доверие, разблокированный выбранный
телефон и подходящая development-подпись/Team. Сверьте bundle/version/signature;
обновляйте без удаления приложения, очистки данных или скрытого downgrade.
Локальный Windows xcross — отдельный [режим и пакет](IOS_XCROSS.md).

## Социальный вход

Google требует согласованных `GOOGLE_IOS_CLIENT_ID`, `GOOGLE_WEB_CLIENT_ID` и
`GOOGLE_REVERSED_CLIENT_ID`. iOS OAuth client соответствует bundle ID, web audience
— настройке CoffeePeek backend; оба ID должны принадлежать одному Google Cloud
проекту. Firebase Authentication SDK не является писателем сессии: ID token
проверяет backend CoffeePeek.

```bash
python3 iosApp/configure-auth.py --google-plist /path/GoogleService-Info.plist \
  --web-client-id YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Скрипт заполняет игнорируемую локальную конфигурацию/xtool plist. Google в Debug
остаётся отключён даже при заполненных ID. Release workflow требует все три ID.

Apple Sign-In использует backend challenge → системный callback → token exchange.
Нужны capability App ID, entitlement `com.apple.developer.applesignin = [Default]`
и соответствующий provisioning profile. При смене bundle/team согласуйте App ID,
profile, Google client и backend Apple audience. Подпись xcross может иметь другой
набор capabilities; успешная установка не доказывает доступность Apple-входа.

Диагностика разделяет этапы: `ASI-CH` — challenge; `ASI-B/W/C/S/T/E…` — native
provider/окно/credential/state/token/SDK; `ASI-H…` — HTTP token exchange;
`ASI-A/R/I` — отказ/формат/неполная сессия; `ASI-P` — сохранение сессии.
Запишите точный код, время и версию/сборку; `ASI-X` — общий остаточный код.
Успешный challenge ещё не подтверждает callback, обмен и сохранение сессии.
Серверную диагностику проводят по текущему backend runbook, не по устаревшему
жёсткому VPS-пути. Перед передачей логов удаляйте credentials, email, nonce/state
и заголовки; полный env/контейнерные дампы для этого не нужны.

## Universal Links

`CoffeePeekApp.swift` передаёт web activity/URL в общий обработчик. Поддерживаемые
публичные пути включают `/`, `/coffee-shops/{slug}`, `/shops/{id}`,
`/roasters/{slug}`, `/coffees/{slug}`, `/users/{slug}`, `/check-ins/{id}`;
password-reset остаётся web-потоком. Конкретный список проверяйте в shared
app-link parser/tests при добавлении маршрута.

Associated Domains в entitlements/profile и серверный AASA должны соответствовать
подписанному application identifier. Файл
`https://coffeepeek.by/.well-known/apple-app-site-association` должен отдаваться
как JSON без SPA/redirect; его публикация принадлежит web-репозиторию. Не копируйте
старый Team ID без проверки текущей подписи. Установка подписанной сборки и
нажатие ссылки из Notes/сообщения в cold/warm состоянии — отдельная проверка;
ввод адреса в Safari и проверка HTTP не заменяют её.

## CI и TestFlight

[`.github/workflows/ios-verify.yml`](../.github/workflows/ios-verify.yml) выполняет
unsigned Kotlin/Swift Simulator build, tooling contracts и выбранные не-UI
Kotlin/Native тесты. Markdown и Android-only пути исключены из PR-триггера.
Новые тесты должны попасть в фильтры Native job и в фактические XML-отчёты;
зелёный compile не означает, что тесты исполнялись. Требуемые merge checks
настраиваются ruleset отдельно от workflow.

[`.github/workflows/ios-testflight.yml`](../.github/workflows/ios-testflight.yml)
запускается на подходящие изменения `main` или вручную. Upload job допускает
`main`, а для ручного dispatch также `codex/ios-native`; один `develop` merge
не является автоматическим TestFlight upload. Environment — `testflight`.
Markdown-only изменения не запускают автоматическую публикацию.

Публичная версия берётся из общего с Android `MARKETING_VERSION` (сейчас 2.0.0), номер сборки —
`BUILD_NUMBER_OFFSET + git rev-list --count HEAD` (сейчас offset 1100).
Повтор того же коммита не увеличивает номер. Источник — `Shared.xcconfig` и шаг
Read build number; не поддерживайте отдельный ручной номер в документации.

| Настройка CI | Назначение |
| --- | --- |
| `API_BASE_URL_MAIN` (variable) | Production HTTPS API |
| `IOS_CERTIFICATE_BASE64`, `IOS_CERTIFICATE_PASSWORD` | Distribution identity `.p12` с закрытым ключом и его пароль |
| `IOS_PROVISIONING_PROFILE_BASE64` | App Store profile нужных bundle/team/capabilities |
| `APPSTORE_API_KEY_ID`, `APPSTORE_ISSUER_ID`, `APPSTORE_API_PRIVATE_KEY` | Согласованный App Store Connect Team API key; PEM с настоящими переводами строк |
| `GOOGLE_IOS_CLIENT_ID`, `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_REVERSED_CLIENT_ID` | Release OAuth |
| `FIREBASE_GOOGLE_SERVICE_INFO_PLIST` | Настоящий Firebase plist приложения |
| `POSTHOG_PROJECT_TOKEN` | Настройка ingest по политике analytics |

Workflow проверяет конфигурацию, доступ App Store Connect и provisioning до
архивации; временные signing-материалы очищаются. Для release Native link он
задаёт отдельный heap/кэш и `GRADLE_MAX_WORKERS=1`, не меняя локальный worker limit.
Сертификат без закрытого ключа не заменяет `.p12`; private key нельзя выводить в лог.

Успешный archive, upload и Apple processing — разные этапы. Финальный шаг ожидает
точную version/build через `wait_testflight_processing.py`. Группа/доступ внешних
тестировщиков и App Store Review — отдельные процессы. Не считайте старый номер
из плана последней доступной сборкой и не dispatch release без текущей задачи.

## Проверки и ограничения

[Native root/session](IOS_NATIVE_ROOT_SESSION_CONTRACTS.md),
[навигация](IOS_NATIVE_NAVIGATION.md), [остаток выхода из legacy](IOS_NATIVE_EXIT_PLAN.md),
[карта](IOS_MAP_PARITY.md), [изображения](IOS_FEED_PHOTO_MEMORY.md).

KLIB-компиляция, unsigned build и fake contracts не доказывают реальный OAuth,
push, Universal Links, сканирование, память или UI-доступность. Для проверки
нужны конкретная ревизия/вариант, выбранное устройство и подходящая авторизация.
В этом обновлении документации сборки/установки/ручные проверки не выполнялись.
