# План миграции iOS на новые feature-модули

Статус: принципы согласованы 2026-10-24; ход работ — в разделе «Статус» ниже. Основан на сверке с продом (`origin/main`) по всем владельцам: инвентарь Gradle-модулей,
iOS-документы (`docs/IOS_NATIVE_EXPERIENCE.md`, `IOS_NATIVE_NAVIGATION.md`, `IOS_COMPOSE_INTEROP_AUDIT.md`, `IOS_MAP_PARITY.md`,
`IOS_FEED_PHOTO_MEMORY.md`, `IOS_SETUP.md`), `feature/LEGACY_REMOVAL.md`, `feature/roaster/IOS_MIGRATION_PLAN.md`.
Первоначальный инвентарь не был проверен запуском UI на симуляторе или устройстве.
Фактически выполненные проверки последующих срезов и оставшиеся gates записаны ниже отдельно.

## Статус на 2026-10-25 (ветка `feat/ios-build-foundation`, PR #195 → `develop`)

В рабочую migration-ветку также включены изменения develop `105b8994`; детали
разрешения конфликтов и проверки объединённого кода — в checkpoint синхронизации ниже.

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

**Этап 2 — сессия: F23 и F24 готовы** (выход стирает старые строки; сбой удаления Keychain сообщается). Активные app-адаптеры переведены с `SessionRepository` на core `SessionReader`; 13 файлов legacy fallback и старый data/writer bridge сохранены до приёмки.

**Этап 3 — подключение на iOS: начат.** Через новые модули на iOS подключены: вкладка Settings (и три её подэкрана), окно обновления, колокольчик уведомлений, экраны входа, регистрации и восстановления пароля, **Community (вкладка «Лента»)**, **Discovery (вкладка «Поиск»)**, **Coffee (вкладка каталога и детали кофе)**, **Profile (собственный/публичный профиль, его чекины, редактирование и подтверждение удаления аккаунта)** и **Contributions (списки заявок, добавление кофейни/обжарщика, предложение правки, редактор и детали заявки)**, **Shop (кофейня, публичные чекины, галерея меню, форма чекина)** и **Shop-report (неточность в данных кофейни)**, **Check-in history (личная история, календарь и Home detail/editor внутри неё)** и **общий create flow (выбор типа + Home create)**, **выбор кофейни для чекина**, **личная Activity шторка и недельный обзор Profile**, **контентные жалобы на чекин/совместимый отзыв**. Подключены также Google/Apple кнопки и существующие Swift-провайдеры (при доступной конфигурации); реальный OAuth ещё не проверен.
На iOS Profile, Community, Discovery, Coffee, Contributions, Shop/Shop-report и личная история идут через feature-рендереры.
В истории подключены Home detail/editor; общий выбор типа, Home create и выбор кофейни тоже используют feature entries.
**Favorites: полный экран и собственное превью Profile используют feature entry на обеих платформах** с тем же серверным источником.
Личная Activity использует отдельный entry истории; публичные карточки избранного/чекинов в Profile сохраняют существующие app slots.

**Важные находки для следующих срезов**
1. Kotlin/Native не линкует переопределение composable-метода интерфейса с аргументом по умолчанию (`modifier: Modifier = Modifier`): ошибка видна только при линковке и только у достижимого кода. У каждого владельца при подключении убрать значения по умолчанию из api-интерфейсов (так сделано у Settings и Auth).
2. Подключение владельца требует: перенести platform-neutral адаптеры из `androidMain` в `commonMain` (как у Settings) или добавить iOS-пары, перенести зависимость `impl` в `commonMain.dependencies` composeApp и добавить фабрику в `PlatformKoin.ios.kt`.
3. **Auth Google/Apple: реализация и offline-проверки готовы** (см. checkpoint ниже): домен challenge/login, `AppleAuthorizer` поверх существующего native provider, Google launcher поверх `IosGoogleAuthBridge`, кнопки и тесты. Реальный OAuth, signing/configuration и ручная iOS-приёмка остаются release gate; компиляция не закрывает их.
4. Для сообщения проверки обновлений на iOS нет тоста: временно используется `ErrorHandler.showError`, нужно решение по UX.

**Остаётся**
- Этап 3: выбранные feature entries подключены, включая Favorites full screen и own-profile preview. Остались ручная приёмка всех владельцев и live-приёмка Apple/Google; публичные profile card slots и platform image/prefetch/viewer adapters остаются app-owned.
- Ручная приёмка отдельной Activity шторки и недельного обзора Profile ещё нужна; public-only календарь Profile не получает private reader.
- Ручная приёмка контентных ReportCheckIn/ReportReview ещё нужна; они подключены к content-report, отдельно от Shop-report.
- Этап 2: активные app-потребители используют core session port; удаление legacy session bridge вместе со старыми VM — после приёмки, не часть механической замены типов.
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


### Checkpoint: личная история / календарь и Home detail-editor — 2026-10-25

Продолжение PR #195 на базе `b348d37c`. Свежие после fetch
`origin/main=16724b7e`, `origin/develop=8ac9f49a` уже входят в ветку.

Сделано:

- Check-in history api/domain/data/impl и Home api/domain/data/impl — общие
  app dependencies. iOS DI выбирает FeatureCheckInHistoryScreenRenderer для
  VisitedPlaces и EditHomeCheckIn(id); Navigator/root stacks не заменены.
  Это полная личная история, а не preview Activity в Profile.
- Account lease, navigation-owned host/store, Home destination bridge,
  read adapters, timeline presentation bridge, photo preview и creation
  analytics перенесены в common app composition. UI/MVI остаются в feature-модулях.
  Существующие карточки/visibility labels и authenticated CpImage подключаются
  через прежние app slots, не рисуются заново. DTO/API endpoints не менялись.
- Для общей истории нужен Home detail/editor: общие catalog/repository/photo/
  analytics adapters и фабрика редактора переиспользуют те же новые feature entries.
  AppHomeCheckInDates использует Kotlin Instant/LocalDate вместо java.time,
  прежние platform date/label/method helpers и UTC-midnight picker token.
  Home reader и writer — один existing feature repository, без legacy writer.
- Home data assembly выделена в common homeCheckInDataModule; Android global
  creation module включает её. iOS включает только data + history assembly.
  Global create flow на iOS в этом checkpoint не переключён; FAB истории
  создаёт Home через свой bridge, а выбор кофейни использует прежний global route.
- AppCheckInHistoryCalendar использует kotlinx-datetime и один captured zone/today
  на snapshot. Границы месяца вычисляются по правилам зоны для каждой даты,
  а не по текущему offset. Bare/offset server timestamps читаются через existing
  core parseServerInstant. Сохранены leap year, граница года, смена зоны/полуночи,
  spring/fall DST и первый допустимый час при переводе часов в полночь.
- Сохранены revoke-on-account-change, same-account token renewal, отмена поздних
  чтений/операций, очистка VM stores/фотографий, монотонный contentGeneration,
  initial Home id consumption и identity guards старых sheet callbacks.
  Неизвестная публичность/неподтверждённая доставка не превращаются в Public/success.
