package ru.urasha.callmeani.blps.service.eis;

import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EisOperationResult(
    EisOperationType operationType,
    Long requestId,
    Long subscriberId,
    BigDecimal amount,
    TariffChangeRequestStatus status,
    String errorReason,
    OffsetDateTime processedAt
) {
}
