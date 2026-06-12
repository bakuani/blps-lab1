# Пояснение по интеграции Camunda для защиты

## 1. Как приложение взаимодействует с Camunda

### Общая идея интеграции

В этой лабораторной Camunda работает как standalone BPMS-сервис, а наше приложение на Spring Boot/WildFly не встраивает BPM-движок внутрь себя. Приложение общается с Camunda по REST API:

- приложение стартует экземпляры BPMN-процессов через Camunda REST;
- Camunda хранит состояние процесса, выполняет маршрутизацию, условия, таймеры и retry-логику external tasks;
- бизнес-операции выполняются в Java-приложении как external task worker;
- результат выполнения Java-кода возвращается обратно в Camunda как process variables;
- на основании этих variables Camunda выбирает следующие ветки процесса.

То есть Camunda управляет порядком и состоянием бизнес-процесса, а Java-приложение остаётся исполнителем конкретных технических операций: JPA, транзакции, биллинг, уведомления, Dolibarr/EIS.

### Runtime-схема

Основные компоненты:

- Camunda Run: `docker-compose.yml`, сервис `camunda`, image `camunda/camunda-bpm-platform:run-7.24.0`.
- База Camunda: `docker-compose.yml`, сервис `camunda-db`.
- BLPS-приложение: WAR на WildFly.
- BPMN-модели: `src/main/resources/processes/*.bpmn`.
- Деплой BPMN в Camunda: `scripts/deploy-camunda-processes.sh`.

Локально Camunda доступна как:

```text
http://127.0.0.1:8082/engine-rest
```

На helios приложение ходит к Camunda через reverse SSH tunnel:

```text
http://127.0.0.1:18080/engine-rest
```

Это задаётся в `scripts/helios-start.sh` через:

```sh
export CAMUNDA_BASE_URL="${CAMUNDA_BASE_URL:-http://127.0.0.1:18080/engine-rest}"
```

А в обычной конфигурации Spring Boot значение берётся из `src/main/resources/application.properties`:

```properties
app.camunda.base-url=${CAMUNDA_BASE_URL:http://127.0.0.1:8082/engine-rest}
app.camunda.worker-id=${CAMUNDA_WORKER_ID:blps-local-worker}
app.camunda.lock-duration-ms=${CAMUNDA_LOCK_DURATION_MS:30000}
app.camunda.poll-interval-ms=${CAMUNDA_POLL_INTERVAL_MS:2000}
app.camunda.max-tasks=${CAMUNDA_MAX_TASKS:10}
app.camunda.default-retries=${CAMUNDA_DEFAULT_RETRIES:3}
app.camunda.retry-timeout-ms=${CAMUNDA_RETRY_TIMEOUT_MS:60000}
```

### Главные файлы интеграции

#### `CamundaProperties`

Файл:

```text
src/main/java/ru/urasha/callmeani/blps/config/CamundaProperties.java
```

Назначение:

- хранит настройки `app.camunda.*`;
- задаёт base URL Camunda REST API;
- задаёт параметры external task worker: worker id, lock duration, max tasks, retry timeout.

Это точка, через которую приложение получает настройки подключения к standalone Camunda.

#### `CamundaConfig`

Файл:

```text
src/main/java/ru/urasha/callmeani/blps/config/CamundaConfig.java
```

Назначение:

- создаёт Spring `RestClient` для Camunda;
- base URL берётся из `CamundaProperties`;
- bean назван `camundaEngineRestClient`, чтобы не конфликтовать с wrapper-классом `CamundaRestClient`.

Смысл:

```java
@Bean(name = "camundaEngineRestClient")
public RestClient camundaEngineRestClient(CamundaProperties properties) {
    return RestClient.builder()
        .baseUrl(properties.getBaseUrl())
        .build();
}
```

#### `CamundaRestClient`

Файл:

```text
src/main/java/ru/urasha/callmeani/blps/service/camunda/CamundaRestClient.java
```

Это наш wrapper над Camunda REST API.

Главные методы:

- `startProcess(...)` - стартует процесс по process definition key:

```text
POST /process-definition/key/{key}/start
```

- `completeFirstTask(...)` - находит первую user task процесса и завершает её:

