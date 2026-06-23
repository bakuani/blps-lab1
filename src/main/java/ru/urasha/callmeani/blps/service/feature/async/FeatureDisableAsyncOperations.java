package ru.urasha.callmeani.blps.service.feature.async;

import ru.urasha.callmeani.blps.api.dto.feature.FeatureDisableRequestStatusResponse;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureDisableSubmissionResponse;
import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface FeatureDisableAsyncOperations {
    FeatureDisableSubmissionResponse submitFeatureDisable(Long subscriberId, Long featureId);
    FeatureDisableRequestStatusResponse getStatus(Long subscriberId, Long requestId);
    int retryStuckOperations(OffsetDateTime threshold, List<BusinessRequestStatus> targetStatuses);
}

