# Сбор серверных логов входа через Apple

Для ошибки `ASI-X` нужны логи AccountService и Gateway после подтверждения
системного окна Apple. Команды ниже читают логи и два публичных OAuth ID;
сервисы не перезапускаются, настройки не меняются.

## 1. Повторить ошибку

На iPhone ещё раз выполнить вход через Apple и записать время ошибки.
Собрать логи сразу после этого: команды выбирают последние 10 минут.

## 2. Собрать на VPS

Подключиться к серверу по SSH обычным способом. Если подключение выполняется
из PowerShell, заменить `USER` и `SERVER_IP` своими данными:

```powershell
ssh USER@SERVER_IP
```

В SSH-терминале выполнить:

```bash
cd /opt/coffeepeek/deploy
sudo docker compose logs --since 10m --timestamps --no-color account gateway > ~/coffeepeek-apple.log 2>&1
sudo docker compose exec -T account sh -c 'printf "Apple ClientId=%s\nApple ServiceId=%s\n" "$OAuthAppleOptions__ClientId" "$OAuthAppleOptions__ServiceId"' >> ~/coffeepeek-apple.log
```

Путь `/opt/coffeepeek/deploy` соответствует текущему workflow развёртывания.
Файл `coffeepeek-apple.log` будет в домашней папке пользователя SSH.
Для iOS ожидается `Apple ClientId=com.coffeepeek`; `ServiceId` относится к
браузерному входу и может быть пустым. Менять значения до анализа не нужно.

Можно просмотреть подходящие строки с контекстом:

```bash
grep -n -i -B 8 -A 25 -E 'apple/(login|challenge)|AppleAuth|SecurityToken|IDX[0-9]+' ~/coffeepeek-apple.log
```

Даже если есть только строка HTTP 401 без подробной причины, её следует сохранить:
текущий сервер может скрывать конкретную причину отклонения Apple JWT.

## 3. Скачать и передать

Открыть отдельное окно PowerShell на Windows и выполнить:

```powershell
scp USER@SERVER_IP:coffeepeek-apple.log "$env:USERPROFILE\Downloads\coffeepeek-apple.log"
```

Передать файл из «Загрузок» и время воспроизведения. Перед отправкой скрыть
токены, email и значения заголовков Authorization, если они встретились.
Содержимое `.env` и полный список переменных контейнера для этой проверки не нужны.
