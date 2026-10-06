# Firebase App Distribution

Job `publish_firebase` в `.github/workflows/android-apk.yml` доставляет release APK на каждый push в `main`. Используется тот же APK с production backend, который публикуется на сайте; отдельной Firebase-сборки нет.

Обязательные GitHub secrets:

- `FIREBASE_APP_ID_ANDROID` — Android App ID в Firebase.
- `FIREBASE_SERVICE_ACCOUNT_JSON` — JSON service account с доступом к App Distribution.
- `FIREBASE_TESTER_GROUPS` или `FIREBASE_TESTERS` — группы или email тестировщиков через запятую.

При отсутствующей конфигурации job завершается ошибкой. Доставка на сайт и в Play продолжает выполняться независимо.

Сборка и подпись описаны в [GOOGLE_PLAY_CD.md](GOOGLE_PLAY_CD.md). Отдельный workflow `Android CI` проверяет PR в `main` и `dev` без публикации и release secrets.
