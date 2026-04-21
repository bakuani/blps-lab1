package ru.urasha.callmeani.blps.api.dto.tariff;

import java.math.BigDecimal;
import java.util.List;
import ru.urasha.callmeani.blps.api.dto.billing.BillingTransactionDto;
import ru.urasha.callmeani.blps.api.dto.notification.NotificationDto;
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



