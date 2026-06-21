package ru.urasha.callmeani.blps.logging;

import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class LoggingContext implements AutoCloseable {

    private final Map<String, String> previousValues;
    private boolean closed;

    private LoggingContext(Map<String, String> fields) {
        previousValues = new LinkedHashMap<>();
        fields.forEach((key, value) -> {
            previousValues.put(key, MDC.get(key));
            if (value == null || value.isBlank()) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        });
    }

    public static LoggingContext open(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Logging context fields must be passed as key-value pairs");
        }

        Map<String, String> fields = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            if (!(keyValues[index] instanceof String key)) {
                throw new IllegalArgumentException("Logging context field name must be a String");
            }
            Object value = keyValues[index + 1];
            fields.put(key, value == null ? null : String.valueOf(value));
        }
        return new LoggingContext(fields);
    }

    public static void put(String key, Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            MDC.remove(key);
        } else {
            MDC.put(key, String.valueOf(value));
        }
    }

    public static String get(String key) {
        return MDC.get(key);
    }

    public static void clear() {
        MDC.clear();
    }

    public static String getOrCreateCorrelationId() {
        String correlationId = MDC.get(LoggingFields.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            MDC.put(LoggingFields.CORRELATION_ID, correlationId);
        }
        return correlationId;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        previousValues.forEach((key, previousValue) -> {
            if (previousValue == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, previousValue);
            }
        });
        closed = true;
    }
}
