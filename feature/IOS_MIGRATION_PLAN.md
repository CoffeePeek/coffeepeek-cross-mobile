# План миграции iOS на новые feature-модули

Статус: принципы согласованы 2026-10-24; ход работ — в разделе «Статус» ниже. Основан на сверке с продом (`origin/main`) по всем владельцам: инвентарь Gradle-модулей,
iOS-документы (`docs/IOS_NATIVE_EXPERIENCE.md`, `IOS_NATIVE_NAVIGATION.md`, `IOS_COMPOSE_INTEROP_AUDIT.md`, `IOS_MAP_PARITY.md`,
`IOS_FEED_PHOTO_MEMORY.md`, `IOS_SETUP.md`), `feature/LEGACY_REMOVAL.md`, `feature/roaster/IOS_MIGRATION_PLAN.md`.
Ничего из описанного здесь не проверялось на симуляторе или устройстве: документы сами отмечают, что iOS-часть проверена только компиляцией.

## Статус на 2026-10-24 (ветка `feat/ios-build-foundation`, локальная, не запушена, PR нет)

Сделано и проверено сборкой и юнит-тестами (Android и iOS compile; iOS-фреймворк линкуется; тесты сессии прошли и на iOS-симуляторе).
На симуляторе и устройстве экраны **не запускались**.

**Этап 0 — сборка: готов.**
- iOS-таргеты добавлены: shop, shop-report, favorites (api и impl), check-in-history api, home-check-in api.
- UI, строки и ресурсы перенесены в `commonMain` у 11 владельцев: app-update, notifications, settings, auth, community, profile, discovery, coffee, contributions, home-check-in, check-in-history. Roaster, shop, shop-report и favorites уже были общими.
- В `androidMain` остались только Android-части: установщики обновлений, Google-чузер, форматтер даты ошибки смены имени (Android-actual).
- Найдено по пути: JVM-only `toSortedMap` в `CoffeeListViewModel` (iOS его не собирал) — исправлено.
- В `kotlinx-datetime` переведены даты в notifications, contributions, home-check-in.

**Этап 1 — нативные хуки в design-system: готов.** Через точки подключения (`LocalCoffeePeek*Presenter`); iOS ставит UIKit-реализации одним местом, `ProvideDesignSystemPlatformHooks` в `composeApp`, Android рисует Material как раньше:
шторка, native header, date picker, alert, выбор фото, haptic, scroll-to-top, роли клавиатуры (AutoFill, «Готово»), UIMenu для ActionMenu, glass-кнопки, переключатель с haptic, спиннер, `CoffeePeekBackHandler`, `CoffeePeekScrollableColumn`; `CoffeePeekActionMenu` перенесён в commonMain.
Не использовано экранами пока: `CoffeePeekScrollableColumn`, `nativeAction` в header; native-иконки у части glass-кнопок и меню не проставлены.

**Этап 2 — сессия: F23 и F24 готовы** (выход стирает старые строки; сбой удаления Keychain сообщается). Core session port вместо `SessionRepository` (~43 файла) — **не сделан**.

**Этап 3 — подключение на iOS: начат.** Через новые модули на iOS работают: вкладка Settings (и три её подэкрана), окно обновления, колокольчик уведомлений, экраны входа, регистрации и восстановления пароля (без кнопок Google и Apple).
Остальное на iOS по-прежнему legacy: Coffee, Community, Profile, Discovery и всё остальное, вкладки Coffee, Community, Profile ещё вызывают legacy-экраны напрямую.

**Важные находки для следующих срезов**
1. Kotlin/Native не линкует переопределение composable-метода интерфейса с аргументом по умолчанию (`modifier: Modifier = Modifier`): ошибка видна только при линковке и только у достижимого кода. У каждого владельца при подключении убрать значения по умолчанию из api-интерфейсов (так сделано у Settings и Auth).
2. Подключение владельца требует: перенести platform-neutral адаптеры из `androidMain` в `commonMain` (как у Settings) или добавить iOS-пары, перенести зависимость `impl` в `commonMain.dependencies` composeApp и добавить фабрику в `PlatformKoin.ios.kt`.
3. Вход через Apple и Google на iOS отсутствует в новом auth (в прод-версии есть). Проект: домен `beginAppleSignIn` и `signInWithApple`, порт `AppleAuthorizer` поверх `IosAppleAuthProvider`, Google через `GoogleSignInLauncher` поверх `IosGoogleAuthBridge`, кнопки и тесты. **Не начат.** До его готовности переключать iOS-вход в релиз нельзя.
4. Для сообщения проверки обновлений на iOS нет тоста: временно используется `ErrorHandler.showError`, нужно решение по UX.

**Остаётся**
- Этап 3: Community, Discovery (+вкладка Лента), Coffee (+вкладка), Profile (+вкладка), Contributions, Shop и Shop-report, Check-in history, Home check-in и общий create flow, Favorites (по решению продукта); вход через Apple/Google.
- Этап 2: core session port.
- Roaster: Xcode-линковка, симулятор, устройство.
- Этап 5: удаление legacy — только после полной готовности и ручной приёмки.
- Push на iOS (`PushAppDelegate`, `NativePushBridge`) и Xcode/устройство-гейты не трогались.

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
