# Централизованное логирование BLPS через Elasticsearch, Logstash и Kibana

## Что реализовано

Система собирает два вида логов:

1. Приложение и WildFly пишут структурированные JSON-события в:

   ```text
   wildfly/standalone/log/blps.json
   ```

2. PostgreSQL, Camunda, RabbitMQ, Dolibarr и их базы отправляют stdout/stderr в Logstash через Docker GELF logging driver.

Общий поток:

```text
Spring Boot / WildFly JSON ─┐
                            ├─> Logstash ─> Elasticsearch ─> Kibana
Docker services / GELF ─────┘
```

В приложении логируются:

- начало и завершение каждого HTTP-запроса;
- HTTP-статус и длительность;
- вход пользователя, ошибки JWT, 401 и 403;
- ожидаемые клиентские ошибки и необработанные исключения;
- запуск и остановка приложения;
- запуск scheduler и retry scheduler;
- создание заявок на смену тарифа, отключение услуги и списание абонентской платы;
- запуск экземпляров процессов Camunda;
- начало, успех, ошибка и retries каждой external task;
- причины бизнес-отказов;
- биллинговые транзакции и уведомления;
- операции Dolibarr и EIS-аудита.

Секреты, JWT, пароли, API-ключи, `Authorization` и тела HTTP-запросов не записываются.

## Основные поля

В Elasticsearch доступны:

- `@timestamp`;
- `service.name`;
- `service.environment`;
- `log.level`;
- `message`;
- `correlation.id`;
- `event.category`;
- `event.action`;
- `event.outcome`;
- `event.duration`;
- `user.name`;
- `user.roles`;
- `http.request.method`;
- `http.response.status_code`;
- `url.path`;
- `process.definition.key`;
- `process.instance.id`;
- `process.business.key`;
- `camunda.task.id`;
- `camunda.topic`;
- `business.operation`;
- `business.request.id`;
- `subscriber.id`;
- `error.type`;
- `error.message`;
- `error.stack_trace`.

`event.duration` хранится в наносекундах в соответствии с ECS.

## Требования

- Docker Desktop или Docker Engine с Compose;
- минимум 6 ГБ памяти для Docker, рекомендуется 8 ГБ для полного стенда;
- установленный локальный WildFly в каталоге `wildfly`;
- `curl` для smoke-теста;
- Java 17.

## Запуск полного стенда

Запустить инфраструктуру и ELK:

```bash
sh scripts/elk-start.sh
```

Эквивалентная команда:

```bash
docker compose up -d
```

Проверить контейнер инициализации:

```bash
docker compose ps -a
```

`blps-elk-setup` должен завершиться с кодом `0`. Он автоматически:

- создаёт ILM policy с удалением логов через 14 дней;
- устанавливает index template;
- импортирует data view;
- импортирует готовый Kibana dashboard.

## Настройка JSON-логов WildFly

Запустить WildFly:

```bash
sh scripts/helios-start.sh
```

В другом терминале выполнить:

```bash
sh scripts/configure-wildfly-elk-logging.sh
```

Скрипт идемпотентный: его можно запускать повторно. Он создаёт:

- JSON formatter `BLPS_JSON`;
- daily rotating file handler `BLPS_JSON_FILE`;
- файл `wildfly/standalone/log/blps.json`;
- подключение handler к root logger;
- уровень `INFO` для пакета `ru.urasha.callmeani.blps`.

После настройки WildFly автоматически перезагружается.

Собрать и развернуть приложение:

```bash
GRADLE_USER_HOME=.gradle-local ./gradlew clean war
sh scripts/deploy-root-war.sh
```

## Проверка ELK

```bash
sh scripts/elk-smoke-test.sh
```

Адреса:

- Elasticsearch: <http://127.0.0.1:9200>
- Logstash API: <http://127.0.0.1:9600>
- Kibana: <http://127.0.0.1:5601>
- Dashboard: <http://127.0.0.1:5601/app/dashboards#/view/blps-observability-dashboard>

## Поиск в Kibana

Открыть Discover и выбрать data view `blps-logs-*`.

Полезные KQL-запросы:

```text
log.level: (WARN or ERROR)
```

```text
service.name: blps
```

```text
correlation.id: "нужный-id"
```

```text
process.instance.id: "camunda-process-instance-id"
```

```text
business.request.id: 42
```

```text
camunda.topic: *
```

```text
event.action: http_request_completed and http.response.status_code >= 400
```

```text
business.operation: tariff-change
```

## Сценарий демонстрации преподавателю

### 1. Показать инфраструктуру

```bash
docker compose ps -a
```

Показать Elasticsearch, Logstash, Kibana и прикладные контейнеры.

### 2. Показать dashboard

Открыть:

```text
http://127.0.0.1:5601/app/dashboards#/view/blps-observability-dashboard
```

Dashboard содержит:

- распределение логов по уровням;
- события во времени;
- WARN/ERROR по сервисам;
- Camunda external tasks по topic;
- HTTP-ответы по статусам.

### 3. Успешная смена тарифа

Выполнить логин и запрос смены тарифа. Из ответа скопировать заголовок:

```text
X-Correlation-Id
```

Найти в Kibana:

```text
correlation.id: "<скопированный-id>"
```

В одной цепочке должны быть видны:

1. HTTP request started;
2. принятие заявки;
3. запуск процесса Camunda;
4. external tasks смены тарифа;
5. биллинговые транзакции;
6. обновление тарифа;
7. уведомление;
8. EIS-аудит;
9. HTTP request completed.

### 4. Бизнес-отказ

Вызвать смену на уже подключённый тариф или создать ситуацию с недостаточным балансом.

В Kibana найти:

```text
business.operation: tariff-change and log.level: WARN
```

В сообщении будет причина:

- `tariff_already_selected`;
- `insufficient_funds`;
- `eis_validation`.

### 5. Техническая ошибка и retry

Остановить Camunda:

```bash
docker stop blps-camunda
```

Через несколько секунд найти:

```text
event.action: camunda_fetch_and_lock and log.level: WARN
```

Вернуть Camunda:

```bash
docker start blps-camunda
```

Для ошибки отдельной external task можно временно остановить Dolibarr и запустить списание:

```bash
docker stop blps-dolibarr
```

Фильтр:

```text
camunda.topic: create-dolibarr-invoice and event.outcome: failure
```

Событие содержит stack trace и число оставшихся retries.

### 6. Ошибки безопасности

Выполнить запрос без JWT или с недостаточными правами:

```text
event.category: (authentication or authorization)
```

Будут видны 401/403 без записи самого токена.

## Остановка

```bash
sh scripts/elk-stop.sh
```

Данные Elasticsearch сохраняются в Docker volume.

Для полного удаления данных:

```bash
docker compose down -v
```

Команда удаляет все volumes объединённого стенда, включая базы приложения, поэтому использовать её следует только для полного сброса лабораторного окружения.

## Локальная безопасность

Elasticsearch и Kibana привязаны к `127.0.0.1`, а встроенная Elastic security отключена только для лабораторного локального стенда.

При размещении на удалённом сервере необходимо:

- включить `xpack.security.enabled`;
- настроить пароли и TLS;
- не публиковать Elasticsearch напрямую в интернет;
- ограничить доступ к Kibana;
- хранить секреты в переменных окружения или secret storage.
