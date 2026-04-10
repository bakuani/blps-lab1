package ru.urasha.callmeani.blps.api.dto.admin;

import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;

import java.time.OffsetDateTime;

public record SubscriberFeatureAdminResponse(
    Long id,
    Long subscriberId,
    Long featureId,
    SubscriberFeatureStatus status,
    OffsetDateTime connectedAt,
    OffsetDateTime disabledAt
) {
}

