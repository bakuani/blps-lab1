package ru.urasha.callmeani.blps.api.dto;

import java.math.BigDecimal;

public record ServiceDetailsResponse(
    Long id,
    String name,
    String description,
    String category,
    BigDecimal monthlyFee
) {
}