```text
GET  /task?processInstanceId=...
POST /task/{id}/complete
```

- `fetchAndLock(...)` - забирает external tasks:

```text
POST /external-task/fetchAndLock
```

- `completeExternalTask(...)` - сообщает Camunda, что задача успешно выполнена:

```text
POST /external-task/{id}/complete
```

- `handleFailure(...)` - сообщает Camunda об ошибке external task и передаёт retry count:

```text
POST /external-task/{id}/failure
```

На защите можно сказать так:

> Мы не вызываем Java Delegate внутри Camunda. Вместо этого используем external task pattern: Camunda создаёт задачи с topic, а наше приложение периодически забирает их по REST, выполняет бизнес-действие и завершает задачу обратно через REST API.

#### `CamundaProcessConstants`

Файл:

```text
src/main/java/ru/urasha/callmeani/blps/service/camunda/CamundaProcessConstants.java
```

Здесь собраны process definition keys и topic names. Это связующее место между BPMN XML и Java-кодом.

Process keys:

```java
TariffChangeProcess
FeatureDisableProcess
MonthlyFeeChargeProcess
```

External task topics:

```java
validate-tariff-change
charge-switch-fee
charge-new-monthly-fee
update-subscriber-tariff
validate-feature-disable
disable-feature-billing
update-subscriber-feature
create-monthly-fee-requests
create-dolibarr-invoice
charge-monthly-fee
sync-dolibarr-invoice
send-notification
publish-eis-audit
```

В BPMN эти же строки стоят в `camunda:topic`, а в Java по ним выбирается обработчик.

#### `CamundaExternalTaskWorker`

Файл:

```text
src/main/java/ru/urasha/callmeani/blps/service/camunda/CamundaExternalTaskWorker.java
```

Это scheduled worker.

Что он делает:

1. Раз в `app.camunda.poll-interval-ms` миллисекунд собирает список всех topic из зарегистрированных `CamundaExternalTaskHandler`.
2. Вызывает `fetchAndLock` в Camunda.
3. Для каждой полученной external task находит Java-handler по topic.
4. Выполняет handler.
5. Если всё успешно, вызывает `completeExternalTask`.
6. Если произошла ошибка, уменьшает число retries и вызывает `handleFailure`.

Важная аннотация:

```java
@ConditionalOnProperty(name = "app.camunda.enabled", havingValue = "true", matchIfMissing = true)
```

Это позволяет включать/выключать worker через `CAMUNDA_ENABLED`.

#### `CamundaExternalTaskHandler`

Файл:

```text
src/main/java/ru/urasha/callmeani/blps/service/camunda/CamundaExternalTaskHandler.java
```

Это интерфейс для обработчиков external tasks:

- `topics()` возвращает список topic, которые поддерживает handler;
- `handle(...)` выполняет задачу и возвращает process variables;
- `handleFailure(...)` позволяет дополнительно обновить доменную заявку при ошибке.

#### `CamundaBusinessTaskHandler`

Файл:

```text
src/main/java/ru/urasha/callmeani/blps/service/camunda/CamundaBusinessTaskHandler.java
```

Это тонкий обработчик-маршрутизатор для business topics Camunda.

Он подписан на все основные topics:

- проверка смены тарифа;
- списание платы за смену тарифа;
- списание абонентской платы нового тарифа;
- обновление тарифа абонента;
- проверка отключения услуги;
- биллинг отключения услуги;
- обновление услуги абонента;
- создание заявок на ежемесячное списание;
- создание счёта Dolibarr;
- списание абонентской платы;
- синхронизация счёта Dolibarr;
- отправка уведомления;
- публикация EIS-аудита.

Внутри `handle(...)` используется `switch` по `task.topicName()`. Это и есть связь между BPMN service task и Java-кодом. Сам handler не содержит доменную бизнес-логику, а делегирует её в сервисы соответствующих доменных областей:

```java
case CamundaProcessConstants.VALIDATE_TARIFF_CHANGE -> tariffChangeTaskService.validateTariffChange(task);
case CamundaProcessConstants.CHARGE_SWITCH_FEE -> tariffChangeTaskService.chargeSwitchFee(task);
case CamundaProcessConstants.UPDATE_SUBSCRIBER_TARIFF -> tariffChangeTaskService.updateSubscriberTariff(task);
```

