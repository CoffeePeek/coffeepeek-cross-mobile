# Документация CoffeePeek

Указатель актуальных руководств, сверенных с исходниками `develop` 27.11.2026. Код, build-файлы и workflow определяют действующее поведение; документ с новой датой не подтверждает свежую проверку устройства, production deploy или публикацию в магазине.

## Начало и архитектура

- [README](../README.md) — подготовка и основные точки входа.
- [CONTRIBUTING](../CONTRIBUTING.md) и [AGENTS](../AGENTS.md) — правила изменений/проверок.
- [ARCHITECTURE](../ARCHITECTURE.md) и [структура проекта](PROJECT_STRUCTURE.md) — владение и зависимости.
- [План выхода из legacy](../feature/LEGACY_EXIT_PLAN.md) — текущие остаточные границы и условия приёмки.
- [Фичи](../feature/README.md), [Core](../core/README.md), [Application composition](../composeApp/README.md), [Build infrastructure](../build-logic/README.md).

## Запуск, UI и проверка

- [Android run modes](../composeApp/ANDROID_RUN_MODES.md), [Sandbox](../composeApp/SANDBOX.md), [DEX checks](../build-logic/ANDROID_RUNTIME_PACKAGING.md).
- [Проверки изменений](VERIFICATION_FLOW.md) — unit/contract, preview/manual, live/release gates.
- [Design system](../core/design-system/README.md), [platform design](PLATFORM_DESIGN.md), [media](../core/media/README.md).
- [App icon artwork](../design/app-icon/README.md) — исходник, плотности и варианты оформления.
- [iOS setup](IOS_SETUP.md), [xcross](IOS_XCROSS.md), [native navigation](IOS_NATIVE_NAVIGATION.md), [root/session contracts](IOS_NATIVE_ROOT_SESSION_CONTRACTS.md), [native exit](IOS_NATIVE_EXIT_PLAN.md).
- [iOS map](IOS_MAP_PARITY.md), [feed photo memory](IOS_FEED_PHOTO_MEMORY.md), [optional xtool host](../iosXtool/CoffeePeek/README.md).

## Контракты и эксплуатация

- [Публичные адреса](PUBLIC_ADDRESSES.md).
- [Аналитика](ANALYTICS.md) и [сведения для сторов](ANALYTICS_STORE_RELEASE.md).
- [Push configuration](push-notifications.md) и [notification artwork](../design/notifications/README.md).
- [Google Play/release](GOOGLE_PLAY_CD.md), [APK/VPS](ANDROID_APK_CD.md), [Firebase distribution](FIREBASE_CD.md), [GitHub release drafts](GITHUB_RELEASES.md).
- [Project issue workflow](../tools/project-status/README.md).

## Как поддерживать документы

Обновляйте README владельца вместе с изменением контракта, source set, DI entry, конфигурации или workflow. Сначала проверяйте текущий код; не переносите предположения из старого плана. Закрытые одноразовые планы/аудиты заменяйте каноническим руководством, сохраняя текущие правила и реальные открытые gates. История решений и старые результаты запусков доступны в Git history и соответствующих PR.

При удалении Markdown проверяйте входящие ссылки и упоминания из скриптов/конфигурации. Не удаляйте asset attribution, лицензии и provenance по возрасту. Проверки ссылок/путей и source consistency достаточны для чисто документальных правок; они не являются app/device acceptance.
