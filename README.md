# CoffeePeek

Приложение для поиска кофеен и обжарщиков, кофейных чекинов, подборок и домашней полки. Android использует Compose Multiplatform, iOS — нативные SwiftUI/UIKit-экраны поверх общих Kotlin-контрактов и сессий.

Документация сверена с исходниками `develop` 27 ноября 2026 года. Состояние опубликованной сборки определяется её коммитом и результатом доставки, а не этой датой.

## Начало работы

1. Установите Android SDK 37 и JDK для Gradle: Android CI использует JetBrains Runtime 21, релизные и iOS-задачи — JDK 17. JVM target проекта — 17; версии инструментов закреплены в Gradle wrapper и каталоге зависимостей.
2. Скопируйте [local.properties.example](local.properties.example) в игнорируемый `local.properties` и задайте путь SDK и API URL.
3. Соберите локальное окружение с поддельными данными:

```shell
./gradlew :composeApp:assembleDirectSandbox --max-workers=6
```

В PowerShell используйте `./gradlew.bat`. Сборка не устанавливает приложение. Для запуска выберите вариант и согласованное устройство по [инструкции Android](composeApp/ANDROID_RUN_MODES.md). `directDebug` обращается к настроенному backend; `directSandbox` работает локально без INTERNET.

Для iOS нужны macOS, Xcode 27 и runtime симулятора. Откройте `iosApp/iosApp.xcodeproj`, схему `iosApp`; настройка конфигурации, SDK и подписи — в [IOS_SETUP.md](docs/IOS_SETUP.md).

## Устройство проекта

| Каталог | Ответственность |
| --- | --- |
| `composeApp/` | запуск, корневой DI, маршруты и платформенные адаптеры |
| `feature/<name>/` | обычно `api/domain/data/impl`; Favorites сохраняет только server-backed `domain/data` |
| `core/` | общая инфраструктура и дизайн-система |
| `iosApp/` | Xcode-проект, lifecycle, конфигурация и интеграция iOS |
| `iosXtool/CoffeePeek/Sources/CoffeePeek/` | Swift-экраны, используемые также Xcode-проектом |
| `build-logic/`, `buildSrc/`, `tools/` | Gradle-конвенции, проверки и доставка |

Подробности: [структура](docs/PROJECT_STRUCTURE.md), [архитектура](ARCHITECTURE.md), [правила работы](AGENTS.md), [участие в проекте](CONTRIBUTING.md). Каталог `modules/` удалён; текущие границы и остаточные задачи описаны в [плане выхода из legacy](feature/LEGACY_EXIT_PLAN.md).

## Конфигурация и доставка

- Android: API и ключи из окружения сборки / `local.properties`; версии и варианты — в `composeApp/build.gradle.kts`.
- iOS: общие параметры из `iosApp/Configuration/Shared.xcconfig`, локальные — из игнорируемого `Local.xcconfig`.
- Android Release публикуется из `main`; `develop` доставляет отдельный Debug в Firebase. TestFlight настраивается отдельным workflow.
- Открытые ошибки и планы ведутся в [GitHub Issues](https://github.com/CoffeePeek/coffeepeek-cross-mobile/issues).

Полный [указатель документации](docs/README.md) содержит руководства фич, проверки, аналитику и релизы. Ключи, персональные данные, локальные логи и снимки экранов в репозиторий не добавляются.
