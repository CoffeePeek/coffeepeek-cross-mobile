# Application composition

Сверено 27.11.2026. Модуль владеет startup, root DI/navigation, configuration и адаптерами платформ/межфичевых контрактов. Новый бизнес-UI, ViewModel или HTTP endpoint размещается у владельца в `feature`, reusable infrastructure — в соответствующем `core`.

## Точки входа и границы

- `src/commonMain/kotlin/com/coffeepeek/admin/di/KoinApp.kt` собирает core clients, session и feature factories. Наличие default renderer не является разрешением возрождать удалённый legacy graph.
- `ui/Navigator.kt`, `NavigatorViewModel.kt`, `MainTabReselections` координируют Android root. Удержание bottom tab не показывает старый large-content magnifier; reselection/scroll и обычный click остаются разными событиями.
- `src/iosMain/.../ui/IosNativeRoot.kt` и Swift native root связывают Kotlin state/sessions с нативными routes. См. [native navigation](../docs/IOS_NATIVE_NAVIGATION.md) и [session contracts](../docs/IOS_NATIVE_ROOT_SESSION_CONTRACTS.md).
- `src/androidSandbox` содержит только isolated fake backend и launch fixtures; его проверки не являются live acceptance.

Renderer-адаптеры передают public API фичи, а не её data DTO/UI implementations соседям. Host ViewModel может владеть lifetime presentation session; бизнес-правила и reducers остаются в feature implementation. Остаточные границы перечислены в [LEGACY_EXIT_PLAN.md](../feature/LEGACY_EXIT_PLAN.md).

## Платформенная координация

`location/LocationPermissionRequest.kt` обрабатывает request keys и текущего получателя. Только явный request key может вызвать системный prompt; foreground лишь проверяет реальный доступ. Android различает повторяемый отказ и необходимость Settings, допускает coarse access. iOS слушает изменения authorization/foreground и удаляет observer при закрытии. Проверка позиции выполняется до и после suspend lookup; revoked/unavailable coordinates очищаются, отмена сохраняется. Discovery не удерживает старый Nearby origin после отзыва. Ручные сценарии soft denial → retry, Settings → foreground и revoke остаются device gates.

`settings/AppLanguagePreference.kt` и platform environment применяют выбранный язык; предлагаемые языки сверяются с ресурсами и каталогом Settings. Sandbox locale extra — только fixture, не перевод приложения и не изменение системных настроек.

`session/RoomLegacyCredentialRows.kt` поддерживает чтение старых сохранённых credentials для upgrade. Не удалять данные или migration bridge по возрасту имени. Root связывает `SessionReader`/`SessionStore` с feature ports; refresh принадлежит `core/network` и Auth. Ни logout, ни clear-cache не удаляют серверное избранное.

`analytics` содержит общий privacy/schema/identity/transport lifecycle. Feature events приходят через narrow ports; второй SDK не создаётся. Контракт — [ANALYTICS.md](../docs/ANALYTICS.md).

Shared Compose wrappers и native controls должны сохранять safe areas, keyboard, themes, Reduce Motion и доступность. Канонические компоненты — [design-system](../core/design-system/README.md), image transport/viewer — [media](../core/media/README.md). Одноразовые журналы исправлений шапок/стекла/фотографий заменены этими руководствами.

## Проверка и запуск

- [ANDROID_RUN_MODES.md](ANDROID_RUN_MODES.md): точные variants/packages, manifests, подпись, установка без потери данных.
- [SANDBOX.md](SANDBOX.md): покрытие fake fixtures и ограничения.
- [IOS_SETUP.md](../docs/IOS_SETUP.md): Xcode, конфигурация, подпись.
- [VERIFICATION_FLOW.md](../docs/VERIFICATION_FLOW.md): unit/contract, preview/manual, live/release разделены.

Gradle запускается последовательно с `--max-workers=6`. Чисто документальная правка не требует сборки APK. Проверка UI — colocated previews и ручной Sandbox; не добавлять/запускать автоматические UI/instrumented Compose/screenshot suites. Сборка или версия установленного приложения сама по себе не доказывает конкретный source revision и успешный пользовательский flow.