Основные доменные сервисы для Camunda-задач:

- `src/main/java/ru/urasha/callmeani/blps/service/tariff/camunda/TariffChangeCamundaTaskService.java`;
- `src/main/java/ru/urasha/callmeani/blps/service/tariff/camunda/impl/TariffChangeCamundaTaskServiceImpl.java`;
- `src/main/java/ru/urasha/callmeani/blps/service/feature/camunda/FeatureDisableCamundaTaskService.java`;
- `src/main/java/ru/urasha/callmeani/blps/service/feature/camunda/impl/FeatureDisableCamundaTaskServiceImpl.java`;
- `src/main/java/ru/urasha/callmeani/blps/service/billing/camunda/MonthlyFeeCamundaTaskService.java`;
- `src/main/java/ru/urasha/callmeani/blps/service/billing/camunda/impl/MonthlyFeeCamundaTaskServiceImpl.java`.

Транзакции не перенесены в Camunda. Они остались в приложении, что соответствует заданию: распределённые транзакции переносить на BPM-движок не требуется.

Для бизнес-действий используются обычные Spring transaction boundaries на публичных методах доменных сервисов:

```java
@Override
@Transactional
public Map<String, CamundaVariable> validateTariffChange(LockedExternalTask task) {
    ...
}
```

В каждой реализации напрямую инжектится только свой репозиторий, например `TariffChangeRequestRepository` в тарифном сервисе. Все остальные операции с БД идут через сервисы соседних доменных областей: `SubscriberService`, `TariffService`, `BillingService`, `NotificationService`, `EisValidationService`, `EisOperationAuditService`.

На защите это можно объяснить так:

> Camunda управляет последовательностью шагов, условиями и retry. Но сами изменения в PostgreSQL, биллинге, уведомлениях и EIS выполняются внутри доменных Spring-сервисов в обычных транзакциях приложения. Handler только связывает BPMN topic с нужным сервисным методом.

### Как стартуют процессы из API

#### Смена тарифа

Файл:

```text
src/main/java/ru/urasha/callmeani/blps/service/tariff/async/TariffChangeAsyncService.java
```

Поток:

1. REST controller вызывает `submitTariffChange(...)`.
2. Сервис создаёт `TariffChangeRequest` со статусом `PENDING`.
3. Сервис сохраняет заявку в PostgreSQL.
4. Сервис вызывает Camunda:

```java
camundaRestClient.startProcess(
    CamundaProcessConstants.TARIFF_CHANGE_PROCESS,
    "tariff-change-" + request.getId(),
    variables
);
```

5. В доменной заявке сохраняется `processInstanceId`.
6. Первая user task завершается автоматически через `completeFirstTask(...)`.
7. Дальше процесс идёт по external tasks.

Почему user task завершается автоматически:

- в BPMN есть user task и form fields, чтобы показать Camunda form UI;
- в рабочем API-сценарии пользователь уже отправил форму через BLPS REST API;
- поэтому приложение передаёт те же variables в Camunda и закрывает первый form step.

#### Отключение услуги

Файл:

```text
src/main/java/ru/urasha/callmeani/blps/service/feature/async/FeatureDisableAsyncService.java
```

Схема такая же:

1. Создаётся `FeatureDisableRequest`.
2. Запускается `FeatureDisableProcess`.
3. Business key имеет вид:

```text
feature-disable-{requestId}
```

4. Первая user task закрывается автоматически.
5. Camunda дальше управляет проверкой, биллингом, обновлением услуги, уведомлением и аудитом.

#### Ежемесячное списание

Файл:

```text
src/main/java/ru/urasha/callmeani/blps/service/billing/async/MonthlyFeeChargeAsyncService.java
```

Здесь есть две части:

1. `MonthlyFeeCycleProcess` запускается Camunda timer event.
2. Timer создаёт external task `create-monthly-fee-requests`.
3. Java worker выполняет `createMonthlyFeeRequests()`.
4. Для каждого абонента создаётся `MonthlyFeeChargeRequest`.
5. Для каждой заявки стартует `MonthlyFeeChargeProcess`.

