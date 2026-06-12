# Сценарий защиты Camunda-интеграции

Документ рассчитан на живую защиту: сначала показываем работающую систему через Insomnia и UI, потом объясняем реализацию в коде.

## 0. Подготовка перед защитой

### Что должно быть запущено

Перед началом показа проверь, что подняты все части стенда:

```sh
docker compose up -d postgres rabbitmq dolibarr-db dolibarr camunda-db camunda
./scripts/deploy-camunda-processes.sh
./scripts/open-tunnel.sh
./scripts/helios-start.sh
```

WAR должен быть задеплоен в WildFly.

Основные URL:

```text
BLPS API через helios tunnel: http://127.0.0.1:8180
Camunda UI:                     http://127.0.0.1:8082/camunda
Camunda REST:                   http://127.0.0.1:8082/engine-rest
Dolibarr:                       http://127.0.0.1:8081
```

Если открываешь Camunda UI, стандартный логин обычно:

```text
demo / demo
```

### Что проверить в Insomnia environment

В `docs/insomnia-export.json` уже есть нужные папки:

```text
Auth
Management - Test Data Init
Dolibarr Diagnostics
Lab4 - Camunda BPMS Demo
```

Перед защитой в environment должны быть такие значения:

```text
base_url = http://localhost:8180
camunda_base = http://127.0.0.1:8082/engine-rest
camunda_deployment_name = blps-camunda-processes
subscriber_id = 1
tariff_id = 2
feature_id = 1
```

Для Dolibarr:

```text
dolibarr_base = http://localhost:8081
```

Если API требует токен, сначала выполни:

```text
Auth -> Login as SUBSCRIBER
```

Из ответа скопируй `accessToken` в переменную:

```text
active_token
```

Если нужно создавать тестовые данные, залогинься как admin:

```text
Auth -> Login as ADMIN
```

И скопируй admin token в `active_token`.

## 1. Часть защиты: показываем работу системы

### 1.1. Начальная фраза

Сказать:

> Сначала я покажу работу системы как пользователь и как оператор стенда. Основная цель этой лабораторной - доказать, что бизнес-процессы теперь управляются Camunda: процессы задеплоены в Camunda, REST API приложения стартует process instances, worker обрабатывает external tasks, а таймер ежемесячного списания находится в BPMN.

После этого переходишь к Insomnia.

## 2. Insomnia: базовая проверка приложения

### 2.1. Авторизация

Открыть папку:

```text
Auth
```

Выполнить:

```text
Login as SUBSCRIBER
```

Ожидаемо:

- HTTP `200`;
- в ответе есть `accessToken`;
- token type `Bearer`.

Что сказать:

> Сначала получаю JWT-токен обычного абонента. Дальше все пользовательские операции выполняются от его имени, то есть разграничение доступа по ролям остаётся на уровне Spring Security.

Скопировать `accessToken` в:

```text
active_token
```

### 2.2. Если база пустая: подготовить тестовые данные

Если данные уже есть, этот блок можно не показывать полностью. Можно сказать:

> Тестовые данные уже созданы: есть абонент, тарифы, категории и подключенные услуги.

Если база пустая:

1. Выполнить `Auth -> Login as ADMIN`.
2. Скопировать token в `active_token`.
3. Пройти папку:

```text
Management - Test Data Init
```

Запросы:

```text
1. Create Base Tariff Category
2. Create Premium Tariff Category
3. Create Smart Tariff
4. Create My Tariff
5. Create Tariff Option Minutes
6. Create Tariff Option Traffic
7. Create Safety Feature Category
8. Create Entertainment Feature Category
9. Create Blocker Feature
10. Create Music Feature
11. Create Subscriber
12. Connect Blocker to Subscriber
13. Connect Music to Subscriber
```

Что сказать:

> Эти запросы создают минимальный набор данных для демонстрации: абонента, два тарифа, дополнительные опции и активные услуги. Они не относятся напрямую к Camunda, но нужны, чтобы бизнес-процессы могли пройти реальные проверки.

После подготовки данных снова выполнить:

