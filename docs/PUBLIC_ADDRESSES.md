# Публичные адреса в мобильном клиенте

Сверено с feature data/contracts 27.11.2026. Это правила клиента, не полное описание всех endpoint backend и не подтверждение текущего развёртывания сервера. Конкретные wire-поля проверяются DTO/MockEngine-тестами владельца, OpenAPI и серверным контрактом выбранного релиза.

## Идентичность

Публичная сущность возвращает адрес: `slug`, `canonicalPath`, `revision`, `isAlias`. Клиент сохраняет выданный сервером slug; не строит его из имени и не преобразует в UUID. `canonicalPath` — относительный путь клиентской страницы, не API URL. Share/deep link используют проверенный адрес, а запросы деталей — slug.

В Kotlin/native API исторические имена `id`, `shopId`, `cityId` и `*Ids` могут содержать именно slug. Это не разрешение подставлять GUID в запрос публичной сущности. GUID чекина, фотографии, moderation submission и внутренний account ID сохраняют отдельный смысл.

Историческая связь с удалённой/скрытой сущностью может быть `null` или отсутствовать. UI сохраняет доступную запись и отключает недоступный переход; чужой private/current-account reader не является fallback. Alias/canonical revision приходит с ответом сервера; дополнительное публичное разрешение GUID→slug не добавляется.

## Чтение

- Discovery использует `POST /api/v1/coffee-shops/search`. Старые `GET /api/CoffeeShops` и `POST /api/v1/discovery/search` не используются. Пагинация `page/pageSize`, критерии в JSON `filters`.
- Обычный поиск не требует геопозиции. Nearby передаёт разрешённый `origin`, radius и distance sort; отозванная позиция не сохраняется как origin. Карточка поиска не является источником координат: при необходимости читаются детали.
- Карта читает `/api/Map` с bounds/zoom; публичные адреса маркеров/зон не заменяются локальными GUID.
- Детали кофейни/обжарщика и каталоги читаются соответствующими `feature/*/data` API. Не переносите удалённый legacy DTO из прежнего документа в новую фичу.
- Public profile получает server address и отдельные публичные favorites/activity/check-ins. Личное избранное хранится server-backed с ключом `(kind, slug)`; напиток не получает выдуманный публичный маршрут.

Точные поля и поведение: [Discovery](../feature/discovery/README.md), [Shop](../feature/shop/README.md), [Roaster](../feature/roaster/README.md), [Profile](../feature/profile/README.md), [Favorites](../feature/favorites/README.md).

## Запись и ошибки

DTO фичи задаёт текущие имена `shop`, `city`, `beans`, `equipments`, `roasters`, `brewMethods`, `tags`; domain/UI не импортируют transport-типы. Текущий пользователь определяется authorization, не полем публичного slug в запросе. Журнал старых Reviews endpoint не является действующим check-in API.

Для частичных moderation changes `null`/отсутствующее поле и пустой список имеют различный смысл; сохраняйте section/revision и wire serialization владельца. Retained photo IDs — служебные идентификаторы фото, не public address. Username/cooldown/canonical response обрабатываются Profile, а не локальной генерацией адреса.

Ошибки permission/auth/not-found/conflict/unavailable остаются ошибками; не подставляйте GUID и не выдавайте неизвестное состояние за пустой успешный ответ. Поздний ответ предыдущего аккаунта не обновляет текущий экран. Подробнее — [Contributions](../feature/contributions/README.md), [Home check-in](../feature/home-check-in/README.md) и [Shop](../feature/shop/README.md).

## Проверка

Сверяйте actual route → DTO mapper → feature API/domain и JSON contract tests. Важны alias/canonical, nullable исторические связи, сохранённый город после обновления каталога, guest/public vs personalized state и отсутствие лишнего GUID fallback. Unit/fake checks не подтверждают backfill и маршруты работающего Gateway; это отдельный согласованный integration gate.