- ViewModelStoreOwner задаётся **внутри** Home sheet body: UIKit создаёт отдельный
  Compose root и не наследует внешний provider. Не переносили весь parent
  CompositionLocalContext с его UIViewController в новый контроллер.
  Session/photo-preview/destination/dismiss-allowed memory flows app host
  наблюдаются до disposal, даже когда UIKit перекрыл presenting screen.
  Это сохраняет смену/закрытие sheet, Saved и privacy updates при остановке
  lifecycle родителя; загрузчики feature по-прежнему lifecycle-aware.
- Из History API убраны три composable defaults, из Home editor/detail — modifier
  и notice defaults, из app History renderer — default initialHomeId.
  App callbacks/параметры передаются явно; Kotlin/Native framework линкуется.
- History Scaffold/FAB/list используют DS safe-area helpers; Android padding
  сохранён, native нижний inset учитывается один раз. Home body остаётся внутри
  app-owned native sheet с её собственными inset/keyboard/dismiss правилами.
  Подтверждение удаления Home использует существующий DS native alert hook,
  Material fallback Android сохранён. Добавлены colocated light/dark preview pairs.
- Analytics adapters переехали вместе с владельцем: type-picker attempt/open/
  cancel, tracker handoff, Home editor fields/validation/submit/stage errors/
  saved/cancel и exposure modifiers сохраняют consent/session guards.
  Нового сервиса/пользовательских свойств/production test events нет.

Проверки (Mac/JBR 21, 2026-10-25; исходники этого checkpoint поверх `b348d37c`):

- History domain **8 tests / 3 suites**, data **11 / 2**, impl **54 / 4**;
  Home domain **14 / 2**, data **35 / 5**, impl **71 / 3**;
  app subset `*History*`, `*HomeCheckIn*`, `*HomePreparation*` — **105 / 23**.
  Всего **298 tests**, 0 failures/errors/skips. Общие tests исполнены на JVM.
- Восемь app suites и Home read fixture перенесены в commonTest; calendar tests
  используют injected Kotlin Instant/TimeZone и не меняют часовой пояс компьютера.
  Добавлены четыре common Home date tests: leap day, invalid civil dates,
  сохранение нечитаемой даты для отображения и round trip выбранного дня.
  JVM-specific DI/MockEngine/analytics harness остаются Android unit tests.
- Первый прогон остановился на Native common-test compilation: Home read fixture
  оставалась в androidUnitTest. После переноса фикстуры повтор успешен; исходный
  failure не скрыт. Main-код на обеих платформах компилировался.
- assembleDirectDebug/Sandbox, packaged runtime-class checks и Sandbox isolation
  прошли. Список runtime guards обновлён на общие имена и включает calendar/data
  assembly. APK не устанавливались, приложения/аккаунт/данные не менялись.
- compileKotlinIosArm64, linkDebugFrameworkIosSimulatorArm64 и
  compileTestKotlinIosSimulatorArm64 успешны. После review UIKit owner/lifecycle
  сделана финальная повторная сборка. SHA-256 итоговых APK:
  Debug `757de53c20f23c6dabbe6b8a2c769989817ee3a8dd552ffc1b5682091bb1012c`;
  Sandbox `848ce1ded99441c29ccbe35e4c8883f001f3f2e1ea458b46b4fb46a0f5292af0`.
  Native app tests только скомпилированы; UI/Xcode приложение не запускались.
- Studio inspection пока не разрешает moved feature/datetime dependencies в
  common source model. IDE/preview-приёмка не объявляется успешной; реальные
  Gradle compile/link успешны. Прежние preview/expect-actual warnings остаются.
  Gradle последовательно, --max-workers=6; логи локально в /private/tmp.

Открытые gates: ручная iOS-проверка timeline/calendar, dots/photos и нескольких
чекинов за день, фильтров/месяцев/pull-to-refresh/paging, font/theme/window sizes,
native FAB/keyboard/sheet placement, Home detail→edit→detail и close/reopen,
photo source/picker/cancel/viewer, account replacement/late callbacks при открытом
нативном child и suspend/resume, native delete-confirm cancel/confirm.
Create/update/delete/upload и live analytics acceptance не выполнялись.
Автоматизированные UI/instrumented/screenshot tests и test-only Activity/APK
не создавались. Compile/link не закрывают эти ручные и серверные gates.

Следующий code-срез: **общий create flow и оставшиеся входы Home check-in на iOS**.
Activity calendar в Profile и контентные ReportCheckIn/ReportReview остаются
явными legacy follow-ups; Favorites — по решению продукта. Legacy fallback
до общей приёмки не удаляется.


### Checkpoint: общий выбор типа чекина и Home create — 2026-10-25

Продолжение PR #195 на базе `5fc61bef`. После fetch
`origin/main=16724b7e` и `origin/develop=8ac9f49a` уже предки HEAD.

Сделано:

- iOS включает общий HomeCheckInModule и FeatureCheckInCreateFlowRenderer.
  Реальные входы сверены: CreateAction ленты и CheckInTabContent вкладки Coffee
  уже вызывают CheckInCreateFlow через app renderer. Старый renderer лениво
  создавал legacy Home VM; новый не вызывает legacyContent и использует тот же
  feature Home editor/repository, что и история. Root navigation не заменён.
- GlobalCheckInFlow и его retained child store перенесены в common app composition.
  Модель координирует только lease, type picker, tracker handoff и terminal
  callback; поля/валидация/каталог/фото/HTTP writes остаются в Home feature.
  Guest/session failure закрывает flow, старые callback identity не действуют,
  same-account token renewal сохраняется, accepted Saved и Shop доставляются один раз.
- Настоящая Android-специфика LocalActivity/isChangingConfigurations выделена в
  маленький expect/actual lifetime effect: Android сохраняет child store при
  пересоздании Activity, iOS очищает при окончательном disposal. Закрытие/повторное
  открытие получают свежие flow/editor VM, лишний persistent store не добавлен.
- Как в истории, ViewModelStoreOwner задаётся внутри body UIKit sheet.
  In-memory destination/dismiss-allowed наблюдаются до disposal при перекрытии
  родительского контроллера. Completion остаётся lifecycle STARTED-bound,
  использует latest callbacks и claim: modal исчезает после Saved/revoke,
  затем безопасно доставляется один terminal outcome.
- Type-picker UI находится в feature/home-check-in: узкий CheckInCreatePickerEntry,
  runtime adapter с rememberSaveable selection и stateless content. Первый
  выбор — кофейня; простой выбор строки не навигирует, действует «Продолжить».
  Строки/порядок/размеры прежнего picker сохранены; семь строк вынесены в
  resources единственного существующего locale (values, русский).
- Повторяющаяся карточка выбора выделена в DS CoffeePeekSelectionCard и используется
  новым глобальным picker и существующим picker истории. DS не знает Shop/Home:
  получает title/description/icon/selected/callback. Сохранены 16dp corners,
  72dp minimum, padding/border/theme colors, gold icon badge и radio semantics.
  Парные light/dark previews — рядом с компонентом и content. Legacy picker
  оставлен в composeApp как fallback; нового бизнес-UI туда не добавлено.
