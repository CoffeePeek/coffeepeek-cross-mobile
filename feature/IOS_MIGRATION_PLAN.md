# План миграции iOS на новые feature-модули

Статус: принципы согласованы 2026-10-24; ход работ — в разделе «Статус» ниже. Основан на сверке с продом (`origin/main`) по всем владельцам: инвентарь Gradle-модулей,
iOS-документы (`docs/IOS_NATIVE_EXPERIENCE.md`, `IOS_NATIVE_NAVIGATION.md`, `IOS_COMPOSE_INTEROP_AUDIT.md`, `IOS_MAP_PARITY.md`,
`IOS_FEED_PHOTO_MEMORY.md`, `IOS_SETUP.md`), `feature/LEGACY_REMOVAL.md`, `feature/roaster/IOS_MIGRATION_PLAN.md`.
Первоначальный инвентарь не был проверен запуском UI на симуляторе или устройстве.
Фактически выполненные проверки последующих срезов и оставшиеся gates записаны ниже отдельно.

## Статус на 2026-10-25 (ветка `feat/ios-build-foundation`, PR #195 → `develop`)

Сделано и проверено сборкой и юнит-тестами (Android и iOS compile; iOS-фреймворк линкуется; тесты сессии прошли и на iOS-симуляторе).
На симуляторе и устройстве экраны **не запускались**.

**Этап 0 — сборка: готов.**
- iOS-таргеты добавлены: shop, shop-report, favorites (api и impl), check-in-history api, home-check-in api.
- UI, строки и ресурсы перенесены в `commonMain` у 11 владельцев: app-update, notifications, settings, auth, community, profile, discovery, coffee, contributions, home-check-in, check-in-history. Roaster, shop, shop-report и favorites уже были общими.
- В `androidMain` остались только Android-части: установщики обновлений, Google-чузер, форматтер даты ошибки смены имени (Android-actual).
- Найдено по пути: JVM-only `toSortedMap` в `CoffeeListViewModel` (iOS его не собирал) — исправлено.
- В `kotlinx-datetime` переведены даты в notifications, contributions, home-check-in.

**Этап 1 — нативные хуки в design-system: готов.** Через точки подключения (`LocalCoffeePeek*Presenter`); iOS ставит UIKit-реализации одним местом, `ProvideDesignSystemPlatformHooks` в `composeApp`, Android рисует Material как раньше:
шторка, native header, date picker, alert, выбор фото, haptic, scroll-to-top, роли клавиатуры (AutoFill, «Готово»), UIMenu для ActionMenu, glass-кнопки, переключатель с haptic, спиннер, `CoffeePeekBackHandler`, `CoffeePeekScrollableColumn`; `CoffeePeekActionMenu` перенесён в commonMain. В checkpoint Coffee добавлено подключение UIKit segmented control к DS-капсуле.
`CoffeePeekScrollableColumn` используется фильтрами Coffee. Не использовано экранами пока: `nativeAction` в header; native-иконки у части glass-кнопок и меню не проставлены.

**Этап 2 — сессия: F23 и F24 готовы** (выход стирает старые строки; сбой удаления Keychain сообщается). Core session port вместо `SessionRepository` (~43 файла) — **не сделан**.

**Этап 3 — подключение на iOS: начат.** Через новые модули на iOS подключены: вкладка Settings (и три её подэкрана), окно обновления, колокольчик уведомлений, экраны входа, регистрации и восстановления пароля, **Community (вкладка «Лента»)**, **Discovery (вкладка «Поиск»)**, **Coffee (вкладка каталога и детали кофе)**, **Profile (собственный/публичный профиль, его чекины, редактирование и подтверждение удаления аккаунта)** и **Contributions (списки заявок, добавление кофейни/обжарщика, предложение правки, редактор и детали заявки)**, **Shop (кофейня, публичные чекины, галерея меню, форма чекина)** и **Shop-report (неточность в данных кофейни)**. Подключены также Google/Apple кнопки и существующие Swift-провайдеры (при доступной конфигурации); реальный OAuth ещё не проверен.
На iOS Profile, Community, Discovery, Coffee, Contributions и Shop/Shop-report идут через feature-рендереры.
Личная история/Home и Favorites ещё используют текущие legacy entries;
слоты календаря, избранного и карточек в Profile сохраняют эти существующие реализации.

**Важные находки для следующих срезов**
1. Kotlin/Native не линкует переопределение composable-метода интерфейса с аргументом по умолчанию (`modifier: Modifier = Modifier`): ошибка видна только при линковке и только у достижимого кода. У каждого владельца при подключении убрать значения по умолчанию из api-интерфейсов (так сделано у Settings и Auth).
2. Подключение владельца требует: перенести platform-neutral адаптеры из `androidMain` в `commonMain` (как у Settings) или добавить iOS-пары, перенести зависимость `impl` в `commonMain.dependencies` composeApp и добавить фабрику в `PlatformKoin.ios.kt`.
3. **Auth Google/Apple: реализация и offline-проверки готовы** (см. checkpoint ниже): домен challenge/login, `AppleAuthorizer` поверх существующего native provider, Google launcher поверх `IosGoogleAuthBridge`, кнопки и тесты. Реальный OAuth, signing/configuration и ручная iOS-приёмка остаются release gate; компиляция не закрывает их.
4. Для сообщения проверки обновлений на iOS нет тоста: временно используется `ErrorHandler.showError`, нужно решение по UX.

**Остаётся**
- Этап 3: Check-in history, Home check-in и общий create flow, Favorites (по решению продукта); ручная приёмка Community/Discovery/Coffee/Profile/Contributions/Shop и live-приёмка Apple/Google.
- Контентная жалоба `ReportCheckIn`/`ReportReview` пока использует общий legacy ReviewReportScreen; это отдельный маршрут, а не мигрированный Shop-report.
- Этап 2: core session port.
- Roaster: Xcode-линковка, симулятор, устройство.
- Этап 5: удаление legacy — только после полной готовности и ручной приёмки.
- Push на iOS (`PushAppDelegate`, `NativePushBridge`) и Xcode/устройство-гейты не трогались.

### Checkpoint: Google/Apple Auth — 2026-10-24

