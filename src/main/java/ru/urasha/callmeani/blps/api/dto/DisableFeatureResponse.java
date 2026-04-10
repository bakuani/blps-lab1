package ru.urasha.callmeani.blps.api.dto;

import java.util.List;

public record DisableFeatureResponse(
    boolean success,
    String message,
    Long featureId,
    List<BillingTransactionDto> billingTransactions,
    NotificationDto notification
) {
}

