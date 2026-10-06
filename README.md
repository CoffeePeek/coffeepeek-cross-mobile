# CoffeePeek

Кроссплатформенное приложение экосистемы specialty-кофе для Android и iOS: лента кофеен, карта, профиль, чек-ины и отзывы.

**Стек:** Kotlin · Jetpack Compose (Compose Multiplatform) · Ktor · Koin · Room

---

## Возможности

- Регистрация и вход (email/пароль, Google Sign-In)
- Лента кофеен с пагинацией
- Карта OpenStreetMap на MapLibre
- Карточка кофейни: фото, контакты, расписание, отзывы
- Избранное, чек-ины (публичные и приватные), создание и редактирование отзывов
- Профиль: аватар, статистика, тема оформления
- Добавление кофейни (многошаговая форма с модерацией)
- Community — в разработке (empty state)

---

## Быстрый старт

### Требования

- **JDK 17** (обязательно; JDK 24/26 ломают сборку Gradle)
- Android Studio с SDK **37**
- Для iOS: macOS, Xcode **27** и iOS Simulator runtime
- Git

### Настройка

```bash
git clone <repository-url>
cd CoffeePeek-Android
cp local.properties.example local.properties
```

Заполните `local.properties` (подробнее в [local.properties.example](./local.properties.example)):

| Ключ | Назначение |
|------|------------|
| `sdk.dir` | Путь к Android SDK |
| `API_BASE_URL` | URL backend API |
| `GOOGLE_WEB_CLIENT_ID` | Google Sign-In (опционально) |

Полная инструкция для контрибьюторов: **[CONTRIBUTING.md](./CONTRIBUTING.md)**

### Android: сборка и запуск

```bash
./gradlew :composeApp:assembleDirectDebug
```

Или Run `composeApp` из Android Studio на эмуляторе/устройстве.

### iOS: сборка и запуск

```bash
open iosApp/iosApp.xcodeproj
```

В Xcode выберите схему `iosApp`, любой iPhone Simulator и нажмите `Run` (`⌘R`).
Подробная настройка Xcode, Google Sign-In, подписи и запуск на реальном iPhone описаны в **[docs/IOS_SETUP.md](./docs/IOS_SETUP.md)**.

---

## Структура проекта

```
CoffeePeek-Android/
├── composeApp/              UI, ViewModel, навигация, тема, Koin
│   └── src/
│       ├── commonMain/      Compose UI и общая логика
│       ├── androidMain/     MapLibre, Google Auth, Android-специфика
│       └── iosMain/         MapLibre bridge, Keychain, PhotosUI, CoreLocation
├── iosApp/                  SwiftUI-оболочка и Xcode-проект
├── modules/
│   ├── domain/              модели и интерфейсы репозиториев
│   ├── network/             Ktor, DTO, API-сервисы
│   ├── data/                реализации репозиториев
│   └── room/                SQLite (сессия, настройки)
├── CONTRIBUTING.md          гайд для разработчиков
├── LOG_ISSUES.md            известные баги из logcat-анализа
└── local.properties.example шаблон секретов
```

**Архитектура:** `Screen → ViewModel → domain.Repository → data → network → API`

**DI:** Koin (`composeApp/.../di/KoinApp.kt`, `modules/data/.../DataModule.kt`)

**Навигация:** `composeApp/.../ui/Navigator.kt`

---

## Конфигурация

- **API URL Android:** `local.properties` → `BuildConfig.API_BASE_URL`
- **API URL iOS:** `iosApp/Configuration/Shared.xcconfig` или локальный `Local.xcconfig`
- **Версия приложения:** `1.0.<git-commit-count>` из `composeApp/build.gradle.kts`
- **Application ID:** `com.coffeepeek`

---

## Известные проблемы

Список открытых багов и приоритетов: [LOG_ISSUES.md](./LOG_ISSUES.md)

---

## Полезные ссылки

- [CONTRIBUTING.md](./CONTRIBUTING.md) — онбординг, стиль кода, PR
- [docs/FIREBASE_CD.md](./docs/FIREBASE_CD.md) — настройка CD через Firebase App Distribution (без Play Console)
- [docs/ANDROID_APK_CD.md](./docs/ANDROID_APK_CD.md) — публикация stable и versioned APK на VPS
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
