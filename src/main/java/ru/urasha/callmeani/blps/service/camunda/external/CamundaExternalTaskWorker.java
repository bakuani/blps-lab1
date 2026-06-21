package ru.urasha.callmeani.blps.service.camunda.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.urasha.callmeani.blps.config.CamundaProperties;
import ru.urasha.callmeani.blps.logging.LoggingContext;
import ru.urasha.callmeani.blps.service.camunda.client.CamundaRestClient;
import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.camunda.model.LockedExternalTask;
import ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static ru.urasha.callmeani.blps.logging.LoggingFields.BUSINESS_OPERATION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.BUSINESS_REQUEST_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.CAMUNDA_TASK_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.CAMUNDA_TOPIC;
import static ru.urasha.callmeani.blps.logging.LoggingFields.CORRELATION_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_DURATION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;
import static ru.urasha.callmeani.blps.logging.LoggingFields.PROCESS_BUSINESS_KEY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.PROCESS_INSTANCE_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.PROCESS_KEY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.SUBSCRIBER_ID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.camunda.enabled", havingValue = "true", matchIfMissing = true)
public class CamundaExternalTaskWorker {

    private final CamundaRestClient camundaRestClient;
    private final CamundaProperties properties;
    private final List<CamundaExternalTaskHandler> handlers;

    @Scheduled(fixedDelayString = "${app.camunda.poll-interval-ms:2000}")
    public void fetchAndHandleTasks() {
        if (handlers.isEmpty()) {
            return;
        }

        Map<String, CamundaExternalTaskHandler> handlersByTopic = handlers.stream()
            .flatMap(handler -> handler.topics().stream().map(topic -> Map.entry(topic, handler)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first));

        List<LockedExternalTask> tasks;
        try {
            tasks = camundaRestClient.fetchAndLock(List.copyOf(handlersByTopic.keySet()));
        } catch (RuntimeException ex) {
            try (LoggingContext ignored = LoggingContext.open(
                EVENT_CATEGORY, "process",
                EVENT_ACTION, "camunda_fetch_and_lock",
                EVENT_OUTCOME, "failure"
            )) {
                log.warn("Camunda external task fetch failed", ex);
            }
            return;
        }

        for (LockedExternalTask task : tasks) {
            CamundaExternalTaskHandler handler = handlersByTopic.get(task.topicName());
            if (handler == null) {
                try (LoggingContext ignored = taskContext(task)) {
                    log.error("No handler registered for Camunda external task topic");
                }
                continue;
            }
            handleTask(task, handler);
        }
    }

    private void handleTask(LockedExternalTask task, CamundaExternalTaskHandler handler) {
        long startedAt = System.nanoTime();
        try (LoggingContext ignored = taskContext(task)) {
            log.info("Camunda external task started");
            try {
                Map<String, CamundaVariable> variables = handler.handle(task);
                Map<String, CamundaVariable> result = variables == null ? Map.of() : variables;
                camundaRestClient.completeExternalTask(task.id(), result);
                try (LoggingContext outcome = LoggingContext.open(
                    EVENT_OUTCOME, "success",
                    EVENT_DURATION, System.nanoTime() - startedAt
                )) {
                    log.info("Camunda external task completed: outputVariables={}", result.keySet());
                }
            } catch (RuntimeException ex) {
                int currentRetries = task.retries() == null ? properties.getDefaultRetries() : task.retries();
                int retriesLeft = Math.max(currentRetries - 1, 0);
                try (LoggingContext outcome = LoggingContext.open(
                    EVENT_OUTCOME, "failure",
                    EVENT_DURATION, System.nanoTime() - startedAt
                )) {
                    log.error("Camunda external task failed: retriesLeft={}", retriesLeft, ex);
                }
                try {
                    handler.handleFailure(task, ex, retriesLeft);
                } catch (RuntimeException persistenceEx) {
                    log.error("Failed to persist Camunda business task failure", persistenceEx);
                }
                try {
                    camundaRestClient.handleFailure(task.id(), ex.getMessage(), stackTrace(ex), retriesLeft);
                } catch (RuntimeException failureEx) {
                    log.error("Camunda external task failure report failed", failureEx);
                }
            }
        }
    }

    private LoggingContext taskContext(LockedExternalTask task) {
        String correlationId = task.stringVariable(CamundaProcessVariables.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return LoggingContext.open(
            CORRELATION_ID, correlationId,
            EVENT_CATEGORY, "process",
            EVENT_ACTION, "camunda_external_task",
            PROCESS_KEY, task.processDefinitionKey(),
            PROCESS_INSTANCE_ID, task.processInstanceId(),
            PROCESS_BUSINESS_KEY, task.businessKey(),
            CAMUNDA_TASK_ID, task.id(),
            CAMUNDA_TOPIC, task.topicName(),
            BUSINESS_OPERATION, task.stringVariable(CamundaProcessVariables.OPERATION_TYPE),
            BUSINESS_REQUEST_ID, task.longVariable(CamundaProcessVariables.REQUEST_ID),
            SUBSCRIBER_ID, task.longVariable(CamundaProcessVariables.SUBSCRIBER_ID)
        );
    }

    private String stackTrace(RuntimeException exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
