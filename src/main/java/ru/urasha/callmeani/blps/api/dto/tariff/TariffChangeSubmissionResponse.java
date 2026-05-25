package ru.urasha.callmeani.blps.api.dto.tariff;

import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;

public record TariffChangeSubmissionResponse(
    Long requestId,
    TariffChangeRequestStatus status,
    String message
) {
}
