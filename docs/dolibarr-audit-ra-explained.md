# `dolibarr-audit-ra`: как реализован и зачем нужен

## 1) Что это за модуль
`dolibarr-audit-ra` — отдельный Java-модуль, который реализует локальный connector-слой в стиле JCA для отправки audit-событий в Dolibarr.

Идея:
1. Бизнес-сервисы BLPS не отправляют HTTP напрямую.
2. Они вызывают абстракцию `EisOperationAuditService`.
3. Внутри уже используется `DolibarrConnectionFactory -> DolibarrConnection -> DolibarrInteraction`.

Это соответствует архитектурному стилю JCA: есть factory, managed connection, interaction.

---

## 2) Зачем он нужен
Основной смысл:
1. Выполнить требование про интеграцию через connector-подход (JCA-style).
2. Отделить бизнес-логику от транспортного слоя EIS.
3. Унифицировать отправку внешних событий (одна точка интеграции).
4. Упростить замену/расширение EIS в будущем (меняется connector, не бизнес-сервисы).

Почему не прямой `RestClient` из async-сервиса:
1. Тогда бизнес-код будет смешан с деталями протокола.
2. Сложнее эволюционировать к полноценному RA/JNDI сценарию.
3. Сложнее централизованно контролировать ошибки и формат interaction.

---

## 3) Где он подключается в основном приложении

### 3.1 Spring-конфиг подключения
Файл: `src/main/java/ru/urasha/callmeani/blps/config/DolibarrAuditConnectionConfig.java`

Ключевая логика:
1. Создается `DolibarrManagedConnectionFactory` с:
   - `url`
   - `apiKey`
   - `connectTimeout`
   - `readTimeout`
2. Оборачивается в `DolibarrConnectionFactoryImpl` + `DolibarrLocalConnectionManager`.

Итог:
1. В Spring-контексте появляется bean `DolibarrConnectionFactory`.
2. Его использует `DolibarrOperationAuditJcaService`.

### 3.2 Вызов из async pipeline
Файл: `src/main/java/ru/urasha/callmeani/blps/service/eis/impl/DolibarrOperationAuditJcaService.java`

Что происходит:
1. После terminal статуса операции (`SUCCESS/REJECTED/FAILED`) вызывается `registerOperationResult(...)`.
2. Если `audit.enabled=true`, формируется payload и отправляется через connector.

---

## 4) Архитектура модуля по классам

## 4.1 `DolibarrConnectionFactory` (интерфейс)
Файл: `dolibarr-audit-ra/.../DolibarrConnectionFactory.java`

Назначение:
1. Контракт выдачи connection-handle:
   - `DolibarrConnection getConnection()`.

---

## 4.2 `DolibarrConnectionFactoryImpl`
Файл: `dolibarr-audit-ra/.../DolibarrConnectionFactoryImpl.java`

Роль:
1. Реализация factory через JCA API:
   - хранит `ManagedConnectionFactory`
   - хранит `ConnectionManager`
2. При `getConnection()` вызывает:
   - `connectionManager.allocateConnection(...)`.

Почему важно:
1. Это точка, где абстракция JCA соединяется с прикладным connection-handle.

---

## 4.3 `DolibarrLocalConnectionManager`
Файл: `dolibarr-audit-ra/.../DolibarrLocalConnectionManager.java`

Роль:
1. Локальный `ConnectionManager` (без app server контейнера RA).
2. На `allocateConnection(...)`:
   - создает `ManagedConnection`
   - возвращает connection-handle.

Почему так:
1. Для лабораторного стенда не нужен полный серверный RA deployment.
2. Но структура остается JCA-подобной.

---

## 4.4 `DolibarrManagedConnectionFactory`
Файл: `dolibarr-audit-ra/.../DolibarrManagedConnectionFactory.java`

Роль:
1. Создание `ManagedConnection`.
2. Валидация конфигурации:
   - `baseUrl` не пустой
   - `apiKey` не пустой
3. Реализация `equals/hashCode` по параметрам (с маскировкой api key).

Ключевые моменты:
1. `createConnectionFactory()` умеет работать как с переданным `ConnectionManager`, так и локально.
2. Нормализует base URL (убирает хвостовые `/`).
3. Не хранит “живую” HTTP-сессию, только параметры.

---

## 4.5 `DolibarrManagedConnection`
Файл: `dolibarr-audit-ra/.../DolibarrManagedConnection.java`

Роль:
1. Реализует интерфейс `ManagedConnection`.
2. Возвращает `DolibarrConnection` как handle для прикладного слоя.

Особенности:
1. `destroy/cleanup` — no-op (нет пулового физического state).
2. `add/removeConnectionEventListener` — no-op.
3. `getXAResource()` бросает ошибку:
   - XA не поддерживается в локальном режиме.
4. `getLocalTransaction()` возвращает no-op транзакцию.

Смысл:
1. Модуль честно показывает ограничения локального JCA-style варианта.

