# Выход из legacy: текущий статус и оставшиеся gates

Сверено 27.11.2026 с `develop` (`8cdc15f9`, исходная база обновления документации). Это рабочий список границ, а не журнал всех промежуточных коммитов. История удалённых планов и результаты прежних запусков остаются в Git history и связанных PR.

## Что подтверждено исходниками

- Каталог `modules/` удалён. `Modules.kt` регистрирует `composeApp`, семь инфраструктурных `core`-владельцев и бизнес-фичи в `feature/`.
- Целевые бизнес-границы — `api/domain/data/impl`; фактический Favorites сохраняет только server-backed `domain/data`, без отдельного экрана/api/impl. Факт наличия этих модулей не доказывает отсутствие всех остаточных app-owned consumers.
- Android выбирает feature entries через application DI. Активный iOS root использует нативные SwiftUI/UIKit-экраны, Kotlin-сессии и общий data/domain; старый Compose root не является целевой оболочкой.
- Избранное хранится на сервере в `feature/favorites/data`. Локальный Room writer и автоматическая загрузка старых строк не восстанавливаются. Logout очищает локальную session/memory, сохраняя серверное избранное.
- Транспорт и refresh coordination находятся в `core/network`, сессия и защищённое хранилище — в `core/session`, Room bootstrap — в `core/database`, media pipeline — в `core/media`.
- Author blocking UI находится в `feature/author-blocking/impl`; root поставляет account/route/platform hooks.
- Root всё ещё содержит адаптеры, preference/session migration glue, infrastructure analytics, route hosts и часть общих platform/UI wrappers. Для каждого нужен разбор реальных consumers; название файла само по себе не является причиной удаления.

## Остаточные границы

| Зона | Текущий владелец / след в исходниках | Условие завершения |
| --- | --- | --- |
| Root presentation и общий UI | `composeApp/.../ui/component`, `ui/screen`, `base/BaseViewModel.kt` | route/runtime hosts остаются root; бизнес-состояние и reusable primitives переходят владельцам после проверки Android и native entry variants |
| Preferences | `composeApp/.../settings`, `theme/ThemeManager.kt`, `locator` | платформенное хранение и feature contracts согласованы без второго источника истины и без сброса настроек |
| Миграция прежних credentials | `composeApp/.../session/RoomLegacyCredentialRows.kt`, `core/session` | сохранить upgrade/read-delete semantics; удаление возможно только после обоснованного окончания поддержки старых данных |
| Analytics infrastructure | `composeApp/.../analytics` | сохранять один SDK, consent/session/visibility gates и feature ports; выносить только при конкретной infra-границе |
| iOS native exit | [IOS_NATIVE_EXIT_PLAN.md](../docs/IOS_NATIVE_EXIT_PLAN.md) | нативные routes, sessions, lifecycle и device gates проверены; отсутствие fallback не подменяется наличием native файла |
| Общий media pipeline | [core/media](../core/media/README.md) | защищённые фото, revoke/account switch, фон/возврат и memory проверены на платформах |

Не удаляйте `RoomLegacyCredentialRows` только из-за слова legacy: это совместимость сохранённых данных, а не старый экран. `UnsupportedShelfScreenRenderer` и другие default/interop paths нужно проверять по production, Sandbox, previews, tests и native callers перед снятием последнего consumer.

## Сохранённые follow-up из прежнего UI-аудита

Это source-identified остатки для проверки/отдельных исправлений, не новые runtime результаты и не утверждение о приоритете:

- `feature/community/impl/.../timeline/CommunityViewModel.kt`: реакция отменяет общий `loadJob`, который используется и для pagination. Проверить лайк во время `loadMore`, отмену/повтор страницы и актуальные счётчики без потери списка.
- `composeApp/.../theme/ThemeManager.kt`: тема публикуется до фоновой записи, у которой нет явного Result/error handling. Ошибка persistence остаётся follow-up; City/Language уже имеют другой проверяемый путь. См. [Settings](settings/README.md).
- `feature/collections/impl/.../CollectionsEntryImpl.kt` и `ui/compose/CollectionsScreen.kt`: прежний public Compose path после detail-read может делегировать `savedPublic` и читать коллекцию повторно. Сначала проверить достижимость конкретного caller/native/Compose варианта, затем устранять дублирование; не объявлять оба platform roots сломанными по одному старому аудиту.

