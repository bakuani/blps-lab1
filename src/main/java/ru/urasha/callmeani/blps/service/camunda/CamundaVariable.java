package ru.urasha.callmeani.blps.service.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CamundaVariable(Object value, String type) {

    public static CamundaVariable string(String value) {
        return new CamundaVariable(value, "String");
    }

    public static CamundaVariable bool(boolean value) {
        return new CamundaVariable(value, "Boolean");
    }

    public static CamundaVariable integer(int value) {
        return new CamundaVariable(value, "Integer");
    }

    public static CamundaVariable longValue(Long value) {
        return new CamundaVariable(value, "Long");
    }
}
