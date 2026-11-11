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

### 3.1. Slide-направление Business-таба — ✅ реализовано
`MainScreen.kt:307-313` — `tabIndex()`: BusinessTab и SettingsTab оба попадают
в `else -> 4`. В бизнес-режиме доступны только эти два таба, переключение между
ними всегда слайдит влево, в обе стороны. Фикс: явная ветка
`hasRoute<Navigator.Screen.BusinessTab>() -> 0` (в личном режиме таб недостижим,
ветка ни на что не влияет).

### 3.2. Битый драфт отключает autosave — ✅ реализовано (lease до read)
`AppShopCheckInDraftStore.kt:39-47` — `open()` ставит `leases[key] = token`
после `storage.read()`; битый манифест роняет read, lease не выставляется, все
последующие `save()` падают с «Draft binding was replaced». Парадокс: битый
драфт (который полезнее всего перезаписать свежим) нельзя перезаписать. Фикс:
ставить lease до `read()`; fail-open → следующий save самолечит файл.

### 3.3. Гостевой онбординг: autoplay — ✅ реализовано (стоп на последнем слайде)
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

---

## 7. Полный аудит по зонам (2026-11-10, дефект-хантинг субагентами)

Приложение разбито на 8 зон, каждая проверена отдельным агентом по UI/VM-слою
(разделы 1–6 этого файла исключены из отчётов). Все находки подтверждены по
окружению (вызывающие, платформенные actual, DI). Скриншоты состояний —
локально в `~/StudioProjects/coffeepeek-ui-evidence/`. Краткий шортлист — в
конце раздела.

### 7.1 Shop: детали + чекин

1. **[BUG]** Ошибка голоса «полезно» невидима: тап по сердцу карточки ниже вьюпорта — оптимистичное состояние тихо откатывается, `hasVoteError` рендерится статусом вверху списка за кадром — ShopCheckInListViewModel.kt:163 + ShopCheckInListScreen.kt:119. Фикс: снекбар или inline у карточки, как в ShopDetailScreen.kt:163.
2. **[BUG]** Сбой записи драфта парализует форму: submit требует guard-драфт (159), редактирование блокировано при `deliveryUnconfirmed` (133), выхода «отправить без драфта» нет; определённый HTTP-отказ (400/403) помечается «доставка не подтверждена» (196) — ShopCheckInCreateViewModel.kt. Фикс: разделить флаги, дать отправку без guard-драфта.
3. **[BUG]** Названия напитков в меню не локализуются: всегда `nameRu` (ShopMenuDrinkRow.kt:32) при locale-выборе в карточках (ShopCheckInCard.kt:65) и поле напитка. Фикс: общий locale-выбор имени.
4. **[UX]** Блок «рейтинг + чекины» исчезает целиком при `summary == null` (`?: return` — ShopCheckInRatingsOverview.kt:53), хотя серверный overallRating/count есть. Фикс: строка без категорийных полос.
5. **[UX/ANIM]** Список чекинов полностью перезагружается на каждый ON_RESUME (`isLoading=true` — ShopCheckInListViewModel.kt:43,100) — строка-спиннер дёргает контент. Фикс: окно свежести + `isLoading = !hasLoaded`, как в разделе 2.
6. **[UX]** Заметка/«свой напиток» молча обрезаются (`take(1000)`/`take(100)` — ShopCheckInCreateViewModel.kt:47,55) без maxLength и счётчика — ввод «перестаёт работать». Фикс: maxLength + счётчик.
7. **[ANIM]** Полноэкранные loading→content→error без переходов: ShopCheckInListScreen.kt:104, ShopMenuGalleryScreen.kt:75 (не вошли в раздел 4). Фикс: Crossfade.
8. **[BUG]** Шторка источника фото мгновенно закрывается при достижении лимита/подготовке снимков (`if ... photos.size < MAX` — ShopCheckInPhotoAttachments.kt:29). Фикс: держать шторку, блокировать пункты.

### 7.2 Discovery: карта и поиск

