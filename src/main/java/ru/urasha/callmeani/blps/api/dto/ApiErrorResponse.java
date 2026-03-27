package ru.urasha.callmeani.blps.api.dto;

import java.time.OffsetDateTime;

public record ApiErrorResponse(OffsetDateTime timestamp, String message) {
}
