# Полный выход из legacy

Статус: в работе. Решение владельца от 2026-10-25. База `029a572a`,
ветка `feat/ios-build-foundation`, PR #195 → develop.
Вариант для кофейни отложен; новые продуктовые возможности не добавляем.
Текущий дочерний срез: `codex/migration-discovery-ui-ownership` от `7f78ea21`,
draft PR #201 → открытая родительская ветка `feat/ios-build-foundation` (#195).

## Условие завершения

Переключённые маршруты — промежуточный результат. Миграция закончена, когда:

- Android и iOS используют только feature entries, без старых экранов,
  ViewModel, карточек, бизнес-слотов, репозиториев и моделей из composeApp/modules.
- В composeApp остаются запуск, конфигурация платформы, root DI и корневая
  навигация с вызовами публичных feature entries. Корневой setContent/host —
  сборка приложения; экранов, карточек, диалогов и feature state там нет.
- Общие UI/ресурсы/платформенные визуальные примитивы принадлежат
  core/design-system; бизнес-UI, включая модальные варианты, — feature/*/impl.
- `modules/` и `module.legacy.*` удалены, прямых/транзитивных ссылок нет.
- Не осталось typealias/обёрток, которые называют старую реализацию новой.
- Существующие данные, серверное избранное, consent, аналитика, session guards
  и native поведение сохранены. Обновление не очищает локальную БД/Keychain.

Нативная оболочка iosApp, lifecycle и SDK-провайдеры не являются устаревшими
экранами. Они остаются платформенной границей. Из composeApp реализации общих
платформенных сервисов также уходят в подходящий core; бизнес-часть карты —
в Discovery. Новый глобальный core/common/utils и новая app-модель не нужны.

## Подтверждённый остаток, 2026-10-25

Подсчёт импортов только production commonMain/androidMain/iosMain composeApp:

| Legacy package | Файлов-потребителей | Примеры реальных зависимостей |
| --- | ---: | --- |
| com.coffeepeek.domain | 115 | ShopRepository, CheckIn, CatalogItem, UserRepository, SessionRepository, City |
| com.coffeepeek.data | 3 | KoinApp/dataModule, AuthAdapters/UserSessionCleaner, SessionRealtimeManager |
| com.coffeepeek.api | 18 | CoffeePeekClient, ApiException, RequestObservation |
| com.coffeepeek.room | 10 | SettingRepository, DatabaseCore, MyRoomRepositoryImpl |
| com.coffeepeek.legacy.homecheckin | 11 | прежние календарь/история/Home readers |

Один файл может быть в нескольких строках. Это инвентарь ссылок, не число
активных экранов. Включая тесты, потребителей соответственно 209/7/28/26/24.
Файлов с @Composable в production composeApp: commonMain 231, androidMain 27,
iosMain 28. Здесь смешаны недостижимый UI, активные вставки, инфраструктура и
root hosts; нельзя ни считать всё активным, ни удалять по одному этому признаку.

Основные feature renderers уже выбраны в PlatformKoin.ios.kt и native tabs.
Но FeaturePublicProfileScreenRenderer вызывает app-owned favorite/check-in/
Activity UI, FeatureCheckInHistoryScreenRenderer — HistoryTimelineCheckInCard.
KoinApp сохраняет Legacy* defaults, dataModule строит старые writers/readers.
core/database пока содержит только конфигурацию драйвера, не готовую замену
DatabaseCore. Старый LEGACY_REMOVAL.md — исторический снимок 2026-10-21.

## Точка остановки — 2026-10-27

Текущая ветка `codex/migration-contributions-ownership`, draft
[PR #202](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/202) → #201.
Последний проверенный код — коммиты поверх `9f5291b8` (`8ba59ab1` и следующий): Contributions больше не читает каталоги, снимок кофейни и
напитки через legacy `ShopRepository` (`ShopContributionCatalogs` удалён); данные читает собственный источник в
`feature/contributions/data`, roasters — из общего `PublicRoasterCatalog`. Подробности, сравнение с legacy и проверки:
[Contributions ownership exit — точка остановки](contributions/OWNERSHIP_EXIT_PLAN.md).

Следующий срез в той же ветке/#202 выполнен: пять недостижимых Contributions ViewModels, их Koin-фабрики и тесты старого
кода удалены после сверки покрытия с feature-тестами; найденный пробел (отправка формы обжарщика во время загрузки городов)
закрыт. Остаются `ShopChangeEditorScreen`/`ShopChangeEditorViewModel`/`ShopChangeLabels`: их держит чужой `ShopMenuEditorTest`.
Instrumented-компиляция `compileDirectDebugAndroidTestKotlinAndroid` падает на двух не связанных файлах (`CpImageLoadingTest`,
`DisplayPhotoTransparencyTest`) — отдельная задача. Подробности: [Contributions ownership exit](contributions/OWNERSHIP_EXIT_PLAN.md).

CI-фикс #195 `027c2029` уже в стеке (#201 `89778c23`). Свежий `origin/develop` (`d8864318`, bump 1.1.2) в стек пока не передан.
Полный локальный Android lint/unit/APK/DEX/Sandbox и iOS compile/framework для этого среза прошли; ручная/native/full Xcode
приёмка открыта. После Contributions остаются root create/tab UI, platform bridges, legacy favorites/session/storage/transport и
L4 cleanup старых consumers. Полный выход из legacy ещё не завершён.

Не трогать пользовательский локальный project.pbxproj (48+/41−). Продолжать стек, не main; перед кодом сверить свежие
remotes и при необходимости передать изменения сначала в родителя, затем в потомков. Новую бизнес-фичу пока не начинать.

## Очерёдность срезов

### L1. Убрать бизнес-UI из активных app slots

- [x] Profile UI: публичное избранное, публичная недельная Activity, карточки
  чекинов; на публичном профиле никогда не подключать личный reader.
  Избранное/неделя — первый срез; карточки — второй. Public check-in reader
  заменён в третьем срезе L3, enrichment/own-account ports — в четвёртом.
  Недостижимые старые UI/VM/test consumers остаются отдельным L4.
- [x] History/Profile: один владелец общей геометрии чекина через
  существующие DS primitives; без преобразования обратно в legacy CheckIn.
- [x] Собственные визуальные слоты Discovery (фото/заглушки/error/фильтры/
  catalogue icons/prefetch) больше не приходят из composeApp; седьмой срез.
- [x] Вложенные Roaster search/empty/error/image/viewer: активный UI принадлежит
  Roaster impl, публичные app visual ports удалены; восьмой срез.
- [x] Contributions: активные photo/price/icons/images/map-picker UI и состояние
  принадлежат impl/DS, root поставляет native canvas/launchers; десятый срез.
- [ ] Остальные catalog/create UI и root tab содержание:
  оставшиеся бизнес-вставки/состояние из composeApp — в соответствующие impl.
- [x] Author blocking: экран/host из composeApp в feature/author-blocking/impl;
  Android/Sandbox и видимый native iOS controller вызывают feature entry.
  Ручная приёмка native prompts остаётся открытой в шестом срезе.
- [ ] Root tabs/check-in creation host: root оставляет routing, содержимое —
  feature entries. Вкладка Coffee не должна зависеть от legacy CheckInTabContent.

Не переносим весь composeApp/ui в feature одним каталогом. Каждая вставка
переходит к владельцу, её ресурсы и paired previews — вместе с ней. Общая
геометрия карточек/календаря уже есть в core/design-system и переиспользуется.

### L2. Убрать app-owned инфраструктурный UI и сервисы

- [x] Владение CpImage, prefetch, original/preview contracts, viewer перенесено
  в core/media и design-system; старые реализации и aliases удалены.
- [ ] Ручная приёмка общего media pipeline/viewer на Android/iOS: темы,
  жесты, safe area, фон/возврат, отзыв доступа и private фото. Сборки и unit
  contracts не заменяют эту проверку; подробности пятого среза ниже.
- [ ] Нативные sheets/alerts/keyboard/headers/glass/theme/иконки: реализации
  в core/design-system; composeApp только устанавливает платформенные зависимости.
- [ ] Даты/форматирование, browser/share/maps/photo picker: узкие платформенные
  порты у действительного core/feature владельца; не общий мешок helpers.
- [ ] Analytics SDK/session/consent infrastructure: core с существующим сервисом;
  определения бизнес-событий — features, routing/exposure ownership не дублируется.

Новые core-модули создаются только при конкретной общей ответственности и
проверенных consumers; описываем зависимости, API, скрытую реализацию и причину,
почему существующего модуля недостаточно. Это не массовое создание api/data/domain.

### L3. Заменить активные legacy data adapters

- [x] Public profile check-ins — profile data через read-only community API,
  без старого PublicProfileRepository/FeedRepository/CheckIn projection.
- [x] Public favorites enrichment и own-account ports — feature data,
  без старых ShopRepository/RoasterRepository/UserRepository в активном graph
  Profile. Четвёртый срез также переключает identity readers Shop/Community;
  старые UI/VM/test consumers остаются отдельной задачей L4.
- [x] Discovery map-details/filter catalog/picker и Roaster shop-photo reader —
  публичные Shop/Roaster API вместо legacy ShopRepository (девятый срез #201).
- [ ] Остальные Shop/catalog/roaster/favorites readers и caches — соответствующие feature
  repositories; composition соединяет только узкие api-контракты.
- [ ] Auth cleanup/refresh/realtime — core SessionStore и feature ports;
  ни SessionRepository alias, ни старого UserSessionCleaner в активном graph.
- [ ] CoffeePeekClient: transport/cache/diagnostics/auth infrastructure —
  core/network; feature endpoints/DTO — feature data. AuthService не остаётся
  зависимостью core: refresh business exchange поставляется через порт.
- [ ] Room: driver/schema infrastructure — core/database; настройки/город —
  Settings data, credentials migration — core/session, feature entities —
  своим data. Сохраняем имя БД/таблицы и проверяем migration без data clearing.

### L4. Удалить недостижимые экраны и запасной graph

- [ ] Полный список production, Sandbox, previews, tests и native callers для
  каждого Legacy*Renderer; проверить обе платформы, все deep links и modal entry.
- [x] Удалить legacy renderer defaults из initKoin, заменить обязательными
  актуальными factories. Нельзя незаметно откатиться при пропущенном binding.
  Удалены 21 Legacy*Renderer; CheckInRendererFactories требует все три factories.
- [ ] Удалить старые экраны/ViewModel/старые app business packages и их DI.
- [ ] Полезные unit/contract regressions перенести к новым владельцам; не
  выбрасывать проверки ради сборки и не удалять тесты других авторов вслепую.
- [ ] Удалить совместимые wrappers/aliases только после последнего consumer.

Удаление идёт в том же срезе, где последний consumer заменён и проверен;
не копим бесконечный fallback. Ручная проверка затронутого пути записывается
отдельно от compile/unit доказательств. Непроверенные live-интеграции не
объявляем исправными и не прикрываем ими сохранение активного старого пути.

### L5. Удалить modules и закончить composition boundary

- [ ] После нулевых consumers удалить modules/home-checkin и modules/data.
- [ ] Удалить modules/network/room/domain с учётом фактического графа, не
  разрушая cache/session/БД; убрать module.legacy из Gradle/build-logic.
- [ ] Проверить commonMain/androidMain/iosMain, Swift imports/framework exports,
  Sandbox и тестовые source sets: не осталось старых package references.
- [ ] ComposeApp не содержит feature ViewModels/state/composables/resources;
  root navigation/startup и DI используют только feature/core API.
- [ ] Обновить ARCHITECTURE/AGENTS/README и packaged runtime-class guards,
  чтобы они проверяли новые владельцы, а не требовали удалённые классы.

## Проверка каждого среза

Сохранить семантику/действия/аналитику до изменения; записать затронутые слои и
сценарии. Unit/contract tests по поведению, Android Debug/Sandbox assembly с
DEX guards, iOS compile/framework link и при необходимости полный Xcode build.
Gradle последовательно, --max-workers=6. Новых UI/instrumented/screenshot suites
и test Activities не создаём. UI — paired previews и согласованная ручная проверка.
Никаких установок, действий с аккаунтом или серверных записей без отдельного
согласованного запуска. Пользовательские local pbx changes сохраняем.

Начальный инвентарь — source audit, не новый прогон сборок. Новые результаты
каждого L-среза записываются здесь с source identity, failures/retries и gates.

## Первый срез L1/L4 — 2026-10-25

База 029a572a + текущие изменения. Новых веток/PR пока нет, работа остаётся
продолжением незавершённой миграции #195. План роли кофейни отложен.

- PublicProfileActivityPreview теперь в profile/impl с чистой подготовкой
  ISO-недели/Minsk dates/числа чекинов и thumbnail sources. UI использует
  CoffeePeekActivityWeek, не старый ProfileActivityWeek/CalendarWeek.
- Публичные favorite cards/carousel теперь в profile/impl и используют
  CoffeePeekShopCard/RoasterCard/Card/StackedCardCarousel/LikeBadge. Старые
  CatalogItem/CoffeeShop/PublicAddress для их отрисовки не конструируются.
  Маркеры избранного только читают состояние автора, не пишут состояние зрителя.
- ProfileSlots больше не содержит favoritesPreview/favoriteCard/activityPreview.
  Media crop/fit остаётся узким платформенным портом; перенос самого CpImage и
  оставшейся карточки чекина ещё не выполнен. Paired previews рядом с UI;
  поддерживаемый locale Profile по-прежнему русский.
- Из common graph удалены 21 Legacy*Renderer и значения по умолчанию для
  соответствующих factories. Два non-UI DI-теста сохранены с неотрисовываемыми
  doubles вместо зависимости от прежних экранов.
- Удалены девять больше не вызываемых старых экранов: AppUpdate prompt,
  Auth, Register, PasswordRecovery, Coffee list/detail, Theme, City и UsagePolicy.
  Также удалён недостижимый CoffeeDetailViewModel; из root DI убраны пять
  устаревших ViewModel factories и неиспользуемая recovery repository binding.
  Проверены Kotlin/Swift callers. Studio call hierarchy AppUpdatePrompt
  не нашёл callers; для SettingsScreen нашёл существующий сторонний
  AuthorBlockingFlowScreenshotsTest, поэтому этот экран и чужой тест здесь
  не удалены и UI-suite не запускался. Это отдельный оставшийся consumer,
  его требуется перевести при очистке Settings/author-blocking.

Промежуточная проверка переноса Profile до удаления fallback:
65 unit tests / 7 suites, 0 failures/errors/skips; Android Debug/Sandbox
с runtime-class guards/isolation, iOS Arm64 compile, Simulator framework link
и common Native test compile прошли. Пять новых unit regressions покрывают
часовой пояс, переходы месяца/года, leap day, fallback counts и thumbnails.
Native tests только скомпилированы, UI вручную ещё не проверен. Studio inspection
двух новых UI-файлов не сообщила ошибок, но timedOut=true: полного IDE заключения нет.
Первый прогон после удаления экранов выявил test compile dependency на
formatCoffeePrice из старого CoffeeListScreen. Четыре assertion перенесены к
уже существующему feature/coffee/impl formatter, filter draft assertions
сохранены на месте. Старый UI/helper обратно не добавлялся.
Повторный объединённый прогон прошёл: **138 tests / 17 suites**, 0 failures/errors/skips
(app subset 26 / 5, Profile impl 65 / 7, Coffee impl 47 / 5); Android Debug/Sandbox
и runtime/isolation guards, iOS Arm64 compile, Simulator framework link и common
Native test compile успешны. Неизменённые suites могут быть UP-TO-DATE; Native
tests не исполнялись. Заключительный прогон после снятия неиспользуемых DI
bindings также прошёл с теми же 138 tests / 17 suites. Предыдущий compile failure
сохранён выше; повторный успех его не отменяет. Полный Xcode app build в этом
срезе не запускался. Сохранились прежние preview/expect-actual/bundle-ID warnings.

APK SHA-256 окончательного среза:
Debug `cc7641aa767dac418c89147eb8751c649c945e0bd64f47aedf2f995596dc482b`;
Sandbox `9f0f7e166177ba7843c652fae6076004755433c2ecb0576f8719883294188c7b`.

Ни приложение, ни Sandbox не устанавливались/не запускались. Данные, аккаунты,
backend и пользовательские local pbx изменения не затронуты. Цель полного
legacy exit не объявлена достигнутой: следующие L1–L5 пункты остаются открыты.

## Второй срез L1/L4: карточки чекинов — 2026-10-26

База fb750d83 + этот checkpoint, тот же незавершённый PR #195. Свежие
origin/develop и origin/main проверены fetch: новых отсутствующих в HEAD
коммитов нет. Локальный project.pbxproj пользователя не включается в срез.

Что изменено:

- Удалены app-injected shopTimelineContent/homeTimelineContent и их composition
  locals. History рисует Shop/Home в своём impl прямо из PersonalCheckIn и
  HomeHistoryEntry. Reverse adapters в старые CheckIn/HomeCheckIn удалены.
- ProfileSlots больше не принимает checkInCard. Profile impl владеет полным
  content и block/report menu; никаких Koin вызовов внутри карточки. Формат даты,
  media и переход на ReportCheckIn остаются узкими app ports до L2/L3.
- PublicProfileCheckIn получил явные author/createdAt/scores/drink/home/affinity/
  detail-photo поля. Stateless projection из ещё существующего public feed reader
  сохраняет их за один проход, без хранения старых записей для повторного UI lookup.
  RememberedCheckIns и его storage-only тесты удалены; сценарии сохранности
  представления заменены regressions на self-contained feature rows.
- В core/design-system общие CoffeePeekTimelineContent/Header/Footer,
  CoffeePeekPhotoStrip/Sources и CoffeePeekTimelineRail. Они не знают feature
  моделей/репозиториев/аккаунтов и не выполняют IO. Сохранены header 44 dp,
  single photo 2:1, коллаж 4:3, +N, footer с названием напитка справа и read-only
  affinity без ripple. CoffeePeekDrinkLabel получил Compact вариант 14 dp;
  исходный Regular сохраняет прежние размеры. Paired previews colocated.
- Photo sources snapshot/alignment общий: отсутствие варианта и явный blank
  различаются; в list доступен preview → detail, viewer получает aligned
  originals. Невалидные/неизвестные album indices отбрасываются. Личная история
  открывает фото отдельным MVI intent, с source-prefixed key, session envelope и
  существующей app lease проверкой. Нажатие на фото не открывает detail sheet.
- Сохранены visitedAt/preparedAt timestamps для личной истории и createdAt для
  публичного card header, custom/localized drink preference, Home recipe,
  visibility label и отсутствие придуманных нулевых category scores.
- Гостю и владельцу карточки Profile не предлагается чужой block/report menu.
  Signed-in state отслеживает смену аккаунта, в том числе logout; существующие
  write-side guards остаются у владельцев действий.
- Удалены больше не вызываемые старые ProfileScreen, ProfileCheckInsScreen и
  HistoryTimelineCheckInCard. Их Kotlin/Swift callers проверены; Studio call
  hierarchy подтверждал единственный старый caller последней карточки.
  У существующего стороннего CheckInPresentationTest заменены только import и
  вызов TimelineRail на перенесённый DS компонент. Новых UI-тестов/hosts нет;
  существующий UI-suite не запускался. Старый публичный Profile UI ещё нужен
  другим сторонним tests; его удаление требует отдельного переноса consumers.

Проверки:

- Production compile Android DirectDebug и iOS Arm64 прошёл до финального
  удаления освободившихся экранов. Первый unit run остановился на отсутствии
  HomeHistoryPhoto import в новом тесте; import исправлен, повторный набор прошёл.
- Финальный unit/contract набор: **263 tests / 42 suites**, 0 failures/errors/skips:
  design-system 50 / 14; Profile impl 66 / 7; History impl 82 / 8;
  app subset 65 / 13. Неизменённые tasks могут быть UP-TO-DATE; эти counts не
  суммируются с первым checkpoint. Перенесены полезные adapter regressions:
  даты, recipe, thumbnail/detail/original alignment; добавлены unit проверки
  источников фото, их snapshot, двух album с одинаковым id, неверных индексов,
  session delivery и видимости действий после logout. Unit-only, без сети.
- Android DirectDebug/Sandbox assembly прошли, включая packaged runtime-class
  guards и Sandbox isolation. iOS Arm64 production compile, Simulator Debug
  framework link и app common Native test compile прошли. Дополнительно прошла
  compileTestKotlinIosSimulatorArm64 для design-system, Profile impl и History
  impl. Native tests не исполнялись. Gradle последовательно, --max-workers=6.
- Studio MCP get_file_problems завершился без ошибок для CoffeePeekPhotoStrip,
  PublicProfileCheckInCard и HistoryHomeTimelineCard. Это не preview rendering.
- APK SHA-256: Debug `a009257ed3157660784ca79ef1e31394a8de3b1e71fa344c525bde2dbb3bf79d`;
  Sandbox `1e8c8406c9d110f00395a7ec292e910023047a816ed5f7c14e47c461679ae753`.

Полный Xcode build и ручная визуальная приёмка этого среза пока не выполнены.
Ничего не устанавливалось/не запускалось, реальные аккаунты и данные не менялись.
Live backend/OAuth/push/analytics acceptance этим не подтверждены.

Следующий остаток: активный public profile data reader/favorite enrichment и
прочие app data adapters, затем media infrastructure. Старые экраны/VM/тестовые
потребители удаляются по последнему consumer; modules/ ещё не готовы к удалению.

## Третий срез L3: публичные чекины профиля — 2026-10-26

База `790834bc` + этот checkpoint, тот же незавершённый PR #195 → develop.
Fetch origin/develop и origin/main не обнаружил отсутствующих в HEAD коммитов.
Пользовательский local project.pbxproj сохранён отдельно и не входит в срез.

Владелец и границы:

- `community/api` публикует узкий read-only `PublicAuthorPosts` с отдельной
  проекцией данных; чужие domain models, DTO, transport и mutation API через
  неё не передаются. `community/data` адаптирует уже существующий repository,
  повторно используя feed backend, URL/variant mapper и ошибки пагинации.
  Новый endpoint, HTTP client, cache или Gradle module не создаётся.
- `profile/data` реализует существующий `PublicCheckInsPort` и владеет
  фильтрацией/преобразованием в self-contained `PublicProfileCheckIn`.
  Зависимости добавлены на собственный api и `community/api`; импортов чужих
  domain/data/impl нет. Community не зависит от Profile, граф ацикличен.
- Android/iOS composition получает готовые `PublicProfileData` и
  `PublicCheckInsPort`, соединяет factories и передаёт их feature entry.
  Удалены `AppPublicProfileRepository`, `AppPublicCheckIns`,
  `PublicCheckInProjection` и root binding старого PublicProfileViewModel.
  Прежняя UI/model/repository-interface поверхность, ещё используемая чужими
  тестами, не удалялась; она не подключается к активным feature entries.
- Единственный оставшийся класс бывшего app repository-файла выделен в
  `LegacyPublicProfileFavoriteDetails.kt`: это прежнее enrichment через старые
  каталоги, без новой логики или алиаса на новый reader. Его regression сохранён.

Сохраняемое поведение и unit evidence:

- Чтение только `/api/v1/feed`, authorSlug, pageSize=20, opaque cursor и no-store;
  private/mine endpoints для публичного профиля не вызываются, включая свой.
  Профиль дополнительно отбрасывает чужого/неизвестного автора и всё, кроме
  Public + Approved. Неизвестные значения состояния остаются закрытыми.
  Полностью отфильтрованная страница сохраняет nextCursor.
- Сохранены порядок публикации, visited/prepared/created даты, Home kind/recipe,
  имена напитков, category scores, likes, author/avatar и original/thumb/detail
  alignment. Явный blank thumbnail не заменяется оригиналом; отсутствие variants
  и detail fallback по-прежнему обрабатывает существующий community mapper.
- Ошибка reader передаётся без подмены, cursor restart flag сохраняется,
  CancellationException пробрасывается. Existing profile/session/visibility
  guards не менялись; feature rows не нуждаются в обратном legacy lookup.
- Старые projection/reader assertions перенесены к реальным новым владельцам.
  Добавлен non-UI app contract через фактические community/profile factories и
  локальный Sandbox backend: свой профиль, другой автор и пустой результат.
  Сервер/аккаунт/аналитика не используются. Это не ручной запуск Sandbox.

Проверки:

- Первый запуск Gradle в ограниченном окружении остановился до задач на доступе
  к wrapper lock в ~/.gradle. Повтор с доступом к локальному Gradle cache дошёл
  до новых unit tests и выявил пропущенный начальный cursor; default `null`
  добавлен в существующий PublicCheckInsPort. Исходные логи сохранены локально.
- Финальный объединённый прогон успешен: **188 tests / 23 suites**,
  0 failures/errors/skips: Community data 20 / 3; Profile data 18 / 2;
  Profile impl 66 / 7; app subset 84 / 11. Неизменённые tasks могут быть
  UP-TO-DATE; результаты не суммируются с прошлым checkpoint.
- Android DirectDebug и DirectSandbox assembly успешны, включая оба packaged
  DEX runtime-class guards и Sandbox isolation. Ничего не устанавливалось.
- iOS Arm64 production compile и Simulator Debug framework link прошли.
  Community data, Profile data и app common tests для iOS Simulator
  скомпилированы; Native tests не исполнялись. Gradle последовательно,
  --max-workers=6. Полный Xcode app build не запускался.
- Studio inspection нового PublicAuthorPosts API завершился без ошибок.
  Диагностика Profile data пока сообщает unresolved community import после
  добавления Gradle dependencies: IDE graph требует синхронизации; это не
  объявляется чистой IDE-проверкой. CLI Gradle компилирует этот код на обеих
  платформах. Прежние preview/expect-actual/test opt-in warnings сохранены.
- APK SHA-256: Debug `8955ea0e7e5ca77d149e6f7529bfd98dcfcde035e739191c7fd6a5261110fb1c`;
  Sandbox `ed594cb7d5cfeb5a90f546cb91f5befbc2b0e4a802fc49bceb0c4d6879e93265`.

UI, analytics ownership, session policy и platform media в этом срезе не
менялись. Ручная visual/live-backend/OAuth/push/analytics приёмка не проводилась;
новых автоматизированных UI/инструментальных tests/hosts нет.
Следующий остаток Profile: legacy favorite enrichment и own-account reader;
затем media ports и недостижимые legacy UI/test consumers. Modules пока не удаляем.

## Четвёртый срез L3: избранное и собственный аккаунт — 2026-10-26

База `6607ce83` + этот checkpoint, тот же открытый PR #195 → develop.
Fetch origin/develop и origin/main: отсутствующих в HEAD коммитов нет.
Локальный пользовательский project.pbxproj остаётся вне checkpoint (48+/41−).

Public favorites:

- Shop/Roaster api публикуют read-only проекции карточек; только их api доступны
  Profile data, без импортов чужих domain/data/impl. Shop data переиспользует
  существующий details backend, не запрашивает меню и не отдаёт personal rows
  или favorite state. Сохранены card → full cover, tags + shopTags, порядок,
  distinct, fallback brewMethods/beans и ограничение трёх тегов.
- Roaster data проецирует существующий repository, с тем же memory/persistent
  catalog cache и ключом; второго каталога/кэша нет. Сохранены фото, описания,
  теги и все три счётчика. Root соединяет factories, а Profile data переводит
  public projections в собственные favorite details. Новый Gradle module не нужен.
- Удалён LegacyPublicProfileFavoriteDetails из composeApp. Неуспешное optional
  enrichment сохраняет исходный favorite, отмена пробрасывается. Серверное
  избранное зрителя не читается, не записывается и не очищается этими readers.

Own account:

- `createProfileAccountData` в Profile data возвращает уже существующие
  ProfileAccountPort/ProfileEditPort/AccountDeletionPort, связанные одним
  внутренним repository/cache. HttpClient, отдельный credential-free upload
  client, core SessionReader и app lifetime scope предоставляет composition.
  Domain не получает platform/network/DI зависимости.
- Владелец кэша — active user ID, при его отсутствии token (прежний контракт
  порта). Наблюдаемый logout/replacement увеличивает generation и очищает кэш;
  обычное обновление токена того же ID сохраняет его. Peek проверяется и перед
  выдачей cached profile, поэтому задержка observer не выдаёт прежнего владельца.
  Поздние reads/writes отвергаются; read, начатый до удачного edit, не откатывает
  изменённый кэш. Profile fields/canonical address и forceRefresh сохранены.
- Profile, public identity, редактирование и deletion получают новые ports.
  Shop viewer и Community ownAuthorSlug подключены к тому же кэшу через
  Profile api: Shop использует обычное чтение, Community сохраняет refresh.
  Сохраняется fallback определения владельца чекинов при недоступном профиле;
  guest Shop не читает профиль.
- Username/about PATCH обновляют этот общий кэш. Сохранены отдельные причины
  username failures и retry instant; server prose не уходит в username UI.
  Avatar: `/api/Photos/avatar` → проверка public presigned URL → PUT байтов через
  отдельный upload client → PATCH `{uploadedPhoto: ...}` → refresh профиля.
  Владелец проверяется между шагами: смена аккаунта/отмена/ошибка PUT не дают
  отправить avatar PATCH следующему аккаунту. Нового общего media сервиса нет;
  перенос загрузчика/viewer остаётся L2, endpoint аватара принадлежит Profile.
- Deletion по-прежнему лишь создаёт e-mail request и не завершает сессию.
  Existing 404/not-found — null, server retry instant и его PascalCase alias
  сохранены. DTO/запросы скрыты в data. UI confirmation/session-end routing и
  порядок logout не изменены.
- Общий core/network ApiEnvelope получил decode-only aliases IsSuccess/Message/
  Data, которые принимал старый API. Нижний регистр остаётся основным; отдельный
  второй envelope/parser для Profile не создавался. Compatibility и error prose
  проверяются unit tests, включая реальную цепочку загрузки OwnProfile.

Снятие legacy:

- Удалены AppProfileAccountPort, AppProfileEditPort, AppAccountDeletionPort и
  app username failure mapper. Убраны UserRepository binding из dataModule и
  пять недостижимых VM bindings (Profile/Edit/Deletion/ShopDetail/старый Community).
  Их старые Kotlin/Swift screen callers проверены: активные routes идут через
  обязательные feature renderers. Удалён DeleteAccountPendingScreen без callers.
- UserRepositoryImpl ещё нужен прежнему UserRepositorySessionTest; старые
  интерфейс/модели/VM также используются оставшимися tests/legacy UI. Они не
  подключены к новой цепочке и не объявляются полностью удалёнными. Их тестовые
  потребители и useful regressions требуют переноса в L4; modules ещё остаются.
- Unit regressions старых app ports/username mapper/favorite adapter перенесены
  к feature data, с дополнениями для реальных HTTP contracts и owner races.
  Остальные app logout/visibility/analytics и Community regressions сохранены;
  чужие UI/instrumented suites не удалялись и не запускались.

Проверки:

- Первый Android/iOS production compile прошёл (45s). Первый unit/contract набор
  — **259 tests / 42 suites**, 0 failures/errors/skips. Первая упаковка Android
  и iOS compilation/link также прошли (2m). Сверка legacy response contract после
  этого обнаружила поддержку PascalCase envelope; aliases и regressions добавлены
  до финального прогона, промежуточная сборка не является окончательным артефактом.
- Итоговый объединённый прогон прошёл (3m06s): **302 tests / 50 suites**,
  0 failures/errors/skips: core/network 40 / 8; Profile data 42 / 7; Shop data
  43 / 14; Roaster data 14 / 1; Profile impl 66 / 7; app subset 97 / 13.
  Counts не суммируются между прогонами; неизменённые tasks могут быть UP-TO-DATE.
- Оба Android APK assembled с packaged DEX runtime-class guards и Sandbox
  isolation. iOS Arm64 production compile и Debug Simulator framework link
  успешны. Общие tests core/network, Profile/Shop/Roaster data и app для iOS
  Simulator скомпилированы, Native tests не исполнялись. Полного Xcode app build
  не было. Gradle последовательно, --max-workers=6.
- Unit evidence: field/address mapping и один cache, cache/refresh/account keys,
  смена пользователя/повторный вход/rotation, stale reads/writes, read-before-edit,
  cancellation, deletion contracts, avatar metadata/байты/URL/header isolation и
  запрет следующего шага после смены аккаунта, typed username errors, optional
  enrichment, card cover/tag parity, Shop ownership fallback и guest guard.
  App contracts вызывают фактические factories через локальный fake backend;
  это не live backend или ручная UI-приёмка.
- Studio inspections двух новых Shop/Roaster API-файлов завершились без ошибок.
  Profile backend inspection пока показывает unresolved UsernameChangeFailure
  после новых Gradle edges; IDE graph требует sync. Полной чистой IDE-диагностики
  не заявляем; Android/Native CLI compile успешны. Прежние preview/expect-actual/
  test opt-in warnings остались.
- Финальные APK SHA-256: Debug `e356d3fcc557ece73e0dc8c482aad49e9c12884d0e2e95cbbae0d63fa1f92f75`;
  Sandbox `db62e8fb9fe0825885b921fb921c26a2801cc3077a859bc80f5dc77b390c7917`.

Ничего не устанавливалось и не запускалось на устройствах; production SDK/events,
аккаунты, локальные данные, камера и backend writes не затронуты. Никаких новых
UI tests/hosts. Визуальные компоненты, тексты экранов и previews не менялись.
Ручные UI/OAuth/push/analytics/persistence gates остаются непроверенными.
Дальше — L2 media и оставшиеся app business slots, затем L4 consumers/старые
реализации и общий transport/session/storage остаток L3/L5. Полный exit ещё не завершён.

## Пятый срез L2: общий media pipeline — 2026-10-26

База `7a7b0283` + этот checkpoint, PR #195 → develop остаётся открытым.
Повторный fetch origin/develop и origin/main по запросу владельца: оба refs
уже являются предками HEAD, отсутствующих коммитов и merge conflicts нет.
Дополнительное слияние не нужно. Пользовательский project.pbxproj (48+/41−)
сохранён вне среза. Это один инфраструктурный перенос: число затронутых путей
включает moves и замену импортов у существующих consumers на обеих платформах.

Владение и удаление старых реализаций:

- Новый `core/media` владеет transport, public/private byte/painter caches,
  сроками разрешений, session/visibility invalidation, platform decoding,
  URL resolution и ограниченным prefetch. Он зависит от core/session,
  Ktor/coroutines, Compose/Kamel/lifecycle и платформенных codecs, но не от
  application, features, legacy, design-system или Koin. Отдельный Gradle
  boundary запрещает прежнюю обратную зависимость общих картинок от приложения;
  публичная поверхность/consumers/риски описаны в core/media/README.md.
- В core/media перенесены Fetcher, ImageFetcherFactory, PlatformImageConfig,
  protected image pipeline, iOS ImageIO/Skia decoder и Android bitmap codec.
  CustomUrl/Fetcher переименованы в ImageUrl/Fetcher, BitmapUtil — в
  AndroidImageCodec. Не осталось старых alias, wrapper или второй копии loader.
  Feature-specific photo upload endpoints этим переносом не менялись.
- `CoffeePeekImage`, `CoffeePeekPhotoViewerImages` и единый
  `CoffeePeekPhotoViewer` принадлежат design-system. Старые CpImage,
  FullScreenImageDialog, PhotoViewerImages и PlatformFullScreenDialog удалены.
  Старый простой DS viewer заменён общей реализацией: attachments сохраняют
  свой photo slot и используют те же gestures/controls без фиктивных HTTP URLs.
  Общий full-screen dialog также в design-system; paired light/dark previews
  находятся рядом с компонентами, тексты loading/close принадлежат DS resources.
- Root DI предоставляет существующие HTTP clients, SessionReader, API origin
  и visibility flow. AppContent устанавливает environment и platform hooks
  аналитического occlusion/reduce motion. SDK и Koin не попали в core/media.
  Sandbox использует прежний локальный fake client, без default real HTTP
  fetcher. Production и native callers переключены на новые компоненты.

Сохраняемые контракты:

- Private grant/ETag/304/TTL и cache bounds, смена владельца и mutation/block
  invalidation остаются в одном pipeline. Истёкший или отозванный painter не
  выдаётся; память сохранённого composable resource остаётся наблюдаемой,
  включая результат decode после первого кадра. Cancellation не подавляется.
- Preview-first, foreground и 50% visibility gate, delayed detail upgrade,
  приоритет открытой галереи и bounded prefetch сохранены. До шести кандидатов,
  глобально два запроса, без speculative private originals. URL storage keys
  по-прежнему идут через /api/file, API photo routes — через API origin.
- У viewer сохранены preview/original alignment, текущая/видимая страница,
  neighboring previews без offscreen painter retention, original-on-zoom,
  pinch/pan/double-tap, swipe-down dismiss, safeDrawing и reduce-motion dismissal.
  Изменения gallery UI требуют ручной визуальной приёмки; compile/unit evidence
  не доказывает поведение жестов или platform dialog rendering.
- Android alpha/EXIF и bounded decode, iOS ImageIO downsampling/alpha/Skia и
  lifetime возвращаемого byte channel перенесены с прежними реализациями.
  Unit/native regressions переехали к владельцам, вместо зависимости от app
  Constants используют явную тестовую конфигурацию. Новых UI cases/hosts нет.

Проверки и ограничения:

- Первый compile остановился на пропущенном Resource import в перенесённом
  ImagePrefetch. Import исправлен; успешный повтор (2m15s) выполнил 125 unit
  tests / 25 suites: media 68 / 10, design-system 57 / 15; 0 failures/errors/
  skips. Android app production compile и iOS Arm64 compile прошли.
- Финальный объединённый прогон успешен (6m26s): **161 tests / 29 suites**,
  0 failures/errors/skips: media 68 / 10, design-system 57 / 15, app subset
  36 / 4 (image projection, photo picker contract, renderer factories, local
  Sandbox backend). Неизменённые unit tasks UP-TO-DATE; counts не суммируются
  с первым прогоном. Gradle последовательно, --max-workers=6.
- Android DirectDebug/DirectSandbox assembled, оба packaged DEX runtime-class
  guards и Sandbox isolation прошли. Guards дополнены новыми media/DS classes.
  Debug Simulator framework linked; common Native tests media, design-system
  и app скомпилированы, но не исполнялись. Прежние preview/expect-actual/test
  opt-in и bundle-ID inference warnings сохранены, не скрыты.
- APK SHA-256: Debug `ed872e2b64543ea344bc295836a7307c8eda0314ee2326e769377d4e9774ac22`;
  Sandbox `fd02b7b297b0c3d09b121e9e89837690788d92710fc2ee81f46627dd99f9866c`.
- Чужие существующие UI suites сохранены; исправлены ссылки на перемещённые
  символы. Для их доступа к прежним internal symbols добавлена только compile
  friend metadata, без расширения production API. Автоматическая проверка
  разрешений отклонила запрос на compilation instrumentation/UI sources;
  отвергнутая задача не выполнялась и не повторялась. Metadata/source
  compatibility этих suites остаётся непроверенной, UI tests не запускались.
- Studio inspection CoffeePeekImage показала unresolved imports нового media
  module до Gradle sync IDE. Чистая IDE-диагностика/preview rendering не заявлены;
  CLI evidence и IDE sync — разные проверки.
- Установок и запусков на устройстве, полного Xcode app build, Native test
  execution, реальных account/backend writes, камеры и отправки analytics не
  было. Ручные темы/шрифты/окна/accessibility/back/cancel/private-photo gates
  открыты. Настройки и данные телефона не менялись.

Следующий срез: оставшиеся активные app business slots, начиная с author-blocking
screen/host; затем shared platform UI/services и L3 transport/session/storage.
Modules и недостижимые старые screens/VM/test consumers ещё не готовы к полному
удалению. Media code ownership завершён, полная миграция — нет.

## Шестой срез L1: Author blocking UI — 2026-10-26

База `e067afbd`, тот же незавершённый PR #195 → develop. Нового business feature
или Gradle module нет: уже существующий Author-blocking impl получил свой общий
Compose UI и ресурсы, api — минимальный AuthorBlockingEntry. Composition передаёт
существующий singleton presenter, callbacks back/login/register и clearance;
нового repository, HTTP client, mutation scope или DI внутри UI нет.

Перенос и поведение:

- BlockedAuthorsScreen/Content и AuthorBlockingHost/Content находятся в
  impl/ui/compose; строки — в default Russian resources. Других переводов
  прежнего app UI не было; переводы не заявляются. Список, guest/login/register,
  loading/error/retry/empty, avatar/name/slug, unblock и working guards сохранены.
  Превью парные light/dark, colocated, с фиктивными состояниями и без Koin/IO.
- Общие ActionSheet/TextAction/OperationStatus/EmptyState перенесены в DS с
  прежними границами, cancel group, pressed opacity без ripple и nonmodal
  progress. CpActionSheet/CpTextAction/CpOperationStatus/CpEmptyState удалены,
  последние production callers переключены. Avatar выделен из существующей
  timeline геометрии и используется также списком и прежним ReviewAvatar;
  letter fallback/44dp/цвета сохранены. Бизнес-тексты остаются в feature.
- iOS presenter hooks адаптируют прежние NativeActionSheet/OperationStatus;
  native platform реализации пока остаются открытым L2, не считаются удалёнными.
  Сохраняется dismissBeforeAction=false, destructive block, ordinary unblock,
  отдельная отмена, native error/retry alert и доступный progress. Android
  использует тот же DS renderer. Дублирующей feature-native реализации нет.
- Оба root navigation пути и Android Sandbox вызывают api entry. Обнаружено,
  что прежний Host существовал только в App(), а native controllers используют
  AppContent без него. NativeScreenController теперь устанавливает entry.Host
  только когда controller видим; скрытые tab/back-stack controllers не должны
  одновременно показывать одну и ту же глобальную confirmation. Вложенные
  native sheets не получают ещё один Host через AppContent.
- Существующие события/маршрутная аналитика проверены: у блокировок нет отдельных
  business events, BlockedAuthors по-прежнему имеет page=other (existing unit
  contract). Root screenShown/visit ownership и consent/session SDK не менялись;
  feature не получила второй analytics service и не отправляла тестовые events.
- В presenter исправлены две границы account смены: confirmation проверяет
  владельца перед запуском и перед фактическим вызовом repository; первый
  account emission больше не теряет replacement через drop(1). Отмена старой
  mutation не может закрыть новый prompt. Session/visibility repository guards,
  endpoints/DTO, reconciliation при timeout и cache invalidation не менялись.
  Четыре unit regression покрывают до/после confirm, задержанный observer и
  задержанный cancellation cleanup. Не создаётся автоматическая mutation после login.

Проверки до синхронизации develop:

- Первый compile завершился тремя ошибками переноса: lifecycle-effect argument,
  nullable smart cast из api и loader accessibility argument. Все исправлены;
  повтор Android/iOS production compile и unit tests успешен (58s).
- Объединённый прогон (3m11s): **110 tests / 21 suites**, 0 failures/errors/skips:
  Author-blocking impl 8 / 1, data 7 / 1, DS 57 / 15, app subset 38 / 4.
  Неизменённые tasks могут быть UP-TO-DATE; результаты не суммируются.
- Debug/Sandbox assembly, оба packaged DEX guards с AuthorBlockingEntry и
  factory class, Sandbox isolation прошли. iOS Arm64 production compile,
  Debug Simulator framework link и common Native test compile impl/data/app
  успешны. Native tests не исполнялись; Gradle последовательно, --max-workers=6.
- Studio inspection api entry пока видит unresolved Compose imports после
  изменения module graph — требуется IDE Gradle sync. CLI compiler прошёл на
  обеих платформах; чистые IDE/preview checks не заявляются.
- Существующие чужие UI sources сохранены, только ссылки направлены на api/DS.
  UI/instrumentation/screenshot tests не создавались, не компилировались и не
  запускались. Нового test Activity/APK infrastructure нет. Установок, запусков,
  реальных account writes, camera/backend mutations и production analytics нет.
- Полный Xcode app build, ручная native activation/one-host проверка, обе темы,
  шрифты/окна/accessibility, cancel/retry/foreground и работа с реальной сессией
  остаются отдельными gates. Перенос UI ownership не объявляется этой приёмкой.

Во время проверки develop обновился до `381c6cc2`: merge #199 с map density и
обновление discovery search contract. Этот срез сохраняется отдельно перед
слиянием; прежние APK не являются артефактами объединённой ветки. Результат
слияния и повторные проверки фиксируются следующим checkpoint ниже.
Пользовательский project.pbxproj сохранён вне среза (48+/41−).

## Синхронизация develop после шестого среза — 2026-10-26

Author-blocking checkpoint: `332420b8`; merge: `d791d5e1`, parent develop
`381c6cc2`. Включены map density/animations/status-bar expansion из PR #199
и обновление discovery search contract. Main `16724b7e` уже в истории ветки.

Разрешены два содержательных конфликта:

- DiscoveryMap API сохраняет canvasSize для платформенного размера iOS map
  container и добавляет clusters из develop. Оба поля остаются отдельными.
- Общий feature MapContent передаёт и canvasSize, и canvasClusters(isPreview),
  не возвращаясь к Android-only экрану. Composition адаптирует clusters в
  прежний native canvas; iOS renderer и свойственная ему политика размеров
  сохранены. Изменения root nav/status-bar expansion объединены автоматически
  с новым AuthorBlockingScreenRoute; ни один маршрут не восстановлен в legacy.

Новый MapContentMapper в legacy data и его тест пришли из develop для
сохранявшегося на том родительском checkpoint iOS consumer. В нашей ветке iOS
по-прежнему использует migrated entries. Это не разрешение добавлять новые
business implementations в modules; удаление недостижимых readers остаётся L4.

Повторная проверка объединённого дерева `d791d5e1` успешна (3m04s):

- **230 unit/contract tests / 37 suites**, 0 failures/errors/skips:
  Author-blocking impl 8 / 1 и data 7 / 1; DS 57 / 15; Discovery data 19 / 1
  и impl 63 / 6; retained MapContentMapper 2 / 1; app subset 74 / 12.
  App включает session/author bridge, page mapping, native action selection,
  map clustering/transitions/native payload и локальный Sandbox backend.
  Неизменённые tasks могут быть UP-TO-DATE; это один набор, не сумма запусков.
- DirectDebug/DirectSandbox assembly, оба packaged DEX runtime-class guards
  и Sandbox isolation успешны. iOS Arm64 production compile, Debug Simulator
  framework link и common Native test compilation Author-blocking/Discovery
  data/impl и app успешны. Native tests не исполнялись; Gradle последовательно,
  --max-workers=6. После слияния нет новых ошибок compilation/test retries.
- APK SHA-256: Debug `49887ebb5acd7aeffb6e195b86f73c7f52aeae0c728fa8405aa8a8a78105189d`;
  Sandbox `919ba78d8602fd1b6422954a874fc2d1dc8850e9e8fd7613f94fb10d5d23dcfe`.
- `git diff --check` проходит, unmerged paths нет; fetched develop целиком
  является предком HEAD. Пользовательский Xcode project diff остаётся 48+/41−
  и не включён в коммиты. Прежние preview/expect-actual/opt-in/bundle-ID
  warnings сохранены. UI suites и новые test hosts не запускались/не создавались.

Ручные device/native Swift runtime gates map checkpoint не переисполнялись и
не объявляются закрытыми чужой исторической проверкой Pixel 9. Полного Xcode
app build не было: Kotlin framework link не компилирует Swift renderer.
Установок и действий с реальными данными не было.

Следующий остаток L1: Discovery/catalog/create slots и root tab содержимое.
Затем — оставшиеся общие platform services, L3 transport/session/storage и
удаление последних legacy consumers L4. Полный legacy exit ещё не завершён.

## Седьмой срез L1/L2: визуальные слоты Discovery — 2026-10-26

Дочерняя ветка `codex/migration-discovery-ui-ownership` от `7f78ea21`, parent
checkpoint #195 → develop, draft PR #201. Срез блокировок завершён в родителе, поэтому новая
работа не добавляется в его PR. Сверка fetched main/develop перед переносом:
отсутствующих в parent коммитов нет; develop `381c6cc2`, main `16724b7e`.
Нового Gradle module или business feature нет. Пользовательский локальный
project.pbxproj остаётся вне коммитов (48+/41−).

Владение:

- DiscoveryHost удаляет 13 app-owned visual/prefetch slots. В нём остаются
  exposure, haptic, retained lifecycle, permission effect, reduce-motion и
  native map canvas. Root discoveryHost больше не рисует ошибки/фильтры/
  иконки/аватары/картинки и не создаёт legacy CatalogItem для фильтра.
- Discovery impl сам использует shared media prefetch, branded image/placeholder,
  service error и существующую suggestion card. Roaster avatar, map shop card,
  logo и brew/tag catalog mapping принадлежат ui/compose/component; glyphs и
  artwork — design-system. Сопоставление backend names/slugs не попало в core.
- FilterSheet получает stateless Content/Header/Choices и paired previews;
  цена/лейблы — в DiscoveryPriceSection и feature resources. Сохранены draft,
  reset с городом, close без apply, apply затем close, выбранные строки за
  пределами первых пяти, selected counts и ограничение двух brew chips.
- В DS расширен существующий CoffeePeekImage слотом placeholderContent;
  явный error drawable сохраняет приоритет. CoffeePeekBrandedImage использует
  тот же progressive/private pipeline, без второго resource loader или cache.
  App CoffeeShopImage/CoffeeShopPlaceholderImage удалены, все Kotlin consumers
  обеих платформ переключены. Бренд/контраст/18sp и compact 5/6/7sp сохранены.
  Переданная feature accessibility подпись сохранена; default fallback labels
  теперь общие «Фото отсутствует»/«Загрузка фото…», вместо привязки к кофейне.
- LoadErrorContent удалён из app, общий CoffeePeekLoadErrorContent сохраняет
  mascot 120dp, wording, отступы и компактную retry button. CoffeePeekButton
  получил необходимые fillWidth/minHeight параметры с прежними defaults для
  остальных consumers. Обе общие mascot PNG физически перенесены в DS,
  байты сверены с parent (идентичны), имена ресурсов/происхождение сохранены.
- CoffeePeekPriceLevelSlider в DS владеет только визуальным выбором null/1–3,
  белым thumb 28dp, track 4dp и BYN stops с прежним масштабом 0.9. Подписи
  «До/Около/Больше 8 BYN» принадлежат Discovery. Domain level 4 не удалён;
  прежние clamp UI до 3 и hint=«Любая» для 4 сохранены. Старый app PriceBeanSlider
  теперь делегирует визуал этому DS компоненту: у него ещё есть реальные
  Contributions/add-shop/legacy filter consumers, поэтому он пока остаётся.
  Остальные legacy price/name/tag helpers не объявляются удалёнными.
- Geometry map card остаётся 72dp photo/7sp placeholder, прежние title/rating/
  hours/distance и shadow. Общие карты/шаблоны уже были в DS и переиспользуются,
  нового card builder или transport не создаётся. Map bounds/clusters/canvasSize,
  location permission и навигация не заменялись.

Контракты и проверки:

- Инвентаризированы Search/Filters/ShopCatalog/Favorite/MapPreview/MapFull
  exposure: прежние page/catalog/context strings и dynamic favorite page
  сохранены. Haptic остаётся перед favorite action; consent/session SDK и
  события ViewModel не менялись. Новых production analytics events не было.
- Новые/существенно изменённые функции проверены по review limits: composable
  bodies не длиннее 40, ordinary helpers не длиннее 20 строк. Без нового lint
  или build gate. Colocated paired multiplatform previews используют fake state
  и локальные картинки; UI-тестов/hosts нет.
- Первый compile остановился на отсутствовавшем CoffeePeekIcons.Sun. Добавлен
  прежний Phosphor Sun, ThemeLight использует тот же glyph. Повтор (1m12s)
  Android/iOS production compile и unit tests успешен: Discovery impl, DS/media.
- Studio inspection CoffeePeekPriceLevelSlider завершилась без ошибок; это
  анализ одного файла, не render previews и не полная IDE приёмка.
- Финальный прогон (4m02s), source `7f78ea21` + этот срез: **300 unit/contract
  tests / 47 suites**, 0 failures/errors/skips. Discovery domain 12 / 1,
  data 19 / 1, impl 63 / 6; DS 57 / 15; media 68 / 10; app subset 81 / 14
  (discovery/map/renderers/photo projection/pickers и локальный Sandbox backend).
  Неизменённые tasks могут быть UP-TO-DATE; counts не суммируются с повтором.
- DirectDebug и DirectSandbox assembled, оба packaged DEX runtime-class guards
  и Sandbox isolation прошли. Guards дополнены BrandedImage/LoadErrorContent/
  PriceLevelSlider classes. iOS Arm64 production compile, Debug Simulator
  framework link и common Native test compilation Discovery impl/DS/media/app
  успешны. Native tests не исполнялись. Gradle последовательно, --max-workers=6.
- APK SHA-256: Debug `880ecfbf87d5792a0a9d30700833c9a4a628631571eace420f0a89b3a57dc357`;
  Sandbox `56192d2e46c170f3ea99ee04f68b11dd84a79fc72375d8d9633ec0e2a2e261fa`.
  Новых UI/instrumentation/screenshot tests/hosts нет, чужие tests не удалялись
  и не запускались. `git diff --check` проходит. Прежние preview/expect-actual/
  opt-in/bundle-ID warnings сохранены; compiler pass не называется UI приёмкой.

Остаток после этого среза:

- Native map/platform effects/retained host и exposure service ещё app-owned;
  map bridge всё ещё использует legacy map models до отдельного переноса.
- На момент этого среза Roaster search/suggestion и POST adaptation ещё были
  остатком; оба закрыты последующей синхронизацией parent и восьмым срезом ниже.
  Contributions editor slots, root create/tab и legacy data readers остаются.
  HTTP/filter semantics седьмой UI-срез не менял; последующий единый POST
  пришёл из develop и проверен на обеих ветках стека. Passing fake tests
  по-прежнему не подтверждают live backend acceptance.
- Ручная Android/iOS visual/accessibility/keyboard/modal/gesture приёмка,
  live backend, Native test execution и полный Xcode app build остаются gates.
  Ничего не устанавливалось/не запускалось, реальные аккаунты/данные/камера
  не затронуты. Полный legacy exit не завершён.

## Синхронизация develop: единый POST поиска — 2026-10-26

Parent checkpoint #195 обновлён первым: merge `5eefd288` включает develop
`21d9ee9a` (POST migration + quick-filter reload + verification). После проверки
этот parent передаётся в дочерний #201; новая реализация поиска поверх готового
исправления не создаётся. Main `16724b7e` уже в истории.

- Обычный и nearby поиск используют POST /api/v1/coffee-shops/search; без origin
  radius/sort опущены, с origin — distance_asc и 3 км. Обновлены DTO/mapper,
  строковые type/price значения, quick-filter reload, карта без координат в
  search row и локальные Sandbox fixtures. Removed GET/старый discovery POST
  не восстанавливаются. Это код develop, а не новая feature в legacy modules.
- Автоматическое слияние не оставило конфликтов, но потеряло Coordinates import
  в ранее перенесённом DiscoveryAdapters: первый прогон упал на compile Debug/
  Sandbox. Import восстановлен в composition, domain зависимости не расширены.
  Исходный failure сохранён, не замещён записью успешного повтора.
- Повтор (1m06s): **178 unit/contract tests / 23 suites**, 0 failures/errors/skips:
  Discovery data 19 / 1, impl 66 / 6; legacy network contracts 8 / 2, data
  mapper contracts 4 / 2; app subset 81 / 12. Неизменённые tasks могут быть
  UP-TO-DATE. Оба Android APK и runtime-class guards/Sandbox isolation прошли;
  iOS Arm64 production compile прошёл. Simulator link на parent не повторялся.
- Parent APK SHA-256: Debug `c283eae1b153d2cbb4e7dee2dcfa61904cdf5208db8faf8ff0943037184ad427`;
  Sandbox `70e1059da81792ac013c680a0d2f12ac7f6049dfbd46ec46a8702a31a4df765d`.
- Gradle последовательно, --max-workers=6. Никаких app/device запусков, account
  writes, UI/instrumentation tests или новых test hosts. Полный Xcode/Swift build,
  Native execution и live backend acceptance этим прогоном не подтверждены.
  Пользовательский project.pbxproj сохраняется вне коммитов (48+/41−).

## Восьмой срез L1: собственный UI Roaster в Discovery — 2026-10-26

PR #201, parent `a648e200`; merge в child — `7e0ff262`. POST contract уже
реализован в свежем develop и повторно не переписывался. Parent обновлён и
проверен первым, затем child; единственный конфликт child был в дополнениях
плана, обе записи сохранены. Runtime предупреждение о reduced GET больше не
является открытой задачей: list/picker/map suggestions теперь используют POST.

Владение и parity:

- RoasterDiscoveryEntry принимает callbacks query/cancel/add вместо app header.
  Текущий query и overlay navigation остаются у вызывающей Discovery, shared
  list ViewModel/store/instance key/tag/favorite state не менялись. Root renderer
  лишь вызывает entry и маршруты. Новый RoasterSearchHeader принадлежит impl,
  включает прежние back/clear/cancel/keyboard действия, placeholder, отступы и
  размер clear control 28dp. Основные DS search consumers сохраняют default 48dp.
- На iOS сохранён прежний NativeSearchField через узкий DS presenter, установленный
  root platform composition. Остальные shared search поля не переключены на
  UIKit автоматически. Native реализация остаётся открытым L2, не дублируется.
  Android использует существующий CoffeePeekSearchField; extraction его decoration
  сохраняет SyncedTextFieldState, selection/composition, IME и clear semantics.
- Empty/suggestion copy теперь в Roaster resources с прежними текстами и shared
  mascot/DS card; create callback ведёт в прежний AddRoaster. Error content — DS.
  Image и viewer собираются внутри feature через общий media pipeline/DS, без
  отдельного cache/client. Public RoasterSlots удалён, renderer seams оставлены
  только internal для stateless feature content/previews; aliases на legacy нет.
- Search exposure перенесён на узкий RoasterExposure.Search со старыми значениями
  search/roasters/roasters. List/detail search trackers, события и свойства,
  consent/session guards, click ownership и lifecycle collection сохранены.
  Тестовые события отправляются только in-memory RecordingTransport, не в SDK.
- Парные light/dark previews header/suggestion находятся рядом и используют fake
  state без DI/IO. Substantive composables <=40 строк, обычные helpers <=20;
  нет нового lint/build size gate. Новых module edges, DTO в UI или feature
  зависимости в core не добавлено. Backend/caches/HTTP для Roaster не менялись.

Проверки:

- Первый Android/iOS compile с header/empty переносом успешен (1m24s); затем
  удалены оставшиеся public visual ports и выполнен финальный прогон.
- Объединённое дерево `7e0ff262` + этот срез (3m25s): **285 unit/contract tests /
  48 suites**, 0 failures/errors/skips. Discovery data 19 / 1, impl 66 / 6;
  Roaster impl 19 / 2; DS 57 / 15; network contracts 8 / 2; data mapper
  contracts 4 / 2; app subset 112 / 20, включая Roaster analytics adapters,
  favorites/ports, Discovery/Map, renderer factories и Sandbox backend.
  Неизменённые tasks могут быть UP-TO-DATE, counts не суммируются с parent.
- DirectDebug/DirectSandbox assembly, оба packaged DEX checks и Sandbox isolation
  прошли. iOS Arm64 production compile, Debug Simulator framework link и common
  Native tests Discovery/Roaster impl, DS/app скомпилированы. Native tests не
  исполнялись. Gradle последовательно, --max-workers=6.
- APK SHA-256: Debug `811400e47e5f7cde4011c4816e1d01c9f50dff4eef336abdb3a39edc9c0bd054`;
  Sandbox `62b6b4ff599d3dc80c5c68d58d602cabb250c18cf8c0f92a0997b5342fca3547`.
- Studio inspection RoasterSearchHeader завершилась без ошибок. SearchField
  inspection вернула timeout без найденных ошибок: это неполная проверка, а не
  clean IDE/render acceptance. Полного Xcode/Swift build и ручной native
  keyboard/cancel/VoiceOver/темы/шрифты/окна приёмки не было.
- UI/instrumentation/screenshot tests/hosts не создавались, не компилировались
  и не запускались; чужие тесты не удалялись. Никаких device/app установок,
  backend/account mutations, production analytics или camera действий.
  Пользовательский project.pbxproj сохранён вне среза (48+/41−).

Следующий остаток: активные Discovery map-details/catalog data adapters и
roaster-shop readers через legacy repositories; Contributions/create/root UI
slots и platform infrastructure. Старые недостижимые screens/VM/test consumers
удаляются по последнему потребителю в L4. Modules и полный legacy exit ещё не завершены.

## Parent #195: конфликты с develop — 2026-10-26

Develop `063f4c5b` добавил Community post links/share и отложенное разрешение
геопозиции Discovery. Сначала обновляется parent #195, затем child #201.
Конфликтующие перемещения Android → commonMain разрешены с сохранением новых
возможностей develop и общих migrated entries; старые renderer defaults не возвращены.

- Post entry/screen/resources находятся в Community commonMain; обе платформы
  явно поставляют feature renderer. Внешние ссылки iOS по-прежнему оставлены
  сайту, как в develop; внутриприкладной маршрут не использует unsupported fallback.
  Native меню сохраняет enabled/session gate, добавлены Share glyphs на обеих
  платформах; анимация общего popup из develop сохранена.
- JVM-only форматирование share-текста заменено общим Kotlin с одноразовой
  подстановкой: пользовательские `%2$s`/`%3$s` не интерпретируются как шаблон.
  Два unit-теста проверяют текст, порядок analytics callback и private gating.
- Discovery permission request key сохранён; picker адаптирован к новому
  контракту с прежним запросом геопозиции при открытии выбора кофейни.
- Первый compile выявил пропущенный request key picker, второй — отсутствующий
  Share branch Android menu. Оба исправлены. Следующий прогон был прерван при
  framework link; он не записан успешным. Финальный повтор (2m19s) успешен.
- **312 unit/contract tests / 44 suites**, 0 failures/errors/skips: Community
  data/domain/impl 22/12/42, Discovery impl 71, DS 57, app subset 108.
  Неизменённые tasks могут быть UP-TO-DATE; результаты прогонов не суммируются.
  DirectDebug/DirectSandbox APK, DEX guards и Sandbox isolation прошли;
  iOS Arm64 production compile, Simulator framework link и common Native
  Community impl test compilation прошли. Native tests не исполнялись.
- Studio PostCard inspection вернула timeout без найденных ошибок — неполная
  проверка. Manual links/share/native menus/location acceptance, темы и полный
  Xcode app build остаются gates. Gradle последовательно, --max-workers=6;
  UI/instrumentation tests/hosts не создавались и не запускались. Никаких
  установок приложений, account writes или production telemetry.
  Пользовательский project.pbxproj (48+/41−) исключён из merge commit.

## Девятый срез L3: Discovery/Roaster public readers — 2026-10-26

PR #201, parent `8843e5af`, child merge `76f2a5c8` + этот срез. Конфликты #195
с develop разрешены и проверены на parent первым; GitHub сообщает MERGEABLE.
Текущий срез завершает три активных чтения, а не удаление всего modules.

- Discovery tab/map и check-in picker получают PublicShopPreviews и
  PublicRoasterCatalog. В di/discovery и di/roaster нет ShopRepository,
  getShopDetails/getCatalogs или app formatMapHoursSummary. Composition
  соединяет только публичные контракты двух feature, не их domain/data.
- Shop data использует тот же /api/CoffeeShops/{slug}, existing authenticated
  client и DTO/backend. Наружу выходят только публичные cover/detail variants,
  рейтинг/счётчик, isOpen, координаты и структурированное расписание. Нет
  menu-catalog/favorites requests, private check-in/menu DTO или второго cache.
  PublicShopDetails для Profile остаётся прежним, отдельный узкий preview API
  нужен карте и фото у обжарщика; новые Gradle-модули/зависимости не добавлены.
- Сохранены порядок фото (map: первый card/full → detail; roaster: первый
  пригодный detail/full → cover), null/blank fallback, положительный rating и
  flexible counters. UTC offset берётся при чтении из прежнего device provider;
  shared schedule mapper сохраняет нормализацию дней и week rollover. Выбор
  первого доступного интервала сохраняется, без нового today/DST поведения.
  Discovery impl форматирует HH:mm – HH:mm и локализованное «Закрыто»,
  а не app/data. Координаты проверяются перед перемещением карты.
- PublicRoasterCatalog читает тот же RoasterRepository и persisted
  roaster_catalog_v1:<host>, без второго HTTP/cache/store/JSON формата.
  Фильтры получают public slug/name/logo, недоступные публично строки пропускаются;
  memory/disk/offline fallback остаются у Roaster data. Фото адаптер сохраняет
  мягкую обычную ошибку и не проглатывает CancellationException.
- Koin связывает новые API на Android/iOS и в общем Sandbox graph. Non-UI
  RoasterModuleTest больше не регистрирует legacy ShopRepository: он разрешает
  настоящие feature readers, проверяет public catalog и фото через local fake
  backend. Остальные favorite/settings/platform adapters не объявляются удалёнными.

Проверки и ограничения:

- Первый compile обнаружил невозможность smart cast nullable API properties
  из другого модуля; исправлено safe-call форматированием. Повтор выявил старую
  DI-фикстуру (1 failure из 116 app tests), которая регистрировала legacy reader.
  Фикстура обновлена на реальные новые ports; исходные failures сохранены.
- Финальный прогон (26s), source `76f2a5c8` + этот срез: **275 unit/contract
  tests / 46 suites**, 0 failures/errors/skips: Shop data 50 / 15, Roaster
  data 17 / 2, Discovery data 19 / 1, impl 73 / 7, app subset 116 / 21.
  Неизменённые tasks могут быть UP-TO-DATE; результаты повторов не суммируются.
- DirectDebug/DirectSandbox assembled; DEX guards дополнены новыми public API,
  factories и MapOpeningHours, оба guards и Sandbox isolation прошли.
  iOS Arm64 production compile, Debug Simulator framework link и common Native
  tests Shop/Roaster data, Discovery impl и app скомпилированы. Native tests
  не исполнялись. Gradle последовательно, --max-workers=6.
- APK SHA-256: Debug `24c093cc2b0e1692a2a28f2acb2cf1c582f42865db67e13c89369aac9dc37542`;
  Sandbox `db8e5f39c8d84e0515a264a5a2d30c6a4f2114b0c8f974dffe67e793f761386b`.
- Studio mapper/adapter inspections завершились timeout без найденных ошибок:
  это неполная диагностика. Парные previews map card сохранены; rendering и
  ручные visual/native/location/back/share checks не заявляются. Полный Xcode
  app build, live backend/OAuth/analytics/persistence остаются открытыми gates.
  UI/instrumentation/screenshot tests/hosts не создавались и не запускались;
  чужие suites не удалялись. Приложения не устанавливались, аккаунт/камера/
  production telemetry не затрагивались. Пользовательский pbxproj (48+/41−)
  исключён. Открытые parent/child branches сохранены как зависимости стека.

Далее: Contributions/create/root UI slots; platform map/город/settings/media
bridge relocation; оставшиеся legacy favorites/session/storage/transport readers
и удаление старых screen/VM consumers по последней зависимости. Старые функции
карты/репозитории всё ещё нужны оставшимся legacy consumers и не удаляются вслепую.

## Parent #195: Android CI metadata repair — 2026-10-26

GitHub Android CI run `40374896381`, job `121755150974`, source `8843e5af`,
failed at `:core:media:compileCommonMainKotlinMetadata`: concrete expect
ImageUrlFetcher inherited Fetcher<ImageUrl> without declaring its required
members. Android/iOS target compilation alone had not exercised this separate
metadata gate. iOS build verification for the same revision succeeded.

The common expect contract now explicitly declares inputDataKClass, source,
isSupported and fetch. Existing Android/iOS implementations mark those same
members actual override. Clients, HTTP/permissions/session/cache policy and
decoder behavior are unchanged; no dependency upgrade or CI bypass is used.

- Targeted metadata compile (media and DS), media unit contracts and media
  iOS Arm64 compile passed (22s).
- The complete Android CI command then passed locally (3m03s): DS common
  metadata, DirectDebug/PlayDebug assembly and DEX guards, aggregate
  testDebugUnitTest plus app Direct/Play unit suites, all lintDebug tasks plus
  app Direct/Play lint. Unchanged tasks may be cached/up-to-date.
- No UI/instrumentation/screenshot suites, test hosts, app launches, production
  requests/analytics or account writes. The repair was isolated from unfinished
  Contributions work; local user Xcode/config files are not committed. The
  exact published revision still needs its new GitHub run before CI is called green.
- Shared Kotlin changes must include common metadata compilation in local
  verification; target assembly/Native compile cannot stand in for it.

## Десятый срез L1: Contributions UI ownership — 2026-10-26

Новая дочерняя ветка codex/migration-contributions-ownership от #201,
source `89778c23` + этот срез. Подробный scope/остаток и evidence:
[Contributions ownership exit](contributions/OWNERSHIP_EXIT_PLAN.md).

- Удалены 11 app-owned visual contracts photo/price/brew/logo/cover/remote-image/
  map-picker из public hosts. Feature собирает UI из DS; root поставляет только
  permission/native photo launchers/map canvas/haptic и прежние data/session/
  navigation adapters. Contribution catalogs через legacy reader — следующий
  supporting slice этой же фичи, не новая продуктовая функциональность.
- Map modal/address state/350ms debounce/drift/confirmation теперь в feature.
  Native CoffeeMap/provider сохранён; DS map-control presenter сохраняет iOS
  clipping/z-order. Stateless content и paired previews находятся рядом.
- Общий DS photo grid получил Simple mode с прежней геометрией без reorder,
  индексов и viewer; существующий default режим других consumers сохранён.
  Source sheet использует прежний native presenter и shared DS sheet. Platform
  pipeline URI/JPEG/camera не копировался. Late/duplicate/disposed callbacks,
  current capacity, byte ownership и account check закреплены unit contracts.
- Удалены семь недостижимых app UI файлов, включая старый выбор точки.
  ShopChangeEditorScreen сохраняет существующий чужой ShopMenuEditorTest как
  consumer; этот экран не выбран production routes. Чужой тест не удалялся/
  не расширялся/не запускался. Legacy ViewModels/readers — отдельный остаток.
- Analytics page mapping и прежние routes/step state/status/haptics сохранены;
  legacy forms не имели собственного event tracker. Новых SDK/events/consent
  policy нет. Price/brew mapping остаётся в feature, не в generic core.
- Первый compile обнаружил trailing-lambda preview mismatch и обязательную
  accessibility подпись sheet; исправлено. Повтор compile/старых unit suites
  успешен (59s). Новые contracts и удаления затем проверены финальным прогоном.
- Финальный прогон (9m34s): library **1501 tests / 231 suites** по 55 текущим
  testDebugUnitTest tasks, app Direct/Play **по 825 / 172**, 0 failures/errors/
  skips. Это варианты одного приложения; их результаты не суммируются как
  уникальные сценарии. Старые build outputs исключены из подсчёта; неизменённые
  tasks могут быть cached/up-to-date. Contributions domain/data/impl 16/19/76,
  DS 57; новые regressions покрывают debounce/cancellation/stale results и фото.
- Полный Android CI набор прошёл: common metadata, Direct/Play APK + DEX guards,
  все unit tasks и lint. Дополнительно прошли Contributions api/impl common
  metadata, Sandbox APK/DEX/isolation, iOS Arm64 compile, Debug Simulator
  framework link и common Native tests Contrib impl/DS/app compile. Native tests
  не исполнялись. Gradle последовательно, --max-workers=6.
- Studio state/photo inspections вернули timeout без найденных ошибок, то есть
  неполную диагностику. Полного Xcode app build/preview rendering/manual UI/
  native camera/location acceptance не было. UI/instrumentation/screenshot
  tests/hosts не создавались и не запускались; приложения не устанавливались,
  аккаунт/production backend/telemetry не затронуты. Пользовательский pbxproj
  (48+/41−) исключён. Полный legacy exit и вся фича не объявляются завершёнными.

## Срез Home check-in legacy UI — 2026-10-27

- Удалены недостижимые: fallback `LegacyCheckInCreateFlow` и параметры
  `legacyContent` (create flow, shop check-in form), 6 файлов
  `feature/homecheckin/ui`, `PersonalProfileActivityCalendar/Preview`, легаси
  `CheckInBottomSheet`, Koin-фабрика `HomeCheckInViewModel`.
- Найден и закрыт пробел: новый глобальный create flow не вызывал
  `NotificationFeature.onCheckInCreated()` (запрос уведомлений после создания
  Home). Теперь вызывается один раз при принятом создании; тест
  `GlobalCheckInFlowStoreTest`.
- Удалены тесты мёртвого кода (commonTest Calendar/Editor/PhotoViewer/
  Visibility/SelectedDayEntries, 4 инструментальных Home/Calendar, 2 теста в
  `CheckInPresentationTest`). Сценарии покрыты в feature/check-in-history и
  feature/home-check-in; отдельных unit-тестов нет у конвертации минут/секунд и
  русских plural сводки ошибок (теперь UI/ресурсы feature).
- Остаётся: `CheckInCreateSheet/Content` (используется инструментальным
  `CheckInPresentationTest`), легаси-репозиторий `modules/home-checkin` (L3).
- Прошли: Direct/Play unit, lintDirectDebug, assemble Direct/Sandbox, DEX и
  Sandbox isolation guards, iOS Arm64 compile, simulator test-compile, framework
  link. Native tests не исполнялись; UI/manual не проверялись.

## Срез Roaster legacy UI — 2026-10-27

- Удалены недостижимые `ui/screen/roaster/Roaster{Detail,List}{Screen,ViewModel}`,
  их Koin-фабрики и тесты старого кода (`RoasterDescriptionTest`,
  `RoasterListTest`, `RoasterAnalyticsParityTest`). Активный UI/состояние —
  `feature/roaster/impl`; `RoasterScreenRenderer` остаётся как слот.
- Покрытие: ленивая загрузка описаний/лимит 4 запроса, фильтры/теги,
  избранное, stale-результаты — `RoasterListViewModelTest`/
  `RoasterDetailViewModelTest`; аналитика — `RoasterAnalyticsAdapterTest`.
  Parity-тест сравнивал с удалённым baseline и снят вместе с ним. Подпись
  счётчика «—» теперь только часть рендера карточки feature (без unit-теста).
- Прошли: Direct/Play unit, lintDirectDebug, assemble Direct/Sandbox, DEX и
  isolation, iOS Arm64 compile, simulator test-compile, framework link.
  Native tests/manual UI не выполнялись.
- Кандидаты следующих срезов (по скрипту «имена не используются в main»):
  auth Stepper/Footer/Layout и `AuthViewModel`/`RegisterViewModel`,
  `PasswordRecovery*`, `DeleteAccountPendingViewModel`, `ReviewReportScreen`,
  `ShopReviewsScreen`, `CheckInShopPickerScreen`, `CheckInSubmissionContent`,
  `DiscoveryTabContent`, `ProfileActions/LogoutConfirmation`,
  `OtherUserProfileScreen`, `Coffee{FiltersSheet,RoasterLogo}`,
  `NotificationsButton` — каждый требует проверки тестов-потребителей.

## Срез Auth legacy — 2026-10-27

- Удалены недостижимые `ui/screen/auth/{AuthViewModel,registr/RegisterViewModel,
  AuthStepper,AuthFooterRow,AuthLayout,AuthFields,AuthHeading}` и
  `feature/passwordrecovery/ui/PasswordRecoveryViewModel` с тестами старого кода
  (`AuthViewModelTest`, `RegisterViewModelTest`, `RegisterAnalyticsConsentTest`,
  `PasswordRecoveryViewModelTest`). Активный Auth — `feature/auth/impl`.
- Покрытие: `LoginViewModelTest`, `RegisterViewModelTest`,
  `PasswordRecoveryViewModelTest` в feature/auth/impl. Найденный пробел —
  социальная регистрация не должна применять согласие на аналитику — закрыт
  утверждением в `aGoogleSignUpSignsInWithTheRegisterEntryAndEmitsSignedIn`.
- Остаётся: `PasswordRecoveryRepositoryImpl` (+тест) — его использует чужой
  `SandboxApplicationBackendTest`; убрать вместе с переводом Sandbox-теста на
  feature-репозиторий (L3).
- Прошли: Direct/Play unit, `:feature:auth:impl:allTests`, lintDirectDebug,
  assemble Direct/Sandbox, DEX и isolation, iOS Arm64 compile, simulator
  test-compile, framework link. Native tests/manual UI не выполнялись.

## Срез Shop/Review legacy — 2026-10-27

- Удалены недостижимые `ui/screen/review/ReviewReportScreen`,
  `ui/screen/shop/{ShopReviewsScreen,CheckInShopPickerScreen,
  CheckInSubmissionContent}`. Prod-потребителей не было; активные экраны —
  `feature/shop/impl` и `feature/home-check-in/impl`.
- Инструментальный тест `checkInResultFitsItsContentAndKeepsBothActionsVisible`
  в `SubmissionStatusTest` (чужой, зависел от удалённого композабла) снят; два
  других теста файла сохранены. Unit-тестов на удалённые экраны не было.
- Прошли: Direct/Play unit, lintDirectDebug, assemble Direct/Sandbox, DEX и
  isolation, iOS Arm64 compile, simulator test-compile, framework link.
  Native tests/manual UI не выполнялись.
- Остаются кандидаты: `DiscoveryTabContent`, `ProfileActions`,
  `ProfileLogoutConfirmation`, `OtherUserProfileScreen`, `CoffeeFiltersSheet`,
  `CoffeeRoasterLogo`, `NotificationsButton`, `PasswordRecoveryRepositoryImpl`.

## Срез остатков Profile/Discovery/Coffee/Notifications — 2026-10-27

- Удалены недостижимые `ui/screen/main/DiscoveryTabContent`,
  `ui/screen/profile/ProfileLogoutConfirmation`,
  `feature/coffee/ui/{CoffeeFiltersSheet,CoffeeRoasterLogo}`,
  `feature/notifications/ui/NotificationsButton`,
  `ui/screen/checkins/HomeTimelinePresentation` (+ `HomeTimelinePresentationTest`:
  конверсия легаси HomeCheckIn → CheckIn, потребители удалены в срезе Home).
- Прошли: Direct/Play unit, lintDirectDebug, assemble Direct/Sandbox, DEX и
  isolation, iOS Arm64 compile, simulator test-compile, framework link.
  Native tests/manual UI не выполнялись.
- Теперь недостижимы из main, но удалены НЕ были (нужна сверка тестов):
  `feature/coffee/ui/CoffeeListViewModel` (держат `CoffeeFilterAnalyticsTest`,
  `CoffeeAnalyticsParityTest` — сверить ключи фильтров с feature/coffee impl и
  `CoffeeAnalyticsAdapterTest`), `feature/notifications/ui/NotificationsSheet`
  (инструментальный `NotificationsPresentationTest`), `ui/screen/feed/FeedScreen`
  (`discoveryMapOffsetY` проверяет `FeedFiltersUiTest`),
  `DeleteAccountPendingViewModel` (+`DeleteAccountPendingViewModelTest`, 6
  сценариев; в feature покрытия не найдено), `feature/profile/ui/
  OtherUserProfileScreen` и `ProfileActions` (чужие инструментальные
  `PublicProfilePresentationTest`/`AuthorBlockingFlowScreenshotsTest`),
  `PasswordRecoveryRepositoryImpl` (Sandbox-тест).

## Срез сверки оставшихся кандидатов — 2026-10-27

- Coffee: удалены легаси `CoffeeListViewModel`, `CoffeeFilterAnalyticsTest`,
  `CoffeeAnalyticsParityTest`. Ключи фильтров/сброс/позиции/устаревшие ответы
  покрыты `CoffeeAnalyticsPortsTest` + `CoffeeAnalyticsAdapterTest`; parity-тест
  сравнивал с удалённым baseline.
- Delete account: удалён `DeleteAccountPendingViewModel` + тест; 6 сценариев
  покрыты `AccountDeletionViewModelTest` (feature/profile), он шире (тикер,
  конец сессии). Осознанное отличие feature: открытие без заявки только просит
  подтверждение, письмо не шлёт.
- Notifications: удалены `NotificationsSheet`, `NotificationRow`,
  `NotificationEmptyState`, `NotificationPushSettings` и инструментальный
  `NotificationsPresentationTest` (проверял только удалённый композабл).
- Feed/Discovery: удалены `FeedScreen`, `ShopFiltersScreen`,
  `CatalogSuggestionCard`, `ShopTagGroups`; три геометрических теста из
  `FeedFiltersUiTest` сняты (дубль `ChoreographyTest` в feature/discovery), тесты
  живого `FeedViewModel` оставлены.
- Прошли: Direct/Play unit, lintDirectDebug, assemble Direct/Sandbox, DEX и
  isolation, iOS Arm64 compile, simulator test-compile, framework link.
  Native tests/manual UI не выполнялись.
- Намеренно НЕ удалено: `feature/profile/ui/OtherUserProfileScreen` и
  `ui/screen/profile/ProfileActions` (держат чужие инструментальные скриншот-
  тесты `PublicProfilePresentationTest` 943 строки,
  `AuthorBlockingFlowScreenshotsTest`; эквивалента в feature нет — нужно решение);
  `PasswordRecoveryRepositoryImpl` (его и легаси `AuthService` проверяет
  `SandboxApplicationBackendTest`; уходит вместе с L3 auth cleanup).
