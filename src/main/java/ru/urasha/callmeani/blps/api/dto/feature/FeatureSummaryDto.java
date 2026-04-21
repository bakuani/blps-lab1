package ru.urasha.callmeani.blps.api.dto.feature;

import java.math.BigDecimal;

public record FeatureSummaryDto(
    Long id,
    String name,
    String category,
    BigDecimal monthlyFee,
    String status
) {
}


