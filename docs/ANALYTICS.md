# Аналитика CoffeePeek для Android и iOS

Сверено с мобильными исходниками 27.11.2026. Версия схемы — `1`.
Источник wire-контракта — `composeApp/.../analytics/AnalyticsEvent.kt` и schema/privacy tests.
Старый web-to-mobile implementation prompt снят: здесь описана действующая мобильная реализация, не состояние web.

Сведения для релиза и заполнения App Privacy / Data safety:
[ANALYTICS_STORE_RELEASE.md](ANALYTICS_STORE_RELEASE.md).

## SDK и конфигурация

Используется официальный `com.posthog:posthog-kmp:0.5.1`. Его нативные зависимости:
Android SDK `3.59.0`, JVM core `6.32.0`, iOS SDK `3.64.1`. Версия KMP закреплена в
`gradle/libs.versions.toml`; iOS закреплена в сгенерированном SwiftPM-пакете.

Проект — CoffeePeek EU 306763, ingest — `https://eu.i.posthog.com`. В клиенте
используется только project ingestion token из конфигурации сборки.
Management API keys и пользовательские токены в события не передаются.

Токен не хранится в репозитории. Android читает `POSTHOG_PROJECT_TOKEN` из
окружения сборки (приоритет) или игнорируемого `local.properties`. iOS читает
`PostHogProjectToken` из Info.plist, подставленный из `POSTHOG_PROJECT_TOKEN`
в игнорируемом `iosApp/Configuration/Local.xcconfig`. Release CI получает значение
из GitHub Actions secret `POSTHOG_PROJECT_TOKEN`. Без токена сборка работает,
аналитика выключена. Не добавлять токен в отслеживаемые файлы.

* Android: `app=coffee-peek-android`; iOS: `app=coffee-peek-ios`.
* `app_version` содержит версию и номер сборки; `environment` — `development`
  для Debug и `production` для Release. Отдельного staging-варианта в проекте нет.
* Debug выключен по умолчанию. Для локальной Android-проверки передать
  `-PposthogEnableDev=true`. На iOS добавить `POSTHOG_ENABLE_DEV = YES` в
  игнорируемый `iosApp/Configuration/Local.xcconfig`. В xtool-лаунчере аналогичный
  явный переключатель — plist key `PostHogEnableDev`.
* Даже при включённом Debug-переключателе требуется загрузить включённую настройку аккаунта с сервера.