1. **[BUG]** Nearby: таймаут 10с меньше худшего пути фикса (до ~12с: PlatformLocation.android.kt:26-52) — запрос молча роняется, пришедший фикс не применяется, аналитика ложно рапортует Unavailable — SearchViewModel.kt:157-165. Фикс: не ронять, пока `location.refresh()` в полёте.
2. **[BUG]** Гонка `suppressBounds` при повторной паузе: `finally` старой job сбрасывает флаг посреди новой — запросы вьюпорта уходят во время анимации камеры — MapViewModel.kt:479-494. Фикс: токен поколений вместо булева.
3. **[BUG]** Карточка на карте дублирует «Закрыто · Закрыто» (DiscoveryMapShopCard.kt:85-95 + PublicShopPreviewMapper.kt:26-28). Фикс: при isClosed — только статус.
4. **[BUG]** Иконки тегов: рассинхрон нормализации `_`→`-` между artwork-маппингом (DiscoveryTagIcon.kt:14-29) и ярлыками фильтров (FilterCatalogLabels.kt:61) — to-go/laptop/pet падают в заглушку. Фикс: общая нормализация.
5. **[BUG]** Пикер чекина показывает сырой serverMessage вопреки контракту SearchError (CheckInShopPickerList.kt:66 против SearchContract.kt:14). Фикс: локализованная копия.
6. **[UX]** Тупик гео: один отказ навсегда отключает Nearby (persистентный «asked» — DiscoveryHost.kt:36-38, LocationPermissionEffect.android.kt:44), в уведомлении нет пути в настройки. Фикс: soft/hard denial + действие «открыть настройки».
7. **[UX]** Ввод в поиске мгновенно стирает маркеры/карусель до прихода новых (MapViewModel.kt:211-226). Фикс: держать прежние маркеры.
8. **[UX]** Карта: нет loading/error/empty состояний контента вьюпорта (MapState.isLoading не рендерится — MapViewModel.kt:271,320). Фикс: error+Retry как в поиске.

### 7.3 Community: лента и посты

1. **[BUG]** Пустая thumbnail ломает fallback фото: `""` подставляется как есть — вечный лоадер (PostCard.kt:336; контракт допускает `""` — PostMapper.kt:38). Фикс: `takeIf(String::isNotBlank) ?: originals`.
2. ~~Лайк собственного поста~~ — **не баг, задумано** (решение владельца 2026-11-10): лайк своих постов разрешён.
3. **[A11Y]** Гостевой тизер невидим для TalkBack: `clearAndSetSemantics {}` вычищает и пост, и оверлей с кнопкой входа (CommunityGuestTeaser.kt:23). Фикс: merged-узел с Role.Button.
4. **[BUG]** Вьюер фото переживает исчезновение поста (блокировка/смена аккаунта) — PostScreen.kt:99-100. Фикс: закрывать preview при post==null/error.
5. **[UX]** Голосование в полёте: без индикации, повторный тап молча игнорится (CommunityViewModel.kt:243), блокирует pull-to-refresh (:162) и меню всех карточек (CommunityFeedContent.kt:193). Фикс: прогресс на сердце, не блокировать прочее.
6. **[BUG]** Тап лайка отменяет in-flight loadMore → потерянный ответ, двойной запрос и дубль аналитики (CommunityViewModel.kt:247). Фикс: мержить ответ, не трогая голосуемые посты.
7. **[UX]** Пустая лента у гостя без CTA входа и без стилевого empty-блока (CommunityFeedContent.kt:95,177). Фикс: styled empty + вход.
8. **[ANIM]** Мгновенная подмена loader↔контент↔error в ленте (CommunityFeedContent.kt:158-164). Фикс: AnimatedContent на статусах.

### 7.4 Profile + история чекинов

1. **[BUG]** Любая смена visibility-ревизии стирает загруженный профиль (PublicProfileViewModel.kt:126-129): полный скелетон, шит подписчиков закрывается — даже если ревизия не касается этого автора. Фикс: stale-while-revalidate, скрывать только при `hides(автор)`.
2. **[BUG]** Чекин с нечитаемым timestamp всплывает наверх дневника (`sortedBy { parseServerInstant }` — null первым; ProfileActivityState.kt:14). Фикс: переиспользовать compareHistoryMoments.
3. **[BUG]** Фото календаря парсят мгновенья мимо `parseServerInstant` (Instant.parse на offset-less строках — HistoryCalendarPhotos.kt:22) — старое фото вместо нового. Фикс: parseServerInstant.
4. **[UX]** Ошибка загрузки редактирования профиля оставляет пустую несохраняемую форму без retry (EditProfileViewModel.kt:50, VM живёт по key). Фикс: error-state с retry.
5. **[BUG]** В шапке календаря при фильтре «Дома» — «0 кофеен» (HistoryCalendarCard.kt:62-64,75). Фикс: фильтровать фразу по источнику.
6. **[UX]** Экран «все чекины» заблокированного автора = голая строка вместо карточки блокировки (PublicProfileCheckInsScreen.kt:56 против PublicProfileBody.kt:74-86). Фикс: переиспользовать PublicProfileBlocked.
7. **[UX]** Ошибка перезагрузки ленты чекинов невидима при непустом списке (PublicProfileBody.kt:166). Фикс: inline-ошибка независимо от пустоты.
8. **[ANIM]** Подпись месяца «счётчики ↔ Загрузка» — мгновенная подмена (HistoryCalendarCard.kt:71-76). Фикс: Crossfade.

