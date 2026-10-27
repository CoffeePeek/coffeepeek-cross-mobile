# Запуск CoffeePeek на iOS

В репозитории уже есть готовый Xcode-проект. Создавать новый iOS-проект вручную не нужно.

## Требования

- Mac с Apple Silicon;
- Xcode 27 с установленным iOS Simulator runtime;
- JDK 17;
- Android SDK 37 — Gradle конфигурирует общие KMP-модули и при iOS-сборке;
- доступ в интернет при первом открытии: Xcode скачает официальный Swift Package `GoogleSignIn-iOS`.

Минимальная версия приложения — iOS 15. Поддерживаются симуляторы Apple Silicon (`iosSimulatorArm64`) и реальные устройства (`iosArm64`).

Проверка окружения:

```bash
xcodebuild -version
xcrun --sdk iphonesimulator --show-sdk-version
xcrun simctl list devices available
/usr/libexec/java_home -V
```

Для Gradle выберите JDK 17:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

## Локальная конфигурация

Базовый dev API уже задан в `iosApp/Configuration/Shared.xcconfig`. Для персональных значений создайте игнорируемый Git файл:

```bash
cp iosApp/Configuration/Local.xcconfig.example iosApp/Configuration/Local.xcconfig
```

Раскомментируйте и заполните нужные строки:

```xcconfig
API_BASE_URL = https:/$()/your-api.example.com/
GOOGLE_IOS_CLIENT_ID = 000000000000-xxxxxxxx.apps.googleusercontent.com
GOOGLE_WEB_CLIENT_ID = 000000000000-yyyyyyyy.apps.googleusercontent.com
GOOGLE_REVERSED_CLIENT_ID = com.googleusercontent.apps.000000000000-xxxxxxxx
```

Без Google OAuth приложение полностью запускается, а Google-кнопка скрыта. Email/password вход продолжает работать.

Debug собирается без `iosApp/iosApp/GoogleService-Info.plist`: Firebase push в
этом случае недоступен. Если настоящий локальный файл добавлен, он копируется
в приложение и push можно проверять отдельно. В Debug Google-вход отключён
независимо от заполненных OAuth ID. Для Release Firebase-файл обязателен;
он не добавляется в Git.

В локальном iOS Debug отключены проверка обновлений App Store, баннер и пункт
ручной проверки в настройках. Локальный номер сборки может быть меньше
опубликованного и не означает, что разработчику нужно обновиться через магазин.
Номера версий не подменяются; в iOS Release серверная политика обновлений работает.

Для Google Sign-In нужны два OAuth client ID из одного Google Cloud проекта:

1. iOS client ID для bundle id `com.coffeepeek`;
2. Web client ID, который принимает backend (тот же audience, что используется Android-приложением);
3. `GOOGLE_REVERSED_CLIENT_ID` — reversed client ID от iOS-клиента.

Секреты и `Local.xcconfig` коммитить нельзя.

### Импорт Google из Firebase

В Firebase зарегистрируйте iOS-приложение с Bundle ID `com.coffeepeek`, включите
Authentication → Sign-in method → Google и заново скачайте `GoogleService-Info.plist`.
Поля `CLIENT_ID` и `REVERSED_CLIENT_ID` относятся к iOS. Web Client ID должен
совпадать с `OAuthGoogleOptions__ClientId` backend; Firebase Authentication SDK
в приложение не добавляется — Google ID token проверяет CoffeePeek backend.

