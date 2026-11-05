# Core

Общая инфраструктура без бизнес-логики. Структура всего проекта и разделение на общий и платформенный код описаны в
[docs/PROJECT_STRUCTURE.md](../docs/PROJECT_STRUCTURE.md).

| Модуль | Назначение | Платформенные части |
|---|---|---|
| `network` | клиенты Ktor (`ApiClients`), авторизация по сессии, обновление токена, кэш публичных ответов, диагностика | движок OkHttp (Android) / Darwin (iOS) |
| `session` | хранилище сессии (`SessionStore`, `SessionReader`) | токены: `EncryptedSharedPreferences` / Keychain |
| `database` | Room (KMP): общая настройка, построитель БД | построитель на каждой платформе |
| `media` | загрузка и кэш изображений, просмотрщик, предзагрузка | декодеры и загрузчики Android и iOS |
| `design-system` | токены, тема, шрифты, иконки, компоненты, «стекло», нативные контролы | Compose-реализации (Android) и UIKit-контролы (iOS) |
| `presentation` | базовый MVI `ViewModel` | — |
| `datetime` | разбор и форматирование дат с сервера | `ServerJavaInstant` (Android) |

Правила:
- Модуль core появляется только под реальных общих потребителей; навигация, DI и «utils» отдельными модулями не заводятся.
- Core не зависит от `feature/*` и от `composeApp`.
- Корневая навигация и сборка Koin остаются в `composeApp`.
- Строки модуля лежат в его `composeResources` (`values` — русский, `values-be` — белорусский).
