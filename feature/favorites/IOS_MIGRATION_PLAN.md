# Favorites: подключение iOS

## Срез 2026-10-25

Продолжение общей iOS-миграции в PR #195 → develop поверх b47a9417.
Пользователь поручил закончить оставшиеся пункты после core SessionReader.
Исторический Android-first checkpoint в README не описывает это подключение.

### Точки входа и сохранение поведения

- Navigator.Screen.Favorites открывается из Profile; оба navigation host
  вызывают FavoritesDestination. На iOS он пока вызывает старый FavoritesScreen,
  на Android — FavoritesEntry. Корневые UIKit stacks/back/screen exposure
  остаются у приложения; дополнительная навигационная система не создаётся.
- Цель — существующий feature/favorites/impl экран на обеих платформах:
  shops, roasters и drinks, ошибки/повтор, сохранённые public slugs, удаление,
  существующая геолокация/формат расстояния, native header и safe area.
- ServerFavoritesRepository остаётся единственным серверным источником.
  Новых endpoints, local writer/схемы или импорта старых строк нет.
  Logout/clearCache очищает только память, не server membership.
- Profile favorites preview — отдельный entry того же feature-модуля с
  lifecycle-owned ViewModel и теми же server ports; старый application singleton
  больше не используется активным OwnProfile renderer. Public-profile cards
  остаются app slots, со своей моделью публичного профиля; private reader к ним
  не подключается.

### Границы

- Platform-neutral server module, roaster/drink adapters и analytics exposure
  перемещаются из app/androidMain в app/commonMain без нового Gradle-модуля.
  Архитектура остаётся api/domain/data/impl; app адаптирует legacy server models
  к узким domain ports. Android-only local storage compatibility graph остаётся
  на месте и не включается в активный iOS graph.
- App commonMain использует favorites api/impl. Domain не получает UI/Koin/Ktor.
  Feature UI не импортирует app/data/legacy models; DI остаётся в приложении.
- Header/scaffold/insets используют core/design-system и установленные UIKit
  hooks. Defaults composable interface удаляются для корректной Native-линковки.
- Общая геометрия карусели перенесена в CoffeePeekStackedCardCarousel: полные
  карточки, пары высотой с соседнюю полную карточку, затем полные карточки.
  Layout не знает моделей избранного и используется own/public profile.
  Старый app carousel стал compatibility wrapper. Feature preview использует
  свои domain snapshots/cards, error/retry/dismiss actions и парные previews.
  Missing/loading/failed photo использует имеющийся CoffeePeekImagePlaceholder.

### Аналитика

Route exposure остаётся в Android root / NativeScreenController; favorites_opened
остаётся у входа из Profile. Exposure hearts использует прежний consent-aware
analyticsExposure, placement по текущей странице. Shop/roaster mutations идут
через trackFavoriteMutation (attempt/result/account operation guards).
Drink removal в прежней схеме не имел отдельного business event; нового не
придумываем. Реальные события в тестах не отправляются.

### План проверок

1. Unit/contract: feature favorites domain/impl, app server adapters, ownership,
   cancellation, удаление и сохранённая аналитика. Новый общий production DI
   graph проверить с in-memory ports: создание entry без чтений/записей,
   все три kind и очистка памяти без server deletion.
2. Android Direct Debug/Sandbox с packaged DEX/isolation checks; iOS arm64
   compile, simulator framework link и common test compile. Gradle последовательно,
   --max-workers=6; автоматизированных UI/instrumentation/screenshot tests нет.
3. Colocated light/dark previews — UI contract, без production DI/network.
   Runtime/manual iOS-приёмка: header/native back, safe area, large text/VoiceOver,
   guest/restore/logout/replacement/token renewal, ошибки/повтор, mixed/empty
   state и подтверждение удаления на безопасном аккаунте. Эти gates не закрываются
   одной компиляцией или fake-тестами.

## Результат

Общий production graph и маршрут подключены на обеих платформах. Android-only
local compatibility modules не перемещены и не включены в iOS. Fallback screen,
legacy VM и bindings сохранены для старых fallback callers до общей приёмки.

Первый прогон unit tests остановился на inference nullable Triple в новом
graph assertion; исправлен явным типом. После переноса preview найден compile
failure нового Layout: modifier был передан первым positional argument,
который API считает content. Исправлен named modifier. Первоначальные failures
не заменяются последующими успешными прогонами.

Итог 2026-10-25: app subset 74 tests / 17 suites, favorites domain 1 / 1,
impl 19 / 3 — всего 94 tests / 21 suites, 0 failures/errors/skips.
assembleDirectDebug/Sandbox, runtime-class guards и Sandbox isolation прошли.
compileKotlinIosArm64, linkDebugFrameworkIosSimulatorArm64,
compileTestKotlinIosSimulatorArm64 app и favorites impl успешны; Native tests
только скомпилированы. Gradle последовательно с --max-workers=6.

Source: b47a9417 + этот checkpoint. SHA-256 APK:
Debug fef4d4329bc8ff2b6cb042f863f592cfcf1343f3cd79b529e270df0ac9701bcd;
Sandbox 75eaf2c7bcacfe6db207eeb6918108c3752e2de79b6ed6eaf8d0f1a658002dd5.
Приложения не устанавливались/не запускались; телефон, credentials и сервер
не менялись. Ручные gates из плана выше остаются открытыми.

После checkpoint e67692f3 выполнена синхронизация с develop 105b8994 и повторная
интеграционная проверка: 601 selected unit/contract tests без failures/errors/skips,
обе Android assemblies/guards, iOS compile/framework link/common test compile
успешны. Favorites impl входит в этот набор (19 tests); подробные counts,
конфликты и итоговые APK hashes — в общем IOS_MIGRATION_PLAN.
