package ru.urasha.callmeani.blps.api.dto.feature;

import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;

import java.time.OffsetDateTime;

public record FeatureDisableRequestStatusResponse(
    Long requestId,
    BusinessRequestStatus status,
    String errorMessage,
    int attemptCount,
    OffsetDateTime updatedAt
) {
}
