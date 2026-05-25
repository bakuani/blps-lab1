package ru.urasha.callmeani.blps.api.dto.feature;

import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;

import java.time.OffsetDateTime;

public record FeatureDisableRequestStatusResponse(
    Long requestId,
    TariffChangeRequestStatus status,
    String errorMessage,
    int attemptCount,
    OffsetDateTime updatedAt
) {
}
