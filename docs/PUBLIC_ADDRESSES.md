# Контракт публичных адресов для клиентов

Текущий API меняется напрямую, без `/v2`. Кофейни, обжарщики, города, кофейные зоны, пользователи и справочники используют slug. Адрес приходит вместе с данными; дополнительный запрос `public-address` не нужен. Этот контракт относится к новой сборке, а не к уже работающей версии сервера.

## Идентификаторы и адреса

```ts
type PublicAddress = {
  slug: string;
  canonicalPath: string;
  revision: number;
  isAlias: boolean;
};

type ApiResponse<T> = {
  isSuccess: boolean;
  message: string | null;
  data: T;
};

type Page<T> = {
  items: T[];
  totalItems: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
};
```

`canonicalPath` — относительный путь страницы клиента, например `/coffee-shops/26-october-16`, а не API URL. Для навигации используйте этот путь, для запросов — `slug`. Не генерируйте slug из имени самостоятельно. Полученные значения справочников тоже сохраняйте как строки, не преобразуйте в UUID.

Публичные DTO не содержат GUID кофейни, города, обжарщика, зоны, пользователя или справочника. GUID отзывов, check-in, фотографий и заявок модерации остаются служебными идентификаторами. В ответе создания заявки `shopId`/`roasterId` обозначает заявку, а не опубликованную сущность. Административный API, кабинет владельца, внутренние сообщения и аутентификация сохраняют внутренние ID.

Slug уникален внутри вида сущности. Переименование отображаемого имени кофейни или справочника не меняет его. При явной смене адреса старый slug остаётся alias; удалённые адреса резервируются навсегда. У пользователя адрес следует за username; смена username разрешена раз в семь дней.

## Чтение

| GET | Данные ответа |
| --- | --- |
| `/api/CoffeeShops/{slug}` | `ApiResponse<ShopDetails>`; данные лежат непосредственно в `data`, без `shopDto` |
| `/api/Roasters/{slug}` | `ApiResponse<RoasterDetails>` |
| `/api/Catalogs/cities/{slug}` | `{address: PublicAddress, name: string}` без `ApiResponse` |
| `/api/Catalogs/coffee-zones/{slug}` | `{address, name, description, latitude, longitude, radiusMeters, shopCount, polygon}` без `ApiResponse` |
| `/api/Users/by-slug/{slug}` | `{data: PublicUserProfile, address: PublicAddress}` |
| `/api/Users/me` | `ApiResponse<UserProfile>`; в `data.address` адрес текущего пользователя или `null` |
| `/api/users/{slug}/reviews` | `ApiResponse<Page<Review>>`, нужна авторизация |
| `/api/CheckIns` | `ApiResponse<Page<CheckIn>>` текущего пользователя, нужна авторизация |
| `/api/Map?minLat=…&minLon=…&maxLat=…&maxLon=…&zoom=…` | `ApiResponse<{shops, clusters, zones, isTruncated}>` |

Для деталей также доступны `/api/CoffeeShops/by-slug/{slug}`, `/api/Roasters/by-slug/{slug}`, `/api/Catalogs/cities/by-slug/{slug}`, `/api/Catalogs/coffee-zones/by-slug/{slug}`. Формат ответа совпадает с коротким маршрутом. У пользователей краткий `/api/Users/{slug}` тоже доступен, но `me` и `exists` — специальные маршруты; для профиля используйте `by-slug`.

У `ShopDetails` есть `address`, `city: PublicAddress | null`, `name`, `photos`, `rating`, `checkInCount`, `isVisited`, `isNew`, `isOpen`, `dataCompletenessScore`, `priceRange`, `type`, `location`, `beans`, `roasters`, `equipments`, `brewMethods`, `shopContact`, `description`, `checkIns` (публичные одобренные посещения), `userCheckIns` (посещения текущего пользователя), `schedules`, `tags`, `menu`. В деталях зёрна называются `beans`. `checkIns` содержит карточки с `text`, `createdAtUtc`, `rating`, `photos[].url`, `helpfulCount`; поля прежних отзывов `comment` и `photos[].fullUrl` к ним не применяются. Счётчик `checkInCount` — общее количество, а не длина вложенного списка.

