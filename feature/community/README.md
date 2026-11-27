# Сообщество и чекины в ленте

Сверено с `develop` `8cdc15f9` — 2026-11-27.

## Владение и активные entry points

`api` предоставляет feed/post entries, публичные посты автора, saved posts,
presentation contracts и injected ports. `domain` владеет моделями и правилами,
`data` — HTTP/DTO, `impl` — общими `CommunityViewModel`/`PostViewModel`,
Android Compose и presentation sessions нативной ленты/поста.
App composition предоставляет сессию, media, навигацию, share, blocking и аналитику.
Соседние features подключаются через API; второго feed store или analytics service нет.

На Android активны feature Compose feed, standalone post, edit/report sheets.
На iOS лента, карточки, post, редактор и report используют SwiftUI/UIKit.
`NativeCommunityFeed` и `NativeCommunityPost` владеют ViewModel stores;
`IosCommunitySheet` передаёт локализованные presentation values и user intents.
Compose screen/editor/report renderer больше не является iOS fallback.
Notification bell — app-supplied slot из [Notifications](../notifications/README.md).

## Сохраняемые правила

- Гость видит один читаемый чекин и teaser следующего при наличии. Sign-in CTA
  открывает существующий auth flow; signed-in pagination остаётся общей.
- Публичность, статус публикации, account generation и author blocking определяют
  доступность поста и действий. Blur не является серверной авторизацией.
- Like/unlike оптимистично меняет сердце и счётчик до ответа. В feed pending
  operation принадлежит конкретному посту: повторный тап блокируется, другие
  посты доступны. Успех сверяет server count, ошибка возвращает прежние значения.
  Поздний ответ прежнего аккаунта игнорируется; cancellation пробрасывается.
- Редактор сохраняет три rating axes, лимиты, текущий drink name и допустимый
  выбор при catalog failure. Каталог использует retry, а shared model — submit.
- Edit/report sheets принадлежат исходному feed/post и имеют уникальный key.
  Принятый dismiss отзывает старый transport; временное покрытие photo viewer
  сохраняет draft. Busy/dirty guards не обходятся старым callback после открытия.
- Account replacement закрывает owner и очищает private state, даже под другим
  контроллером. Disposal закрывает store и collectors.

SwiftUI управляет sheet/keyboard safe areas и medium/large detents. Messages
используют общий native message view: четыре секунды в обычном режиме,
явное закрытие при VoiceOver. Они не позиционируются Compose SnackbarHost.
Нативный feed header реагирует на scroll, foreground/visibility контролируют
date ticker, refresh и exposures. Prefetch ограничен двумя запросами и шестью
изображениями, останавливается под photo cover и не продлевает display grant
и не скачивает защищённый original.

## Публикация и интеграция

Создание home-чекина принадлежит [Home check-in](../home-check-in/README.md),
shop-форма — [Shop](../shop/README.md); Community владеет их отображением,
редактированием и модерационным состоянием в ленте. Возврат из внешнего home editor
вызывает один refresh. Корневые маршруты и screen/exposure analytics остаются
в composition, like/report/edit events — у существующих моделей и consent guards.
Тестовые действия не отправляют production analytics.

## Источники и проверка

- [Общие модели, native transports и unit contracts](impl/src/).
- [Публичные contracts](api/src/), [HTTP](data/src/).
- [SwiftUI renderers](../../iosXtool/CoffeePeek/Sources/CoffeePeek/) и
  [native root composition](../../composeApp/src/iosMain/).

Затронутые commonTest suites проверяют optimistic likes, independent pending
posts, failures/cancellation, account/author guards, draft revocation, rating/drink
mapping и JSON transport. Native tests и Swift build задаёт
[ios-verify.yml](../../.github/workflows/ios-verify.yml).
После логических изменений нужны relevant unit/contracts и Android assembly/DEX.

Ручные gates: keyboard/growing text, Back/cancel/swipe при отправке, catalog retry,
moderation states, protected photos, темы/ru/be, крупный текст и VoiceOver/TalkBack.
Live публикация/likes/report требуют согласованного тестового аккаунта.
Обновление документации не выполняло эти gates заново. Завершённые rollout/fix
журналы сохранены в Git history, а не используются как доказательство текущей сборки.
