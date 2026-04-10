package ru.urasha.callmeani.blps.api.dto;

import java.util.List;

public record DisableFeatureResponse(
    boolean success,
    String message,
    Long serviceId,
    List<BillingTransactionDto> billingTransactions,
    NotificationDto notification
) {
}

