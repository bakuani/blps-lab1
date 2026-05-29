# План показа лабораторной 3 (подробная версия)

## 1) Цель показа
Показать, что в проекте реализованы:
1. Асинхронная обработка бизнес-операций через RabbitMQ + JMS.
2. Выполнение периодических задач по расписанию через `@Scheduled`.
3. Интеграция с внешней EIS (Dolibarr): валидация перед операцией + аудит после операции.
4. Connector-слой в стиле JCA для аудита (через модуль `dolibarr-audit-ra`).

## 2) Что подготовить до демонстрации
1. Поднять инфраструктуру:
   - `docker compose up -d postgres rabbitmq dolibarr-db dolibarr`
2. Проверить доступность:
   - RabbitMQ UI: `http://localhost:15672`
   - Dolibarr UI/API: `http://localhost:8081`
3. Если backend работает на helios:
   - поднять туннель (`scripts/open-tunnel.sh` или вручную).
4. Запуск backend с переменными окружения:
   - `RABBITMQ_HOST=127.0.0.1`
   - `RABBITMQ_PORT=15673`
   - `EIS_DOLIBARR_URL=http://127.0.0.1:28081`
   - `EIS_DOLIBARR_AUDIT_ENABLED=true`
5. Импортировать коллекцию:
   - `docs/insomnia-export.json`

---

## 3) Подробно: что сделано и где это в коде

### 3.1 Точка входа: включение JMS и scheduler
Файл: `src/main/java/ru/urasha/callmeani/blps/BlpsApplication.java`

Ключевые строки:
```java
@EnableJms
@EnableScheduling
```

Что это значит:
1. `@EnableJms` включает обработку `@JmsListener`.
2. `@EnableScheduling` включает `@Scheduled`.
3. Без них:
   - слушатели очередей не поднимутся;
   - cron-задачи не будут исполняться.

---

### 3.2 Конфигурация JMS: зачем каждая часть
Файл: `src/main/java/ru/urasha/callmeani/blps/config/JmsConfig.java`

#### Блок 1: параметры подключения к RabbitMQ
```java
@Value("${rabbitmq.host}") private String host;
@Value("${rabbitmq.port}") private int port;
@Value("${rabbitmq.username}") private String username;
@Value("${rabbitmq.password}") private String password;
@Value("${rabbitmq.vhost}") private String vhost;
```

Назначение:
1. `host`/`port` - адрес брокера.
2. `username`/`password` - авторизация.
3. `vhost` - виртуальный хост RabbitMQ (изоляция пространства имен).

#### Блок 2: `ConnectionFactory`
```java
RMQConnectionFactory factory = new RMQConnectionFactory();
factory.setHost(host);
factory.setPort(port);
factory.setUsername(username);
factory.setPassword(password);
factory.setVirtualHost(vhost);
return new CachingConnectionFactory(factory);
```

Почему так:
1. `RMQConnectionFactory` - JMS-мост к RabbitMQ.
2. `CachingConnectionFactory` уменьшает накладные расходы:
   - не открывает новое соединение на каждое сообщение;
   - переиспользует сессии и продюсеры.

#### Блок 3: `MessageConverter`
```java
converter.setTargetType(MessageType.TEXT);
converter.setTypeIdPropertyName("_type");
```

Почему важно:
1. Сообщения идут как JSON `TextMessage` (удобно дебажить).
2. `_type` помогает при десериализации в нужный Java-класс.

#### Блок 4: `JmsTemplate`
```java
JmsTemplate template = new JmsTemplate(connectionFactory);
template.setMessageConverter(jmsMessageConverter);
```

Почему важно:
1. Отправка сообщений (`convertAndSend`) выполняется без ручной сериализации.
2. Payload-объект автоматически превращается в JSON.

#### Блок 5: `DefaultJmsListenerContainerFactory`
```java
factory.setSessionTransacted(true);
```

Смысл:
1. Обработка сообщения идет в транзакции JMS-сессии.
2. При ошибке в listener сообщение не считается подтвержденным и может быть доставлено повторно.
3. Это повышает надежность (паттерн at-least-once).

---

### 3.3 Асинхронная смена тарифа (полный pipeline)
Файлы:
1. `src/main/java/ru/urasha/callmeani/blps/api/controller/TariffController.java`
2. `src/main/java/ru/urasha/callmeani/blps/service/tariff/async/TariffChangeAsyncService.java`

#### Шаг A: submit через HTTP
Ключевой endpoint:
```java
@PostMapping("/subscribers/{subscriberId}/tariff/change")
```

