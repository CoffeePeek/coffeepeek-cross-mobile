# Contributions ownership exit — 2026-10-26

Branch `codex/migration-contributions-ownership`, child of
`codex/migration-discovery-ui-ownership` (#201). This is a separate feature
checkpoint; Discovery/Roaster changes remain in the parent diff.

## Точка остановки и порядок продолжения — 2026-10-27

**Остановились после замены legacy data reader Contributions, до разбора старых ViewModels.**
Работа в той же ветке `codex/migration-contributions-ownership` и draft
[PR #202](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/202); база — `9f5291b8`.
Стек не менялся: #195 → develop; #201 → feat/ios-build-foundation; #202 → codex/migration-discovery-ui-ownership.
Свежий `origin/develop` содержит один коммит поверх стека (`d8864318`, bump версии 1.1.2); он не затрагивает эти файлы
и не передавался в родителей этим срезом.

Что сделано:

- `ShopContributionCatalogs` и его чтения `ShopRepository.getCatalogs/getShopDetails/getMenuDrinks` удалены. Формы читают
  `ContributionCatalogs` (api port) через `SourceContributionCatalogs` поверх нового domain-интерфейса
  `ContributionCatalogSource`; data-реализация — `createContributionCatalogSource(client, roasters)` в `feature/contributions/data`.
  Из composeApp больше нет зависимости Contributions от legacy `ShopRepository`.
- Тот же HTTP, что у legacy: `/api/Catalogs/{cities,beans,equipments,brew-methods,shop-tags}`, `/api/menu/drinks`,
  `/api/CoffeeShops/{slug}` (slug — закодированный сегмент пути). DTO узкие и приватные: читаются только поля форм,
  остальное сервер может менять. Ничего из legacy DTO не проходит через core, чужие feature domain/data не импортируются.
- Roasters берутся из публичного `PublicRoasterCatalog` (Koin `single`, тот же список и кэш, что у экранов Roaster и Discovery):
  `CatalogOption(slug публичного адреса, имя, логотип)`. По смыслу тот же отбор, что у legacy (roaster с публичным адресом); побайтное равенство id с прежним `toCatalogItem` отдельно не сверялось.
- Кэш: каталоги и список напитков хранятся на время жизни источника после успешного чтения; сбой не кэшируется и повторяется
  следующим вызовом; одновременные вызовы делят одну загрузку, она выполняется в собственном scope и сама записывает успех,
  поэтому ушедший вызывающий её не отменяет и сразу следующий не запускает второе чтение. Снимок кофейни читается каждый раз заново.
- Снимок кофейни сохраняет прежнюю форму: название, описание, контакты, фото (маленький вариант, без фото без id/url, по sortIndex),
  id тегов (сначала `shopTags`, иначе `tags`, без повторов), id roasters/equipment/brew methods по public slug, строки меню
  (доступность `Present/1`, `Absent/2`, иначе Unknown; цена числом или строкой с запятой) и фото меню. Строки меню идут в порядке
  каталога напитков, неизвестные — после; сбой каталога напитков не ломает снимок.
- Различие с legacy, намеренное: снимок больше не читает признаки избранного, чекины и отзывы кофейни (в форме они не нужны);
  неправильные посторонние части ответа не могут его сломать.

Что делать следующим, по порядку:

1. Старые ViewModels и DI — **выполнено в следующем срезе (2026-10-27), см. раздел ниже**; остался только `ShopChangeEditor*`.
2. Ручные и платформенные gates без изменений: Sandbox темы/шрифты/accessibility/modal/back, native picker/map/camera,
   выполнение Native tests, полный Xcode app build. Реальный ответ API этими тестами не проверен: данные — фикстуры MockEngine;
   Sandbox отдаёт те же маршруты, но экраны форм на устройстве не запускались.
3. После Contributions по LEGACY_EXIT_PLAN: root create/tab UI, platform bridges, legacy favorites/session/storage/transport, L4.

Не трогать пользовательский локальный `iosApp/iosApp.xcodeproj/project.pbxproj` (48+/41−): в коммиты он не входит.

### Проверка среза — 2026-10-27

Последовательные запуски Gradle, `--max-workers=6`, без автоматических UI/instrumentation/screenshot тестов:

- Contributions: domain **16**, data **29** (из них **10** новых на `ContributionCatalogSource`), impl **76** тестов, 0 failures/errors/skips.
  Новые сценарии: маппинг всех списков и логотипа roaster, public-slug над plain slug, кэш и повтор после сбоя, одна загрузка для одновременных
  вызовов (трижды подряд), сбой roasters, имена и порядок напитков, форма снимка, порядок меню по каталогу и без него, свежесть снимка,
  кодирование slug, отклонённый ответ и HTTP-ошибка.
- Первый запуск теста одновременных вызовов упал: быстрый ответ успевал завершить загрузку раньше, чем первый вызывающий записывал успех,
  и второй вызывающий запускал повторное чтение (то же поведение было в legacy). Запись успеха перенесена в саму загрузку; повторные прогоны зелёные.
- composeApp: Direct **823** и Play **823** теста (172 набора каждый), 0 failures/errors/skips; варианты не складываются в уникальные сценарии.
  Число на 2 меньше прежних 825: четыре теста удалённого legacy-моста заменены двумя тестами адаптера и roaster-опций.
- `lintDebug`, `:composeApp:lintDirectDebug`, `:composeApp:lintPlayDebug` успешны.
- `assembleDirectDebug`, `assembleDirectSandbox`, `verifyDirectDebugRuntimeClasses`, `verifyDirectSandboxRuntimeClasses`, `verifyDirectSandboxIsolation` успешны.
  SHA-256: DirectDebug `954176dd29806876f36122a5a4a6e96c4ea8b427df7e88c166c8f42f34768945`;
  DirectSandbox `62baa28942984c65068171135fd8bd5acd3f7a9f52562dcd274e148234d8b337`. APK не устанавливались.
- Common metadata Contributions domain/data/impl, `compileKotlinIosArm64` и `linkDebugFrameworkIosSimulatorArm64` успешны.
  Native tests не выполнялись; framework link не заменяет полный Xcode app build.

### Старые ViewModels Contributions — 2026-10-27

Сверено покрытие и удалено недостижимое; всё в той же ветке и #202.

- Удалены: `AddShopViewModel`, `MyContributionsViewModel`, `AddRoasterViewModel`, `SuggestShopChangeViewModel`,
  `ShopChangeRequestDetailViewModel` и пять их фабрик в `KoinApp.kt`. Ни один production-маршрут их не использовал:
  экраны идут через feature entries. Тесты старого кода удалены вместе с ним: `ContributionsParityGapsTest` (21),
  `MyContributionsUiStateTest` (2) и два теста форм кофейни и обжарщика из `SubmissionViewModelTest`; в последнем его тесте убраны проверки этих двух форм.
- Из файла `MyContributionsViewModel.kt` сохранён только enum `ContributionKind` (в новом файле, без неиспользуемых
  `title`/`emptyText`): на него опираются маршруты, renderer и навигация уведомлений.
- Оставлены, потому что их держит чужой `ShopMenuEditorTest`: `ShopChangeEditorScreen`, `ShopChangeEditorViewModel`,
  `ShopChangeLabels` и фабрика редактора в Koin; в `SubmissionViewModelTest` остались три теста редактора (последний сужен до редактора). Остаток **не закрыт**.
  Чужой тест не менялся и не запускался.
- Соответствие сценариев (все 21 теста parity и пять submission-тестов сверены по коду, не по названиям):
  список заявок — `ContributionsListViewModelTest` (маппинг статусов, подзаголовок обжарщика, строка изменений, сбой статуса,
  подгрузка, последняя страница, выбор вкладки); мастер кофейни — `AddShopViewModelTest` и `ScheduleRulesTest`
  (обрезка названия, шаги, единственный город, выбранная точка, фото, шаги часов, расписание, отправка, повтор после сбоя);
  редактор правок — `ChangeEditorViewModelTest`; форма обжарщика — `AddRoasterViewModelTest`. Намеренные отличия: feature принимает
  ссылку Instagram в контактах правки (legacy её отвергал); сбой каталога городов в feature даёт типизированное состояние,
  а не текст; бросок исключения из репозитория не покрыт, потому что контракт feature-репозитория возвращает `Result`.
- **Найден и закрыт пробел:** legacy-форма обжарщика не давала отправлять, пока грузится каталог городов, а feature-форма давала
  (кнопка активна при валидной форме). Теперь отправка ждёт ответа списка городов, сбой списка не блокирует; добавлен
  `AddRoasterState.ready`, защита во ViewModel и в кнопке. Тест на блокировку проверен: без защиты во ViewModel он падает.
  Второй новый тест — после успеха форму нельзя отправить повторно, после сбоя можно (legacy проверял это в submission-тесте).

Проверка (Mac, последовательно, `--max-workers=6`, без автоматических UI/instrumentation/screenshot тестов):

- Contributions: domain **16**, data **29**, impl **78** тестов (76 прежних и 2 новых), 0 failures/errors/skips.
- composeApp: Direct **798** и Play **798** тестов (170 наборов каждый), 0 failures/errors/skips. Было 823; разница 25 = 21 + 2 + 2
  удалённых теста старого кода (варианты не складываются в уникальные сценарии).
- `lintDebug`, `:composeApp:lintDirectDebug`, `:composeApp:lintPlayDebug`; `verifyDirectDebugRuntimeClasses`,
  `verifyDirectSandboxRuntimeClasses`, `verifyDirectSandboxIsolation`; `compileKotlinIosArm64`, `compileTestKotlinIosSimulatorArm64`,
  `linkDebugFrameworkIosSimulatorArm64` — успешны. SHA-256 DirectDebug `916c3d26cb6659d520383326fb497e4dec410e9a96048ae79d6892bb5fa02ac7`,
  DirectSandbox `37ef7bd6663b89cd705fafc13e540ba9140bd8e79be7a88af7b525fa86e2afec`. APK не устанавливались.
- `:composeApp:compileDirectDebugAndroidTestKotlinAndroid` **падает** на двух файлах, не связанных с этим срезом:
  `CpImageLoadingTest` и `DisplayPhotoTransparencyTest` ссылаются на `PrefetchPhotos` и `AndroidImageCodec`, которых нет в main.
  `ShopMenuEditorTest` компилируется. Эти instrumented-тесты устарели ещё до среза и не запускались; починка — отдельная задача.

## Предыдущая точка остановки (история) — 2026-10-26

**Остановились после публикации UI ownership среза, до замены legacy data reader.**
Код проверен и опубликован коммитом `cd1bac2b` в draft
[PR #202](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/202).
Продолжать в этой же ветке и PR: это поддерживающая часть Contributions,
новую фичу/ветку пока не начинать.

Стек: #195 → develop; #201 → feat/ios-build-foundation;
#202 → codex/migration-discovery-ui-ownership. В ветке уже есть CI fix
родителя `027c2029`, переданный через #201 (`89778c23`). Android CI #195
успешен на GitHub; iOS verification при проверке этого checkpoint ещё выполняется.

Что завершено: активные формы/фото/цена/иконки/изображения/выбор точки принадлежат
feature/DS, семь старых app UI файлов удалены. Полный локальный Android CI,
Sandbox и iOS framework прошли; точные counts, APK hashes и ограничения ниже.

Что делать следующим, по порядку:

1. Проверить свежие develop/родительские ветки и фактические consumers. При
   обновлениях проводить синхронизацию от #195 через #201 к #202.
2. Заменить `ShopContributionCatalogs` в
   `composeApp/src/commonMain/kotlin/com/coffeepeek/admin/di/contributions/ContributionsAdapters.kt`:
   сейчас `catalogs()`, `shop(slug)`, `drinks()` обращаются к legacy
   `ShopRepository.getCatalogs/getShopDetails/getMenuDrinks`. Это ещё **не сделано**.
3. Сначала сверить существующие публичные Shop/Roaster/catalog API и реальные
   HTTP/mapper/cache contracts. `PublicShopPreviews` не является полной заменой
   editor snapshot: нужны также контакты, IDs сохранённых фото/фото меню,
   выбранные catalog IDs и строки меню. Сохранить preload/overlay существующей
   заявки, retries, offline cache, cancellation и смену владельца. Не обращаться
   из feature к чужим domain/data и не создавать второй каталог/кэш в core.
4. Обновить composition adapters/DI и non-UI contract tests; удалить заменённый
   legacy bridge после проверки последнего активного consumer. Оставшиеся старые
   ViewModels/DI разбирать по фактическим ссылкам. `ShopChangeEditorScreen` пока
   удерживается чужим `ShopMenuEditorTest`; не удалять чужой тест и не создавать
   ему новую UI test infrastructure. Этот остаток не объявлять закрытым.
5. Повторить common metadata, затронутые unit/contracts, Android APK/DEX/lint и
   iOS compile/framework. Ручные Sandbox темы/шрифты/accessibility/modal/back,
   native picker/map/camera, Native test execution и полный Xcode app build
   остаются отдельными непроверенными gates. Автоматические UI-тесты не делать.

В основном checkout нет незакоммиченного кода Contributions. Единственный
локальный diff — пользовательский `iosApp/iosApp.xcodeproj/project.pbxproj`
(48+/41−); не включать его в коммиты и не сбрасывать. Временный checkout CI
удалён после проверки опубликованных коммитов; открытые ветки стека сохранены.

## Scope and ownership

Existing api/domain/data/impl modules remain in place. No new Gradle module,
backend endpoint, submission flow, product feature or analytics service is added.
Android and iOS already call the feature entries through root renderers.

- [x] Remove the public application-owned photo/price/brew/logo/cover/remote-image
  UI slots. Hosts supply only native launchers, permission, native map canvas and
  success/error feedback. The feature composes existing design-system primitives.
- [x] Move photo-source/attachment presentation out of composeApp. Shared DS photo
  attachments have an explicit Simple mode preserving the old 96dp grid and
  remove badge; reorder, position numbers, manage hint and viewer are absent in
  this mode. Existing default consumers retain their ReorderAndPreview behavior.
- [x] Keep platform URI/JPEG/camera handling in composition behind a narrow API.
  Launchers are scoped to requests; feature delivery bounds selection to current
  room, owns byte buffers, drops duplicates/superseded/disposed callbacks and
  checks the account at delivery. Account changes re-key ephemeral form UI.
- [x] Move map selection state/dialog/header/pin/confirmation to Contributions.
  The app supplies CoffeeMap only, with the existing native provider. The feature
  retains 350ms address debounce, sub-meter drift tolerance, center/target/location
  callbacks, zoom 16, compact-landscape layout and a fixed two-line address panel
  outside the viewport. Confirmation requires the currently resolved point.
- [x] Preserve native iOS map-control clipping/z-order through the existing DS
  presenter capability. No UIKit implementation is copied into a feature.
- [x] Preserve price thresholds, slider clamp, RU/EN brew-name compatibility,
  28dp logos/6sp placeholders, 16sp cover fallback, saved-photo Crop and haptics.
- [x] Delete seven obsolete app UI files with no remaining external consumers:
  AddShop, AddShopSuccess, PickShopLocationDialog, AddRoaster, MyContributions,
  SuggestShopChange and ShopChangeRequestDetail.
- [x] Replace ShopContributionCatalogs legacy details/catalog/drink reads with
  Contributions-owned data contracts and the public Roaster catalog (2026-10-27,
  see the stop point above). No legacy DTO passes through core and no feature
  imports another feature's data/domain.
- [ ] Retire remaining old ViewModels and their DI after their last test/data
  consumers migrate. Preserve other contributors' tests: ShopChangeEditorScreen
  still has an existing ShopMenuEditorTest consumer; it is retained for that
  reason, not selected by either production route. This UI suite is not run,
  extended, restored or compiled by this checkpoint.

## Parity and verification

Legacy behavior reference: [LEGACY_INVENTORY.md](LEGACY_INVENTORY.md).
Root routes and analytics page mapping are unchanged: MyContributions and
ShopChangeRequestDetail map to contributions; SuggestShopChange/ShopChangeEditor/
AddShop map to shop_edit. Legacy forms had no feature-owned event tracker. This
slice adds no events, SDK or second consent/session policy.

The first compile found a trailing-lambda preview mismatch after extending the
shared attachment and the required dismiss accessibility label. Both were fixed;
the Android/iOS compile and pre-existing Contributions/DS unit suites then passed
(59s). New cancellation/photo-delivery regressions were added afterwards, so that
earlier result does not certify them. Final verification is recorded below.

Verification plan: Contributions domain/data/impl and design-system unit tests;
app non-UI Contributions/location/renderer/analytics/Sandbox contracts; Android
Debug/Sandbox assembly with packaged DEX guards; shared metadata and iOS production
compile/framework/common test compile. Gradle runs sequentially, --max-workers=6.
No automated UI/instrumentation/screenshot tests or test Activities/APKs.

Manual Sandbox theme/font/window/accessibility review, real native map/picker/
camera permissions, keyboard, modal/back/cancel and full Xcode app build remain
separate acceptance gates. No account mutation, production telemetry, app install
or device run is authorized merely by an assembly or fake contract result.

The user's local iosApp project.pbxproj diff is excluded from this checkpoint.
The parent #195 CI repair was isolated while these feature changes were still
uncommitted, then propagated through #201 before publishing this child.

## Final verification — 2026-10-26

Source `89778c23` + this ownership slice; 9m34s, all requested tasks successful.
Counts come from the tasks in this run, excluding stale retired-module outputs:

- Library testDebugUnitTest: **1501 tests / 231 suites** across 55 tasks;
  app Direct and Play: **825 tests / 172 suites each**, zero failures/errors/skips.
  The variants are not added together as unique scenarios. Cached/up-to-date
  results are distinguished from freshly executed tasks. Contributions
  domain/data/impl results: 16/19/76, DS: 57.
- Shared metadata compilation: core media/DS and Contributions domain/api/impl.
- Direct/Play Debug and Direct Sandbox APKs assembled; packaged DEX guards
  include new API/entry/map/photo components; Sandbox isolation passed.
- Full Android CI aggregate unit and lint tasks passed. iOS Arm64 production
  compile, Simulator Debug framework link and common Native test compilation
  Contrib impl/DS/app passed. Native tests were not executed; full Swift/Xcode
  app build is not certified by a framework link.
- Studio inspections of map state and photos timed out without reported errors:
  partial diagnostics, not a clean IDE or preview-rendering result. Existing
  preview deprecation/expect-actual/opt-in warnings remain outside this slice.
- New unit contracts cover 350ms debounce, drift, current-point confirmation,
  stale/non-cooperative cancelled geocoding, missing/failed address, capacities,
  copied bytes, duplicate/superseded/disposed photo callbacks and account gating.
  No automated UI tests or real native device actions were used.

APK SHA-256:

- DirectDebug: `9c3bd229aec977ec840aaf74994fa894d51e0f78f3ad5121630e8dc724c216db`
- DirectSandbox: `e6d2467318c14cb1468824c011eda0e42a77662e104e8f2a3d80cb076e4df77e`
- PlayDebug: `106c64f609736357c281166f16c3aad98340e0bfa4b8b07d8d6691decae81862`

Parent CI repair `027c2029` has a successful GitHub Android CI run
[40384202349](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/40384202349).
Its iOS verification was still running at the last observed status; local success
is not substituted for a remote result. The same full Android CI also passed
locally on #201 (`89778c23`) before being passed to this branch.
