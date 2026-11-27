# Core

Проверено по исходникам `develop` 27 ноября 2026 года. Core содержит общую
инфраструктуру и не зависит от `feature/` или `composeApp`.

| Модуль | Ответственность |
| --- | --- |
| `network` | Ktor-клиенты, JSON, bearer/refresh, публичный HTTP-кэш, диагностика |
| `session` | Сессия и атомарная замена credentials; Android secure preferences / iOS Keychain |
| `database` | Room/SQLite, настройки и миграции базы приложения |
| `media` | Загрузка, ограниченные кэши и разрешения на показ изображений |
| `design-system` | Тема, ресурсы и переиспользуемые визуальные компоненты |
| `presentation` | Типизированная база MVI ViewModel |
| `datetime` | Разбор и форматирование серверных дат |

Корневая DI-сборка, платформенная конфигурация, жизненный цикл приложения и
навигация принадлежат `composeApp`. Доменные правила и данные конкретной функции
принадлежат её `feature/<name>`. Общие инфраструктурные модули не обязаны иметь
отдельные `domain`/`data` подмодули.

Руководства: [сеть](network/README.md), [сессия](session/README.md), [база](database/README.md),
[медиа](media/README.md), [MVI](presentation/README.md),
[дизайн-система](design-system/README.md). Общая структура —
[PROJECT_STRUCTURE](../docs/PROJECT_STRUCTURE.md), остаток миграции —
[LEGACY_EXIT_PLAN](../feature/LEGACY_EXIT_PLAN.md).