Business key:

```text
monthly-fee-{requestId}
```

Отдельный старый Spring scheduler для ежемесячного списания выключен по умолчанию:

```properties
app.scheduler.monthly-fee-enabled=${MONTHLY_FEE_SCHEDULER_ENABLED:false}
```

То есть периодика демонстрируется именно через Camunda timer, а не через Spring cron.

### BPMN-файлы

Файлы лежат здесь:

```text
src/main/resources/processes/
```

Текущие BPMN-файлы:

```text
tariff-change-process.bpmn
feature-disable-process.bpmn
monthly-fee-cycle-process.bpmn
monthly-fee-charge-process.bpmn
```

В каждом процессе есть:

- `isExecutable="true"` - процесс исполняемый;
- `camunda:historyTimeToLive="30"` - требуется Camunda для history cleanup;
- `bpmndi:BPMNDiagram` - координаты диаграммы, чтобы Cockpit мог отрисовать процесс;
- `camunda:type="external"` и `camunda:topic="..."` у service tasks;
- `camunda:candidateGroups="SUBSCRIBER,OPERATOR,ADMIN"` у user tasks;
- form fields в user tasks;
- conditions на gateway через process variables.

Пример service task:

```xml
<bpmn:serviceTask
    id="Task_ValidateTariffChange"
    name="Проверить смену тарифа"
    camunda:type="external"
    camunda:topic="validate-tariff-change">
</bpmn:serviceTask>
```

Пример user task с Camunda form:

```xml
<bpmn:userTask
    id="UserTask_TariffChangeForm"
    name="Подтвердить смену тарифа"
    camunda:candidateGroups="SUBSCRIBER,OPERATOR,ADMIN">
  <bpmn:extensionElements>
    <camunda:formData>
      <camunda:formField id="subscriberId" label="Subscriber id" type="long" />
      <camunda:formField id="targetTariffId" label="Target tariff id" type="long" />
      <camunda:formField id="options" label="Selected tariff options" type="string" />
    </camunda:formData>
  </bpmn:extensionElements>
</bpmn:userTask>
```

Пример timer event:

```xml
<bpmn:timeCycle xsi:type="bpmn:tFormalExpression">R/PT30S</bpmn:timeCycle>
```

### Деплой BPMN в Camunda

Скрипт:

```text
scripts/deploy-camunda-processes.sh
```

Что он делает:

1. Берёт Camunda REST URL из `CAMUNDA_BASE_URL`.
2. По умолчанию ищет все `.bpmn` файлы в `src/main/resources/processes`.
3. Отправляет их multipart POST-запросом в Camunda:

```text
POST /deployment/create
```

4. Использует:

```text
enable-duplicate-filtering=true
deploy-changed-only=true
```

Это значит: если BPMN не изменился, Camunda не будет создавать лишнюю новую версию процесса.

Команда:

```sh
./scripts/deploy-camunda-processes.sh
```

После деплоя в Cockpit видны process definitions.

## 2. Как мы могли самостоятельно сделать BPMN через bpmn.io и интегрировать с Camunda

На защите не нужно говорить, что BPMN кто-то сгенерировал. Нормальный ожидаемый процесс разработки такой:

1. Сначала мы выделили бизнес-процессы из старой статической логики.
2. Затем нарисовали их в BPMN.io.
3. После этого экспортировали BPMN 2.0 XML.
4. Добавили Camunda-specific свойства: process id, external task topics, candidate groups, form fields, TTL.
5. Положили `.bpmn` файлы в проект.
6. Реализовали Java external task worker.
7. Задеплоили BPMN в standalone Camunda через REST API.
8. Проверили процессы в Cockpit/Tasklist.

### Шаг 1. Выделить процессы

Мы разбили старую бизнес-логику на 4 процесса:

1. Смена тарифа: `TariffChangeProcess`.
2. Отключение услуги: `FeatureDisableProcess`.
3. Периодический цикл абонентской платы: `MonthlyFeeCycleProcess`.
4. Разовое списание абонентской платы: `MonthlyFeeChargeProcess`.

