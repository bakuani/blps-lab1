package ru.urasha.callmeani.blps.eis.model;

import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EisOperationResult(
    EisOperationType operationType,
    Long requestId,
    Long subscriberId,
    BigDecimal amount,
    BusinessRequestStatus status,
    String errorReason,
    OffsetDateTime processedAt
) {
}