```text
Auth -> Login as SUBSCRIBER
```

И вернуть subscriber token в `active_token`.

## 3. Insomnia + Camunda UI: показываем, что Camunda работает

Открыть папку:

```text
Lab4 - Camunda BPMS Demo
```

### 3.1. Проверить Camunda engine

Выполнить:

```text
1) Camunda engine version (expect 200)
```

Ожидаемо:

- HTTP `200`;
- версия Camunda, например `7.24.0`.

Что сказать:

> Это отдельный standalone Camunda Run. Наше приложение не содержит embedded engine, оно общается с этим сервисом по REST API.

### 3.2. Проверить deployment

Выполнить:

```text
2) Deployments (expect blps-camunda-processes)
```

Ожидаемо:

- в ответе есть deployment с именем `blps-camunda-processes`.

Что сказать:

> BPMN-файлы не лежат просто как ресурс приложения. Они деплоятся в Camunda как process definitions через REST endpoint `/deployment/create`.

### 3.3. Проверить process definitions

Выполнить:

```text
3) Process definitions (expect 4 BPMN processes)
```

Ожидаемо увидеть 4 процесса:

```text
TariffChangeProcess
FeatureDisableProcess
MonthlyFeeCycleProcess
MonthlyFeeChargeProcess
```

Что сказать:

> Мы разделили старую бизнес-логику на четыре BPMN-процесса: смена тарифа, отключение услуги, периодический цикл абонентской платы и обработка конкретного списания.

### 3.4. Открыть Camunda Cockpit

Открыть:

```text
http://127.0.0.1:8082/camunda
```

Перейти:

```text
Cockpit -> Processes
```

Показать:

- `TariffChangeProcess`;
- `FeatureDisableProcess`;
- `MonthlyFeeCycleProcess`;
- `MonthlyFeeChargeProcess`;
- диаграммы процессов.

Что сказать:

> Здесь видно, что процессы загружены именно в Camunda. Диаграммы отображаются, потому что BPMN-файлы содержат не только исполняемую модель, но и BPMN DI-разметку с координатами элементов.

Если видны старые demo-процессы Camunda (`Invoice Receipt`, `Review Invoice`), сказать:

> Это стандартные demo-процессы Camunda Run. К нашей лабораторной относятся процессы с ключами `TariffChangeProcess`, `FeatureDisableProcess`, `MonthlyFeeCycleProcess`, `MonthlyFeeChargeProcess`.

## 4. Демонстрация процесса смены тарифа

### 4.1. Перед запуском открыть диаграмму

В Cockpit открыть:

```text
TariffChangeProcess
```

Показать элементы:

- user task подтверждения;
- service task проверки;
- gateway `можно сменить тариф?`;
- gateway `есть плата за смену?`;
- service tasks списания;
- обновление тарифа;
- уведомление;
- аудит EIS.

Что сказать:

> Этот процесс показывает, что порядок шагов больше не зашит целиком в Java-метод. Порядок действий, условия переходов и состав шагов описаны в BPMN.

### 4.2. Запустить процесс через BLPS API

В Insomnia выполнить:

```text
5) BLPS submit tariff change -> starts TariffChangeProcess
```

Ожидаемо:

- HTTP `202`;
- в ответе есть `requestId`;
- статус сначала может быть `PENDING`.

Скопировать `requestId` в переменную:

```text
tariff_change_request_id
```

Что сказать:

> Пользователь вызывает обычный REST API нашего приложения. Но внутри сервис не выполняет весь сценарий сам, а создаёт заявку и стартует `TariffChangeProcess` в Camunda с business key `tariff-change-{requestId}`.

### 4.3. Проверить статус заявки в BLPS

Выполнить:

```text
6) BLPS tariff request status (copy requestId first)
```

Ожидаемо:

- статус `SUCCESS`, `REJECTED`, `FAILED` или промежуточный `PROCESSING`;
- если worker уже всё обработал, будет terminal status.

Что сказать:

> Итоговый статус хранится в доменной таблице приложения, но переходы к этому статусу выполнялись по Camunda-процессу.

