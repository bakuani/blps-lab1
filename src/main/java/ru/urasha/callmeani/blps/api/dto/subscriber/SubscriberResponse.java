package ru.urasha.callmeani.blps.api.dto.subscriber;

import java.math.BigDecimal;

public record SubscriberResponse(
    Long id,
    String phone,
    String fullName,
    BigDecimal balance,
    Long currentTariffId
) {
}