- Root DI сгруппировала две check-in фабрики в CheckInRendererFactories:
  один app-owned набор lazy callbacks для истории/создания, одинаковый на
  Android/iOS. Разрешение одного экрана не конструирует другой; legacy defaults
  сохранены. Отдельного Gradle integration-модуля нет.
- Analytics attempt остаётся у исходной floating action; type select, cancel,
  Home field/submit/save/error и Shop tracker handoff используют прежний сервис.
  Exposure modifier type picker передаётся из app composition, SDK не добавлен
  в feature. Native back Shop route сохраняет CheckInFlowHandoff.cancelFromBack.
  Consent/session guards и absence of duplicate attempt/cancel покрыты существующими
  in-memory tests; настоящие события не отправлялись.

Проверки (Mac/JBR 21, 2026-10-25; source checkpoint поверх `5fc61bef`):

- Home impl **71 tests / 3 suites**, History impl **54 / 4**;
  app subset `*GlobalCheckIn*`, `*RetainedViewModelStore*`, `*HistoryCreation*`,
  `*HomeCheckIn*`, `*CheckInHistoryModule*`, `*CheckInRendererFactories*` —
  **60 / 12**. Всего **185 tests**, 0 failures/errors/skips.
- Global flow store lifetime suite перенесена в commonTest (4 tests).
  JVM reflection/analytics harness остаются Android unit tests. Новый чистый
  DI test проверяет отсутствие eager history resolution при открытии создания.
  Native общие app tests скомпилированы, не исполнены.
- assembleDirectDebug/Sandbox, packaged runtime-class checks и Sandbox isolation
  успешны. Guard использует общие flow имена и новые picker/factory classes.
  compileKotlinIosArm64, linkDebugFrameworkIosSimulatorArm64 и
  compileTestKotlinIosSimulatorArm64 успешны; partial-linkage errors не скрывались.
  Gradle последовательно, --max-workers=6. Предыдущие preview/expect-actual warnings
  остаются. UI/IDE previews не рендерились.
- SHA-256 APK: Debug `0a0582ca354807b43b6b1ee8f5ab983de132beb6acf4443283c00a9a132a422d`;
  Sandbox `5b13f94cca9356e62857638fec496901c812851361a4f98b403060a08d7a66e6`.
  Установок, запуска приложений, UI/instrumented/screenshot tests или test-only
  Activity/APK не было. Пользовательские аккаунты/данные не менялись.

Открытые gates: ручная iOS-проверка входов Coffee/Community, radio/Continue,
размеры шрифта/темы, UIKit sheet height/keyboard, Home cancel/save/busy dismissal,
смена аккаунта/foreground/background, закрытие/повторное открытие, фото-picker,
переход в выбор кофейни и Native back/cancel tracker. Реальные записи, загрузки,
OAuth, analytics и Xcode/signing не выполнялись.

Следующий code-срез: **выбор кофейни для чекина**. Screen.CreateCheckIn в обоих
navigation hosts пока открывает общий legacy CheckInShopPickerScreen/FeedViewModel
(список, поиск, nearby/map). Передача в уже мигрированный Shop detail/form сохранена;
этот экран не объявляется перенесённым лишь из-за готового выбора типа.
Затем остаются Profile Activity preview, контентные жалобы и Favorites по решению
продукта; удаление legacy — после общей приёмки.

### Checkpoint: выбор кофейни для чекина — 2026-10-25

Продолжение PR #195 на базе `0c52b0fa`, ветка `feat/ios-build-foundation`.
После fetch `origin/main=16724b7e` остаётся предком HEAD. `origin/develop`
продвинулся до `5a1fe4a1`; его параллельные изменения notifications/photo attachments
в этот срез не включались. Нового checkpoint PR не создавали.

Сделано:

- Реальные входы `Screen.CreateCheckIn` в обоих hosts Navigator теперь разрешают
  CheckInShopPickerRenderer. Android/iOS используют одну lazy factory из
  CheckInRendererFactories; разрешение общего создания не конструирует поиск.
  Legacy экран/FeedViewModel сохранены как явный fallback.
- UI выбора находится в feature/discovery api/impl, зависимости Gradle уже были
  общими. Узкий CheckInShopPickerEntry получает back/close/select/sign-in;
  app composition владеет навигацией, сессией, адаптерами и tracker handoff.
  Новый repository или параллельный backend не добавлены.
- Список/поиск/пагинация/избранное используют существующий SearchViewModel,
  карта — MapViewModel и прежний CoffeeMap/MapLibre host. Карточка — ShopRow с
  CoffeePeekShopCard; header/search/chips/loader/insets — design-system.
  Порядок чипов, тексты, page size 20, debounce и отсутствие расширенных
  catalog details сохранены; строки принадлежат существующему русскому values.
  Прокрутка списка сохраняется при переходе на карту и обратно.
- SearchPurpose.CheckInPicker сохраняет старое правило picker: первая валидная
  координата включает Nearby, nearby-запрос не ограничен городом, ручное выключение
  не отменяется следующими координатами. Discovery tab сохраняет обычные defaults.
  Без координат остаётся поиск по городу; выдача и гостевой sign-in gate общие.
- ShopBrowsingHost создаёт отдельный child ViewModelStore на текущую session lease.
  Старые select/close callbacks после смены аккаунта или disposal недействительны;
  выбор/закрытие доставляются один раз. Пустой slug не закрывает экран.
  Выбор передаёт публичный slug в ShopDetail(forCheckIn=true), закрывает клавиатуру
  и устанавливает continueToShop до native pop callback.
- Analytics inventory: исходный caller владеет attempt, picker продолжает тот же
  CheckInTracker/Shop step; close/back сохраняют причины отмены. List search,
  filter и map события используют AppDiscoveryAnalytics/существующий сервис.
  Legacy picker не вызывал openedShop при выборе строки и не отправлял discovery
  geolocation: purpose не добавляет их. Map selection сохраняет прежние map events.
  Consent/session guards не обходятся, второй analytics SDK не добавлен.
  Парные previews контента и ошибки списка размещены рядом с их компонентами.

Проверки (Mac/JBR 21, 2026-10-25, source поверх `0c52b0fa`):

- Discovery domain **12 tests / 1 suite**, data **17 / 1**, impl **60 / 5**;
  app subset `*CheckInShop*`, `*CheckInRendererFactories*`, `*Discovery*`,
  `*ShopBrowsing*`, `*GlobalCheckIn*`, `*AppShopAnalytics*` — **63 / 14**.
  Итого **152 tests**, 0 failures/errors/skips. Шесть новых search cases проверяют
  cached/first/invalid position, Nearby choice, query и guest favorite;
  navigation и lazy DI — семь новых cases; analytics — четыре in-memory cases
  handoff, back/close single cancel и declined consent. События не уходили в SDK.
- Первый запуск остановился до тестов: неверное имя Gradle task
  testAndroidHostTest, заменено на существующее testDebugUnitTest.
  Следующий compile нашёл два вызова loader без обязательного contentDescription:
  исправлены существующим локализованным resource. Последующие прогоны успешны.
