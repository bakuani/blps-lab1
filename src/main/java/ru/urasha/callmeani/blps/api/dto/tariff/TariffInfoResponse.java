package ru.urasha.callmeani.blps.api.dto.tariff;

import java.math.BigDecimal;

public record TariffInfoResponse(
    Long subscriberId,
    String subscriberPhone,
    String subscriberName,
    BigDecimal balance,
    TariffSummaryDto currentTariff
) {
}