У вложенного обжарщика: `{address, name, photoUrl, coverPhoto}`. У отзывов `author` и `shop` — адреса вместо `userId` и `coffeeShopId`. У check-in `shop` — адрес вместо `shopId`. У маркера карты `address` вместо `id`, `primaryZone` вместо `primaryZoneId`; у зоны карты `address` вместо `id`. В деталях обжарщика `shops[]` содержит `{address, name, coverPhoto}`.

Недоступная историческая связь с удалённой/скрытой сущностью возвращается как `null` или исключается из массива связей. GUID fallback отсутствует. Фото и содержимое отзывов используют прежние поля.

## Справочники и фильтры

Все шесть каталогов возвращают `ApiResponse<T[]>`: массив находится непосредственно в `data`, без обёрток `cities`, `beans` и т. п.

| GET `/api/Catalogs/…` | Элемент |
| --- | --- |
| `cities` | `{address, name}` |
| `roasters` | `{address, name, photoUrl, coverPhoto}` |
| `beans` | `{slug, name}` |
| `equipments` | `{slug, name, brand, model, category}` |
| `brew-methods` | `{slug, name, category}` |
| `shop-tags` | `{slug, name, description, sortOrder}`; только активные теги |

### Поиск кофеен: изменение контракта от 26.10.2026

`POST /api/v1/discovery/search` удалён вместе с маршрутом Gateway, DTO и схемами OpenAPI. Клиенты не должны вызывать этот endpoint.

`GET /api/CoffeeShops` полностью удалён, в том числе для выбора кофейни в чекине. Прежние параметры, формат списка `data.coffeeShops` и пагинация этого endpoint больше не являются действующим контрактом. Для обычного поиска и выбора кофейни в чекине используется `POST /api/v1/coffee-shops/search`.

Единственный endpoint поиска кофеен — `POST /api/v1/coffee-shops/search`. Обычный поиск не требует геолокации:

```http
POST /api/v1/coffee-shops/search
Content-Type: application/json

{"q":"coffee","filters":{"city":"minsk"},"page":1,"pageSize":20}
```

Пустой `q` клиент опускает. Фильтры `city`, `tags`, `roasters`, `equipments`, `beans`, `brewMethods`, `isOpen`, `isNew`, `visitedOnly`, `favoritesOnly`, `minRating`, `priceRange`, `type` передаются в JSON `filters`. Значения `type`: `specialty`, `coffee-bar`, `cafe`; `priceRange`: `cheap`, `moderate`, `expensive`. Пагинация — `page`, `pageSize` (1–100). Ответ — `ApiResponse<{items: ShopCard[], totalItems, totalPages, currentPage, pageSize}>`.

Для поиска рядом добавляются `filters.origin: {latitude, longitude}`, `filters.radiusKm` (в клиенте 3 км) и `sort: "distance_asc"`. Без `origin` клиент опускает радиус и сортировку, сервер выбирает `relevance` при запросе и `name_asc` без текста.

Карточка поиска содержит публичный адрес, название, адрес строкой, обложку, рейтинг, счётчик чекинов, признаки открытия/посещения/избранного, расстояние при наличии origin и теги. Координат в ней нет: при выборе подсказки карты клиент получает их через существующий запрос деталей. Тип, цена и признак новизны также не возвращаются в карточке; тип и новизна учитывают применённый фильтр.

Пагинация отзывов пользователя: `pageNumber`, `pageSize`. Для `/api/CheckIns`: заголовки `X-Page-Number`, `X-Page-Size`, опциональные `from`/`to` с часовым поясом, интервал `[from, to)`. Ответы также содержат `X-Total-Count`, `X-Total-Pages`, `X-Current-Page`, `X-Page-Size`.

## Запись

Текущий пользователь берётся из JWT; `userId` передавать не нужно. Для check-in, отзывов, жалоб, изменения кофейни и `/mine` нужен `Authorization: Bearer …`. Новую кофейню или обжарщика можно предложить анонимно.

```http
POST /api/CheckIns
Content-Type: application/json

{"shop":"26-october-16","isPublic":false,"visitedAt":"2026-09-30T12:00:00Z","rating":{"coffee":5,"place":4,"service":5},"note":"Отличный кофе"}
```

```http
POST /api/ModerationReviews
Content-Type: application/json

{"shop":"26-october-16","header":"Хорошее место","comment":"Отличный кофе","rating":{"coffee":5,"place":4,"service":5},"photos":[]}
```

