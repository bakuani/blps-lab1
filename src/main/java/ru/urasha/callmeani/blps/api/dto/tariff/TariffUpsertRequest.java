package ru.urasha.callmeani.blps.api.dto.tariff;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TariffUpsertRequest(
    @NotBlank String name,
    @NotBlank String description,
    @NotNull @DecimalMin(value = "0.00") BigDecimal monthlyFee,
    @NotNull @DecimalMin(value = "0.00") BigDecimal switchFee,
    boolean customizable,
    @NotBlank String pdfUrl,
    @NotNull Long categoryId
) {
}


