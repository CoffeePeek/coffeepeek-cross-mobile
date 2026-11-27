# Текущая структура CoffeePeek

Сверено 27.11.2026 с `settings.gradle.kts`, каталогом `Modules.kt`, source sets и платформенными точками входа. Точные зависимости задают Gradle-файлы; этот обзор не заменяет их проверку перед изменением границ.

## Граф модулей

`settings.gradle.kts` подключает `build-logic` и settings plugin `com.coffeepeek.modules`. Список модулей находится в `build-logic/utilities/src/main/kotlin/com/coffeepeek/buildlogic/Modules.kt`. `buildSrc` предоставляет build-only помощники; runtime от него не зависит.

| Уровень | Состав | Правило |
| --- | --- | --- |
| Приложение | `composeApp` | старт, root DI, navigation, platform configuration/adapters |
| Фичи | обычно `feature/<name>/{api,domain,data,impl}` | бизнес-владение; текущие исключения проверяются в `Modules.kt` |
| Инфраструктура | `core/database`, `datetime`, `design-system`, `media`, `network`, `presentation`, `session` | без зависимости на фичи |
| Нативный iOS | `iosApp`, общие Swift-исходники в `iosXtool/CoffeePeek/Sources/CoffeePeek` | SwiftUI/UIKit presentation и platform lifecycle поверх Kotlin bridge |

`modules/` и верхнего `features/` в текущем графе нет. У `settings` есть собственный `data`-модуль. У `favorites` остаются только `domain/data`: отдельный экран и его `api/impl` удалены; активная библиотека — Collections. Не создавайте новый app-модуль или второй каталог модулей по схеме из старого плана.

## Платформы

- `commonMain`: domain/data, shared state/presentation и Compose UI там, где он используется. Наличие Compose-экрана не означает, что именно он выбран на iOS.
- `androidMain`: Android интеграции, entry points, MapLibre, разрешения и платформенные actual.
- `iosMain`: Kotlin/Native integration, фабрики сессий, реальные platform abstractions и мост к native presentation.
- `androidSandbox`: изолированный in-memory HTTP backend и synthetic media только для Android Sandbox.
- `iosApp`: Xcode-проект, entitlements, xcconfig, resources, lifecycle и SDK-провайдеры.

Android root использует `AppContent`/`Navigator` и feature renderers. Активный iOS root — `IosNativeRoot` плюс Swift-экраны и native navigation; `MainViewController`/Compose interop сам по себе не является текущей пользовательской оболочкой. Root stack, auth и lifetime см. в [iOS navigation](IOS_NATIVE_NAVIGATION.md) и [session contracts](IOS_NATIVE_ROOT_SESSION_CONTRACTS.md).

## Владение кодом

Внутри фичи `data → domain`, `impl → api/domain/core`. Application composition собирает data factories и impl entries. UI не импортирует DTO/DAO/repository implementations. Domain не зависит от Compose, Ktor, Room, Koin или платформы. Подробнее — [ARCHITECTURE.md](../ARCHITECTURE.md).

`composeApp` ещё содержит исторические preference/session bridges, analytics infrastructure и route hosts. Это текущие остатки для разбора, а не разрешение размещать там новый бизнес-UI. Проверяемый список границ и незавершённые gates — в [LEGACY_EXIT_PLAN.md](../feature/LEGACY_EXIT_PLAN.md).

## Документация и конфигурация

- [Feature index](../feature/README.md) — владельцы возможностей и публичные границы.
- [Core index](../core/README.md) — общая инфраструктура.
- [Composition](../composeApp/README.md) — DI, root и platform coordination.
- [Build infrastructure](../build-logic/README.md) — конвенции, dependency helpers и DEX guards.
- [Android run modes](../composeApp/ANDROID_RUN_MODES.md) — варианты, package IDs, безопасная установка.
- [iOS setup](IOS_SETUP.md) — Xcode/SwiftPM/configuration; [xcross](IOS_XCROSS.md) — альтернативная сборка.
- [Documentation index](README.md) — процессы, контракты, аналитика, доставка.

Android compile/min/target SDK задаются в Gradle/`Config.kt`. Android version name рассчитывается в `composeApp/build.gradle.kts`; iOS marketing version и общий build offset — в `iosApp/Configuration/Shared.xcconfig`. Не копируйте в планы номер последней установленной сборки как текущую конфигурацию проекта.
