# Проверки изменений

Актуальные правила на 27.11.2026. Это план проверки, не отчёт о выполненных сценариях. Выбирайте релевантный набор под конкретный diff и фиксируйте реальный результат в PR.

## Уровни проверки

| Уровень | Что доказывает | Что остаётся отдельно |
| --- | --- | --- |
| Unit / contract | reducers, guards, validation, HTTP JSON, fake data/DI/database contracts | runtime UI и live backend |
| Android assembly + DEX | компиляция и наличие runtime class definitions; Sandbox isolation | старт и пользовательский сценарий на устройстве |
| Kotlin/Native tests | фактически исполненные shared contracts для iOS | Swift lifecycle и UI |
| Xcode compile/link | совместимость native source/framework/SPM | подпись, установка, permissions, device behavior |
| Превью | компоновка с поддельным состоянием | lifecycle, сеть, gestures и persistence |
| Ручной Sandbox | root/modal/Back/Cancel, fake CRUD, темы и ошибки | OAuth, реальные camera/media/push/analytics и server acceptance |
| Согласованный live/release check | конкретный platform/backend/delivery flow | непроверенные сценарии и остальные каналы |

Не создавайте, не восстанавливайте и не запускайте automated UI, instrumented Compose или screenshot suites и test-only Activity/APK. Чужие существующие тесты не удаляйте. Unit и не-UI contract/DI/database tests остаются допустимыми; перед instrumentation проверьте реальные initialization/analytics effects.

## Перед запуском

Определите поведение, владельцев и слой изменений. Используйте существующие targeted tests; каждый новый Native-класс сверяйте с выборочными CI-фильтрами и XML execution, а не только компиляцией. Gradle запускается последовательно с `--max-workers=6`.

Для agent UI-review по умолчанию выбран `directSandbox` (`com.coffeepeek.sandbox`), если пользователь не выбрал иное. Generic Studio run configuration не фиксирует вариант. До установки проверьте variant/manifest/entry/package, serial/model/lock, installed version и signature. Не меняйте выбранный пользователем вариант молча, не очищайте данные и не обходите downgrade/signature conflict. Подробности — [ANDROID_RUN_MODES.md](../composeApp/ANDROID_RUN_MODES.md).

Реальные account writes, logout/account replacement, favorites и consent требуют авторизованного безопасного сценария. Sandbox не создаёт записи в production. Доставка TestFlight, Apple processing и доступность тестировщикам — отдельные состояния; успешный upload не равен опубликованному App Store release.

## Ручные регрессии по затронутому поведению

- Навигация: tab/reselection, scroll retention, отсутствие старого overlay при Back; route arguments и deep links; native iOS root отдельно от Compose.
- Подборки/избранное: открыть membership → создать → Cancel/Back/свайп; ошибки/409/retry без потери правок; save → Profile/Collections; возврат не восстанавливает одноразовый intent повторно; cache обложки учитывает смену изображения/аккаунта.
- Полка: активные пачки, архив/search/settings sheet, корректная начальная высота sheet, Back, photo/scan errors и восстановление; source contracts — в [Shelf](../feature/shelf/README.md).
- Чекин: create/edit, существующий draft, photo picker/cancel, явный отказ и неопределённый исход, retry и account switch. Fake successful save не подтверждает live moderation.
- Discovery/location: denial → явный retry, permanent denial → Settings → foreground, revoke во время lookup; stale origin/distance очищаются; selected city не подменяется случайной GPS-позицией.
- Community/notifications: paging + action/read не схлопывает загруженные страницы, stale account responses не проходят, invalidation закрывает недоступное фото.
- Общий UI: светлая/тёмная тема, font/window sizes, VoiceOver/TalkBack, Reduce Motion/opaque glass, keyboard/safe area и доступная Cancel в modal/popover.

Полное поведение и узкие тестовые входы описаны в [README фич](../feature/README.md), [native contracts](IOS_NATIVE_ROOT_SESSION_CONTRACTS.md) и [Sandbox coverage](../composeApp/SANDBOX.md).

## Запись результата

Укажите дату, source SHA/build identity, variant, device/OS при использовании, suite/test counts, failures/errors/skips и retries. Разделяйте compiled/executed/cached, preview compiled/rendered, fake/live и unsigned/signed delivery. Сохраняйте первоначальный сбой; прошедший повтор не доказывает устранённую причину. Diagnostics/screenshots остаются вне tracked sources без account identity, credentials и полного logcat.

Чисто документальное изменение проверяется по исходникам, путям и ссылкам. Оно не требует app install и не закрывает device/release gates новой датой документа.
