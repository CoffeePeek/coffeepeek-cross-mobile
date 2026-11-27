# Мобильная атрибуция v1

Дата: 27.11.2026. Ветка `codex/migration-attribution` начата от migration tip `2ed931f730b11f7cee340329f4bd47239eb6faf2` (#459). Пока шла работа, #459 и #460 уже вошли в main; проверена идентичность дерева и выполнен fast-forward на `e44ac20fc`, PR направляется в main. Общий контракт: [backend](https://github.com/CoffeePeek/coffeepeek-backend/blob/6bcc048f950542489a32d44bad072de40034d0c5/docs/ATTRIBUTION-CONTRACT.md).

Сохраняются текущие события/schema v1, `AnalyticsService` и единственный PostHog KMP 0.5.1. Native guest collection остаётся выключенным. SDK identity читается только после подтверждённого аккаунта, согласованной настройки, ready и foreground. Функциональные links/resolve/share продолжают работать без аналитики. Смена аккаунта/отзыв закрывают прежний observer; фон приостанавливает сбор. Данные SDK, JWT, public codes и receipts не становятся произвольными аналитическими properties.

## Владельцы

Новая функция принадлежит `feature/attribution`, а не root UI или общей `core/utils`.

- `api`: минимальные порты открытия, успешного показа назначения и функционального шеринга для соседних features; без DTO/Ktor/DAO.
- `domain`: входящие доказательства, состояние observer/установки и правила разрешённого наблюдения; зависимости только Kotlin/coroutines, без платформ/SDK/serialization.
- `data`: контрактные HTTP DTO, Ktor-адаптер, ограниченное локальное хранение и платформенный install referrer. Возвращает публичный API через узкую фабрику.
- `impl`: реальное состояние resolver/retry и локализованный диалог ошибки на текущих дизайн-компонентах. Только composition подключает entry; DTO/SDK ему не доступны.
- `composeApp`: существующий SDK lifecycle, адаптация его разрешённого контекста, root DI и перевод разрешённого canonical path в текущий Navigator. Бизнес-правила не переносятся в composition.

Отдельные Gradle boundaries нужны, чтобы существующие Shop/Coffee/Profile/Settings могли зависеть только от `api`, не получая Ktor, observer persistence и SDK. Impl содержит работающий resolver/retry, а не пустую оболочку: ошибка сети/404 не превращается в успешный показ главной. Core остаётся независимым от feature. Существующие публичные API не переименовываются.

## Порядок и проверки

1. Точные DTO/порты и проверка строгих union payload, Idempotency-Key, observer capability; mock HTTP, без production.
2. Разрешённый foreground runtime: повтор сигнала не создаёт второй Touch/installation, identity/consent invalidation отменяет устаревшую работу, отказ стирает личный контекст. Только credential отзыва допускается на семь дней. Installation ID случайный, создаётся после разрешения; firstRunObserved=false при позднем включении. Backup/Keychain не используются для восстановления ID.
3. Функциональные `/l/*` и `cp_link`, readiness после успешной загрузки контента; приватные коллекции сохраняют старый путь. Play referrer переносит только подтверждённый handoff; direct APK/неизвестный магазин не объявляются Google Play. iOS не получает произвольный receipt через App Store.
4. Подключение к уже существующему AnalyticsService и сохранение screen/exposure ownership.
5. Unit/HTTP/session регрессии; Gradle последовательно с `--max-workers=6`, function-body limit, affected module compilation, directSandbox assembly и packaged DEX checks. IDE checks недоступны: Studio MCP отсутствует в сессии.

Новые automated UI/instrumentation/screenshot tests не создаются. На телефоны ничего не устанавливается, реальные аккаунты/consent не изменяются. Реальные Play install referrer и подписанные iOS Universal Links требуют отдельной проверки выпущенной сборки на согласованных устройствах; mock/API/build не подтверждают эти механизмы. Cloud smoke и production включение остаются отдельным rollout gate.

## Реализованное поведение

`/l/{code}` и единственный `cp_link` разрешаются до навигации. Сбор ждёт успешно загруженные Shop/Coffee/Roaster/PublicProfile/публичный CheckIn либо Discovery home, foreground и действующее SDK-разрешение. Каталожные списки `/coffee-shops`, `/roasters`, `/coffees`, для которых текущий KMP Navigator не имеет отдельного назначения, открываются на сайте с публичным cp_link; это web attribution, а не выдуманный native view. В отдельном native Swift iOS приложении эти каталоги маршрутизируются нативно.

Создание owner share-link подключено к существующим действиям Shop/Coffee/Roaster/Profile/CheckIn/Settings; гостю и при сетевой ошибке остаётся каноническая ссылка. Окно системного шеринга не доказывает отправку, поэтому share_action_completed не добавляется. Backend сам отправляет link_opened/acquisition_touch_observed/attribution_attached/installation_observed через outbox; клиент их не дублирует. Строгие union requests не содержат произвольных source/ownerUserId/userId.

После истечения observer lease ещё действующий receipt можно привязать через новый permitted observer. Исходный observer не отзывается только из-за истечения lease. Повтор claim не повторяет уже принятый Touch; installation ID/ACK сохраняется при account switch и сбрасывается при отзыве. firstRunObserved всегда false для этой account-only поздней интеграции. Play installer определяется по com.android.vending; отсутствие подтверждения даёт unknown. Install Referrer 2.2 читает только cp_handoff после разрешения, raw referrer/utm не сохраняется; временная ошибка допускает повтор.

## Проверка и ограничения

27.11.2026: финальный gate после resolver UI и защиты refresh прошёл — 890 unit tests, 0 failures/errors: composeApp 785, core/network 76, attribution domain 22, data 7. Direct Debug компилируется; Direct Sandbox APK собран, изоляция и packaged DEX runtime-class checks успешны. Function-body gate: 0 новых нарушений. Первоначальная ошибка подключения androidMain.dependencies исправлена, не скрывалась baseline. Mac CI отдельно компилирует Kotlin/Native data с настоящими Foundation bindings; это не полный iOS UI release gate. Диалог имеет colocated light/dark previews; IDE-render и ручная проверка UI на устройстве не выполнялись.

Security review: Kotlin/Ktor/Android/Foundation; входные границы — Intent/Universal Link, resolver response, Play referrer, SDK identity и локальный файл. V4/CWE-862: principal берётся из текущей сессии, permission/generation проверяется до отправки и после ответа; в Claim нет целевого account ID. Узкий authorizeWhile guard проверяется после загрузки credentials и повторно после refresh, не давая старому payload уйти с новым principal; unit tests моделируют revoke/account switch внутри этих suspension points. Resolve/capabilities/revoke используют plain transport, наблюдения — существующий authenticated transport/refresh. V5/CWE-20: code/канонический путь/union/собственный домен share URL ограничены. V7/CWE-532: attribution HTTP исключён из api_request_completed, observer header и весь request body скрыты из debug curl; есть unit regressions. V8/CWE-359: ID создаётся после permission, хранилище исключено из backup, Keychain/hardware IDs/отдельной persistent event queue нет; отзыв очищает evidence и сохраняет лишь bounded control credential до 7 дней.

На production analyticsEnabled=false на момент проверки. Ни сборка, ни mock tests не подтверждают ingestion PostHog. Сначала web/AASA release, затем мобильные релизы и согласованный store/deep-link smoke; после этого staging cloud UUID smoke и включение server collection/delivery с проверенной конфигурацией. Store aggregates, business conversion adapters и историческое восстановление источника не входят в эту клиентскую интеграцию.

SDK lease включает revision действующего транспорта и consentRevision: старое разрешение не оживает после opt-out/opt-in или foreground restart, даже при прежнем session ID. Account generation проверяется немедленно, до асинхронной сверки AnalyticsService. При startup/pause очищается истёкший receipt вместе с account ID; остаётся только credential отзыва с отдельным семидневным пределом. Новых событий при этой очистке нет.

Ключ запроса связан с неизменным телом: при сетевом повторе Touch/Claim/installation сохраняются исходные реальные SDK IDs и receipt, а permission predicate проверяется по новому действующему lease. Новый входящий link не переписывает ещё не подтверждённый источник installation. Поздний observer, поставленный на revoke, не используется вновь. Каждый сценарий покрыт unit tests. iOS contract gate обнаружил регрессию web reset-password; восстановлен прежний отказ от обработки recovery URL, добавлен common unit test и атомарное consume перед навигацией, чтобы несколько root surfaces не применяли один callback повторно.