### 7.5 Shelf: полка и сканер

1. **[BUG]** Ошибка съёмки не видна на шаге камеры и «выстреливает» снекбаром при следующем успешном скане (ShelfAddScreen.kt:97-102,121,72). Фикс: эффект на уровень экрана / сразу Retake.
2. **[BUG]** Публичный профиль: скелетон-флэш на каждый ON_RESUME — полный сброс через `initialShelfState` (PublicShelfProfileViewModel.kt:31 + ShelfProfilePreviewScreen.kt:54,56). Фикс: stale-while-revalidate, ревизия только при смене данных.
3. **[BUG]** Finish/UndoFinish молча глотаются при параллельной записи «busy» пачки (ShelfViewModel.kt:129-132) — шторка закрыта, подтверждения нет. Фикс: очередь на пачку.
4. **[BUG]** Offline при рестарте показывается как «Пачка не найдена» без ретрая (ShelfBagViewModel.kt:36-38). Фикс: различать Offline/NotFound + «Повторить».
5. **[UX]** Архив: повторный поиск без индикатора — мгновенная подмена результатов (ShelfArchiveViewModel.kt:55-62). Фикс: linear-progress/crossfade.
6. **[UX]** Поиск кофе в форме пачки: ни loading, ни error, ни retry (ShelfAddViewModel.kt:130-133, ShelfCoffeeField.kt:45). Фикс: спиннер + «Не удалось · Повторить».
7. **[BUG]** iOS-сканер не ловит OutOfMemoryError — краш вместо «Переснять» (IosBagScanner.kt:52-60; Android-версия ловит — AndroidBagScanner.kt:44). Фикс: зеркальный catch.
8. **[BUG]** Подлёт новой пачки: bringIntoView с зашитым 160.dp вместо реального rowHeight (ShelfScreen.kt:158,166 против ShelfGrid.kt:68-71) — низ «НОВАЯ» пачки за краем. Фикс: высота из geometry.

### 7.6 Auth + Settings

1. **[BUG]** Транзиентный каталог городов молча перезаписывает сохранённый город в БД (CityPreference.kt:43-44,28-29) — задевает и discovery (DiscoveryAdapters.kt:70). Фикс: fallback только in-memory.
2. **[BUG]** Сбой хранилища при загрузке городов крэшит приложение (SettingsPorts.kt:61-64: `resolve` внутри `Result.map` не ловит исключения; Room без обработки; select — launch без handler). Фикс: обернуть в CityPreference (образец AppLanguagePreference.kt:47-58); то же в ThemeManager.kt:47.
3. **[BUG]** «Проверить обновления» мёртвая там, где проверки выключены: фейковый спиннер 700мс и ноль обратной связи (SettingsScreen.kt:220-235, AppUpdateAdapters.kt:18, `canCheckForUpdates` нигде не читается). Фикс: скрыть/дизейблить или тост на Disabled.
4. **[UX]** После сбоя каталога строка города навсегда «Не выбран» при сохранённом городе (SettingsViewModel.kt:91 — onSuccess без onFailure; улика `before/settings_offline.png`). Фикс: имя из id + ретрай на возврате.
5. **[UX]** Восстановление пароля: поле активно, но ввод молча игнорируется при busy (PasswordRecoveryScreen.kt:71-77 без `enabled`; на login/register дизейблятся). Фикс: `enabled = !busy`.
6. **[UX]** Кнопка «Отправить» восстановления не использует штатный loading-спиннер (PasswordRecoveryScreen.kt:83-87). Фикс: `loading = state.busy`.
7. **[A11Y]** «Забыли пароль?»/«Создать аккаунт»: кликабелен только текст ~34dp, 48dp-строка не кликабельна (AuthChrome.kt:356-364). Фикс: clickable на строку.
8. **[ANIM]** Регистрация: шаги, маскот и strength-бар мгновенной подменой (RegisterScreen.kt:155-161,353-355). Фикс: AnimatedContent + animateContentSize.

