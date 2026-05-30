package ru.urasha.callmeani.blps.api.dto.billing;

import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;

import java.time.OffsetDateTime;

public record MonthlyFeeChargeRequestStatusResponse(
    Long requestId,
    String billingPeriod,
    TariffChangeRequestStatus status,
    String errorMessage,
    int attemptCount,
    Long dolibarrThirdPartyId,
    Long dolibarrInvoiceId,
    String dolibarrInvoiceRef,
    OffsetDateTime updatedAt
) {
}
