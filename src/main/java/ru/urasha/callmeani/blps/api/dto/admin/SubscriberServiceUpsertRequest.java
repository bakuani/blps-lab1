package ru.urasha.callmeani.blps.api.dto.admin;

import jakarta.validation.constraints.NotNull;
import ru.urasha.callmeani.blps.domain.enums.SubscriberServiceStatus;

import java.time.OffsetDateTime;

public record SubscriberServiceUpsertRequest(
    @NotNull Long subscriberId,
    @NotNull Long serviceId,
    @NotNull SubscriberServiceStatus status,
    OffsetDateTime connectedAt,
    OffsetDateTime disabledAt
) {
}