У отзыва также поддерживаются плоские `ratingCoffee`, `ratingPlace`, `ratingService`; при наличии вложенного `rating` он имеет приоритет. Оценки и бизнес-ограничения проверяет существующий обработчик.

| POST | Изменённые поля запроса |
| --- | --- |
| `/api/ShopIssueReports` | `shop: string`; `category`, `description` сохраняются |
| `/api/ShopChangeRequests` | `shop: string`, `section`, `payload`; в payload `tags`, `roasters`, `equipments`, `brewMethods` — массивы строк вместо `*Ids` |
| `/api/ModerationShops` | `city: string`, `beans`, `equipments`, `roasters`, `brewMethods`: массивы строк; `name`, `address`, контакты, расписание и фото сохраняются |
| `/api/ModerationRoasters` | `city: string | null` вместо `cityId`; остальные поля сохраняются |

`null` у поля payload означает «поле не задано», `[]` — «очистить список». Одна заявка изменения относится к одной `section`; правила допустимых полей сохраняются. В gallery/menu `retainedPhotoIds` остаются служебными GUID фотографий.

```json
{"shop":"26-october-16","section":"Tags","payload":{"tags":["dog-friendly","laptop-friendly"]}}
```

`GET /api/ModerationShops/mine`, `/api/ModerationRoasters/mine`, `/api/ModerationReviews/mine`, `/api/ShopChangeRequests/mine` возвращают `ApiResponse<Page<T>>`, список всегда в `data.items`. В заявках кофейни `city` и `publishedShop` — адреса, справочники — массивы slug. В заявках отзывов/изменений `shop` — адрес. Фильтр заявок изменений называется `shop` и принимает slug.

`PATCH /api/Users/me/username` принимает `{"username":"Petr"}` и возвращает 202 с `data: {username, address}`. Ранняя повторная смена — 409. Адрес может быть `null`, если функция отключена или пользователь ещё не прошёл backfill.

## Ошибки и canonical

- 400: некорректный/reserved/GUID-shaped slug, слишком большой массив или невалидное тело.
- 401/403: аутентификация/права.
- 404: неизвестная, скрытая, удалённая сущность или неизвестный фильтр.
- 409: бизнес-конфликт, включая cooldown username.
- 503: адреса отключены, обязательный адрес ещё не создан либо внутренний сервис адресов недоступен. Не пытайтесь подставлять GUID.

При запросе alias сервер возвращает 200, текущие `slug`, `canonicalPath`, `revision` и `isAlias: true`. Клиент может заменить URL/canonical на полученный путь. API сам не выполняет 301. Публичные ответы с адресами используют `Cache-Control: no-store`.

Публичные `/{id}/public-address` и `/public-addresses?ids=…` удалены. `/internal/public-addresses/**` предназначен только для серверов и недоступен через gateway. Порядок включения и backfill описан в серверном runbook `public-addresses.md`.

## Применение в KMP-клиенте

Сетевые DTO читают `PublicAddressDto`; data преобразует его в независимый от
сериализации `PublicAddress`. Существующие имена `id`, `shopId`, `cityId` и
`*Ids` в моделях приложения и аргументах нативных маршрутов пока сохранены:
для публичных сущностей их значения теперь являются slug. `SerialName` в
запросах задаёт новые имена `shop`, `city`, `beans`, `equipments`, `roasters`,
`brewMethods` и `tags`. GUID отзывов, фото, заявок и ID сессии сохраняют смысл.

Ссылка «Поделиться» использует полученный `canonicalPath`. Свои отзывы и
сравнение автора используют `address.slug` из `/api/Users/me`, а не ID JWT.
Разрешение на отзыв и ID существующего отзыва берутся из деталей кофейни.

Сохранённый город сверяется со свежим каталогом. Старые записи локального
избранного сохраняются для просмотра и удаления, но без публичного адреса
переход к кофейне отключён: восстановить slug из GUID по этому контракту
невозможно. Новые записи сохраняют адрес. Исторические связи `null` не
превращаются в GUID; переходы к отсутствующей кофейне отключены.

Контракт проверяется JSON-тестами и `PublicAddressContractTest` с Ktor
MockEngine. Интеграция с новой сборкой сервера требует отдельной проверки
после её развёртывания.
