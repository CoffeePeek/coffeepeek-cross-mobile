# Подборки

Сверено с `develop` `8cdc15f9` — 2026-11-27.

## Владение и представление

`api` содержит entry points, цели выбора и платформенные picker/share contracts;
`domain` — metadata, порядок, правила и repository ports; `data` — HTTP, revisions,
cover upload/read и account-scoped caches; `impl` — MVI, Android Compose и нативные
presentation sessions. Навигация, выбор локального изображения и system share
остаются в application composition. Feature не импортирует чужие data/impl.

Android использует общий UI; iOS — SwiftUI library/workspace/poster через те же
Kotlin-модели. Активны library, editor, membership picker с quick-create,
public saved list и read-only старые snapshot links. Сериализуемый старый Composer
сохранён для совместимости маршрутов; на iOS он открывает native library.

В профиле карточки квадратные, в две колонки, без повторного заголовка «Подборки».
Один плавающий glass-контрол над нижней навигацией открывает полный список.
Переключатель показа подборки и отдельная шестерёнка в профиле удалены;
сохранённый `ProfilePreference` API не означает наличие такой кнопки в UI.
Public featured projection остаётся отдельным контрактом и не раскрывает приватные списки.

## Поведение и приватность

| Действие | Контракт |
| --- | --- |
| Первое owner-чтение | Обеспечить каноническую default-подборку. Совместимость по точному названию используется при HTTP 404 нового endpoint. |
| Переименовать или опубликовать default | Сохранить ID, marker и содержимое. Не создавать ещё одну приватную копию. |
| Создать/изменить подборку | Сохранить metadata/cover; metadata-only save не заменяет membership. Новая подборка начинается без мест. |
| Нажать сердце кофейни/обжарщика | Сразу открыть picker для любого состояния сердца; загрузка происходит внутри. До подтверждения membership не меняется. |
| Done без изменения выбора | Закрыть без PUT, дополнительных GET и mutation analytics. |
| Подтвердить изменение | Применить delta к актуальному membership; сохранить посторонние изменения другого устройства. |
| Убрать одно из нескольких memberships | Сердце остаётся выбранным, пока эффективное серверное избранное содержит место. |
| Отменить picker | Не писать memberships и не менять сердце. |
| Отменить quick-create | Вернуться в тот же picker с запросом и черновиком выбора; отмена доступна и при ошибке создания. Уже успешно созданный список не удаляется автоматически. |
| Сохранить обычный editor | После успешных необходимых writes перейти в Profile → Collections один раз. Ошибка, отмена и устаревший account/root lifetime не навигируют. Quick-create этого события не отправляет. |

Серверное избранное — union активных owned memberships и совместимых legacy
favorites. Drinks остаются вне этого shop/roaster контракта. Публичная default
никогда не заполняется приватными legacy favorites автоматически. Чтение,
metadata change или no-op не являются разрешением очистить legacy favorite.
Logout/cache clear также не удаляют серверные записи.

Успешный membership PUT может вернуть `X-CoffeePeek-Is-Favorite` и
`X-CoffeePeek-Membership-Changed`. Они образуют command receipt для немедленного
обновления сердца. Отсутствующее/невалидное значение — unknown; пустой membership
не доказывает false для старого сервера. Analytics принадлежит app adapter,
учитывает consent/account и подтверждённую реальную мутацию, не открытие/отмену.

Owner cache metadata/membership живёт в памяти 24 часа с независимым возрастом
целей; explicit refresh обходит его. `CollectionImageMemoryCache` повторно
использует cover bytes до 10 минут, максимум 24 entries/8 MiB. Ключи включают
состояние обложки, а opaque media scope отсекает прежнюю login lifetime.
Смена аккаунта очищает retained bytes; поздний ответ не заполняет новый scope.
Это не disk cache и не обещание сохранности после завершения процесса.

Приватные cover reads идут через authenticated repository endpoint, без bearer
в URL и без anonymous fallback. Public/featured content не загружает owner bytes.
Upload reservations используют `photoId/uploadUrl/storageKey`, DTO attached cover —
`id/url`; transport строит delivery URL из проверенных идентификаторов.

## Экспорт и проверка

Poster preview и PNG используют один layout: shops перед roasters со стабильным
порядком, компактные logos, актуальные доступные card values и Chat/Story pagination.
QR и печатный адрес сайта из poster удалены; native share сохраняет ссылку.
Отзыв ссылки не может отозвать уже скачанные изображения.

Источники: [API](api/src/), [data/cache](data/src/commonMain/),
[общая презентация и тесты](impl/src/),
[корневая навигация](../../composeApp/src/commonMain/kotlin/com/coffeepeek/admin/ui/),
[нативные screens](../../iosXtool/CoffeePeek/Sources/CoffeePeek/).

Unit/contracts покрывают default/privacy, delta/receipt, 409/retry, cover lifetimes,
quick-create cancel, editor completion и export order/bounds. Native selectors
заданы в [ios-verify.yml](../../.github/workflows/ios-verify.yml), включая
`CollectionProfileReturnTest`. Для изменений нужны Android assembly/DEX и
затронутые suites; Native execution и Swift build подтверждаются macOS CI.
Совместимость live backend не следует из Sandbox: default uniqueness и atomic
delta/receipt зависят от серверного контракта.

Ручные gates остаются для safe-account create/edit/delete, Photos permission/cancel,
private/public media, конфликтов, сохранения scroll, Back/swipe во время работы,
двух тем/языков, больших шрифтов и всех export/share pages. При обновлении этой
документации они заново не выполнялись; прежние журналы находятся в Git history.