### 7.7 Collections + Contributions

1. **[BUG]** Пагинация заявок: бесшумный вечный retry-цикл (ошибка проглочена, LaunchedEffect перезапускает — ContributionsListViewModel.kt:99 + Screen.kt:125-128). Фикс: футер «Не удалось · Повторить».
2. **[BUG]** AddShop/AddRoaster: описание не ограничено лимитом 1000 (AddShopViewModel.kt:59, AddRoasterViewModel.kt:43; ChangeEditor ограничивает) — отказ сервера на 5-м шаге. Фикс: take + счётчик + isSendable().
3. **[BUG]** 409 в редакторе подборки: «Повторить» молча стирает несохранённые правки (SavedCollectionsStatus.kt:36 → reload(force=true)). Фикс: диалог потери правок или ребейз правок.
4. **[BUG]** Обложки библиотеки: N+1 detail-запросов на карточку без кастомной обложки, без кэша (SavedCollectionCoverImage.kt:33-67 + HttpSavedCollectionsRepository.kt:86). Фикс: URL в summary / кэш по (id, revision).
5. **[BUG]** Публичная подборка: двойная загрузка одного id (CollectionsScreen.kt:26-29 + CollectionsEntryImpl.kt:94). Фикс: передать загруженное или один VM.
6. **[UX]** Выход из редакторов теряет несохранённые правки без подтверждения (SavedCollectionsScreen.kt:31, ChangeEditorScreen.kt:121). Фикс: destructive-confirmation при hasEdits().
7. **[ANIM]** Пикер: мгновенная подмена шторки на quick-create (SavedCollectionsPicker.kt:31-34). Фикс: AnimatedContent.
8. **[UX]** Список заявок не обновляется никогда, кроме ошибки (нет pull-to-refresh/ON_RESUME — ContributionsListScreen.kt:85). Фикс: PTR + reload на resume.

### 7.8 Roaster / Coffee / Home-check-in / Business / прочее

1. **[BUG]** Сетевой сбой при resume молча выкидывает из бизнес-режима (`catch (_: Exception) { reset() }` — OwnershipAccess.kt:39): таб сброшен на Feed, кнопки редактирования исчезли, без сообщения. Фикс: stale-while-revalidate + поверхностить.
2. **[BUG]** Тогл бизнес-режима уничтожает состояние всех табов: `popUpTo(inclusive)` без saveState (MainScreen.kt:130-133 против :141-146). Фикс: saveState/restoreState.
3. **[BUG]** Кнопка «к продавцу» открывает сырой URL: пустая строка (enabled при `!= null` — CoffeeOfferCard.kt:62), `intent://` → краш (CoffeeDetailViewModel.kt:66-68; обжарщики нормализуют — RoasterLinks.kt:11). Фикс: нормализация + безопасный open.
4. **[BUG]** Авто-«прочитано» схлопывает подгруженные страницы инбокса: точечный markRead → полный refresh → `items` = первая страница (NotificationPresenter.kt:196,124). Фикс: обновлять только unreadCount/флаги.
5. **[BUG]** Тап по push у гостя молча проглатывается (FeatureNotificationsRenderer.kt:37-46 — consume без действия). Фикс: редирект в Auth.
6. **[UX]** Переключение в бизнес-режим без busy-индикатора, фейл не доходит до UI (OwnershipAccountSwitch.kt:22, OwnershipAccess.kt:41-51). Фикс: busy в строке + снекбар.
7. **[BUG]** Диалог публикации: «Отмена» мертва во время сохранения (гвард busy глотает CancelConfirmation — BusinessOwnershipScreen.kt:97). Фикс: пропускать Cancel мимо гарда.
8. **[UX]** Префетч деталей карточки обжарщика фейлится навсегда и молча (RoasterListViewModel.kt:110-115) — карточка без описания/фото без retry. Фикс: per-item failed + retry.
9. **[BUG]** AppLink `?checkin=1&checkin=1`/`checkin=true` теряет флаг (сравнение `getAll == listOf("1")` — AppLinks.kt:14). Фикс: `firstOrNull() == "1"`.

