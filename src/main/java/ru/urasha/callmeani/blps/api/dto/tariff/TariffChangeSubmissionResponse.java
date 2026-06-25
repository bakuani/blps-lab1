package ru.urasha.callmeani.blps.api.dto.tariff;

import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;

public record TariffChangeSubmissionResponse(
    Long requestId,
    BusinessRequestStatus status,
    String message
) {
}
