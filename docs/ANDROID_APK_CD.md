# Публикация Android APK на VPS

Сверено с workflow 28.11.2026; конфигурация внешнего VPS/админки этим документом не подтверждается.

Workflow `.github/workflows/android-apk.yml` (`Android Release`) собирает подписанные release APK и AAB при push в `main`. Один APK доставляется на сайт и в Firebase; AAB — в Play internal testing.

Публичные ссылки:

- production: `https://coffeepeek.by/downloads/android/main/coffeepeek.apk`
- история: `https://coffeepeek.by/downloads/android/releases/<version>-<code>/coffeepeek.apk`

Стабильный APK заменяется атомарно. Уже опубликованный versioned APK можно повторно использовать только при совпадении размера и SHA-256; заменить его другим файлом workflow не позволит.

## Проверки и зависание Gradle

Android CI и Android Release запускают unit-тесты и lint отдельными вызовами `tools/ci/run_android_checks.sh`. Состав проверок сохраняется: все Android-модули, Direct Debug и Play Debug. Каждый вызов использует до 6 workers, heap 6 ГБ, build cache и отключённый configuration cache; вывод `--info --stacktrace` помогает определить незавершённую задачу или финализацию.

Через 25 минут GNU `timeout` отправляет SIGQUIT всей группе процессов Gradle для дампов потоков JVM в GitHub log, ещё через 30 секунд принудительно завершает зависшие процессы. Таймаут остаётся ошибкой и блокирует доставку. Внешние лимиты: 30 минут на шаг проверки и 90 минут на build job. Полные сырые логи и configuration cache в artifacts не выгружаются.

Диагностика 28.11.2026: в завершённом запуске `46036141392`, attempt 1, после `:composeApp:lintPlayDebug` в 17:01:03 UTC не появилось итогового результата до отмены в 18:42:13 UTC. Обычный CI `46036141399` на том же commit выполнил проверки за 5 минут 24 секунды. В следующем main запуске зависли уже оба workflow на объединённом шаге тестов/lint. Раннеры были выделены и выполняли предыдущие шаги; доказательств OOM или конкретного JVM deadlock в старых логах нет. Разделение шагов и отключение configuration cache — мера локализации и обхода; конкретную блокировку подтверждает только дамп при повторе. Эти изменения не воздействуют на уже запущенные jobs.

Локальная проверка 28.11.2026, база `6f08cbd03`: 58 release-тестов и 4 fake-command теста watchdog прошли; Bash syntax и actionlint прошли. Первый запуск fake-команд выявил зависимость теста от кодировки/локали Windows; после явной UTF-8/C locale тест таймаута прошёл. Отдельный sandbox-запуск блокировался служебным MSYS IPC, повтор вне sandbox прошёл. Android APK на этом изменении локально не пересобирался, устройство не запускалось; исполнение новых workflow на Linux и исчезновение исходного зависания требуют отдельной CI-проверки.

## VPS

```bash
sudo mkdir -p /var/www/coffeepeek-downloads/android/main
sudo mkdir -p /var/www/coffeepeek-downloads/android/releases
sudo chown -R deploy:deploy /var/www/coffeepeek-downloads
```

Пользователь из `VPS_USER` должен иметь права записи в `/var/www/coffeepeek-downloads` и `/tmp`.

Пример серверной настройки для Caddy в Docker Compose (реальную конфигурацию VPS проверяет оператор отдельно):

```yaml
volumes:
  - /var/www/coffeepeek-downloads:/srv/downloads:ro
```

Конфигурация Caddy для `api.coffeepeek.by`:

```caddyfile
handle_path /downloads/android/releases/* {
    root * /srv/downloads/android/releases
    header Cache-Control "public, max-age=31536000, immutable"
    file_server
}

handle_path /downloads/* {
    root * /srv/downloads
    header Cache-Control "public, max-age=60"
    file_server
}

handle {
    reverse_proxy gateway:80
}
```