### 4.4. Проверить Camunda instance

Сначала выполнить:

```text
7) Camunda active tariff instance by businessKey
```

Возможные варианты:

- если процесс ещё выполняется, будет active instance;
- если worker быстро всё обработал, ответ будет пустым.

Если пусто, сразу выполнить:

```text
8) Camunda tariff history by businessKey
```

Ожидаемо:

- есть historic process instance;
- business key вида `tariff-change-{requestId}`;
- process definition key `TariffChangeProcess`.

Скопировать `id` process instance в:

```text
camunda_process_instance_id
```

Затем выполнить:

```text
15) Activity history for selected processInstanceId
```

Показать в ответе activity names:

- подтверждение смены тарифа;
- проверка смены тарифа;
- списание;
- обновление тарифа;
- уведомление;
- аудит.

Что сказать:

> Если active instance пустой, это не ошибка: worker обрабатывает external tasks быстро. Поэтому для завершённых процессов мы смотрим history и activity history.

### 4.5. Показать этот же процесс в Cockpit

В Cockpit открыть history/process instance для `TariffChangeProcess`.

Показать:

- диаграмму процесса;
- пройденные activity;
- variables, если удобно;
- business key.

Что сказать:

> По activity history видно, что процесс реально прошёл через Camunda, а не просто вернул статус из Java-кода.

## 5. Демонстрация процесса отключения услуги

### 5.1. Открыть диаграмму

В Cockpit открыть:

```text
FeatureDisableProcess
```

Показать:

- user task;
- проверку возможности отключения;
- gateway;
- биллинг отключения;
- обновление услуги абонента;
- уведомление;
- аудит.

### 5.2. Запустить через BLPS API

В Insomnia выполнить:

```text
9) BLPS submit feature disable -> starts FeatureDisableProcess
```

Ожидаемо:

- HTTP `202`;
- в ответе есть `requestId`.

Скопировать `requestId` в:

```text
feature_disable_request_id
```

Что сказать:

> Это второй пользовательский процесс. Он стартует так же через REST API приложения, но дальнейшее выполнение идёт через BPMN и external tasks.

### 5.3. Проверить статус и history

Выполнить:

```text
10) BLPS feature request status (copy requestId first)
11) Camunda feature history by businessKey
```

Ожидаемо:

- в BLPS виден итоговый статус заявки;
- в Camunda history виден `FeatureDisableProcess`;
- business key вида `feature-disable-{requestId}`.

Что сказать:

> Здесь показываем тот же принцип на другом процессе: Camunda отвечает за orchestration, приложение выполняет бизнес-действия.

## 6. Демонстрация периодического процесса

### 6.1. Показать timer process в Cockpit

Открыть:

```text
MonthlyFeeCycleProcess
```

Показать:

- timer start event;
- service task `Создать заявки на списание`;
- завершение процесса.

Что сказать:

> Раньше периодическая задача могла быть обычным Spring scheduler. В лабораторной периодический запуск вынесен в BPMN timer event Camunda.

### 6.2. Проверить timer jobs через Insomnia

Выполнить:

```text
20) MonthlyFeeCycleProcess timer job definition
21) MonthlyFeeCycleProcess timer jobs
```

Ожидаемо:

- есть job definition для `MonthlyFeeCycleProcess`;
- есть timer job.

Что сказать:

> Эти запросы показывают, что периодика находится в Camunda job executor.

### 6.3. Проверить созданные monthly fee requests

Подождать 30 секунд или использовать уже созданные заявки.

Выполнить:

```text
12) BLPS monthly fee requests (timer-created)
```

Ожидаемо:

- список не пустой;
- есть `requestId`;
- есть billing period.

Скопировать id одной заявки в:

```text
monthly_fee_request_id
```

Затем выполнить:

```text
13) BLPS monthly fee status (copy requestId first)
14) Camunda monthly fee history by businessKey
```

Ожидаемо:

- BLPS показывает статус заявки;
- Camunda history показывает `MonthlyFeeChargeProcess`;
- business key вида `monthly-fee-{requestId}`.

