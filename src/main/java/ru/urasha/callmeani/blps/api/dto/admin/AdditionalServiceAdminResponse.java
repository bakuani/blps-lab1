package ru.urasha.callmeani.blps.api.dto.admin;

import java.math.BigDecimal;

public record AdditionalServiceAdminResponse(
    Long id,
    String name,
    String description,
    BigDecimal monthlyFee,
    Long categoryId
) {
}
