package ru.urasha.callmeani.blps.api.dto.notification;

import java.time.OffsetDateTime;

public record NotificationDto(
    String type,
    String message,
    boolean success,
    OffsetDateTime createdAt
) {
}

