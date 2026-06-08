package ru.urasha.callmeani.blps.service.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LockedExternalTask(
    String id,
    String topicName,
    String processInstanceId,
    String businessKey,
    Integer retries,
    Map<String, CamundaVariable> variables
) {
    public Long longVariable(String name) {
        Object value = variableValue(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        return null;
    }

    public String stringVariable(String name) {
        Object value = variableValue(name);
        return value == null ? null : String.valueOf(value);
    }

    public boolean booleanVariable(String name) {
        Object value = variableValue(name);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private Object variableValue(String name) {
        CamundaVariable variable = variables == null ? null : variables.get(name);
        return variable == null ? null : variable.value();
    }
}
