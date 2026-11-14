# Firebase App Distribution

Job `publish_firebase` в `.github/workflows/android-apk.yml` доставляет release APK на каждый push в `main`. Используется тот же APK с production backend, который публикуется на сайте; отдельной Firebase-сборки нет.

Workflow `.github/workflows/android-firebase-develop.yml` дополнительно собирает
`directDebug` при push в `develop` и доставляет подписанный APK только в Firebase
App Distribution. Пакет — `com.coffeepeek.debug`, название — CoffeePeek Debug,
иконка — с золотым фоном `#D4A84B`. Используются существующие production backend,
keystore и список тестировщиков, но отдельное Android-приложение Firebase для Debug.
APK и metadata с точным SHA/версией
сохраняются в artifact `coffeepeek-develop-apk-<run-id>`. Публикация на сайте,
Google Play и изменение серверной политики обновлений остаются отдельными.

Обязательные GitHub secrets:

- `FIREBASE_APP_ID_ANDROID` — Android App ID production-приложения для `main`.
- `FIREBASE_GOOGLE_SERVICES_JSON_DEBUG` — конфигурация зарегистрированного
  Android-приложения `com.coffeepeek.debug` в проекте `coffeepeek`. Workflow `develop`
  проверяет пакет и извлекает Firebase App ID из этого же клиента; production App ID
  для Debug не используется. Google OAuth client ID в Debug не требуется.
- `FIREBASE_SERVICE_ACCOUNT_JSON` — JSON service account с доступом к App Distribution.
- `FIREBASE_TESTER_GROUPS` или `FIREBASE_TESTERS` — группы или email тестировщиков через запятую.

При отсутствующей конфигурации job завершается ошибкой. Доставка на сайт и в Play продолжает выполняться независимо.

Debug подписывается существующим distribution keystore из `ANDROID_KEYSTORE_*` /
`ANDROID_KEY_*`. Локальная сборка без этих переменных использует debug keystore;
замена локальной Debug на Firebase Debug требует совпадения подписи. Sandbox
всегда использует debug keystore и пакет `com.coffeepeek.sandbox`. Prod, Debug и
Sandbox хранят данные раздельно; production-обновления в Debug отключены.

Для локальной интеграции конфигурацию Debug можно положить в игнорируемый файл
`composeApp/src/debug/google-services.json`; production-конфигурация остаётся в
`composeApp/google-services.json` или `composeApp/src/release/google-services.json`.
Файлы конфигурации и ключи не коммитятся. До первой доставки нужно зарегистрировать
новый package ID в Firebase и заполнить `FIREBASE_GOOGLE_SERVICES_JSON_DEBUG`.

Сборка и подпись описаны в [GOOGLE_PLAY_CD.md](GOOGLE_PLAY_CD.md). Отдельный workflow `Android CI` проверяет PR в `main` и `develop` без публикации и release secrets.
