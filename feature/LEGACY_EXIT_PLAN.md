# Полный выход из legacy

Статус: в работе. Решение владельца от 2026-10-25. База `029a572a`,
ветка `feat/ios-build-foundation`, PR #195 → develop.
Вариант для кофейни отложен; новые продуктовые возможности не добавляем.

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

## Очерёдность срезов

### L1. Убрать бизнес-UI из активных app slots

- [x] Profile UI: публичное избранное, публичная недельная Activity, карточки
  чекинов; на публичном профиле никогда не подключать личный reader.
  Избранное/неделя — первый срез; карточки — второй. Public check-in reader
  заменён в третьем срезе L3; enrichment/own-account ports ещё остаются.
- [x] History/Profile: один владелец общей геометрии чекина через
  существующие DS primitives; без преобразования обратно в legacy CheckIn.
- [ ] Discovery/catalog и создание чекина: бизнес-UI/состояние только в impl.
- [ ] Author blocking: экран/host из composeApp в feature/author-blocking/impl.
- [ ] Root tabs/check-in creation host: root оставляет routing, содержимое —
  feature entries. Вкладка Coffee не должна зависеть от legacy CheckInTabContent.

Не переносим весь composeApp/ui в feature одним каталогом. Каждая вставка
переходит к владельцу, её ресурсы и paired previews — вместе с ней. Общая
геометрия карточек/календаря уже есть в core/design-system и переиспользуется.

### L2. Убрать app-owned инфраструктурный UI и сервисы

- [ ] CpImage, prefetch, original/preview contracts, viewer: единая media
  инфраструктура и DS hooks; не новая копия загрузчика на каждую feature.
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
- [ ] Public favorites enrichment и own-account ports — feature data,
  без старых ShopRepository/RoasterRepository/UserRepository.
- [ ] Shop/catalog/roaster/favorites readers и caches — соответствующие feature
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