Сохранённые решения паритета: гостевой history может требовать вход; `step_viewed(bean)` означает открытие picker, а `retry_clicked(bean)` — настоящий retry; report failure использует безопасный фиксированный текст вместо raw server message; у неадресуемого roaster без slug нет отправляемого favorite intent. Изменять эти контракты нужно явно, не восстанавливать старое поведение механически.

## Приоритет и порядок следующего среза

1. От действующего Android/native маршрута проследить entry → composition adapter → feature contract → data owner. Различать full-screen, профильный preview и modal.
2. Инвентаризировать consumers, analytics ownership, consent/session guards и сохранённые данные.
3. Перенести одну ответственность в действующего владельца; не создавать универсальный `core/common/utils` или второй application module.
4. Заменить все consumers этого checkpoint и удалить ставший ненужным fallback в том же срезе.
5. Проверить границы/ресурсы/previews, релевантные unit/contract tests, Android assembly и packaged DEX, а для iOS — Native execution и Xcode compile/link.
6. Отдельно записать ручные проверки и непроверенные live/release gates. Не считать архитектурный выход законченным по одному зелёному build.

Новые product-фичи не должны возвращать бизнес-UI/VM в `composeApp`. Общие правила, размер checkpoint и стек PR — в [AGENTS.md](../AGENTS.md) и [ARCHITECTURE.md](../ARCHITECTURE.md).

## Сохранение паритета

- Личные и публичные profile/history readers не взаимозаменяемы; приватный дневник не является fallback для public feed.
- Данные и ответы предыдущего аккаунта не переходят следующему. Отмена coroutine должна распространяться, Result-ошибки остаются явными.
- Серверное избранное, aliases/canonical public addresses, drafts и Room/Keychain upgrades сохраняют действующий контракт.
- Аналитика сохраняет событие/свойства, exposure owner и correlation, без новой службы SDK и без тестовых событий в production. См. [ANALYTICS.md](../docs/ANALYTICS.md).
- Ресурсы и accessibility-тексты принадлежат владельцу; языки сверяются по фактическим `values`/`values-be` и native strings.

## Совместимость восстановленного Android-стека

Синхронизировано с изменением develop [179fd8aee](https://github.com/CoffeePeek/coffeepeek-cross-mobile/commit/179fd8aeec1efc412d491f4ab1d866b28b631ff3), 27.11.2026. В `Navigator` сохранён сериализуемый `Screen.VisitedPlaces` для задач Android, записанных до переноса истории в профиль. Когда восстановленный entry достигает RESUMED и остаётся текущим, root возвращается к `Main` и запрашивает Profile → Check-ins. Свежая навигация не использует этот маршрут; прежний отдельный бизнес-экран не восстановлен.

`VisitedPlacesRouteTest` сохраняет serial identity и проверяет отсутствие route в fresh root navigation. В upstream-коммите зафиксированы 22 JVM-теста навигации, function limits, Direct Debug/Sandbox assembly с DEX/isolation и ручной Sandbox smoke. Это результаты исходного исправления, не повторный запуск документационного merge. Полное восстановление исходного сохранённого Activity bundle и реальный аккаунт вне Sandbox остаются непроверенными. Новую миграцию стека нельзя обосновывать удалением этого совместимого типа без проверки upgrade consumers.

## Что не подтверждено этим обновлением документации

Ручная проверка устройств, production API, OAuth, пушей, store declarations и доставки не выполняется при редактировании Markdown. Новая дата документа не переаттестовывает старые тесты или релиз. Для каждого следующего изменения фиксируйте SHA/variant/device, executed/compiled/cached tests, исходные сбои и оставшиеся gates согласно [VERIFICATION_FLOW.md](../docs/VERIFICATION_FLOW.md).