Такое разделение удобно объяснять:

- tariff/feature - пользовательские операции;
- monthly cycle - периодический процесс;
- monthly charge - процесс обработки одной конкретной заявки на списание.

### Шаг 2. Нарисовать диаграмму в BPMN.io

В BPMN.io рисуются стандартные элементы:

- start event;
- user task;
- service task;
- exclusive gateway;
- send task;
- timer start event;
- end event;
- sequence flows.

Практический момент: онлайн bpmn.io в первую очередь нужен для рисования BPMN 2.0-диаграммы и сохранения layout/координат. Если в выбранной онлайн-версии нет панели Camunda-specific properties, то Camunda-атрибуты (`camunda:type`, `camunda:topic`, `camunda:candidateGroups`, `camunda:formData`, `camunda:historyTimeToLive`) добавляются после экспорта вручную в XML. Это нормальный вариант: графическая часть делается в bpmn.io, а технические extension properties дописываются в `.bpmn` как XML.

Например для смены тарифа:

```text
Start
 -> User Task "Подтвердить смену тарифа"
 -> Service Task "Проверить смену тарифа"
 -> Gateway "Можно сменить?"
 -> Gateway "Есть плата за смену?"
 -> Service Task "Списать плату за смену"
 -> Service Task "Списать абонентскую плату тарифа"
 -> Service Task "Обновить тариф абонента"
 -> Send Task "Отправить уведомление"
 -> Service Task "Записать аудит EIS"
 -> End
```

Для отрицательной ветки gateway делается flow сразу в audit/end, чтобы отказ тоже фиксировался.

### Шаг 3. Настроить process id

У каждого процесса должен быть стабильный id. Эти id используются Java-кодом при старте процесса:

```text
TariffChangeProcess
FeatureDisableProcess
MonthlyFeeCycleProcess
MonthlyFeeChargeProcess
```

Если в BPMN поменять id, то нужно поменять и `CamundaProcessConstants`, иначе Java будет стартовать несуществующий process definition key.

### Шаг 4. Сделать процесс исполняемым

В BPMN XML у процесса должно быть:

```xml
isExecutable="true"
```

Без этого Camunda может видеть модель как схему, но не как исполняемый процесс.

### Шаг 5. Добавить Camunda TTL

В Camunda 7.24 executable process должен иметь history TTL:

```xml
camunda:historyTimeToLive="30"
```

Иначе при деплое будет ошибка:

```text
History Time To Live (TTL) cannot be null
```

TTL можно задать двумя способами:

- прямо в BPMN-модели, как у нас;
- глобально в настройках Camunda engine.

Для лабораторной лучше задавать прямо в BPMN, потому что модель становится самодостаточной.

### Шаг 6. Настроить service tasks как external tasks

Для каждого service task нужно указать Camunda external task properties. Если редактор поддерживает Camunda properties panel, это можно заполнить через UI. Если нет, после экспорта BPMN XML добавляется вручную:

```xml
camunda:type="external"
camunda:topic="validate-tariff-change"
```

Почему external task:

- Camunda standalone не должна знать классы нашего WAR;
- Java-код находится в WildFly-приложении;
- Camunda только создаёт задачу с topic;
- наше приложение забирает задачу через REST и выполняет её.

Если использовать Java Delegate, пришлось бы деплоить Java-классы рядом с Camunda engine. Для standalone Camunda и отдельного WildFly это менее удобно.

### Шаг 7. Настроить user tasks и Camunda forms

Для user task можно добавить candidate groups:

```xml
camunda:candidateGroups="SUBSCRIBER,OPERATOR,ADMIN"
```

И form fields:

```xml
<camunda:formData>
  <camunda:formField id="subscriberId" label="Subscriber id" type="long" />
  <camunda:formField id="targetTariffId" label="Target tariff id" type="long" />
</camunda:formData>
```

Это нужно для демонстрации UI Camunda Tasklist: Camunda сама показывает форму на основе form fields.

В нашем рабочем REST-сценарии пользователь отправляет данные через BLPS API, поэтому приложение автоматически закрывает первую user task. Но для защиты можно вручную стартовать процесс в Tasklist и показать, что форма генерируется Camunda.

