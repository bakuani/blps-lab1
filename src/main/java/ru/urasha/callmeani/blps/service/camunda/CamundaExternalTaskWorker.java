package ru.urasha.callmeani.blps.service.camunda;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.urasha.callmeani.blps.config.CamundaProperties;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            log.warn("Camunda external task fetch failed: {}", ex.getMessage());
            return;
        }

        for (LockedExternalTask task : tasks) {
            CamundaExternalTaskHandler handler = handlersByTopic.get(task.topicName());
            if (handler == null) {
                continue;
            }
            handleTask(task, handler);
        }
    }

    private void handleTask(LockedExternalTask task, CamundaExternalTaskHandler handler) {
        try {
            Map<String, CamundaVariable> variables = handler.handle(task);
            camundaRestClient.completeExternalTask(task.id(), variables == null ? Map.of() : variables);
        } catch (RuntimeException ex) {
            int currentRetries = task.retries() == null ? properties.getDefaultRetries() : task.retries();
            int retriesLeft = Math.max(currentRetries - 1, 0);
            handler.handleFailure(task, ex, retriesLeft);
            try {
                camundaRestClient.handleFailure(task.id(), ex.getMessage(), stackTrace(ex), retriesLeft);
            } catch (RuntimeException failureEx) {
                log.warn("Camunda external task failure report failed: {}", failureEx.getMessage());
            }
        }
    }

    private String stackTrace(RuntimeException exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
