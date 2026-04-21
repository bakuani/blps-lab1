package ru.urasha.callmeani.blps.api.dto.common;

import java.time.OffsetDateTime;

public record ApiErrorResponse(OffsetDateTime timestamp, String message) {
}

