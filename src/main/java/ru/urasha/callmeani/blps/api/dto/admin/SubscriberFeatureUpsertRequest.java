package ru.urasha.callmeani.blps.api.dto.admin;

import jakarta.validation.constraints.NotNull;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;

import java.time.OffsetDateTime;

public record SubscriberFeatureUpsertRequest(
    @NotNull Long subscriberId,
    @NotNull Long featureId,
    @NotNull SubscriberFeatureStatus status,
    OffsetDateTime connectedAt,
    OffsetDateTime disabledAt
) {
}

