# Author blocking

Сверено с исходниками 27.11.2026. `api/domain/data/impl` владеют контрактом блокировки авторов, server state, account invalidation и presentation. Root инжектирует `SessionAuthorBlockingAccount` на основе `SessionReader`, маршруты и платформенные hooks.

## Контракт

- `GET/PUT/DELETE /api/v1/me/blocks`: server-backed список и idempotent mutations по opaque `authorId`; UI не строит идентичность из отображаемого имени.
- `data` скрывает DTO/HTTP, pagination/revision/reconciliation; `domain` задаёт repository contract, `api` — presenter/account/visibility/entry contracts.
- `impl/ui/compose/AuthorBlockingHost.kt` и `BlockedAuthorsScreen.kt` принадлежат фиче. Native iOS использует public presenter/session и собственную presentation; UI не находится в корневом legacy пакете.
- Гостевое действие направляет к входу без автоматической блокировки после авторизации. Собственная запись не предлагает block своего автора. Жалоба остаётся отдельным действием.
- Мутация меняет локальную видимость после подтверждения сервера. Потерянный ответ требует reconciliation перед следующей записью. Ошибка/неполная пагинация не изображается успешно пустым списком.
- Смена аккаунта сбрасывает pending confirmation/visibility; поздние страницы не проходят generation guard. Foreground обновляет персонализированное состояние; photo viewers учитывают invalidation.

Двусторонние ограничения, прямые photo/detail URL, подписки и модерация — серверная ответственность; клиентская фильтрация не заменяет server enforcement. Независимая встречная блокировка не снимается при unblock одной стороны. Сведения о текущем deploy/миграциях проверяются в backend, а не предполагаются из наличия клиента.

## Проверка

`data/src/commonTest/.../AuthorBlockRepositoryTest.kt` и `impl/src/commonTest/.../AuthorBlockingViewModelTest.kt` покрывают account generations, pagination, lost responses, ошибки и подтверждения. Выполняйте релевантные unit/contract suites и build затронутой платформы; новый запуск результатов этим README не заявляется.

Ручная приёмка: чужой пост → block → Cancel/confirm; собственный пост; guest/login; закрытие фото и обновление открытого профиля/ленты; settings → unblock/error/retry; background/account switch. Для iOS проверяются sheet/popover, VoiceOver, длинные имена и safe areas. Реальные записи требуют согласованного тестового аккаунта; автоматические UI suites не запускаются. App Review маршрут и credentials задаются вне репозитория отдельно от технического README.
