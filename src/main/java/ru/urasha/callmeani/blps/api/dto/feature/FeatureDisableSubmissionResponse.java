package ru.urasha.callmeani.blps.api.dto.feature;

import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;

public record FeatureDisableSubmissionResponse(
    Long requestId,
    TariffChangeRequestStatus status,
    String message
) {
}