- assembleDirectDebug/Sandbox, verifyDirectDebugRuntimeClasses,
  verifyDirectSandboxRuntimeClasses и verifyDirectSandboxIsolation успешны.
  DEX guard дополнен picker API/factory/app renderer/navigation classes.
  compileKotlinIosArm64, linkDebugFrameworkIosSimulatorArm64 и
  compileTestKotlinIosSimulatorArm64 успешны. Native tests скомпилированы,
  не исполнены. Gradle --max-workers=6, один процесс сборки.
- Studio MCP смог прочитать shared MapScreen, но inspection нового picker
  не разрешает даже Compose/design-system/resources из-за текущей IDE classpath
  модели; чистой IDE-проверкой это не считается. Android/Native compiler прошли.
  Preview deprecation и прежние expect/actual/bundle-ID warnings остаются;
  IDE previews не рендерились.
- SHA-256 APK: Debug `2746314d302cc53e1437c7fe373c68fe1231c732ed63fbf69eb85fd91870d79a`;
  Sandbox `b4f4f301b1b41b41c4d27278f39a0cc1fd840a6b9fa9d4da9133e31ed53892aa`.
  Приложения не устанавливались/не запускались, UI/instrumented/screenshot tests
  и test-only Activity/APK не создавались. Аккаунты и backend не менялись.

Открытые gates: ручная iOS-проверка открытия через Coffee/Community, native header
и close/back, размеры/темы/инсеты/клавиатура, permission grant/denial/revocation,
nearby/city, список↔карта/MapLibre, pagination/retry, guest sign-in и session renewal/
replacement, переход в Shop form и cancel tracker. Live analytics/backend,
OAuth/Keychain и Xcode/signing не проверялись этим checkpoint.

Следующий code-срез: **календарь Activity в Profile** — проверить отдельный modal
entry относительно уже перенесённой полной истории, затем переносить его владельца.
Далее контентные ReportCheckIn/ReportReview, Favorites по продуктовому решению
и core session port; удаление legacy — после общей ручной приёмки.

### Checkpoint: личная Activity в Profile / месяц и неделя — 2026-10-25

Продолжение PR #195 на базе `fef8e201`. После fetch актуальный
`origin/main=16724b7e` уже входит в HEAD; `origin/develop=105b8994`
с параллельными изменениями не вливался в этот срез. Ветка/PR прежние.

Сделано:

- Сверены два входа: own-profile недельная карточка открывает Activity modal
  (в fallback-представлении — полную историю), месяц внутри Activity позволяет
  выбрать день. Это отдельный сценарий от VisitedPlaces/HomeCheckIns.
  Публичный Activity calendar уже принадлежит Profile и читает только public API;
  personal Shop/Home readers в него не передаются.
- OwnProfileSlots на Android/iOS теперь используют app-owned
  FeatureProfileActivityRenderer → ProfileActivityEntry в feature/check-in-history.
  Нового бизнес-UI в composeApp нет. Прежние PersonalProfileActivity* и их VM
  сохранены для legacy fallback, но активные feature slots их не вызывают.
- Отдельные typed MVI month/week модели используют существующие history readers.
  Shop читает полный instant period, Home — календарь и все страницы civil range
  (100 rows/page, проверка metadata/пустой промежуточной страницы, dedup по ID).
  Shop timeline pages и Home editor/writer не создаются. Ошибка одного источника
  не стирает другой; retry и возврат на экран перечитывают range.
- Неделя — Monday–Sunday, включая соседний месяц/год; она не меняется при
  листании месячного календаря. Snapshot datePeriod переводит обе локальные
  границы отдельно, сохраняя DST. Home bounds inclusive, Shop toExclusive.
  Today, future-date/month guards, выбор/снятие выбора даты, shared counts/photos
  сохранены. При выборе дня идут Shop visits, затем Home, по времени внутри
  источника. Shop открывает его публичный slug, Home/отсутствующий shop — историю,
  как в прежнем Activity; новый маршрут деталей не придуман.
- HistoryCalendarCard получил frameless-вариант для Activity, сохраняя shared
  geometry/dots/photos и framed-вариант полной истории. Header/grid выделены
  в связные функции. Компактная дневная строка принадлежит истории и использует
  shared rating. Имена напитка/время/thumbnail fallback и адаптация к крупному
  шрифту сохранены; текст/доступность вынесены в русский values.
- Общая визуальная недельная карточка выделена в CoffeePeekActivityWeek:
  только labels, accessibility, flags/photo count и image slot, без дат/HTTP/feature
  зависимостей. Personal feature и public/legacy adapter используют одну геометрию,
  20dp corners, 0.7dp border, 10/12dp padding, dots/photos/+N.
  Публичные counts и прежняя Minsk date policy остаются у public adapter.
- Каждый personal surface имеет собственный CheckInHistoryHostViewModel/child
  store и тот же revocable lease: guest не читает private data, replacement
  очищает данные/VM, token renewal сохраняется, поздние ответы отменённых reads
  не принимаются. Owner передаётся внутрь body UIKit sheet явно; live account
  observation не замораживается при перекрытии родительского контроллера.
  Buffered navigation имеет owner token и не воспроизводится новой шторкой.
- Analytics inventory: старые PersonalProfileActivity* не отправляли отдельных
  событий даты/месяца/строки. Новых событий/SDK не добавлено; существующий Profile
  screen/sheet exposure и downstream navigation owners сохранены. Избранное,
  consent/session policy и реальные analytics transports не менялись.
  Парные colocated light/dark previews добавлены для изменённых компонентов.

Проверки (Mac/JBR 21, 2026-10-25, source поверх `fef8e201`):

- Design-system **44 tests / 12 suites**, history domain **8 / 3**,
  history impl **75 / 7**, Profile impl **60 / 6**; app subset
  `*CheckInHistory*`, `*HistoryHost*`, `*Profile*`, `*Activity*` — **89 / 19**.
  Всего **276 tests**, 0 failures/errors/skips. 21 новых feature cases покрывают
  week/year/leap bounds, оба источника/selected rows, all-pages/dedup/metadata,
  cancellation, stale month, retry/resume/Today/guest и buffered event owner;
  ещё 2 app cases — DST week boundaries и invalid civil bounds. Lazy DI test
  дополнен разрешением Activity renderer без credentials/session reads.
- Первые compile-попытки нашли отсутствующую UI dependency у API Modifier,
  обращение к internal DS formatter и nullable CpImage argument. API сузили до
  Compose runtime без Modifier/dependency, форматирование взяли из feature
  resources, nullable photo обрабатывается до image call. Затем все тесты прошли.
- assembleDirectDebug/Sandbox, packaged runtime-class checks и Sandbox isolation
  успешны; guard включает Activity API/factory/app renderer/DS facade.
  compileKotlinIosArm64, linkDebugFrameworkIosSimulatorArm64 и
  compileTestKotlinIosSimulatorArm64 успешны. Native tests только скомпилированы.
  Gradle последовательно, --max-workers=6. Прежние preview/expect-actual/bundle-ID
  warnings остаются; partial-linkage errors не скрывались.
- Studio MCP inspection AppCheckInHistoryCalendar: 0 errors. Это не полная IDE
  проверка UI; previews/симулятор/устройство не запускались.
