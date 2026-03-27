package ru.urasha.callmeani.blps.api.dto.admin;

import java.math.BigDecimal;

public record TariffOptionAdminResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Long tariffId
) {
}
