package ru.urasha.callmeani.blps.api.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.urasha.callmeani.blps.api.dto.common.ApiErrorResponse;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.logging.LoggingContext;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
        logClientError(HttpStatus.NOT_FOUND, ex);
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
            .map(this::formatFieldError)
            .collect(Collectors.joining("; "));
        logClientError(HttpStatus.BAD_REQUEST, ex);
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        logClientError(HttpStatus.BAD_REQUEST, ex);
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        logClientError(HttpStatus.BAD_REQUEST, ex);
        return build(HttpStatus.BAD_REQUEST, ApiMessages.MALFORMED_JSON_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = ApiMessages.INVALID_PARAMETER_PREFIX + ex.getName();
        logClientError(HttpStatus.BAD_REQUEST, ex);
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        logClientError(HttpStatus.CONFLICT, ex);
        return build(HttpStatus.CONFLICT, ApiMessages.DATA_INTEGRITY_VIOLATION);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex) {
        logClientError(HttpStatus.UNAUTHORIZED, ex);
        return build(HttpStatus.UNAUTHORIZED, ApiMessages.INVALID_CREDENTIALS);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        logClientError(HttpStatus.FORBIDDEN, ex);
        return build(HttpStatus.FORBIDDEN, ApiMessages.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAny(Exception ex) {
        try (LoggingContext ignored = LoggingContext.open(
            EVENT_CATEGORY, "web",
            EVENT_ACTION, "unhandled_exception",
            EVENT_OUTCOME, "failure"
        )) {
            log.error("Unhandled exception", ex);
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ApiMessages.INTERNAL_SERVER_ERROR);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message) {
        ApiErrorResponse response = new ApiErrorResponse(OffsetDateTime.now(), message);
        return ResponseEntity.status(status).body(response);
    }

    private void logClientError(HttpStatus status, Exception exception) {
        try (LoggingContext ignored = LoggingContext.open(
            EVENT_CATEGORY, "web",
            EVENT_ACTION, "request_rejected",
            EVENT_OUTCOME, "failure"
        )) {
            log.warn(
                "Request rejected: status={}, exceptionType={}",
                status.value(),
                exception.getClass().getSimpleName()
            );
        }
    }
}

