package ru.urasha.callmeani.blps.api.dto.tariff;

import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;

import java.time.OffsetDateTime;

public record TariffChangeRequestStatusResponse(
    Long requestId,
    TariffChangeRequestStatus status,
    String errorMessage,
    int attemptCount,
    OffsetDateTime updatedAt
) {
}