- SHA-256 APK: Debug `4a59498ec259dd0ec4398f266154e756d7b3abf8252275bf42d252686a38f536`;
  Sandbox `d0360b36ec3150a2479df62addc2fbc384313729e46748e68b564c19ffd724ab`.
  Приложения не устанавливались, UI/instrumented/screenshot tests или test-only
  Activity/APK не создавались. Backend, аккаунты и реальные analytics не менялись.

Открытые gates: ручная iOS/Android Sandbox-приёмка недели/Activity отдельно от
полной истории, light/dark/крупный шрифт/VoiceOver, фото и пустые дни, несколько
чекинов/источников, month/Today/retry, UIKit height/scroll/insets/back/close,
возврат/полночь/смена зоны, account replacement/renewal, навигация Shop/Home.
Компиляция и fake tests не закрывают реальные backend/Keychain/OAuth/analytics,
Xcode/signing или device acceptance.

Следующий code-срез: **контентные жалобы ReportCheckIn/ReportReview** — сверить
актуальные входы/причины/авторизацию и сохранить существующий контракт. После
них — Favorites по продуктовому решению и core session port. Удаление legacy
остаётся после общей ручной приёмки.

### Checkpoint: контентные ReportCheckIn / ReportReview — 2026-10-25

Продолжение PR #195 на базе `221819cd`, та же ветка `feat/ios-build-foundation`.
После fetch `origin/main=16724b7e` уже предок HEAD; `origin/develop=105b8994`
не добавлялся в этот срез. Нового PR/ветки не создавали.

Сверка и реализация:

- Действующие входы — Shop detail, публичный список чекинов и карточки публичного
  профиля → Screen.ReportCheckIn. У ReportReview сейчас нет найденного producer;
  маршрут/параметр isPreview и старый review HTTP contract сохраняются для
  совместимости, без восстановления снятой фичи. Community inline-report остаётся
  своим текущим dialog flow. Ни один из них не является Shop-report неточностью.
- Новый заполненный feature/content-report имеет api/domain/data/impl boundaries.
  Ответственность, зависимости, consumers, скрытые детали и причины Gradle
  isolation вместо packages описаны в README. Регистрация — прежний Modules.all;
  composeApp commonMain собирает data/impl через lazy module/factory на Android/iOS.
  UI/domain не импортируют Ktor/data/legacy, DTO и HTTP validation скрыты в data.
  Core не зависит от новой фичи, нового app/DI framework нет.
- Оба navigation hosts разрешают ContentReportScreenRenderer. Старые
  ReviewReportScreen/ViewModel, их Koin factory и 2 unit tests сохранены как
  fallback, не активный platform default. Идентификаторы маршрутов не менялись.
- UI — typed MVI и stateless content с colocated light/dark previews. Используются
  DS header/back handler, scroll/IME/insets, compact multiline field, button/loader.
  Сохранены free text до 2000 UTF-16 units, trim перед submit, success/Done,
  preview copy, размеры/отступы/форма поля; лимит не разрывает surrogate pair.
  Pending/submitted form не редактируется и не отправляется повторно. Серверные
  ошибки стали immutable presentation data, transport exceptions в State не лежат.
  Строки принадлежат русскому values; новых категорий жалоб/продуктовых экранов нет.
- Preview не создаёт account host, не читает сессию и не вызывает repository.
  Он только валидирует текст и показывает прежнее локальное подтверждение.
  Guest submit вызывает sign-in один раз до возврата; auth-flow/root navigation
  остаются у приложения. Старое очищение credentials из requireAuthSession
  не копируется в форму: она получает read-only SessionReader.
- App host даёт отдельный child store и revocable account capability. Replacement,
  logout/observation failure очищают draft/VM и отменяют работу; известный user ID
  сохраняет форму при token renewal, без ID используется fail-closed token identity.
  Проверки до/после операции, observer и ensureActive отбрасывают late result.
  Buffered back/sign-in/retry эффекты привязаны к текущему surface owner; старый
  callback не управляет новой формой. Ошибку наблюдения можно закрыть/повторить.
- Data сохраняет POST /api/v1/check-ins/{id}/reports и
  /api/CoffeeShopReviews/{id}/reports, JSON {text}, экранирование ID одним segment,
  HTTP 201 + successful envelope/non-null data; Review дополнительно требует
  непустой string receipt id. Pascal envelope aliases остаются. 401/403/404/429
  получают прежние mode-specific сообщения. Business rejection может показать
  envelope message; private exception details не становятся UI.
- Lost/malformed acknowledgement, unexpected 2xx/5xx и неизвестная transport
  ошибка считаются unconfirmed. Фича не повторяет POST сама и блокирует submit
  в этой форме; это не глобальная дедупликация после новой формы/процесса.
  Нормальный auth-refresh клиента не заменён, idempotency сервера не придуман.
- Analytics inventory: старый экран/VM не отправлял собственных report/field/
  submit events. Root route/screen exposure, native controller и вызывающие
  карточки остаются владельцами. Второго SDK/новой схемы нет, настоящие события
  не отправлялись. Community dialog и его analytics не менялись.
- Sandbox теперь хранит check-in reports только в памяти, валидирует текст/
  POST и запрещает own records; reset очищает reports. Найденный edge case —
  созданный локально visit без authorId — сначала воспроизведён unit test
  (неправильный успешный ответ), затем закрыт проверкой принадлежности visits.
  Это исправление fixture, не свидетельство ошибки deployed backend.
  У review report нет активного Sandbox producer/набора отзывов; его контракт
  проверен MockEngine, реальный dataset не фабриковался.

Проверки (Mac/JBR 21, 2026-10-25, source поверх `221819cd`):

- Новая фича: domain **2 tests / 1 suite**, data **11 / 1**, impl **13 / 1**;
  app subset `*ContentReport*`, `*ReviewReport*`, `*SandboxApplicationBackendTest*`
  — **44 / 6**. Итого **70 feature/app tests**, 0 failures/errors/skips.
  Дополнительно build-logic-utils:test — **4 / 1**, также 0 failures/errors/skips.
  Реальные UI/instrumented/screenshot tests и test-only Activities/APK не создавались.
- Первый app test compile нашёл reset() без обязательного populated argument —
  исправлен явным populated=true. После общего зелёного прогона новый отдельный
  own-report regression был красным, как ожидалось; после store-ownership fix
  повторён весь app subset и Sandbox assembly, всё успешно. Финальная проверка
  общего UI нашла жёлтый default loader на жёлтой кнопке: выставлены 24dp и
  inherited button content color. После этого повторены impl/app unit subset,
  обе Android assemblies/guards, iOS arm64 compile и simulator framework link.
- assembleDirectDebug/Sandbox, runtime-class checks и Sandbox isolation успешны.
  Guard дополнен report API/data factory/impl factory/app renderer.
  compileKotlinIosArm64 и linkDebugFrameworkIosSimulatorArm64 успешны;
  compileTestKotlinIosSimulatorArm64 для app и трёх новых domain/data/impl modules
  успешны. Native tests скомпилированы, не исполнены. Gradle последовательно,
  --max-workers=6; прежние preview/expect-actual/bundle-ID warnings остаются.
