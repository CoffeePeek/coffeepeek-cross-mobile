# Аналитика: сведения перед публикацией в сторах

Сверено с клиентскими исходниками 27.11.2026. Это фактическая памятка к [мобильному контракту](ANALYTICS.md), а не подтверждение настройки PostHog Cloud, опубликованной политики или заполненных форм Apple/Google.

## Что делает клиент

- Один PostHog KMP SDK, EU ingest, build-time project ingestion token. Management keys в приложение не входят.
- Сбор возможен после входа и успешной загрузки включённой account preference. Backend default `true` и предустановленная галочка регистрации не являются доказательством явно данного согласия.
- Гость, неизвестная preference, revoke и background не разрешают сбор. Debug требует явного opt-in и настроенного token; Sandbox не отправляет события.
- Identify использует внутренний backend user ID; schema разрешает только заданные события/свойства. Текст поиска/форм, фото, tokens, headers и точные координаты в свойства аналитики не включаются.
- Replay/autocapture/automatic events/error tracking отключены; native wrapper может делать служебные config requests после разрешённого setup. Клиентские фильтры не скрывают сетевой IP от сервера.
- Локальная очередь ограничена foreground-сеансом и очищается при background/revoke/account switch. Это не срок хранения уже доставленных событий в облаке.

## Что сверить с владельцем релиза

| Данные реализации | Разделы для сверки в формах |
| --- | --- |
| Backend user ID / SDK installation identifiers | User ID, Device ID / Device or other IDs |
| Экраны и разрешённые действия | Product Interaction / App interactions |
| Счётчики и длительности | Usage Data / App activity |
| API latency, безопасные error category и HTTP status | Performance Data / Diagnostics |
| Возможное серверное GeoIP/IP-enrichment | Location — только по фактической конфигурации облака |

Связь аналитики с account ID сохраняется даже без email. Отдельно учтите остальные функции приложения: auth, пользовательские фото, чекины, пуши и координаты карты. Клиентский allowlist не является полной декларацией данных приложения.

Перед submission проверьте назначения, обязательность/optional, sharing/service-provider role, tracking и GeoIP по текущим правилам и реальной конфигурации. Не копируйте старые ответы автоматически:
[Apple App Privacy](https://developer.apple.com/app-store/app-privacy-details/),
[управление App Privacy](https://developer.apple.com/help/app-store-connect/manage-app-information/manage-app-privacy),
[Apple privacy/tracking](https://developer.apple.com/app-store/user-privacy-and-data-use/),
[Google Data safety](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en),
[PostHog privacy](https://posthog.com/docs/privacy/data-storage).

## Политика, архив и доставка

Сверьте опубликованный [Privacy Policy](https://coffeepeek.by/privacy), оператора/контакты, хранение, удаление и доступность opt-out. Выключение аналитики не удаляет доставленные ранее события; нельзя обещать такое удаление без действующего серверного процесса.

На macOS проверьте SwiftPM resolution, подписанный Release archive, privacy resources всех SDK, required-reason declarations и Xcode Privacy Report. Generic SDK manifest не заменяет описание фактического identify. Native hooks и beforeSend/revoke нужно проверять отдельно от compile.

PR iOS verification компилирует/исполняет выбранные контракты без публикации. TestFlight обычно запускается из `main`; workflow также содержит явное ручное исключение для `codex/ios-native`. Upload, Apple processing и доступность тестировщикам — отдельные состояния. Ни workflow, ни обновление этого Markdown не публикуют ответы App Privacy/Data safety и не изменяют публичную политику автоматически. Точные настройки — в [iOS setup](IOS_SETUP.md) и [release guides](GOOGLE_PLAY_CD.md).
