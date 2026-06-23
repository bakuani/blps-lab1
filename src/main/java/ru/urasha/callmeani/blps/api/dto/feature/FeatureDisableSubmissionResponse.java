package ru.urasha.callmeani.blps.api.dto.feature;

import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;

public record FeatureDisableSubmissionResponse(
    Long requestId,
    BusinessRequestStatus status,
    String message
) {
}
