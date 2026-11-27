# Уведомления

Сверено с `develop` `8cdc15f9` — 2026-11-27.

## Владение и представление

`api` предоставляет `NotificationFeature`, immutable inbox state, destination,
account/city и native device ports; `domain` — модели/repository contracts;
`data` — Ktor transport/внутренние DTO; `impl` — presenter и platform UI sessions.
Composition адаптирует существующие session/current city, navigation и native
FCM/APNs providers. Firebase SDK не является зависимостью feature domain.

Feature владеет history/unread, account push switch, device registration,
post-check-in permission coordination и foreground reporting. Android bell
открывает Compose bottom sheet. На iOS bell/inbox — SwiftUI/native sheet с
NavigationStack, large detent и тем же Kotlin owner; старый transitional Compose
inbox больше не описывает активный путь. Settings использует ту же feature.

## History, permission и destinations

- History доступна при выключенном push и denied OS authorization. Login и
  foreground не перезаписывают сохранённое push preference.
- Автоматическая permission opportunity уважает предыдущий denial; explicit
  enable запрашивает разрешение либо предлагает OS settings по существующей policy.
- Строка отмечается прочитанной после двух непрерывных секунд достаточной
  видимости в активном приложении. iOS требует не менее половины строки.
  Viewport exit/background сбрасывают таймер; receipts отправляются последовательно.
- Account/session changes очищают inbox и отсекают поздние read/navigation replies.
  Несколько foreground receipts не теряются за уже выполняющимся refresh.
- Поддерживаются ShopApproved/Rejected, RoasterApproved/Rejected,
  CheckInModeration, CorrectionModeration, NewShop, Inactivity и General;
  неизвестный future type отображается как General.
- Actionable rows остаются такими после read. Feature читает owned destination
  через `GET /api/v1/me/notifications/{id}/destination`, различая public shop slug
  и submission/change ID. Composition разрешает contributions и cross-feature routes.
  Push open ждёт root/session readiness; ошибка сохраняет inbox для retry.

## Аудитория выбранного города

`NotificationCity` пассивно читает текущий город, выбранный пользователем или
автоматикой приложения. Отсутствие первого emission означает loading;
`NotificationCitySelection(null)` — разрешённое пустое значение. Adapter не
запрашивает GPS/permission и не загружает второй каталог.

Для signed-in аккаунта `PUT /api/v1/me/notifications/city` отправляет обязательный
nullable `citySlug`, включая explicit JSON null. Успех — HTTP 204; 404/503 и
другие rejected ответы сохраняют failure для retry на foreground/новом выборе.
Sync не зависит от push enabled, permission или provider availability.

Collector принадлежит account generation; queued city selections conflated к
последней. Отдельный city-write mutex сериализует запросы между generations:
даже noncancellable старый write завершается раньше нового. Account повторно
проверяется внутри lock. Inbox/device/login не ждут city synchronization.
Server отвечает за recipients и queued-delivery revalidation; клиентский фильтр
не может ограничивать background delivery. Контракт companion:
[backend #381](https://github.com/CoffeePeek/coffeepeek-backend/pull/381).

## Источники и проверка

[Публичные contracts](api/src/), [HTTP](data/src/), [presenter и tests](impl/src/),
[app adapters](../../composeApp/src/),
[native screens/provider](../../iosXtool/CoffeePeek/Sources/CoffeePeek/).

Unit/contracts проверяют permission timing, read visibility/account guards,
navigation failure, registration, city loading vs empty, explicit null, retries,
latest-city order и noncancellable A→B→A. Android assembly/packaged DEX и
затронутые suites проверяются отдельно от macOS Swift/Native job в
[ios-verify.yml](../../.github/workflows/ios-verify.yml).

Ручные gates: paging/visible-read, background/swipe dismissal, ru/be, themes,
large text/screen reader, OS permission/settings и реальная FCM/APNs доставка
в foreground/background/terminated states. Для live пушей нужен согласованный
тестовый аккаунт; fake backend не доказывает server audience/deployment.
Документационное обновление эти gates заново не выполняло. Исторические CI,
release и failed-attempt records доступны в Git history; они не обозначают
текущую последнюю TestFlight-сборку.