- Studio MCP прочитал legacy screen; новую domain file inspection не выполнил:
  IDE ещё считает новые modules вне content roots, требуется Gradle sync.
  CLI compilation/tests прошли; IDE/preview rendering не объявляется проверенным.
  CI all-project testDebugUnitTest включает новые модули автоматически; CI здесь
  не запускался и его результат не выводится из локальных checks.
- SHA-256 APK: Debug `cbda1544fdced88ea37d6491cf4f9a7af8d8e12bd9618518e9cfb259d99d21e0`;
  Sandbox `7fe626ebff70f2d0437558684c0697f5e0f51423ecefc4ab2465d591f15c74ab`.
  Приложения не устанавливались/не запускались. Реальные reports, credentials,
  аккаунты, согласия и данные сервера не менялись; только in-memory fixtures.

Открытые gates: ручная Sandbox/iOS-приёмка обоих entry modes/preview, light/dark,
крупный шрифт/VoiceOver, IME/emoji/инсеты/native back, pending/back/return,
guest auth/401, replacement/renewal, success/rejection/unconfirmed. Live moderation,
analytics, Xcode/signing/устройство, Google/Apple и Keychain остаются отдельной
приёмкой. Сборки/fake scenarios её не сертифицируют.

Следующий срез после content-report — **оставшиеся потребители legacy SessionRepository bridge** (выполнен для активных адаптеров следующим checkpoint).
Он уже extends core/session SessionStore; новый store создавать не нужно.
Текущий Kotlin-import scan app common/android/ios + feature нашёл 25 файлов,
включая legacy fallback. Сначала разделить активные analytics/realtime/media/
notification/author-blocking/navigation adapters и оставляемые legacy VM,
перевести первые на узкие SessionReader/SessionStore, сохранить account/consent
и cancellation guards. Это не разрешение переписывать старые экраны или удалять
fallback до приёмки. Favorites iOS — по продуктовому решению; затем общая ручная
приёмка и только после неё удаление legacy.

### Checkpoint: core SessionReader в активных app-адаптерах — 2026-10-25

Продолжение PR #195 → develop, source поверх `066e9b94`. Новых модулей,
веток и хранилища сессии нет. Fresh origin/main `16724b7e` уже в истории.
Origin/develop `105b8994` содержит отдельные последующие изменения; этот
checkpoint не утверждает их интеграцию или проверку их поведения.

Что сделано:

- 12 production Kotlin-файлов переведены на core SessionReader:
  AnalyticsInstallation, SessionRealtimeManager, SessionNotificationAccount,
  SessionAuthorBlockingAccount, ServerRoasterFavorites, TrackedFavoriteInteractions,
  ProtectedPhotoSession, PhotoSessionInvalidation, CpImage, NavigatorViewModel,
  CheckInTabContent и Android CoffeePeekApplication. В затронутых адаптерах
  используется core Session вместо legacy typealias. Common app graph выводит
  тип Koin dependency из новых конструкторов; платформа не получает второй store.
- Startup warmup читает getSession(), эквивалент прежнего warmCache().
  Favorite gate проверяет isActiveSession(getSession()), эквивалент isLoggedIn().
  Эти потребители не записывают сессию и не требуют SessionStore. В настоящем
  dataModule SessionRepository, SessionStore и SessionReader остались aliases
  одного SessionRepositoryImpl/DefaultSessionStore; строго offline DI test
  разрешает все три и проверяет одну in-memory snapshot без Room/Keychain/HTTP.
- Analytics сохраняет ключ/значения consent, debug opt-in, schema/envelope,
  API_REQUEST_COMPLETED properties, operation/account guard и только backend
  userId как identity. Observer и request adapter выделены из install в небольшие
  app-функции; второй SDK/feature analytics service не вводился. Все проверки
  используют MemoryConsent/RecordingTransport, настоящие events не отправлялись.
- Notifications получает только активный userId. Author blocking сохраняет
  opaque fallback accessToken, если userId отсутствует; он не становится
  notification/analytics identity. Protected photos сохраняют инвалидирование
  при token rotation, account replacement/logout и до/после mutation, включая
  потерянный ответ; helpful-only votes по-прежнему не инвалидируют фото.
- Исправлена отмена suspend-загрузки сессии: favorite gate не открывает auth,
  освобождает pending key и допускает следующую попытку. Startup не глотает
  отмену warmup. Realtime retry пробрасывает CancellationException; transport
  failures/обычный disconnect повторяются с прежней задержкой только пока token
  актуален. URL, SignalR protocol, ForceLogout и cleanup не переписывались.
- 13 оставшихся app imports SessionRepository принадлежат BaseViewModel,
  legacy Community/PublicProfile/Feed/Profile/Activity/VisitedPlaces,
  DeleteAccountPending/ReviewReport/ShopDetail/Favorites/RoasterList/RoasterDetail.
  Старые экраны, Sandbox writer и modules/data SessionTokenProvider/Auth/cleanup
  bridge не удалены и не объявлены мигрированными. NavigatorViewModel всё ещё
  наследует transitional BaseViewModel; этот checkpoint не заменяет его на MVI.

Проверки:

- App unit/contract subset: **157 tests / 31 suites**, 0 failures/errors/skips,
  включая **18 новых tests** для reader identity/consent/request observation,
  отмены favorite gate/realtime и aliases настоящего dataModule. Существующие
  favorite analytics parity, photo cache/request/transport/invalidation прошли.
  Первое выполнение остановилось на компиляции новой заглушки: inferred
  MutableStateFlow мешал подставить failing Flow. Возвращаемый тип задан Flow;
  после этого повторён весь выбранный набор, без замены первоначального failure.
- assembleDirectDebug/Sandbox, verifyDirectDebugRuntimeClasses,
  verifyDirectSandboxRuntimeClasses и verifyDirectSandboxIsolation успешны.
  Packaged guards дополнены core SessionReader/SessionStore; Debug также
  проверяет DefaultSessionStore и SessionRealtimeManager. compileKotlinIosArm64,
  linkDebugFrameworkIosSimulatorArm64 и compileTestKotlinIosSimulatorArm64
  успешны. Native tests только скомпилированы, не исполнены. Gradle запускался
  последовательно с --max-workers=6. Прежние preview/expect-actual/bundle-ID
  warnings остаются; новых Gradle dependency/source-set boundaries нет.
  UI/instrumentation/screenshot tests не создавались и не запускались.
- SHA-256 APK: Debug `23fddfdecbd5a3f596468c46d06726cd71c51f4be4e59794ee31de36644b87f2`;
  Sandbox `0fa12d7778a2257ef2b621bdd53c05211011a5e1d10c26c806015601b9bf3a0d`.
  Source: `066e9b94` + этот checkpoint. Приложения не устанавливались и не
  запускались, данные телефона/аккаунта/сервера не менялись.
- Studio MCP: SessionRealtimeManager и AnalyticsInstallation проверены без
  ошибок. Проверки нового request adapter, favorite bridge и Android startup
  сначала вернули timedOut; после сборки повторены, ошибок не найдено.
  Это выбранные файлы, не полная IDE-проверка. UI/preview rendering не выполнялся.

