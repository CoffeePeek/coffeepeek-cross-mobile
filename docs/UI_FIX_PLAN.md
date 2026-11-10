# План UI-фиксов: фидбек касаний и анимации

Обзор от 2026-11-10 по состоянию develop `c35207f34`. Разделы 1–4 — реализованы в
PR `fix/ui-feedback-fixes`; разделы 5–6 — очереди на будущие PR с точными местами
и предложенными решениями. Скриншоты-улики лежат локально вне репозитория
(AGENTS.md запрещает трекать снимки) в `/Users/motvii/StudioProjects/coffeepeek-ui-evidence/`
(`before/` — поведение до фиксов, `after/` — после).

## 1. Сердечко: нажатие отвечает только сердцем — ✅ реализовано

**Проблема.** У всех лайков визуальный отклик касания охватывал область больше
самого сердца:

- `CoffeePeekLikeButton` скейлил весь 48dp-контейнер (`coffeePeekGlassPress` на
  контейнере) и для Glass-стилей подсвечивал весь диск
  (`CoffeePeekNativeControlBackground(pressed)`): нажатие в углу невидимой зоны
  заставляло «дёрнуться» сердце и стеклянную таблетку целиком
  (улика `before/glass_heart_disc.png` — сердечко в карточке кофейни занимает
  центр большого стеклянного диска).
- `PostCard.HelpfulButton` («полезно» в посте сообщества) вешал `clickable` с
  дефолтным риплом на всю строку «сердце + счётчик» — рипл накрывал всю строку,
  а не сердце.

**Фикс.**

- `CoffeePeekLikeButton.kt`: контейнер больше не скейлится и не передаёт
  `pressed` в фон; scale 0.88 при нажатии получает только иконка сердца
  (`graphicsLayer` на `CoffeePeekLikeIcon`). Зона касания 48dp сохранена —
  отзывчивость доступности не пострадала.
- `CoffeePeekLikeIcon.kt`: цвет сердца теперь анимирован
  (`animateColorAsState`, 180ms) — переключение избранного больше не мгновенная
  подмена. Один фикс красит все сердечки приложения (shop/roaster/каталог/пост).
- `PostCard.HelpfulButton`: рипл переехал на круг сердца 44dp
  (`clip(CircleShape).clickable`); строка сохранила расширенную зону касания
  (без рипла) и мерженую семантику для TalkBack.

## 2. Полка в профиле: дёрганый возврат — ✅ реализовано

**Проблема.** Профиль → Полка → открыть пачку → назад: недельная лента в шапке
профиля на каждый возврат мигала скелетоном — 7 серых плиток вместо дней недели
(улики `before/shelf_return_skeleton_flash.png` против `after/shelf_return_fixed.png`).

**Причина (цепочка).** `ProfileActivityApiImpl.kt:55` шлёт `Resume` на каждый
`ON_RESUME`; `ProfileActivityViewModel` на каждый Resume, кроме первого, зовёт
`refresh()` → `loadShop/loadHome` безусловно ставили `isLoading = true` →
`HistoryCalendarCard.kt:71` рисует скелетон, пока данные не приедут заново.

**Фикс.** `ProfileActivityViewModel.loadShop/loadHome`: `isLoading =
!hasLoaded` — фоновый refresh при уже загруженных данных не трогает видимое
содержимое (stale-while-revalidate). При смене месяца данные сбрасываются явно,
и скелетон по-прежнему показывается как надо.

Туда же: `ShelfViewModel.refresh()` больше не поднимает `loading = true` при уже
загруженной полке (`loading = !loaded`) — на каждый возврат полка обновляется
тихо.

**Проверено на эмуляторе** (Sandbox, directSandbox): кадры возврата
`after/return_*.png` идентичны друг другу и содержат готовую ленту недели; до
фикса первый кадр был скелетоном.

## 3. Очередь багов (подтверждены разбором, ещё не фикшены)

### 3.1. Slide-направление Business-таба
`MainScreen.kt:307-313` — `tabIndex()`: BusinessTab и SettingsTab оба попадают
в `else -> 4`. В бизнес-режиме доступны только эти два таба, переключение между
ними всегда слайдит влево, в обе стороны. Фикс: явная ветка
`hasRoute<Navigator.Screen.BusinessTab>() -> 0` (в личном режиме таб недостижим,
ветка ни на что не влияет).

### 3.2. Битый драфт отключает autosave до переоткрытия шторки
`AppShopCheckInDraftStore.kt:39-47` — `open()` ставит `leases[key] = token`
после `storage.read()`; битый манифест роняет read, lease не выставляется, все
последующие `save()` падают с «Draft binding was replaced». Парадокс: битый
драфт (который полезнее всего перезаписать свежим) нельзя перезаписать. Фикс:
ставить lease до `read()`; fail-open → следующий save самолечит файл.