Документация и исходники, по которым сверялся API:
[KMP](https://posthog.com/docs/libraries/kmp),
[SDK 0.5.1](https://github.com/PostHog/posthog-kmp/tree/0.5.1),
[Android](https://posthog.com/docs/libraries/android),
[iOS](https://posthog.com/docs/libraries/ios).

## Владение кодом и зависимости

Настройка аккаунта принадлежит `feature/settings/domain` и `feature/settings/data`; DTO, HTTP и локальные неподтверждённые изменения скрыты в data. Инфраструктура SDK пока остаётся пакетом
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
процесса чекина между формами — в `feature/home-check-in/api`. При недоступной аналитике
действия с избранным продолжают выполняться.

`core/network` предоставляет независимый `RequestObserver`; корень приложения
подключает его к аналитике. Сетевой модуль не зависит от приложения или PostHog.
DTO, URL и тела запросов через observer не передаются. Domain не зависит от SDK,
Ktor, Compose или Koin. Признаки стадии загрузки фотографии относятся к контракту
ошибки сохранения, чтобы UI различал upload и save без доступа к data.

## Согласие, identity и очередь

С 13.11.2026 настройка хранится в аккаунте на backend. Начальное значение — `true`
для новых и существующих аккаунтов по решению владельца продукта. Это значение
по умолчанию, а не запись о явно данном согласии; серверный `updatedAtUtc` остаётся
`null`, пока пользователь или перенос старого выбора не сохранит решение.

Email-регистрация передаёт галочку в `analyticsEnabled`. Google/Apple могут
инициализировать ещё не выбранную настройку; существующий явный выбор сохраняется.
Переключатель доступен после входа: Настройки → Политика использования.
Отключение немедленно останавливает отправку на этом устройстве и сохраняется через
`PUT /api/Users/me/analytics-preference`. `accountId` в запросе проверяется сервером
по текущей авторизации; адрес не допускает изменение другого аккаунта.

После входа и возвращения в foreground приложение читает настройку с сервера.
При отсутствии ответа, ошибке, неверном accountId или неподтверждённой записи сбор
выключен. Между устройствами обновление применяется при следующем восстановлении
или foreground; мгновенной рассылки изменений нет. В гостевом режиме SDK выключен.
Debug/Sandbox guards, схема событий и запрет на replay/auto-capture сохранены.

Старый `coffeepeek.analyticsConsent.v1` переносится один раз только в аккаунт,
восстановленный при запуске, с `onlyIfUnset=true`. Более новый выбор сервера имеет
приоритет. Гостевую запись нельзя достоверно приписать будущему аккаунту. Уже
удалённые при переустановке записи восстановить невозможно. Неотправленный выбор
хранится по accountId в `coffeepeek.analyticsPreference.pending.v1.*` и повторяется
при следующей загрузке, foreground или периодической попытке в активном приложении.
Ошибка показывается на странице настройки. Смена аккаунта не переносит выбор.

Backend и миграция должны быть развёрнуты раньше этого клиента. До появления API
клиент остаётся с выключенной аналитикой; отсутствие API не трактуется как `true`.
Текущий account preference contract: [Settings](../feature/settings/README.md).

После входа SDK получает `identify` только с внутренним backend user ID.
До входа SDK не создаёт гостевую identity и не накапливает события.
Email, имя, JWT и публичный профильный slug не
используются. Восстановление сессии не создаёт `sign_in_completed`. При выходе
или смене уже идентифицированного аккаунта выполняются opt-out, reset, close и
очистка очереди. Настройка нового аккаунта загружается отдельно. Поколения
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
  прежнее предложение хранить offline до 24 часов между запусками
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

Имена shared web/mobile wire-контракта описаны в `AnalyticsEvent` и проверяются закрытой
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
| Избранное | Одна попытка на pending элемент; гостевой intent хранится только в памяти до 10 минут, после входа возобновляется один раз. Success после подтверждённого сервером изменения, failure по безопасной категории, старый account result исключается. Кофейни и обжарщики сохраняются по контракту `/api/v1/favorites`; новые события отмечены `favorite_storage=server`. |
| Чекин | Общий flow между type picker, shop picker и visit/home editor; VM сохраняет tracker при rotation. Create/edit, первые изменения полей, attempt, client/server validation, upload/save/bean-search failure, retry, явная отмена, saved и отдельный created только для create. |
| Лента | Viewed по первой успешно загруженной странице public feed; refresh, load more, retry, open/photo/shop/like/unlike/share, `coffee` — нажатие на пачку домашнего поста открывает кофе в каталоге. В собственном дневнике эти действия не выдаются за использование public feed. |
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
   Для `favorite_changed/failed` необязательное `favorite_storage=server`
   обозначает серверное хранение. Исторические события `favorite_storage=local`
   сохраняют прежний смысл локальной записи и допустимы в схеме.
5. В ответе каталога кофе исторически отсутствовал `totalCount`. Теперь он
   читается, когда предоставлен сервером. При одной странице допустима длина
   полной выдачи. При нескольких страницах без total событие results не
   отправляется с размером первой страницы: started может остаться незавершённым.
   Для полного отчёта backend должен отдавать `totalCount`.
6. Быстрые локальные переключатели кофеен, которые существующий UI применяет к
   уже загруженным страницам, не создают фиктивного нового server total.
7. Не применимо: storefront download buttons; roaster assortment UI; переход
   по автору/feed home detail, если элемент
   не интерактивен в текущем UI. События ради отсутствующих функций не добавляются.
8. Privacy schema и lifecycle покрываются unit/contract tests. Физическая очередь
   нативного SDK, реальный HTTP payload, отзыв, rotation и возвращение из photo picker
   требуют отдельного согласованного platform check. Старые device results не являются
   свежей приёмкой текущего develop.
9. По продуктовому решению мобильная аналитика теперь собирается только после
   входа. Гостевые exposure/attempt/auth-gate не попадают в PostHog. Для email
   регистрация сама не создаёт сессию: при согласии успешная регистрация хранит
   в памяти одно намерение на час. `sign_up_completed` отправляется один раз при
   последующем password-входе с тем же email и backend user ID. Email используется
   только для сопоставления в памяти, не сохраняется и не передаётся в свойствах.
   Другой аккаунт, социальный вход, отзыв согласия или перезапуск удаляют намерение.
   Существующие аккаунты без согласия могут включить сбор на странице
   Настройки → Политика использования.

## Проверки и источники

Схема событий/enum/required props — `AnalyticsEvent.kt`; privacy allowlist —
`AnalyticsPrivacy.kt`; очередь/consent/identity — `AnalyticsService.kt` и
`AnalyticsSessionObservation.kt`; native delivery — `PostHogTransport.kt` и
platform bootstrap; visibility/correlation — `AnalyticsVisibility.kt`,
`FeatureExposureLedger.kt`, `AnalyticsTrackers.kt`. HTTP observation принадлежит
`core/network`, не удалённому `modules/network`.

Проверяйте соответствующие app `commonTest` и platform contract tests, Settings
account preference tests и feature analytics ports. Обязательны отсутствие guest
setup, запрет raw text/coordinates/headers, revoke/account generations, API
refresh+retry без дублей и screen/exposure ownership на активных native routes.
Новые тесты должны реально исполняться в выборочных Native CI filters.

Ручная приёмка SDK payload, облачного хранения/GeoIP, offline/foreground/revoke
и store declarations остаётся отдельной проверкой на согласованном окружении.
Не отправляйте тестовые события в production ради unit/UI smoke test. Сборка и
исторический прогон не подтверждают фактическую настройку облачного проекта.

iOS linkage подключается существующим KotlinMultiplatformLinkedPackage.
После изменения SDK сверяйте сгенерированное package, SwiftPM resolution и
privacy resources через [iOS setup](IOS_SETUP.md); второй SDK в Swift не создаётся.
Для релиза — [сведения для сторов](ANALYTICS_STORE_RELEASE.md).