Если основной домен проксирует downloads через Vercel, соответствующий web-репозиторий должен направлять эти пути на файловый сервер до SPA rewrites, например:

```json
"redirects": [
  {
    "source": "/downloads/:path*",
    "destination": "https://api.coffeepeek.by/downloads/:path*",
    "permanent": false
  }
]
```

## GitHub Environments

Создайте environments:

- `production` — разрешённая ветка `main`; существующие reviewer gates применяются перед доставкой. Изменение этой политики не является частью сборки.

## GitHub Variables

| Variable | Назначение |
|---|---|
| `API_BASE_URL_MAIN` | Production backend URL для APK из `main` |

Production URL обязателен. `GOOGLE_WEB_CLIENT_ID` остаётся опциональным secret.

## GitHub Secrets

Доступ к VPS:

| Secret | Назначение |
|---|---|
| `VPS_HOST` | Hostname или IP VPS |
| `VPS_USER` | SSH-пользователь, обычно `deploy` |
| `VPS_SSH_KEY` | Приватный SSH-ключ |
| `VPS_SSH_FINGERPRINT` | SHA256 fingerprint host key VPS |

Подпись Android:

| Secret | Назначение |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Keystore целиком в base64 без переносов |
| `ANDROID_KEYSTORE_PASSWORD` | Пароль keystore |
| `ANDROID_KEY_ALIAS` | Alias ключа |
| `ANDROID_KEY_PASSWORD` | Пароль ключа |

Регистрация production-релиза в backend:

| Secret | Назначение |
|---|---|
| `COFFEEPEEK_ADMIN_API_URL` | Base URL backend, например `https://api.coffeepeek.by` |
| `COFFEEPEEK_ADMIN_TOKEN` | Bearer token пользователя с ролью `Admin` |

Получить base64 в Linux:

```bash
base64 -w 0 coffeepeek-release.jks
```

Fingerprint VPS берите по доверенному каналу. На самом сервере для ED25519 host key:

```bash
ssh-keygen -l -E sha256 -f /etc/ssh/ssh_host_ed25519_key.pub | awk '{print $2}'
```

## Версии и metadata

Android version name читается из общего с iOS `MARKETING_VERSION` (сейчас `2.0.0`) в `iosApp/Configuration/Shared.xcconfig`. Номер сборки хранится отдельно: `BUILD_NUMBER_OFFSET + git rev-list --count HEAD`, текущий offset `1100`. Источник и правила повторов — [GOOGLE_PLAY_CD.md](GOOGLE_PLAY_CD.md). Workflow сохраняет в GitHub job summary:

- version name и version code;
- размер APK;
- SHA-256;
- UTC-время публикации;
- stable и versioned URL.

После загрузки APK из `main` workflow проверяет доступность immutable URL и автоматически создаёт черновик через `POST /api/admin/v1/app-downloads/android/releases`. Повторный запуск с тем же `VersionCode` пропускает регистрацию, если metadata совпадает, и завершается ошибкой при конфликте.

Bearer token должен оставаться действительным на момент запуска workflow. Публикация черновика выполняется вручную из админки.

CI не меняет серверную политику обновлений и не активирует APK. Актуальный код Google Play и общий минимум Android сохраняются вручную в админке через `PUT /api/admin/v1/app-downloads/android/version`. Для APK актуальный код берётся из production-релиза, выбранного в админке; сервер ограничивает минимум активной версией. Сначала обеспечьте доступность сборки пользователям нужного канала, затем меняйте актуальный код или обязательный минимум. Production workflow запускается только из `main`.

## Rollback

1. Найдите нужный immutable URL в истории workflow или backend.
2. На VPS проверьте SHA-256 выбранного APK.
3. Скопируйте его во временный файл рядом со stable APK.
4. Замените stable APK командой `mv`, чтобы переключение было атомарным.

Versioned-файл при rollback не изменяется.
