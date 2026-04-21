package ru.urasha.callmeani.blps.api.dto.subscriber;

import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;

import java.time.OffsetDateTime;

public record SubscriberFeatureResponse(
    Long id,
    Long subscriberId,
    Long featureId,
    SubscriberFeatureStatus status,
    OffsetDateTime connectedAt,
    OffsetDateTime disabledAt
) {
}




