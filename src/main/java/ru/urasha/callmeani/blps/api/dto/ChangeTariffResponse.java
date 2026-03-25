package ru.urasha.callmeani.blps.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ChangeTariffResponse(
    boolean success,
    String message,
    TariffSummaryDto previousTariff,
    TariffSummaryDto newTariff,
    Map<String, String> selectedOptions,
    BigDecimal balance,
    List<BillingTransactionDto> billingTransactions,
    NotificationDto notification
) {
}