Что происходит:
1. Контроллер передает запрос в `tariffChangeAsyncService.submitTariffChange(...)`.
2. Клиент получает `202 Accepted` и `requestId`.

#### Шаг B: создание request + постановка в очередь
Ключевые строки в `submitTariffChange(...)`:
```java
requestEntity.setStatus(TariffChangeRequestStatus.PENDING);
...
TariffChangeRequest saved = tariffChangeRequestRepository.save(requestEntity);
jmsTemplate.convertAndSend(tariffChangeQueue, new TariffChangeRequestedMessage(saved.getId()));
```

Что важно сказать:
1. В БД сохраняется отдельная запись запроса.
2. В очередь отправляется только легковесное сообщение с `requestId`.
3. Реальная бизнес-обработка идет асинхронно.

#### Шаг C: обработка в listener
Ключевая аннотация:
```java
@JmsListener(destination = "${app.jms.tariff-change-queue}")
```

Ключевые этапы в `processTariffChange(...)`:
1. Загрузка request по `requestId`.
2. Перевод в `PROCESSING`, `attemptCount + 1`.
3. Вызов EIS-валидации:
```java
if (!eisValidationService.allowTariffChange(request)) {
    request.setStatus(TariffChangeRequestStatus.REJECTED);
    request.setErrorMessage(ApiMessages.TARIFF_CHANGE_REJECTED_BY_EIS);
    ...
    return;
}
```
4. Если EIS разрешил - вызов основной логики:
```java
ChangeTariffResponse response = tariffManagementService.changeTariff(...)
```
5. Установка terminal-статуса:
   - `SUCCESS` или `REJECTED`.
6. При исключении:
   - `RETRY` (до 3 попыток) или `FAILED`.
7. После terminal-статуса - аудит в EIS:
```java
publishEisResult(request, operationAmount);
```

#### Шаг D: получение статуса
Endpoint:
```java
@GetMapping("/subscribers/{subscriberId}/tariff-change-requests/{requestId}")
```

Что клиент видит:
1. `PENDING` -> `PROCESSING` -> `SUCCESS/REJECTED/FAILED`.
2. `errorMessage` показывает причину отклонения/ошибки.

---

### 3.4 Асинхронное отключение услуги (feature disable)
Файлы:
1. `src/main/java/ru/urasha/callmeani/blps/api/controller/FeatureController.java`
2. `src/main/java/ru/urasha/callmeani/blps/service/feature/async/FeatureDisableAsyncService.java`

Pipeline аналогичен смене тарифа:
1. `POST /subscribers/{subscriberId}/features/{featureId}/disable` -> создается request (`PENDING`) -> отправка в очередь.
2. `@JmsListener` обрабатывает:
   - EIS-валидация;
   - бизнес-логика отключения;
   - terminal-статус;
   - аудит результата.
3. `GET /subscribers/{subscriberId}/feature-disable-requests/{requestId}` -> poll статуса.

Важные строки:
```java
@JmsListener(destination = "${app.jms.feature-disable-queue}")
```
```java
if (!eisValidationService.allowFeatureDisable(request)) { ... REJECTED ... }
```
```java
eisOperationAuditService.registerOperationResult(...)
```

---

### 3.5 Периодическое списание monthly fee
Файлы:
1. `src/main/java/ru/urasha/callmeani/blps/service/billing/async/MonthlyFeeChargeScheduler.java`
2. `src/main/java/ru/urasha/callmeani/blps/service/billing/async/MonthlyFeeChargeAsyncService.java`
3. `src/main/java/ru/urasha/callmeani/blps/api/controller/BillingController.java`

#### Шаг A: cron-триггер
```java
@Scheduled(cron = "${app.scheduler.monthly-fee-cron}")
```

Что происходит:
1. Планировщик вызывает `enqueueCurrentCycleCharges()`.
2. Для абонентов формируются `MonthlyFeeChargeRequest` с `PENDING`.
3. В очередь уходит `MonthlyFeeChargeRequestedMessage`.

#### Шаг B: async-обработка monthly fee
Listener:
```java
@JmsListener(destination = "${app.jms.monthly-fee-queue}", concurrency = "2")
```

Почему `concurrency = "2"` важно:
1. Одновременно работают два consumer-потока.
2. Это демонстрирует распределенную параллельную обработку задач.

Критичные проверки в обработчике:
1. Есть ли тариф у абонента.
2. Не списывали ли уже за тот же период.
3. Хватает ли баланса.
4. Создание billing transaction + notification.

