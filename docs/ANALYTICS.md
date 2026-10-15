# Аналитика CoffeePeek для Android и iOS

Дата: 15.10.2026. Базовый контракт: [POSTHOG-MOBILE-IMPLEMENTATION.md](POSTHOG-MOBILE-IMPLEMENTATION.md).
Версия схемы — `1`. Здесь описана именно мобильная реализация; результаты проверок web из исходного документа к ней не относятся.

Сведения для релиза и заполнения App Privacy / Data safety:
[ANALYTICS_STORE_RELEASE.md](ANALYTICS_STORE_RELEASE.md).

## SDK и конфигурация

Используется официальный `com.posthog:posthog-kmp:0.5.1`. Его нативные зависимости:
Android SDK `3.59.0`, JVM core `6.32.0`, iOS SDK `3.64.1`. Версия KMP закреплена в
`gradle/libs.versions.toml`; iOS закреплена в сгенерированном SwiftPM-пакете.

Проект — CoffeePeek EU 306763, ingest — `https://eu.i.posthog.com`. В клиенте
используется только публичный project token из предоставленного контракта.
Management API keys и пользовательские токены в события не передаются.

* Android: `app=coffee-peek-android`; iOS: `app=coffee-peek-ios`.
* `app_version` содержит версию и номер сборки; `environment` — `development`
  для Debug и `production` для Release. Отдельного staging-варианта в проекте нет.
* Debug выключен по умолчанию. Для локальной Android-проверки передать
  `-PposthogEnableDev=true`. На iOS добавить `POSTHOG_ENABLE_DEV = YES` в
  игнорируемый `iosApp/Configuration/Local.xcconfig`. В xtool-лаунчере аналогичный
  явный переключатель — plist key `PostHogEnableDev`.
* Даже при включённом Debug-переключателе необходимо отдельное согласие пользователя.

