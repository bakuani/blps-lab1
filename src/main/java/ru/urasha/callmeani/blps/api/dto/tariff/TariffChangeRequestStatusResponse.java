package ru.urasha.callmeani.blps.api.dto.tariff;

import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;

import java.time.OffsetDateTime;

public record TariffChangeRequestStatusResponse(
    Long requestId,
    BusinessRequestStatus status,
    String errorMessage,
    int attemptCount,
    OffsetDateTime updatedAt
) {
}
