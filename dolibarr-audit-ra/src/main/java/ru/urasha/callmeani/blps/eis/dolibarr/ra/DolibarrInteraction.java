package ru.urasha.callmeani.blps.eis.dolibarr.ra;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class DolibarrInteraction {

    private static final String API_PREFIX = "/api/index.php";

    private final String baseUrl;
    private final String apiKey;
    private final Duration readTimeout;
    private final HttpClient httpClient;
    private boolean closed;

    public DolibarrInteraction(
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout
    ) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey;
        this.readTimeout = readTimeout;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build();
    }

    public ExecutionResult execute(String interactionName, Map<String, Object> payload) {
        ensureOpen();
        String endpoint = resolveEndpoint(interactionName);
        URI uri = URI.create(baseUrl + endpoint);

        HttpRequest request;
        if (isStatusEndpoint(endpoint)) {
            request = HttpRequest.newBuilder(uri)
                .timeout(readTimeout)
                .header("Accept", "application/json")
                .header("DOLAPIKEY", apiKey)
                .GET()
                .build();
        } else {
            request = HttpRequest.newBuilder(uri)
                .timeout(readTimeout)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("DOLAPIKEY", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build();
        }

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean accepted = response.statusCode() >= 200 && response.statusCode() < 300;
            String error = accepted ? null : abbreviate(response.body(), 600);
            return new ExecutionResult(accepted, response.statusCode(), endpoint, error);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ExecutionResult(false, 0, endpoint, ex.getMessage());
        }
    }

    public void close() {
        this.closed = true;
    }

    public record ExecutionResult(
        boolean accepted,
        int statusCode,
        String endpoint,
        String error
    ) {
    }

    private void ensureOpen() {
        if (closed) {
            throw new DolibarrAuditException("Dolibarr interaction is closed");
        }
    }

    private String resolveEndpoint(String interactionName) {
        if (interactionName == null || interactionName.isBlank()) {
            return API_PREFIX + "/status";
        }

        String candidate = interactionName.trim();
        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            URI uri = URI.create(candidate);
            return uri.getRawPath();
        }

        if (candidate.startsWith(API_PREFIX + "/")) {
            return candidate;
        }

        if (candidate.startsWith("/")) {
            return API_PREFIX + candidate;
        }

        if ("status".equalsIgnoreCase(candidate)) {
            return API_PREFIX + "/status";
        }

        String normalized = candidate.replace('.', '/');
        return API_PREFIX + "/" + normalized;
    }

    private boolean isStatusEndpoint(String endpoint) {
        return endpoint != null && endpoint.endsWith("/status");
    }

    private String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null) {
            return "";
        }
        String normalized = rawUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return "\"" + escapeJson(text) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                builder.append("\"")
                    .append(escapeJson(Objects.toString(entry.getKey(), "")))
                    .append("\":")
                    .append(toJson(entry.getValue()));
                if (iterator.hasNext()) {
                    builder.append(",");
                }
            }
            return builder.append("}").toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder builder = new StringBuilder("[");
            Iterator<?> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                builder.append(toJson(iterator.next()));
                if (iterator.hasNext()) {
                    builder.append(",");
                }
            }
            return builder.append("]").toString();
        }
        return "\"" + escapeJson(String.valueOf(value)) + "\"";
    }

    private String escapeJson(String source) {
        StringBuilder builder = new StringBuilder(source.length() + 16);
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch <= 0x1F) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }

    private String abbreviate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit) + "...";
    }
}
