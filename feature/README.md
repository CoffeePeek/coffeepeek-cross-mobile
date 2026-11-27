# Фичи CoffeePeek

Сверено с текущим `develop` 27.11.2026. Граф регистрируется в `build-logic/utilities/src/main/kotlin/com/coffeepeek/buildlogic/Modules.kt`, а фактические зависимости находятся в build-файлах каждого модуля.

## Границы

Целевая схема бизнес-фичи — `api/domain/data/impl`; фактический граф проверяется в `Modules.kt`. Исключение: `favorites` сейчас содержит только `domain/data`, его отдельный экран и `api/impl` удалены; библиотека пользователя принадлежит Collections. `api` публикует только необходимые entry/route/сессионные контракты; `domain` — модели и правила без инфраструктуры; `data` скрывает HTTP/DTO/persistence; `impl` владеет presentation и UI. Application composition соединяет независимые порты. Соседняя фича доступна только через `api`.

Android рендерит Compose-реализации. Нативный iOS использует публичные Kotlin-сессии/контракты и SwiftUI/UIKit. Общий ViewModel или существующий Compose screen не доказывает, что iOS использует Compose для этого маршрута.

## Владельцы

| Фича | Контракты и поведение |
| --- | --- |
| Auth | [auth/README.md](auth/README.md) |
| Author blocking | [author-blocking/README.md](author-blocking/README.md) |
| Business ownership | [business-ownership/README.md](business-ownership/README.md) |
| Check-in history | [check-in-history/README.md](check-in-history/README.md) |
| Coffee | [coffee/README.md](coffee/README.md) |
| Collections | [collections/README.md](collections/README.md) |
| Community | [community/README.md](community/README.md) |
| Content report | [content-report/README.md](content-report/README.md) |
| Contributions | [contributions/README.md](contributions/README.md) |
| Discovery | [discovery/README.md](discovery/README.md) |
| Favorites | [favorites/README.md](favorites/README.md) |
| Home check-in | [home-check-in/README.md](home-check-in/README.md) |
| Leaderboard | [leaderboard/README.md](leaderboard/README.md) |
| Notifications | [notifications/README.md](notifications/README.md) |
| Profile | [profile/README.md](profile/README.md) |
| Roaster | [roaster/README.md](roaster/README.md) |
| Settings | [settings/README.md](settings/README.md) |
| Shelf | [shelf/README.md](shelf/README.md) |
| Shop report | [shop-report/README.md](shop-report/README.md) |
| Shop | [shop/README.md](shop/README.md) |
| App update | `app-update` owns update policy/presentation; delivery is described in [Android release](../docs/GOOGLE_PLAY_CD.md) |

## Изменения и приёмка

Следуйте [AGENTS.md](../AGENTS.md), [ARCHITECTURE.md](../ARCHITECTURE.md) и текущему [плану выхода из legacy](LEGACY_EXIT_PLAN.md). Законченный checkpoint не становится местом для новых несвязанных фич. Сохраняйте серверное избранное, consent, аналитику, session guards и отмену coroutine.

README фичи описывает текущие контракты и оставшиеся проверки. Старые журналы по коммитам, версии установленных APK и первоначальные планы доступны в Git history, но не дублируются как действующие инструкции. Unit/contract results, compiled tests и ручные device gates фиксируйте отдельно в конкретном PR; выбранный новый entry point не доказывает завершённую миграцию.
