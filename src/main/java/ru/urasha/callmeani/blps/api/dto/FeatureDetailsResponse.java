package ru.urasha.callmeani.blps.api.dto;

import java.math.BigDecimal;

public record FeatureDetailsResponse(
    Long id,
    String name,
    String description,
    String category,
    BigDecimal monthlyFee
) {
}

