package ru.urasha.callmeani.blps.service.tariff.async;

import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffChangeRequestStatusResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffChangeSubmissionResponse;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface TariffChangeAsyncOperations {
    TariffChangeSubmissionResponse submitTariffChange(Long subscriberId, ChangeTariffRequest request);
    TariffChangeRequestStatusResponse getStatus(Long subscriberId, Long requestId);
    int retryStuckOperations(OffsetDateTime threshold, List<TariffChangeRequestStatus> targetStatuses);
}