### Шаг 8. Настроить gateways

Gateway должен опираться на process variables, которые возвращает Java-handler.

Например handler `validateTariffChange(...)` возвращает:

```text
canChangeTariff = true/false
hasSwitchFee = true/false
```

А в BPMN на sequence flow ставится condition:

```xml
${canChangeTariff}
${!canChangeTariff}
${hasSwitchFee}
${!hasSwitchFee}
```

Это важный момент для защиты:

> Решение о переходе по ветке принимает не Java-код напрямую. Java-код возвращает переменные, а Camunda по условиям BPMN выбирает следующую ветку процесса.

### Шаг 9. Настроить timer

Периодическая задача реализуется BPMN timer start event.

У нас:

```xml
R/PT30S
```

Это означает повторение каждые 30 секунд. Для реального production-сценария период был бы больше, но для лабораторной 30 секунд удобно демонстрировать.

Spring scheduler для monthly fee отключён по умолчанию, чтобы периодика была именно в Camunda.

### Шаг 10. Сохранить BPMN с диаграммой

Важно не только наличие process XML, но и наличие diagram interchange-разметки:

```xml
<bpmndi:BPMNDiagram>
<bpmndi:BPMNShape>
<bpmndi:BPMNEdge>
```

Если этих элементов нет, Camunda сможет выполнить процесс, но Cockpit покажет:

```text
Could not render diagram: no diagram to display
```

Поэтому при экспорте из BPMN.io нужно сохранять полноценный `.bpmn` файл с layout/координатами диаграммы.

### Шаг 11. Положить BPMN в проект

Файлы кладутся в:

```text
src/main/resources/processes/
```

Мы используем отдельный файл на каждый процесс:

```text
tariff-change-process.bpmn
feature-disable-process.bpmn
monthly-fee-cycle-process.bpmn
monthly-fee-charge-process.bpmn
```

Так проще показывать процессы по отдельности и проще поддерживать диаграммы.

### Шаг 12. Связать BPMN с Java-кодом

После создания BPMN нужно проверить соответствие:

| BPMN | Java |
| --- | --- |
| process id | `CamundaProcessConstants.*_PROCESS` |
| `camunda:topic` | `CamundaProcessConstants.*` topic |
| process variables | `CamundaVariable` в async service/handler |
| gateway conditions | variables, которые возвращает handler |
| user task form fields | variables, которые передаются при старте/complete |

Если topic в BPMN и topic в Java отличаются хотя бы одним символом, worker не будет выполнять эту задачу.

### Шаг 13. Задеплоить BPMN в Camunda

Команда:

```sh
./scripts/deploy-camunda-processes.sh
```

Что происходит внутри:

```text
POST http://127.0.0.1:8082/engine-rest/deployment/create
```

Скрипт отправляет все `.bpmn` файлы из `src/main/resources/processes`.

Можно было бы сделать это руками через `curl`:

```sh
curl -fsS \
  -X POST "http://127.0.0.1:8082/engine-rest/deployment/create" \
  -F "deployment-name=blps-camunda-processes" \
  -F "enable-duplicate-filtering=true" \
  -F "deploy-changed-only=true" \
  -F "tariff-change-process.bpmn=@src/main/resources/processes/tariff-change-process.bpmn;type=text/xml" \
  -F "feature-disable-process.bpmn=@src/main/resources/processes/feature-disable-process.bpmn;type=text/xml" \
  -F "monthly-fee-cycle-process.bpmn=@src/main/resources/processes/monthly-fee-cycle-process.bpmn;type=text/xml" \
  -F "monthly-fee-charge-process.bpmn=@src/main/resources/processes/monthly-fee-charge-process.bpmn;type=text/xml"
```

### Шаг 14. Проверить в Camunda UI

После деплоя:

```text
http://127.0.0.1:8082/camunda
```

В Cockpit нужно показать:

- process definitions;
- BPMN-диаграммы;
- process instances;
- history/activity instance;
- external tasks;
- incidents, если специально спровоцировать ошибку.

В Tasklist нужно показать:

