package ru.urasha.callmeani.blps.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import static ru.urasha.callmeani.blps.logging.LoggingFields.CLIENT_ADDRESS;
import static ru.urasha.callmeani.blps.logging.LoggingFields.CORRELATION_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_DURATION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;
import static ru.urasha.callmeani.blps.logging.LoggingFields.HTTP_METHOD;
import static ru.urasha.callmeani.blps.logging.LoggingFields.HTTP_STATUS_CODE;
import static ru.urasha.callmeani.blps.logging.LoggingFields.URL_PATH;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final int MAX_CORRELATION_ID_LENGTH = 128;
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]+");

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        @NotNull HttpServletResponse response,
        @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(CORRELATION_HEADER));
        response.setHeader(CORRELATION_HEADER, correlationId);
        long startedAt = System.nanoTime();

        try (LoggingContext ignored = LoggingContext.open(
            CORRELATION_ID, correlationId,
            EVENT_CATEGORY, "web",
            HTTP_METHOD, request.getMethod(),
            URL_PATH, request.getRequestURI(),
            CLIENT_ADDRESS, clientAddress(request)
        )) {
            try (LoggingContext event = LoggingContext.open(EVENT_ACTION, "http_request_started")) {
                log.info("HTTP request started");
            }

            try {
                filterChain.doFilter(request, response);
            } finally {
                long duration = System.nanoTime() - startedAt;
                int status = response.getStatus();
                try (LoggingContext event = LoggingContext.open(
                    EVENT_ACTION, "http_request_completed",
                    EVENT_OUTCOME, status < 400 ? "success" : "failure",
                    EVENT_DURATION, duration,
                    HTTP_STATUS_CODE, status
                )) {
                    if (status >= 500) {
                        log.error("HTTP request completed with server error");
                    } else if (status >= 400) {
                        log.warn("HTTP request completed with client error");
                    } else {
                        log.info("HTTP request completed");
                    }
                }
            }
        } finally {
            LoggingContext.clear();
        }
    }

    private String resolveCorrelationId(String candidate) {
        if (candidate == null
            || candidate.isBlank()
            || candidate.length() > MAX_CORRELATION_ID_LENGTH
            || !SAFE_CORRELATION_ID.matcher(candidate).matches()) {
            return UUID.randomUUID().toString();
        }
        return candidate;
    }

    private String clientAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
