package ru.urasha.callmeani.blps.service.billing.async;

import ru.urasha.callmeani.blps.api.dto.billing.MonthlyFeeChargeRequestStatusResponse;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface MonthlyFeeChargeAsyncOperations {
    int enqueueCurrentCycleCharges();
    MonthlyFeeChargeRequestStatusResponse getStatus(Long subscriberId, Long requestId);
    List<MonthlyFeeChargeRequestStatusResponse> getRecentStatuses(Long subscriberId);
    int retryStuckOperations(OffsetDateTime threshold, List<TariffChangeRequestStatus> targetStatuses);
}

