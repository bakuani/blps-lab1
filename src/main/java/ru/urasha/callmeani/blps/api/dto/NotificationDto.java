package ru.urasha.callmeani.blps.api.dto;

import java.time.OffsetDateTime;

public record NotificationDto(
    String type,
    String message,
    boolean success,
    OffsetDateTime createdAt
) {
}
