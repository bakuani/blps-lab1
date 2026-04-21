package ru.urasha.callmeani.blps.api.dto.feature;

import java.math.BigDecimal;

public record AdditionalFeatureResponse(
    Long id,
    String name,
    String description,
    BigDecimal monthlyFee,
    Long categoryId
) {
}




