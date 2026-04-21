package ru.urasha.callmeani.blps.api.dto.feature;

import java.util.List;
import ru.urasha.callmeani.blps.api.dto.billing.BillingTransactionDto;
import ru.urasha.callmeani.blps.api.dto.notification.NotificationDto;

public record DisableFeatureResponse(
    boolean success,
    String message,
    Long featureId,
    List<BillingTransactionDto> billingTransactions,
    NotificationDto notification
) {
}