Открытые gates: ручная iOS-приёмка session restore/logout/account replacement,
реальный Keychain, OAuth, серверное realtime revocation и доставка analytics
после consent. Unit fixtures не удостоверяют live backend, Xcode/signing или UI.
Далее после core SessionReader — Favorites full screen и собственное Profile
preview (следующий checkpoint). Удаление fallback/bridge — после ручной приёмки.

### Checkpoint: Favorites full screen и Profile preview — 2026-10-25

Продолжение PR #195 → develop поверх `b47a9417`, по поручению закончить
оставшиеся пункты миграции. Fresh origin/main `16724b7e` уже в истории;
origin/develop `105b8994` содержит отдельные дальнейшие изменения и не
объявляется интегрированным этим checkpoint.

- iOS FavoritesDestination теперь использует тот же feature FavoritesEntry,
  что Android. Убрана лишняя expect/actual-развилка; root Navigator/NativeScreenController
  остаются владельцами навигации, UIKit stack/back и screen exposure.
- Активный serverFavoritesModule, adapters roaster/drink и heart exposure
  перенесены из androidMain в app/commonMain. api/impl dependency доступна
  app commonMain; новых Gradle-модулей нет. Серверный source один для shops,
  roasters и drinks; retired local writer/JSON import не включались.
- Собственное Favorites preview в новом Profile тоже идёт через feature,
  с отдельным lifecycle ViewModel и теми же server ports. Активный Profile
  больше не разрешает старый app FavoritesViewModel singleton. Старые
  FavoritesScreen/ProfileScreen, VM, bindings и preview wrapper оставлены
  доступными fallback callers до приёмки.
- У preview сохранены пустое/загружаемое/ошибочное состояния, retry и dismiss
  ошибки операции; наблюдение владеет списком, не оптимистическое удаление.
  Shop/roaster открываются только по public slug; у drinks не придуман маршрут.
  Геолокация/permission и прежний формат расстояния остаются app-owned.
- Общая геометрия own/public profile carousel перенесена в design-system
  CoffeePeekStackedCardCarousel: 300dp full cards, пары с высотой соседней
  полной карточки, gap токен и relative placement сохранены. Layout принимает
  только группы/ключи/визуальный slot, не знает feature/legacy models.
  Старый app carousel стал compatibility wrapper без второго layout.
- Feature использует CoffeePeekScaffold и scroll content padding, поэтому
  NativeScreenController safe area учитывается в списке. Заголовок использует
  существующий native header hook. Composable interface defaults убраны для
  Native link. Missing/loading/failure photo использует существующий branded
  CoffeePeekImagePlaceholder. Light/dark previews colocated; UI runtime не запускался.
- Analytics inventory: favorites_opened(entry_point=profile), root screen owner,
  consent-aware heart exposure и shop/roaster attempted/changed/failed wire
  остались прежними. Drink removal не получает выдуманного event. Tests используют
  только in-memory analytics, не production SDK/backend.
- Публичные Profile favorite/check-in cards — намеренные app slots с публичными
  моделями. Их источник не заменён private reader. Image/viewer/prefetch/platform
  adapters остаются у composition; их наличие не означает активный старый
  экран/VM. Полное удаление legacy modules/bridges этим не разрешается.

Проверки:

- App subset `*Favorite*Test`/`*Favorites*Test`: **74 tests / 17 suites**;
  favorites domain **1 / 1**, impl **19 / 3**. Всего **94 tests / 21 suites**,
  0 failures/errors/skips. Новые 4 actual production-graph cases проверяют lazy
  resolution, три вида server-backed reads/removal, owner guard и memory-only
  clear. Pure preview policy и dismiss regression не используют Compose instrumentation.
  Перенесены в commonTest существующие server bridge/roaster/distance tests;
  их исполнение подтверждено Android unit, а Native test compile записан отдельно.
- Первые прогоны остановились на compile errors новых исходников: inference
  nullable Triple в ожидаемом результате graph test и positional modifier вместо
  content у Layout. Исправлены явный тип и named modifier; затем весь subset
  повторён успешно. Эти failures сохранены в записи, не названы зелёными runs.
- assembleDirectDebug/Sandbox, runtime-class guards и Sandbox isolation прошли.
  compileKotlinIosArm64, linkDebugFrameworkIosSimulatorArm64 и общие test
  compile app/favorites impl успешны; Native tests только скомпилированы.
  Gradle последовательно с --max-workers=6. Source: b47a9417 + этот checkpoint.
  APK SHA-256: Debug `fef4d4329bc8ff2b6cb042f863f592cfcf1343f3cd79b529e270df0ac9701bcd`;
  Sandbox `75eaf2c7bcacfe6db207eeb6918108c3752e2de79b6ed6eaf8d0f1a658002dd5`.
  Приложения не устанавливались/не запускались; аккаунт и сервер не менялись.
  Автоматизированные UI/instrumentation/screenshot tests не создавались и не запускались.
- Studio MCP: FavoritesList, FavoritesPreview и новая DS-карусель проверены
  без ошибок. App server module получил unresolved api/impl из прежней IDE
  модели common dependencies; требуется Gradle sync. Screen inspection один
  раз timedOut. CLI compilation/tests/link прошли; IDE/preview rendering
  не объявляется полностью проверенным.

Gates: ручная iOS/Sandbox-приёмка полного экрана и Profile carousel, обе темы,
крупный шрифт/VoiceOver, safe area/header/back/scroll, mixed/single/empty/error,
гость/restore/logout/replacement/token renewal и защищённые фото. Real server
removal допускается только на безопасном явно выбранном аккаунте; его не делали.
Xcode/signing/устройство, OAuth/Keychain/realtime/analytics delivery — открытые
общие release gates. После приёмки возможен отдельный этап удаления legacy;
compile и fake-тесты не закрывают этот gate.

### Checkpoint: синхронизация с develop — 2026-10-25

После Favorites checkpoint `e67692f3` PR #195 оказался CONFLICTING с целевой
develop `105b8994`. Выполнено обычное merge в migration-ветку, без rebase/force push
или записи в main/develop. Fresh main `16724b7e` уже входит в историю.

Что сохранено при разрешении конфликтов:

- Новый split Discovery из develop перенесён в commonMain: content, map/search/
  roaster layers, effects/state, section title и shop preview. Не восстановлен
  прежний большой Tab. Android BackHandler заменён существующим CoffeePeekBackHandler;
  строки и парные previews общие. Геометрия карты, host.retained, reduce-motion,
  photo prefetch, overlays и callbacks оставлены по общей реализации.
- Notifications сохраняет resolve destination/open/mark read, account/generation
  guards и pending-open handoff. Общий FeatureNotificationsRenderer используется
  обеими платформами; инъекция новых портов включена в Android и iOS. Это не
  проверка настоящего iOS push tap/cold launch — Swift PushAppDelegate не менялся.
- Новый contributionsModule стал общим. Notification navigation и Contributions
  screen получают один registered feature repository. Вместо java.time в
  commonMain используется существующий contributionUtcOffsetMinutes; backend
  contract и cancellation fix ChangeEditor из develop сохранены.