---

## 4.6 `DolibarrConnection`
Файл: `dolibarr-audit-ra/.../DolibarrConnection.java`

Роль:
1. Легковесный connection-handle с параметрами подключения.
2. Создает `DolibarrInteraction` через `createInteraction()`.
3. Проверяет состояние `closed`.

Почему полезно:
1. Изоляция жизненного цикла interaction от бизнес-сервиса.

---

## 4.7 `DolibarrInteraction`
Файл: `dolibarr-audit-ra/.../DolibarrInteraction.java`

Это ядро модуля: именно здесь формируется HTTP-запрос к Dolibarr.

Ключевые функции:
1. `execute(String interactionName, Map<String, Object> payload)`:
   - резолвит endpoint;
   - строит HTTP request;
   - отправляет через `HttpClient`.
2. `resolveEndpoint(...)`:
   - поддерживает `status`, абсолютный URL, относительные пути, dotted-name формат.
3. Для `/status` делает `GET`.
4. Для остальных endpoint делает `POST` с JSON body.
5. Возвращает `ExecutionResult`:
   - `accepted`
   - `statusCode`
   - `endpoint`
   - `error`

Особенности сериализации:
1. Есть собственный `toJson(...)` (Map/Iterable/String/Number/Boolean/BigDecimal).
2. При non-2xx ответе тело обрезается до 600 символов для логов.

Обработка ошибок:
1. `IOException/InterruptedException` не пробрасываются наружу как checked.
2. Возвращается `ExecutionResult(false, 0, endpoint, message)`.

---

## 4.8 `DolibarrAuditException`
Файл: `dolibarr-audit-ra/.../DolibarrAuditException.java`

Роль:
1. Runtime-исключение адаптера.
2. Используется для ошибок создания/закрытого connection и проблем выделения connection.

---

## 5) Полная цепочка вызова (call flow)
Ниже путь одного audit-события:

1. Async-сервис завершил запрос terminal-статусом.
2. Вызвал `eisOperationAuditService.registerOperationResult(result)`.
3. `DolibarrOperationAuditJcaService` проверил `auditEnabled`.
4. Взял connection:
   - `connectionFactory.getConnection()`.
5. Создал interaction:
   - `connection.createInteraction()`.
6. Сформировал payload из `EisOperationResult`.
7. Вызвал:
   - `interaction.execute(interactionName, payload)`.
8. По результату:
   - лог `info` при успехе;
   - лог `warn` при negative/failed.
9. Закрыл interaction и connection.

---

## 6) Конфигурация, от которой зависит работа модуля
Источник: `src/main/resources/application.properties`

Ключевые параметры:
1. `eis.dolibarr.url=${EIS_DOLIBARR_URL:http://localhost:8081}`
2. `eis.dolibarr.api-key=${EIS_DOLIBARR_API_KEY:test_api_key}`
3. `eis.dolibarr.connect-timeout-ms=${EIS_DOLIBARR_CONNECT_TIMEOUT_MS:3000}`
4. `eis.dolibarr.read-timeout-ms=${EIS_DOLIBARR_READ_TIMEOUT_MS:5000}`
5. `eis.dolibarr.audit.enabled=${EIS_DOLIBARR_AUDIT_ENABLED:false}`
6. `eis.dolibarr.audit.interaction-name=${EIS_DOLIBARR_AUDIT_INTERACTION:status}`

Практический смысл:
1. Если `audit.enabled=false` — модуль физически не отправляет ничего.
2. При `interaction-name=status` отправка идет в health endpoint (`/api/index.php/status`).
3. Чтобы писать реальный бизнес-аудит, нужно выбрать endpoint, который создает объект в Dolibarr.

---

## 7) Что можно посмотреть руками, чтобы доказать работу
1. Логи backend:
   - `Dolibarr audit sent via local connector ...`
   - `Dolibarr audit response is negative ...`
   - `Dolibarr audit send failed ...`
2. Прямой вызов Dolibarr API:
   - `GET /api/index.php/status` (проверка доступности и ключа).
3. Запуск async-запроса в BLPS:
   - после terminal статуса должен появиться лог аудита.

---

## 8) Ограничения текущей реализации
1. Это local JCA-style adapter, а не полноценный серверный RA с JNDI deployment.
2. XA-транзакции не поддерживаются (`getXAResource()` возвращает ошибку).
3. В текущем demo режиме (`interaction-name=status`) аудит не создает сущность в UI Dolibarr.
4. JSON сериализация реализована вручную (достаточно для текущего payload, но не универсальна как ObjectMapper).

---

## 9) Почему это все равно полезно для лабораторной
1. Показан connector-подход и разделение слоев (business vs integration).
2. Есть явный жизненный цикл соединения и interaction.
3. Есть централизованная обработка ошибок/логов.
4. Архитектура готова к переходу на “полноценный” RA сценарий при необходимости.