```bash
python3 iosApp/configure-auth.py --google-plist /path/GoogleService-Info.plist \
  --web-client-id YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Скрипт сохраняет ID в игнорируемый `Local.xcconfig` и готовит `.build/Info.plist`
для xtool. `prepare-resources.sh` обновляет этот plist перед сборкой xtool.
В GitHub задайте `GOOGLE_IOS_CLIENT_ID`, `GOOGLE_REVERSED_CLIENT_ID` и
`GOOGLE_WEB_CLIENT_ID` для TestFlight. Release workflow требует все три значения,
чтобы сборка не попала в TestFlight со скрытой Google-кнопкой.

### Sign in with Apple

Кнопка входа через Apple доступна на экранах входа и регистрации. Backend выдаёт
одноразовый challenge на 5 минут (`POST /api/Tokens/apple/challenge`), iOS передаёт
его nonce и state в Apple, затем отправляет `challengeId` и `idToken` на
`POST /api/Tokens/apple/login`. Сервер проверяет подпись, issuer, audience, срок
действия и nonce; возвращает access/refresh tokens CoffeePeek в JSON.
Повторный вход определяется по Apple subject, поэтому отсутствие email в
последующем ответе Apple допустимо. Скрытый relay email поддерживается.

1. В Apple Developer включите Sign in with Apple у App ID `com.coffeepeek`.
2. В Profiles пересоздайте App Store distribution profile для этого App ID.
3. Замените GitHub secret `IOS_PROVISIONING_PROFILE_BASE64` содержимым нового
   `.mobileprovision`, закодированного в Base64. Проверка CI требует entitlement
   `com.apple.developer.applesignin = [Default]`.
4. При автоматической подписи Xcode обновляет development profile; выберите
   правильную Team. Capability требует платной Apple Developer Program.
5. Backend: `OAuthAppleOptions__ClientId=com.coffeepeek` (значение по умолчанию),
   доступ к Apple OIDC/JWKS по HTTPS и Redis для одноразовых challenge.

Если меняется Bundle ID, одновременно обновите Apple App ID, provisioning profile,
Google iOS client и audience Apple в backend. Настройте отправителя писем
CoffeePeek в Apple Private Email Relay, чтобы письма доходили на скрытые адреса.

`iOS build verification` на PR с изменениями iOS/KMP-кода в `main`/`develop`
и при ручном запуске компилирует Kotlin и Swift для симулятора без сертификата,
затем выполняет не-UI Kotlin/Native тесты совместимости Koin/ViewModel.
Для unsigned-проверки используется синтетический Firebase plist из `tools/ios`;
секреты не нужны, приложение не запускается. Проверка реального входа требует
устройства, актуального профиля и настроенных OAuth ID.

При ошибке нативного входа приложение сохраняет короткий диагностический код:

| Код | Этап сбоя |
| --- | --- |
| `ASI-CH` | Не удалось получить challenge от CoffeePeek. |
| `ASI-B` | Другой социальный вход ещё выполняется. |
| `ASI-W` | Не найдено активное окно для показа авторизации. |
| `ASI-C` | Apple вернул неподдерживаемый тип credential. |
| `ASI-S` | State ответа не совпал с запросом. Повторить вход с новым challenge. |
| `ASI-T` | В ответе отсутствует пригодный identity token. |
| `ASI-E<число>` | Ошибка SDK Apple; рядом отображается допустимый NSError domain. |
| `ASI-H<HTTP-статус>` | Сервер отклонил обмен токена, например `ASI-H401` или `ASI-H500`. |
| `ASI-A` | Сервер вернул HTTP 200, но сообщил об отказе во входе. |
| `ASI-R` | Успешный HTTP-ответ не соответствует JSON-контракту сессии. |
| `ASI-I` | В успешном ответе отсутствует data, access token или refresh token. |
| `ASI-P` | Сессия получена от сервера, но её сохранение завершилось ошибкой. |
| `ASI-X` | Общий сбой после получения токена Apple, если более точный код недоступен. |

Для диагностики нужен точный текст ошибки и этап, на котором остановился вход.
Код и домен не содержат токен, nonce, state, email или произвольный `userInfo`.
Успешный ответ `/challenge` проверяет только начало цепочки; для подтверждения
исправности входа нужен реальный Apple callback и обмен токена на сессию.

## Запуск в симуляторе

Откройте проект:

```bash
open iosApp/iosApp.xcodeproj
```

Дальше в Xcode:

1. дождитесь окончания `Resolving Package Graph`;
2. в верхней панели выберите схему `iosApp`;
3. выберите iPhone Simulator;
4. нажмите `Run` или `⌘R`.

Xcode сам вызывает Gradle task `:composeApp:embedAndSignAppleFrameworkForXcode`, собирает Kotlin framework и затем SwiftUI-приложение. Первый запуск может занять несколько минут; повторные сборки используют кэш.

Лимит Gradle workers задан Xcode-настройкой `GRADLE_MAX_WORKERS = 6` в
`Configuration/Shared.xcconfig`. Для локального изменения укажите положительное
число в `Local.xcconfig`; TestFlight workflow передаёт `GRADLE_MAX_WORKERS=1`
как build setting. Не используйте shell-default вида `${GRADLE_MAX_WORKERS:-2}`
в аргументах Gradle внутри Build Phase: KMP-плагин IDE может преобразовать его в
пустой `--max-workers=`.

В Xcode 27 отдельного `Simulator.app` может не быть: устройства открываются через `Window → Devices and Simulators` / Device Hub. Для сборки это не имеет значения.

## Запуск на реальном iPhone

1. Подключите iPhone и подтвердите доверие к Mac.
2. В target `iosApp` откройте `Signing & Capabilities`.
3. Включите `Automatically manage signing` и выберите свою `Team`.
4. Если bundle id занят, задайте уникальный `PRODUCT_BUNDLE_IDENTIFIER` в локальном xcconfig.
5. Выберите iPhone в списке устройств и нажмите `Run`.

Для бесплатного Apple ID профиль разработки имеет ограничения по сроку действия. Для TestFlight/App Store нужен Apple Developer Program.

## CI/CD: загрузка в TestFlight

Workflow [`.github/workflows/ios-testflight.yml`](../.github/workflows/ios-testflight.yml)
архивирует основное приложение `iosApp` на GitHub-hosted Mac с Xcode 27 и
загружает архив в App Store Connect. Он запускается после изменений iOS/KMP-кода
в `main` или вручную через **Actions → iOS TestFlight → Run workflow**
с выбором `main`. Публикация из других веток запрещена условием job.
Используется текущий `API_BASE_URL_MAIN`. Изменения только документации
не запускают автоматическую сборку: сохраняется фильтр путей iOS/KMP-кода.
Для pull request ключи подписи не выдаются. Номер сборки `CFBundleVersion` — целое
число `1000 + git rev-list --count HEAD`, как `versionCode` Android. Сдвиг `1000`
задан в `BUILD_NUMBER_OFFSET`, чтобы не уменьшить ранее опубликованный код. Повторный
запуск CI для того же коммита не повышает номер. Публичная версия задаётся вручную
в `iosApp/Configuration/Shared.xcconfig` (`MARKETING_VERSION = 1.1.1`). Android
использует `1.1.1.N`, где `N` равен `versionCode`; iOS хранит номер сборки отдельно. Серверные коды
обновления и обязательный минимум задаются вручную через админку.

Перед первым запуском:

1. В Apple Developer Program зарегистрируйте App ID `com.coffeepeek`, а в
   App Store Connect создайте запись iOS-приложения с тем же Bundle ID.
2. Создайте сертификат **Apple Distribution** с экспортируемым закрытым ключом
   (`.p12`) и provisioning profile типа **App Store Connect / App Store** для
   `com.coffeepeek`, связанный с этим сертификатом. Запишите пароль от `.p12`.
3. В App Store Connect создайте **Team API Key** с правом загрузки сборок
   (например, роль App Manager), сохраните файл `AuthKey_*.p8`, Key ID и Issuer
   ID. Ключ понадобится для неинтерактивной загрузки через `xcodebuild`.
4. В настройках репозитория GitHub создайте Environment `testflight`. Секреты
   из таблицы могут быть заданы на уровне репозитория или этого Environment.
   Если для Environment включены ограничения веток, разрешите только `main`.
   Также можно включить требование
   одобрения перед публикацией.

| Имя | Тип | Значение |
|---|---|---|
| `API_BASE_URL_MAIN` | Variable | HTTPS URL production API; используется также в Android release |
| `IOS_CERTIFICATE_BASE64` | Secret | Содержимое `.p12` в Base64, одной строкой |
| `IOS_CERTIFICATE_PASSWORD` | Secret | Пароль от `.p12` |
| `IOS_PROVISIONING_PROFILE_BASE64` | Secret | Содержимое App Store `.mobileprovision` в Base64, одной строкой |
| `APPSTORE_API_KEY_ID` | Secret | Key ID ключа App Store Connect API |
| `APPSTORE_ISSUER_ID` | Secret | Issuer ID ключа App Store Connect API |
| `APPSTORE_API_PRIVATE_KEY` | Secret | Исходный многострочный текст файла `AuthKey_*.p8`, включая строки `BEGIN/END PRIVATE KEY`; не Base64 и не символы `\n` |
| `GOOGLE_IOS_CLIENT_ID` | Secret | OAuth iOS client ID для `com.coffeepeek` |
| `GOOGLE_WEB_CLIENT_ID` | Secret | OAuth Web client ID для backend (можно использовать существующий секрет) |
| `GOOGLE_REVERSED_CLIENT_ID` | Secret | Reversed iOS client ID для URL scheme |
| `FIREBASE_GOOGLE_SERVICE_INFO_PLIST` | Secret | Исходный Firebase plist для `coffeepeek` / `com.coffeepeek`; обязателен для TestFlight |

На macOS Base64 для первых двух файлов можно получить командами
`base64 -i distribution.p12 | tr -d '\n'` и
`base64 -i profile.mobileprovision | tr -d '\n'`. Сертификат, профиль и `.p8`
не добавляйте в Git. Workflow проверяет команду, Bundle ID, срок действия
профиля и release-конфигурацию приложения до загрузки. Apple Team ID берётся
непосредственно из provisioning profile. При архивировании профиль подписи
задаётся только для `iosApp`; Swift Package targets его не используют. Для Google Sign-In
задайте все три OAuth ID вместе: release workflow требует их наличия.
При локальном запуске без OAuth кнопка Google скрыта.

FirebaseCore/FirebaseMessaging 12.19.2, ресурс `GoogleService-Info.plist` и `-ObjC`
закреплены в Xcode-проекте. CI больше не переписывает `project.pbxproj`.
Для локальной сборки поместите реальный Firebase plist в
`iosApp/iosApp/GoogleService-Info.plist`; файл исключён из Git. Синтетический
`tools/ios/GoogleService-Info.verification.plist` подходит только для unsigned
проверки компиляции. Release-скрипт отклоняет его даже при передаче через секрет.
Xcode обновляет SPM lockfile при разрешении добавленной зависимости;
PR-проверка сохраняет фактический `Package.resolved` вместе с логами и результатами тестов.

Фильтры обоих workflow включают `feature/`, `features/` и `tools/ios/`;
Android-only изменения и Markdown не запускают iOS-сборку.
Обязательность статуса `iOS build verification / build` для merge задаётся
отдельно в GitHub rulesets; изменение workflow само по себе её не включает.

Если импорт сертификата завершается ошибкой `Unknown format in import`, заново
экспортируйте из Keychain Access именно **Apple Distribution identity вместе с
закрытым ключом** в файл `.p12`. Файл `.cer` или сертификат без закрытого ключа
не подходит. При обновлении `IOS_CERTIFICATE_BASE64` кодируйте байты нового
`.p12`, а в `IOS_CERTIFICATE_PASSWORD` задайте пароль, указанный при экспорте.
На Windows PowerShell Base64 можно получить так:
`[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\distribution.p12).Path))`.
Не публикуйте полученную строку и пароль в чате или репозитории.

Для `APPSTORE_API_PRIVATE_KEY` откройте скачанный **App Store Connect API**
файл `AuthKey_*.p8` и сохраните в GitHub Secret его содержимое без изменений:
с настоящими переносами строк и строками `-----BEGIN PRIVATE KEY-----` и
`-----END PRIVATE KEY-----`. Если файл утрачен, Apple не позволяет скачать этот
же ключ повторно; создайте новый API key и обновите также `APPSTORE_API_KEY_ID`.
Используйте **Team Key** из App Store Connect → Users and Access → Integrations
→ App Store Connect API → Team Keys. `APPSTORE_ISSUER_ID` берите с той же страницы,
а `APPSTORE_API_KEY_ID` и `.p8` — от одного активного ключа. Для загрузки сборок
ключу нужна роль App Manager, Developer или Admin.
Workflow проверяет доступ к `com.coffeepeek` через App Store Connect API до сборки.

После успешного запуска дождитесь обработки сборки в App Store Connect →
CoffeePeek → TestFlight. Чтобы новые сборки автоматически появлялись у
внутренних тестировщиков, создайте внутреннюю группу и включите
**Enable automatic distribution**. Для внешних тестировщиков Apple может
потребовать информацию о тестировании, проверку сборки и ответы по экспортному
контролю. Загрузка в TestFlight сама по себе не публикует приложение в App Store.

## Universal Links: открытие ссылок CoffeePeek

Приложение объявляет Associated Domains для `applinks:coffeepeek.by` и обрабатывает
главную страницу, `/coffee-shops/{slug}`, `/shops/{id}`, `/roasters/{slug}`,
`/coffees/{slug}`, `/users/{slug}` и `/check-ins/{id}`. Экран отдельного поста
использует общий `CommunityPostEntry`, подключённый в iOS composition.
Ссылки сброса пароля остаются на сайте.

Для TestFlight и App Store нужны настройки на обеих сторонах:

1. В Apple Developer включите **Associated Domains** для App ID `com.coffeepeek`.
   Пересоздайте App Store provisioning profile и обновите GitHub Secret
   `IOS_PROVISIONING_PROFILE_BASE64`. Workflow проверяет наличие capability до сборки.
2. Разместите JSON ниже по адресу
   `https://coffeepeek.by/.well-known/apple-app-site-association` с HTTP 200,
   `Content-Type: application/json`, без перенаправлений и HTML-подстановки SPA.
   App ID текущей подписанной сборки — `JJ7867S6PS.com.coffeepeek`.
   При смене Apple-команды сверяйте его с `Entitlements.application-identifier`
   из профиля подписи. Файл сайта хранится в репозитории `coffeepeek-cross-web`:
   `coffee-peek/public/.well-known/apple-app-site-association`; Vercel отдаёт
   его напрямую также по `/apple-app-site-association`.