Продолжение опубликованного [PR #195](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/195), на базе `1c622b90`.
Новых веток/PR для этого поддерживающего среза не создавали. Legacy fallback не удалён.

Что сделано:

- `feature/auth/domain`: `AppleSignInChallenge`, `beginAppleSignIn`, `signInWithApple`.
  Data выполняет прежние `/api/Tokens/apple/challenge` и `/api/Tokens/apple/login`,
  валидирует session envelope, сохраняет токены через тот же SessionStore и cleanup port.
  Другой аккаунт очищается по общему правилу, серверное избранное не удаляется.
- Impl координирует challenge → native authorize → exchange. Challenge ID идёт
  как state, server nonce — без преобразования/повторного хеширования. Каждая
  новая попытка запрашивает новый challenge. Отмена корутины не превращается в ошибку UI.
- `composeApp/iosMain/di/auth`: iOS-порты используют существующие AppleAuth /
  IosAppleAuthProvider и IosGoogleAuthBridge; Swift-реализации и OAuth IDs не менялись.
  Google chooser не запускается повторно, поздний callback ушедшего экрана не
  вызывает backend. Существующее значение отмены Swift Google chooser игнорируется.
- В Login/Register доступны оба провайдера, если native bridge настроен. Кнопки
  рядом, если помещаются, иначе колонкой с учётом размера текста; Apple logo —
  прежний asset с сохранённой provenance. Есть colocated light/dark previews.
  Без доступного провайдера кнопка скрыта; Android не получает Apple-заглушку.
- Ошибки Apple имеют собственные ресурсы, не текст «через Google». Аналитика
  сохраняет `sign_in_completed`, `method=apple/google`, `entry_point=login/register`;
  social sign-in не создаёт password-sign-up и не меняет consent автоматически.
  Busy guards не дают одновременно отправить password/Google/Apple запросы.
- `core/network/HttpStatusFailure`: минимальный контракт статуса ошибки валидатора,
  без UI/legacy dependencies. Старый ApiException реализует его, поэтому Apple
  сохраняет ASI-H статус даже при раннем 5xx throw. Feature data не импортирует
  legacy exception; ASI-CH/X/R/A/I и provider diagnostics остаются распознаваемыми.

Проверки этого среза (Mac, JBR 21, 2026-10-24; база `1c622b90` + изменения auth):

- Auth Android unit: **71 tests**, 7 suites, 0 failures/errors/skips. App auth/Apple
  contract subset (`*Auth*`, `*Apple*`): **54 tests**, 24 suites, 0 failures/errors/skips.
  Итого **125**, без UI/instrumentation/screenshot suites. Проверены отмена,
  duplicate/competing submits, новый challenge после cancel, ошибка challenge,
  exchange/body/HTTP/validator failures, отсутствие сохранения при cleanup failure,
  аналитика и отсутствие password-registration/consent side effects.
- Первый запуск остановился на компиляции двух новых assertEquals (generic type
  inference), до полного выполнения impl suite. После явного типа прогоны зелёные;
  первоначальный failure не скрывается успешным повтором.
- `:composeApp:assembleDirectDebug` прошёл, включая packaged runtime-class/DEX checks.
  APK не устанавливался, обычный CoffeePeek не запускался и его данные не менялись.
  SHA-256 APK: `11922bf87096ea85236480bea94a93b059e21bb3c1a38d7050bbf5a792778812`.
- `:composeApp:compileKotlinIosArm64` и `:composeApp:linkDebugFrameworkIosSimulatorArm64`
  прошли; вторая задача также компилирует simulator target. Новые iOS adapter/entry
  действительно достижимы в link, не только лежат в исходниках.
- Все Gradle-запуски последовательные, `--max-workers=6`. Логи сохранены локально
  вне tracked sources. Есть существующие deprecation/expect-actual warnings и
  warning bundle ID framework; это не ошибки link и не изменения signing приложения.
- Studio inspection для AppleAuthBackend не нашла errors. Для common Compose UI
  IDE не разрешает Compose classpath, для iOS launcher сообщает conflicting
  overloads; успешные Android/Native compile+link зафиксированы отдельно. IDE
  inspection/preview не объявляется пройденной визуальной проверкой.

Открытые gates: настоящий Google/Apple chooser на iOS, cancel/back, повторная попытка,
nonce/state/expiry на реальном API, provider account-conflict/rejection, обновление
токенов/сессии, обе темы/крупный шрифт/keyboard и screen/exposure events на устройстве.
Нужны согласованный тестовый аккаунт и явное разрешение на live sign-in; offline
fake tests не доказывают OAuth, серверную конфигурацию, Xcode app signing или acceptance.
В этом срезе simulator/device UI и реальная авторизация не запускались.

После auth checkpoint следующий code-срез Community выполнен ниже. Ручная
Auth-приёмка при готовом тестовом окружении остаётся отдельным gate.

### Checkpoint: Community / вкладка «Лента» — 2026-10-24

Продолжение того же PR #195, база `cf68d8cb` + этот срез. Сверены свежие
`origin/main=16724b7e` и `origin/develop=8ac9f49a`: обе ревизии уже входят
в ветку, дополнительный merge не требовался.

Сделано:

- `FeatureCommunityScreenRenderer` перенесён из Android DI в `composeApp/commonMain/di/community`.
  Android и iOS собирают один feature entry из уже общих repository/account/drinks/
  analytics/author-blocking ports. Нового data source, session store или writer нет.
  Экран и его MVI остаются в `feature/community/impl`, не перенесены в composeApp.
- В `PlatformKoin.ios.kt` подключена feature-фабрика; native Community tab в
  `PlatformRootNavigation.ios.kt` больше не вызывает legacy CommunityScreen напрямую.
  В native header передаётся тот же NotificationsRenderer.Bell; UIKit shell не заменён.
- Из CommunityEntry.Content убраны default arguments composable-интерфейса;
  shared app adapter передаёт modifier, header, clearance и global-create slot явно.
  Legacy renderer остаётся fallback, а его UI/VM не удаляются до общей приёмки.
- Сохранены authenticated CpImage, feed/detail/original policy, prefetch, viewer,
  display date, feed/exposure analytics, snackbar и нижний clearance. Community
  использует DS Scaffold; в native tab inset уже принадлежит NativeScreenController,
  явный clearance передаётся существующим slot-контрактом, не добавлен второй отступ.
- Existing DS pull-to-refresh берёт native haptic/scroll-to-top hooks. Edit/report
  sheets и drink select идут через native presenters. Block/Edit menu actions
  получили существующие native glyphs; правила доступности действий не менялись.
- Исправлен найденный при подключении дефект DS native menu: признак `enabled`
  теперь доходит до UIKit overlay, а поздний callback проверяет текущее состояние
  кнопки и текущий набор действий. При release кнопка отключается и menu удаляется.
  Compose fallback тоже закрывается/игнорирует действие при блокировке. Добавлены
  пять чистых common unit tests callback policy и paired light/dark menu previews;
  UIKit touch/lifecycle этим не сертифицированы.
- Global create и EditHomeCheckIn(id) маршруты сохранены. На iOS они пока используют
  прежние формы/History renderer: подключение Community не означает миграцию Home
  или общего создания. Переходы к Shop/Public Profile тоже используют текущий app renderer.
- Account/catalog/analytics contract tests перенесены из androidUnitTest в commonTest.
  Добавлены paired light/dark previews карточек и ленты/ошибки рядом с компонентами.
  Автоматизированные UI/screenshot tests или test-only Activity/APK не создавались.

Проверки этого среза (Mac/JBR 21, 2026-10-24):

- Feature Community domain/data/MVI: **61 tests**, 6 suites. App subset
  `*Community*` / `*Feed*` / `*PlatformMenuAction*`: **73 tests**, 12 suites;
  design-system: **41 tests**, 11 suites. Итого **175**,
  0 failures/errors/skips. Покрытие существующими fake unit/contract suites:
  public/mine, account change и late results, pagination, edit/report/visibility/
  helpful state, API mapping и analytics properties. Это не реальные server writes.
- `assembleDirectDebug` и packaged runtime-class checks прошли. Проверка APK
  теперь требует определения FeatureCommunityScreenRenderer, его factory и
  CommunityEntryImplKt. Play Debug в этом срезе отдельно не собирался.
- `compileKotlinIosArm64`, `linkDebugFrameworkIosSimulatorArm64` и
  `compileTestKotlinIosSimulatorArm64` прошли. Common tests для Native только
  **скомпилированы**, не выполнялись на симуляторе. Native interface/entry действительно
  достижимы в линковке благодаря новой iOS factory и tab call.
- Первые два полных прогона (до исправления native menu) успешны: **129** tests;
  после исправления enabled/callback policy полный прогон с DS и новыми unit tests
  тоже успешен: **175** tests. Gradle — последовательно, `--max-workers=6`; логи вне tracked sources.
  Сохранились прежние Preview/expect-actual/other-module warnings.
- Studio inspection shared adapter всё ещё сообщает несогласованные Compose
  classpath/slot errors, как в auth checkpoint. Android/Native compile+link
  зафиксированы независимо; у изменённого CoffeePeekActionMenu inspection ошибок
  не нашёл. IDE preview не объявляется ручной UI-приёмкой.

Не проверено: ручное открытие Community на iOS, обе темы/крупный шрифт/VoiceOver,
native menus/sheets, keyboard, safe area/tab bar, refresh,
photo viewer/cancel, возврат из Home editor, реальные API mutations и аналитика
с согласованным аккаунтом. Устройства/симуляторы и обычный CoffeePeek не запускались;
APK не устанавливался. Empty/live feed не использовался для создания записей.

Следующая точка code-плана на этом checkpoint: **Discovery / вкладка «Поиск»**, затем Coffee.
Auth и Community runtime/device acceptance остаются открытыми отдельно от следующих переносов.

### Checkpoint: Discovery / вкладка «Поиск» — 2026-10-25

Продолжение PR #195, база `15ea78de` + этот срез. Свежие `origin/main=16724b7e`
и `origin/develop=8ac9f49a` уже входят в ветку; дополнительный merge не нужен.
Сверены entry, legacy DiscoveryTabContent/MapScreen и текущий ShopRepositoryImpl.

Сделано:

- Platform-neutral renderer, host, account/city/favorites/detail adapters и analytics
  перенесены из `androidMain` в `commonMain/di/discovery`. В `PlatformKoin.ios`
  подключена feature-фабрика. Native tab уже вызывал DiscoveryScreenRenderer,
  поэтому второй маршрут или замена UIKit shell не требуются. UI/MVI остаются
  в `feature/discovery/impl`; FeedViewModel/MapViewModel не удаляются: они нужны
  оставшимся legacy consumers, включая выбор кофейни при создании чекина.
- У DiscoveryTabEntry.Content убраны default arguments composable-интерфейса;
  общий адаптер явно передаёт modifier/clearance/focus/callback. Сохраняются
  переходы к кофейне/Auth/AddShop, pending map focus/consume, roaster slots,
  CheckInTabContent и скрытие глобальной create-кнопки при раскрытии карты.
- На обеих платформах карта по-прежнему CoffeeMap; iOS использует существующий
  Swift MapLibre provider. Политика размера canvas перенесена в app host через
  существующий platform mapCanvasSize: Android сохраняет полноразмерный canvas
  во время анимации; iOS ограничивает UIView видимым контейнером. Общий UI больше
  не навязывает нативной мини-карте requiredSize всего экрана. Рендеринг Swift не менялся.
- Проверена гипотеза о потерянных server clusters: текущий legacy repository
  запрашивает shops на zoom 22 и возвращает пустые server clusters. Discovery
  делает то же; группировка точек остаётся у platform map renderer. Новый
  DTO/domain cluster contract не вводился. Два запроса shops/zones и fallback зон
  при ошибке сохраняются существующими repository contract tests.
- Существующие ServerFavoriteShopsRepository и mutation analytics перенесены
  в общий app DI; узкий serverFavoriteShopsModule используется Android и iOS.
  Источник тот же server FavoritesRepository, включая account/cancellation guards;
  iOS legacy FavoriteRepository, экран Favorites и его writer не заменены.
  DI-контракт проверяет чтение membership без гидрации карточек/нового writer.
- AppDiscoveryLocation использует app-owned scope и Dispatchers.Main.immediate:
  CoreLocation получает main run loop. Чтение только при уже выданном разрешении,
  после ответа разрешение перепроверяется; запрос разрешения остаётся у существующего
  LocationPermissionEffect. Четыре fake unit tests не вызывают платформенную геолокацию.
- Сохранены media/placeholders/prefetch, haptic/Reduce Motion, RetainedContent,
  search/map correlation и consent guards. Добавлены три in-memory analytics
  контракта; тесты adapter/mutation analytics доступны в commonTest. Native sheets
  идут через существующий DS hook; root/sheet уже потребляют safe area, новые
  дополнительные отступы не добавлялись. Добавлены paired light/dark previews
  SearchContent/MapContent; карта в preview — заглушка, не Swift/MapLibre.

Проверки этого среза (Mac/JBR 21, 2026-10-25, база `15ea78de` + этот срез):

- Discovery domain/data/MVI: **83 tests**, 6 suites. App subset `*Discovery*`,
  `*di.favorites*`, `com.coffeepeek.admin.map.*`, `com.coffeepeek.admin.ui.screen.map.*`:
  **81 tests**, 19 suites. Итого **164**, 0 failures/errors/skips, первый полный
  прогон успешен. В выборку по имени попали также чистые geometry/tab-route/fake-backend
  контракты; это не UI rendering/instrumentation. Проверены main-dispatcher location
  seam, DI/server membership, account cancellation, favorite analytics/consent,
  list/map correlation и отсутствие текста запроса в telemetry, search/filter/map правила.
- `assembleDirectDebug` и `assembleDirectSandbox` прошли, включая packaged runtime-class
  checks обоих APK и Sandbox isolation. Новая проверка требует FeatureDiscoveryScreenRenderer,
  его factory, DiscoveryEntriesImplKt и общего ServerFavoriteShopsRepository.
  Обычный CoffeePeek/Sandbox не устанавливались и не запускались; Build Variant Studio не менялся.
- `compileKotlinIosArm64`, `linkDebugFrameworkIosSimulatorArm64` и
  `compileTestKotlinIosSimulatorArm64` прошли. Общий entry/host/iOS graph достижимы
  в линковке; Native common tests только скомпилированы, не выполнялись.
  Swift-приложение и signing в этом срезе не собирались.
- Gradle последовательно, `--max-workers=6`; лог вне tracked sources. Прежние
  Preview/expect-actual и framework bundle-ID warnings остаются. Studio inspections
  для location/module вернули timeout: результат inconclusive, не успешная IDE-приёмка.
- Light/dark previews с fake state компилируются, IDE rendering не проверялся.
  Автоматизированные UI/screenshot tests и test-only Activity/APK не создавались и не запускались.

Открытые gates: ручной iOS поиск/быстрые фильтры/черновик шторки и cancel, mini-map
clip/touch при прокрутке и раскрытии, selected pin/zone/carousel, pending focus и
возврат из кофейни, permission denied/granted, keyboard/VoiceOver/крупный шрифт,
обе темы/Reduce Motion/tab safe area, global create и roaster overlay. Настоящие
API favorite writes, permission dialogs и production analytics не запускались;
fake проверки не заменяют их приёмку с согласованным аккаунтом.

Следующий владелец code-плана на этом checkpoint: **Coffee / каталог и вкладка кофе**.

### Checkpoint: Coffee / каталог и детали — 2026-10-25

Продолжение PR #195, база `4b108d3a` + этот срез. Свежие `origin/main=16724b7e`
и `origin/develop=8ac9f49a` уже входят в ветку, дополнительный merge не требовался.
Сверены native tab call, общий detail route, legacy CoffeeList/Detail/Filters,
client/DI, weight selector и аналитика. Legacy-экраны/модели не удалены.

Сделано:

- `FeatureCoffeeScreenRenderer`, `CoffeeModule`, `AppCoffeeAnalytics` и screen
  analytics перенесены в `composeApp/commonMain/di/coffee`. Четыре зависимости
  Coffee api/domain/data/impl перенесены из Android в common source set приложения.
  UI/MVI/resources остаются в `feature/coffee`, root navigation и platform ports — в app.
- iOS подключает тот же coffeeModule и renderer; native Coffee tab больше не
  вызывает legacy CoffeeListScreen напрямую. Detail route уже шёл через renderer.
  Сохранены общий create slot, возврат, переходы к обжарщику/похожему кофе,
  browser продавца, канонический share path, gallery и формат проверенной даты.
- Repository по-прежнему использует существующий plainClient. Публичный feature
  CoffeePicker регистрируется рядом с repository, legacy picker — отдельный контракт
  и продолжает обслуживать текущие legacy формы. Auth/session/client policy не менялись.
- Из CoffeeListEntry/CoffeeDetailEntry.Content убраны default arguments composable
  интерфейсов; adapter передаёт modifier явно. List/detail используют DS Scaffold
  и scroll content padding: clearance native tab и safe area pushed detail не теряются.
  Фильтры используют существующий CoffeePeekScrollableColumn и native DS sheet;
  MVI apply/cancel/reset и ресурсы не менялись.
- В DS добавлен native SegmentedControl hook для не прокручиваемых капсул.
  iOS делегирует существующему UISegmentedControl (2–4 варианта), остальные случаи
  оставляют общий renderer; Android визуально не меняется. Нативная реализация
  вынесена из PlatformPresentation в отдельный NativeSegmentedControl.ios файл:
  сохранены surface-подложка против чёрных углов, underlay/clipping, цвета тем и
  haptic. Передаются enabled и текущий callback; release отключает control и снимает target.
- Три чистых DS unit tests проверяют disabled/невалидный индекс/current options
  callback policy без Compose/UIKit execution. Analytics adapter tests перенесены
  в commonTest с multiplatform IOException; добавлен consent-declined контракт.
  Android legacy-vs-feature analytics parity и fake DI tests сохранены.
- У list/detail/error/empty/filter/weight offer добавлены paired light/dark previews
  рядом с владельцами, без DI/сети. Native selector сохраняет собственную пару previews.

Проверки (Mac/JBR 21, 2026-10-25, база `4b108d3a` + этот срез):

Первый полный прогон остановился на Native compilation:
перенесённые common adapters ещё не видели Coffee, потому что app dependencies
остались в androidMain. DS/feature unit suites успели пройти, общий прогон не был
успешным. После переноса четырёх зависимостей в commonMain повторный полный прогон
успешен. Исходная ошибка сохранена, не объявляется только проблемой IDE.

- Coffee domain/data/impl: **64 tests**, 7 suites. Design-system: **44 tests**,
  12 suites. App subset `*Coffee*` / `*NativeTabRoutes*`: **24 tests**, 8 suites.
  Итого **132**, 0 failures/errors/skips. DS/feature suites прошли до первой Native
  ошибки, а при повторе были up-to-date; app subset выполнен в успешном прогоне.
  Нативные callbacks/рисование UI этим не проверялись. Включены fake DI/plain-client,
  API mapping, picker, list/detail MVI, legacy-vs-feature analytics parity,
  consent-declined и invalid/disabled segment callback contracts.
- `assembleDirectDebug`, `assembleDirectSandbox`, packaged runtime-class checks
  обоих APK и Sandbox isolation прошли. Новые guards требуют renderer/factory,
  CoffeeModuleKt и entry factories. APK не устанавливались/не запускались;
  обычный CoffeePeek, аккаунт и выбранный Studio Build Variant не менялись.
- `compileKotlinIosArm64`, `linkDebugFrameworkIosSimulatorArm64`,
  `compileTestKotlinIosSimulatorArm64` прошли. Новые adapter/module/entry и UIKit
  selector достижимы в Native link. Common app tests для iOS только скомпилированы,
  не выполнены на симуляторе. Swift app и signing в этом срезе не собирались.
- Gradle последовательно, `--max-workers=6`, локальные логи вне tracked sources.
  Прежние deprecation/expect-actual/framework bundle-ID warnings остаются. После
  исправления Gradle source set Studio по-прежнему сообщает unresolved feature
  dependencies в CoffeeModule; успешные Android/Native builds зафиксированы
  отдельно, IDE inspection/preview не объявляются успешной ручной приёмкой.
- Paired previews компилируются; IDE rendering и реальный UIKit UI не проверялись.
  Автоматизированных UI/Compose/instrumentation/screenshot tests и их Activity/APK
  инфраструктуры не добавлялось и не запускалось.

Открытые gates: iPhone light/dark/большой шрифт/VoiceOver, scrolling и clipping
package photos/roaster circles, смена веса и отсутствие чёрных углов native selector,
filter sheet draft/apply/cancel/reset, pagination/retry, native gallery/back,
safe area/tab bar, возврат из Roaster/similar Coffee и browser/share presentation.
Реальные browser/share actions, backend responses и production analytics не
проверялись; fake unit suites не сертифицируют эти сценарии.

Следующий владелец code-плана на этом checkpoint: **Profile / собственный и публичный профиль, account**.

### Checkpoint: Profile / own, public, edit, deletion — 2026-10-25

Продолжение PR #195, база `ca86168d` + этот срез. Свежие `origin/main=16724b7e`
и `origin/develop=8ac9f49a` сверены; обе ревизии уже входят в ветку. Сверены
native Profile tab, UserProfile/UserCheckIns/EditProfile/DeleteAccountPending routes,
ProfileScreen/OtherUserProfileScreen и legacy edit/deletion формы.

Сделано:

- Renderer/ports/username failure mapper перенесены из `androidMain/di/profile`
  в общий app DI с именами FeatureOwn/Public/AccountScreenRenderer и AppProfile….
  Зависимость profile impl перенесена в commonMain приложения; api/domain/data
  уже были общими. UI/VM и ресурсы остаются в `feature/profile`, нового backend
  или writer нет. Legacy screens остаются fallback, а shared slots не копировались.
- iOS-фабрики выбирают feature own/public/account entries; native Profile tab
  больше не вызывает legacy ProfileScreen напрямую. UserProfile/UserCheckIns,
  edit и deletion routes уже проходили через renderer. Удалены default arguments
  composable Content в feature API и PublicProfileScreenRenderer; modifier и режим
  checkInsOnly передаются явно, без Native partial-linkage stubs.
- Сохранены private/home personal activity calendar и weekly preview, Favorites
  preview, собственная полная история, contributions/blocked authors routes,
  канонические public share paths, фотографии и их preview/original policy,
  prefetch, author visibility/actions, followers и legacy-rendered cards.
  Подключение Profile не объявляет личную историю/Home/Favorites мигрированными.
- JVM LinkedHashMap/access-order/synchronized заменены атомарным snapshot-кэшем
  на StateFlow: последние четыре профиля, все строки каждого, refresh replacement,
  merge одновременных страниц. Порядок хранится списком: равные по содержимому Map
  не должны скрывать обновление recency. Есть проверки 500 строк, eviction,
  refresh, concurrent pages и повторного открытия неизменившегося профиля.
- Сохранён порядок выхода push → server → platform → drafts и login redirect
  после session end. Result CancellationException теперь пробрасывается в sign-out
  и username failure mapper; отмена не становится UI-ошибкой и не запускает остаток
  очистки. Поведение обычной ошибки logout сохранено. Проверки используют fake
  notifications/auth/settings/platform callbacks, без реальных действий аккаунта.
- Avatar picker и его payload прежние; заголовок теперь приходит из ресурсов
  feature через EditProfileSlots. Общий app bridge не содержит строку «Фото профиля».
  UIKit photo source/gallery/camera остаются существующими platform интеграциями.
- DS Scaffold/scroll padding сохраняют native tab clearance и нижнюю safe area
  pushed profile/edit/delete/check-ins. Back/share публичного профиля используют
  DS glass/native glyphs как legacy; menus идут через ранее подключённый UIMenu.
  Username/delete confirmation сначала используют DS native-alert hook с прежним
  Android fallback; ошибка deletion использует DS error dialog. Logout confirmation
  уже шёл через DS destructive sheet. Нового дизайна Android-диалогов не вводилось.
- AccountDeletionScreen разделён на runtime adapter и stateless content с малыми
  helpers; добавлены paired confirm/sent previews. Обновлены light/dark previews
  edit/check-ins/actions/username/error. Полные own/public экраны со слотовыми
  календарями/карточками требуют ручной приёмки; preview не объявляется их проверкой.
- Account/cache/mapper/sign-out tests перенесены в commonTest без JVM reflection.
  Profile ports/analytics parity остаются Android unit tests из-за JVM parity harness.
  Inventory analytics: прежний favorites_opened(entry_point=profile), session/
  consent guards и screen ownership сохранены; новых edit/deletion events в legacy
  нет. Test telemetry только in-memory, второго analytics service нет.

Проверки (Mac/JBR 21, 2026-10-25, база `ca86168d` + этот срез):

- Первый полный прогон прошёл: **192** unit/contract tests. Затем отдельный новый
  regression test подтвердил дефект recency в первой Map-based версии общего кэша:
  **5 tests, 1 failure** (`touchingAnUnchangedProfileUpdatesItsRecency`). После замены
  snapshot на упорядоченный список полный прогон повторён успешно; это исправление
  подтверждённого дефекта, не повтор случайно упавшего теста.
- Итог: Profile data **10 tests** / 1 suite, impl **60 tests** / 6 suites;
  domain task — без test sources. Design-system **44 tests** / 12 suites;
  app subset `*Profile*`, `*AccountPorts*`, `*UsernameChangeFailureMapper*`,
  `*RememberedCheckIns*`, `*NativeTabRoutes*` — **79 tests** / 17 suites.
  Всего **193**, 0 failures/errors/skips. Проверены server/DTO contracts,
  own/public/edit/deletion MVI, account switching/late results, payload/redirect,
  cancellation, cache concurrency/eviction, analytics parity. DS/feature suites
  в итоговом прогоне up-to-date после успешного первого выполнения; app subset выполнен заново.
- `assembleDirectDebug`, `assembleDirectSandbox`, packaged runtime-class checks
  обоих APK и Sandbox isolation прошли. Guards требуют три profile renderer
  classes и три entry factory classes. APK не устанавливались/не запускались;
  аккаунт, данные приложения и выбранный Studio Build Variant не менялись.
- `compileKotlinIosArm64`, `linkDebugFrameworkIosSimulatorArm64`,
  `compileTestKotlinIosSimulatorArm64` прошли. Общие profile factories и все четыре
  entry достижимы в Native link. Common app tests только скомпилированы для iOS,
  не выполнены на симуляторе. Swift app/signing в этом срезе не собирались.
- Gradle последовательно, `--max-workers=6`, логи вне tracked sources. Прежние
  Preview/expect-actual/framework bundle-ID warnings остаются. Первая Studio
  inspection кэша закончилась timeout; после финального изменения повторная
  inspection не нашла errors. Это не визуальная приёмка или тест запуска.
- Paired previews компилируются; rendering в IDE не проверялся. Автоматизированные
  UI/Compose/instrumentation/screenshot tests и их Activity/APK не создавались и не запускались.

Открытые gates: iPhone own/public/alias/guest/blocked states и обе темы/крупный шрифт/
VoiceOver, title/back/share/menu, personal activity sheet отдельно от личной истории,
followers/favorites/check-ins sheets и фото/cancel, safe area/keyboard, avatar picker,
username/deletion confirmations и системные ошибки. Live avatar/profile writes,
logout/push unregistration/account deletion, browser/share и production analytics
не выполнялись; нужны отдельные разрешённые проверки с безопасным аккаунтом.

Следующий владелец code-плана: **Contributions / списки заявок и редакторы**.


### Checkpoint: Contributions / заявки и редакторы — 2026-10-25

Продолжение того же PR #195 на базе `6cebcdb5`. Свежие после fetch
`origin/main=16724b7e` и `origin/develop=8ac9f49a` уже входят в ветку;
дополнительный merge не требуется.

Сделано:

- Общие account/catalog adapters, app-owned photo/map/price/haptic slots и
  `FeatureContributionsScreenRenderer` перенесены из Android composition в
  `composeApp/commonMain/di/contributions`. Android/iOS используют одну фабрику,
  существующий authenticated API client и отдельный upload client для signed PUT.
  Все шесть маршрутов уже проходят через app renderer в обоих navigation hosts.
  UI/ViewModel остаются в `feature/contributions/impl`; legacy fallback сохранён.
- Из шести API Content-методов убраны composable interface defaults; app adapter
  передаёт Modifier явно. Модули Contributions уже были common-зависимостями app;
  новых модулей, repository/session/analytics service или серверных endpoints нет.
- Для недельного расписания сохранён текущий UTC offset устройства в минутах,
  вычисляемый при операции, через kotlinx-datetime вместо Android java.time.
  App получает прямую dependency на уже используемую в проекте библиотеку:
  не зависит от скрытого legacy data helper. Чистые тесты покрывают знак,
  неполные часы и winter/summer DST; часовой пояс устройства не меняется.
- Чтение разрешённой позиции и reverse geocoding выполняются на Main.immediate
  для CoreLocation. Повторная проверка permission после чтения отбрасывает
  устаревшую позицию; ручная точка карты разрешается без доступа к GPS.
  CancellationException не превращается в пустую позицию/адрес.
- Сохранены существующие map picker/MapLibre, native photo-source/picker bridge,
  выбор цены и логотипы. Две строки заголовка цены перенесены в app resources
  единственного существующего locale; новых переводов не заявляется.
- Формы/списки/детали/статусы используют DS Scaffold, scroll-safe-area helpers
  и нижний clearance для fixed submit/error actions. Success bottom bars используют
  clearance Scaffold. Добавлены парные light/dark previews с фиктивным состоянием;
  длинная форма обжарщика и editor adapter разделены по смысловым блокам.
- Аналитика: сверены прежние app page mappings (contributions/shop_edit),
  route lifecycle и native screen owner остаются app-owned. В legacy Contributions
  ViewModel нет отдельных track events; новые события и второй сервис не добавлены.
  Реальная доставка screen/exposure events не проверялась.

Проверки этого среза (Mac/JBR 21, 2026-10-25):

- Domain: **16 tests / 2 suites**, data: **19 / 1**, impl: **65 / 7**;
  app subset `*Contribution*`, `*AppContributionLocation*`: **35 / 5**.
  Всего **135 tests**, 0 failures/errors/skips. Есть семь новых тестов location/time
  zone; пять catalog/account adapter tests перенесены в commonTest.
  Сохранены тесты account switching, drafts, submit guards, cancellation,
  upload/HTTP contracts, validation и parity gaps.
- Первый запуск остановился на app compilation: не было прямой зависимости
  kotlinx-datetime у composeApp. После добавления зависимости повторный прогон
  успешен. Первоначальная ошибка не скрывается повтором.
- Финальные `assembleDirectDebug` и `assembleDirectSandbox` прошли, включая
  packaged runtime-class checks и Sandbox isolation. Прежний порядок Android
  scroll padding сохранён; native bottom clearance добавляется внутри scroll content.
  Превью гостевого состояния находятся в том же файле, что и компонент.
- `compileKotlinIosArm64`, `linkDebugFrameworkIosSimulatorArm64` и
  `compileTestKotlinIosSimulatorArm64` прошли. Общие app tests на Native только
  скомпилированы, не исполнены. Приложения/APK не устанавливались и не запускались.
- Fingerprint APK (исходники этого checkpoint поверх `6cebcdb5`), SHA-256:
  Debug `7533a1805923a6fca565aebc60410612e3e5eae563e1dc484b5141c08a28a1ec`,
  Sandbox `7099815579348dbbc832e5cc3e8ca65e4e44f777fa4a243e2bdb56359bde2c02`.
- Studio inspection: AppContributionLocation — 0 errors. Модель IDE пока
  не разрешает common Compose/resources и новую datetime dependency; полноценная
  IDE/preview-проверка не объявляется пройденной. Реальные Gradle compile/link
  успешны. Есть прежние deprecation/expect-actual/framework bundle-id warnings.
  Все Gradle-запуски последовательные, `--max-workers=6`; логи локально в `/private/tmp`.

Открытые gates: ручной iOS-просмотр списков/статусов, всех шагов формы и секций
редактора, safe area/клавиатуры/крупного шрифта, back/cancel/draft при переходах,
MapLibre placement, permission denied/granted, фото-пикер/cancel и success/error
native feedback. Серверные create/update/upload, аккаунт и analytics acceptance
не выполнялись. UI/screenshot/instrumented тесты или test-only приложения
не создавались; установок и запусков приложения не было.

Следующий владелец code-плана: **Shop / кофейня, её формы и Shop-report**.


### Checkpoint: Shop / кофейня, публичные чекины, создание и Shop-report — 2026-10-25

Продолжение PR #195 на базе `dd8c7a5e`. После fetch
`origin/main=16724b7e`, `origin/develop=8ac9f49a`; оба ref уже предки HEAD.

Сделано:

- Shop api/domain/data/impl и Shop-report api/data/impl перенесены из Android
  в common app dependencies. Общие renderer, navigation-owned session hosts,
  author blocking, server favorites adapter, draft bridge, photo picker и analytics
  находятся в app composition. iOS DI выбирает те же feature entries, что Android.
  Экранный UI и бизнес-логика остаются в feature; новых модулей или клиентов нет.
- Подключены ShopDetail, ShopReviews (публичные чекины), ShopMenuGallery,
  ReportShop и modal создания чекина. Маршруты уже проходили через app renderer
  в обоих navigation hosts. Native root/navigation stack не заменены.
  ReportCheckIn/ReportReview остаются на прежнем общем ReviewReportScreen:
  контентная жалоба не объявляется мигрированным Shop-report.
- Сохранены authenticated CpImage, original/thumbnail policy, photo viewer,
  prefetch списка, lifecycle-aware подпись даты, расстояние, copy/share/link,
  выбор Apple/Google/Яндекс карт на iOS, переходы к обжарщику и автору.
  Снятую ранее кнопку «Фото меню» в новом detail не возвращали. Legacy Android
  gallery affordance передаётся platform slot, не копируется в общий feature UI.
- У двух Shop API Content-методов убраны default arguments; list adapter явно
  передаёт refreshGeneration. Эффекты детали разбиты на небольшие app-owned
  navigation/platform helpers, состояние формы сохраняется через прежний saver.
- Shared browsing/modal leases сохраняют обновление токена того же аккаунта,
  отзываются при замене аккаунта/visibility и отменяют устаревшие операции.
  Черновик использует прежний единый CheckInDraftStore; Java Instant в его адаптере
  заменён Kotlin Instant без смены timestamp/storage format или приватности.
- Android сохраняет свой проверенный formatter с явным ZoneId; iOS использует
  существующие platform DateTimeUtils через AppShopCheckInDateFormatter.
  Сохраняются UTC-midnight picker token, локальный день, время/миллисекунды,
  поведение DST и ограничение будущего визита. Четыре новых common tests проверяют
  invalid/offset-equivalent input, удержание точного instant и clamp.
  Schedule UTC offset в общем browsing module вычисляется через kotlinx-datetime.
- JVM synchronized в single pending favorite intent заменён AtomicReference.
  Найденный дефект: смена аккаунта во время загрузки отложенного избранного могла
  отправить запись в новую сессию. Новый тест сначала воспроизвёл лишний save.
  Теперь intent захватывает lease наблюдавшегося аккаунта до запуска дочерней
  операции; чтение/запись и onApplied проверяют владельца. Отмена операции
  сохраняется, collector сессии продолжает жить; второй сервис/локальный writer
  не создан, TTL/однократное выполнение и resumed-after-auth analytics сохранены.
- Shop detail/list/gallery/report используют DS Scaffold и scroll bottom
  clearance; floating actions/snackbar добавляют native safe area один раз.
  Android viewport padding остаётся прежним. Native sheet уже потребляет нижний
  inset и сохраняет существующий photo source/date picker; общую sheet механику
  в этом срезе не меняли. Парные light/dark previews остаются рядом с content;
  session-error preview адаптирован к common tooling.
- Аналитика перенесена вместе с app adapters, не реализована заново:
  shop_action, shop_content_interacted, auth gates, favorite attempt/resume,
  entity detail/exposure, check-in tracker/handoff, field/submit/error/save/cancel.
  Consent/session guards, безопасная классификация photo-upload ошибок и
  прежние app page mappings сохранены; реальные события не отправлялись.

Проверки (Mac/JBR 21, 2026-10-25; исходники checkpoint поверх `dd8c7a5e`):

- Shop domain **7 tests / 3 suites**, data **42 / 13**, impl **106 / 12**;
  Shop-report data **9 / 2**, impl **6 / 1**; app subset `*Shop*`,
  `*CheckInDate*` — **109 / 35**. Всего **279 tests**, 0 failures/errors/skips.
  Семь чистых app suites перенесены в commonTest; JVM date/DI/analytics harness
  остались androidUnitTest. Проверены lease replacement, draft identity,
  cancellation, HTTP/upload contracts, votes/favorites и analytics wire parity.
- Первый compile остановился на двух app helpers, оставшихся Android-only.
  Lifecycle date-label helper перенесён в common composition; legacy gallery
  action остаётся platform slot. После исправления Android/Native compile успешны.
- Воспроизведение гонки: **3 tests / 1 failure** (лишний `shop-1` в saved после
  смены аккаунта). После lease fix весь app subset успешен; исходный failure сохранён.
- Первая assembly остановилась на DEX guard со старыми Android* именами трёх
  переименованных классов. Guard обновлён на реальные общие имена и дополнен
  detail/list/report/gallery renderers; проверка не отключалась.
  Финальные assembleDirectDebug/Sandbox, runtime-class checks и Sandbox isolation
  успешны. Новые библиотеки в APK проверены, APK не установлен.
- compileKotlinIosArm64, linkDebugFrameworkIosSimulatorArm64 и
  compileTestKotlinIosSimulatorArm64 успешны. Native app tests только скомпилированы;
  simulator/device UI и Xcode приложение не запускались.
- SHA-256: Debug `853c447bdd3989e615da8f65351151e1ba100e66a2633ac7a9927d9290488eb8`;
  Sandbox `7b6bc0117ef5b1f5397dc91b90abaf99ecc56f4854bfa3cdc6db74677fa84a09`.
- Studio common source model ещё не разрешает новые зависимости Shop; ошибки
  IDE classpath не объявляются успешной inspection/preview-приёмкой.
  Реальные Gradle compile/link успешны; прежние preview/expect-actual warnings
  остаются. Gradle последовательно, --max-workers=6, логи локально в /private/tmp.

Открытые gates: iOS detail/list/gallery в обеих темах, размер шрифта/экрана,
положение floating controls и sheet, клавиатура, свайп/back/cancel,
реальный photo/camera picker и даты около полуночи/DST на устройстве,
auth→favorite resume, авторские меню/блокировка, карты/share/copy.
Create/upload/vote/favorite/report и настоящая аналитика на сервере не выполнялись.
Ручная проверка Sandbox/устройства не заменяется 279 unit/contract tests.
Автоматизированные UI/instrumented/screenshot tests и test-only Activity/APK
не создавались. Пользовательские приложения/аккаунт/данные не менялись.

Следующий владелец code-плана: **Check-in history / личная история и её карточки**;
затем Home check-in и общий create flow. Контентная жалоба остаётся явным
legacy follow-up, удаление fallback до общей приёмки не допускается.

## 1. Принципы (решение 2026-10-24)

- **iOS остаётся как в проде.** Нативная оболочка: `UITabBarController`, пять `UINavigationController`-стеков, `NativeScreenController`
  (инсеты, жест «назад», закрытие клавиатуры). Нативные шторки, алерты, меню, карта MapLibre (Swift), Apple/Google вход (Swift), push, фото-пикер.
  Это платформенная инфраструктура, она не legacy и не удаляется.
- **Общие слои:** `domain`, `data`, ViewModel (MVI-состояние) используют обе платформы. Полный SwiftUI — план на будущее, сейчас не делаем.
- **Один большой PR с миграцией.** Внутри — коммиты по этапам и владельцам. Старый legacy-код **удаляем только когда миграция готова целиком**
  и проверена (см. этап 5). До этого `Legacy*Renderer` остаются запасным путём.
- **Проблемные места не остаются в legacy.** Нужен нативный элемент — делаем его реализацию в `iosMain` и подключаем к новому UI через
  design-system, а не оставляем старый экран.

## 2. Что показала сверка с продом

### 2.1 Сборка
- Compose-компилятор выключен для iOS в `kmp.shared-android-compose-library` (`KmpSharedAndroidComposeLibraryPlugin.kt:14`).
  Так собраны impl/api: coffee, contributions, home-check-in, profile, notifications, settings, community, auth, app-update, discovery.
  Их UI лежит в `androidMain` и под iOS не соберётся.
- favorites, shop, shop-report: UI в commonMain, но плагин `kmp.android-compose-library` без iOS-таргетов.
- check-in-history: iOS-таргеты есть, но 24 файла UI в `androidMain`; api без iOS.
- Образец для остальных — roaster: `kmp.shared-library` + `kmp.android-compose-library`, UI и `composeResources` в commonMain.
- Строки в `android res/values` у десяти модулей надо перенести в `composeResources`.

### 2.2 Нативные хуки недоступны новому UI (главная находка)
Хуки лежат в `composeApp` как `expect/actual`, а `feature/*` зависят только от `core/design-system`. В design-system из нативного есть только
`CoffeePeekGlassButton.ios` и `CoffeePeekSelectMenu.ios` (UIMenu). Поэтому, если просто включить новый UI на iOS, он получит Material-аналоги:

| Нативный хук в проде (composeApp) | Что в новом UI на iOS | Где нужен |
|---|---|---|
| `NativeModalSheet` (через `SwipeDismissModalBottomSheet`) | Material `ModalBottomSheet` | фильтры, формы чекина, уведомления, правка/жалоба, профиль |
| `NativeDatePicker` (колёса UIDatePicker) | Material `DatePicker` | форма чекина кофейни, home-редактор |
| `PlatformAlert`, `NativeActionSheet`, `NativePhotoSourceSheet` | Compose-диалоги и шторки | удаление, выход, ошибки, выбор фото, маршрут |
| `PlatformScrollToTop` + haptic в pull-to-refresh | в core нет | списки: лента, поиск, кофе |
| `NativeNavigationBar` через `CpTopBar` | Material `CenterAlignedTopAppBar` (дубль заголовка) | все pushed-экраны |
| `PlatformScrollableColumn` (IME) | `verticalScroll` | формы, auth, настройки |
| `NativeKeyboardDoneAccessory`, `platformAuthKeyboardOptions` (AutoFill) | только `KeyboardOptions` | числовые поля, логин и регистрация |
| `PlatformMenuButton`/UIMenu, `PlatformGlassIconButton` | `CoffeePeekActionMenu` только Android; Haze-стекло | меню профиля и чекина, кнопки «назад/поделиться» |
| `PlatformSwitch`, `PlatformActivityIndicator`, haptics | `CoffeePeekSwitch`, `CoffeePeekLoader`, Compose-haptics | настройки, календарь, успех |

Уже нативное и подходит как есть: `CoffeePeekSelectField` (UIMenu на iOS). Home-редактор использует `DropdownMenu` для способа заваривания — перевести на него.

### 2.3 Адаптеры и вкладки
- iOS-пар к Android-адаптерам нет: слоты и порты profile, community, discovery, coffee, contributions, shop, history, home-check-in, settings, favorites.
  Есть общие кусочки: `ImagePicker.ios`, `PhotoSourceSheet.ios`, `FullScreenImageDialog.ios`, `ShareHelper.ios`, `OpenInBrowser.ios`,
  `ShopRouteOptions.ios`, `PlatformLocation.ios`, `DateTimeUtils.ios`, `PostHogPlatform.ios`, карта `CoffeeMap.ios` + Swift MapLibre.
- `PlatformKoin.ios.kt` вызывает только `initKoin()`, рендереры не подменяются: iOS получает `Legacy*Renderer`.
- Вкладки iOS (`PlatformRootNavigation.ios.kt`, ~150–162) вызывают legacy напрямую, мимо рендерера: **Coffee, Community, Profile, Settings**
  (Discovery и колокольчик уведомлений — через рендерер). Для переключения этот файл надо править.
- Pushed-экраны идут через `Navigator.kt` и рендереры, их достаточно подменить в Koin.

### 2.4 Вердикты по владельцам
| Владелец | Что можно общим Compose | Что нужно сначала | Что остаётся нативным |
|---|---|---|---|
| App update | prompt-диалог (чистый Compose `Dialog`) | перенос из androidMain | `StoreUpdateInstaller` (App Store через `OpenInBrowser`) |
| Notifications | колокольчик и список | `NativeModalSheet` для шторки | `PushAppDelegate`, `NativePushBridge` |
| Settings | все 4 экрана | native header, `PlatformScrollableColumn`, iOS-адаптеры тем/города/обновлений/внешних действий, вкладка через рендерер | `ThemeManager` ↔ UIKit-стиль |
| Auth | макеты и ViewModel | **вход через Apple отсутствует в новом домене** (нужны challenge/login/`SignInMethod.Apple`), iOS-мост Google, AutoFill-типы, glass back/close | `SocialSignInProvider.swift`, Apple/Google actuals |
| Discovery | список и поиск | host: `mapCanvas`, `retained`, `haptic`, `reduceMotion`, `locationPermission`; шторка фильтров; scroll-to-top и haptic | карта (`CoffeeMap.ios` + Swift MapLibre) |
| Coffee | список и деталь | шторка фильтров, scroll-to-top, native header, share/фото/браузер через адаптеры; вкладка через рендерер | `NativeScreenController`, edge-back |
| Contributions | список, предложение, детали заявки | для форм: фото-пикер, алерты, Done-аксессуар, time picker, haptics; busy-блокировка edge-back | карта выбора места |
| Profile / Edit / Delete | экраны и ViewModel | слоты (`ProfileSlots`, `EditProfileSlots`), нативные меню, алерты, шторки, share, header; вкладка через рендерер | action sheet выхода, photo source sheet, viewer |
| Community | `PostCard`, ViewModel | host (image, viewer, prefetch), `NativeModalSheet`, scroll-to-top, haptic, меню; вкладка через рендерер | память фото ленты (`IosImageBitmapDecoder`) |
| Favorites | весь экран (самый простой) | iOS-таргеты, Koin-адаптер, решение по header; продуктово «отложено» | — |
| Shop (detail, list, gallery) | содержимое | iOS-таргет, iOS `ShopDetailScreenRenderer` (маршрут, viewer, header) | `NativeActionSheet` маршрута, viewer |
| Shop check-in form | поля | нативный date picker, фото-sheet, host шторки, Done-аксессуар, haptic успеха | `NativePhotoSourceSheet` |
| Shop report | весь экран | iOS-таргет; **F9: классификация ошибок Darwin** | — |
| Check-in history | таймлайн и календарь | UI из androidMain в commonMain, iOS-таргет api, шторки, iOS-haptic | header, viewer |
| Home check-in | деталь | редактор: date picker, алерт удаления, brew method на `SelectField`, фото-sheet; общий create flow | `ImagePicker.ios`, viewer |

## 3. Гейты безопасности (не UI)
- **F24:** `write(null)` в Keychain игнорирует результат `SecItemDelete` и публикует null (`PlatformSessionSecureStore.ios.kt`).
- **F23:** при secure=A и Room SESSION=B после logout следующий store импортирует B; сохранять приоритет secure store.
- **F9:** классификация pre-send ошибок Darwin для жалобы на кофейню; не считать любую Darwin-ошибку безопасной для повтора.
- Koin native-линковка (TestFlight-старт, `composeApp/IOS_STARTUP_FIX.md`): фикс не проверен на устройстве.
- Core session port вместо `SessionRepository` (~43 файла) — первый блокер удаления legacy.

## 4. Порядок работ (внутри одного большого PR)

### Этап 0. Сборка
- Перевести impl/api на сочетание как у roaster (или включить Compose для iOS в общем плагине, только если решим иначе);
  добавить iOS-таргеты favorites, shop, shop-report, check-in-history api, home-check-in api.
- Перенести UI из `androidMain` в `commonMain`, строки в `composeResources`, убрать `R.*`, tooling-preview, `java.*`, `String.format`, `BackHandler`/`Context`.
- Подключить модули в `iosMain.dependencies`. Критерий: `:composeApp:compileKotlinIosSimulatorArm64` проходит, Android без изменений.

### Этап 1. Нативный слой в design-system
Перенести из `composeApp` в `core/design-system` общие `expect/actual` с iOS-реализацией в `iosMain` и подключить к компонентам `CoffeePeek*`:
шторка (`NativeModalSheet`), пикер даты, алерт и action sheet, photo source sheet, scroll-to-top и haptic в pull-to-refresh, native header в `CoffeePeekTopBar`,
scrollable column с IME, Done-аксессуар и content types, UIMenu-меню, glass back/close, switch и индикатор. Поведение Android не меняется.

### Этап 2. Сессия и безопасность
F24, F23, core session port.

### Этап 3. iOS-адаптеры и Koin
Для каждого владельца: iosMain-пары слотов и портов (фото, черновики, даты, аналитика, session lease, hosts), iOS-привязка рендереров в Koin,
переключение вкладок Coffee, Community, Profile, Settings на рендереры. Auth: Apple-вход в новом домене.

### Этап 4. Владельцы по порядку (каждый — приёмка на симуляторе и устройстве)
1. Roaster — довести (Xcode-линковка, симулятор, устройство).
2. App update, Notifications, Settings.
3. Auth (после Apple-входа).
4. Community, Discovery, Coffee.
5. Profile, Contributions.
6. Shop, Shop-report (после F9).
7. Check-in history, Home check-in, общий create flow.
8. Favorites — по продуктовому решению.

### Этап 5. Удаление legacy (когда всё выше готово и принято)
`Legacy*Renderer` и их экраны/ViewModel/репозитории в `composeApp`, затем `modules/home-checkin`, `data`, `network`, `room`, `domain`,
группа legacy в `Modules.kt` и Koin-обвязка. Навигация (Navigation 3) — отдельное решение.

## 5. Чеклист на каждый срез
- [ ] Карта точек входа на iOS; нет прямых вызовов legacy мимо рендерера (вкладки!).
- [ ] UI и ресурсы совместимы с Kotlin/Native.
- [ ] Каждый нативный хук из таблицы 2.2 подключён или осознанно заменён.
- [ ] Один владелец инсета на край (`NativeScreenController`), прозрачные нативные вьюхи с `placedAsOverlay`, фон контроллера закрашен.
- [ ] Реальные iOS-порты: фото, карта, браузер, haptics, разрешения, вход.
- [ ] Аналитика: `screenShown` не дублируется с `NativeScreenController`.
- [ ] Отмена корутин и классификация ошибок Darwin проверены.
- [ ] Серверное избранное не затрагивается выходом.
- [ ] Юнит-тесты, Android assemble, компиляция iOS.
- [ ] Ручной проход на симуляторе и устройстве записан здесь.

## 6. Что не делаем
- Не переключаем все рендереры разом и не повторяем Roaster.
- Не считаем Android-only баги (даты, календарь) iOS-багами без проверки.
- Не удаляем legacy до полной готовности миграции.

## 7. Не проверено сверкой
- Пересылает ли `CoffeePeekTopBar` заголовок в `UINavigationItem` на iOS (сейчас нет).
- iOS-привязки `ShopMenuGalleryScreenRenderer` и `ShopCheckInListScreenRenderer` (предполагается тот же паттерн).
- Работа аналитики экспозиции вкладок внутри `NativeScreenController`.
- Как `iosApp` линкует пакет `iosXtool`.
- Любое поведение на симуляторе и устройстве.
