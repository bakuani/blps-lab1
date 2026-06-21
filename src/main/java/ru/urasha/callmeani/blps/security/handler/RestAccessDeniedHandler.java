package ru.urasha.callmeani.blps.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import ru.urasha.callmeani.blps.api.dto.common.ApiErrorResponse;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.logging.LoggingContext;

import java.io.IOException;
import java.time.OffsetDateTime;

import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        try (LoggingContext ignored = LoggingContext.open(
            EVENT_CATEGORY, "authorization",
            EVENT_ACTION, "access_denied",
            EVENT_OUTCOME, "failure"
        )) {
            log.warn("Access to the requested resource was denied");
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse payload = new ApiErrorResponse(OffsetDateTime.now(), ApiMessages.FORBIDDEN);
        objectMapper.writeValue(response.getOutputStream(), payload);
    }
}