```json
{
  "applinks": {
    "details": [{
      "appIDs": ["JJ7867S6PS.com.coffeepeek"],
      "components": [
        {"/": "/"},
        {"/": "/coffee-shops/*"},
        {"/": "/shops/*"},
        {"/": "/roasters/*"},
        {"/": "/users/*"},
        {"/": "/coffees/*"},
        {"/": "/check-ins/*"}
      ]
    }]
  }
}
```

После публикации файла и установки новой подписанной сборки проверьте на iPhone
нажатие ссылок в Notes или сообщении: должны открываться соответствующие карточки.
Обновление кэша Apple может занимать время; ввод адреса в строку Safari не заменяет
эту проверку. Требования Apple:
[Supporting Associated Domains](https://developer.apple.com/documentation/xcode/supporting-associated-domains).

Проверка 2026-10-27: `codex/migration-ios-all-public-links` от `8ba59ab1`,
сайт `16d2916` ([PR #32](https://github.com/CoffeePeek/coffeepeek-cross-web/pull/32)).
План: проверить маршруты всех публичных карточек, iOS-приём ссылки на пост,
сохранение веб-восстановления пароля, сборку приложения и HTTP-контракт AASA.
Выполнены 21 unit/contract-проверка приложения (9 маршрутов, 5 native tab routes,
7 community adapters) и 9 проверок поста/шеринга; ошибок и повторов нет.
`assembleDirectDebug` и `verifyDirectDebugRuntimeClasses` прошли; APK не
устанавливался. Последний manifest пересобран после добавления новых путей.
Kotlin для iOS Arm64 и iOS Simulator tests скомпилирован на Windows;
это не выполнение native-тестов или Xcode-сборка. Их запуск включён в `ios-verify`.
Сайт: 49 server/hosting-тестов, TypeScript и production build прошли;
оба production-адреса AASA после публикации отвечают HTTP 200, JSON, без redirect.
Исходный HTTP 404 исправлен на сайте, но CDN Apple ещё возвращал закэшированный
`SWCERR00101`/404 при проверке. iPhone, cold/warm launch и переходы из Notes
не проверялись; нужны обновлённая подписанная сборка и обновление кэша Apple.

## Проверка из Terminal

Список доступных destination ID:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showdestinations
```

Сборка для симулятора без подписи:

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,id=<SIMULATOR_UDID>' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Если есть несколько симуляторов с одинаковым именем, всегда используйте `id=<UDID>`, а не `name=...`.

Отдельная быстрая проверка Kotlin-кода:

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

## Что реализовано на iOS

- общий Compose UI, навигация, ViewModel и Koin;
- Ktor Darwin и тот же REST API;
- Room/SQLite;
- Keychain для access/refresh token;
- MapLibre: кофейни, кластеры, зоны и их радиусы; провайдер регистрируется при запуске iOS-приложения;
- CoreLocation и reverse geocoding;
- системные камера и многовыбор фото через PhotosUI;
- Google Sign-In через официальный Swift Package;
- общий SignalR JSON transport для принудительного завершения сессии;
- системные буфер обмена, браузер, тема и форматирование дат.

## Частые проблемы

### `linkReleaseFrameworkIosArm64`: `Java heap space`

Это нехватка heap у отдельного JVM-процесса Kotlin/Native при линковке release framework. В TestFlight workflow для него задано `kotlin.native.jvmArgs=-Xmx5g`, Gradle запускает одну задачу за раз, а release binary caches уменьшают объём оптимизации при линковке. Кэширование release бинарника экспериментальное в Kotlin 2.4.20 и может повлиять на производительность приложения; проверяйте сборку в TestFlight перед выпуском. Локальные значения в `gradle.properties` при этом не меняются. Повторно запустите workflow из нового коммита в `main`; перезапуск старого run использует прежний workflow.

### `exportArchive`: `No Accounts with App Store Connect Access`

Если Xcode сообщает `Unable to authenticate with App Store Connect`, проверьте
три GitHub Secret: `APPSTORE_API_KEY_ID`, `APPSTORE_ISSUER_ID` и
`APPSTORE_API_PRIVATE_KEY`. Корректный PEM-файл может не соответствовать Key ID
или Issuer ID, либо ключ мог быть отозван. Шаг `Verify App Store Connect access`
проверяет эту связку до долгой сборки: HTTP 401 означает проблему аутентификации,
HTTP 403 — недостаточный доступ, а успешный запрос без `com.coffeepeek` —
другую команду Apple или отсутствие приложения в App Store Connect.

### Xcode не видит JDK

Укажите JDK 17 в текущей shell-сессии или в настройках Gradle IDE:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### Swift Package не скачался

В Xcode выполните `File → Packages → Reset Package Caches`, затем `Resolve Package Versions`.

### Два симулятора с одинаковым именем

Удалите лишний в Device Hub либо выбирайте точный UDID в `xcodebuild`.

### Xcode 27 предупреждает про `libicu.icudtl_dat.o`

Compose может вывести предупреждение, что этот ICU data object собран для iOS Simulator 18.5, а приложение — для iOS 15. Сборка при этом завершается успешно, а итоговый бинарник сохраняет deployment target 15.0.

### Google-кнопка не показывается

Проверьте все три значения `GOOGLE_*` в `Local.xcconfig`, bundle id iOS OAuth-клиента и повторно соберите приложение.