### 3.3. Гостевой онбординг: бесконечный autoplay на последнем слайде
`GuestCheckInOnboardingAutoAdvance.kt:33-39` — после последнего слайда каждые
2с запускается `animateScrollToPage(currentPage + 1)`; пейджер клампит
(визуально ничего), но цикл живёт вечно. Фикс: завершать цикл
(`if (pager.currentPage == pager.pageCount - 1) return@repeatOnLifecycle`) или
дисмиссить онбординг по окончании.

## 4. Анимации: очередь по убыванию заметности

| # | Что | Где | Предложение |
|---|---|---|---|
| 1 | Loading→content→error — рывок | ShopDetailScreen.kt:219,249; RoasterDetailScreen.kt:114; CoffeeDetailScreen.kt:104; PostScreen.kt:111; LeaderboardStatus.kt:26; NotificationsSheet.kt:157; ChangeEditorScreen.kt:92; LanguageScreen.kt:55 | `Crossfade`/`AnimatedContent` на статус-развилке |
| 2 | Табы профиля: контент `when`-подмена + underline не скользит | ProfileContentTabs.kt:54-64; CoffeePeekUnderlineTabs.kt:56 | crossfade + `animateDpAsState` на offset индикатора |
| 3 | Ошибки форм появляются мгновенно | LoginScreen.kt:107; ShopCheckInCreateScreen.kt:211-218; OwnershipCheckInQrAction.kt:93; NotificationPushSettings.kt:71 | `AnimatedVisibility` (в AuthChrome.kt:311 образец уже есть) |
| 4 | Фото-сетка: add/remove без анимации | CoffeePeekPhotoAttachments.kt:45 (4 экрана-потребителя) | fade-in превью + `animateContentSize` на FlowRow |
| 5 | Кнопка submit: label↔спиннер рывком | AuthChrome.kt:345; ShopCheckInCreateScreen.kt:144,162 | `AnimatedContent` |
| 6 | Выбор (язык/тема): галочка мгновенная | LanguageScreen.kt:104; ThemeScreen.kt:75 | fade/scale у иконки |
| 7 | Сегмент-контрол: фон сегмента мгновенный | CoffeePeekCapsuleSegmentedControl.kt:99 | `animateColorAsState` |
| 8 | Бейдж unread без pop | NotificationsButton.kt:105 | scale-pop при появлении |

## 5. `platformReduceMotion`: нарушители собственной конвенции

Уважают настройку только discovery, табы MainScreen и photo-viewer. Игнорируют
(пользователь с «уменьшить движение» всё равно получает движение):

- корневые переходы `Navigator.kt:493-528` (фикс 300ms) — при том что табовые
  переходы в `MainScreen.kt:221-232` уважают;
- полка целиком: ShelfScreen.kt:100, ShelfAddScreen.kt:68, ShelfCameraContent.kt:260,264,
  ShelfFinishZone.kt:60, ShelfFinishSheet.kt:120, ShelfLanding.kt:25;
- AddShopScreen.kt:154; ShopScheduleSection.kt:102,117; ShopCheckInNote.kt:34;
  AuthChrome.kt:311; FloatingBottomNavBar.kt:97; CoffeePeekGlassPress.kt:14;
  CoffeePeekNativeSwitch.android.kt:33; отскок шитов SwipeDismissModalBottomSheet.kt:101.

Фикс-паттерн уже есть в кодовой базе: `val duration = if (platformReduceMotion()) 0 else 360`
(DiscoveryTabState.kt:74) — распространить на перечисленные места. Новые фиксы
из разделов 1–2 делают press-scale и перекраску сердца через `animateFloatAsState`/
`animateColorAsState` — при подключении reduceMotion сюда добавить те же нулевые
длительности.

## 6. Не баги (проверено и снято с учёта)

- Сердечки карточек (PhotoBadge) не имеют рипла и не должны: `indication = null`
  в `CoffeePeekLikeButton` — «рипл» на стеклянных дисках давал контейнер,
  исправлено в п.1.
- `ProfileOwnShelf` с `signedIn = true` — безопасно: гостю вкладка профиля не
  показывается (`NativeTabRoutes.kt:11`, редирект `MainScreen.kt:176`).
- Пресс-scale сердца отключён на iOS native glass намеренно (`CoffeePeekLikeButton.kt`
  early-return в native presenter) — iOS даёт собственный отклик.
- Локальная сборка iOS: падение `dumpXcodebuildArgs` — битый кэш SPM-артефакта
  onnxruntime; лечение `rm -rf build/kotlin/swiftPMCheckouts build/kotlin/swiftSyntheticPackages build/kotlin/swiftPMXcodeDumps`.
