# Android release: Google Play, сайт и Firebase

Сверено с исходниками и workflow 27.11.2026.

Workflow `.github/workflows/android-apk.yml` (`Android Release`) запускается при push в `main` или вручную из `main`.

1. Запускает Android unit tests и lint.
2. Последовательно собирает подписанные release APK и AAB отдельными вызовами Gradle с heap 6 GiB и двумя workers.
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

Публичная версия iOS и Android читается из `MARKETING_VERSION` в `iosApp/Configuration/Shared.xcconfig`: сейчас `2.0.0`. Android `versionName` берётся из этого значения в `composeApp/build.gradle.kts`; номер сборки хранится отдельно в `versionCode` / `CFBundleVersion`.

Общий номер сборки — `BUILD_NUMBER_OFFSET + git rev-list --count HEAD`; текущий offset `1100` читается из `Shared.xcconfig`. iOS хранит этот номер в `CFBundleVersion`, Android — в `versionCode` и суффиксе версии. Это конфигурация исходников, не номер последней опубликованной сборки. Повтор того же SHA не повышает номер. Сверяйте artifact metadata и выбранный SHA, не переписывайте историю `main`; при сбое канала повторяйте его failed jobs.

Production-сборки публикуются только из `main`. CI не активирует APK и не меняет серверные актуальный/минимальный коды. После доступности сборки в нужном канале администратор сохраняет политику Android/iOS на экране «Приложения». Актуальный код Android относится к Google Play; production APK выбирается отдельно в таблице релизов. Минимум Android общий для обоих каналов, поэтому его повышают только после доступности нужной сборки в обоих каналах.

APK с сайта/Firebase и APK из Play могут иметь разные сертификаты, если upload key отличается от app signing key. Зарегистрируйте нужные SHA-1 для Google Sign-In; обновление между такими установками поверх приложения невозможно.

## Обновление внутри приложения

Сборка `play` обновляется через Play In-App Updates, без браузера. Сборка `direct` для сайта и Firebase скачивает APK через Android DownloadManager, показывает прогресс и вызывает системное подтверждение установки. Только `direct` содержит разрешение `REQUEST_INSTALL_PACKAGES`.

В `play` доступное обновление сразу запускает нативное подтверждение Google Play, без предварительной карточки CoffeePeek. Необязательное обновление использует flexible flow: загрузка идёт в фоне, затем snackbar предлагает перезапустить приложение. Обязательное обновление использует immediate flow с нативным полноэкранным интерфейсом; незавершённый flow восстанавливается при возвращении в приложение. Для обязательной загрузки сохраняется блокирующая карточка под нативным экраном, в том числе при восстановлении старой фоновой загрузки. При отмене или ошибке она позволяет повторить попытку и не даёт использовать версию ниже минимума. При запуске новой загрузки проверяется, что доступная в Play сборка соответствует серверному порогу.

Для проверки нативного окна установите `play` из доступного этому аккаунту testing track и опубликуйте там сборку с большим versionCode. Локально установленный debug APK не проверяет доступность окна Play. После отмены необязательного обновления проверьте отсутствие повторного окна при возврате и возможность ручной проверки в настройках; после фоновой загрузки — установку по кнопке перезапуска. Для обязательного обновления проверьте отмену, повторную попытку и восстановление после перезапуска процесса.

Release tasks: `:composeApp:assembleDirectRelease :composeApp:bundlePlayRelease`. У обоих вариантов одинаковые application ID и versionCode; подписи установленных приложений должны быть совместимы с выбранным каналом.

Проверка APK отклоняет другой package name, неожиданный versionCode, старую версию и несовместимую подпись. При отмене загрузка удаляется; после перезапуска восстановление выполняется по сохранённому DownloadManager ID. Необязательную карточку можно скрыть свайпом вверх, загрузка продолжится.
