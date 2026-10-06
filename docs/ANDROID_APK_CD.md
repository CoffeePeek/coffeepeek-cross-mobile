# Публикация Android APK на VPS

Workflow `.github/workflows/android-apk.yml` (`Android Release`) собирает подписанные release APK и AAB при push в `main`. Один APK доставляется на сайт и в Firebase; AAB — в Play internal testing.

Публичные ссылки:

- production: `https://coffeepeek.by/downloads/android/main/coffeepeek.apk`
- история: `https://coffeepeek.by/downloads/android/releases/<version>-<code>/coffeepeek.apk`

Стабильный APK заменяется атомарно. Уже опубликованный versioned APK можно повторно использовать только при совпадении размера и SHA-256; заменить его другим файлом workflow не позволит.

## VPS

```bash
sudo mkdir -p /var/www/coffeepeek-downloads/android/main
sudo mkdir -p /var/www/coffeepeek-downloads/android/dev
sudo mkdir -p /var/www/coffeepeek-downloads/android/releases
sudo chown -R deploy:deploy /var/www/coffeepeek-downloads
```

Пользователь из `VPS_USER` должен иметь права записи в `/var/www/coffeepeek-downloads` и `/tmp`.

На текущем VPS файлы раздаёт Caddy из Docker Compose. В сервис `caddy` добавлен read-only mount:

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

Основной домен размещён на Vercel. Его `vercel.json` должен перенаправлять download-маршруты на VPS до SPA rewrites:

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

- `production` — разрешить ветку `main` без required reviewers для автоматической доставки;

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

Версия остаётся в текущем формате проекта: `1.0.<количество-коммитов>`. Workflow сохраняет в GitHub job summary:

- version name и version code;
- размер APK;
- SHA-256;
- UTC-время публикации;
- stable и versioned URL.

После загрузки APK из `main` workflow проверяет доступность immutable URL и автоматически создаёт черновик через `POST /api/admin/v1/app-downloads/android/releases`. Повторный запуск с тем же `VersionCode` пропускает регистрацию, если metadata совпадает, и завершается ошибкой при конфликте.

Bearer token должен оставаться действительным на момент запуска workflow. Публикация черновика выполняется вручную из админки.

## Rollback

1. Найдите нужный immutable URL в истории workflow или backend.
2. На VPS проверьте SHA-256 выбранного APK.
3. Скопируйте его во временный файл рядом со stable APK.
4. Замените stable APK командой `mv`, чтобы переключение было атомарным.

Versioned-файл при rollback не изменяется.