- В Sandbox оставлены оба local store: notifications и content reports,
  включая их reset/dispatch. Реального сервера или аккаунта в них нет.
- Перестановка фото Shop/Home из develop не потеряна. Общий Home photo component
  использует CoffeePeekPhotoAttachments, callbacks move/remove/add и имеющиеся
  feature resources; App Home media сохраняет Fit/Crop. Shared previews используют
  multiplatform tooling. Не создавались новые UI/instrumentation test hosts.

Проверки объединённого кода:

- Выбранный unit/contract набор: **601 tests / 83 suites**, 0 failures/errors/skips.
  App 202 / 36; design-system 47 / 13; Discovery impl 60 / 5;
  Contributions impl 66 / 7; Notifications data 4 / 1 и impl 22 / 3;
  Home impl 73 / 3; Shop impl 108 / 12; Favorites impl 19 / 3.
  Используется Gradle cache для неизменённых прошедших suites; counts относятся
  к результатам всего выбранного набора, не к числу заново исполненных tasks.
- Первый app test compile после merge выявил старую ProfileSignOutAdapterTest
  заглушку без нового NotificationFeature.destination. Она теперь fail-fast
  запрещает этот вызов при logout; sign-out suite включён в повторный subset.
  Исходный compile failure записан, повторный набор прошёл.
- assembleDirectDebug/Sandbox, runtime-class guards и Sandbox isolation прошли.
  compileKotlinIosArm64, linkDebugFrameworkIosSimulatorArm64 и common test compile
  app, Notifications data/impl, Discovery impl, Contributions impl, Home impl и
  Shop impl успешны. Native tests только скомпилированы, не исполнены.
  Gradle последовательно с --max-workers=6; прежние preview/expect-actual/
  bundle-ID warnings остаются.
- Source: e67692f3 + merge develop 105b8994 с описанными resolutions.
  SHA-256 APK: Debug `64eaad503d9db62f5120f82839f4bf2f4fe3bb9b5c6d3625f9583a679d1d0125`;
  Sandbox `a6f5d2a3d9f8e23fbd9e3eaf60950214d397226853517575cd84294e936de36b`.
  Приложения не устанавливались/не запускались, телефон и реальные
  credentials/аккаунты/согласия/серверные записи не менялись.
- Studio MCP проверил общие DiscoveryTabEffects и ContributionsModule без
  ошибок. Полная IDE/preview проверка не заявляется; для новых common dependency
  roots Favorites по-прежнему требуется Gradle sync.

Gates прежние: ручной UI обеих платформ и safe test-account acceptance,
Xcode/signing/устройство, реальный OAuth/Keychain/сервер/realtime/analytics.
Наличие единичных legacy visual slots не означает смену публичного reader на
private. Fallback/legacy bridge не удаляются до общей ручной приёмки.

### Исправление запуска iOS из Studio — 2026-10-25

На базе adc0f642 подтверждён реальный startup аргумент Gradle в IDE:
`--max-workers=`. Плагин разбирал shell-default `${GRADLE_MAX_WORKERS:-2}`
из Xcode Build Phase как неизвестную переменную и терял значение.

Лимит теперь задан Xcode build setting GRADLE_MAX_WORKERS=6 в Shared.xcconfig,
а Build Phase ссылается на переменную без shell-default. TestFlight archive
передаёт явный build setting GRADLE_MAX_WORKERS=1, сохраняя CI memory limit.
Локальные пользовательские изменения project.pbxproj, включая IDE skip guard,
не отменены и не включены в коммит исправления.

Проверки: plutil project lint и git diff --check успешны; аргумент извлечён из
настоящего Build Phase и проверен для значений 6/1; Gradle --version с
--max-workers=6 завершился успешно. Чтение полного Xcode build settings
не завершено: SwiftPM загрузка Firebase получила RPC/HTTP2 error; запущенный
для этой проверки xcodebuild остановлен. Это отдельная сетевая ошибка,
не успешная iOS-сборка. Приложение не собиралось/не запускалось, UI/аккаунт
не затрагивались; Kotlin/Android код не менялся.

### Debug без обязательного Firebase-конфига — 2026-10-25

После исправления max-workers пользовательская сборка дошла до Xcode и упала
на одном обязательном resource input GoogleService-Info.plist. Это конфигурация
Firebase push, не запрос Google OAuth. PushAppDelegate уже пропускает Firebase
при отсутствии файла, но статический Copy Bundle Resources не позволял собрать Debug.

Статический resource заменён отдельной фазой copy-firebase-config.sh: Debug
без файла пропускает Firebase и удаляет устаревшую копию из generated bundle,
при наличии файл копируется; Release без файла явно падает. Настоящий plist
остаётся локальным/ignored, тестовая заглушка в приложение не подставляется.
IosGoogleAuthBridge не включает Google-вход при AppConfig.isDebug даже с OAuth
IDs; email/password, backend и Apple provider не менялись.

Проверки на базе e754799c + фикс и прежние пользовательские local pbx changes:
4 изолированных shell contract сценария (missing/present Debug/Release и
stale Debug artifact), 2 существующих Firebase config unit tests, plutil и
shell syntax прошли. linkDebugFrameworkIosSimulatorArm64 успешен.
Полный xcodebuild Debug, generic iOS Simulator/arm64, CODE_SIGNING_ALLOWED=NO
прошёл: BUILD SUCCEEDED. В полученном com.coffeepeek bundle CoffeePeekDebug=YES,
GoogleService-Info.plist отсутствует. Приложение не устанавливалось/не запускалось,
настоящие OAuth/push/аккаунты не проверялись. Gradle внутри Xcode ограничен 6
workers; Release archive/signing и устройство не запускались.

## 1. Принципы (решение 2026-10-24)

- **iOS остаётся как в проде.** Нативная оболочка: `UITabBarController`, пять `UINavigationController`-стеков, `NativeScreenController`
  (инсеты, жест «назад», закрытие клавиатуры). Нативные шторки, алерты, меню, карта MapLibre (Swift), Apple/Google вход (Swift), push, фото-пикер.
  Это платформенная инфраструктура, она не legacy и не удаляется.
- **Общие слои:** `domain`, `data`, ViewModel (MVI-состояние) используют обе платформы. Полный SwiftUI — план на будущее, сейчас не делаем.
- **Один большой PR с миграцией.** Внутри — коммиты по этапам и владельцам. Старый legacy-код **удаляем только когда миграция готова целиком**
  и проверена (см. этап 5). До этого `Legacy*Renderer` остаются запасным путём.
- **Проблемные места не остаются в legacy.** Нужен нативный элемент — делаем его реализацию в `iosMain` и подключаем к новому UI через
  design-system, а не оставляем старый экран.

## 2. Что показала сверка с продом (исходный инвентарь; текущий статус выше)

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
- Core session port вместо `SessionRepository` (оставшиеся app-потребители legacy bridge) — первый блокер удаления legacy.

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
8. Favorites — full screen и own-profile preview подключены в checkpoint 2026-10-25; ручная приёмка открыта.

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
