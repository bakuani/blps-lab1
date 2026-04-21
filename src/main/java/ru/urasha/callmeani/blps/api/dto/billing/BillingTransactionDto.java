package ru.urasha.callmeani.blps.api.dto.billing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BillingTransactionDto(
    String type,
    BigDecimal amount,
    String description,
    OffsetDateTime createdAt
) {
}

