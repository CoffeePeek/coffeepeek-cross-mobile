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

Для Google Sign-In нужны два OAuth client ID из одного Google Cloud проекта:

1. iOS client ID для bundle id `com.coffeepeek.ios`;
2. Web client ID, который принимает backend (тот же audience, что используется Android-приложением);
3. `GOOGLE_REVERSED_CLIENT_ID` — reversed client ID от iOS-клиента.

Секреты и `Local.xcconfig` коммитить нельзя.

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

В Xcode 27 отдельного `Simulator.app` может не быть: устройства открываются через `Window → Devices and Simulators` / Device Hub. Для сборки это не имеет значения.

## Запуск на реальном iPhone

1. Подключите iPhone и подтвердите доверие к Mac.
2. В target `iosApp` откройте `Signing & Capabilities`.
3. Включите `Automatically manage signing` и выберите свою `Team`.
4. Если bundle id занят, задайте уникальный `PRODUCT_BUNDLE_IDENTIFIER` в локальном xcconfig.
5. Выберите iPhone в списке устройств и нажмите `Run`.

Для бесплатного Apple ID профиль разработки имеет ограничения по сроку действия. Для TestFlight/App Store нужен Apple Developer Program.

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
