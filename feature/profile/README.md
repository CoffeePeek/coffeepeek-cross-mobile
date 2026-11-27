# Профиль

Сверено с `develop` `8cdc15f9` — 2026-11-27.

## Владение и active entries

`api` содержит own/public/edit/deletion entries, account/data/likes/author ports,
snapshot store и slots соседних features; `domain` — профильные модели/правила;
`data` — профильный HTTP; `impl` — общие ViewModel, Android Compose и native sessions.
Приложение собирает history, shelf, collections, media, auth, analytics и navigation.
Profile управляет расположением slots, но не импортирует чужие domain/data/impl.

Android использует migrated feature entries, включая owner без public slug и
own-public-profile path. На iOS own/public profile, full public check-in list,
edit profile и account deletion — SwiftUI/UIKit, с Kotlin-owned state/actions.
Shelf/collections используют свои native controllers; прежнее описание их как
непереведённых Compose views устарело. Photo viewer остаётся у media owner.

## Header, connections и actions

- Header показывает avatar, display name и ровно три business counters:
  check-ins, shops, roasters, включая ноль. Followers/following — отдельные
  connection buttons; Follow виден для другого пользователя.
- Имя владельца берётся из непустого текущего own `userName`, затем public
  nickname, затем canonical profile slug. Старый public nickname не перекрывает
  новое own имя; email/auth identifier не используется как display name.
- Following count читается отдельным лёгким запросом. При ошибке остаётся unknown,
  а не выдуманный zero. Обычная загрузка connections supersedes этот запрос;
  refresh/return/account/visibility guards отсекают старые ответы.
- Owner menu содержит edit, contribution actions, blocked authors и sign-out.
  Blocked authors не дублируются в Settings. Menu закрывается до навигации или
  destructive confirmation; Cancel confirmation не выполняет logout.
- Edit profile подтверждает изменённый trimmed username до записи. About/avatar
  могут сохраняться отдельно. Успешные partial steps запоминаются для retry,
  typed quota/cooldown ошибки не расходуют повторно уже сохранённый username.
  Ограничения окончательно проверяет сервер; remaining quota не придумывается.

## Calendar и sections

Own profile содержит Check-ins, Collections и Shelf. Header/calendar переходит
между их scroll containers с сохранением state/effects. History feature владеет
week/month, selected day, type filters, paging и списком; Profile лишь размещает
его slot. Дата в calendar и список должны использовать одно выбранное значение.
Pending like защищает row от повторной записи; reload учитывает pending work.

Collections показываются квадратным grid в две колонки прямо под tab strip,
без второго заголовка «Подборки». Один floating glass entry над navigation
открывает полный список. Shelf settings размещены аналогично; shelf content,
archive и visibility принадлежат [Shelf](../shelf/README.md), коллекции —
[Collections](../collections/README.md). Root profile display gear/switch удалены.

После успешного обычного collection editor save root передаёт одноразовый
`OwnProfileSectionRequest` для Collections. Shelf/history/collections intents
замещают друг друга; consumption происходит после выбора секции. Logout/account
replacement очищают private requests. Обычный тап Profile сохраняет секцию;
cancel/error/quick-create не являются successful editor completion.

Public profile соблюдает author visibility/block/follow guards. Приватные owner
collections, архив полки и owner-only submission actions не попадают посетителю.
Наличие клиентского preview не отменяет серверную проверку private media.

## Lifetime и проверка

Own/public snapshots и caches привязаны к identity/login lifetime. Token refresh
в той же login lifetime сохраняет native stack; новая сессия отзывает прежний
profile/history owner. Production iOS module list включает profile snapshot
dependencies; это проверяется non-UI DI контрактом с negative control.
Успешная compilation сама по себе не доказывает корректность DI на открытии tab.

Источники: [API](api/src/), [presentation/native tests](impl/src/), [data](data/src/),
[root profile adapters](../../composeApp/src/),
[native profile screens](../../iosXtool/CoffeePeek/Sources/CoffeePeek/).

Regression suites покрывают display name, count unknown/stale/return, username
confirmation/partial retry, account replacement, privacy, snapshot/DI и root
section requests. Native selectors находятся в
[ios-verify.yml](../../.github/workflows/ios-verify.yml), включая
`CollectionProfileReturnTest`. При изменениях нужны relevant unit/contracts,
Android assembly/packaged DEX и macOS native compilation/tests.

Ручные gates: retained scrolling/calendar/tab changes, connection paging,
Back/cancel/edit/delete dialogs, private/public media, both themes/ru/be,
large text и VoiceOver/TalkBack. Follow/logout/edit/delete или visibility writes
требуют безопасного тестового setup. Эта документационная сверка не выполняла
их заново; старые verification/failure journals доступны в Git history.

27.11.2026, `codex/migration-shop-checkin-steps` от `496980e71`. Для состояния
«Вы подписаны» использован вторичный стиль кнопки. `directDebug` и `directSandbox`
1.2.2.3453 собраны, 110 тестов профиля прошли. В офлайн Sandbox на Pixel 8, API 37,
сверены начальное и подписанное состояния, подписанное состояние также проверено
в тёмной теме. На Pixel 9 установлен отдельный `CoffeePeek Debug Local` рядом с
Debug 3456. Подтверждены контрольная сумма установленного APK и холодный запуск.
Профиль с рабочим аккаунтом на устройстве не проверялся. Для iOS прошли
компиляция Kotlin-модуля и синтаксическая проверка Swift.

27.11.2026, рабочие изменения на той же ветке. `directDebug` и `directSandbox`
1.2.2.3453 собраны, 110 JVM-тестов профиля прошли. В тёмной теме офлайн Sandbox
на эмуляторе Pixel 8 API 37 цвет текста «Вы подписаны» совпал с цветом счётчиков.
Подписка сработала сразу, «Отмена» в нижнем подтверждении сохранила подписку,
а «Отписаться» вернуло счётчик к нулю. Прошли компиляция Kotlin Arm64 и
проверка синтаксиса Swift. Проверка на iPhone и изменения в рабочем аккаунте
не выполнялись.

27.11.2026, Pixel 9 API 37. Рабочие правки от `496980e71` собраны как
`directDebug` 1.2.2.3453 с идентификатором `com.coffeepeek.debug.local`.
Проверка DEX прошла. После обновления поверх прежнего локального APK
контрольная сумма на устройстве совпала с новой сборкой, холодный запуск
и выход `MainActivity` на передний план подтверждены. Флоу отписки на
рабочем аккаунте не запускался.
