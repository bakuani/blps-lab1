package ru.urasha.callmeani.blps.api.dto.tariff;

import java.math.BigDecimal;

public record TariffOptionResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Long tariffId
) {
}