При ошибках:
1. `RETRY`/`FAILED` по числу попыток.
2. После terminal статуса - аудит.

#### Шаг C: чтение статуса
Endpoints:
1. `GET /subscribers/{subscriberId}/monthly-fee-requests`
2. `GET /subscribers/{subscriberId}/monthly-fee-requests/{requestId}`

---

### 3.6 Retry scheduler (повторная доставка зависших задач)
Файл: `src/main/java/ru/urasha/callmeani/blps/scheduler/RetryScheduler.java`

Ключевые строки:
```java
@Scheduled(cron = "${app.scheduler.retry-cron:0 */5 * * * *}")
```
```java
List<TariffChangeRequestStatus> targetStatuses = List.of(PENDING, RETRY);
```

Как работает:
1. Раз в заданный cron смотрит старые `PENDING/RETRY` записи.
2. Повторно отправляет их в соответствующую JMS-очередь.
3. Обновляет `updatedAt`, чтобы не дергать одну и ту же запись слишком часто.

Что это доказывает на защите:
1. Есть механизм восстановления после сбоев.
2. Обработка не зависит от единственной попытки.

---

### 3.7 Dolibarr как EIS-валидатор (до выполнения операции)
Файлы:
1. `src/main/java/ru/urasha/callmeani/blps/config/DolibarrProperties.java`
2. `src/main/java/ru/urasha/callmeani/blps/config/DolibarrClientConfig.java`
3. `src/main/java/ru/urasha/callmeani/blps/service/eis/impl/DolibarrValidationServiceImpl.java`

#### Алгоритм `DolibarrValidationServiceImpl`
1. Получить `Subscriber` по `subscriberId`.
2. Нормализовать телефон.
3. Найти `thirdparty` в Dolibarr по телефону (`/thirdparties`, фильтр `sqlfilters`).
4. Если thirdparty не найден -> `false` (операция отклоняется).
5. Проверить unpaid invoices (`/invoices?thirdparty_ids=...&status=unpaid`).
6. Если есть хотя бы один unpaid -> `false`.
7. Иначе `true`.

#### Ошибки сети/HTTP
Критичная логика:
```java
boolean decision = !dolibarrProperties.isFailClosed();
```

При `failClosed=true`:
1. Любая техническая ошибка Dolibarr => `decision=false`.
2. То есть операция блокируется (безопасный режим).

---

### 3.8 JCA-style аудит в Dolibarr (после terminal статуса)
Файлы:
1. `src/main/java/ru/urasha/callmeani/blps/service/eis/impl/DolibarrOperationAuditJcaService.java`
2. `src/main/java/ru/urasha/callmeani/blps/config/DolibarrAuditConnectionConfig.java`
3. `dolibarr-audit-ra/src/main/java/.../DolibarrInteraction.java`

#### Когда запускается аудит
Во всех async-сервисах только для terminal-статусов:
1. `SUCCESS`
2. `REJECTED`
3. `FAILED`

#### Что отправляется
`EisOperationResult` содержит:
1. `operationType` (`TariffChangeRequested`, `FeatureDisableRequested`, `MonthlyFeeChargeRequested`)
2. `requestId`
3. `subscriberId`
4. `amount`
5. `status`
6. `errorReason`
7. `processedAt`

#### Как отправляется
1. `DolibarrOperationAuditJcaService` берет `DolibarrConnectionFactory`.
2. Создает `DolibarrInteraction`.
3. Вызывает `interaction.execute(interactionName, payload)`.

#### Что значит в текущей конфигурации
Если `eis.dolibarr.audit.interaction-name=status`, то фактически проверяется канал до Dolibarr через `/status`.
Это технический аудит канала, а не запись бизнес-объекта в UI Dolibarr.

#### Где смотреть результат
В логах backend:
1. `Dolibarr audit sent via local connector ...`
2. `Dolibarr audit response is negative ...`
3. `Dolibarr audit send failed ...`

---

### 3.9 Статусы заявок: как объяснять преподавателю
`TariffChangeRequestStatus`:
1. `PENDING` - создано, ожидает обработку.
2. `PROCESSING` - listener взял в работу.
3. `SUCCESS` - выполнено успешно.
4. `REJECTED` - бизнес/валидация отклонила.
5. `RETRY` - временная ошибка, будет повтор.
6. `FAILED` - исчерпаны попытки или фатальная ошибка.

---

## 4) Полный разбор аргументов из `application.properties`
Файл: `src/main/resources/application.properties`

Важно проговорить: справа в `${...:default}` указано значение по умолчанию, которое заменяется env-переменной на стенде.

