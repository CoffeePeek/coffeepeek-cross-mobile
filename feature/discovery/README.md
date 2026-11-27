# Поиск, карта и выбор кофейни

Сверено с `develop` `8cdc15f9` — 2026-11-27.

## Владение и активные экраны

`api` предоставляет tab/picker entries, map canvas, catalog и account/city/location/
favorites/analytics ports; `domain` — criteria, геометрию и repository contracts;
`data` — search/map/catalog HTTP и heatmap reads; `impl` — `SearchViewModel`,
`MapViewModel`, Android Compose и shared/native presentation sessions.
Composition соединяет их с session, выбранным городом, permission effects,
MapLibre и Roaster API. Root владеет tab/navigation, auth и check-in continuation.

Android использует feature Compose. iOS Discovery tab и CreateCheckIn shop picker
используют SwiftUI и destination-owned Kotlin sessions. Нативные roaster slots
собираются composition через Roaster session; они больше не являются следующим
неподключённым Compose checkpoint. Один native map остаётся смонтированным при
preview expansion/collapse, а списки/controls/zone cards/carousel рисует SwiftUI.
Global plus/type-choice sheet — отдельный app route, не этот shop picker.

## Поиск, город, permission и favorites

- List search и map text suggestions используют текущий выбранный город.
  Ручной выбор имеет приоритет после перезапуска. Автовыбор сопоставляет позицию
  и каталог независимо от порядка ответов; города без известного центра доступны
  вручную. Map viewport request не ограничивается городом списка.
- Nearby виден без GPS. Тап вызывает platform permission/fresh position flow;
  denial, timeout и permanently denied сохраняют recovery. Request identity
  проверяется до принятия callback, повторный разрешённый запрос работает.
  При отзыве permission stale origin/distance очищаются. Возврат из OS settings
  переоценивает authorization/position без изменения настроек телефона агентом.
- Query debounce, принятые criteria и retained rows принадлежат Kotlin. Pending
  query не должна выглядеть как подтверждённый empty result; stale rows не дают
  пагинировать или выбирать результат другой criteria.
- Гости сохраняют name search и public quick filters; advanced filters и roaster
  destination следуют существующим sign-in gates. Suggest-add карточки появляются
  после подтверждённых пустых shop/roaster результатов, с отличной от ошибки recovery.
- Heart передаёт управление [Collections](../collections/README.md) немедленно,
  включая уже выбранное сердце. Picker cancel не делает favorite write.
- Android и нативный фильтр имеют собственный draft. На текущем iOS крестик и
  swipe применяют draft через guarded apply, отдельной Done-кнопки нет. Reset
  меняет draft; при применении сохраняется актуальный city. Не восстанавливать
  прежнее описание «любой dismiss всегда отменяет выбор».
- Retap Search закрывает overlays/keyboard и запрашивает top-scroll, сохраняя
  query/city/filters; обычный переход вкладок не воспроизводит старый retap.

## Карта, clusters и heatmap

Map читает `POST /api/v1/coffee-shops/map` по camera bounds и фактическому zoom;
query/filters/Nearby origin согласованы с search. Camera center не подменяет GPS
origin. Нет silent fallback на старый нефильтрованный endpoint. Points/clusters/
zones — взаимно исключающие представления текущего ответа; cached overlays
предыдущего viewport не превращаются в stale count groups.

Cluster tap вписывает bounds и ограничивает шаг zoom; faces используют count,
type icon при единице и доступное полное описание. Native transitions допускают
interruption и учитывают reduced motion. Detailed carousel читает область вокруг
viewport, сохраняя surviving card order; поздний coarse response не стирает
более актуальные detailed cards. Viewport/filter changes обновляют карту
автоматически; старой кнопки «Искать в этой области» нет.

Heatmap использует только публичные CoffeePeek check-in counts: detailed map
points соединяются с paged search counters по slug. Невалидные координаты и
неположительные counts исключаются, missing/truncated data отмечается partial.
Полностью загруженные публичные counters кэшируются на 60 секунд; personalized
visited/favorites selections не используют этот cache. Ошибки/отмена не публикуют
частичный cache. Camera-only refresh сохраняет старые точки до успешного ответа;
успешный empty очищает их. Filter/account invalidation и toggle-off отзывают
старое состояние. Heatmap и zones взаимоисключающие; легенда/выбор dataset удалены.

## Источники и проверка

[Entry contracts](api/src/), [data/map/heatmap](data/src/),
[ViewModels/native sessions/unit tests](impl/src/),
[native composition](../../composeApp/src/iosMain/kotlin/com/coffeepeek/admin/di/discovery/),
[SwiftUI и MapLibre](../../iosXtool/CoffeePeek/Sources/CoffeePeek/).

Unit/contracts покрывают debounce/accepted rows/paging, empty/catalog/error retry,
city/permission recovery, map viewport/cache races, clusters, heatmap и account
replacement. Изменения требуют затронутых suites, Android assembly/packaged DEX,
а native части — macOS Swift/Native checks в
[ios-verify.yml](../../.github/workflows/ios-verify.yml).

Ручные gates: rapid list/map/roaster open-close, scroll restoration, keyboard,
permission/settings/real GPS, marker/cluster/zone/heatmap/carousel gestures,
ru/be/themes, large text и screen readers. Sandbox не заверяет live geography,
OAuth или backend filtering. Документационная сверка не выполняла эти gates
заново; прошлые rollout/failure records доступны в Git history.
