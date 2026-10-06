# Android release: Google Play, сайт и Firebase

Workflow `.github/workflows/android-apk.yml` (`Android Release`) запускается при push в `main` или вручную из `main`.

1. Запускает Android unit tests и lint.
2. Собирает подписанные release APK и AAB одним вызовом Gradle.
3. Сохраняет APK, AAB и R8 mapping в artifacts.
4. Независимые jobs загружают AAB с mapping в Google Play internal testing, APK на сайт и тот же APK в Firebase App Distribution.

Production в Play выпускается продвижением проверенной сборки через Play Console. Сайт получает stable APK автоматически; запись в backend создаётся как черновик для ручной публикации в админке.

## Настройка Google Play

- В Play Console создайте приложение `com.coffeepeek` и вручную загрузите первый AAB.
- Настройте Play App Signing и убедитесь, что GitHub keystore соответствует upload key.
- В Google Cloud включите Google Play Android Developer API.
- Создайте service account, выдайте ему доступ к приложению и внутренним релизам в Play Console.
- Сохраните JSON service account в GitHub secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.

## Конфигурация сборки

Repository variable: `API_BASE_URL_MAIN` — production backend для всех трёх каналов.

Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `GOOGLE_WEB_CLIENT_ID` (для Google Sign-In).

Настройка сайта: [ANDROID_APK_CD.md](ANDROID_APK_CD.md). Настройка Firebase: [FIREBASE_CD.md](FIREBASE_CD.md).

Если environment `production` требует reviewers, доставка на сайт ожидает подтверждения. Для автоматической доставки этот environment должен разрешать `main` без required reviewers.

## Версии и повторные запуски

Сохраняется текущая версия `1.0.<git-commit-count>`. Следующий versionCode должен быть больше уже загруженного в Play. Не переписывайте историю main. Повторная загрузка того же versionCode в Play будет отклонена: при ошибке одного канала повторяйте только failed jobs.

APK с сайта/Firebase и APK из Play могут иметь разные сертификаты, если upload key отличается от app signing key. Зарегистрируйте нужные SHA-1 для Google Sign-In; обновление между такими установками поверх приложения невозможно.

## Обновление внутри приложения

Сборка `play` обновляется через Play In-App Updates, без браузера. Сборка `direct` для сайта и Firebase скачивает APK через Android DownloadManager, показывает прогресс и вызывает системное подтверждение установки. Только `direct` содержит разрешение `REQUEST_INSTALL_PACKAGES`.

Release tasks: `:composeApp:assembleDirectRelease :composeApp:bundlePlayRelease`. У обоих вариантов одинаковые application ID и versionCode; подписи установленных приложений должны быть совместимы с выбранным каналом.

Проверка APK отклоняет другой package name, неожиданный versionCode, старую версию и несовместимую подпись. При отмене загрузка удаляется; после перезапуска восстановление выполняется по сохранённому DownloadManager ID. Необязательную карточку можно скрыть свайпом вверх, загрузка продолжится.