### 4.1 База данных
1. `spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5433/studs}`
2. `spring.datasource.username=${DB_USER:s413022}`
3. `spring.datasource.password=${DB_PASSWORD:1i3O5V9ts2y1GN7M}`
4. `spring.datasource.driver-class-name=org.postgresql.Driver`
5. `spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:create}`
6. `spring.jpa.properties.hibernate.format_sql=true`

### 4.2 HTTP и JWT
1. `server.port=${SERVER_PORT:8080}`
2. `jwt.secret=${JWT_SECRET:01234567890123456789012345678901}`
3. `jwt.expiration=PT15M`
4. `jwt.issuer=blps`

### 4.3 Пользователи/безопасность
1. `security.users.xml-path=classpath:security/users.xml`

### 4.4 RabbitMQ/JMS
1. `rabbitmq.host=${RABBITMQ_HOST:localhost}`
2. `rabbitmq.port=${RABBITMQ_PORT:5672}`
3. `rabbitmq.username=${RABBITMQ_USER:guest}`
4. `rabbitmq.password=${RABBITMQ_PASSWORD:guest}`
5. `rabbitmq.vhost=${RABBITMQ_VHOST:/}`

### 4.5 Очереди и scheduler
1. `app.jms.tariff-change-queue=${TARIFF_CHANGE_QUEUE:tariff.change.requests}`
2. `app.jms.feature-disable-queue=${FEATURE_DISABLE_QUEUE:feature.disable.requests}`
3. `app.jms.monthly-fee-queue=${MONTHLY_FEE_QUEUE:monthly.fee.requests}`
4. `app.scheduler.monthly-fee-cron=${MONTHLY_FEE_CRON:*/30 * * * * *}`
5. `app.scheduler.monthly-fee-cycle-pattern=${MONTHLY_FEE_CYCLE_PATTERN:yyyy-MM-dd-HH:mm}`
6. `app.scheduler.retry-cron=${RETRY_CRON:0 */1 * * * *}`

### 4.6 Dolibarr EIS
1. `eis.dolibarr.url=${EIS_DOLIBARR_URL:http://localhost:8081}`
2. `eis.dolibarr.api-key=${EIS_DOLIBARR_API_KEY:test_api_key}`
3. `eis.dolibarr.fail-closed=${EIS_DOLIBARR_FAIL_CLOSED:true}`
4. `eis.dolibarr.connect-timeout-ms=${EIS_DOLIBARR_CONNECT_TIMEOUT_MS:3000}`
5. `eis.dolibarr.read-timeout-ms=${EIS_DOLIBARR_READ_TIMEOUT_MS:5000}`
6. `eis.dolibarr.audit.enabled=${EIS_DOLIBARR_AUDIT_ENABLED:false}`
7. `eis.dolibarr.audit.interaction-name=${EIS_DOLIBARR_AUDIT_INTERACTION:status}`

Ключевые акценты:
1. Для демонстрации аудита нужно `EIS_DOLIBARR_AUDIT_ENABLED=true`.
2. При `fail-closed=true` недоступность Dolibarr блокирует операции (REJECTED_BY_EIS).

---

## 5) Подробный сценарий показа в Insomnia (шаг за шагом)

### 5.1 Подготовка Environment
Проверить значения:
1. `base_url = http://localhost:8180`
2. `admin_username = admin1`
3. `admin_password = password`
4. `subscriber_id = 1`
5. `tariff_id = 2`
6. `feature_id = 1`
7. `dolibarr_base = http://localhost:8081`
8. `dolibarr_api_key = <твой api key>`

Что сказать:
1. Все запросы идут либо в BLPS API, либо напрямую в Dolibarr для диагностики.

### 5.2 Login
Папка: `Auth`
1. Запрос: `Login as ADMIN`
2. Ожидание: `200`, есть `accessToken`.
3. Действие: записать токен в `active_token`/`admin_token`.

Что сказать:
1. Дальнейшие защищенные endpoint работают с JWT.

### 5.3 Инициализация данных
Папка: `Management - Test Data Init`
Прогнать последовательно 13 запросов создания.

Ожидание:
1. Обычно `201`.
2. Если дубль - возможна ошибка (это не баг async-логики, а состояние данных).

Что сказать:
1. Здесь формируется минимальный набор данных для сценариев async.

### 5.4 Tariff async submit + poll
Папка: `Tariff flow` или `Lab3 - EIS Dolibarr JCA Tests`
1. Запрос submit: `Change tariff (async submit)` / `1) TariffChangeRequested submit`.
2. Ожидание: `202`, тело:
   - `requestId`
   - `status = PENDING`
