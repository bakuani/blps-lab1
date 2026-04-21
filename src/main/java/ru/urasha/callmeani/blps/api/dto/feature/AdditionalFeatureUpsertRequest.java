package ru.urasha.callmeani.blps.api.dto.feature;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdditionalFeatureUpsertRequest(
    @NotBlank String name,
    @NotBlank String description,
    @NotNull @DecimalMin(value = "0.00") BigDecimal monthlyFee,
    @NotNull Long categoryId
) {
}