Что сказать:

> `MonthlyFeeCycleProcess` не списывает деньги напрямую. Он создаёт заявки, а для каждой заявки стартует отдельный `MonthlyFeeChargeProcess`. Так периодика и обработка конкретной операции разделены.

## 7. Показ Camunda Tasklist и generated forms

Открыть:

```text
http://127.0.0.1:8082/camunda
Tasklist
```

Что показать:

1. Возможность стартовать процесс вручную.
2. Форму user task для `TariffChangeProcess` или `FeatureDisableProcess`.
3. Поля формы:
   - `subscriberId`;
   - `targetTariffId`;
   - `featureId`;
   - `options`.

Что сказать:

> Формы описаны в BPMN через Camunda form fields. В рабочем REST-сценарии пользователь отправляет данные через BLPS API, поэтому приложение автоматически закрывает первую user task. Но Tasklist показывает, что UI-форма может быть сгенерирована Camunda из BPMN-модели.

Если преподаватель спросит, почему user task не висит в обычном сценарии:

> Потому что в нашем API-сценарии пользователь уже заполнил форму через REST endpoint. Поэтому приложение после старта процесса вызывает completion первой user task и передаёт те же process variables в Camunda.

## 8. Показ external tasks, retries и incidents

### 8.1. External tasks

Выполнить:

```text
18) External tasks visible to workers
```

Ожидаемо:

- часто список пустой.

Что сказать:

> Пустой список здесь нормален: worker быстро делает fetch-and-lock и завершает задачи. Сам механизм external tasks виден в BPMN по `camunda:type="external"` и в коде worker.

Если нужно показать pending external tasks:

1. Запустить приложение с `CAMUNDA_ENABLED=false`.
2. Стартовать процесс через Insomnia.
3. Выполнить `18) External tasks visible to workers`.
4. Показать, что задачи накопились.
5. Перезапустить приложение с `CAMUNDA_ENABLED=true`.
6. Показать, что worker их обработал.

### 8.2. Incidents

Выполнить:

```text
19) Incidents / exhausted retries
```

Ожидаемо:

- пустой список.

Что сказать:

> Пустой список incidents означает, что external tasks не исчерпали retries. Если Java-handler падает, worker вызывает Camunda failure API, Camunda уменьшает retries и при исчерпании создаёт incident.

## 9. Показ Dolibarr

Dolibarr нужен, чтобы показать интеграцию внешней EIS-подсистемы, особенно для monthly fee invoice.

Открыть папку:

```text
Dolibarr Diagnostics
```

Выполнить:

```text
1) Dolibarr status (expect 200)
```

Ожидаемо:

- HTTP `200`;
- Dolibarr API доступен.

Если уже есть monthly fee request с invoice id, выполнить:

```text
6) Invoice by id (from monthly fee status)
```

Или:

```text
5) Monthly fee invoices by ref_ext (MFC-*)
```

Что сказать:

> Camunda не общается с Dolibarr напрямую. Camunda создаёт external task `create-dolibarr-invoice` или `sync-dolibarr-invoice`, а Java worker выполняет интеграцию с Dolibarr через сервис приложения.

Если не хочешь долго показывать Dolibarr UI, достаточно показать:

- `Dolibarr status`;
- invoice id/ref в BLPS monthly fee status;
- запрос invoice by id.

## 10. Как завершить demo-часть

Сказать:

> В демонстрации видно три уровня: BLPS API принимает пользовательские операции, Camunda хранит и исполняет BPMN-процессы, а external task worker в приложении выполняет конкретные бизнес-действия. Для пользователя API остаётся прежним, но orchestration перенесён в BPMS.

После этого переходишь к коду.

## 11. Часть защиты: как реализовано в коде

### 11.1. Начать с общей архитектуры

Открыть:

```text
docs/camunda-demo-plan.md
```

Показать runtime layout:

- WildFly с BLPS WAR на helios;
- Camunda Run локально в Docker Compose;
- reverse tunnel для доступа WAR к Camunda;
- RabbitMQ и Dolibarr как внешние подсистемы.

