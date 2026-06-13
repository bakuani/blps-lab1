package ru.urasha.callmeani.blps.service.camunda.external;

import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.camunda.model.LockedExternalTask;

import java.util.Map;
import java.util.Set;

public interface CamundaExternalTaskHandler {
    Set<String> topics();
    Map<String, CamundaVariable> handle(LockedExternalTask task);

    default boolean supports(String topic) {
        return topics().contains(topic);
    }

    default void handleFailure(LockedExternalTask task, RuntimeException exception, int retriesLeft) {
    }
}
