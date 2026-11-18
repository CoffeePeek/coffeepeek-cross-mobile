# План перехода CoffeePeek на SwiftUI

База плана на 16 ноября 2026 года — `codex/ios-native` (`bc87cfe54`). Экран «Настройки → Тема» влит в [PR #366](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/366). Сборка Xcode для iOS Simulator и 85 тестов Kotlin/Native прошли. Ручная проверка темы остаётся открытой. [Прогон `ios-verify`](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/43852969839)

Цель — перевести активные экраны и модальные сценарии iOS на SwiftUI. Kotlin сохраняет бизнес-правила, данные, сессию и состояние сценариев, UIKit — существующие стеки вкладок и системные интеграции. Compose-реализации продолжают обслуживать Android и оставшиеся iOS-маршруты до их переноса. Остаток legacy exit ведётся в [feature/LEGACY_EXIT_PLAN.md](../feature/LEGACY_EXIT_PLAN.md).

## Итоговый аудит активных входов — 2026-11-18

Все перечисленные ниже активные iOS-экраны и вложенные тела модалок имеют
SwiftUI-рендерер. MapLibre, камера, PHPicker, системные alerts/action sheets,
share sheet и UIKit-навигация сохраняют нативные системные реализации.
Kotlin сохраняет существующие ViewModels, данные, валидацию, запись, сессию,
пагинацию, права на фото и аналитику. Android использует прежние рендереры.

Срез собран в заключительном [PR #407](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/407)
из самостоятельных checkpoint #399/#401/#404/#405/#406/#408/#409. До их слияния
это зависимости draft-кандидата; каждый parent допускается в integration только
после успешных Xcode и Kotlin/Native проверок своего последнего SHA. Зелёные
mergeable PR сливаются отдельно. Остаточные чисто структурные конфликты разрешены
в #407: после green всех parent и общего кандидата их проверенные commits могут
войти одним обычным merge #407 с сохранением ancestry. Base-only pushes ради
повторения уже решённых конфликтов не нужны. Точные SHA, прогоны и выводы
проверок фиксируются в описаниях соответствующих PR.

Таблица покрывает все 53 сериализуемых ID из `Navigator.Screen`, включая
технические контейнеры и совместимость. Это не число разных экранов: некоторые
ID выбирают вариант того же нативного контроллера.

| Входы | Нативный владелец и checkpoint |
| --- | --- |
| `Main`, `FeedGraph`, `CommunityGraph`, `ProfileGraph`, `BusinessGraph`, `SettingsGraph` | UIKit root/tab/back stacks; startup loading — SwiftUI (#403). Технические контейнеры без самостоятельного feature UI. |
| `Auth`, `Register`, `ForgotPassword`, `ShopCheckInSignIn`, `ResetPassword` | `makeAuthController`; общий Kotlin auth/handoff. ResetPassword — совместимый ID с существующим переходом к web-only recovery/login, отдельного старого UI не открывает. |
| `FeedTab`, `CommunityTab`, `ProfileTab`, `SettingsTab`, `BusinessTab` | Discovery (#401), native community feed, собственный/гостевой профиль, native settings, native ownership (#404). |
| `ShopDetail`, `RoasterDetail`, `CoffeeDetail`, `ShopMenuGallery`, `ShopReviews`, `ReportShop` | Native Shop (#405), Roaster (#402), Coffee, menu/gallery/check-ins и report controllers. Owner actions общие с #404; временный roaster Compose toolbar удалён в #407. |
| `SuggestShopChange`, `ShopChangeEditor`, `MyContributions`, `ShopChangeRequestDetail`, `AddShop`, `AddRoaster` | `IosContributionsRoute`/`IosContributionForm` → SwiftUI; четыре формы (#408), существующие native list/detail. Расписание, меню, фото/камера и карта входят в этот же путь. |
| `MyBusiness` | Native overview/detail/editor/claim/photos/QR/publish/discard, тот же OwnershipViewModel (#404). |
| `UserProfile`, `UserCheckIns`, `EditProfile`, `DeleteAccountPending` | Native public profile/list/edit, native deletion pending (#398). Публичные фото заменены в #397. |
| `CollectionsComposer`, `PublicCollection`, `CollectionsLibrary`, `CollectionEditor`, `SavedPublicCollection` | Native workspace/library/public snapshot/poster/export и profile preferences (#400). CollectionsComposer не имеет production caller/app link; ID сохранён для совместимости и на iOS ведёт в native library. |
| `CreateCheckIn`, `ShopCheckInEditor`, `CheckInDetail`, `HomeCheckInDetail`, `EditHomeCheckIn` | Native shop picker (#401), существующие SwiftUI check-in editors/details. Вложенные viewer и ошибка наблюдения сессии — #407; редактор старого shop-поста и report sheet — #409. |
| `ShelfArchive`, `ShelfAddBag`, `ShelfBag` | Native shelf/archive/add/scanner/bag, включая собственный/публичный preview, visibility/finish/undo и brew handoff (#399). |
| `CitySettings`, `ThemeSettings`, `LanguageSettings`, `BlockedAuthors`, `UsagePolicy` | Существующие native Settings/Theme/BlockedAuthors controllers. |
| `ReportReview`, `ReportCheckIn` | Существующий native content-report controller; отдельные draft owners в Feed/Post report sheet сохраняются (#409). |

Дополнительно проверены поверхности без отдельного route ID: notification inbox,
check-in create picker и гостевой onboarding, обязательное/необязательное
обновление, глобальные loading/error (#398/#403), profile shelf/collections и
preferences, poster preview/picker, owner/claim, price, photo viewer, меню действий,
finish/visibility/undo, Feed/Post editor/report и snackbar (#406/#409).
`CollectionsPickerHost` на iOS остаётся `Unit`, leaderboard выключен в действующей
композиции; отсутствующий продуктовый вход не восстанавливается ради миграции.

В Root #407 добавлен non-UI контракт парности и полноты factory registration
в обоих Swift launcher. Он ловит обнаруженные пропуски `configureNativeDiscovery`
и `configureNativeShopDetails` в xtool, которые одна Xcode-сборка не выявляла.
Устранены остаточные SF photo/leaf/bag placeholders: используется существующий
CFPK (чёрный/белый в светлой теме, белый/чёрный в тёмной). Shared Shop card
применяет brand gold для звезды/выбранного лайка в обоих своих consumers.

### Что остаётся от Compose на iOS

`MainViewController`, часть `NativeScreenController`, feature entry adapters,
`NativeEditorControllerHost` и sheet containers пока используют Compose runtime
для существующих VM/ViewModelStore, lifecycle/effects, ресурсов, root DI,
аналитики и UIKit interop. Они всё ещё имеют действующих инфраструктурных
потребителей и не являются неиспользуемыми fallback. Их удаление потребует
отдельного изменения API/source sets/Gradle-графа после этого UI checkpoint.
Перенос экранов не означает, что Compose уже удалён из iOS framework.
Общие Compose-экраны Android также сохраняются.

### Проверки и открытые ручные ворота

- Общий кандидат: 32 исполненных Python iOS tooling/resource/bootstrap tests,
  без ошибок; полный OpenStep синтаксис, уникальность 392 Xcode objects и ссылки
  проверены. Единственная старая Firebase build-phase reference сохранена.
- Combined `verifyFunctionBodyLimits` прошёл до обновления baseline. Штатный
  generator только удалил/уменьшил старый долг, повторный gate прошёл;
  новых либо увеличенных baseline entries нет. Первый совместный вызов generator
  и verify был отклонён Gradle из-за producer/consumer task dependency; затем
  обе задачи успешно выполнены отдельными последовательными invocation.
- Локальная попытка app metadata ранее остановилась в старом generated Room
  expect/actual конфликте core/database до компиляции новых app adapters;
  database не менялась. Feature metadata проверена отдельно; app build и
  исполнение native-контрактов проверяются macOS CI, а не подменяются metadata.
- Подтверждённые checkpoint: #399 — 98 Shelf tests (8 новых native contracts),
  #400 — 99 Collections tests, #402 — полный Native job и Xcode; #398/#403 также
  имеют успешные Xcode/Native прогоны. Остальные результаты привязаны к final SHA
  в PR; слияние при красном/незавершённом CI не разрешено.
- В первом Native прогоне #408 компиляция теста не вывела generic type для
  `List<ChangeDraft.Description>` против `MutableList<ChangeDraft>`; assertion
  явно типизирован `List<ChangeDraft>`, аналогичный Community assertion исправлен.
  Старый Xcode build и app contracts прошли, feature suite до отказа не исполнился.
  Новый final SHA проходит повторный Native CI. Media guard отдельно проверяет
  live owner/epoch до и после fetch, включая смену аккаунта до flow collector.
- Требуют ручного iPhone review: камера/сканер и возврат после фото, повторный `+`,
  фото с реальными разрешениями, OAuth/backend/persistence, смена аккаунта,
  keyboard/VoiceOver/крупный шрифт, обе темы, native sheet/back/cancel и запуск
  из cold start/deep link/push. Unit/contract tests и сборка этого не сертифицируют.
  В этой волне нет автоматизированных UI/screenshot tests, Android app runs,
  реальных account writes или ручного TestFlight dispatch.

## Статус первой партии

### Продолжение полной сегрегации — 2026-11-18

Повторный аудит `5f381a4f7` проверил фактические native factories и вложенные
потребители. Осталось 15 активных поверхностей маршрутизации, включая две вкладки,
а также вложенные подборки/полка, постер, фотопросмотр и два корневых диалога.
Наличие SwiftUI-оболочки не считается переносом встроенного Compose-содержимого.
Kotlin сохраняет ViewModels, бизнес-правила, сеть, сессию и аналитику.

| Волна | Владелец и ветка | Переносимые входы |
| --- | --- | --- |
| 1 | Агент поиска, `codex/ios-native-discovery-ui` | Discovery, выбор кофейни для чекина, обвязка существующей нативной карты |
| 1 | Агент полки, `codex/ios-native-shelf-ui` | Своя полка, публичный preview, добавление/сканирование, карточка пачки и действия |
| 1 | Агент подборок, `codex/ios-native-collections-ui` | Редактор, публичная подборка из ссылки, профиль, настройки показа, постер и picker |
| 1 | Интеграция, `codex/ios-native-system-ui` | Фотопросмотр публичного профиля и списка чекинов кофейни |
| 2 | Освободившийся агент | Карточки кофейни и обжарщика, preview/search обжарщиков внутри Discovery |
| 2 | Освободившийся агент | Добавление кофейни/обжарщика, предложение изменения и редактор изменения |
| 2 | Освободившийся агент | BusinessTab и MyBusiness со вложенными сценариями владения |
| 2 | Интеграция | Ожидание удаления аккаунта, гостевой onboarding, предложение обновления |
| 3 | Интеграция | Повторный аудит активных входов и удаление последнего Compose fallback/корневого UI на iOS |

Каждый checkpoint получает отдельный PR в `codex/ios-native`. Перед слиянием
проверяются актуальная база, итоговый diff, успешные Xcode build и Kotlin/Native
контракты на последнем SHA. Android CI в этих PR не запускается. Ручные проверки
на iPhone фиксируются отдельно; зелёный CI не подтверждает камеру, OAuth,
реальный backend, VoiceOver и визуальное соответствие в обеих темах.

Начальная проверка фотопросмотра: сохранить выбранный индекс, ленивую загрузку
оригинала, отмену подписок, запрет устаревших кадров при смене аккаунта/блокировке,
аналитику overlay и возврат к исходному экрану. Используется существующий
`CoffeePeekNativePhotoViewer`; Compose-viewer удаляется из заменённых входов.

- **Интеграция iOS влита.** Корни вкладок и вложенные маршруты используют общий выбор нативного контроллера. Повторный прогон после подключения галереи подтвердил сборку Xcode и 93 теста Kotlin/Native. [PR #368](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/368) · [ios-verify](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/43865870802)
- **Галерея меню влита.** Экран SwiftUI использует существующую Kotlin-загрузку и открывает полноэкранный просмотрщик на выбранном фото. Сборка Xcode и 91 тест Kotlin/Native прошли. [PR #369](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/369) · [ios-verify](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/43860308720)
- **Настройки влиты.** Перенесены вкладка, город, язык и политика использования. Сборка Xcode и 96 тестов Kotlin/Native прошли на `3d95e7c5` до разрешения конфликта с экраном заблокированных авторов. [PR #370](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/370) · [ios-verify](https://github.com/CoffeePeek/coffeepeek-cross-mobile/actions/runs/43870701296)
- **Влиты отдельные экраны следующих пакетов.** Библиотека подборок `CollectionsLibrary` относится к пакету 7, форма жалобы на кофейню `ReportShop` — к пакету 5, заблокированные авторы и подтверждение блокировки — к пакету 4. [PR #372](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/372) · [PR #374](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/374) · [PR #373](https://github.com/CoffeePeek/coffeepeek-cross-mobile/pull/373)

Версии #370, #372 и #374 до разрешения конфликтов прошли `ios-verify`. Перед слиянием объединённого кода сверены маршруты, аргументы обоих Swift launcher, ресурсы Xcode и фильтры Kotlin/Native. Повторный CI оставлен выполняться после слияния.

Ручные проверки темы, галереи и настроек в iOS Simulator остаются открытыми. Следующий пакет по плану — вход и восстановление пароля.

### 2026-11-18 — native check-in nested overlays

Final entry tracing found the shared Compose photo viewer still reachable from
native feed cards, community post detail and home check-in detail. A platform
adapter now presents the existing SwiftUI viewer through the existing UIKit
presenter, retaining original/preview selection, image authorization, revocation,
overlay analytics, reduced motion and return to the same Kotlin-owned route.
The history session-observation failure screen also has a SwiftUI renderer with
the same retry/back actions and localized feature-owned text. Large text scrolls
from the beginning while shorter content remains centered. Coffee and check-in
photo fallbacks reuse the existing theme-aware CFPK placeholder instead of SF
photo/leaf symbols, following the requested light/dark colors. Android retains
its existing viewers and failure content.

Three non-UI native contracts cover revoked/disposed photo callbacks and failure
screen lifetime. Both launcher registrations, Xcode source references and paired
previews are included. Local OpenStep parsing and 29 existing iOS tooling tests pass. The app metadata
attempt stopped in the inherited core/database Room generated expect/actual
conflict before compiling these adapters; database sources were not changed.
The separate function-body gate has only the inherited ownership factory finding
fixed by the Business checkpoint, with no new findings here. Xcode and the three
native contracts remain pending ios-verify. Manual device acceptance remains for
photos, session recovery, VoiceOver, large fonts, themes and camera/modal return.

### 2026-11-18 — native transient messages

The final nested-UI audit found the Compose snackbar still mounted over the
native community feed and post detail. Both now use SwiftUI message cards via a
platform adapter. The same Kotlin SnackbarHostState and Material host retain
queue ordering, accessible timeouts, action results and transition disposal;
Material does not render a message card on iOS. Android keeps its current host.

Verification plan: preserve FIFO/action/dismiss semantics and prevent delayed
callbacks from affecting a replaced or disposed message; two non-UI native
contracts cover callback identity, closure and late geometry. Native content
measures its height, uses the existing 12 pt toast radius, supports scrolling
for large text and announces the message. Both launcher registrations and the
Xcode source references are included. Existing local iOS tooling tests pass
(29 tests). Xcode and native contracts are pending ios-verify; manual checks for
VoiceOver timing, large fonts and touch-through outside the card remain open.

### 2026-11-18 — native roaster detail, carousel and search

Ownership: feature/roaster retains both existing ViewModels, filters, favorites,
link normalization, photo validation, lazy detail reads and interaction analytics.
Composition supplies the native iOS renderer and account-scoped image source.
SwiftUI replaces RoasterDetail, the owner carousel and full search within Discovery.
The public RoasterScreenRenderer contract stays unchanged, including shared list
state between the carousel/search and Discovery-owned criteria. Android keeps its
current Compose renderer. Shared native media keeps CFPK placeholders and access
revocation. The ownership toolbar/claim slot remains under the Business checkpoint;
its existing API is reused, so this checkpoint does not certify that separate UI.

Verification: all 29 roaster JVM tests pass (4 suites, 0 failures/errors), including
4 new native-session contracts for filters/favorite writes/navigation/photo selection.
The function-body gate reports only the two inherited ownership/public-profile
violations after making the roaster factory body explicit; its first run also found
that factory expression and it was corrected. Native account-lifetime contracts and
the full roaster suite passed on c980a6137 in ios-verify run 44273211112,
alongside the Xcode build/binary compatibility check; PR #402 is merged.
The earlier Xcode resource-group parser failure was fixed before that run.
Manual device review remains pending. Previews use fake state in both themes;
only existing supported translations are copied, missing be entries keep Russian.

## Пакеты работ

Каждая строка задаёт владельца сценария. Один пакет может потребовать нескольких PR, если пользовательские сценарии можно принять по отдельности. Зависимости в колонке относятся к подключению и сквозной проверке. Подготовку SwiftUI на тестовых данных и аудит текущих маршрутов можно вести раньше.

| Пакет | Граница работ | Зависимости и приёмка |
| --- | --- | --- |
| 0. Интеграция iOS | Воспроизвести ручную проверку «Темы». Подключать SwiftUI через `NativeScreenController`, существующие стеки и `UIHostingController`. Вести список активных маршрутов, вкладок и модальных входов. | Сначала тема, язык и возврат из пилота. Для следующих экранов проверить push, восстановление стека, завершённый и отменённый свайп назад, один показ в аналитике. Общий механизм регистрации выделять, когда конкретные экраны покажут, что повторяется. |
| 1. Галерея меню | Только `ShopMenuGallery` из карточки кофейни. Переиспользовать `ShopMenuGalleryViewModel`, загрузку и открытие выбранного фото. | Пакет 0 подключает маршрут. Проверить загрузку, пустую галерею, ошибку, повтор, возврат из просмотра фото и отмену загрузки при уходе. Сохранить текущую передачу оригинала и индекс. |
| 2. Настройки | `SettingsTab`, `CitySettings`, `LanguageSettings`, `UsagePolicy`. `BlockedAuthors` подключается к владельцу модерации, `ThemeSettings` уже перенесён. | Пакет 0 подключает вкладку. Проверить выбор города, смену языка во всём приложении, сохранение выбора после перезапуска и состояния политик и согласия на тестовых данных. |
| 3. Вход и восстановление | `Auth`, `Register`, `ForgotPassword`, `ShopCheckInSignIn`, переход по ссылке восстановления `ResetPassword`. Переиспользовать существующие провайдеры Apple и Google. | Пакет 0 подключает сброс стека. Проверить отмену входа, возврат к исходному чекину после входа, повтор по ошибке и веб-переход восстановления. Реальный OAuth проверять отдельно от тестов с подставными провайдерами. |
| 4. Лента, уведомления и модерация | `CommunityTab`, лидерборд, `CheckInDetail`, колокольчик и шторка уведомлений, `ReportCheckIn`, `ReportReview`, `BlockedAuthors` и глобальное подтверждение блокировки. | Пакет 0 подключает вкладку и глобальную презентацию. Проверить обновление и пагинацию, переход из push и ссылки, состояние после блокировки, отсутствие дублированной аналитики. Ссылка в публичный профиль может временно открывать действующий Compose-экран. |
| 5. Поиск и каталоги | `FeedTab` и его внутренние списки поиска, карты, кофе и обжарщиков. `ShopDetail`, `CoffeeDetail`, `RoasterDetail`, `ShopReviews`, `ReportShop`. Галерея меню принадлежит пакету 1. Сохранить MapLibre. | Пакет 1 закрывает переход к галерее. Проверить поиск и фильтры, смену списка и карты, фокус кофейни из другой вкладки, ссылки, шторки и теги источников перехода. Встроенный вызов «Создать чекин» передать пакету 8. |
| 6. Профиль и история | `ProfileTab`, `UserProfile`, `UserCheckIns`, `EditProfile`, `DeleteAccountPending`, `VisitedPlaces`, `HomeCheckIns`. Учесть недельный блок и календарь в профиле, полную историю и модальные варианты Home. | Пакеты 3 и 8 нужны для сквозных сценариев аккаунта и редактора. Проверить гостевой, свой и публичный профиль, видимость личных данных, смену аккаунта, позиции списков и выбор даты. Текущие серверные привязки избранного используются через существующие контракты. |
| 7. Подборки | `CollectionsLibrary`, `CollectionsComposer`, `CollectionEditor`, `PublicCollection`, `SavedPublicCollection`, выбор обложки и входы из профиля. | Можно разрабатывать параллельно с пакетом 6. Проверить публичную ссылку, возвращение в нужную вкладку, действия с подборкой на тестовых данных. `CollectionsPickerHost` сейчас пуст на iOS, поэтому отдельного переноса у него нет. |
| 8. Создание и редактирование чекина | Кнопка «+» и `CheckInCreateHost`, `CreateCheckIn`, `ShopCheckInEditor`, `EditHomeCheckIn`, Home create/edit и вход из `ShopDetail(openCheckIn)`. Гостевое приглашение при старте остаётся в учёте пакета 11 до переноса. | Связать с пакетами 3, 5 и 6 по текущим callback-контрактам. Проверить вход из каждого места, черновик и отмену, ошибки и повтор публикации, обработку фото, обновление ленты и истории после сохранения. |
| 9. Полка | Превью полки в профиле, `ShelfArchive`, `ShelfAddBag`, `ShelfBag`, выбор пачки из Home. Сохранить native photo picker и существующую обработку изображения и распознавания. | Согласовать входы с пакетами 6 и 8. Проверить возврат выбранной пачки, отмену камеры или загрузки, ошибку распознавания и изоляцию фото и записей между аккаунтами. |
| 10. Бизнес и заявки | `BusinessTab`, `MyBusiness`, `AddShop`, `AddRoaster`, `SuggestShopChange`, `ShopChangeEditor`, `MyContributions`, `ShopChangeRequestDetail`. | Пакеты 3 и 5 дают сквозные входы из аккаунта и каталогов. Проверить бизнес-режим и состав вкладок, статус заявки, редактирование, возврат и доступ гостя. Списки заявок и редакторы можно принимать отдельными сценариями. |
| 11. Корневой запуск и удаление оставшихся iOS-хостов Compose | `Main`, экран ожидания сессии, приглашение гостю, окно обновления и глобальные шторки. Учесть поведение приложения при уходе в фон и возврате. Точки входа — `App.kt`, `MainViewController.kt`, `PlatformRootNavigation.ios.kt` и `NativeScreenController.kt`. | После переноса активных экранов, вкладок и модальных входов. Повторить холодный запуск, deep link и переход из push, восстановление вкладок, смену сессии и согласие аналитики. Удалять iOS-хосты по доказанному отсутствию потребителей. Отдельно проверить Gradle-граф перед удалением Compose из iOS-сборки. |

`FeedGraph`, `CommunityGraph`, `ProfileGraph`, `SettingsGraph` и `BusinessGraph` — идентификаторы существующих вкладок. Их переходы и восстановление покрывает пакет 0 вместе с соответствующим владельцем вкладки.

## Подключение SwiftUI к вкладкам

Фабрика контента получает один и тот же `Navigator.Screen` для корня вкладки и для push.
`IosRootTab` связывает идентификатор вкладки с существующими индексом, символом и страницей
аналитики. Корень и push остаются в `NativeScreenController`. Он создаёт SwiftUI-контроллер
при загрузке и использует Compose для маршрутов без SwiftUI-фабрики.
Проверка `TabDelegateTest` покрывает соответствие идентификаторов и выбор native-контроллера.
Для ручной проверки темы нужен iOS Simulator или устройство.

## Первая параллельная партия

1. **Агент интеграции** ведёт пакет 0. Он фиксирует ручную проверку темы и подключает галерею и настройки к действующей навигации. Его область — `MainViewController.kt`, `PlatformRootNavigation.ios.kt`, `NativeScreenController.kt`, оба Swift-launcher, `project.pbxproj` и список тестов в `ios-verify.yml`.
2. **Агент Shop** берёт пакет 1. Он готовит SwiftUI-галерею, локализацию и узкий адаптер к Kotlin, проверяет сценарии на имеющихся тестах и добавляет регрессию на отмену или повтор, если текущего покрытия недостаточно. Передаёт интегратору фабрику контроллера, параметры маршрута и способ открыть фото.
3. **Агент Settings** берёт пакет 2. Сначала переносит самостоятельные подэкраны языка и города, затем подключает вкладку и политику использования. Передаёт интегратору маршруты и ресурсы, а владельцу модерации — существующий переход к `BlockedAuthors`.
4. **Четвёртый агент, если доступен**, начинает пакет 3 с карты входов и восстановления пароля на тестовых данных. Подключение общего сброса стека согласует с интегратором. При трёх агентах этот пакет остаётся следующим в очереди.

Общие файлы редактирует один агент за раз. Владелец экрана передаёт имя маршрута или вкладки, фабрику контроллера, список ресурсов и контрактных тестов. Интегратор подсоединяет их в том же PR. Каждый готовый к ревью PR открывает свой экран из обычного пути в приложении.

После галереи и настроек можно вести ленту с модерацией, поиск с каталогами и подборки параллельно. Профиль, чекины и полка могут начинаться на тестовом состоянии, пока владельцы согласуют общие входы. Сквозную приёмку этой группы проводят после подключения входа, истории и редакторов. Корневой запуск и снятие хостов закрывают последними.

## Контракт между Kotlin и SwiftUI

- Существующий Kotlin-владелец сценария выдаёт снимок состояния и принимает действия. Адаптер в `composeApp/src/iosMain` переводит их в небольшой Swift-совместимый интерфейс. Модели транспорта, репозитории и Koin остаются на Kotlin-стороне.
- SwiftUI получает зависимости при создании экрана, отображает состояние и передаёт действия обратно. Долговечное состояние и разовые эффекты разделены. Обновления интерфейса идут на главном потоке. При закрытии экрана отменяются подписка и принадлежащая ему работа. Для аккаунтных сценариев смена сессии очищает устаревшее состояние.
- Swift-код хранится в `iosXtool/CoffeePeek/Sources/CoffeePeek` и включается в Xcode и xtool, как пилот темы. Строки экрана разделены по таблицам фич в `ru.lproj` и `be.lproj` и следуют языку приложения. Превью используют тестовое состояние в светлой и тёмной теме. Порог поддержки берётся из текущей конфигурации iOS.
- UIKit сохраняет маршрутизацию, идентичность вкладок, переход назад и системные провайдеры. Для фото используются действующие правила доступа и ссылки из Kotlin. События и показ экрана идут через существующего владельца аналитики с теми же правилами согласия, сессии и видимости.

## Ветки, передача и проверка

1. Создавать ветку и отдельный worktree под пакет от свежей `codex/ios-native`, например `codex/ios-shop-menu-gallery`. Независимые PR направлять туда. Если один PR использует код ещё открытого родительского PR, сначала направить его в родителя, после слияния перенести на `codex/ios-native` и перепроверить diff и CI.
2. До правок подтвердить фактический iOS-вход в свежем `main` и в текущей интеграционной ветке по `Navigator.kt`, `PlatformRootNavigation.ios.kt` и действующему рендереру. Указать в PR пользователя или роль, точку входа, полный экран или шторку, callback для возврата, эффект смены аккаунта и владельца аналитики. Для модального варианта фиксировать отдельную проверку.
3. Для финального коммита проверить сборку Xcode и Kotlin/Native в `ios-verify`. В стеке с временной базой вне `codex/ios-native` запустить `gh workflow run ios-verify.yml --ref <ветка>` и сверить SHA запуска. Android CI на iOS-ветке не запускается. Изменения с влиянием на общий Android-сценарий выделяются в отдельный срез с его проверками.
4. Вручную проверить подключённый сценарий на macOS в симуляторе с тестовым состоянием. Записать ревизию, конфигурацию/устройство, темы, оба языка, крупный шрифт, VoiceOver, кнопку и жест назад и специфические проверки из таблицы. Указать, какие тесты выполнены, какие только собраны и какие проверки по-прежнему требуют устройства, OAuth или тестового аккаунта. Использовать существующие правила репозитория для безопасных проверок аккаунтных действий.
5. В конце волны сверить покрытие с `Navigator.Screen`, `NativeRootTabContent`, корневыми хостами и модалями фич. Для каждого активного iOS-входа записать его Swift-владельца, PR и факт приёмки или причину, по которой Compose ещё используется. После удаления последнего потребителя проверить отсутствие активного iOS-рендеринга Compose и отдельно решить по итогам анализа зависимостей, что можно убрать из фреймворка.

## Визуальная сверка настроек — 2026-11-16

Основание: пять предоставленных владельцем образцов — вкладка настроек, политика
использования, язык, тема и город. Работа от `441ad1b2a` в изолированном checkout
`codex/migration-ios-native-xcross`, доставка в `codex/ios-native`.

План проверки: сохранить Kotlin-состояния, сохранение/повтор, доступность
уведомлений и согласия; собрать SwiftUI с реальным framework, выполнить JVM-тесты
настроек и Android Debug с DEX-проверкой, затем обновить отдельное тестовое
приложение на ранее выбранном iPhone. Без автоматизированных UI-тестов и записей
в реальные аккаунты.

- Вместо системного insetGrouped-списка используются фон/граница/радиусы и
  размеры CoffeePeek из core/design-system; заголовок и пояснение находятся
  перед карточками, иконки имеют цветные подложки, строки выбора — отметку.
- Manrope берётся из существующих Compose-ресурсов. Все статические TTF имеют
  одинаковое внутреннее PostScript-имя, поэтому каждый вес загружается через
  CGFont/CoreText по файлу, без регистрации конфликтующих имён.
- Обработчики и условия доступности сохранены; новые тексты не добавлены.
  Гостевые действия и повтор сохраняют область нажатия не менее 44 pt.
  Обновлены парные светлые/тёмные превью, без production DI.
- Windows: 42 исполненных JVM-теста настроек в 6 наборах, 0 ошибок/пропусков
  (City5, Language12, Theme3, UsagePolicy3, Settings16, AppReview3).
  :composeApp:assembleDirectDebug и verifyDirectDebugRuntimeClasses прошли.
- Первая сборка Swift прошла: Kotlin framework из кэша за 2.5s, Swift27.4s.
  Файлы Manrope присутствуют в app bundle. Последующие прогоны подтвердили
  корректировку ширины значения строки и установку на телефон.
- Kotlin/Native iosTest и Xcode preview rendering на Windows не запускались.
  Полная ручная сверка пяти экранов, крупного текста, VoiceOver, тёмной темы,
  белорусской локали и возврата остаётся отдельной проверкой на устройстве.


### Уточнение гостевой карточки и проверка устройства — 2026-11-16, 23:45

- По дополнительному образцу владельца гостевой блок получил центрированный
  текст15pt, жёлтую кнопку входа48pt и строку регистрации с областью нажатия44pt.
  Отсутствовавшая подпись «Нет аккаунта?» добавлена в native RU/BE из имеющихся
  общих переводов. Действия входа/регистрации и состояние сессии не изменены.
- На выбранный iPhone15,4 / iOS27.0.1 установлен Debug com.coffeepeek.xcross
  1.2.1 (3058), чистая исходная ревизия `d19f0a53f8eb656c9fb918e6d788e74c2d833357` в codex/ios-native.
  Существующий com.coffeepeek 1.2.1 (3050) сохранён; scanner-disabled режим сохранён.
- Финальная локальная Windows-сборка: SwiftPM24.5s, Kotlin14.0s, framework cache2.3s,
  Swift26.8s, упаковка1.3s, подпись3.4s, установка10.2s. Перед обновлением закрыт
  только процесс тестового приложения; данные не очищались. Предыдущая установка
  3057 завершилась успешно за5m23s, несмотря на долгое ожидание после передачи.
- CoreDevice подтвердил работающий процесс после detach. Владелец открыл верх
  настроек; ручной USB-снимок подтвердил Manrope, фон, карточки/подложки и новую
  гостевую карточку в светлой русской версии. Скриншот и signed IPA остаются локально
  в ignored build. Native RU/BE строки присутствуют в конечном bundle.
- Инспектор прочитал подписи, но попытка открыть вложенный экран не подтвердила
  переход; это не засчитано как проверка языка/темы/города/политики. Временный
  мониторинг и overlay инспектора отключены. Полная ручная проверка вложенных
  экранов, тёмной темы, крупного текста, VoiceOver и всех back/cancel сценариев
  остаётся открытой. Никаких аккаунтных действий или изменения согласия не было.


### Нативные вход и регистрация по образцам — 2026-11-16

Область: полноэкранные Navigator.Screen.Auth/Register из гостевых настроек.
Login уже был SwiftUI; Register теперь получает нативный экран через тот же
root controller factory. Вложенная регистрация CheckInLogin пока использует
Compose и остаётся активным потребителем, поэтому общий renderer не удаляется.

План проверки: сохранить RegisterViewModel, defaults условий=false/аналитики=true,
валидацию, ошибки, блокировку повторного запроса, подтверждение email без автологина,
социальный вход и отмену при закрытии. Проверить auth JVM и Android Debug/DEX,
собрать Windows xcross Debug, обновить тот же отдельный тестовый bundle на iPhone,
вручную сравнить пустые формы. Не отправлять формы и не менять реальные согласия.

- Native TextField/SecureField сохраняют autofill, тип клавиатуры, фокус и показ
  пароля; заголовки, капсулы, отступы и кнопки следуют образцам владельца.
- На iOS CheckboxToggleStyle недоступен. Согласия используют системный Toggle
  в button style с SF Symbols и жёлтым цветом; юридические ссылки отделены
  от переключения. Начальные значения приходят из общего ViewModel.
- Цвета и Manrope вынесены из settings-only владельца в CoffeePeekNativeStyle,
  повторяя существующие токены core/design-system; настройки сохраняют оформление.
- NativeAuth RU/BE дополнены существующими переводами. Добавлены парные SwiftUI
  previews регистрации/согласий; Xcode previews на Windows не рендерились.
- Android Debug сборка и packaged DEX checks прошли. Auth JVM результаты и
  Windows iOS сборка/установка фиксируются после завершения проверки.
- Добавлены 3 iosTest для native register session: defaults, подтверждение email
  без SignedIn, отмена до записи аккаунта. На Windows они не исполнены.
- Владение screenShown остаётся у root NativeScreenController; бизнес-аналитика
  и сохранение согласия остаются в существующем RegisterViewModel, без второго сервиса.


#### Проверка нативной авторизации — 2026-11-17

- JVM: 39 тестов impl исполнены (4 suites), 11 domain восстановлены из Gradle
  cache (2 suites); 0 failures/errors/skips. Android Debug/DEX прошли.
- Windows xcross: Swift packages23.8s, Kotlin compile1m04s, framework12m40s,
  Swift35.7s, package1.4s, sign3.8s, USB install10.1s. Debug 1.2.1(3060)
  установлен на прежний iPhone15,4/iOS27.0.1; production1.2.1(3050) сохранён.
- Ручные USB-снимки обеих пустых форм на3060 подтверждают нативный вход и
  регистрацию, Manrope, капсулы и квадратные Toggle/SF Symbols; defaults
  условий=false/аналитики=true видны. Аккаунтные действия не выполнялись.
- Первый снимок выявил системную серую disabled-кнопку и светлый placeholder
  пароля регистрации. Цвета уточнены в нативных Button/TextField/SecureField;
  юридическим ссылкам возвращён SemiBold. Требуется финальная Swift-пересборка.
- Нативные iosTest не исполнялись; dark/BE/крупный шрифт/VoiceOver, клавиатура,
  legal back/cancel и реальный OAuth/создание аккаунта остаются отдельными gates.
- Общий verifyFunctionBodyLimits имеет ранее установленное несвязанное нарушение
  createBusinessOwnershipEntry (56>20); baseline не расширялся.


#### Финальная установка авторизации — 2026-11-17, 00:21

- Debug1.2.1(3062), source `d5c3f7f28eeba3b4d3f632388a4ef930950509d9`,
  установлен на тот же iPhone15,4/iOS27.0.1. CoreDevice подтвердил работающий
  процесс; установленная production1.2.1(3050) сохранена, scanner-disabled режим
  не менялся. Код отправлен в codex/ios-native.
- Повторная Windows-сборка: Swift packages56.8s, Kotlin41.2s, framework cache2.7s,
  Swift30.8s, package1.3s, sign4.0s, install1m17s. Установка ожидала вновь
  запущенное тестовое приложение; закрытие только его процесса завершило update.
- Ручной USB-снимок регистрации3062 подтвердил исправленный серый placeholder,
  SemiBold юридических ссылок, бледно-жёлтую disabled-кнопку, неизменённые defaults
  consent и нативные квадратные Toggle. Пустой Login отдельно проверен на3060;
  общая финальная окраска кнопки дополнительно проверена на регистрации3062.
- Подписанный IPA и build-identity с SHA256 сохранены локально в ignored build.
  Скриншоты не добавлены в Git. Формы не отправлялись, аккаунты не создавались,
  согласия не переключались. Перечисленные выше расширенные ручные gates открыты.


### Apple-вход, гостевой и собственный профиль — 2026-11-17

Запрос владельца: системная Apple-кнопка; одинаковая карточка входа в профиле
и настройках; собственный профиль по новым образцам с раскрытым месяцем и
свёрнутой неделей, статистикой, вкладками и карточкой пачки в домашнем чекине.

План проверки: сохранить существующий AppleAuthorizer/callback без второго OAuth,
состояние аккаунта и root screenShown. Гость получает SwiftUI-карточку; смена
сессии пересоздаёт root через существующий key(generation,isLoggedIn), собственный
профиль продолжает использовать feature entry. Календарь принадлежит
check-in-history impl; данные пачки передаются через public API community→profile,
без DTO в UI и без новой загрузки/записи в backend. Сохраняются полный history,
фото, выбор даты, подборки/полка и аккаунтная изоляция.

Проверки: unit/contract для затронутых проекций и календаря, profile/settings/history
JVM, Android Debug/DEX, Windows xcross Debug на прежний отдельный bundle/iPhone.
Ручная сверка нативной Apple-кнопки и гостевого профиля, затем собственный профиль
при доступной сессии владельца; без выполнения OAuth, создания записей, logout
или изменения согласия агентом. Native previews светлые/тёмные используют fake
state; автоматизированные UI/screenshot тесты не создаются.


#### Проверки перед установкой профиля

- Исполнены community/data24, profile/data44, profile/impl72,
  check-in-history/impl85 unit/contract tests: всего225, 0failures/errors/skips.
  Settings/impl42 — UP-TO-DATE. Повторный прогон после выравнивания заголовка
  календаря подтвердил history85 и Android Debug/packaged DEX checks.
- Новый календарь берёт today из существующего локального provider; новые3теста
  покрывают границу года, високосный месяц и ноябрь2026 с шестью строками.
  Нет новых UI-тестов, модулей или зависимостей. Полная история сохраняет свои
  controls; гостевые root controllers пересоздаются штатно при смене сессии.
- Передача safe thumbnail/roaster/coffee slug подтверждена transport→API→domain
  тестами, включая отсутствие данных и запрет подменять пачку фото чекина.
- VerifyFunctionBodyLimits: найденное новым срезом предупреждение фабрики
  устранено явным блоком; остаётся только старое createBusinessOwnershipEntry
  (56>20). Baseline не расширялся. Старые предупреждения previews/Gradle вне
  изменённых новых компонентов не исправлялись в этом срезе.

- Первый iOS прогон3064 остановился до link: androidx.compose.ui.tooling.preview
  отсутствует в текущем native source set, хотя Android его разрешает. Новые
  commonMain previews возвращены на поддержанный org.jetbrains multiplatform
  импорт без изменения зависимостей. Предупреждение Android о deprecation
  принимается до отдельного обновления preview tooling; не скрывает native failure.

- Повтор3065 прошёл Kotlin compile; остановлен агентом на framework link до
  установки после read-only review. Обнаружена неверная маршрутизация OpenShop
  из раскрытого profile calendar: теперь отдельный callback сохраняет shop slug
  и session guard; collapsed VoiceOver сообщает открытие истории. Добавлена
  non-UI регрессия от события ViewModel до callback; приложение не удалялось.
- После исправления callback повторно прошли history86 tests и Android
  assembleDirectDebug с packaged DEX verification (1m12s). Function-body check
  снова сообщает только прежнее ownership56>20; нового нарушения нет.

#### Установка и ручная проверка профиля3066 — 2026-11-17

- Source `0e8a5d5f3`, Debug1.2.1(3066), отдельный com.coffeepeek.xcross.
  Локальная Windows-сборка прошла: framework13m34s, Swift40.4s. При установке
  framework cache2.5s, Swift33.4s, sign3.9s, USB install10.5s.
- После краткого USB disconnect независимый InstallationProxy подтвердил3066;
  CoreDevice подтвердил работающий процесс. Production1.2.1(3050) сохранён.
  iPhone15,4/iOS27.0.1; профиль подписи включает согласованное устройство,
  ARM64/resources и отключённый ONNX scanner проверены. IPA и SHA256 локальны.
- Пользователь самостоятельно вошёл и разрешил снимок открытого профиля.
  Ручной USB review в dark/RU подтвердил collapsed week, today outline,
  вкладки и реальную карточку пачки с обжарщиком, рецептом и стрелкой.
  Скриншот с личными данными сохранён только в ignored build, не в Git.
- Найдено оставшееся визуальное расхождение: на ширине393dp четвёртый счётчик
  переносится во вторую строку, образец содержит четыре в ряд. Не исправлено
  в3066. Expanded month, light/BE/large font/VoiceOver, native Apple button
  и guest card на устройстве в этом прогоне не проверены. OAuth агент не запускал.
- По последнему запросу владельца публикуется текущая установленная сборка;
  дополнительная пересборка ради найденного переноса не запускается.

### Native profile lifecycle and check-in editors — 2026-11-17

Base: `11535611b` from `origin/codex/ios-native`, pulled fast-forward before work.
The current checkpoint fixes disappearing native profile state and matches the
Android profile's typography, spacing, calendar placement/modes and post rows.
Check-in migration continues beyond the already native creation picker: home
create/edit and shop create reuse their existing feature ViewModels, guarded
repositories, draft/photo ports and analytics. Shop editing is not an existing
capability and is not invented by this migration.

Ownership: feature impl owns platform-independent native presentation sessions;
composition owns account leases, factory assembly and navigation; SwiftUI owns
native fields, sheets, calendar/rating controls and layout. No new writer,
backend, Gradle module, permission policy or scanner enablement is introduced.

Verification plan: account-generation and same-account token-refresh regression;
existing profile/home/shop unit suites plus native session contract tests;
Android Debug assembly/DEX; Windows xcross compile/link. On the agreed iPhone,
read-only review of profile return/navigation and empty check-in forms in both
themes, keyboard/photo cancellation and back flows. Real submission, consent,
account changes and camera capture are not performed by the agent. Native tests
that cannot execute on Windows and manual device gates are reported separately.

- Before native build: 881 executed JVM unit/contract tests (Home88, Shop112,
  composition681), zero failures/errors/skips; Profile72 UP-TO-DATE. Android
  assembleDirectDebug and packaged DEX verification passed in2m48s.
- Swift syntax parsing passed (not type checking). Local packaging suite5/5 passed;
  initial unittest module invocation failed on Python import path before tests,
  direct script invocation succeeded. Initial Swift process lacked runtime PATH;
  rerun with installed runtime PATH passed.
- Function-body verification reports two inherited violations: ownership factory56
  and public-profile native factory31, already present at pulled11535611b.
  No new/grown violation from this checkpoint; baseline unchanged.
- First native attempt was stopped during Swift dependency compilation, before
  Kotlin linking: direct xcross invocation bypassed the pulled scannerless linkage
  context and began compiling ONNX. The existing context is now also used by the
  device-run wrapper; the next build uses that same context and bundle validation.
  No affected package was installed. Home custom-bean input now stays in a fixed
  scrollable footer as on Android, including keyboard/short viewport handling.
- Native compile retry stopped before framework link because editor/list both
  declared IosShopCheckInObservation. Editor now reuses the existing identical
  shop observation contract; no behavior/API changes to cancellation.
- The following attempt built ComposeApp.framework in9m54s, then Windows refused
  to start Swift because its command exceeded the process argument limit. This
  is retained as a separate failed build; no device installation occurred.
- Both editor localization tables passed read-only key/placeholder checks
  (Home143 and Shop71 keys in each RU/BE locale). Device version guard tests3/3
  passed; newer installed marketing/build versions fail closed without erasing data.
- All56 Swift application sources passed actual type checking against the current
  generated ComposeApp headers, with an isolated module cache and explicit MapLibre
  framework search path. This is not executable linking or device verification.
  Existing Sendable warnings remain in report/contribution screens outside this slice.
- Windows response-file regression tests3/3 passed with the real Swift driver,
  Unicode/space paths and arguments exceeding32767 characters. A later framework
  build was interrupted without a completion result; its retry failed inside
  SwiftPM/PostHog with compiler exception29. Neither attempt installed an app.
- PostHog isolated replay with a fresh module cache and Swift driver j1 compiled
  all148 objects and emitted its module successfully. The first emission attempt
  overlapped toolchain bundle replacement and lacked the macro server; repeating
  it after restoring the verified macro/runtime files passed. Stale cache as the
  original cause is not proven. Xcross coffeepeek.8 serializes the Windows driver
  and restores selected binary framework copy nodes; framework/response tests7/7 passed.
- Final Windows build passed using xcross2.1.1-coffeepeek.8: source
  `c651f529d07c4ded50ad158b84b752e583761e46`, clean tree at build time,
  Debug1.2.1(3087), `com.coffeepeek.xcross`. Swift dependencies3m37s,
  Kotlin sources10.8s, framework9m41s, Swift host58.9s, assembly1.3s.
  ARM64 bundle/resource validation passed, including MapLibre embedding and
  absence of ONNX. Unsigned IPA and executable hash are retained locally.
- The changes are published to `origin/codex/ios-native`. USB preflight after
  assembly did not find the agreed iPhone. Signing, installation, profile
  navigation/foreground checks and empty native editor visual review are pending
  device connection. This build has not yet been claimed installed or device-verified.
  iOS-only tests remain authored but unexecuted on Windows; real-account writes
  and full check-in flow migration beyond these editor entries remain separate gates.

#### Device3088 failures and corrective checkpoint

- Installed Debug1.2.1(3088), source `3d131fd13` (documentation-only successor
  of the successful3087 source), on the agreed iPhone15,4/iOS27.0.1. Independent
  InstallationProxy and process checks passed; main CoffeePeek1.2.1(3108) remained
  unchanged. Framework cache2.0s, signing3.3s, USB install13.6s.
- Manual light/RU review confirmed four statistics in one row and expanded native
  calendar placement. User initially confirmed tab return retained the profile.
  Protected check-in, calendar and bag thumbnails displayed placeholders.
- Opening Home create crashed with UIViewControllerHierarchyInconsistency:
  the dialog layer contained the view while the outer camera host owned its child
  controller. User then relaunched and observed inactive/blank profile again.
  The earlier tab-return result does not certify cold-launch recovery. The run
  wrapper returned0 despite the captured crash; it is not startup acceptance.
- Home and Shop editors now use scoped UIKit containment resolved from the mounted
  responder chain; the existing global dialog camera-host workaround remains.
  Profile/Home images use the existing permission-aware native viewer stream and
  clear on invalidation, background or cancelled request. No new photo writer.
- Own-profile session lifetime now belongs to its hosting controller, with Debug
  reason-only diagnostics for invalidation. Cold-start root cause is still under
  investigation; account and privacy guards remain unchanged.
- Corrective checks: all56 Swift files typechecked; media71 JVM tests passed;
  Android Debug/packaged DEX checks passed in19s. Body-limit check still reports
  only the two inherited violations. Five media and two lifecycle iOS non-UI
  regressions are authored but unexecuted on Windows. Device recheck is required.


### 2026-11-17 — Unified owner profile history (Android and native iOS)

Owner scope: replace the separate own check-in history page on both platforms with one
paginated mixed shop/home list in Profile. The calendar filters that same list; no second
daily list, source tabs or “all check-ins” link. Keep standalone check-in detail/edit routes.
Public profiles of other authors retain their separate public-only access contract.

Ownership: check-in-history owns private account-bound reads, merged pagination,
calendar/date filtering and shared presentation state. Profile owns header and tabs,
receiving a calendar/list scope through its public slots. Application composition binds
existing repositories, account leases, navigation and photo rendering. Native SwiftUI
uses an account-owned history session over the same CombinedHistoryViewModel.
No new backend, writer or Gradle module.

Retirement inventory: own-profile preview/history links, VisitedPlaces/HomeCheckIns root
routes, notification CheckIns destinations and shop uncertain-delivery history actions.
Redirect these to Profile; retain Home detail/edit access independently. Analytics creation
continues through the existing picker/editor tracker; Profile remains the owning screen.

Verification plan: unit regressions for date selection across month boundaries, mixed
pagination and stale account/date responses; affected JVM suites; Android directDebug
assembly with packaged DEX checks; native framework/Swift build. Manual device gates:
expand/collapse and month transition, uniform photo/plain day cells, same-list date filter
and reset, pagination, content-sized plus sheet, collections light/dark surfaces. No live
writes or consent changes. Prior 3088 crash evidence remains above; the owner reports
3089 photos and profile persistence now appear fixed, without a full cold-start matrix.

Verification before the native build: Android directDebug assembly and packaged DEX
runtime-class checks pass. JVM suites: history 90, profile 73, composition 685;
848 tests, zero failures/errors/skips. The first run stopped at a missing import in a
new regression; the second stopped because the packaged-class allowlist still named
retired page classes. Both were corrected; the final run passes. Existing preview/API
deprecation warnings remain. Function-body verification reports only the two inherited
violations (ownership factory and IosPublicProfileSession); no new violations, baseline
not expanded. Native build and manual device acceptance are still pending.

Explicit history intents from notifications/uncertain-submit actions select the profile
check-ins tab and clear its date filter. Ordinary tab switching preserves the section
and filter. Calendar collapse retains the selected date and anchors its week to that
date (otherwise today in the displayed month, or the first of a browsed month).
Calendar reads cover adjacent-month days in that week with stale-response guards.


Native result: installed CoffeePeek Xcross 1.2.1 (3092), source c9395d16e,
Debug/scanner disabled, on iPhone15,4 / iOS 27.0.1. InstallationProxy independently
confirms 3092; CoreDevice confirms one running test-app process. The existing
com.coffeepeek 1.2.1 (3108) remains installed. In-place update, no data reset.
Full framework build: 10m00s; Swift Runner 57.8s. Installation preparation reused
the framework in 2.0s, signing took 3.4s and installation 13.7s.

Original native failures retained: first Kotlin compile required ExperimentalComposeUiApi
opt-in for transparent hosts; first Swift preflight rejected writing the read-only
accessibilityReduceMotion environment in a preview. Both corrected. All 56 Swift
files then passed typecheck, and the actual Runner compiled successfully. Real
Reduce Motion remains respected. Native non-UI tests were not executed on Windows.
Manual acceptance of animations/filtering/picker height/collections themes and
Android manual UI review remain pending; successful install is not UI acceptance.
Local signed IPA and sanitized build identity remain under ignored build/xcross-ios.

### 2026-11-17 — Profile visual corrections after 3092

Owner feedback: use actual month row count and a month-only title; synchronize calendar
height with profile tabs; retain photos in the collapsed week. Remove global profile
insets, reuse the feed check-in renderer (including yellow likes), and replace the edit
profile form with native inputs and a save button without a separate background plate.
Android shares the calendar and timeline corrections; native editor integration is iOS.

Verification plan: retain selected-date filtering, pagination, account-scoped image
authorization, feed actions/exposure ownership and existing profile-save validation.
Run history/profile/community/composition JVM suites, Android directDebug assembly
and packaged DEX checks, then the scanner-disabled native Debug build. Device review
is read-only: calendar/photo transitions, feed/profile card parity, native empty/edit
form layout and cancellation; do not save account changes or send reactions.

Before changes, USB captures of 3092 showed the selected 17 November photo both in the
expanded month and collapsed week. The reported missing photo did not reproduce in
that exact scenario; this is not proof for every date or lifecycle transition.
Captures stay local under ignored build/. Native cells now receive explicit geometry
as part of the uniform-size correction; protected media policy remains unchanged.

Implementation: community cards now compose the same design-system timeline primitives
as Android history/profile. The optional iOS presentation seam preserves localized labels,
allowed callbacks and feed exposure tracking, and mounts the same SwiftUI check-in card
used by native own/public profiles. Text has local insets; the multi-photo strip scrolls
through the full viewport. Card height follows its measured content, and mounted UIKit
containment reuses the editor host with cooperative scrolling. Account replacement clears
the card's presentation and invalidates its actions and image observations.

Edit Profile uses native fields over the existing feature ViewModel, confirmation and
partial-save handling. A revoked native snapshot clears the draft; account generation is
checked before writes. Existing avatar picker/compression and resource translations remain.

Android verification: directDebug assembly and packaged DEX class checks pass; 911 JVM
tests passed (history 91, profile 77, community 58, composition 685), zero failures/errors/
skips. Initial compile failed on passing a composable function reference to let; replaced
it with an inline lambda, then the full run passed in 1m01s. Body-limit verification still
reports only the same two inherited violations; the baseline was not expanded. Native
build and manual visual acceptance are pending.

Native preflight: all 65 Swift files typecheck with the newly exported Kotlin header.
The first native compile required matching nullable expect/actual return types; the
first Swift check required an explicit local capture of the profile editor. Both were
corrected. The shared-card renderer also passed an isolated Swift check. Both application
entry points register the native editor/card factories. Full framework/link/install
and device acceptance remain pending.

Native build/install result: full framework completed in 9m54s and Runner in 1m03s.
Installed CoffeePeek Xcross 1.2.1 (3094), source f5a6791a9, on the agreed iPhone15,4 /
iOS27.0.1. Installation preparation reused the framework in 2.1s; signing took 3.5s,
USB installation 14.5s. InstallationProxy independently confirms 3094 and unchanged
main app com.coffeepeek 1.2.1 (3108); CoreDevice confirms one running test-app process.
The update preserved data and the scanner remains disabled. Code is published to
codex/ios-native. Signed IPA/build identity are local under ignored build/xcross-ios.
Manual device review of the new calendar/card/editor and Android visual review are
still pending; successful compilation and startup alone do not certify those gates.

### 2026-11-17 — native check-in details and Settings top inset

Owner feedback: Home/shop detail backgrounds and custom navigation diverge; Settings
retains a fixed top safe-area strip, and Profile title is smaller than Settings.
Scope: optional feature presentation seams reuse existing ViewModels/actions and
compose one native detail body using the existing feed/profile card. UIKit owns
detail navigation. Settings scroll extends under the status bar with initial title
clearance inside scroll content; Profile and Settings share the 28-point bold token.

Verification plan: Home detail presentation/action regressions, community and app
composition JVM tests, directDebug assembly with packaged DEX checks, Kotlin body
limits, full native framework and Swift compilation, then an in-place USB update.
Preserve account revocation, protected photo variants, deletion/back guards, editing,
analytics ownership and real-account data. Manual read-only light/dark detail and
scrolled Settings checks remain device gates; no live write or consent test is planned.

Android verification: 835 JVM tests pass (Home detail/editor 92, community 58,
composition 685), zero failures/errors/skips; directDebug APK assembly and packaged
DEX checks pass. First run failed to compile two new test assertions due to inferred
sealed-action subtype lists; explicit expected list types fixed them. The complete
retry passed in 34s. Function-body verification reports only the two inherited
violations (business ownership factory and public-profile session); no new/grown
violations and no baseline changes. Native compilation and device checks are pending.

Native preflight: Kotlin sources compile and all 68 Swift files typecheck against
the newly exported framework header. The initial Kotlin attempt required explicit
UIKit category-property imports; the corrected compile passes. Native detail back
uses the feature callback (including revoked-account close); ordinary community
details retain the native swipe gesture. Full framework/link and device review remain
pending. Both startup entry points and the Xcode source list register the detail host.

2026-11-18 native delivery: full framework passed in 13m35s, Runner in 1m28s.
Installed CoffeePeek Xcross 1.2.1 (3096), source f2e04e860, on the agreed iPhone15,4 /
iOS27.0.1. The install preparation reused the framework in 2.4s; signing took 6.0s,
USB installation 15.4s. InstallationProxy confirms 3096 and unchanged main app 3108;
CoreDevice confirms one running Xcross process. No data clearing or downgrade.
Signed IPA and source/build identity are retained locally under ignored build/xcross-ios.
Code is published to codex/ios-native. Scrolled Settings and Home/shop detail visual
acceptance on the device remain pending, as does Android manual UI review.

Read-only device check: user opened Settings and scrolled to Other settings.
USB capture in dark theme shows content continuing beneath the status bar and no
fixed root title/top safe-area band. No settings, consents or account data changed.
Capture stays local in ignored build/. Root headings at scroll origin and both detail
variants still need visual review; this one capture does not certify those states.

### 2026-11-18 — detail menus, collection covers and native community feed

Owner accepted Settings safe area and the profile check-in list. Follow-up scope:
Home edit/delete move into the same top ellipsis menu as other check-ins, community
detail title becomes Check-in, collection covers fade from clear at the top to dark
at the bottom, and the iOS feed becomes a native list. Shelf is explicitly excluded.

Verification plan: preserve existing shared feed ViewModel and mutation/analytics
callbacks, guest first-card/teaser access, saved confirmations, refresh/pagination,
editor/report/photo overlays, account revocation and protected media permissions.
Run Home/community/collections/media/composition JVM checks, Android directDebug
assembly with DEX verification, body limits, Swift typecheck and native build. Finish
with an in-place iPhone update and read-only feed/detail/collection visual checks.
Do not create records, like, change visibility or delete user data during validation.

Implementation ownership: feature/community supplies a minimal native presentation
and original callbacks; composition adapts media/session and measured exposure.
SwiftUI owns native list/refresh/geometry and reuses the existing shared check-in card.
The native feed keeps the real pinned title and notifications slot; ranking stays
disabled as in the running app. Android retains the active Compose renderer.

Verification: 993 JVM tests pass (Home 93, community 65, collections 79, media 71,
composition 685), no failures/errors/skips. Android directDebug assembly and packaged
DEX checks pass. Latest refresh-mutation regression executed separately with body
verification; only the same two inherited body-limit violations remain. Account epoch
is separate from refresh/scope generation so native pull-to-refresh retains its host;
account replacement recreates it. Overlay callbacks are sanitized without dropping
card keys, and exposure dwell is keyed per visible card using the existing ledger.
Native compilation and device review are pending. Profile follow-up is committed on
its checkpoint; the feed continues in child branch codex/migration-ios-native-feed.

Native preflight passes: Kotlin iOS sources compile, all 73 Swift sources typecheck
against the updated header. Both entry points and the Xcode source list register the
feed host. Full framework/link and device checks are still pending.

Native delivery: framework passed in 13m45s and Runner in 1m36s. Installed CoffeePeek
Xcross 1.2.1 (3099), source 2f96432fa, on the agreed iPhone15,4 / iOS27.0.1. Installer
reused the framework in 2.5s; signing 4.8s, installation 15.1s. InstallationProxy
independently confirms 3099, CoreDevice confirms one running Xcross process, and main
app com.coffeepeek remains 1.2.1 (3108). Data preserved, scanner still disabled.
Code is pushed to codex/ios-native; signed IPA and build identity are local in ignored
build/xcross-ios. Manual feed/menu/cover verification remains pending; startup alone
does not prove pagination, refresh, theme parity or mutation acceptance.


### 2026-11-18 — native global loading and error UI

The final root audit found Compose-rendered LoadingDialog, ErrorDialog and the
startup language-loading state above native screens. Replace them with SwiftUI
hosts, retaining the existing LoadingHandler/ErrorHandler state owners, translated
messages/dismiss actions and original 96pt mascot. The loader uses the exact 320
reference radius samples from core/design-system; both renderers share immutable
normalized contours. SwiftUI retains the same Catmull-Rom contour, 1600ms rotation,
2800ms fill up to0.72, 5000ms morph and cubic easing; reduced motion freezes it.
Android's existing error visuals are preserved through cohesive component extraction.
The public profile factory now has an explicit block body, removing its inherited
body-scanner false positive without changing construction or account behavior.

Verification plan: core design-system unit suite, unchanged-contour comparison,
new native dialog callback contracts, Xcode build and Kotlin/Native. The local
unit suite passes; body limits leave only the existing ownership factory violation,
which the Business checkpoint is replacing. Device review remains for blocking all
underlying touches, error dismissal, font sizes, both themes, reduced motion and
returning from a camera/modal without a retained loading surface.

### 2026-11-18 — native root modals and account-deletion status

Scope: replace the real DeleteAccountPending route via the existing native Settings
factory, guest-launch onboarding and the root App Store update card. The profile
ViewModel keeps status/email confirmation/resend timing/session events. The shared
AppUpdateController and StoreUpdateInstaller retain update policy, persisted dismissal
and URL opening. Onboarding retains its five original feature-owned illustrations,
ru/be text, guest/login/register destinations and two-second rotation with press/hold;
VoiceOver pauses automatic rotation and reduced motion disables its transition.

Verification plan: Xcode simulator build, native modal contracts (single completion,
closed callback revocation, required/optional updates and existing installer), account
session replacement and queued-send regressions, tooling/resource packaging checks.
Do not send deletion emails or modify a real account. Native light/dark previews are
colocated with each screen. The AppUpdate feature has Russian resources only, so its
Belarusian table preserves the existing Russian fallback rather than claiming a new
translation. Remove obsolete iOS-only Compose update entry after its last consumer.

Local JVM verification: 15 executed profile tests passed (4 native session tests and
11 existing AccountDeletionViewModel tests), no failures/errors. First invocation
could not locate the Android SDK; rerun with the installed SDK path passed. Native
compilation, Kotlin/Native execution and manual iPhone acceptance remain pending.