Что сказать:

> Camunda запущена отдельно, как требует задание. Приложение не поднимает Camunda engine внутри себя, а использует REST API standalone Camunda.

### 11.2. Показать Docker Compose

Открыть:

```text
docker-compose.yml
```

Показать:

```yaml
camunda-db
camunda
image: camunda/camunda-bpm-platform:run-7.24.0
ports:
  - "127.0.0.1:8082:8080"
```

Что сказать:

> Camunda хранит свои process definitions, process instances, jobs и history в отдельной базе `camunda-db`. Наружу Camunda опубликована на `127.0.0.1:8082`.

### 11.3. Показать настройки приложения

Открыть:

```text
src/main/resources/application.properties
```

Показать:

```properties
app.camunda.enabled=${CAMUNDA_ENABLED:true}
app.camunda.base-url=${CAMUNDA_BASE_URL:http://127.0.0.1:8082/engine-rest}
app.camunda.worker-id=${CAMUNDA_WORKER_ID:blps-local-worker}
app.camunda.poll-interval-ms=${CAMUNDA_POLL_INTERVAL_MS:2000}
app.camunda.default-retries=${CAMUNDA_DEFAULT_RETRIES:3}
```

Также показать:

```properties
app.scheduler.monthly-fee-enabled=${MONTHLY_FEE_SCHEDULER_ENABLED:false}
```

Что сказать:

> Worker можно включать и выключать через `CAMUNDA_ENABLED`. Старый monthly fee scheduler выключен, чтобы периодический запуск шёл из Camunda timer.

### 11.4. Показать helios-start

Открыть:

```text
scripts/helios-start.sh
```

Показать:

```sh
export CAMUNDA_BASE_URL="${CAMUNDA_BASE_URL:-http://127.0.0.1:18080/engine-rest}"
```

Что сказать:

> На helios `127.0.0.1:8082` был бы localhost самого сервера, поэтому для WAR используется reverse tunnel на порт `18080`.

### 11.5. Показать BPMN-файлы

Открыть папку:

```text
src/main/resources/processes/
```

Показать 4 файла:

```text
tariff-change-process.bpmn
feature-disable-process.bpmn
monthly-fee-cycle-process.bpmn
monthly-fee-charge-process.bpmn
```

В одном из файлов показать:

```xml
isExecutable="true"
camunda:historyTimeToLive="30"
```

Показать service task:

```xml
camunda:type="external"
camunda:topic="validate-tariff-change"
```

Показать user task:

```xml
camunda:candidateGroups="SUBSCRIBER,OPERATOR,ADMIN"
camunda:formData
```

Показать DI-разметку:

```xml
bpmndi:BPMNDiagram
```

Что сказать:

> В BPMN описаны не только картинки, но и исполняемая модель: process id, external task topics, conditions, timer и form fields. DI-разметка нужна для отображения диаграммы в Cockpit.

### 11.6. Показать deployment script

Открыть:

```text
scripts/deploy-camunda-processes.sh
```

Показать:

```sh
CAMUNDA_BASE_URL="${CAMUNDA_BASE_URL:-http://127.0.0.1:8082/engine-rest}"
PROCESS_TARGET="${1:-src/main/resources/processes}"
```

И endpoint:

```text
/deployment/create
```

Что сказать:

> Скрипт берёт все `.bpmn` файлы из папки `src/main/resources/processes` и деплоит их в Camunda. Включены duplicate filtering и deploy changed only, поэтому новая версия создаётся только при изменении BPMN.

### 11.7. Показать constants

Открыть:

```text
src/main/java/ru/urasha/callmeani/blps/service/camunda/CamundaProcessConstants.java
```

Показать:

- process keys;
- topic names.

Что сказать:

> Этот файл связывает BPMN и Java. Если в BPMN указан topic `validate-tariff-change`, такой же topic должен быть в Java, иначе worker не подхватит задачу.

### 11.8. Показать CamundaRestClient

Открыть:

