# Добавление мест и предложения изменений

Сверено с `develop` `8cdc15f9` — 2026-11-27.

## Владение

`api` содержит entries форм, истории, request detail и narrow photo/location ports.
`domain` владеет draft/validation/schedule/submission rules и repository contracts;
`data` — submission HTTP/DTO и `ContributionCatalogSource`; `impl` — MVI, Android
Compose и Kotlin adapters нативных форм. Composition предоставляет permissions,
подготовку platform media, MapLibre canvas, feedback и root navigation.

Активны AddShop (пятишаговый мастер), AddRoaster, SuggestShopChange,
ShopChangeEditor, MyContributions и ShopChangeRequestDetail. Android использует
feature Compose, iOS — SwiftUI формы и историю через destination-owned sessions.
Старые app-level AddShop/AddRoaster/SuggestShopChange screens/ViewModels и legacy
catalog reader не являются production path. Не восстанавливать их как fallback.
Глобальная незавершённость migration отслеживается в
[LEGACY_EXIT_PLAN](../LEGACY_EXIT_PLAN.md), а не в старом порядке PR #202.

## Каталоги, revisions и writes

- `ContributionCatalogSource` читает собственные narrow DTO городов, beans,
  equipment, brew methods, shop tags, menu drinks и снимка кофейни. Roaster options
  приходят через application adapter публичного Roaster API; чужие domain/data
  не импортируются.
- Успешные каталоги/drinks кэшируются на lifetime источника; ошибка повторяется
  следующим вызовом. Одновременные callers делят catalog load, а уход одного
  ожидающего не отменяет загрузку для остальных. Shop snapshot читается свежим.
- В snapshot сохраняются нужные форме contacts, photo IDs/order, catalog IDs,
  menu availability/prices/photos и `inputRevision`. Favorite/check-in metadata
  формы не требуется.
- Menu editor передаёт `payload.menu.expectedInputRevision` из прочитанного
  snapshot/draft. Revision zero допустим для отсутствующего меню; missing/negative
  revision существующего меню отклоняется до upload/submission. Retry существующей
  заявки сохраняет наблюдавшуюся revision.
- Допустимые catalog IDs и модерация определяются серверным контрактом; успешный
  fake submit не доказывает, что production validator принимает новый ID.
- Non-owner изменения кофейни идут на модерацию. Подтверждённый owner direct edit,
  claims и business mode принадлежат [Business ownership](../business-ownership/README.md).

## Фото, карта и native lifetime

Feature владеет attachment presentation, лимитами и порядком; platform bridge —
URI/JPEG/camera/permission. `ContributionPhotoDelivery` копирует входные bytes,
ограничивает delivery текущим свободным местом, игнорирует duplicate, superseded,
disposed и previous-account callbacks. Draft сохраняется после отмены picker.

`ContributionMapPickerState` сохраняет debounce адреса 350 ms, drift tolerance,
current-point confirmation и StateFlow observation. MapLibre provider рисует карту;
feature/native screen — pin, controls и address/confirm. Подтверждение возможно
только для актуальной разрешённой точки. Native observation живёт до disposal
destination, включая временное покрытие map/gallery/camera.

Все четыре нативные формы используют существующие Kotlin validation/actions.
Back во время отправки поглощается и не уничтожает in-flight destination.
Busy/dirty, account replacement, menu revision и partial failure не обходятся
native callbacks. Успех/ошибка используют существующую haptic adaptation;
screen/exposure analytics сохраняют прежние root route IDs, новый tracker не создаётся.

## Источники и проверка

- [Публичные entries](api/src/), [domain rules](domain/src/),
  [catalog/submission implementation](data/src/), [presentation/tests](impl/src/).
- [Нативные формы](../../iosXtool/CoffeePeek/Sources/CoffeePeek/) и
  [composition adapters](../../composeApp/src/).

Unit/contracts покрывают steps/schedule, catalog mapping/cache/retry, menu revision,
account guards, late photo callbacks, геокодирование, submit/retry и native Back.
Платформенные сборки и Native test selection заданы в
[ios-verify.yml](../../.github/workflows/ios-verify.yml) и Android CI.
При изменениях нужны relevant suites и Android assembly/packaged DEX checks;
Native compilation/исполнение отдельно подтверждает macOS runner.

Ручные gates: Photos/iCloud/camera permission/cancel, повторный плюс, denied/manual
location, map pan/confirmation, keyboard, Back/dirty/busy states, ru/be, темы,
large text/accessibility и safe-account submission outcomes. Эти проверки заново
не выполнялись при документировании; исторические build logs и исправления
находятся в Git history. Не запускать и не восстанавливать UI/instrumented tests
ради удаления старого migration checklist.