Документация и исходники, по которым сверялся API:
[KMP](https://posthog.com/docs/libraries/kmp),
[SDK 0.5.1](https://github.com/PostHog/posthog-kmp/tree/0.5.1),
[Android](https://posthog.com/docs/libraries/android),
[iOS](https://posthog.com/docs/libraries/ios).

## Владение кодом и зависимости

Новые Gradle-модули не создавались. Инфраструктура аналитики пока остаётся пакетом
в существующем `composeApp`: общая схема, согласие, identity, безопасный transport,
корреляция и Compose lifecycle. Только transport и его платформенный bootstrap обращаются к SDK.
Платформенные реализации создают SDK context и очищают очередь своего проекта.
На iOS bootstrap настраивает тот же `PostHogSDK.shared`, к которому обращается
официальный KMP wrapper: `surveys=false`, `enableSwizzling=false`, отключённые
auto-events/replay/error tracking и тот же beforeSend. Это необходимое отличие:
native 3.64.1 включает surveys по умолчанию, а KMP 0.5.1 не предоставляет этот
параметр. Второй экземпляр SDK не создаётся; capture/screen/identify/reset
продолжают использовать официальный KMP API.

Точки событий остаются в соответствующих feature UI/ViewModel. Избранное
публикует небольшой entry point в `feature/favorites/api`, а переход общего
процесса чекина между формами — в `feature/checkin/api`. При недоступной аналитике
действия с избранным продолжают выполняться.

`modules/network` предоставляет независимый `RequestObserver`; корень приложения
подключает его к аналитике. Сетевой модуль не зависит от приложения или PostHog.
DTO, URL и тела запросов через observer не передаются. Domain не зависит от SDK,
Ktor, Compose или Koin. Признаки стадии загрузки фотографии относятся к контракту
ошибки сохранения, чтобы UI различал upload и save без доступа к data.

## Согласие, identity и очередь

На регистрации есть отдельная необязательная галочка, выключенная по умолчанию.
Она не связана с принятием условий и не блокирует создание аккаунта. Один выбор
сохраняется между шагами и применяется при отправке корректной формы, а для
Google/Apple — до запроса, который может опубликовать сессию. Даже при неудачном
сетевом запросе это остаётся явно выбранной локальной настройкой; без входа SDK
не запускается. Всплывающего запроса при первом запуске или после входа нет.
Изменить решение можно переключателем «Аналитика использования» в настройках
после входа; гостям этот переключатель не показывается. Отдельный ключ хранения:
`coffeepeek.analyticsConsent.v1`; согласие на другие сервисы не используется.

SDK не запускается без явно принятого согласия и активной сессии с backend user ID.
В гостевом режиме сбор выключен, в том числе при ранее сохранённом согласии.
При отказе нет
setup, capture или накопления событий. Сначала сохраняется принятое решение;
ошибка хранения выключает сбор. Смена решения имеет собственную revision,
поэтому поздняя операция хранения не активирует более новое отклонённое состояние.

После входа SDK получает `identify` только с внутренним backend user ID.
До входа SDK не создаёт гостевую identity и не накапливает события.
Email, имя, JWT и публичный профильный slug не
используются. Восстановление сессии не создаёт `sign_in_completed`. При выходе
или смене уже идентифицированного аккаунта выполняются opt-out, reset, close и
очистка очереди. Согласие хранится отдельно и применяется снова. Поколения
identity и SDK исключают поздние результаты и callback старого экземпляра SDK.

При отзыве или уходе в фон delivery останавливается сразу; очистка выполняется
последовательно в worker. Очередь старого аккаунта не переносится новому.
Alias не используется. После выхода SDK очищается и не перезапускается для гостя.

Выбрана консервативная **очередь только для текущего foreground-сеанса**:

* До готовности SDK — не более 50 уже разрешённых событий в памяти.
* Временный offline во время работы приложения обслуживает единственная очередь SDK:
  максимум 1000 записей, batch до 50, ограничение свойств одной записи 8 KiB.
* Очередь удаляется при background, отзыве, смене аккаунта и перед повторным setup.
  После завершения процесса события прошлого запуска не воспроизводятся.
* Во время непрерывной foreground-работы SDK перезапускается с очисткой очереди
  примерно раз в час. Отдельной прикладной HTTP-очереди и повторного бизнес-действия нет.
* Эта политика сознательно теряет недоставленные события при background/restart;
  предложенное в исходном документе хранение offline до 24 часов между запусками
  здесь не используется. Байтовый предел относится к свойствам, без служебного
  формата файлов SDK.

Replay, autocapture, автоматические screen/lifecycle/deep-link события,
error tracking и диагностические логи выключены. Flags не запрашиваются кодом
приложения и не оцениваются. KMP wrapper не предоставляет переключатель native
remote config: после разрешённого setup возможны служебные config/flags-запросы
самого нативного SDK, включая обновление при identify/reset. До согласия setup
отсутствует. Это ограничение wrapper, а не гарантия отсутствия всех служебных
запросов после согласия.

## События и реальные точки

Все 37 имён из web-контракта описаны в `AnalyticsEvent` и проверяются закрытой
схемой: обязательные/необязательные ключи, типы, enum, статусы, категории полей
и фильтров. Произвольные имена и свойства отклоняются. Общий контекст добавляет
сервис. `beforeSend` дополнительно удаляет URL, device name, person properties
и неожиданные автоматические события SDK.

| Группа | Реализация |
|---|---|
| Экраны | Один ручной `$screen` со стабильным `page_type`: Android Navigation lifecycle; iOS `viewDidAppear` и переключения discovery/map/roasters. Аргументы маршрута и deep links не передаются. |
| Показы | 10% фактической clipped area / 500 мс, foreground, RESUMED lifecycle, видимый native host, отсутствие перекрывающего sheet/photo viewer. Дедупликация по identity/consent и посещению. Search, filters, каталоги, карта, favorites, check-in, разделы карточек, feed и типы записей. |
| Авторизация | Password, Google и Apple после подтверждённого входа и с согласием. Гостевые auth gates и события регистрации до входа не собираются. |
| Каталоги | Начальная выдача и смена критериев, debounce, один started и settled result, отдельный search ID, stale/cancelled и pagination исключаются. Кофейни используют backend total, обжарщики — весь локально отфильтрованный список. |
| Фильтры | Открытие панели, apply/reset, изменённые имена ключей. Значения, текст и координаты остаются в памяти ViewModel. |
| Карта | Expand/collapse, загрузка/ошибка области и nearby, marker/search selection, zones, кнопки zoom, результат явного native location callback. Центр карты не считается геопозицией пользователя. |
| Карточки | catalog item click → detail после успешной загрузки и реального показа; source/search ID для выдачи и map, related для связанных сущностей. |
| Кофейня | Фото, меню, часы, выбор обжарщика, directions, share, phone copy/call, email, website, Instagram, edit/report/check-in. |
| Кофе/обжарщик | Фото, описание, переход к обжарщику/похожему кофе/кофейне, share и существующие внешние контакты. |
| Избранное | Одна попытка на pending элемент; гостевой intent хранится только в памяти до 10 минут, после входа возобновляется один раз. Success после результата repository, failure по безопасной категории, старый account result исключается. Оба существующих repository избранного сохраняют данные локально; success означает завершённую локальную запись, что явно отмечено `favorite_storage=local`. Серверное избранное этой задачей не добавляется. |
| Чекин | Общий flow между type picker, shop picker и visit/home editor; VM сохраняет tracker при rotation. Create/edit, первые изменения полей, attempt, client/server validation, upload/save/bean-search failure, retry, явная отмена, saved и отдельный created только для create. |
| Лента | Viewed по первой успешно загруженной странице public feed; refresh, load more, retry, photo/shop/like/unlike. В собственном дневнике эти действия не выдаются за использование public feed. |
| API | Один результат всей request pipeline, включая 401 → refresh → повтор; безопасная категория, HTTP method/status и monotonic latency. Refresh и внешние storage uploads не создают самостоятельные результаты; cancellation и старый аккаунт исключаются. |

Длительность flow считается с начала процесса; API latency — с начала запроса.
Содержимое форм, оценки, даты, рецепты, имя/ID сущности, фото, имена файлов,
сырые exceptions, response body и headers не отправляются.

## Именованные мобильные расширения и ограничения

1. `sign_in_completed.method=apple`: реальная существующая Apple-авторизация.
2. `shop_action_clicked.action=phone_call`: открытие `tel:`. Это намерение начать
   звонок, не подтверждение разговора; `phone_copy` сохраняет прежний смысл.
3. `check_in_saved/created.visibility=unknown`: только когда backend не прислал
   видимость домашнего чекина. Не подменяется выбранным draft значением. Для
   известных значений сохраняются `Private` / `Public`.
4. `auth_gate_shown.feature=feed_like` и `report_issue`: причины соответствующих
   мобильных барьеров, без переиспользования несвязанного feature.
   Для `favorite_changed/failed` добавлено необязательное `favorite_storage=local`:
   существующие избранные кофейни и обжарщики хранятся локально. В совместных
   отчётах это следует отличать от подтверждённого сервером web-избранного.
5. В ответе каталога кофе исторически отсутствовал `totalCount`. Теперь он
   читается, когда предоставлен сервером. При одной странице допустима длина
   полной выдачи. При нескольких страницах без total событие results не
   отправляется с размером первой страницы: started может остаться незавершённым.
   Для полного отчёта backend должен отдавать `totalCount`.
6. Быстрые локальные переключатели кофеен, которые существующий UI применяет к
   уже загруженным страницам, не создают фиктивного нового server total.
7. Не применимо: storefront download buttons; roaster assortment UI; переход
   по автору/feed home detail и отдельное действие drink-photo, если элемент
   не интерактивен в текущем UI. События ради отсутствующих функций не добавляются.
8. На Android проверены реальные JSON-записи нативной очереди SDK, сохранение
   boolean-типов, удаление запрещённых свойств и физическая очистка при отзыве.
   Проверка выполняется без сети. Отправка HTTP и PostHog Activity, iOS runtime,
   rotation и возвращение из photo picker на устройстве остаются непроверенными.
   Unit-тесты дополнительно проверяют application payload и lifecycle state.
9. По продуктовому решению мобильная аналитика теперь собирается только после
   входа. Гостевые exposure/attempt/auth-gate и password `sign_up_completed`
   до первого входа не попадают в PostHog и не воспроизводятся позже. Для email
   регистрация сама не создаёт сессию: согласие начнёт действовать при последующем
   входе. Существующие аккаунты без согласия могут включить сбор в настройках.

## Проверки

Исходная интеграция (`d2492e9`):

| Проверка | Результат |
|---|---|
| `:composeApp:testPlayDebugUnitTest` | 163 теста, 0 ошибок/пропусков |
| `:modules:network:testDebugUnitTest` | 54 теста, 0 ошибок/пропусков; в том числе один результат для 401 → refresh → retry |
| `:modules:data:testDebugUnitTest` | 25 тестов, 0 ошибок/пропусков |
| `:features:home-checkin:testDebugUnitTest` | 16 тестов, 0 ошибок/пропусков |
| `:composeApp:assembleDirectDebug`, `:composeApp:assemblePlayDebug` | Обе APK собраны |
| `:composeApp:lintPlayDebug` | 0 ошибок, 65 предупреждений в текущем checkout |
| `:composeApp:compileKotlinIosArm64`, `:composeApp:compileKotlinIosSimulatorArm64` | Kotlin/Native KLIB собраны; это не полная Xcode-сборка приложения |
| `:composeApp:assemblePlayDebugAndroidTest` | Тестовый APK собран |
| `PostHogPayloadTest` на отдельном Pixel 8 AVD | 1 тест прошёл: нативная offline-очередь SDK и её очистка |
| `HomeCheckInFormInteractionTest` на Pixel 8 AVD | 2 UI-теста прошли после выдачи геолокации тестовому AVD |

Итого 258 unit-тестов. Gradle запускался с `--no-daemon --max-workers=2`.
Для device-проверок использовался read-only AVD без сохранения snapshot, с
выключенными Wi-Fi/mobile data и airplane mode. Тесты запускались через `adb -s
<serial> shell am instrument -w -r -e class <test-class>
com.coffeepeek.test/androidx.test.runner.AndroidJUnitRunner`, с явным serial.
`PostHogPayloadTest` пропускается при доступной сети до setup SDK. Тестовые
события в production-проект не отправлялись; просмотр Activity не проверен.

Перенос согласия на регистрацию: 166 unit-тестов приложения прошли, в том числе
3 новых теста необязательного выбора, его сохранения между шагами и применения
до публикации social-login сессии. Проверены запрет setup/событий гостя даже с
сохранённым согласием, отсутствие воспроизведения гостевых событий после входа
и остановка при logout. Play/Direct Debug APK, тестовый APK, оба iOS KLIB и
`lintPlayDebug` собраны успешно (0 lint errors, 65 warnings). Изменённый offline
device-тест для политики «только после входа» в этом прогоне собран, но на AVD
повторно не запускался; результаты трёх device-тестов выше относятся к исходной
интеграции.

После переноса поверх параллельного `2cbdaab` повторно прошли 182 unit-теста
приложения, Play Debug APK и Kotlin/Native iOS Arm64. Конфликтов при интеграции
не было; изменения фото не входят в коммит переноса согласия.

Для macOS: открыть существующий `iosApp/iosApp.xcodeproj`. Сгенерированный
`KotlinMultiplatformLinkedPackage` уже добавлен к target и xtool launcher;
не запускать второй экземпляр PostHog из Swift. После обновления SDK регенерировать:

```bash
XCODEPROJ_PATH="$PWD/iosApp/iosApp.xcodeproj" ./gradlew :composeApp:integrateLinkagePackage
```

На Windows генерация package проходит, но автоматическое изменение Xcode project
останавливается из-за отсутствующего `/usr/bin/plutil`; ссылка на сгенерированный
пакет добавлена в проект вручную. Native Swift build, SwiftPM resolution и
проверка privacy manifest требуют macOS/Xcode.

Основные файлы: `analytics/AnalyticsService.kt`, `AnalyticsEvent.kt`,
`PostHogTransport.kt`, `AnalyticsInstallation.kt`, `AnalyticsVisibility.kt`,
`AnalyticsTrackers.kt`, платформенные `PostHogPlatform.*.kt`, feature
`FavoriteInteractions` / `TrackedFavoriteInteractions`, `CheckInFlowHandoff`,
затронутые feature screens/ViewModels и `modules/network/.../RequestObservation.kt`.