```text
src/main/java/ru/urasha/callmeani/blps/service/camunda/CamundaRestClient.java
```

Показать методы:

```java
startProcess(...)
completeFirstTask(...)
fetchAndLock(...)
completeExternalTask(...)
handleFailure(...)
```

Что сказать:

> Это тонкая обёртка над Camunda REST API. Через неё приложение стартует процессы, завершает user tasks, забирает external tasks, завершает их или сообщает Camunda об ошибке.

### 11.9. Показать запуск процессов из бизнес-сервисов

Открыть:

```text
src/main/java/ru/urasha/callmeani/blps/service/tariff/async/TariffChangeAsyncService.java
```

Показать:

```java
submitTariffChange(...)
startProcess(...)
processVariables(...)
```

Что сказать:

> Сервис создаёт доменную заявку, сохраняет её в PostgreSQL, затем стартует `TariffChangeProcess` в Camunda и сохраняет `processInstanceId`.

Показать business key:

```text
tariff-change-{requestId}
```

Затем открыть:

```text
src/main/java/ru/urasha/callmeani/blps/service/feature/async/FeatureDisableAsyncService.java
```

Сказать:

> Для отключения услуги схема такая же: создаётся заявка, стартует `FeatureDisableProcess`, дальше процесс выполняется через Camunda.

Затем открыть:

```text
src/main/java/ru/urasha/callmeani/blps/service/billing/async/MonthlyFeeChargeAsyncService.java
```

Сказать:

> Для ежемесячного списания Camunda timer создаёт задачу `create-monthly-fee-requests`, а Java-сервис создаёт отдельные заявки и стартует `MonthlyFeeChargeProcess` для каждой.

### 11.10. Показать worker

Открыть:

```text
src/main/java/ru/urasha/callmeani/blps/service/camunda/CamundaExternalTaskWorker.java
```

Показать:

```java
@Scheduled(fixedDelayString = "${app.camunda.poll-interval-ms:2000}")
fetchAndHandleTasks()
```

И:

```java
camundaRestClient.fetchAndLock(...)
camundaRestClient.completeExternalTask(...)
camundaRestClient.handleFailure(...)
```

Что сказать:

> Это наш external task worker. Он раз в несколько секунд опрашивает Camunda, блокирует задачи на worker id, выполняет Java-handler и сообщает результат обратно в Camunda.

### 11.11. Показать Camunda handler и доменные task services

Открыть:

```text
src/main/java/ru/urasha/callmeani/blps/service/camunda/CamundaBusinessTaskHandler.java
```

Показать:

```java
topics()
handle(...)
switch (task.topicName())
```

Что сказать:

> `CamundaBusinessTaskHandler` сейчас не содержит бизнес-логику. Это маршрутизатор между BPMN topic и сервисом нужной доменной области. Так код не превращается в один большой Camunda-класс, а остаётся в структуре проекта: тарифы отдельно, услуги отдельно, биллинг отдельно.

Показать делегирование:

```java
case CamundaProcessConstants.VALIDATE_TARIFF_CHANGE -> tariffChangeTaskService.validateTariffChange(task);
case CamundaProcessConstants.UPDATE_SUBSCRIBER_TARIFF -> tariffChangeTaskService.updateSubscriberTariff(task);
case CamundaProcessConstants.VALIDATE_FEATURE_DISABLE -> featureDisableTaskService.validateFeatureDisable(task);
case CamundaProcessConstants.CREATE_DOLIBARR_INVOICE -> monthlyFeeTaskService.createDolibarrInvoice(task);
```

Открыть:

```text
src/main/java/ru/urasha/callmeani/blps/service/tariff/camunda/TariffChangeCamundaTaskService.java
src/main/java/ru/urasha/callmeani/blps/service/tariff/camunda/impl/TariffChangeCamundaTaskServiceImpl.java
```

Затем аналогично показать:

```text
src/main/java/ru/urasha/callmeani/blps/service/feature/camunda/FeatureDisableCamundaTaskService.java
src/main/java/ru/urasha/callmeani/blps/service/billing/camunda/MonthlyFeeCamundaTaskService.java
```

