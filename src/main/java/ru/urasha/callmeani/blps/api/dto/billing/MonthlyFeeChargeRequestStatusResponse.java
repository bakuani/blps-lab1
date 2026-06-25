package ru.urasha.callmeani.blps.api.dto.billing;

import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;

import java.time.OffsetDateTime;

public record MonthlyFeeChargeRequestStatusResponse(
    Long requestId,
    String billingPeriod,
    BusinessRequestStatus status,
    String errorMessage,
    int attemptCount,
    Long dolibarrThirdPartyId,
    Long dolibarrInvoiceId,
    String dolibarrInvoiceRef,
    OffsetDateTime updatedAt
) {
}