- старт процесса вручную;
- Camunda-generated form из user task.

### Шаг 15. Проверить через Insomnia

В `docs/insomnia-export.json` есть папка:

```text
Lab4 - Camunda BPMS Demo
```

Там есть запросы:

- проверить версию Camunda;
- посмотреть deployments;
- посмотреть process definitions;
- получить BPMN XML;
- стартовать тарифный процесс через BLPS API;
- проверить Camunda history по business key;
- посмотреть activity history;
- посмотреть external tasks;
- посмотреть incidents;
- посмотреть timer jobs.

Для защиты удобно держать рядом UI Camunda и Insomnia:

1. В Insomnia отправляем BLPS-запрос.
2. В Camunda Cockpit показываем process instance.
3. В BLPS API показываем итоговый статус заявки.

## Что важно сказать на защите

Короткая формулировка:

> Мы вынесли управление бизнес-процессом в Camunda. Раньше порядок шагов был зашит в Java-сервисах, теперь порядок шагов, условия, таймер и retry external tasks описаны в BPMN. Java-приложение стартует процессы через Camunda REST API и работает как external task worker: забирает задачи по topic, выполняет бизнес-операции в своих Spring-сервисах и возвращает результат обратно в Camunda через variables.

Про роли:

> Разграничение доступа осталось на уровне Spring Security для REST API. В BPMN user tasks дополнительно имеют candidate groups `SUBSCRIBER`, `OPERATOR`, `ADMIN`, чтобы это было видно в Camunda Tasklist/Form UI.

Про транзакции:

> Транзакции не переносились в Camunda, потому что по заданию распределённые транзакции переносить не требуется. Camunda управляет процессом, а каждое бизнес-действие выполняется в приложении внутри Spring transaction boundary.

Про асинхронность:

> Асинхронная обработка реализована через external tasks. Camunda создаёт external task, приложение периодически делает fetch-and-lock, выполняет задачу и завершает её через REST API.

Про периодику:

> Периодическая задача ежемесячного списания перенесена в BPMN timer event. Spring scheduler для monthly fee выключен, чтобы источником периодического запуска был Camunda process engine.

Про формы:

> Формы описаны в BPMN через Camunda form fields. В рабочем REST-сценарии приложение автоматически закрывает первую user task, потому что данные уже пришли из BLPS API. Но в Tasklist можно вручную стартовать процесс и увидеть форму, сгенерированную Camunda.

Про BPMN.io:

> Диаграммы были смоделированы как BPMN 2.0: start events, user tasks, service tasks, gateways, timer event, sequence flows. После экспорта BPMN XML мы добавили Camunda extension properties: external task topics, candidate groups, form fields и history TTL. Затем BPMN-файлы были задеплоены в standalone Camunda через REST API.

## Частые вопросы преподавателя

### Почему Camunda standalone, а не embedded?

Потому что в задании требуется standalone-сервис. Поэтому приложение не содержит Camunda engine внутри WAR, а общается с отдельным Camunda Run по REST API.

### Почему service tasks сделаны external tasks?

Потому что бизнес-код находится в WildFly-приложении. External task pattern позволяет Camunda не зависеть от классов приложения и не загружать Java delegates в engine.

### Где Camunda реально управляет процессом?

В BPMN:

- gateway conditions;
- timer start event;
- последовательность service tasks;
- external task lifecycle;
- retries/incidents;
- history/activity tracking.

Java не решает, какой BPMN-шаг будет следующим. Java только выполняет текущий topic и возвращает variables.

### Почему в UI иногда external tasks пустые?

Потому что worker быстро забирает и завершает задачи. Это хороший признак. Чтобы показать pending external tasks, можно временно запустить приложение с выключенным worker:

```sh
CAMUNDA_ENABLED=false
```

Тогда процесс стартует, но external tasks останутся в Camunda до включения worker.

### Почему старые instances могли быть без диаграмм?

Если процесс был задеплоен без BPMN DI-разметки, Camunda могла выполнять его, но Cockpit не мог нарисовать диаграмму. После добавления `bpmndi:BPMNDiagram` новые версии процесса отображаются в UI. Старые instances остаются привязанными к старой версии process definition.
