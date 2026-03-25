package ru.urasha.callmeani.blps.api.dto;

import java.util.List;

public record DisableServiceResponse(
    boolean success,
    String message,
    Long serviceId,
    List<BillingTransactionDto> billingTransactions,
    NotificationDto notification
) {
}
