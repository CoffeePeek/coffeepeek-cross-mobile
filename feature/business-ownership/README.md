# Представительство и бизнес-режим

Сверено с `develop` `8cdc15f9` — 2026-11-27.

## Владение и entry points

`api` задаёт `BusinessOwnershipEntry`, session/access state и platform slots;
`domain` — claims, owned businesses, draft/rules и repository/media ports;
`data` — owner HTTP, private evidence и DTO; `impl` — `OwnershipAccess`, MVI,
Android Compose и native presentation sessions. Composition собирает сессию,
Media uploader, permissions, navigation и public-profile invalidation.
Shop/Roaster используют только app-supplied API, а не ownership internals.

Android использует feature UI. На iOS business tab, overview/history, claim,
editor, owner toolbar и QR используют SwiftUI/UIKit через общие Kotlin-сессии.
Нативный toolbar возвращает feature-owned claim controller напрямую; старый
Compose claim host не является его fallback. Другая форма — обычный полный
ownership destination — сохраняет собственный store и lifecycle.

## Доступ, mode switch и профиль

- Обычный авторизованный User может отправить claim. Approval/rejection выполняет
  веб-администрация. Старый Approved claim не доказывает текущее право доступа.
- Non-owner кофейни может предложить изменения через модерацию или заявить
  представительство загруженного public slug. Non-owner обжарщика получает
  representative action. Confirmed owner открывает direct editor.
- Owner writes используют UUID endpoint и `If-Match`. Public slug из owner-list
  сопоставляется editor UUID; raw slug нельзя отправлять как UUID.
- Business mode доступен при текущих owned businesses. Он показывает две вкладки:
  Business и Settings; возврат в personal mode сохраняет доступы аккаунта.
- `OwnershipAccess` сериализует refresh/switch одним mutex. Switch захватывает
  requesting account, reset epoch и целевой mode до ожидания lock; очередной
  callback прежнего аккаунта не переключает его преемника. Duplicate switches
  подавляются. Cancellation не превращается в обычную ошибку.
- Logout/account replacement/reset отзывают локальные grants/mode. Успешный ответ
  без owned businesses выключает mode. Временный сбой refresh сохраняет прежнее
  известное состояние до следующей проверки; он не даёт новых серверных прав.
  Port с identity strings не доказывает защиту от ненаблюдённого same-ID ABA.
- Business tab — список shops/roasters с pull-to-refresh; пустая roaster section
  скрыта. Отсутствующие rating/open/catalog значения не придумываются.
- Profile editor сохраняет untouched city identity, schedules и catalog associations.
  После успешной записи `ProfileSaved` инвалидирует public details независимо
  от кратковременного saved message.

## Claim, media и QR

Claim sheet содержит contact, explanation и до трёх private evidence photos
по 5 MiB. Evidence upload/read использует отдельный owner-authorized путь,
а не public gallery. Native и Compose сохраняют dirty/discard/busy правила,
guest sign-in, account-version guards и cancellation. Отменённый или старый
photo callback не изменяет текущую заявку. Contacts/evidence/bytes не становятся
analytics properties.

Для owned shop с public slug доступен check-in QR: PNG 1024 px с белой рамкой
и существующая system share adaptation. Link не содержит account identifier.
Application navigation сохраняет выбранную кофейню через auth и открывает form
один раз; Cancel очищает continuation. QR не отправляет чекин автоматически.

## Источники и проверка

[API](api/src/), [rules/repository ports](domain/src/), [transport](data/src/),
[presentation и unit contracts](impl/src/),
[нативные screens](../../iosXtool/CoffeePeek/Sources/CoffeePeek/).

Регрессии покрывают access vs claim history, account replacement, serialized
mode switches, UUID/slug mapping, revisions, private media, partial saves,
ProfileSaved и QR links. Изменения проверяются relevant unit/contracts,
Android assembly/packaged DEX и macOS native build/tests; CI selection находится
в [ios-verify.yml](../../.github/workflows/ios-verify.yml).

Ручные gates: mode/back/app links, native dirty/busy swipe dismissal,
Photos return/cancel, QR scanning/share, themes/large text/VoiceOver и safe-account
claim/approval/direct-save/conflict acceptance. Sandbox не подтверждает private
media authorization или production migrations. Эти gates заново не выполнялись
при обновлении документации; завершённые rollout records доступны в Git history.
