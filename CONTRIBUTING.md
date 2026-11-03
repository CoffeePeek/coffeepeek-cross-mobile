# Contributing to CoffeePeek

Спасибо, что хотите помочь с проектом. Это **Kotlin Multiplatform-приложение** на Compose Multiplatform для Android и iOS.

## Быстрый старт

### Требования

| Инструмент | Версия |
|------------|--------|
| JDK | **17** |
| Android Studio | с SDK **37** |
| Xcode | **27** для iOS-сборки |
| Git | актуальная |

> **Важно:** сборка на JDK 24/26 может падать с ошибкой `java.lang.IllegalArgumentException: 26.0.1`. Используйте JDK 17 для Gradle и IDE.

### Клонирование

```bash
git clone <repository-url>
cd CoffeePeek-Android
```

### Локальная конфигурация

1. Скопируйте шаблон секретов:

   ```bash
   cp local.properties.example local.properties
   ```

2. Заполните `local.properties`:
   - `sdk.dir` — путь к Android SDK (Studio часто добавляет сама)
   - `API_BASE_URL` — URL backend (уточните у команды)
   - `GOOGLE_WEB_CLIENT_ID` — опционально, для Google Sign-In

   В шаблоне указан публичный API `https://api.coffeepeek.by/`. Старый
   Railway-адрес больше не обслуживает приложение (`404 Application not found`).
   Для проверки операций записи используйте согласованное QA-окружение и
   тестовый аккаунт. После изменения URL пересоберите и установите APK:
   значение встраивается в Android `BuildConfig`, а не читается на устройстве
   из `local.properties`.

3. Получите у команды:
   - тестовый аккаунт на API
   - ключи, если dev-окружение отличается от дефолтного

**Никогда не коммитьте `local.properties`** — файл в `.gitignore`.

### Сборка

```bash
./gradlew :composeApp:assembleDirectDebug :composeApp:assemblePlayDebug
```

Для локальной установки используйте
`composeApp/build/outputs/apk/direct/debug/composeApp-direct-debug.apk`.
`Direct` поддерживает обновление APK, `Play` — распространение через Google Play.
Debug-сборки автоматически проверяют определения стартовых классов в DEX;
подробнее: [диагностика упаковки APK](build-logic/ANDROID_RUNTIME_PACKAGING.md).

---

## Архитектура

```
composeApp/           запуск, корневой DI (Koin), конфигурация, корневая навигация
core/                 сеть (Ktor, сессия токенов), база данных, медиа, дизайн-система
feature/<имя>/        api, domain, data, impl: экраны, ViewModel, репозитории и строки фичи
```

**Поток:** `Screen` → `ViewModel` → `domain.Repository` → `data` (внутри фичи, поверх `core/network`) → API.

**Ключевые точки входа:**

| Что | Где |
|-----|-----|
| Koin, модули приложения | `composeApp/.../di/KoinApp.kt` |
| Транспорт | `composeApp/.../di/TransportModule.kt` (`core/network`) |
| Навигация | `composeApp/.../ui/Navigator.kt` |
| Base URL | `AppConfig` / `Constants.BASE_URL` |
| Android Application | `FPApplication.kt` |

**Исходники приложения:**

- `composeApp/src/commonMain/` — UI и логика
- `composeApp/src/androidMain/` — MapLibre, Google Auth, manifest, ресурсы
- `composeApp/src/iosMain/` — MapKit, Google Auth bridge, PhotosUI, CoreLocation
- `iosApp/` — SwiftUI entry point, Xcode project и iOS-конфигурация

---

## Как вносить изменения

### Ветки

1. Создайте ветку от актуальной `main` (или `dev` — уточните у мейнтейнера).
2. Именование: `feature/краткое-описание`, `fix/краткое-описание`.

### Стиль кода

- Следуйте существующим паттернам в соседних файлах.
- UI: Compose Material 3, цвета из `CpColor` / `MaterialTheme`, отступы `CpDimens`.
- Строки для UI — в `composeApp/src/commonMain/composeResources/values/strings.xml`.
- Новый API: DTO в `network`, интерфейс в `domain`, реализация в `data`, регистрация в `DataModule` + `KoinApp`.
- Общий код размещайте в `commonMain`; платформенные API — в соответствующих `androidMain`/`iosMain` actual-реализациях.
- Минимальный diff: не рефакторить несвязанный код в том же PR.

### Коммиты

Короткое сообщение на русском или английском, по смыслу изменения:

```
fix: парсинг ошибки при создании чек-ина
feat: публичный чек-ин с обязательным комментарием
```

### Pull Request

В описании PR укажите:

1. **Что сделано** и зачем.
2. **Как проверить** (шаги ручного теста).
3. **Скриншоты** — для UI-изменений.
4. **Известные ограничения** — если есть зависимость от бэкенда.

Перед отправкой:

- [ ] Direct/Play debug-сборки и проверки runtime-классов проходят
- [ ] `xcodebuild` или Run схемы `iosApp` проходит для iOS-изменений
- [ ] Проверен основной сценарий (логин → лента / карта / профиль)
- [ ] Нет секретов и `local.properties` в коммите

---

## Отладка

- В `KoinApp.kt` включён `debug = true` для HTTP — в logcat видны cURL-запросы (`CoffeePeekClient`).
- Известные проблемы и баги из продакшен-логов: [`LOG_ISSUES.md`](LOG_ISSUES.md).

---

## Модули и ответственность

| Задача | Куда класть код |
|--------|-----------------|
| Новый экран и его ViewModel | `feature/<имя>/impl` |
| REST endpoint и маппинг DTO | `feature/<имя>/data` |
| Бизнес-контракт | `feature/<имя>/domain` (порты для других фич — `api`) |
| Строки | `feature/<имя>/impl/src/commonMain/composeResources` (`values` — русский, `values-be` — белорусский; проверка: `python3 tools/l10n/check_parity.py`) |
| Локальная БД | `core/database` |
| Android API (MapLibre, picker) | `composeApp/.../androidMain/` |
| iOS API (MapKit, PhotosUI) | `composeApp/.../iosMain/` |

---

## Вопросы

Если чего-то нет в репозитории (ключи, staging URL, процесс ревью) — спросите мейнтейнера или откройте issue в трекере команды.
