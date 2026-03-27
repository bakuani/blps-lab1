package ru.urasha.callmeani.blps.api.dto.admin;

import ru.urasha.callmeani.blps.domain.enums.SubscriberServiceStatus;

import java.time.OffsetDateTime;

public record SubscriberServiceAdminResponse(
    Long id,
    Long subscriberId,
    Long serviceId,
    SubscriberServiceStatus status,
    OffsetDateTime connectedAt,
    OffsetDateTime disabledAt
) {
}