Что сказать:

> У каждого такого сервиса сначала есть интерфейс, потом реализация. В реализации напрямую подключён только один репозиторий своей доменной заявки. Остальные действия с БД идут через сервисы других доменных областей: `SubscriberService`, `BillingService`, `NotificationService`, `EisValidationService`, `DolibarrInvoiceService`.

Показать:

```java
@Override
@Transactional
public Map<String, CamundaVariable> validateTariffChange(LockedExternalTask task) {
    ...
}
```

Что сказать:

> Транзакции остались внутри приложения. Camunda не управляет распределённой транзакцией, она управляет процессом и retry. Конкретная доменная операция выполняется в Spring-сервисе внутри `@Transactional`.

### 11.12. Показать файл пояснения

Открыть:

```text
docs/camunda-integration-defense.md
```

Если преподаватель задаёт архитектурные вопросы, использовать этот файл как шпаргалку.

## 12. Короткая финальная формулировка

Сказать:

> В результате мы сохранили внешний REST API приложения, но изменили внутреннюю orchestration-модель. Теперь бизнес-процессы описаны в BPMN 2.0 и задеплоены в standalone Camunda. Приложение стартует process instances через REST API, Camunda управляет последовательностью шагов, условиями, таймером, retries и history, а Java worker выполняет external tasks и возвращает результат через process variables.

## 13. Если времени мало: минимальный маршрут показа

Если преподаватель торопит, показывай только это:

1. Insomnia:
   - `Auth -> Login as SUBSCRIBER`;
   - `Lab4 -> 1) Camunda engine version`;
   - `Lab4 -> 3) Process definitions`;
   - `Lab4 -> 5) BLPS submit tariff change`;
   - `Lab4 -> 8) Camunda tariff history by businessKey`;
   - `Lab4 -> 15) Activity history`;
   - `Lab4 -> 20/21) MonthlyFeeCycleProcess timer jobs`;
   - `Lab4 -> 19) Incidents`.

2. Camunda UI:
   - Cockpit -> Processes -> `TariffChangeProcess`;
   - Cockpit -> `MonthlyFeeCycleProcess`;
   - Tasklist -> generated form.

3. Код:
   - `src/main/resources/processes/tariff-change-process.bpmn`;
   - `CamundaRestClient`;
   - `CamundaExternalTaskWorker`;
   - `CamundaBusinessTaskHandler`;
   - `TariffChangeCamundaTaskServiceImpl`;
   - `FeatureDisableCamundaTaskServiceImpl`;
   - `MonthlyFeeCamundaTaskServiceImpl`;
   - `TariffChangeAsyncService`;
   - `scripts/deploy-camunda-processes.sh`.

## 14. Типичные проблемы во время показа

### Active instance пустой

Это нормально. Worker быстро завершил процесс.

Что делать:

- показывать `history/process-instance`;
- затем `history/activity-instance`.

### External tasks пустые

Это тоже нормально. Worker быстро забирает задачи.

Что сказать:

> Пустой список external tasks в нормальном режиме означает, что worker успевает их обработать.

### В Cockpit нет диаграммы

Проверить:

- что открыта последняя версия process definition;
- что BPMN содержит `bpmndi:BPMNDiagram`;
- что после исправления BPMN был выполнен deploy.

### 401/403 в Insomnia

Проверить:

- выполнен ли `Login as SUBSCRIBER`;
- скопирован ли `accessToken` в `active_token`;
- не остался ли admin/subscriber token от другого сценария.

### Monthly fee requests не появились

Проверить:

- запущен ли `MonthlyFeeCycleProcess`;
- есть ли timer job;
- прошло ли 30 секунд;
- есть ли у абонента current tariff;
- работает ли Camunda worker.

### Dolibarr invoice не находится

Проверить:

- `Dolibarr Diagnostics -> 1) Dolibarr status`;
- переменную `dolibarr_invoice_id` из monthly fee status;
- включены ли настройки EIS/Dolibarr в окружении.