3. Подставить `requestId` в переменную.
4. Выполнить poll:
   - `Get tariff change request status` / `2) ... status poll`
5. Повторить 1-2 раза.

Ожидание:
1. `PENDING` или `PROCESSING` на раннем poll.
2. Затем terminal:
   - `SUCCESS`, если валидация прошла и хватает баланса;
   - `REJECTED` при EIS reject или нехватке денег.

Что сказать:
1. Это доказывает асинхронную модель: клиент сразу получает `202`, итог позже через отдельный endpoint.

### 5.5 Feature async submit + poll
Папка: `Features flow` или `Lab3 - EIS Dolibarr JCA Tests`
1. Submit: `Disable feature (async submit)` / `3) FeatureDisableRequested submit`.
2. Ожидание: `202`, `PENDING`.
3. Poll: `Get feature disable request status` / `4) ... status poll`.
4. Ожидание terminal-статуса.

Что сказать:
1. Второй независимый async use-case подтверждает универсальность архитектуры.

### 5.6 Проверка scheduler monthly fee
Папка: `Billing cycle flow`
1. Подождать срабатывания `monthly-fee-cron` (по умолчанию каждые 30 сек).
2. Запрос: `Get recent monthly fee requests`.
3. Выбрать `requestId`.
4. Запрос: `Get monthly fee request status`.

Ожидание:
1. Есть записи monthly fee.
2. Статусы переходят в terminal.

Что сказать:
1. Периодическая задача действительно запускает асинхронные запросы без ручного submit.

### 5.7 Проверка retry
Папка: `Testing Retry Logic`
1. Запрос: `1. Simulate Stuck Request (For Retry)`.
2. Подождать `retry-cron` (по умолчанию раз в минуту).
3. Запрос: `2. Check Stuck Request Status`.

Ожидание:
1. Запрос повторно отправится в очередь.
2. В логах backend: `Retrying ...`.

### 5.8 Dolibarr diagnostics
Папка: `Dolibarr Diagnostics`
1. `1) Dolibarr status`
2. `2) List thirdparties`
3. `3) Unpaid invoices by thirdparty`

Ожидание:
1. Рабочий API-канал и валидный `DOLAPIKEY`.
2. Можно объяснить причины `REJECTED_BY_EIS` через фактические данные в Dolibarr.

### 5.9 Подтверждение audit pipeline
После async terminal-результатов показать backend-логи:
1. `Dolibarr audit sent via local connector`
2. `Dolibarr validation allowed`
3. `Dolibarr validation rejected`
4. `Dolibarr audit send failed` (если негативный кейс)

Что сказать:
1. `validation` происходит до бизнес-операции.
2. `audit` происходит после terminal-статуса и отправляет итог операции во внешний контур.

---

## 6) Частые вопросы и готовые ответы

1. Почему `REJECTED`, если invoice нет?
   - Потому что reject может быть не только из-за долга: например, `thirdparty_not_found` или `fail-closed` при недоступности Dolibarr.

2. Почему `REJECTED: Insufficient funds`?
   - EIS-валидация прошла, но бизнес-логика тарифной операции отклонила из-за баланса.

3. Где распределенность?
   - Сообщения обрабатываются listener-ами независимо от submit потока, monthly fee listener имеет `concurrency = "2"`; при запуске нескольких узлов они могут конкурировать за очередь.

4. Что именно “JCA-style”?
   - Используется connector-модуль (`ConnectionFactory`, `ManagedConnection`, `Interaction`) как отдельный слой интеграции, а не прямые вызовы RestClient из бизнес-сервиса аудита.

5. Можно ли увидеть audit в интерфейсе Dolibarr?
   - В текущей демо-конфигурации `interaction-name=status` это проверка канала, смотреть нужно backend-логи. Для видимого объекта в UI нужен endpoint создания сущности Dolibarr.

---

## 7) Короткий финальный текст на защиту
1. Асинхронные use-case реализованы через RabbitMQ + JMS с `@JmsListener` и статусной моделью `PENDING -> PROCESSING -> terminal`.
2. Периодические операции реализованы через `@Scheduled`, включая retry-механику.
3. EIS-интеграция с Dolibarr используется в двух местах:
   - pre-check валидация операции;
   - post-result аудит terminal-результата через connector-слой в стиле JCA.
4. Поведение полностью управляется внешней конфигурацией (`application.properties` + env), что упрощает перенос между локальным стендом и helios.