### 7.9 Шортлист «топ по влиянию» (из раздела 7)

1. Тихий выкид из бизнес-режима на каждый resume при сетевом сбое (7.8.1).
2. Паралич формы чекина при сбое записи драфта + ложный «не подтверждено» (7.1.2).
3. Стирание загруженного профиля при смене visibility-ревизии (7.4.1).
4. Город молча перезаписывается в БД / крэш на сбое хранилища (7.6.1-2).
5. Лайк собственного поста в ленте → отказ сервера (7.3.2).
6. Схлопывание страниц инбокса при авто-«прочитано» (7.8.4).
7. Вечный retry-цикл пагинации заявок (7.7.1).
8. Краш на кнопке «к продавцу» с битым URL (7.8.3).
9. «Повторить» в редакторе подборки стирает правки (7.7.3).
10. 409-скелетон публичной полки при возврате (7.5.2).


---

## 8. Волна 2 (2026-11-11) — реализовано и проверено на эмуляторах

Все позиции раздела 7, кроме отложенных ниже:

- **Shop (7.1):** баннер ошибки голоса над списком (7.1.1), локализация названий
  напитков меню (7.1.3), fallback блока рейтингов без категорийной сводки
  (7.1.4), тихий refresh списка чекинов (7.1.5), шторка фото не закрывается на
  лимите (7.1.8).
- **Discovery (7.2):** pauseBounds на поколениях (7.2.2 гонка), один «Закрыто»
  на карточке (7.2.3), общая нормализация ключей тегов (7.2.4), локализованная
  копия в пикере чекина (7.2.5).
- **Community (7.3):** fallback пустой thumbnail (7.3.1), гвард вьюера при
  исчезновении поста (7.3.4), a11y гостевого тизера (7.3.3), CTA входа в пустой
  ленте (7.3.7). Лайк своих постов (7.3.2) — не баг, задумано.
- **Profile/история (7.4):** сортировка дневника с null-временем в конец
  (7.4.2), parseServerInstant для фото календаря (7.4.3), «0 кофеен» под
  фильтром «Дома» (7.4.5), карточка блокировки на полном списке чекинов
  (7.4.6), inline-ошибка при непустой ленте (7.4.7).
- **Shelf (7.5):** iOS-сканер ловит OOM (7.5.7), bringIntoView по реальной
  высоте строки из geometry (7.5.8), сообщение об ошибке съёмки на шаге камеры
  описано в VERIFICATION_FLOW (полный перенос эффекта — с деловой проверкой).
- **Auth (7.6):** recovery-поля disabled при busy (7.6.5), loading-кнопка
  (7.6.6), 48dp-строки ссылок (7.6.7). Крэш хранилища городов (7.6.2) и
  перезапись города (7.6.1) закрыты волной 1.
- **Collections (7.7):** 409-«Повторить» подтверждает потерю правок (7.7.3).
- **Прочее (7.8):** тогл бизнес-режима с saveState (7.8.2), «Отмена» публикации
  при busy (7.8.7), AppLink duplicate-query (7.8.9).

### Осталось (осознанно отложено)

- 7.2.1 Nearby-таймаут, 7.2.6 гео-тупик, 7.2.7 стирание маркеров при вводе,
  7.2.8 error-состояния карты — нужен продуктовый сценарий геолокации.
- 7.4.1 visibility-refresh профиля — частично (стале-ревизия), полный
  stale-while-revalidate требует ревизии модели.
- 7.4.4 retry редактирования профиля, 7.5.x очередь busy-пачек, offline-экран
  пачки, поиск кофе в полке, N+1 обложек, 7.7.1 пагинация заявок (footer
  добавлен, стейт-модель бездействия — ок), 7.6.3 «Проверить обновления»,
  7.6.4 город-«Не выбран».
- Раздел 4 §4.1 Crossfade на detail-экранах — по одному экрану за PR с
  визуальной проверкой.
- Раздел 5 reduceMotion — распространение нулевых длительностей.

### Верификация волны 2

- Android: `directSandbox` на эмуляторе — установка, запуск, лента, карточки,
  чекин; крашей нет. Pixel 7 — из более ранней волны (AGENTS: устройство
  подключается только по согласованию).
- iOS: симулятор iPhone 17 Pro — сборка зелёная, установка, запуск, гостевой
  онбординг; крашей нет.
