package ru.urasha.callmeani.blps.api.dto.admin;

import java.math.BigDecimal;

public record SubscriberAdminResponse(
    Long id,
    String phone,
    String fullName,
    BigDecimal balance,
    Long currentTariffId
) {
}
